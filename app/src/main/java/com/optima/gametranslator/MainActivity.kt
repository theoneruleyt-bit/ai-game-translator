package com.optima.gametranslator

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.RectF
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.speech.*
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.translate.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : ComponentActivity() {
    private enum class Mode { CAMERA, MIC, IMAGE }
    private var mode=Mode.CAMERA
    private val ink=Color.rgb(11,17,29)
    private val panel=Color.rgb(23,34,53)
    private val mint=Color.rgb(100,235,204)
    private val muted=Color.rgb(170,185,209)
    private lateinit var status: TextView
    private lateinit var metrics: TextView
    private lateinit var source: TextView
    private lateinit var result: TextView
    private lateinit var history: TextView
    private lateinit var preview: PreviewView
    private lateinit var subtitleArea: SubtitleAreaView
    private val subtitleFilter=SubtitleFilter()
    private lateinit var previewBox: LinearLayout
    private lateinit var micHero: LinearLayout
    private lateinit var localSpeech: CheckBox
    private lateinit var signal: ProgressBar
    private lateinit var signalLabel: TextView
    private lateinit var voiceChoice: Button
    private var allowNetworkVoice=false
    private var finalRequested=false
    private var emptySpeechRounds=0
    private lateinit var hint: TextView
    private lateinit var start: Button
    private lateinit var prepare: Button
    private lateinit var autoVoice: CheckBox
    private lateinit var region: CheckBox
    private lateinit var headphones: CheckBox
    private lateinit var freeze: Button
    private lateinit var tabs: List<Button>
    private lateinit var voice: VoicePlayer
    private val handler=Handler(Looper.getMainLooper())
    private val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val translator=Translation.getClient(TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.TURKISH).build())
    private val gate=StabilityGate()
    private val speakers=SpeakerRouter()
    private var selectedSpeaker=0
    private var lastSpeaker=0
    private var automaticSubtitleNames=true
    private lateinit var speakerChoice: Button
    private val profilePrefs by lazy { getSharedPreferences("speaker_profiles",MODE_PRIVATE) }
    private val waiting=LineQueue()
    private val recent=RecentLines()
    private var speechDeadline: Runnable?=null
    private var speechHardDeadline: Runnable?=null
    private var partialText=""
    private var provider: ProcessCameraProvider?=null
    private var speech: SpeechRecognizer?=null
    private var active=false
    private var alive=true
    private var modelReady=false
    private var busy=false
    private var translating=false
    private var epoch=0L
    private var lastFrame=0L
    private var frozen=false
    private var lastTranslation=""
    private var rejected=0
    private var micRound=0L
    private var listening=false
    private var errors=0
    private var preferSystemSpeech=false
    private var completed=0
    private var lastLag=0L
    private val log=java.util.ArrayDeque<String>()
    private val restart=Runnable { listenOnce() }
    private val cameraPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if(it) startCamera() else message("Kamera izni verilmedi. Tekrar başlatabilirsin.")
    }
    private val microphonePermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if(it) startMicrophone() else message("Mikrofon izni verilmedi. Kamera modu kullanılabilir.")
    }
    private val speechTest=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        if(response.resultCode==RESULT_OK) {
            val text=response.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            source.text=text
            if(text.isNotBlank()) accept(text,SystemClock.elapsedRealtime())
            else message("Sistem testi metin döndürmedi.")
        } else message("Sistem konuşma testi iptal edildi veya sonuç alınamadı.")
    }
    private val photo=registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null) {
            stopSession();val token=epoch;source.text="Görsel okunuyor…";result.text="Yeni görsel çevriliyor…";lastTranslation="";message("Görseldeki metin okunuyor…")
            try {
                recognizer.process(InputImage.fromFilePath(this,uri)).addOnSuccessListener {
                    if(valid(token)) {
                        source.text=it.text
                        if(it.text.isBlank()) message("Yazı bulunamadı. Daha net bir görsel seç.")
                        else accept(it.text,SystemClock.elapsedRealtime())
                    }
                }.addOnFailureListener { if(valid(token)) message("Görsel okunamadı. Başka bir görsel dene.") }
            } catch (_: Exception) { message("Görsel açılamadı.") }
        }
    }
    private fun dp(value: Int)=(value*resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Float=20f)=GradientDrawable().apply {
        setColor(color);cornerRadius=dp(radius.toInt()).toFloat()
    }
    private fun label(text: String,size: Float=15f,color: Int=Color.WHITE)=TextView(this).apply {
        this.text=text;textSize=size;setTextColor(color);setPadding(0,dp(5),0,dp(5))
    }
    private fun button(text: String, primary: Boolean=false, action: () -> Unit)=Button(this).apply {
        this.text=text;isAllCaps=false;textSize=15f;minHeight=dp(48)
        setTextColor(if(primary) ink else Color.WHITE)
        background=rounded(if(primary) mint else panel,14f)
        setPadding(dp(12),dp(8),dp(12),dp(8));setOnClickListener { action() }
    }
    private fun vertical()=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
    private fun card(parent: LinearLayout): LinearLayout {
        val v=vertical().apply { background=rounded(panel);setPadding(dp(18),dp(16),dp(18),dp(16)) }
        parent.addView(v,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(14) });return v
    }
    private fun row(parent: LinearLayout, buttons: List<Button>) {
        val line=LinearLayout(this)
        buttons.forEachIndexed { index,b -> line.addView(b,LinearLayout.LayoutParams(0,-2,1f).apply { if(index>0) leftMargin=dp(8) }) }
        parent.addView(line,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(12) })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speakers.restore((1..8).mapNotNull { slot -> profilePrefs.getString("speaker_$slot",null)?.let { it to slot } }.toMap())
        window.statusBarColor=ink;window.navigationBarColor=ink
        val root=vertical().apply { setPadding(dp(20),dp(24),dp(20),dp(28));background=GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(ink,Color.rgb(17,30,47))) }
        val brand=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val logo=ImageView(this).apply { setImageResource(com.optima.gametranslator.R.drawable.brand_icon) }
        brand.addView(logo,LinearLayout.LayoutParams(dp(46),dp(46)).apply { rightMargin=dp(12) })
        val titles=vertical();titles.addView(label("AI Game Translator",23f).apply { setTypeface(null,Typeface.BOLD) })
        titles.addView(label("Oyun ve video • Canlı çeviri",13f,muted));brand.addView(titles)
        root.addView(brand,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(18) })
        root.addView(label("İNGİLİZCE   →   TÜRKÇE",12f,mint))
        tabs=listOf(button("Kamera") { selectMode(Mode.CAMERA) },button("Dinle") { selectMode(Mode.MIC) },button("Görsel") { selectMode(Mode.IMAGE) })
        row(root,tabs)
        previewBox=card(root)
        preview=PreviewView(this).apply {
            scaleType=PreviewView.ScaleType.FIT_CENTER
            implementationMode=PreviewView.ImplementationMode.COMPATIBLE
        }
        subtitleArea=SubtitleAreaView(this).apply { onAreaChanged={
            if(active&&mode==Mode.CAMERA) {
                epoch++;translating=false;waiting.clear();gate.reset();recent.clear();subtitleFilter.clear();voice.stop()
                message("Yeni altyazı alanı seçildi. Yalnızca çerçevenin içi okunuyor.")
            }
        } }
        val cameraFrame=FrameLayout(this)
        cameraFrame.addView(preview,FrameLayout.LayoutParams(-1,-1))
        cameraFrame.addView(subtitleArea,FrameLayout.LayoutParams(-1,-1))
        previewBox.addView(cameraFrame,LinearLayout.LayoutParams(-1,dp(250)))
        previewBox.addView(button("Altyazı alanını sıfırla") { subtitleArea.resetArea() })
        previewBox.addView(label("CANLI KAMERA",11f,mint))
        micHero=card(root)
        micHero.addView(label("▂  ▅  ▇  ▄  ▆  ▂",34f,mint).apply { gravity=Gravity.CENTER })
        micHero.addView(label("Oyunu veya videoyu Türkçe takip et",21f).apply { gravity=Gravity.CENTER })
        micHero.addView(label("Telefonu oyun sesinin duyulabileceği yere koy. Bu mod konuşma bitişlerinde çevirir; tanıma cihazına göre değişir.",14f,muted))
        signalLabel=label("Mikrofon bekliyor",13f,mint);micHero.addView(signalLabel)
        signal=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply { max=100 }
        micHero.addView(signal,LinearLayout.LayoutParams(-1,dp(10)))
        localSpeech=CheckBox(this).apply {
            text="Cihazda konuşma tanımayı dene";setTextColor(muted);isChecked=false
            setOnCheckedChangeListener { _,_ -> if(active&&mode==Mode.MIC) { stopSession();message("Tanıma seçimi değişti. Dinlemeyi yeniden başlat.") } }
        };micHero.addView(localSpeech)
        micHero.addView(button("Konuşmayı bitir ve çevir") {
            if(active&&mode==Mode.MIC&&listening) requestSpeechFinal(epoch,micRound)
            else message("Önce dinlemeyi başlat.")
        })
        micHero.addView(button("Kısa konuşma testi") {
            if(!modelReady) { message("Önce çeviri modelini hazırla.") }
            else {
                stopSession()
                AlertDialog.Builder(this).setTitle("Sistem konuşma testi")
                    .setMessage("Android konuşma penceresi açılacak. İngilizce kısa bir cümle söyle. Sistem servisi internet kullanabilir.")
                    .setPositiveButton("Testi aç") { _,_ ->
                        try { speechTest.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US")
                            putExtra(RecognizerIntent.EXTRA_PROMPT,"İngilizce kısa bir cümle söyle")
                        }) } catch (_: Exception) { message("Sistem konuşma penceresi bulunamadı. Konuşma servisini telefon ayarlarından kontrol et.") }
                    }.setNegativeButton("Vazgeç",null).show()
            }
        })
        hint=label("",14f,muted);root.addView(hint)
        val controls=card(root)
        status=label("Çeviri modelini bir kez hazırla, sonra başlat.",15f);controls.addView(status)
        prepare=button("Çeviri modelini hazırla") { prepareModel() };controls.addView(prepare)
        controls.addView(label("",3f))
        start=button("Kamerayı başlat",true) { primaryAction() }
        row(controls,listOf(start,button("Durdur") { stopSession();message("Oturum durduruldu.") }))
        metrics=label("",12f,mint);controls.addView(metrics)
        autoVoice=CheckBox(this).apply {
            text="Türkçe sesli oku";setTextColor(Color.WHITE);isChecked=true
            setOnCheckedChangeListener { _,checked -> if(!checked) voice.stop() }
        };controls.addView(autoVoice)
        region=CheckBox(this).apply {
            text="Yalnızca seçili altyazı alanını oku";setTextColor(muted);isChecked=true
            setOnCheckedChangeListener { _,checked ->
                subtitleArea.visibility=if(checked) View.VISIBLE else View.GONE
                if(active) { stopSession();message("Okuma alanı değişti. Kamerayı yeniden başlat.") }
            }
        };controls.addView(region)
        headphones=CheckBox(this).apply {
            text="Türkçe sesi kulaklıkla dinliyorum";setTextColor(muted);isChecked=false
            setOnCheckedChangeListener { _,checked ->
                if(active&&mode==Mode.MIC) {
                    if(!checked&&voice.count>0) pauseMicrophone() else scheduleListening()
                }
            }
        };controls.addView(headphones)
        controls.addView(label("Okuma hızı",13f,muted))
        controls.addView(SeekBar(this).apply {
            max=6;progress=2
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?,progress: Int,fromUser: Boolean) { if(::voice.isInitialized) voice.rate=0.9f+progress*0.1f }
                override fun onStartTrackingTouch(bar: SeekBar?) { }
                override fun onStopTrackingTouch(bar: SeekBar?) { }
            })
        })
        voiceChoice=button("Türkçe ses seç") { chooseVoice() };controls.addView(voiceChoice)
        controls.addView(CheckBox(this).apply {
            text="İnternet gerektiren sesleri de göster";setTextColor(muted);isChecked=false
            setOnCheckedChangeListener { box,checked ->
                if(checked) AlertDialog.Builder(this@MainActivity).setTitle("İnternet sesi")
                    .setMessage("Seçtiğin ses motoru Türkçe çeviri metnini kendi hizmetine gönderebilir. İnternet bağlantısı gerekir; sesin daha doğal olacağı garanti değildir.")
                    .setPositiveButton("Göster") { _,_ -> allowNetworkVoice=true;voice.networkAllowed=true }
                    .setNegativeButton("Vazgeç") { _,_ -> box.isChecked=false }.show()
                else {
                    allowNetworkVoice=false
                    if(::voice.isInitialized)voice.networkAllowed=false
                    if(::voice.isInitialized) voice.availableVoices(false).firstOrNull()?.let { voice.selectVoice(it) }
                }
            }
        })
        controls.addView(label("Ses tonu • gerçek aksan değiştirmez",13f,muted))
        controls.addView(SeekBar(this).apply {
            max=6;progress=3
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?,progress: Int,fromUser: Boolean) { if(::voice.isInitialized) voice.pitch=.85f+progress*.05f }
                override fun onStartTrackingTouch(bar: SeekBar?) { }
                override fun onStopTrackingTouch(bar: SeekBar?) { }
            })
        })
        controls.addView(button("Seçili sesi dinle") { enqueueVoice("Merhaba! Oyun ve video çevirilerini bu sesle dinleyebilirsin.") })
        val speakerCard=card(root)
        speakerCard.addView(label("KONUŞMACI SESLERİ",11f,mint))
        speakerCard.addView(label("İsimli altyazıda aynı kişiye aynı ses. Dinle modunda konuşmacıyı elle seç; sesten otomatik kişi ayrımı bu sürümde yok.",13f,muted))
        speakerChoice=button("Etiketsiz konuşmacı: Genel ses") {
            AlertDialog.Builder(this).setTitle("Etiketsiz / mikrofon konuşmacısı")
                .setItems((0..8).map { if(it==0) "Genel ses" else "Konuşmacı $it" }.toTypedArray()) { _,slot ->
                    selectedSpeaker=slot;speakerChoice.text=if(slot==0) "Etiketsiz konuşmacı: Genel ses" else "Etiketsiz konuşmacı: $slot"
                }.show()
        };speakerCard.addView(speakerChoice)
        speakerCard.addView(CheckBox(this).apply {
            text="Altyazıdaki konuşmacı adlarını eşleştir";isChecked=true;setTextColor(muted)
            setOnCheckedChangeListener { _,checked -> automaticSubtitleNames=checked }
        })
        speakerCard.addView(button("Konuşmacı seslerini ayarla") {
            AlertDialog.Builder(this).setTitle("Sesini değiştireceğin konuşmacı")
                .setItems((1..8).map { "Konuşmacı $it" }.toTypedArray()) { _,index -> chooseSpeakerVoice(index+1) }.show()
        })
        speakerCard.addView(button("Konuşmacı eşleştirmelerini sıfırla") {
            profilePrefs.edit().clear().apply();speakers.clear();message("Ses ve isim eşleştirmeleri sıfırlandı.")
        })
        freeze=button("Tek kareyi çevir") {
            if(active&&mode==Mode.CAMERA) { frozen=true;message("Bir sonraki kare okunacak…") }
            else message("Önce kamerayı başlat.")
        };root.addView(freeze,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(12) })
        val output=card(root)
        output.addView(label("ALGILANAN İNGİLİZCE",11f,muted))
        source=label("Kaynak metin burada görünecek.",16f);output.addView(source)
        output.addView(label("TÜRKÇE ÇEVİRİ",11f,mint))
        result=label("Çevirin burada görünecek.",23f).apply { setTypeface(null,Typeface.BOLD) };output.addView(result)
        output.addView(button("Son çeviriyi tekrar oku") { enqueueVoice(lastTranslation) })
        val recent=card(root);recent.addView(label("SON REPLİKLER",11f,muted))
        history=label("Henüz çeviri yok.",14f);recent.addView(history)
        root.addView(button("Türkçe ses ayarları") {
            try { startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
            catch (_: Exception) { message("Telefon ayarlarından metinden konuşma bölümünü aç.") }
        })
        setContentView(ScrollView(this).apply { isFillViewport=true;addView(root) })
        voice=VoicePlayer(this,{ refreshMetrics();if(active&&mode==Mode.MIC) scheduleListening() },
            { if(active&&mode==Mode.MIC&&!headphones.isChecked) pauseMicrophone() },{ message(it) })
        selectMode(Mode.CAMERA)
    }
    private fun speakerVoiceName(slot: Int): String? {
        if(slot==0)return null
        val saved=profilePrefs.getString("voice_$slot",null)
        val available=voice.availableVoices(allowNetworkVoice)
        if(saved!=null&&available.any { it.name==saved })return saved
        val chosen=available.getOrNull((slot-1)%available.size.coerceAtLeast(1))?.name
        if(chosen!=null)profilePrefs.edit().putString("voice_$slot",chosen).apply()
        return chosen
    }
    private fun chooseSpeakerVoice(slot: Int) {
        val voices=voice.availableVoices(allowNetworkVoice)
        if(voices.isEmpty()) { message("Türkçe ses bulunamadı. Ses ayarlarını kontrol et.");return }
        val labels=voices.mapIndexed { i,v -> "Türkçe ${i+1} • ${if(v.isNetworkConnectionRequired) "İnternet" else "Cihazda"}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Konuşmacı $slot • gerçek ses seç")
            .setItems(labels) { _,index ->
                profilePrefs.edit().putString("voice_$slot",voices[index].name).apply()
                message("Konuşmacı $slot için ses kaydedildi.")
                voice.add("Bu konuşmacının Türkçe sesi böyle duyulacak.",false,voices[index].name)
            }.setNegativeButton("Kapat",null).show()
    }
    private fun chooseVoice() {
        val voices=voice.availableVoices(allowNetworkVoice)
        if(voices.isEmpty()) { message("Bu ses motorunda kullanılabilir Türkçe ses yok. Türkçe ses ayarlarını kontrol et.");return }
        val locale=java.util.Locale.forLanguageTag("tr-TR")
        val labels=voices.mapIndexed { index,v ->
            "Türkçe ${index+1} • ${v.locale.getDisplayCountry(locale).ifBlank { "Türkçe" }} • ${if(v.isNetworkConnectionRequired) "İnternet" else "Cihazda"}"
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Türkçe sesler (${voices.size})")
            .setItems(labels) { _,index ->
                if(voice.selectVoice(voices[index])) { voiceChoice.text=labels[index];message("Ses seçildi. Seçili sesi dinle ile karşılaştır.") }
                else message("Bu ses seçilemedi. Başka bir ses dene.")
            }.setNegativeButton("Kapat",null).show()
    }
    private fun message(text: String) { if(alive&&::status.isInitialized) status.text=text }
    private fun valid(token: Long)=alive&&token==epoch
    private fun selectMode(next: Mode) {
        stopSession();mode=next
        tabs.forEachIndexed { index,b ->
            val selected=index==mode.ordinal;b.background=rounded(if(selected) mint else panel,14f)
            b.setTextColor(if(selected) ink else Color.WHITE)
        }
        previewBox.visibility=if(mode==Mode.CAMERA) View.VISIBLE else View.GONE
        micHero.visibility=if(mode==Mode.MIC) View.VISIBLE else View.GONE
        region.visibility=if(mode==Mode.CAMERA) View.VISIBLE else View.GONE
        headphones.visibility=if(mode==Mode.MIC) View.VISIBLE else View.GONE
        freeze.visibility=if(mode==Mode.CAMERA) View.VISIBLE else View.GONE
        start.text=when(mode) { Mode.CAMERA->"Kamerayı başlat";Mode.MIC->"Dinlemeyi başlat";Mode.IMAGE->"Görsel seç" }
        hint.text=when(mode) {
            Mode.CAMERA->"TV veya monitörde İngilizce altyazıyı aç. Telefonu sabit tut. Yeşil alanı yalnızca altyazı satırlarına çiz; süre ve simgeleri dışarıda bırak."
            Mode.MIC->"Dinleme deneysel. Android konuşma servisi internet kullanabilir. Kulaklık yoksa Türkçe okuma sırasında mikrofon durur; o aradaki oyun konuşması yakalanmaz."
            Mode.IMAGE->"Fotoğraf veya ekran görüntüsündeki İngilizce yazıyı Türkçeye çevir ve sesli dinle."
        }
        if(modelReady) message("Hazır. Başlatabilirsin.")
    }
    private fun prepareModel() {
        prepare.isEnabled=false;message("Çeviri modeli hazırlanıyor. İlk indirme internet gerektirir…")
        translator.downloadModelIfNeeded().addOnSuccessListener {
            if(alive) { modelReady=true;prepare.isEnabled=true;prepare.text="Çeviri modeli hazır ✓";message("Hazır. Modunu seç ve başlat.") }
        }.addOnFailureListener { if(alive) { prepare.isEnabled=true;message("Model indirilemedi. İnterneti kontrol edip yeniden dene.") } }
    }
    private fun primaryAction() {
        if(!modelReady) { prepareModel();return }
        if(active) { stopSession();message("Oturum durduruldu.");return }
        when(mode) {
            Mode.CAMERA->if(has(Manifest.permission.CAMERA)) startCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
            Mode.IMAGE->{ stopSession();photo.launch("image/*") }
            Mode.MIC->{
                AlertDialog.Builder(this).setTitle("Oyun sesini dinle")
                    .setMessage("Telefon mikrofonu İngilizce konuşmayı Android konuşma tanıma servisine iletir. Servis cihazına göre internet kullanabilir. Türkçe sesi kulaklıkla dinlemek, okuma sırasında oyunu dinlemeye devam etmeyi sağlar. Kulaklık yoksa okuma sırasında dinleme durur.")
                    .setPositiveButton("Dinlemeyi başlat") { _,_ -> if(has(Manifest.permission.RECORD_AUDIO)) startMicrophone() else microphonePermission.launch(Manifest.permission.RECORD_AUDIO) }
                    .setNegativeButton("Vazgeç",null).show()
            }
        }
    }
    private fun has(permission: String)=ContextCompat.checkSelfPermission(this,permission)==PackageManager.PERMISSION_GRANTED
    @androidx.annotation.OptIn(markerClass=[ExperimentalGetImage::class, androidx.camera.view.TransformExperimental::class])
    private fun startCamera() {
        if(mode!=Mode.CAMERA)return
        stopSession();active=true;start.text="Kamerayı durdur";val token=epoch;message("Kamera açılıyor…")
        val future=ProcessCameraProvider.getInstance(this)
        future.addListener({
            if(valid(token)&&active) try {
                provider=future.get()
                val view=Preview.Builder().build().also { it.surfaceProvider=preview.surfaceProvider }
                val analysis=ImageAnalysis.Builder().setResolutionSelector(ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy(android.util.Size(1280,720),ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build())
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { proxy ->
                    val now=SystemClock.elapsedRealtime()
                    if(!active||mode!=Mode.CAMERA||busy||now-lastFrame<150) proxy.close()
                    else {
                        val image=proxy.image
                        if(image==null) proxy.close() else {
                            busy=true;lastFrame=now;val frameToken=epoch;val still=frozen
                            val rotation=proxy.imageInfo.rotationDegrees
                            val factory=ImageProxyTransformFactory().apply { isUsingCropRect=false;isUsingRotationDegrees=true }
                            val target=preview.outputTransform
                            val transform=target?.let { CoordinateTransform(factory.getOutputTransform(proxy),it) }
                            recognizer.process(InputImage.fromMediaImage(image,rotation)).addOnSuccessListener { text ->
                                if(valid(frameToken)&&active) {
                                    val selected=if(still) text.text else {
                                        val area=subtitleArea.bounds()
                                        text.textBlocks.flatMap { it.lines }.filter { line ->
                                            val box=line.boundingBox
                                            if(box==null||transform==null) false else {
                                                val mapped=RectF(box);transform.mapRect(mapped)
                                                val inArea=!region.isChecked||area.contains(mapped.centerX(),mapped.centerY())
                                                inArea&&subtitleFilter.keep(line.text,
                                                    mapped.centerX()/preview.width.coerceAtLeast(1),
                                                    mapped.centerY()/preview.height.coerceAtLeast(1),SystemClock.elapsedRealtime())
                                            }
                                        }.joinToString("\n") { it.text }
                                    }
                                    source.text=selected.ifBlank { "Okunabilir yazı bekleniyor…" }
                                    if(still) {
                                        stopSession()
                                        if(selected.isBlank()) message("Bu karede yazı bulunamadı.") else accept(selected,SystemClock.elapsedRealtime())
                                    } else {
                                        recent.observeVisibility(selected,SystemClock.elapsedRealtime())
                                        gate.observe(selected,SystemClock.elapsedRealtime())?.let {
                                        if(accept(it,SystemClock.elapsedRealtime())) gate.commit(it)
                                        }
                                    }
                                }
                            }.addOnFailureListener { if(valid(frameToken)) message("Kare okunamadı. Kameranın netliğini kontrol et.") }
                             .addOnCompleteListener { proxy.close();busy=false }
                        }
                    }
                }
                val viewport=preview.viewPort ?: error("Camera preview layout is not ready")
                val group=UseCaseGroup.Builder().setViewPort(viewport).addUseCase(view).addUseCase(analysis).build()
                provider?.unbindAll();provider?.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,group)
                message("Kamera açık. Altyazıyı takip ediyoruz.")
            } catch (_: Exception) { stopSession();message("Kamera açılamadı. İzinleri kontrol et.") }
        },ContextCompat.getMainExecutor(this))
    }
    private fun accept(text: String,seenAt: Long): Boolean {
        if(text.isBlank()||!modelReady)return false
        if(active&&mode==Mode.CAMERA) {
            if(!recent.shouldSubmit(text,seenAt)) return true
            waiting.clear() // One current pending camera line, not a backlog of old screen text.
        }
        val segments=speakers.route(text,selectedSpeaker,mode!=Mode.MIC&&automaticSubtitleNames)
        val editor=profilePrefs.edit()
        speakers.bindings().forEach { (name,slot) -> editor.putString("speaker_$slot",name) }
        editor.apply()
        var accepted=true
        for(segment in segments) {
            if(!waiting.offer(segment.text,seenAt,segment.slot,segment.name)) {
                rejected++;accepted=false;message("Çeviri sırası doldu. Bazı replikler alınamadı.")
            }
        }
        pumpTranslation();refreshMetrics();return accepted
    }
    private fun pumpTranslation() {
        if(translating||!alive)return
        val line=waiting.take()?:return
        val token=epoch;val fromCamera=active&&mode==Mode.CAMERA;translating=true
        translator.translate(line.text).addOnSuccessListener { translated ->
            if(valid(token) && (!fromCamera || SystemClock.elapsedRealtime()-line.seenAt<=3000)) {
                completed++;lastLag=SystemClock.elapsedRealtime()-line.seenAt
                lastTranslation=translated;lastSpeaker=line.speakerSlot
                result.text=if(line.speakerName.isBlank()) translated else "${line.speakerName} • Konuşmacı ${line.speakerSlot}\n$translated"
                log.addFirst("${if(line.speakerName.isBlank()) "Konuşmacı ${line.speakerSlot}" else line.speakerName} • ${line.text}\n$translated")
                while(log.size>12) log.removeLast()
                history.text=log.take(4).joinToString("\n\n")
                if(autoVoice.isChecked) enqueueVoice(translated,line.speakerSlot)
                if(active&&mode==Mode.CAMERA) message("Canlı çeviri açık")
                else if(!active) message("Görsel çevirisi hazır")
            }
        }.addOnFailureListener {
            if(valid(token)) { gate.release(line.text);recent.forget(line.text);rejected++;message("Bir replik çevrilemedi. İnterneti/modeli kontrol et.") }
        }.addOnCompleteListener { if(valid(token)) { translating=false;refreshMetrics();pumpTranslation();if(mode==Mode.MIC) scheduleListening() } }
    }
    private fun enqueueVoice(text: String, speakerSlot: Int=lastSpeaker) {
        if(text.isBlank())return
        if(!voice.ready) { message("Türkçe ses hazır değil. Ses ayarlarından Türkçe paket yükle.");return }
        if(!voice.add(text, active&&mode==Mode.CAMERA,speakerVoiceName(speakerSlot))) { rejected++;message("Ses sırası doldu. Çeviri ekranda korunuyor.") }
        refreshMetrics()
    }
    private fun refreshMetrics() {
        if(!::metrics.isInitialized)return
        val sounds=if(::voice.isInitialized) voice.count else 0
        metrics.text="Çevrilen $completed  •  Bekleyen ${waiting.size + if(translating) 1 else 0}  •  Ses $sounds\nSon çeviri ${lastLag/1000.0} sn  •  Sıra/işlem uyarısı $rejected"
    }
    private fun startMicrophone() {
        if(mode!=Mode.MIC)return
        stopSession()
        if(!SpeechRecognizer.isRecognitionAvailable(this)) { message("Bu telefonda konuşma tanıma servisi bulunamadı.");return }
        active=true;start.text="Dinlemeyi durdur";errors=0;emptySpeechRounds=0;preferSystemSpeech=!localSpeech.isChecked
        message("İngilizce oyun / video konuşması dinleniyor…");listenOnce()
    }
    private fun canListen()=alive&&active&&mode==Mode.MIC&&has(Manifest.permission.RECORD_AUDIO)&&
        (headphones.isChecked||voice.count==0)
    private fun scheduleListening(delay: Long=350) {
        handler.removeCallbacks(restart)
        if(canListen()&&!listening) handler.postDelayed(restart,delay)
    }
    private fun listenOnce() {
        if(!canListen()||listening)return
        speech?.destroy();speech=null
        val round=++micRound;val token=epoch
        try {
            val onDevice=Build.VERSION.SDK_INT>=31&&!preferSystemSpeech&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
            speech=if(Build.VERSION.SDK_INT>=31&&onDevice) SpeechRecognizer.createOnDeviceSpeechRecognizer(this) else SpeechRecognizer.createSpeechRecognizer(this)
            speech?.setRecognitionListener(object : RecognitionListener {
                private fun current()=valid(token)&&round==micRound&&active&&mode==Mode.MIC
                override fun onReadyForSpeech(params: Bundle?) {
                    if(current()) { signalLabel.text="Mikrofon açık • İngilizce konuşma bekleniyor";message("Dinleniyor • İngilizce konuşma bekleniyor") }
                }
                override fun onBeginningOfSpeech() {
                    if(current()) { message("Konuşma algılandı…");armSpeechDeadline(token,round,6000) }
                }
                override fun onRmsChanged(rmsdB: Float) {
                    if(current()) { signal.progress=((rmsdB+2f)*8f).toInt().coerceIn(0,100);signalLabel.text="Mikrofon açık • Ses düzeyi ${signal.progress}/100" }
                }
                override fun onBufferReceived(buffer: ByteArray?) { }
                override fun onEndOfSpeech() {
                    if(current()&&!finalRequested) { message("Konuşma çözümleniyor…");armSpeechDeadline(token,round,5000) }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if(current()) partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                        if(it.isNotBlank()) {
                            val changed=it!=partialText;partialText=it;source.text=it
                            if(changed&&!finalRequested) armSpeechDeadline(token,round,1400)
                        }
                    }
                }
                override fun onResults(results: Bundle?) {
                    if(!current())return
                    errors=0
                    val text=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().ifBlank { partialText }
                    finishSpeechChunk(text,token,round,false)
                }
                override fun onError(error: Int) {
                    if(!current())return
                    clearSpeechDeadline()
                    listening=false
                    if(partialText.isNotBlank() && (error==SpeechRecognizer.ERROR_NO_MATCH||error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                        finishSpeechChunk(partialText,token,round,true);return
                    }
                    if(error==SpeechRecognizer.ERROR_NO_MATCH||error==SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        emptySpeechRounds++
                        if(emptySpeechRounds>=3) { stopSession();message("Üç denemede İngilizce metin alınamadı. Kısa konuşma testini dene; mikrofonu, İngilizce konuşma paketini ve interneti kontrol et.") }
                        else { message("Konuşma metni alınamadı. Sesi yaklaştır; yeniden deneniyor…");scheduleListening(900) }
                    }
                    else {
                        val languageError=Build.VERSION.SDK_INT>=31 && (error==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED||error==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
                        if(onDevice&&languageError) {
                            preferSystemSpeech=true;errors=0
                            message("Yerel İngilizce tanıma hazır değil. Android konuşma servisi deneniyor; internet kullanabilir.")
                            scheduleListening(1000);return
                        }
                        errors++
                        if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS||languageError||errors>=3) {
                            stopSession();message("Dinleme durdu (kod $error). İngilizce konuşma paketini, mikrofonu ve interneti kontrol et.")
                        } else { message("Dinleme yeniden deneniyor (kod $error)…");scheduleListening(1500) }
                    }
                }
                override fun onEvent(eventType: Int,params: Bundle?) { }
            })
            partialText="";finalRequested=false;listening=true
            armSpeechDeadline(token,round,20000)
            val hard=Runnable {
                if(valid(token)&&round==micRound&&active&&mode==Mode.MIC)
                    requestSpeechFinal(token,round)
            }
            speechHardDeadline=hard;handler.postDelayed(hard,20000)
            speech?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)
            })
        } catch (_: Exception) { stopSession();message("Konuşma tanıma başlatılamadı. Telefonun konuşma servisini kontrol et.") }
    }
    private fun clearSpeechDeadline() {
        speechDeadline?.let { handler.removeCallbacks(it) };speechDeadline=null
        speechHardDeadline?.let { handler.removeCallbacks(it) };speechHardDeadline=null
    }
    private fun armSpeechDeadline(token: Long,round: Long,delay: Long) {
        speechDeadline?.let { handler.removeCallbacks(it) }
        val task=Runnable {
            if(valid(token)&&round==micRound&&active&&mode==Mode.MIC)
                requestSpeechFinal(token,round)
        }
        speechDeadline=task;handler.postDelayed(task,delay)
    }
    private fun requestSpeechFinal(token: Long,round: Long) {
        if(!valid(token)||round!=micRound||!active||mode!=Mode.MIC||finalRequested)return
        finalRequested=true;clearSpeechDeadline()
        message("Konuşma metni hazırlanıyor…")
        try { speech?.stopListening() } catch (_: Exception) { }
        val fallback=Runnable {
            if(valid(token)&&round==micRound&&active&&mode==Mode.MIC)
                finishSpeechChunk(partialText,token,round,true)
        }
        speechDeadline=fallback;handler.postDelayed(fallback,5000)
    }
    private fun finishSpeechChunk(text: String,token: Long,round: Long,interim: Boolean) {
        if(!valid(token)||round!=micRound||!active||mode!=Mode.MIC)return
        clearSpeechDeadline();micRound++;listening=false
        speech?.cancel();speech?.destroy();speech=null;partialText=""
        if(text.isNotBlank()) {
            source.text=text
            message(if(interim) "Ara konuşma sonucu çevriliyor…" else "Konuşma çevriliyor…")
            accept(text,SystemClock.elapsedRealtime())
        } else {
            emptySpeechRounds++
            if(emptySpeechRounds>=2) {
                stopSession();message("İki denemede konuşma metni alınamadı. Kısa konuşma testini aç; mikrofon izni, İngilizce tanıma ve interneti kontrol et.");return
            }
            message("Konuşma metni alınamadı. Bir kez daha deneniyor…")
        }
        if(text.isNotBlank())emptySpeechRounds=0
        scheduleListening(700)
    }
    private fun pauseMicrophone() {
        handler.removeCallbacks(restart);clearSpeechDeadline();micRound++;listening=false;partialText=""
        speech?.cancel();speech?.destroy();speech=null
        if(active&&mode==Mode.MIC) message("Türkçe okunuyor • Okuma bitince dinleme devam edecek")
    }
    private fun stopSession() {
        epoch++;active=false;frozen=false;translating=false;gate.reset();waiting.clear();recent.clear();subtitleFilter.clear()
        provider?.unbindAll();pauseMicrophone()
        if(::voice.isInitialized)voice.stop()
        if(::start.isInitialized)start.text=when(mode) { Mode.CAMERA->"Kamerayı başlat";Mode.MIC->"Dinlemeyi başlat";Mode.IMAGE->"Görsel seç" }
        refreshMetrics()
    }
    override fun onStop() { stopSession();super.onStop() }
    override fun onDestroy() {
        alive=false;epoch++;handler.removeCallbacks(restart);clearSpeechDeadline();provider?.unbindAll()
        speech?.destroy();if(::voice.isInitialized)voice.close()
        recognizer.close();translator.close();super.onDestroy()
    }
}
