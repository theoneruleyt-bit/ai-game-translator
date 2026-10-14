package com.optima.gametranslator

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** One utterance at a time. New translations never flush a running utterance. */
class VoicePlayer(context: Context, private val onChange: () -> Unit,
                  private val beforeSpeak: () -> Unit, private val onError: (String) -> Unit) {
    private val messages=context.resources
    private val cache=context.cacheDir
    var professional: ProfessionalGateway?=null
    private val networkWorker=java.util.concurrent.Executors.newSingleThreadExecutor()
    private var cloudPlayer: android.media.MediaPlayer?=null
    private var cloudFile: java.io.File?=null
    private val handler = Handler(Looper.getMainLooper())
    private data class Job(val text: String,val voiceName: String?,val rate: Float,val pitch: Float,val createdAt: Long,val sourceMillis: Long)
    private val tempo=AdaptiveTempo()
    private val playbackDeadline=PlaybackDeadline()
    private var watchdog: Runnable?=null
    private var startedAt=0L
    private var playingJob: Job?=null
    private var playingRate=1f
    val currentRate: Float get()=playingRate
    val estimatedWaitingMillis: Long get()=pending.sumOf { tempo.estimateMillis(it.text) }
    private val pending = java.util.ArrayDeque<Job>()
    private var defaultVoiceName: String?=null
    private var targetLanguage="tr"
    private var engine: TextToSpeech? = null
    private var currentId: String? = null
    private var sequence = 0L
    private var closed = false
    var networkAllowed=false
    var adaptiveSpeed=true
    var lastFailure: String?=null
        private set
    fun currentVoiceName(): String?=defaultVoiceName
    var ready = false
        private set
    var rate = 1.1f
        set(value) { field=value.coerceIn(.7f,1.5f) }
    var pitch=1f
        set(value) { field=value.coerceIn(.85f,1.15f) }
    fun availableVoices(includeNetwork: Boolean): List<android.speech.tts.Voice> = engine?.voices
        ?.filter { it.locale.language==targetLanguage && (includeNetwork || !it.isNetworkConnectionRequired) && !it.features.contains("notInstalled") }
        ?.sortedWith(compareBy<android.speech.tts.Voice> { it.isNetworkConnectionRequired }
            .thenByDescending { it.quality }.thenBy { it.name }).orEmpty()
    fun setTargetLanguage(language: String) {
        if(targetLanguage==language&&ready)return
        stop();targetLanguage=language;defaultVoiceName=null
        val code=engine?.setLanguage(Locale.forLanguageTag(if(language=="tr") "tr-TR" else "en-US"))
        ready=code!=null&&code>=TextToSpeech.LANG_AVAILABLE
        if(ready) {
            availableVoices(false).firstOrNull()?.let { engine?.voice=it }
            defaultVoiceName=engine?.voice?.name
        }
        onChange()
    }
    fun refreshLanguage() { if(!ready&&!closed)setTargetLanguage(targetLanguage) }
    fun selectVoice(selected: android.speech.tts.Voice): Boolean {
        if(selected.locale.language!=targetLanguage)return false
        stop()
        val selectedOk=engine?.setVoice(selected)==TextToSpeech.SUCCESS
        if(selectedOk)defaultVoiceName=selected.name
        return selectedOk
    }
    val count: Int get() = pending.size + if(currentId==null) 0 else 1
    init {
        engine = TextToSpeech(context) { code ->
            handler.post { if(!closed) {
                val language = if(code==TextToSpeech.SUCCESS) engine?.setLanguage(Locale.forLanguageTag(if(targetLanguage=="tr") "tr-TR" else "en-US")) else null
                ready = language != null && language>=TextToSpeech.LANG_AVAILABLE
                if(ready) {
                    val best=availableVoices(false).firstOrNull()
                    if(best!=null) engine?.voice=best
                    defaultVoiceName=engine?.voice?.name
                    engine?.setSpeechRate(rate);engine?.setPitch(pitch)
                } else onError(messages.getString(R.string.ui_227))
                if(ready)engine?.setAudioAttributes(android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
                onChange()
            } }
        }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { handler.post { if(currentId==id&&id!=null) {
                startedAt=android.os.SystemClock.elapsedRealtime();playbackDeadline.started(id,startedAt);onChange()
            } } }
            override fun onDone(id: String?) { finish(id,false) }
            override fun onStop(id: String?, interrupted: Boolean) { finish(id,true) }
            @Deprecated("Android TTS callback")
            override fun onError(id: String?) { finish(id,true) }
            override fun onError(id: String?, errorCode: Int) { finish(id,true,messages.getString(R.string.voice_error_code,errorCode.toString())) }
        })
    }
    fun add(text: String, latestPending: Boolean = false, voiceName: String?=null, selectedPitch: Float=pitch,selectedRate: Float=rate,sourceMillis: Long=0): Boolean {
        lastFailure=null
        if((!ready&&professional?.configured!=true) || closed || text.isBlank()) { lastFailure=messages.getString(R.string.ui_228);return false }
        val usable=availableVoices(networkAllowed)
        val selected=usable.firstOrNull { it.name==(voiceName ?: defaultVoiceName) }?.name
            ?: usable.firstOrNull()?.name
        if(selected==null&&engine?.voice?.isNetworkConnectionRequired==true&&!networkAllowed) {
            lastFailure=messages.getString(R.string.ui_229);return false
        }
        val limit = TextToSpeech.getMaxSpeechInputLength().coerceAtLeast(100).let { if(professional?.configured==true) minOf(it,900) else minOf(it,260) }
        val chunks = mutableListOf<String>()
        var rest=text.trim()
        while(rest.length>limit) {
            val split=rest.lastIndexOf(' ',limit-1).takeIf { it>limit/2 } ?: (limit-1)
            chunks.add(rest.substring(0,split));rest=rest.substring(split).trimStart()
        }
        if(rest.isNotEmpty()) chunks.add(rest)
        // Keep the current spoken utterance intact, but replace waiting camera
        // speech with the latest translation so it cannot lag dozens of lines.
        if(latestPending) pending.clear()
        if(count+chunks.size>64) { lastFailure=messages.getString(R.string.ui_186);return false }
        chunks.forEach { pending.addLast(Job(it,selected,selectedRate,selectedPitch,android.os.SystemClock.elapsedRealtime(),sourceMillis/chunks.size)) };pump();onChange();return true
    }
    private fun pump() {
        if(currentId!=null || pending.isEmpty() || closed) return
        val job=pending.removeFirst();val id="voice-${++sequence}";currentId=id
        playingJob=job;startedAt=0
        playingRate=tempo.choose(job.text,job.rate,estimatedWaitingMillis,
            android.os.SystemClock.elapsedRealtime()-job.createdAt,job.sourceMillis,adaptiveSpeed)
        val service=professional
        if(service?.configured==true) {
            val language=targetLanguage;val rateNow=playingRate
            networkWorker.execute {
                var file: java.io.File?=null
                try {
                    val audio=service.synthesize(job.text,language,rateNow)
                    file=java.io.File.createTempFile("speech-",".mp3",cache).apply { writeBytes(audio) }
                    val output=requireNotNull(file)
                    handler.post {
                        if(closed||currentId!=id) { output.delete();return@post }
                        try {
                            val player=android.media.MediaPlayer();cloudPlayer=player;cloudFile=output
                            player.setAudioAttributes(android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build())
                            player.setDataSource(output.absolutePath)
                            player.setOnCompletionListener { if(currentId==id&&!closed) { releaseCloud();finish(id,false) } else { player.release();output.delete() } }
                            player.setOnErrorListener { _,_,_ -> if(currentId==id&&!closed) { releaseCloud();finish(id,true) } else { player.release();output.delete() };true }
                            player.setOnPreparedListener { if(currentId==id&&!closed) { beforeSpeak();startedAt=android.os.SystemClock.elapsedRealtime();it.start() } else { player.release();output.delete() } }
                            player.prepareAsync()
                        } catch(_: Exception) { releaseCloud();onError(messages.getString(R.string.professional_fallback));speakLocal(job,id) }
                    }
                } catch(_: Exception) {
                    file?.delete()
                    handler.post { if(!closed&&currentId==id) { onError(messages.getString(R.string.professional_fallback));speakLocal(job,id) } }
                }
            }
            return
        }
        speakLocal(job,id)
    }
    private fun cancelWatchdog() {
        watchdog?.let { handler.removeCallbacks(it) };watchdog=null;playbackDeadline.clear()
    }
    private fun watchPlayback(job: Job,id: String) {
        cancelWatchdog()
        playbackDeadline.begin(id,android.os.SystemClock.elapsedRealtime(),tempo.estimateMillis(job.text))
        val check=object : Runnable {
            override fun run() {
                if(closed||currentId!=id)return
                if(playbackDeadline.expired(id,android.os.SystemClock.elapsedRealtime())) {
                    lastFailure=messages.getString(R.string.voice_timeout)
                    // Invalidating the job happens before stop callbacks are dispatched.
                    finish(id,true);engine?.stop()
                } else handler.postDelayed(this,1000)
            }
        }
        watchdog=check;handler.postDelayed(check,1000)
    }
    private fun speakLocal(job: Job,id: String) {
        if(!ready) { finish(id,true);return }
        val selected=engine?.voices?.firstOrNull { it.name==job.voiceName }
        if(selected!=null) {
            if(selected.isNetworkConnectionRequired&&!networkAllowed) { finish(id,true);return }
            if(engine?.setVoice(selected)!=TextToSpeech.SUCCESS) { finish(id,true);return }
        } else if(job.voiceName!=null) { finish(id,true);return }
        engine?.setSpeechRate(playingRate);engine?.setPitch(job.pitch.coerceIn(.85f,1.15f))
        beforeSpeak()
        watchPlayback(job,id)
        val params=android.os.Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,1f) }
        if(engine?.speak(job.text,TextToSpeech.QUEUE_ADD,params,id)!=TextToSpeech.SUCCESS) finish(id,true)
    }
    private fun finish(id: String?, failed: Boolean,reason: String?=null) {
        handler.post {
            if(!closed && currentId==id && id!=null) {
                if(!failed&&startedAt>0) playingJob?.let { tempo.completed(it.text,playingRate,android.os.SystemClock.elapsedRealtime()-startedAt) }
                cancelWatchdog();playingJob=null;startedAt=0;currentId=null
                if(failed) {
                    lastFailure=reason ?: lastFailure ?: messages.getString(R.string.ui_230)
                    onError(requireNotNull(lastFailure))
                }
                pump();onChange()
            }
        }
    }
    private fun releaseCloud() {
        cloudPlayer?.release();cloudPlayer=null;cloudFile?.delete();cloudFile=null
    }
    fun clearWaiting() { pending.clear();onChange() }
    fun stop() {
        cancelWatchdog();releaseCloud();currentId=null;playingJob=null;startedAt=0;tempo.reset();pending.clear();sequence++;engine?.stop();onChange()
    }
    fun close() { closed=true;cancelWatchdog();releaseCloud();networkWorker.shutdown();pending.clear();currentId=null;engine?.stop();engine?.shutdown();engine=null }
}
