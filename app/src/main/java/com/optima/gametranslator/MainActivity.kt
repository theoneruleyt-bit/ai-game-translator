package com.optima.gametranslator

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.widget.*
import android.util.Size
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.translate.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var preview: PreviewView
    private lateinit var source: TextView
    private lateinit var result: TextView
    private lateinit var status: TextView
    private lateinit var start: Button
    private lateinit var download: Button
    private lateinit var autoVoice: CheckBox
    private lateinit var subtitleRegion: CheckBox
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val translator = Translation.getClient(TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.TURKISH).build())
    private val gate = StabilityGate()
    private var provider: ProcessCameraProvider? = null
    private var tts: TextToSpeech? = null
    private var voiceReady = false
    private var modelReady = false
    private var alive = true
    private var live = false
    private var busy = false
    private var translating = false
    private var epoch = 0L
    private var lastFrame = 0L
    private var lastTranslation = ""
    private var frozen = false

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else status.text = "Kamera izni verilmedi. Başlat ile yeniden deneyebilirsin."
    }
    private val photo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            stopLive(); val token = epoch
            source.text = ""; result.text = ""; lastTranslation = ""
            status.text = "Fotoğraf / ekran görüntüsü okunuyor…"
            try {
                recognizer.process(InputImage.fromFilePath(this, uri))
                    .addOnSuccessListener { text ->
                        if (valid(token)) {
                            source.text = text.text
                            if (text.text.isBlank()) status.text = "Görüntüde okunabilir metin bulunamadı."
                            else translate(text.text, token, false)
                        }
                    }.addOnFailureListener { if (valid(token)) status.text = "Görüntü okunamadı. Başka bir fotoğraf dene." }
            } catch (_: Exception) { status.text = "Görüntü açılamadı." }
        }
    }
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(24, 32, 24, 24)
            setBackgroundColor(Color.rgb(16,24,39))
        }
        fun label(text: String, size: Float) = TextView(this).apply {
            this.text=text; textSize=size; setTextColor(Color.WHITE); setPadding(0,8,0,8)
        }
        content.addView(label("AI Game Translator",26f))
        content.addView(label("Kameradan canlı İngilizce → Türkçe",15f))
        status=label("Önce çeviri modelini hazırla. İlk indirme internet gerektirir.",15f);content.addView(status)
        preview=PreviewView(this).apply { scaleType=PreviewView.ScaleType.FIT_CENTER }
        content.addView(preview,LinearLayout.LayoutParams(-1,(240*resources.displayMetrics.density).toInt()))
        download=Button(this).apply { text="EN → TR modelini hazırla";setOnClickListener { prepareModel() } }
        content.addView(download)
        start=Button(this).apply { text="Canlı kamerayı başlat";isEnabled=false;setOnClickListener {
            if (ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) startCamera()
            else permission.launch(Manifest.permission.CAMERA)
        } };content.addView(start)
        content.addView(Button(this).apply { text="Durdur";setOnClickListener { stopLive();status.text="Durduruldu." } })
        subtitleRegion=CheckBox(this).apply {
            text="Canlı mod: görüntünün alt %45'indeki yazıları oku";isChecked=true;setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _,_ -> epoch++;gate.reset();tts?.stop();translating=false }
        };content.addView(subtitleRegion)
        autoVoice=CheckBox(this).apply {
            text="Türkçe çeviriyi otomatik sesli oku";isChecked=true;setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _,checked -> if(!checked) tts?.stop() }
        };content.addView(autoVoice)
        content.addView(Button(this).apply { text="Kareyi dondur ve çevir";setOnClickListener {
            if (live) { frozen=true;gate.reset();status.text="Bir sonraki kare okunacak…" }
            else status.text="Önce canlı kamerayı başlat."
        } })
        content.addView(Button(this).apply { text="Fotoğraf / ekran görüntüsü seç";setOnClickListener {
            stopLive();photo.launch("image/*")
        } })
        source=label("İngilizce metin burada görünecek.",16f);content.addView(source)
        result=label("Türkçe çeviri burada görünecek.",21f);content.addView(result)
        content.addView(Button(this).apply { text="Çeviriyi tekrar oku";setOnClickListener { speak(lastTranslation) } })
        content.addView(label("TV/monitörde İngilizce altyazıyı aç. Telefonu sabit tut, yazıları net ve büyük kadraja al. Bu sürüm oyun sesini dinlemez. Galeri modunda görüntünün tüm metni okunur.",14f))
        setContentView(ScrollView(this).apply { addView(content) })
        tts=TextToSpeech(this) { code ->
            if(alive) {
                val ready=if(code==TextToSpeech.SUCCESS) tts?.setLanguage(Locale.forLanguageTag("tr-TR")) else null
                voiceReady=ready != null && ready>=TextToSpeech.LANG_AVAILABLE
                if(!voiceReady) status.text="Türkçe ses bulunamadı. Android metinden konuşma ayarlarından Türkçe ses yükle; metin çevirisi kullanılabilir."
            }
        }
    }
    private fun valid(token: Long) = alive && token==epoch
    private fun prepareModel() {
        download.isEnabled=false;status.text="Çeviri modeli hazırlanıyor / indiriliyor…"
        translator.downloadModelIfNeeded().addOnSuccessListener {
            if(alive) { modelReady=true;start.isEnabled=true;download.isEnabled=true;status.text="Model hazır. Kamerayı başlatabilirsin." }
        }.addOnFailureListener {
            if(alive) { download.isEnabled=true;status.text="Model indirilemedi. İnterneti kontrol edip tekrar dene." }
        }
    }
    @androidx.annotation.OptIn(markerClass = [androidx.camera.core.ExperimentalGetImage::class])
    private fun startCamera() {
        if(!modelReady) { status.text="Önce EN → TR modelini hazırla.";return }
        stopLive();live=true;start.isEnabled=false;val token=epoch
        status.text="Kamera açılıyor…"
        val future=ProcessCameraProvider.getInstance(this)
        future.addListener({
            if(valid(token)&&live) {
                try {
                    provider=future.get()
                    val view=Preview.Builder().build().also { it.surfaceProvider=preview.surfaceProvider }
                    val analysis=ImageAnalysis.Builder()
                        .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy(Size(1280,720),ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { proxy ->
                        val now=SystemClock.elapsedRealtime()
                        if(!live || busy || now-lastFrame<450) proxy.close()
                        else {
                            val media=proxy.image
                            if(media==null) proxy.close() else {
                                busy=true;lastFrame=now;val currentEpoch=epoch
                                val rotation=proxy.imageInfo.rotationDegrees
                                val height=if(rotation==90 || rotation==270) proxy.width else proxy.height
                                val scanFrozen=frozen
                                recognizer.process(InputImage.fromMediaImage(media,rotation))
                                    .addOnSuccessListener { text ->
                                        if(valid(currentEpoch)&&live) {
                                            val selected=if(subtitleRegion.isChecked && !scanFrozen)
                                                text.textBlocks.filter { (it.boundingBox?.centerY() ?: 0)>=height*0.55 }.joinToString("\n") { it.text }
                                                else text.text
                                            if(scanFrozen) {
                                                frozen=false;stopLive();source.text=selected
                                                if(selected.isBlank()) status.text="Bu karede metin bulunamadı."
                                                else translate(selected,epoch,false)
                                            } else {
                                                val stable=gate.observe(selected,SystemClock.elapsedRealtime())
                                                if(stable!=null&&!translating) { source.text=stable;translate(stable,currentEpoch,true) }
                                            }
                                        }
                                    }.addOnFailureListener { if(valid(currentEpoch)) status.text="Kare okunamadı. Işık/netliği kontrol et." }
                                    .addOnCompleteListener { proxy.close();busy=false }
                            }
                        }
                    }
                    provider?.unbindAll()
                    provider?.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,view,analysis)
                    status.text="Canlı çeviri açık • Kamerayı altyazıya yönelt."
                } catch (_: Exception) { stopLive();status.text="Arka kamera açılamadı. İzni/cihazı kontrol et." }
            }
        },ContextCompat.getMainExecutor(this))
    }
    private fun translate(text: String, token: Long, fromLive: Boolean) {
        if(!modelReady) { status.text="Metin okundu. Modeli hazırla ve görüntüyü yeniden seç.";return }
        translating=true;status.text="Türkçeye çevriliyor…"
        translator.translate(text).addOnSuccessListener { translated ->
            if(valid(token) && (!fromLive || (live&&gate.current==text))) {
                lastTranslation=translated;result.text=translated
                if(fromLive) gate.commit(text)
                status.text=if(fromLive) "Canlı çeviri açık" else "Görüntü çevirisi hazır"
                if(autoVoice.isChecked) speak(translated)
            }
        }.addOnFailureListener { if(valid(token)) status.text="Çeviri başarısız. Tekrar dene." }
         .addOnCompleteListener { if(valid(token)) translating=false }
    }
    private fun speak(text: String) {
        if(text.isBlank()) return
        if(!voiceReady) { status.text="Türkçe ses paketi hazır değil. Android metinden konuşma ayarlarını kontrol et.";return }
        // Flush prevents accumulating old dialogue in the speech queue.
        tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"translation")
    }
    private fun stopLive() {
        epoch++;live=false;frozen=false;translating=false;gate.reset()
        provider?.unbindAll();tts?.stop()
        if(::start.isInitialized) start.isEnabled=modelReady
    }
    override fun onStop() { stopLive();super.onStop() }
    override fun onDestroy() {
        alive=false;epoch++;provider?.unbindAll();tts?.stop();tts?.shutdown()
        recognizer.close();translator.close();super.onDestroy()
    }
}
