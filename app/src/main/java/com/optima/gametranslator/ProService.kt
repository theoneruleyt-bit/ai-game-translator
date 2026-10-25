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
import java.util.concurrent.Executors

/** Account-backed Play checks. Credentials remain in memory; secrets stay on the Worker. */
class ProService(private val activity: Activity) {
    private val base = activity.getString(R.string.pro_server_url).trim().trimEnd('/')
    private val clientId = activity.getString(R.string.google_web_client_id).trim()
    val enabled = base.isNotBlank() || clientId.isNotBlank()
    private val configured = try {
        val u = URL(base)
        u.protocol == "https" && u.host.isNotBlank() && u.userInfo == null &&
            u.query == null && u.ref == null && clientId.endsWith(".apps.googleusercontent.com")
    } catch (_: Exception) { false }
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var closed = false
    private var cancellation: CancellationSignal? = null
    private var signingIn = false
    private var idToken = ""
    private var identityExpiresAt = 0L
    var accountId = ""
        private set
    // Trial counters remain local and separate for camera, microphone and images.
    var usedMillis = 0L
        private set
    private var grantedUntil = 0L
    val pro get() = signedIn && SystemClock.elapsedRealtime() < grantedUntil
    val signedIn get() = idToken.isNotBlank() && System.currentTimeMillis() < identityExpiresAt
    private var stateBusy = false
    private var stateAgain = false
    private val waiters = mutableListOf<(Boolean) -> Unit>()
    private var knownPurchases = JSONArray()
    private var lastStatus = "not_checked"
    private var lastHttp = 0
    private var lastRecords = ""
    var migrationRequired = false
        private set

    fun diagnostics(): String = buildString {
        appendLine("--- Server verification ---")
        appendLine("Configured: $configured; signed in: $signedIn; busy: $stateBusy")
        appendLine("HTTP: $lastHttp; result: $lastStatus")
        appendLine("Server Pro: $pro; lease remaining ms: ${(grantedUntil-SystemClock.elapsedRealtime()).coerceAtLeast(0)}")
        appendLine("Legacy migration required: $migrationRequired")
        if (lastRecords.isNotBlank()) appendLine(lastRecords)
    }

