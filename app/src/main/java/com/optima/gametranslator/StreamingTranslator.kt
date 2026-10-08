package com.optima.gametranslator

import android.os.Handler
import android.os.Looper
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit

/** One microphone stream for the entire foreground session. No SpeechRecognizer restarts. */
class StreamingTranslator {
    private val worker=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private val generation=AtomicLong()
    private var recognizer: TranslationRecognizer?=null
    private var audio: AudioConfig?=null
    private var config: SpeechTranslationConfig?=null
    @Volatile private var closed=false
    private fun release() {
        val current=recognizer;recognizer=null
        try { current?.stopContinuousRecognitionAsync()?.get(10,TimeUnit.SECONDS) } catch (_: Exception) { }
        try { current?.close() } catch (_: Exception) { }
        try { audio?.close() } catch (_: Exception) { };audio=null
        try { config?.close() } catch (_: Exception) { };config=null
    }
    fun start(credentials: SpeechSettings.Credentials,onReady: ()->Unit,
              onText: (String,String,String,Boolean)->Unit,onError: (String)->Unit) {
        if(closed)return
        val token=generation.incrementAndGet()
        fun deliver(action: ()->Unit) { main.post { if(generation.get()==token&&!closed) action() } }
        worker.execute {
            release()
            if(generation.get()!=token)return@execute
            try {
                val settings=SpeechTranslationConfig.fromSubscription(credentials.key,credentials.region)
                config=settings
                settings.setSpeechRecognitionLanguage("en-US")
                settings.addTargetLanguage("tr")
                val input=AudioConfig.fromDefaultMicrophoneInput();audio=input
                val client=TranslationRecognizer(settings,input);recognizer=client
                client.recognizing.addEventListener { _,event ->
                    val value=event.result
                    val source=value.text.orEmpty();val target=value.translations["tr"].orEmpty();val id=value.resultId.orEmpty()
                    if(source.isNotBlank()) deliver { onText(id,source,target,false) }
                }
                client.recognized.addEventListener { _,event ->
                    val value=event.result
                    if(value.reason==ResultReason.TranslatedSpeech) {
                        val source=value.text.orEmpty();val target=value.translations["tr"].orEmpty();val id=value.resultId.orEmpty()
                        if(source.isNotBlank()&&target.isNotBlank()) deliver { onText(id,source,target,true) }
                    }
                }
                client.canceled.addEventListener { _,_ -> deliver {
                    onError("Canlı bağlantı kesildi. İnterneti, Azure bölgesini, anahtarı ve kotayı kontrol et. Yeniden başlatabilirsin.")
                } }
                client.sessionStopped.addEventListener { _,_ -> deliver { onError("Canlı ses oturumu sona erdi. Yeniden başlatabilirsin.") } }
                client.startContinuousRecognitionAsync().get(20,TimeUnit.SECONDS)
                deliver(onReady)
            } catch (_: Exception) {
                release()
                deliver { onError("Canlı ses bağlantısı kurulamadı. Mikrofon iznini, interneti ve Azure ayarlarını kontrol et.") }
            }
        }
    }
    fun stop() { if(!closed) { generation.incrementAndGet();worker.execute { release() } } }
    fun close() { if(!closed) { generation.incrementAndGet();closed=true;worker.execute { release() };worker.shutdown() } }
}
