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
    private val handler = Handler(Looper.getMainLooper())
    private data class Job(val text: String,val voiceName: String?,val rate: Float,val pitch: Float)
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
        set(value) { field=value;engine?.setSpeechRate(value) }
    var pitch=1f
        set(value) { field=value;engine?.setPitch(value) }
    fun availableVoices(includeNetwork: Boolean): List<android.speech.tts.Voice> = engine?.voices
        ?.filter { it.locale.language==targetLanguage && (includeNetwork || !it.isNetworkConnectionRequired) && !it.features.contains("notInstalled") }
        ?.sortedWith(compareBy<android.speech.tts.Voice> { it.isNetworkConnectionRequired }
            .thenByDescending { it.quality }.thenBy { it.name }).orEmpty()
    fun setTargetLanguage(language: String) {
        if(targetLanguage==language)return
        stop();targetLanguage=language;defaultVoiceName=null
        val code=engine?.setLanguage(Locale.forLanguageTag(if(language=="tr") "tr-TR" else "en-US"))
        ready=code!=null&&code>=TextToSpeech.LANG_AVAILABLE
        if(ready) {
            availableVoices(false).firstOrNull()?.let { engine?.voice=it }
            defaultVoiceName=engine?.voice?.name
        }
        onChange()
    }
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
            if(!closed) {
                val language = if(code==TextToSpeech.SUCCESS) engine?.setLanguage(Locale.forLanguageTag(if(targetLanguage=="tr") "tr-TR" else "en-US")) else null
                ready = language != null && language>=TextToSpeech.LANG_AVAILABLE
                if(ready) {
                    val best=availableVoices(false).firstOrNull()
                    if(best!=null) engine?.voice=best
                    defaultVoiceName=engine?.voice?.name
                    engine?.setSpeechRate(rate);engine?.setPitch(pitch)
                } else onError(messages.getString(R.string.ui_227))
                onChange()
            }
        }
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { }
            override fun onDone(id: String?) { finish(id,false) }
            @Deprecated("Android TTS callback")
            override fun onError(id: String?) { finish(id,true) }
            override fun onError(id: String?, errorCode: Int) { finish(id,true) }
        })
    }
    fun add(text: String, latestPending: Boolean = false, voiceName: String?=null, selectedPitch: Float=pitch,selectedRate: Float=rate): Boolean {
        lastFailure=null
        if(!ready || closed || text.isBlank()) { lastFailure=messages.getString(R.string.ui_228);return false }
        val selected=voiceName ?: defaultVoiceName
        val requestedVoice=engine?.voices?.firstOrNull { it.name==selected }
        if(requestedVoice!=null&&requestedVoice.locale.language!=targetLanguage) { lastFailure=messages.getString(R.string.ui_228);return false }
        if(requestedVoice?.isNetworkConnectionRequired==true&&!networkAllowed) { lastFailure=messages.getString(R.string.ui_229);return false }
        val limit = TextToSpeech.getMaxSpeechInputLength().coerceAtLeast(100)
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
        chunks.forEach { pending.addLast(Job(it,selected,selectedRate,selectedPitch)) };pump();onChange();return true
    }
    private fun pump() {
        if(currentId!=null || pending.isEmpty() || closed) return
        val job=pending.removeFirst();val id="voice-${++sequence}";currentId=id
        val selected=engine?.voices?.firstOrNull { it.name==job.voiceName }
        if(selected!=null) {
            if(selected.isNetworkConnectionRequired&&!networkAllowed) { finish(id,true);return }
            if(engine?.setVoice(selected)!=TextToSpeech.SUCCESS) { finish(id,true);return }
        } else if(job.voiceName!=null) { finish(id,true);return }
        engine?.setSpeechRate(SpeakerStyle.catchUpRate(job.rate,pending.size,adaptiveSpeed));engine?.setPitch(job.pitch)
        beforeSpeak()
        if(engine?.speak(job.text,TextToSpeech.QUEUE_ADD,null,id)!=TextToSpeech.SUCCESS) finish(id,true)
    }
    private fun finish(id: String?, failed: Boolean) {
        handler.post {
            if(!closed && currentId==id && id!=null) {
                currentId=null
                if(failed) onError(messages.getString(R.string.ui_230))
                pump();onChange()
            }
        }
    }
    fun clearWaiting() { pending.clear();onChange() }
    fun stop() {
        currentId=null;pending.clear();sequence++;engine?.stop();onChange()
    }
    fun close() { closed=true;pending.clear();currentId=null;engine?.stop();engine?.shutdown();engine=null }
}