    fun signIn(done: (Boolean) -> Unit) {
        if (!configured || closed || stateBusy || signingIn) {
            lastStatus = if (!configured) "configuration_incomplete" else "busy"
            done(false); return
        }
        signingIn = true
        grantedUntil = 0
        idToken = ""; accountId = ""; identityExpiresAt = 0
        lastRecords = ""; migrationRequired = false
        cancellation?.cancel(); cancellation = CancellationSignal()
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        CredentialManager.create(activity).getCredentialAsync(
            activity, request, cancellation, ContextCompat.getMainExecutor(activity),
            object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    if (closed) return
                    signingIn = false
                    try {
                        val credential = result.credential
                        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        val claims = JSONObject(String(android.util.Base64.decode(token.split('.')[1], android.util.Base64.URL_SAFE), Charsets.UTF_8))
                        val subject = claims.getString("sub")
                        val expiry = Math.multiplyExact(claims.getLong("exp"), 1000L)
                        check(subject.isNotBlank() && expiry > System.currentTimeMillis())
                        // Used only for the Play binding; the Worker verifies the JWT signature.
                        accountId = MessageDigest.getInstance("SHA-256").digest(subject.toByteArray(Charsets.UTF_8))
                            .joinToString("") { "%02x".format(it.toInt() and 255) }
                        idToken = token; identityExpiresAt = expiry
                        lastStatus = "identity_received"
                        refresh(done = done)
                    } catch (_: Exception) {
                        idToken = ""; accountId = ""; identityExpiresAt = 0
                        lastStatus = "identity_parse_failed"; done(false)
                    }
                }
                override fun onError(e: GetCredentialException) {
                    if (closed) return
                    signingIn = false
                    // Class name only: never exception messages containing account data.
                    lastStatus = "sign_in_" + e.javaClass.simpleName.take(80)
                    done(false)
                }
            }
        )
    }

    private class RequestFailure(val http: Int, val code: String) : Exception()
    private fun request(body: JSONObject, token: String): JSONObject {
        check(configured && token.isNotBlank())
        val http = URL("$base/v1/state").openConnection() as HttpURLConnection
        try {
            http.instanceFollowRedirects = false
            http.connectTimeout = 5000; http.readTimeout = 20000
            http.requestMethod = "POST"; http.doOutput = true
            http.setRequestProperty("Authorization", "Bearer $token")
            http.setRequestProperty("Content-Type", "application/json")
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            http.setFixedLengthStreamingMode(bytes.size)
            http.outputStream.use { it.write(bytes) }
            val status = http.responseCode
            val stream = if (status == 200) http.inputStream else http.errorStream
            val text = stream?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    check(output.size() + n <= 65536)
                    output.write(buffer, 0, n)
                }
                output.toString("UTF-8")
            }.orEmpty()
            if (status != 200) {
                val candidate = runCatching { JSONObject(text).optString("error") }.getOrDefault("")
                val allowed = setOf("sign_in_required", "invalid_identity", "play_permission_denied",
                    "play_verification_unavailable", "google_service_unavailable", "invalid_server_configuration",
                    "invalid_google_response", "configuration_incomplete", "acknowledgement_failed",
                    "purchase_account_mismatch", "verification_unavailable", "too_many_purchases")
                throw RequestFailure(status, if (candidate in allowed) candidate else "server_request_failed")
            }
            return JSONObject(text)
        } finally { http.disconnect() }
    }

    fun refresh(purchases: JSONArray? = null, done: (Boolean) -> Unit = {}) {
        if (purchases != null) {
            knownPurchases = JSONArray(purchases.toString())
            if (stateBusy) stateAgain = true
        }
        if (!enabled) { done(true); return }
        if (closed || !signedIn) {
            grantedUntil = 0; lastStatus = "sign_in_required"; done(false); return
        }
        waiters.add(done)
        if (stateBusy) return
        stateBusy = true
        val token = idToken
        val body = JSONObject().put("purchases", knownPurchases)
        val startedAt = SystemClock.elapsedRealtime()
        worker.execute {
            var reply: JSONObject? = null
            var failure = "network_or_invalid_response"
            var responseCode = 0
            try {
                val result = request(body, token)
                responseCode = 200
                check(result.get("pro") is Boolean)
                val ttl = result.getLong("validForMillis")
                check(ttl in 0..60000L)
                check(result.getLong("trialUsedMillis") == 0L)
                reply = result
            } catch (e: RequestFailure) {
                responseCode = e.http; failure = e.code
            } catch (_: Exception) { }
            main.post {
                if (!closed) {
                    stateBusy = false
                    lastHttp = responseCode
                    if (responseCode == 401) {
                        idToken = ""; identityExpiresAt = 0; accountId = ""; grantedUntil = 0
                    }
                    if (stateAgain && signedIn) {
                        stateAgain = false
                        // Do not apply an earlier ownership snapshot while a newer one is queued.
                        refresh(); return@post
                    }
                    val result = reply
                    if (result != null) {
                        migrationRequired = result.optBoolean("migrationRequired", false)
                        val records = result.optJSONArray("records") ?: JSONArray()
                        lastRecords = buildString {
                            for (i in 0 until minOf(records.length(), 4)) {
                                val record = records.optJSONObject(i) ?: continue
                                fun safe(key: String) = record.optString(key, "").take(96)
                                    .filter { it.isLetterOrDigit() || it in "_-.:TZ+" }
                                appendLine("Product=${safe("product")}; state=${safe("state")}; result=${safe("result")}; expiresAt=${safe("expiresAt")}; ref=${safe("purchaseRef")}")
                            }
                        }
                        // Start from request time so network delay cannot extend a server lease.
                        grantedUntil = if (result.getBoolean("pro")) startedAt + result.getLong("validForMillis") else 0
                        lastStatus = if (migrationRequired) "legacy_migration_required" else if (pro) "verified_pro" else "verified_no_pro"
                    } else {
                        lastStatus = failure
                        // Fail closed: do not retain a positive result after a failed refresh.
                        grantedUntil = 0
                    }
                    stateAgain = false
                    val callbacks = waiters.toList(); waiters.clear()
                    callbacks.forEach { it(result != null) }
                }
            }
        }
    }

    // Compatibility with old call sites; local trial meters are the sole trial authority.
    fun beginUsage() = Unit
    fun reportUsage(@Suppress("UNUSED_PARAMETER") elapsed: Long, done: (Boolean) -> Unit = {}) { done(true) }
    fun close() {
        closed = true; grantedUntil = 0; idToken = ""; accountId = ""
        cancellation?.cancel(); worker.shutdown(); waiters.clear()
    }
}
