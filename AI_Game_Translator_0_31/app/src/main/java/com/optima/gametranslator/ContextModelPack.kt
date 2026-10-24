package com.optima.gametranslator

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** One resumable download; pinned revision, length and SHA; inference never uses the network. */
class ContextModelPack(context: Context): AutoCloseable {
    private val directory=File(context.noBackupFilesDir,"context-mt-v1").apply { mkdirs() }
    val file=File(directory,"hy-mt2-1.8b.litertlm")
    private val partial=File(directory,"download.part")
    private val marker=File(directory,"verified")
    private val worker=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private val busy=AtomicBoolean(false)
    private val cancelled=AtomicBoolean(false)
    private val closed=AtomicBoolean(false)
    private val client=OkHttpClient.Builder().connectTimeout(30,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS).callTimeout(45,TimeUnit.MINUTES).build()
    @Volatile private var call: okhttp3.Call?=null
    fun ready()=file.isFile&&file.length()==BYTES&&marker.isFile
    fun verify() {
        check(ready()) { "Context model not installed" }
        if(digest(file)!=SHA) { marker.delete();throw IOException("Context model checksum failed") }
    }
    private fun digest(path: File): String {
        val sha=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(1024*1024)
        path.inputStream().buffered().use { input -> while(true) { check(!closed.get()) { "Closed" };val n=input.read(buffer);if(n<0)break;sha.update(buffer,0,n) } }
        return sha.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    fun download(progress: (Int)->Unit,done: (Boolean)->Unit) {
        if(closed.get()||!busy.compareAndSet(false,true)) { done(false);return }
        cancelled.set(false)
        worker.execute {
            var success=false
            try {
                if(ready()) { verify();success=true }
                else {
                    if(partial.length()>BYTES)partial.delete()
                    var offset=partial.length()
                    check(directory.usableSpace>=BYTES-offset+2L*1024*1024*1024) { "Insufficient storage" }
                    if(offset<BYTES) {
                        val request=Request.Builder().url(URL).apply { if(offset>0)header("Range","bytes=$offset-") }.build()
                        val running=client.newCall(request);call=running
                        running.execute().use { response ->
                            check(response.request.url.isHttps&&response.isSuccessful) { "Download response" }
                            val body=checkNotNull(response.body)
                            val transfer=ContextDownloadContract.response(response.code,offset,body.contentLength(),response.header("Content-Range"),BYTES)
                            val append=transfer.append;offset=transfer.offset
                            var lastPercent=-1
                            body.byteStream().use { input -> java.io.FileOutputStream(partial,append).use { output ->
                                val buffer=ByteArray(256*1024)
                                while(true) {
                                    check(!cancelled.get()&&!closed.get()) { "Cancelled" }
                                    val n=input.read(buffer);if(n<0)break
                                    offset+=n;check(offset<=BYTES) { "Oversized model" };output.write(buffer,0,n)
                                    val percent=(offset*100/BYTES).toInt()
                                    if(percent!=lastPercent) { lastPercent=percent;main.post { if(!closed.get()&&!cancelled.get())progress(percent) } }
                                }
                                output.fd.sync()
                            } }
                        }
                    }
                    check(!cancelled.get()&&!closed.get()&&partial.length()==BYTES) { "Incomplete download" }
                    if(digest(partial)!=SHA) { partial.delete();throw IOException("Model checksum failed") }
                    check(!cancelled.get()&&!closed.get())
                    check(partial.renameTo(file)) { "Model install failed" };marker.writeText(SHA);success=true
                }
            } catch(_: Exception) { /* Keep partial bytes for a verified resume; no partial model is used. */ }
            finally { call=null;busy.set(false);val result=success;main.post { if(!closed.get())done(result) } }
        }
    }
    fun cancel() { cancelled.set(true);call?.cancel() }
    fun remove(): Boolean { if(busy.get())return false;marker.delete();partial.delete();return !file.exists()||file.delete() }
    override fun close() { if(closed.compareAndSet(false,true)) { cancel();worker.shutdown();client.dispatcher.executorService.shutdown();client.connectionPool.evictAll() } }
    companion object {
        const val BYTES=1827026224L
        const val SHA="275ff52bf04c4d577e1bf48733daa77d873d78b0637841574d7a10e1560a011a"
        const val URL="https://huggingface.co/litert-community/Hy-MT2-1.8B/resolve/a45c92d69a8dccfbeee5000507780f78dae2b153/Hy-MT2-1.8B_int8.litertlm?download=true"
    }
}
