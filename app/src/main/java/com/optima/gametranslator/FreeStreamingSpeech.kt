package com.optima.gametranslator

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.k2fsa.sherpa.onnx.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** English Zipformer streaming entirely on the device. No inference API. */
class FreeStreamingSpeech(context: Context) {
    private val root=File(context.filesDir,"free-streaming-en-v1")
    private val parent=context.filesDir
    private val messages=context.resources
    private val main=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor()
    private val generation=AtomicLong()
    private val lock=Any()
    private var recorder: AudioRecord?=null
    @Volatile private var download: HttpURLConnection?=null
    @Volatile private var closed=false
    @Volatile private var flush=false
    private class AudioQueueOverrun: Exception()
    private data class Part(val file: String,val size: Long,val sha: String)
    private val parts=listOf(
        Part("encoder-epoch-99-avg-1-chunk-16-left-64.int8.onnx",71082637,"0d072fd4ef956294ba9db9e9a71a541ac70659095ec4934c8453d8b2fe740187"),
        Part("decoder-epoch-99-avg-1-chunk-16-left-64.int8.onnx",1307236,"98da299f471e38bb4e1a8df579b8cc9122d6039576a77e357b3c60f17dd83b02"),
        Part("joiner-epoch-99-avg-1-chunk-16-left-64.int8.onnx",259335,"d944208d660d67c8d72cd2acaeac971fa5ceb8c80e76c1968148846fedd6e297"),
        Part("tokens.txt",5048,"49e3c2646595fd907228b3c6787069658f67b17377c60aeb8619c4551b2316fb"))
    private val base="https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-26/resolve/672fbf1b30579d6585301139bb363f42a0ad4a24/"
    fun ready()=File(root,"ready-v1").isFile&&parts.all { File(root,it.file).length()==it.size }
    private fun deliver(token: Long,action: ()->Unit) { main.post { if(!closed&&generation.get()==token)action() } }
    fun prepare(onProgress: (Int)->Unit,onDone: ()->Unit,onError: (String)->Unit) {
        stop();val token=generation.get()
        worker.execute {
            val staging=File(parent,"free-streaming-staging-$token")
            try {
                if(ready()) { deliver(token,onDone);return@execute }
                check(staging.mkdirs()||staging.isDirectory)
                val total=parts.sumOf { it.size };var received=0L;var previous=-1
                for(part in parts) {
                    check(generation.get()==token&&!closed)
                    val connection=URL(base+part.file).openConnection() as HttpURLConnection
                    download=connection;connection.connectTimeout=15000;connection.readTimeout=20000
                    try {
                        check(connection.responseCode==200&&connection.url.protocol=="https")
                        val digest=MessageDigest.getInstance("SHA-256");var size=0L
                        connection.inputStream.use { input -> File(staging,part.file).outputStream().use { output ->
                            val buffer=ByteArray(32768)
                            while(true) {
                                check(generation.get()==token&&!closed)
                                val n=input.read(buffer);if(n<0)break
                                size+=n;check(size<=part.size);output.write(buffer,0,n);digest.update(buffer,0,n)
                                received+=n;val progress=(received*100/total).toInt()
                                if(progress!=previous) { previous=progress;deliver(token) { onProgress(progress) } }
                            }
                        } }
                        check(size==part.size&&digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }==part.sha)
                    } finally { connection.disconnect();download=null }
                }
                synchronized(lock) {
                    check(generation.get()==token&&!closed)
                    if(root.exists())check(root.deleteRecursively())
                    check(staging.renameTo(root));File(root,"ready-v1").writeText("zipformer-en-int8")
                }
                deliver(token,onDone)
            } catch(_: Exception) { deliver(token) { onError(messages.getString(R.string.free_download_failed)) } }
            finally { staging.deleteRecursively() }
        }
    }
    @SuppressLint("MissingPermission")
    fun start(mode: Int,onReady: ()->Unit,onText: (String,Boolean)->Unit,onLevel: (Int)->Unit,onError: (String)->Unit) {
        stop();val token=generation.get();flush=false
        worker.execute {
            var decoder: OnlineRecognizer?=null;var stream: OnlineStream?=null
            var input: AudioRecord?=null;var capture: Thread?=null
            try {
                check(generation.get()==token&&!closed);check(ready())
                val policy=SpeechEndpointPolicy.forMode(mode)
                val endpoint=EndpointConfig(EndpointRule(false,2.4f,0f),
                    EndpointRule(true,policy.trailingSilence,0f),
                    EndpointRule(true,policy.longTurnSilence,policy.longTurnSeconds))
                val config=OnlineRecognizerConfig(modelConfig=OnlineModelConfig(
                    transducer=OnlineTransducerModelConfig(File(root,parts[0].file).absolutePath,
                        File(root,parts[1].file).absolutePath,File(root,parts[2].file).absolutePath),
                    tokens=File(root,"tokens.txt").absolutePath,numThreads=2,provider="cpu"),
                    enableEndpoint=true,endpointConfig=endpoint,decodingMethod="modified_beam_search",maxActivePaths=policy.paths)
                val engine=try { OnlineRecognizer(config=config) } catch(error: Exception) { File(root,"ready-v1").delete();throw error };decoder=engine
                val session=engine.createStream();stream=session
                val min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);check(min>0)
                val mic=AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,maxOf(min*2,6400));input=mic
                synchronized(lock) { check(generation.get()==token&&!closed);check(mic.state==AudioRecord.STATE_INITIALIZED);recorder=mic;mic.startRecording() }
                check(mic.recordingState==AudioRecord.RECORDSTATE_RECORDING)
                val frames=java.util.concurrent.ArrayBlockingQueue<FloatArray>(40)
                val failure=java.util.concurrent.atomic.AtomicReference<Exception?>()
                val producer=Thread({
                    try {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
                        val samples=ShortArray(800)
                        while(generation.get()==token&&!closed) {
                            val n=mic.read(samples,0,samples.size,AudioRecord.READ_BLOCKING)
                            if(generation.get()!=token||closed)break
                            check(n>0);if(!frames.offer(FloatArray(n) { samples[it]/32768f }))throw AudioQueueOverrun()
                        }
                    } catch(error: Exception) { failure.set(error) }
                },"zipformer-microphone").apply { isDaemon=true }
                capture=producer;producer.start();deliver(token,onReady)
                val turns=StreamingTurns()
                fun publish(outputs: List<StreamingTurns.Output>) { outputs.forEach { output -> deliver(token) { onText(output.text,output.final) } } }
                var previous="";var lastPartial=0L;var lastLevel=0L
                while(generation.get()==token&&!closed) {
                    failure.get()?.let { throw it }
                    val frame=frames.poll(100,java.util.concurrent.TimeUnit.MILLISECONDS)
                    if(frame==null) { check(producer.isAlive);publish(turns.tick(android.os.SystemClock.elapsedRealtime()));continue }
                    val now=android.os.SystemClock.elapsedRealtime()
                    if(now-lastLevel>150) {
                        lastLevel=now
                        val level=(kotlin.math.sqrt(frame.sumOf { (it*it).toDouble() }/frame.size)*600).toInt().coerceIn(0,100)
                        deliver(token) { onLevel(level) }
                    }
                    session.acceptWaveform(frame,16000)
                    while(engine.isReady(session)) { check(generation.get()==token&&!closed);engine.decode(session) }
                    val text=engine.getResult(session).text.trim()
                    if(engine.isEndpoint(session)||flush) {
                        flush=false;previous=""
                        publish(turns.update(text,true,now))
                        engine.reset(session)
                    } else if(text.isNotBlank()&&text!=previous&&now-lastPartial>=120) {
                        previous=text;lastPartial=now;publish(turns.update(text,false,now))
                    }
                    publish(turns.tick(now))
                }
            } catch(_: AudioQueueOverrun) { deliver(token) { onError(messages.getString(R.string.free_capture_overrun)) } }
            catch(_: OutOfMemoryError) { deliver(token) { onError(messages.getString(R.string.speech_memory_error)) } }
            catch(_: LinkageError) { deliver(token) { onError(messages.getString(R.string.ui_225)) } }
            catch(_: Exception) { deliver(token) { onError(messages.getString(R.string.free_capture_failed)) } }
            finally {
                synchronized(lock) { if(recorder===input)recorder=null;try { input?.stop() } catch(_: Exception) {};capture?.join(1000);input?.release() }
                stream?.release();decoder?.release()
            }
        }
    }
    fun finishUtterance() { flush=true }
    fun stop() { generation.incrementAndGet();synchronized(lock) { try { recorder?.stop() } catch(_: Exception) {} };download?.disconnect() }
    fun close() { closed=true;stop();worker.shutdown() }
}
