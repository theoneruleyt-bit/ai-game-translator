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
    private val handler = Handler(Looper.getMainLooper())
    private val pending = java.util.ArrayDeque<String>()
    private var engine: TextToSpeech? = null
    private var currentId: String? = null
    private var sequence = 0L
    private var closed = false
    var ready = false
        private set
    var rate = 1.1f
        set(value) { field=value;engine?.setSpeechRate(value) }
    val count: Int get() = pending.size + if(currentId==null) 0 else 1
    init {
        engine = TextToSpeech(context) { code ->
            if(!closed) {
                val language = if(code==TextToSpeech.SUCCESS) engine?.setLanguage(Locale.forLanguageTag("tr-TR")) else null
                ready = language != null && language>=TextToSpeech.LANG_AVAILABLE
                if(ready) {
                    val best=engine?.voices?.filter { it.locale.language=="tr" && !it.isNetworkConnectionRequired }
                        ?.sortedWith(compareByDescending<android.speech.tts.Voice> { it.quality }.thenBy { it.latency })?.firstOrNull()
                    if(best!=null) engine?.voice=best
                    engine?.setSpeechRate(rate)
                } else onError("Türkçe ses paketi bulunamadı. Ses ayarlarından Türkçe ses yükle.")
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
    fun add(text: String, latestPending: Boolean = false): Boolean {
        if(!ready || closed || text.isBlank()) return false
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
        if(count+chunks.size>64) return false
        chunks.forEach { pending.addLast(it) };pump();onChange();return true
    }
    private fun pump() {
        if(currentId!=null || pending.isEmpty() || closed) return
        val text=pending.removeFirst();val id="voice-${++sequence}";currentId=id
        beforeSpeak()
        if(engine?.speak(text,TextToSpeech.QUEUE_ADD,null,id)!=TextToSpeech.SUCCESS) finish(id,true)
    }
    private fun finish(id: String?, failed: Boolean) {
        handler.post {
            if(!closed && currentId==id && id!=null) {
                currentId=null
                if(failed) onError("Ses motoru bir repliği okuyamadı. Çeviri ekranda korunuyor.")
                pump();onChange()
            }
        }
    }
    fun stop() {
        currentId=null;pending.clear();sequence++;engine?.stop();onChange()
    }
    fun close() { closed=true;pending.clear();currentId=null;engine?.stop();engine?.shutdown();engine=null }
}
