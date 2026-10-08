package com.optima.gametranslator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

/** Recognition and capture remain on one worker; stop invalidates all queued UI events. */
class OfflineSpeech(context: Context) {
    private val messages=context.resources
    private val app=context.applicationContext
    private val root=File(app.filesDir,"speech-models")
    private val modelPath=File(root,ModelArchive.ROOT)
    private val main=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor()
    private val generation=AtomicLong()
    private val lock=Any()
    private var recorder: AudioRecord?=null
    @Volatile private var connection: HttpURLConnection?=null
    @Volatile private var closed=false
    @Volatile private var flushRequested=false
    fun ready()=File(modelPath,"ready-v1").isFile
    private fun deliver(token: Long,action: ()->Unit) { main.post { if(!closed&&generation.get()==token) action() } }
    fun prepare(onProgress: (String)->Unit,onDone: ()->Unit,onError: (String)->Unit) {
        if(closed)return
        stop();val token=generation.get()
        worker.execute {
            if(generation.get()!=token||closed)return@execute
            val staging=File(root,"staging-${token}")
            val archive=File(root,"download-${token}.zip")
            try {
                if(ready()) { deliver(token,onDone);return@execute }
                check(root.mkdirs()||root.isDirectory)
                deliver(token) { onProgress(messages.getString(R.string.ui_220)) }
                val http=URL("https://alphacephei.com/vosk/models/${ModelArchive.ROOT}.zip").openConnection() as HttpURLConnection
                connection=http;http.connectTimeout=15000;http.readTimeout=15000
                require(http.responseCode==200&&http.url.protocol=="https")
                val total=http.contentLengthLong
                var size=0L;var lastProgress=-1
                http.inputStream.use { input -> archive.outputStream().use { output ->
                    val buffer=ByteArray(32768)
                    while(true) {
                        check(generation.get()==token&&!closed)
                        val count=input.read(buffer);if(count<0)break
                        size+=count;require(size<=100L*1024*1024)
                        output.write(buffer,0,count)
                        val progress=if(total>0) (size*100/total).toInt().coerceIn(0,100) else (size/1024/1024).toInt()
                        if(progress!=lastProgress) { lastProgress=progress;deliver(token) {
                            onProgress(if(total>0) messages.getString(R.string.ui_221, progress) else messages.getString(R.string.ui_222, size/1024/1024))
                        } }
                    }
                } }
                http.disconnect();connection=null
                deliver(token) { onProgress(messages.getString(R.string.ui_223)) }
                check(staging.mkdirs()||staging.isDirectory)
                archive.inputStream().use { ModelArchive.unpack(it,staging) { generation.get()!=token||closed } }
                val unpacked=File(staging,ModelArchive.ROOT)
                Model(unpacked.absolutePath).close()
                synchronized(lock) {
                    check(generation.get()==token&&!closed)
                    if(modelPath.exists()) check(modelPath.deleteRecursively())
                    check(unpacked.renameTo(modelPath))
                    File(modelPath,"ready-v1").writeText("vosk-small-en-us-0.15")
                }
                deliver(token,onDone)
            } catch (_: Exception) {
                deliver(token) { onError(messages.getString(R.string.ui_224)) }
            } catch (_: LinkageError) {
                deliver(token) { onError(messages.getString(R.string.ui_225)) }
            } finally {
                connection?.disconnect();connection=null;archive.delete();staging.deleteRecursively()
            }
        }
    }
    @SuppressLint("MissingPermission")
    fun start(mode: Int=0,onReady: ()->Unit,onText: (String,Boolean)->Unit,onLevel: (Int)->Unit,onError: (String)->Unit) {
        if(closed)return
        stop();val token=generation.get();flushRequested=false
        worker.execute {
            if(generation.get()!=token||closed)return@execute
            var model: Model?=null;var recognizer: Recognizer?=null;var input: AudioRecord?=null
            try {
                check(ContextCompat.checkSelfPermission(app,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)
                check(ready())
                RecognitionTuning.configure(modelPath,mode)
                model=Model(modelPath.absolutePath)
                val decoder=Recognizer(model,16000f);recognizer=decoder
                val minimum=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
                check(minimum>0)
                val mic=AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,
                    AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,maxOf(minimum*4,32000));input=mic
                check(mic.state==AudioRecord.STATE_INITIALIZED)
                synchronized(lock) {
                    check(generation.get()==token&&!closed)
                    recorder=mic;mic.startRecording()
                }
                check(mic.recordingState==AudioRecord.RECORDSTATE_RECORDING)
                deliver(token,onReady)
                val buffer=ShortArray(800);var previous="";var lastLevel=0L
                while(generation.get()==token&&!closed) {
                    val count=mic.read(buffer,0,buffer.size,AudioRecord.READ_BLOCKING)
                    if(generation.get()!=token||closed)break
                    check(count>0) { "Microphone read failed" }
                    val now=android.os.SystemClock.elapsedRealtime()
                    if(now-lastLevel>=150) {
                        lastLevel=now
                        var sum=0.0;for(i in 0 until count)sum+=buffer[i].toDouble()*buffer[i]
                        val level=(sqrt(sum/count)/32768.0*600).toInt().coerceIn(0,100)
                        deliver(token) { onLevel(level) }
                    }
                    val ended=decoder.acceptWaveForm(buffer,count)
                    if(ended||flushRequested) {
                        val json=if(ended)decoder.result else decoder.finalResult
                        if(flushRequested&&!ended)decoder.reset()
                        flushRequested=false;previous=""
                        val text=JSONObject(json).optString("text").trim()
                        if(text.isNotBlank()) deliver(token) { onText(text,true) }
                    } else {
                        val partial=JSONObject(decoder.partialResult).optString("partial").trim()
                        if(partial!=previous) { previous=partial;deliver(token) { onText(partial,false) } }
                    }
                }
            } catch (_: Exception) {
                deliver(token) { onError(messages.getString(R.string.ui_226)) }
            } catch (_: LinkageError) {
                deliver(token) { onError(messages.getString(R.string.ui_225)) }
            } finally {
                synchronized(lock) {
                    if(recorder===input)recorder=null
                    try { input?.stop() } catch (_: Exception) { }
                    input?.release()
                }
                recognizer?.close();model?.close()
            }
        }
    }
    fun finishUtterance() { flushRequested=true }
    fun stop() {
        synchronized(lock) {
            generation.incrementAndGet()
            try { recorder?.stop() } catch (_: Exception) { }
        }
        connection?.disconnect()
    }
    fun close() { if(!closed) { stop();closed=true;worker.shutdown() } }
}
