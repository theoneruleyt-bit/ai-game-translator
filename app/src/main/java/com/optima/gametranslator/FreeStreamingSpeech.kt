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
class FreeStreamingSpeech(context: Context,private val echo: EchoReference?=null,private val profile: LocalSpeechModel=LocalSpeechModels.giga) {
    private val root=File(context.filesDir,profile.folder)
    private val parent=context.filesDir
    private val messages=context.resources
    private val assets=context.assets
    private val main=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor()
    private val generation=AtomicLong()
    private val lock=Any()
    private var recorder: AudioRecord?=null
    @Volatile private var download: HttpURLConnection?=null
    @Volatile private var closed=false
    @Volatile private var flush=false
    private class AudioQueueOverrun: Exception()
    private class ModelCorrupt: Exception()
    private class GateUnavailable: Exception()
    private data class Frame(val samples: FloatArray,val capturedAt: Long)
    private val parts=profile.parts
    private val base=profile.base
    fun ready()=File(root,"ready-v1").isFile&&parts.all { File(root,it.file).length()==it.size }
    private fun deliver(token: Long,action: ()->Unit) { main.post { if(!closed&&generation.get()==token)action() } }
    fun prepare(onProgress: (Int)->Unit,onDone: ()->Unit,onError: (String)->Unit) {
        stop();val token=generation.get()
        worker.execute {
            val staging=File(parent,"free-streaming-staging-${java.util.UUID.randomUUID()}")
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
    fun start(mode: Int,onReady: ()->Unit,onText: (String,Boolean,String)->Unit,onLevel: (Int)->Unit,onError: (String)->Unit,onSignal: (CaptureSignal)->Unit={},onSpeech: (Boolean)->Unit={},adaptiveClauses: Boolean=true,onClauseStatus: (Boolean)->Unit={}) {
        stop();val token=generation.get();flush=false
        worker.execute {
            var decoder: OnlineRecognizer?=null;var stream: OnlineStream?=null;var voiceDetector: Vad?=null
            var input: AudioRecord?=null;var capture: Thread?=null;var echoEffect: MicrophoneEcho?=null
            var punctuator: PunctuationFallback?=null
            val clauses=AdaptiveSpeechClauses(adaptiveClauses)
            val consensus=TranscriptConsensus()
            val turns=StreamingTurns()
            var utterance=0L
            fun publish(outputs: List<StreamingTurns.Output>) { outputs.forEach { output ->
                val id="$token:$utterance"
                deliver(token) { onText(output.text,output.final,id) }
                if(output.final)utterance++
            } }
            fun report(message: String) { publish(turns.flushConfirmed());deliver(token) { onError(message) } }
            try {
                check(generation.get()==token&&!closed);check(ready())
                for(part in parts) {
                    check(generation.get()==token&&!closed)
                    val digest=MessageDigest.getInstance("SHA-256")
                    File(root,part.file).inputStream().use { input ->
                        val buffer=ByteArray(65536)
                        while(true) { check(generation.get()==token&&!closed);val n=input.read(buffer);if(n<0)break;digest.update(buffer,0,n) }
                    }
                    val checksum=digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                    if(checksum!=part.sha) { File(root,"ready-v1").delete();throw ModelCorrupt() }
                }
                val vadDigest=MessageDigest.getInstance("SHA-256")
                assets.open("silero_vad.onnx").use { input ->
                    val buffer=ByteArray(65536)
                    while(true) { val n=input.read(buffer);if(n<0)break;vadDigest.update(buffer,0,n) }
                }
                if(vadDigest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }!="9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6")throw GateUnavailable()
                val vad=try { Vad(assetManager=assets,config=VadModelConfig(
                    sileroVadModelConfig=SileroVadModelConfig(model="silero_vad.onnx",threshold=.5f,
                        minSilenceDuration=.25f,minSpeechDuration=.064f,windowSize=512,maxSpeechDuration=60f),
                    sampleRate=16000,numThreads=1,provider="cpu")) } catch(_: Exception) { throw GateUnavailable() }
                voiceDetector=vad
                if(adaptiveClauses) {
                    punctuator=try {
                        val model=SpeechPunctuator(assets)
                        PunctuationFallback(model::apply,model::close) { deliver(token) { onClauseStatus(false) } }
                    } catch(_: Exception) { null }
                        catch(_: LinkageError) { null } catch(_: OutOfMemoryError) { null }
                    val loaded=punctuator!=null;deliver(token) { onClauseStatus(loaded) }
                }
                fun punctuate(text: String): String {
                    return punctuator?.apply(text)?:text
                }
                val windows=PcmWindows();val evidence=SpeechEvidence();val audioGate=SpeechAudioGate()
                val boundary=SpeechTurnBoundary()
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
                val mic=AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,16000,AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,maxOf(min*2,6400));input=mic
                echoEffect=MicrophoneEcho(mic)
                synchronized(lock) { check(generation.get()==token&&!closed);check(mic.state==AudioRecord.STATE_INITIALIZED);recorder=mic;mic.startRecording() }
                check(mic.recordingState==AudioRecord.RECORDSTATE_RECORDING)
                val frames=java.util.concurrent.ArrayBlockingQueue<Frame>(40)
                val failure=java.util.concurrent.atomic.AtomicReference<Exception?>()
                val producer=Thread({
                    try {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
                        val samples=ShortArray(800)
                        while(generation.get()==token&&!closed) {
                            val n=mic.read(samples,0,samples.size,AudioRecord.READ_BLOCKING)
                            if(generation.get()!=token||closed)break
                            check(n>0);val at=android.os.SystemClock.elapsedRealtime()
                            val raw=FloatArray(n) { samples[it]/32768f }
                            if(!frames.offer(Frame(echo?.clean(raw,at)?:raw,at)))throw AudioQueueOverrun()
                        }
                    } catch(error: Exception) { failure.set(error) }
                },"zipformer-microphone").apply { isDaemon=true }
                capture=producer;producer.start();deliver(token,onReady)
                var previous="";var lastPartial=0L;var lastLevel=0L;var lastSignal=0L
                var audioSamples=0L;var quietSamples=0L;var lastFrameAt=android.os.SystemClock.elapsedRealtime()
                while(generation.get()==token&&!closed) {
                    failure.get()?.let { throw it }
                    val frame=frames.poll(100,java.util.concurrent.TimeUnit.MILLISECONDS)
                    if(frame==null) { check(producer.isAlive);check(android.os.SystemClock.elapsedRealtime()-lastFrameAt<3000);continue }
                    val now=android.os.SystemClock.elapsedRealtime();lastFrameAt=now
                    val samples=frame.samples
                    audioSamples+=samples.size;val audioNow=audioSamples*1000/16000
                    if(now-lastSignal>=500) { lastSignal=now;val health=CaptureSignal.inspect(samples,now-frame.capturedAt);deliver(token) { onSignal(health) } }
                    if(now-lastLevel>150) {
                        lastLevel=now
                        val level=(kotlin.math.sqrt(samples.sumOf { (it*it).toDouble() }/samples.size)*600).toInt().coerceIn(0,100)
                        deliver(token) { onLevel(level) }
                    }
                    var speech=false
                    windows.accept(samples) { window ->
                        vad.acceptWaveform(window);speech=speech||vad.isSpeechDetected()
                        while(!vad.empty())vad.pop()
                    }
                    // Reject digital silence even during a stale VAD hangover.
                    speech=speech&&SpeechTurnBoundary.hasSignal(samples)
                    boundary.observe(speech,audioNow)
                    deliver(token) { onSpeech(speech) }
                    evidence.observe(speech)
                    quietSamples=if(speech)0 else quietSamples+samples.size
                    val acceptedAudio=audioGate.accept(samples,speech)
                    if(acceptedAudio.isEmpty()) {
                        if(flush) { flush=false;publish(turns.flushConfirmed()) }
                        publish(turns.tick(audioNow));continue
                    }
                    acceptedAudio.forEach { session.acceptWaveform(it,16000) }
                    var decoded=false
                    while(engine.isReady(session)) { check(generation.get()==token&&!closed);engine.decode(session);decoded=true }
                    val hypothesis=engine.getResult(session).text.trim()
                    val text=boundary.accept(hypothesis,audioNow)
                    val stableWords=consensus.observe(text,decoded)
                    // Do not let decoder noise keep a silent turn alive indefinitely.
                    if(engine.isEndpoint(session)||boundary.mustEnd(audioNow)||flush) {
                        flush=false;previous=""
                        if(evidence.endTurn())clauses.update(text,audioNow,quietSamples*1000/16000,true,::punctuate)
                            .forEach { publish(turns.update(it.text,it.final,audioNow)) }
                        else clauses.reset()
                        engine.reset(session);audioGate.endTurn();boundary.reset();consensus.reset()
                    } else if(evidence.allowsPreview()&&text.isNotBlank()) {
                        // Preserve the recognizer context; only the translation cursor advances at a soft boundary.
                        clauses.updateStable(text,audioNow,quietSamples*1000/16000,false,stableWords,::punctuate).forEach { output ->
                            if(output.final||output.text!=previous&&now-lastPartial>=120) {
                                previous=if(output.final) "" else output.text;lastPartial=now
                                publish(turns.update(output.text,output.final,audioNow))
                            }
                        }
                    }
                    publish(turns.tick(audioNow))
                }
            } catch(_: GateUnavailable) { report(messages.getString(R.string.speech_gate_failed)) }
            catch(_: ModelCorrupt) { report(messages.getString(R.string.free_model_corrupt)) }
            catch(_: AudioQueueOverrun) { report(messages.getString(R.string.free_capture_overrun)) }
            catch(_: OutOfMemoryError) { report(messages.getString(R.string.speech_memory_error)) }
            catch(_: LinkageError) { report(messages.getString(R.string.ui_225)) }
            catch(_: Exception) { report(messages.getString(R.string.free_capture_failed)) }
            finally {
                synchronized(lock) { if(recorder===input)recorder=null;try { input?.stop() } catch(_: Exception) {};capture?.join(1000);input?.release() }
                stream?.release();decoder?.release();voiceDetector?.release();punctuator?.close();echoEffect?.close()
            }
        }
    }
    fun finishUtterance() { flush=true }
    fun stop() { generation.incrementAndGet();synchronized(lock) { try { recorder?.stop() } catch(_: Exception) {} };download?.disconnect() }
    fun close() { closed=true;stop();worker.shutdown() }
}
