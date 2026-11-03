package com.optima.gametranslator

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import okhttp3.*
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** PCM microphone -> authenticated WSS gateway -> STT V2 streaming responses. */
class CloudSpeech(private val echo: EchoReference?=null) {
    private val main=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor()
    private val client=OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS)
        .readTimeout(0,TimeUnit.MILLISECONDS).pingInterval(20,TimeUnit.SECONDS).build()
    private val generation=AtomicLong()
    private val lock=Any()
    private var socket: WebSocket?=null
    private var recorder: AudioRecord?=null
    private var closed=false
    fun start(service: ProfessionalGateway,language: String,onReady: ()->Unit,onText: (String,Boolean)->Unit,onError: (String)->Unit) {
        stop();val token=generation.get();val events=RecognitionEvents();val started=java.util.concurrent.atomic.AtomicBoolean(false)
        fun deliver(action: ()->Unit) { main.post { if(!closed&&generation.get()==token)action() } }
        fun fail(reason: String) { deliver { stop();onError(reason) } }
        if(!service.configured) { fail("configuration");return }
        val request=Request.Builder().url(service.endpoint.replaceFirst("https://","wss://")+"/v1/listen")
            .header("Authorization","Bearer "+service.sessionToken).build()
        socket=client.newWebSocket(request,object: WebSocketListener() {
            override fun onOpen(ws: WebSocket,response: Response) {
                if(generation.get()!=token) { ws.cancel();return }
                ws.send(JSONObject().put("language",language).toString())
            }
            override fun onMessage(ws: WebSocket,text: String) {
                if(generation.get()!=token)return
                try {
                    val data=JSONObject(text)
                    when(data.getString("type")) {
                        "ready" -> if(started.compareAndSet(false,true)) {worker.execute { capture(ws,token,{ deliver(onReady) },::fail) } }
                        "result" -> {
                            val final=data.optBoolean("final");val result=data.getString("text")
                            events.accept(result,final,if(final)data.getString("id") else "")?.let { event ->
                                deliver { onText(event.text,event.final) }
                            }
                        }
                        "rotate" -> fail("rotate")
                        "error" -> fail(data.optString("reason","service"))
                    }
                } catch(_: Exception) { fail("protocol") }
            }
            override fun onFailure(ws: WebSocket,t: Throwable,response: Response?) { fail(if(response?.code==401) "unauthorized" else "connection") }
            override fun onClosed(ws: WebSocket,code: Int,reason: String) { fail("connection") }
        })
        main.postDelayed({ if(generation.get()==token&&!started.get())fail("connection") },15000)
    }
    @SuppressLint("MissingPermission")
    private fun capture(ws: WebSocket,token: Long,onReady: ()->Unit,onError: (String)->Unit) {
        var input: AudioRecord?=null;var echoEffect: MicrophoneEcho?=null
        try {
            if(generation.get()!=token)return
            val min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
            check(min>0)
            val mic=AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,16000,AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,maxOf(min*2,6400));input=mic
            echoEffect=MicrophoneEcho(mic)
            synchronized(lock) { check(generation.get()==token);check(mic.state==AudioRecord.STATE_INITIALIZED);recorder=mic;mic.startRecording() }
            check(mic.recordingState==AudioRecord.RECORDSTATE_RECORDING);onReady()
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            val buffer=ByteArray(1600)
            while(generation.get()==token) {
                val n=mic.read(buffer,0,buffer.size,AudioRecord.READ_BLOCKING)
                if(generation.get()!=token)break
                check(n>0&&n%2==0)
                val raw=FloatArray(n/2) { val i=it*2;((buffer[i].toInt() and 255) or (buffer[i+1].toInt() shl 8)).toShort()/32768f }
                val clean=echo?.clean(raw,android.os.SystemClock.elapsedRealtime())?:raw
                for(i in clean.indices) { val value=(clean[i]*32768f).toInt().coerceIn(-32768,32767);buffer[i*2]=value.toByte();buffer[i*2+1]=(value shr 8).toByte() }
                // 64 KB ~= 2 seconds of PCM: never queue minutes of stale audio.
                check(ws.queueSize()<=64000&&ws.send(buffer.toByteString(0,n)))
            }
        } catch(_: Exception) { if(generation.get()==token)onError("capture") }
        finally { synchronized(lock) { if(recorder===input)recorder=null;try { input?.stop() } catch(_: Exception) {};input?.release();echoEffect?.close() } }
    }
    fun stop() { generation.incrementAndGet();synchronized(lock) { try { recorder?.stop() } catch(_: Exception) {} };socket?.cancel();socket=null }
    fun close() { closed=true;stop();worker.shutdown();client.dispatcher.executorService.shutdown();client.connectionPool.evictAll() }
}
