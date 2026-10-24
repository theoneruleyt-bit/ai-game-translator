package com.optima.gametranslator

import android.app.Activity
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors

/** Optional account-backed authority. No secrets/ID tokens are persisted or logged. */
class ProService(private val activity: Activity) {
    private val base=activity.getString(R.string.pro_server_url).trim().trimEnd('/')
    private val clientId=activity.getString(R.string.google_web_client_id).trim()
    val enabled=base.isNotBlank()||clientId.isNotBlank()
    private val configured=try { val u=URL(base);u.protocol=="https"&&u.host.isNotBlank()&&u.userInfo==null&&u.query==null&&u.ref==null&&clientId.endsWith(".apps.googleusercontent.com") } catch (_: Exception) { false }
    private val main=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor()
    @Volatile private var closed=false
    private var cancellation: CancellationSignal?=null
    private var idToken=""
    var accountId=""; private set
    var usedMillis=0L; private set
    private var grantedUntil=0L
    val pro get()=SystemClock.elapsedRealtime()<grantedUntil
    val signedIn get()=idToken.isNotBlank()
    private var session=UUID.randomUUID().toString()
    private var stateBusy=false
    private val stateWaiters=mutableListOf<(Boolean)->Unit>()
    private var stateAgain=false
    private var usageBusy=false
    private val pendingUsage=java.util.LinkedHashMap<String,Pair<Long,(Boolean)->Unit>>()
    private var knownPurchases=JSONArray()
    fun signIn(done: (Boolean)->Unit) {
        if(!configured||closed||stateBusy||usageBusy) { done(false);return }
        grantedUntil=0
        cancellation?.cancel();cancellation=CancellationSignal()
        val option=GetSignInWithGoogleOption.Builder(clientId).build()
        val request=GetCredentialRequest.Builder().addCredentialOption(option).build()
        CredentialManager.create(activity).getCredentialAsync(activity,request,cancellation,ContextCompat.getMainExecutor(activity),
            object: CredentialManagerCallback<GetCredentialResponse,GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    if(closed)return
                    try {
                        val credential=result.credential
                        if(credential !is CustomCredential||credential.type!=GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { done(false);return }
                        idToken=GoogleIdTokenCredential.createFrom(credential.data).idToken
                        // Subject is only used for Play account binding; server verifies the signed token.
                        val claims=JSONObject(String(android.util.Base64.decode(idToken.split('.')[1],android.util.Base64.URL_SAFE),Charsets.UTF_8))
                        accountId=MessageDigest.getInstance("SHA-256").digest(claims.getString("sub").toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
                        refresh(done=done)
                    } catch (_: Exception) { idToken="";accountId="";done(false) }
                }
                override fun onError(e: GetCredentialException) { if(!closed)done(false) }
            })
    }
    private fun request(path: String,body: JSONObject,token: String): JSONObject {
        check(configured&&token.isNotBlank())
        val http=URL(base+path).openConnection() as HttpURLConnection
        try {
            http.instanceFollowRedirects=false;http.connectTimeout=5000;http.readTimeout=20000
            http.requestMethod="POST";http.doOutput=true
            http.setRequestProperty("Authorization","Bearer $token");http.setRequestProperty("Content-Type","application/json")
            val bytes=body.toString().toByteArray(Charsets.UTF_8);http.setFixedLengthStreamingMode(bytes.size)
            http.outputStream.use { it.write(bytes) }
            if(http.responseCode==401)throw SecurityException("sign_in_required")
            check(http.responseCode==200)
            val data=http.inputStream.use { input ->
                val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096)
                while(true) { val n=input.read(buffer);if(n<0)break;check(output.size()+n<=65536);output.write(buffer,0,n) };output.toString("UTF-8")
            }
            return JSONObject(data)
        } finally { http.disconnect() }
    }
    fun refresh(purchases: JSONArray?=null,done: (Boolean)->Unit={}) {
        if(purchases!=null) { knownPurchases=purchases;if(stateBusy)stateAgain=true }
        if(!enabled) { done(true);return }
        if(!signedIn||closed) { done(false);return }
        stateWaiters.add(done)
        if(stateBusy)return
        stateBusy=true
        val token=idToken;val body=JSONObject().put("purchases",knownPurchases)
        worker.execute {
            var reply: JSONObject?=null;var unauthenticated=false
            try {
                val result=request("/v1/state",body,token)
                check(result.getLong("trialUsedMillis") in 0..TrialMeter.LIMIT)
                check(result.get("pro") is Boolean)
                check(result.getLong("validForMillis") in 0..300000L)
                reply=result
            } catch (_: SecurityException) { unauthenticated=true } catch (_: Exception) { }
            main.post {
                if(!closed) {
                    stateBusy=false
                    if(unauthenticated) { idToken="";accountId="";grantedUntil=0 }
                    if(stateAgain&&signedIn) { stateAgain=false;refresh();return@post }
                    if(reply!=null) {
                        val result=reply!!;usedMillis=maxOf(usedMillis,result.getLong("trialUsedMillis").coerceIn(0,TrialMeter.LIMIT))
                        grantedUntil=if(result.optBoolean("pro",false))SystemClock.elapsedRealtime()+result.optLong("validForMillis",0).coerceIn(0,300000) else 0
                    }
                    stateAgain=false
                    val callbacks=stateWaiters.toList();stateWaiters.clear()
                    callbacks.forEach { it(reply!=null) }
                }
            }
        }
    }
    fun beginUsage() { session=UUID.randomUUID().toString() }
    fun reportUsage(elapsed: Long,done: (Boolean)->Unit={}) {
        if(!enabled||!signedIn||closed) { done(!enabled);return }
        val snapshot=session to elapsed.coerceIn(0,TrialMeter.LIMIT)
        if(usageBusy) {
            if(pendingUsage.size>=32&&!pendingUsage.containsKey(snapshot.first)) { done(false);return }
            pendingUsage[snapshot.first]=snapshot.second to done;return
        }
        sendUsage(snapshot,done)
    }
    private fun sendUsage(snapshot: Pair<String,Long>,done: (Boolean)->Unit) {
        usageBusy=true;val token=idToken
        worker.execute {
            var reply: JSONObject?=null
            try {
                val result=request("/v1/usage",JSONObject().put("session",snapshot.first).put("elapsedMillis",snapshot.second),token)
                check(result.getLong("trialUsedMillis") in 0..TrialMeter.LIMIT);reply=result
            } catch (_: Exception) { }
            main.post {
                if(!closed) {
                    usageBusy=false
                    if(reply!=null)usedMillis=maxOf(usedMillis,reply!!.getLong("trialUsedMillis").coerceIn(0,TrialMeter.LIMIT))
                    done(reply!=null)
                    val key=pendingUsage.keys.firstOrNull()
                    if(key!=null) {
                        val pending=pendingUsage.remove(key)!!
                        sendUsage(key to pending.first,pending.second)
                    }
                }
            }
        }
    }
    fun close() { closed=true;cancellation?.cancel();worker.shutdown() }
}
