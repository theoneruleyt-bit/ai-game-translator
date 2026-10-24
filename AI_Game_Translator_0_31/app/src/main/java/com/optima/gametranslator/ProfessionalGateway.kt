package com.optima.gametranslator

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Optional pilot gateway. Provider credentials never enter the APK. */
class ProfessionalGateway {
    private val active=java.util.concurrent.ConcurrentHashMap.newKeySet<HttpURLConnection>()
    fun cancelRequests() { active.forEach { it.disconnect() } }
    @Volatile var endpoint=""
    @Volatile var sessionToken=""
    @Volatile var voiceStyle="female"
    @Volatile var mood="natural"
    val configured: Boolean get()=endpoint.isNotBlank()&&sessionToken.isNotBlank()
    fun configure(url: String,token: String) {
        val parsed=URL(url)
        require(parsed.protocol=="https"&&parsed.host.isNotBlank()&&parsed.userInfo==null&&parsed.query==null&&parsed.ref==null)
        require(token.length in 16..2048&&token.none { it=='\n'||it=='\r' })
        endpoint=url.trimEnd('/');sessionToken=token
    }
    fun clear() { cancelRequests();endpoint="";sessionToken="" }
    private fun request(path: String,data: JSONObject): JSONObject {
        check(configured)
        val connection=URL(endpoint+path).openConnection() as HttpURLConnection
        active.add(connection)
        try {
            connection.instanceFollowRedirects=false;connection.connectTimeout=5000;connection.readTimeout=12000
            connection.requestMethod="POST";connection.doOutput=true
            connection.setRequestProperty("Authorization","Bearer $sessionToken")
            connection.setRequestProperty("Content-Type","application/json")
            connection.outputStream.use { it.write(data.toString().toByteArray(Charsets.UTF_8)) }
            check(connection.responseCode==200) { "Professional service unavailable" }
            val result=connection.inputStream.use { input ->
                val bytes=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) { val n=input.read(buffer);if(n<0)break;check(bytes.size()+n<=4_000_000);bytes.write(buffer,0,n) }
                bytes.toString("UTF-8")
            }
            return JSONObject(result)
        } finally { active.remove(connection);connection.disconnect() }
    }
    fun translate(text: String,source: String,target: String): String {
        val answer=request("/v1/translate",JSONObject().put("text",text).put("source",source).put("target",target))
        return answer.getString("text").also { require(it.isNotBlank()&&it.length<=12000) }
    }
    fun synthesize(text: String,language: String,rate: Float): ByteArray {
        val answer=request("/v1/speak",JSONObject().put("text",text).put("language",language)
            .put("style",voiceStyle).put("mood",mood).put("rate",rate.toDouble()))
        return android.util.Base64.decode(answer.getString("audio"),android.util.Base64.DEFAULT).also { require(it.isNotEmpty()&&it.size<=3_000_000) }
    }
}
