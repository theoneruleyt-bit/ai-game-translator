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
    private var ink=Color.rgb(11,17,29)
    private var panel=Color.rgb(23,34,53)
    private var mint=Color.rgb(100,235,204)
    private var muted=Color.rgb(170,185,209)
    private var textInk=Color.WHITE
    private var dark=true
    private lateinit var meter: AudioMeterView
    private var cameraRevision=0L
    private var lastSource=""
    private val phraseBook by lazy { PhraseBook(this) }
    private lateinit var readiness: TextView
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
    private val offline by lazy { OfflineSpeech(this) }
    private var offlineMode=true
    private var partialRevision=0L
    private var partialBusy=false
    private var partialScheduled=false
    private var partialUtterance=0L
    private var pendingPartial=""
    private var pendingPartialFinal=false
    private var latestRecognized=""
    private val liveCaption=java.util.ArrayDeque<String>()
    private val deferredSystemVoice=java.util.ArrayDeque<Pair<String,Int>>()
    private val liveSegments=LiveSpeechSegments()
    private val conversation by lazy { ConversationLexicon(this) }
    private lateinit var packChoice: Button
    private lateinit var speedChoice: Button
    private var speechDisplayRevision=0L
    private var lastSpeechDisplayRevision=-1L
    private var slangEnabled=true
    private val speechPulse=object : Runnable {
        override fun run() {
            if(alive&&active&&mode==Mode.MIC) {
                if(latestRecognized.isNotBlank()) processLiveSpeech(latestRecognized,false)
                handler.postDelayed(this,180)
            }
        }
    }
    private val partialTask=Runnable { partialScheduled=false;translatePartial() }
    private lateinit var localSpeech: CheckBox
    private lateinit var signal: ProgressBar
    private lateinit var signalLabel: TextView
    private lateinit var voiceChoice: Button
    private var allowNetworkVoice=false
    private var restoredVoice=false
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
    private val reverseTranslator=Translation.getClient(TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.TURKISH).setTargetLanguage(TranslateLanguage.ENGLISH).build())
    private fun translatingTurkish()=mode==Mode.MIC&&offline.pack.language=="tr"
    private fun translateSmart(text: String): com.google.android.gms.tasks.Task<String> {
        val reverse=translatingTurkish()
        val exact=if(slangEnabled) conversation.exact(text,reverse) else null
        if(exact!=null)return com.google.android.gms.tasks.Tasks.forResult(exact)
        val prepared=if(slangEnabled&&!reverse) conversation.normalizeEnglish(text) else text
        return (if(reverse) reverseTranslator else translator).translate(prepared)
    }
    private fun updateSpeechDirection() {
        if(::voice.isInitialized) voice.setTargetLanguage(if(translatingTurkish()) "en" else "tr")
    }
    private val gate=StabilityGate()
    private val speakers=SpeakerRouter()
    private var selectedSpeaker=0
    private var lastSpeaker=0
    private var automaticSubtitleNames=true
    private lateinit var speakerChoice: Button
    private lateinit var proService: ProService
    private var captureClockStarted=false
    
    
    private var lastServerCheck=0L
    private var accessChecking=false
    private var visible=true
    private lateinit var billing: PlayBilling
    private lateinit var proStatus: TextView
    private var proDialog: AlertDialog?=null
    private var proOffers: LinearLayout?=null
    private val cameraTrial by lazy { TrialMeter(settings.getLong("trial_camera_used_v2",0)) }
    private val microphoneTrial by lazy { TrialMeter(settings.getLong("trial_microphone_used_v2",0)) }
    private val trial get()=if(mode==Mode.MIC)microphoneTrial else cameraTrial
    private val imageTrial by lazy { ImageTrial(settings.getInt("trial_images_used_v2",0)) }
    private var pendingTrialImage: Long?=null
    private fun saveTrialUsage(sync: Boolean=false) {
        val editor=settings.edit().putLong("trial_camera_used_v2",cameraTrial.usedMillis)
            .putLong("trial_microphone_used_v2",microphoneTrial.usedMillis)
            .putInt("trial_images_used_v2",imageTrial.usedCount)
        if(sync)editor.commit() else editor.apply()
    }
    private val trialTick=object : Runnable {
        override fun run() {
            if(!alive||!active)return
            val now=SystemClock.elapsedRealtime()
            if(billing.pro)trial.stop(now) else { if(!trial.running)trial.start(now);trial.tick(now) }
            if(now-lastServerCheck>=60000) {
                lastServerCheck=now
                if(proService.enabled)proService.refresh { ok -> if(alive&&active&&!ok) { stopSession();message(getString(R.string.account_offline)) } } else billing.refresh()
            }
            saveTrialUsage();refreshPro()
            if(!billing.pro&&trial.remainingMillis==0L) { stopSession(true);showPro();return }
            handler.postDelayed(this,250)
        }
    }
    private fun countLiveUse() {
        if(!captureClockStarted) {
            captureClockStarted=true;lastServerCheck=SystemClock.elapsedRealtime()
        }
        if(!billing.pro)trial.start(SystemClock.elapsedRealtime())
        handler.removeCallbacks(trialTick);handler.post(trialTick)
    }
    private fun refreshPro() {
        if(::proStatus.isInitialized)proStatus.text=if(billing.pro)getString(R.string.pro_active) else getString(R.string.pro_remaining,((cameraTrial.remainingMillis+999)/1000).toString(),((microphoneTrial.remainingMillis+999)/1000).toString(),imageTrial.remaining.toString())
    }
    private fun renderProOffers() {
        val content=proOffers?:return
        content.removeAllViews()
        val configured=if(proService.enabled)proService.signedIn else getString(R.string.play_license_key).isNotBlank()
        if(billing.plans.isEmpty()||!configured)content.addView(label(getString(R.string.pro_unavailable),14f,muted))
        billing.plans.sortedBy { listOf("monthly","yearly","lifetime").indexOf(it.name) }.forEach { plan ->
            val title=when(plan.name) { "monthly"->R.string.pro_monthly;"yearly"->R.string.pro_yearly;else->R.string.pro_lifetime }
            content.addView(button(getString(title)+" • "+plan.price) { proDialog?.dismiss();billing.buy(plan) }.apply { isEnabled=configured })
        }
    }
    private fun showPro() {
        val content=vertical().apply { setPadding(dp(20),dp(12),dp(20),dp(12)) }
        content.addView(label(getString(R.string.pro_detail),16f))
        content.addView(label(getString(R.string.pro_plans),14f,mint))
        proDialog?.dismiss()
        val offers=vertical();proOffers=offers;content.addView(offers)
        val scroll=ScrollView(this).apply { addView(content) }
        val dialog=AlertDialog.Builder(this).setTitle(getString(R.string.pro_title)).setView(scroll).setNegativeButton(getString(R.string.pro_close),null).create()
        proDialog=dialog
        dialog.setOnDismissListener { if(proDialog===dialog) { proDialog=null;proOffers=null } }
        renderProOffers()
        content.addView(label(getString(R.string.pro_terms),12f,muted))
        dialog.show();billing.refresh()
    }
    private val settings by lazy { getSharedPreferences("app_settings",MODE_PRIVATE) }
    private var speedMode=0
    private var adaptiveReading=true
    private var keepScreen=true
    private var sessionStarted=0L
    private val sessionTick=object : Runnable { override fun run() { if(active&&alive) { refreshMetrics();handler.postDelayed(this,1000) } } }
    private var cachedSource=""
    private var cachedTranslation=""
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
        if(it&&visible&&mode==Mode.CAMERA) startCamera() else if(!it) message(getString(R.string.ui_000))
    }
    private val microphonePermission=registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if(it&&visible&&mode==Mode.MIC) startMicrophone() else if(!it) message(getString(R.string.ui_001))
    }
    private val speechTest=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        if(response.resultCode==RESULT_OK) {
            val text=response.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            source.text=text
            if(text.isNotBlank()) accept(text,SystemClock.elapsedRealtime())
            else message(getString(R.string.ui_002))
        } else message(getString(R.string.ui_003))
    }
    private val photo=registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null&&alive&&mode==Mode.IMAGE) {
            if(!billing.pro&&imageTrial.remaining==0) { showPro();return@registerForActivityResult }
            stopSession();val token=epoch;source.text=getString(R.string.ui_004);result.text=getString(R.string.ui_005);lastTranslation="";message(getString(R.string.ui_006))
            try {
                recognizer.process(InputImage.fromFilePath(this,uri)).addOnSuccessListener {
                    if(valid(token)) {
                        source.text=it.text
                        if(it.text.isBlank()) message(getString(R.string.ui_007))
                        else { pendingTrialImage=if(billing.pro) null else token;accept(it.text,SystemClock.elapsedRealtime()) }
                    }
                }.addOnFailureListener { if(valid(token)) message(getString(R.string.ui_008)) }
            } catch (_: Exception) { message(getString(R.string.ui_009)) }
        }
    }
    private fun dp(value: Int)=(value*resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Float=20f)=GradientDrawable().apply {
        setColor(color);cornerRadius=dp(radius.toInt()).toFloat()
    }
    private fun label(text: String,size: Float=15f,color: Int=textInk)=TextView(this).apply {
        this.text=text;textSize=size;setTextColor(color);includeFontPadding=false;setLineSpacing(dp(3).toFloat(),1.04f);setPadding(0,dp(5),0,dp(5))
    }
    private fun button(text: String, primary: Boolean=false, action: () -> Unit)=Button(this).apply {
        this.text=text;isAllCaps=false;textSize=14f;minHeight=dp(48);stateListAnimator=null
        setTextColor(if(primary) (if(dark) ink else Color.WHITE) else textInk)
        background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(if(dark) 0x2257d9c0 else 0x22006462),rounded(if(primary) mint else panel,14f),null)
        setPadding(dp(12),dp(8),dp(12),dp(8));setOnClickListener { action() }
    }
    private fun vertical()=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
    private fun card(parent: LinearLayout): LinearLayout {
        val v=vertical().apply { background=rounded(panel).apply { setStroke(dp(1),if(dark) Color.rgb(39,53,73) else Color.rgb(219,228,236)) };elevation=dp(2).toFloat();setPadding(dp(18),dp(16),dp(18),dp(16)) }
        parent.addView(v,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(14) });return v
    }
    private fun row(parent: LinearLayout, buttons: List<Button>) {
        val line=LinearLayout(this)
        buttons.forEachIndexed { index,b -> line.addView(b,LinearLayout.LayoutParams(0,-2,1f).apply { if(index>0) leftMargin=dp(8) }) }
        parent.addView(line,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(12) })
    }
    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs=newBase.getSharedPreferences("app_settings",MODE_PRIVATE)
        val config=android.content.res.Configuration(newBase.resources.configuration)
        val language=prefs.getString("language","system").orEmpty()
        if(language!="system")config.setLocale(java.util.Locale.forLanguageTag(language))
        val theme=prefs.getString("theme","system")
        val night=when(theme) { "dark"->android.content.res.Configuration.UI_MODE_NIGHT_YES;"light"->android.content.res.Configuration.UI_MODE_NIGHT_NO;else->config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK }
        config.uiMode=(config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark=resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK==android.content.res.Configuration.UI_MODE_NIGHT_YES
        ink=if(dark)Color.rgb(10,17,29) else Color.rgb(243,247,251)
        panel=if(dark)Color.rgb(20,31,48) else Color.WHITE
        textInk=if(dark)Color.rgb(238,244,250) else Color.rgb(20,36,53)
        mint=if(dark)Color.rgb(100,235,204) else Color.rgb(0,112,101)
        muted=if(dark)Color.rgb(163,181,201) else Color.rgb(82,102,121)
        androidx.core.view.WindowInsetsControllerCompat(window,window.decorView).apply {
            isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark
        }
        offline.selectPack(SpeechPack.ALL[settings.getInt("speech_pack",0).coerceIn(0,2)]);slangEnabled=settings.getBoolean("slang",true)
        speedMode=settings.getInt("speed_mode",0).coerceIn(0,2);adaptiveReading=settings.getBoolean("adaptive",true);keepScreen=settings.getBoolean("screen",true)
        selectedSpeaker=settings.getInt("speaker",0).coerceIn(0,16)
        automaticSubtitleNames=settings.getBoolean("names",true)
        speakers.restore((1..16).mapNotNull { slot -> profilePrefs.getString("speaker_${slot}",null)?.let { it to slot } }.toMap())
        window.statusBarColor=ink;window.navigationBarColor=ink
        val root=vertical().apply { setPadding(dp(20),dp(24),dp(20),dp(28));background=GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(ink,if(dark)Color.rgb(15,26,43) else Color.rgb(232,241,247))) }
        val brand=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        val logo=ImageView(this).apply { setImageResource(com.optima.gametranslator.R.drawable.brand_icon) }
        brand.addView(logo,LinearLayout.LayoutParams(dp(46),dp(46)).apply { rightMargin=dp(12) })
        val titles=vertical();titles.addView(label("AI Game Translator",23f).apply { setTypeface(null,Typeface.BOLD) })
        titles.addView(label(getString(R.string.ui_010),13f,muted));brand.addView(titles)
        root.addView(brand,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(18) })
        root.addView(label(getString(R.string.ui_011),12f,mint))
        readiness=label("",12f,muted);root.addView(readiness)
        proService=ProService(this)
        billing=PlayBilling(this,{ if(alive) { refreshPro();renderProOffers() } },proService)
        val proCard=card(root)
        proStatus=label("",15f,mint);proCard.addView(proStatus)
        proCard.addView(button(getString(R.string.pro_open)) { showPro() })
        proCard.addView(button(getString(R.string.pro_restore)) { billing.refresh() })
        if(proService.enabled)proCard.addView(button(getString(R.string.account_sign_in)) {
            stopSession();proService.signIn { ok ->
                if(alive) { refreshPro();message(getString(if(ok) R.string.account_ready else R.string.account_failed));if(ok)billing.refresh() }
            }
        })
        refreshPro();billing.connect()
        tabs=listOf(button(getString(R.string.ui_012)) { selectMode(Mode.CAMERA) },button(getString(R.string.ui_013)) { selectMode(Mode.MIC) },button(getString(R.string.ui_014)) { selectMode(Mode.IMAGE) })
        row(root,tabs)
        row(root,listOf(button(getString(R.string.ui_015)) { showSettings() },button(getString(R.string.ui_016)) { showGuide() }))
        previewBox=card(root)
        preview=PreviewView(this).apply {
            scaleType=PreviewView.ScaleType.FIT_CENTER
            implementationMode=PreviewView.ImplementationMode.COMPATIBLE
        }
        subtitleArea=SubtitleAreaView(this).apply { onAreaChanged={
            if(active&&mode==Mode.CAMERA) {
                epoch++;busy=false;translating=false;waiting.clear();gate.reset();recent.clear();subtitleFilter.clear();voice.stop()
                message(getString(R.string.ui_017))
            }
        } }
        subtitleArea.restoreArea(settings.getFloat("area_left",.1f),settings.getFloat("area_top",.55f),
            settings.getFloat("area_right",.9f),settings.getFloat("area_bottom",.78f))
        val cameraFrame=FrameLayout(this)
        cameraFrame.addView(preview,FrameLayout.LayoutParams(-1,-1))
        cameraFrame.addView(subtitleArea,FrameLayout.LayoutParams(-1,-1))
        previewBox.addView(cameraFrame,LinearLayout.LayoutParams(-1,dp(250)))
        previewBox.addView(button(getString(R.string.ui_018)) { subtitleArea.resetArea() })
        previewBox.addView(label(getString(R.string.ui_019),11f,mint))
        micHero=card(root)
        meter=AudioMeterView(this).apply { color=mint;contentDescription=getString(R.string.audio_level) }
        micHero.addView(meter,LinearLayout.LayoutParams(-1,dp(76)))
        micHero.addView(label(getString(R.string.ui_020),21f).apply { gravity=Gravity.CENTER })
        micHero.addView(label(getString(R.string.listen_version),12f,mint))
        micHero.addView(label(getString(R.string.ui_021),14f,muted))
        signalLabel=label(getString(R.string.ui_022),13f,mint);micHero.addView(signalLabel)
        signal=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply { max=100 }
        micHero.addView(signal,LinearLayout.LayoutParams(-1,dp(10)))
        packChoice=button(getString(R.string.speech_model_choice,resources.getStringArray(R.array.speech_packs)[SpeechPack.ALL.indexOf(offline.pack)])) {
            AlertDialog.Builder(this).setTitle(getString(R.string.speech_pack_title))
                .setSingleChoiceItems(resources.getStringArray(R.array.speech_packs),SpeechPack.ALL.indexOf(offline.pack)) { dialog,index ->
                    stopSession();offline.selectPack(SpeechPack.ALL[index]);settings.edit().putInt("speech_pack",index).apply()
                    packChoice.text=getString(R.string.speech_model_choice,resources.getStringArray(R.array.speech_packs)[index])
                    updateSpeechDirection();refreshReadiness();dialog.dismiss()
                    message(getString(R.string.speech_pack_selected))
                }.setNegativeButton(getString(R.string.ui_039),null).show()
        };micHero.addView(packChoice)
        speedChoice=button(getString(R.string.ui_136,recognitionModes()[speedMode])) { chooseRecognitionSpeed() }
        micHero.addView(speedChoice)
        micHero.addView(label(getString(R.string.complete_speech_help),13f,muted))
        micHero.addView(label(getString(R.string.speech_pack_help),13f,muted))
        micHero.addView(CheckBox(this).apply {
            text=getString(R.string.slang_support);setTextColor(textInk);isChecked=slangEnabled
            setOnCheckedChangeListener { _,checked ->
                stopSession();slangEnabled=checked;settings.edit().putBoolean("slang",checked).apply()
            }
        })
        micHero.addView(button(getString(R.string.ui_023)) { prepareSpeechModel() })
        micHero.addView(CheckBox(this).apply {
            text=getString(R.string.ui_024);setTextColor(mint);isChecked=true
            setOnCheckedChangeListener { _,checked ->
                stopSession();offlineMode=checked;localSpeech.visibility=if(checked) View.GONE else View.VISIBLE
                message(if(checked) getString(R.string.ui_025) else getString(R.string.ui_026))
            }
        })
        localSpeech=CheckBox(this).apply {
            visibility=View.GONE
            text=getString(R.string.ui_027);setTextColor(muted);isChecked=false
            setOnCheckedChangeListener { _,_ -> if(active&&mode==Mode.MIC) { stopSession();message(getString(R.string.ui_028)) } }
        };micHero.addView(localSpeech)
        micHero.addView(button(getString(R.string.ui_029)) {
            if(active&&mode==Mode.MIC&&offlineMode) { offline.finishUtterance();message(getString(R.string.ui_030)) }
            else if(active&&mode==Mode.MIC&&listening) requestSpeechFinal(epoch,micRound)
            else message(getString(R.string.ui_031))
        })
        micHero.addView(button(getString(R.string.ui_032)) {
            if(!modelReady) { message(getString(R.string.ui_033)) }
            else {
                stopSession()
                AlertDialog.Builder(this).setTitle(getString(R.string.ui_034))
                    .setMessage(getString(R.string.ui_035))
                    .setPositiveButton(getString(R.string.ui_036)) { _,_ ->
                        try { speechTest.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE,if(translatingTurkish()) "tr-TR" else "en-US")
                            putExtra(RecognizerIntent.EXTRA_PROMPT,getString(R.string.ui_037))
                        }) } catch (_: Exception) { message(getString(R.string.ui_038)) }
                    }.setNegativeButton(getString(R.string.ui_039),null).show()
            }
        })
        hint=label("",14f,muted);root.addView(hint)
        val controls=card(root)
        status=label(getString(R.string.ui_040),15f);controls.addView(status)
        prepare=button(getString(R.string.ui_041)) { prepareModel() };controls.addView(prepare)
        controls.addView(label("",3f))
        start=button(getString(R.string.ui_042),true) { primaryAction() }
        row(controls,listOf(start,button(getString(R.string.ui_043)) { stopSession();message(getString(R.string.ui_044)) }))
        metrics=label("",12f,mint);controls.addView(metrics)
        autoVoice=CheckBox(this).apply {
            text=getString(R.string.ui_045);setTextColor(textInk);isChecked=settings.getBoolean("auto_voice",true)
            setOnCheckedChangeListener { _,checked ->
                if(!checked) { deferredSystemVoice.clear();if(::voice.isInitialized)voice.stop() }
                if(checked&&active&&mode==Mode.MIC&&offlineMode) message(getString(R.string.ui_046))
            }
        };controls.addView(autoVoice)
        region=CheckBox(this).apply {
            text=getString(R.string.ui_047);setTextColor(muted);isChecked=settings.getBoolean("region",true)
            setOnCheckedChangeListener { _,checked ->
                subtitleArea.visibility=if(checked) View.VISIBLE else View.GONE
                if(active) { stopSession();message(getString(R.string.ui_048)) }
            }
        };controls.addView(region)
        headphones=CheckBox(this).apply {
            text=getString(R.string.ui_049);setTextColor(muted);isChecked=false
            setOnCheckedChangeListener { _,checked ->
                if(active&&mode==Mode.MIC) {
                    if(offlineMode) { message(if(checked) getString(R.string.ui_050) else getString(R.string.ui_051)) }
                    else if(!checked&&voice.count>0) pauseMicrophone() else scheduleListening()
                }
            }
        };controls.addView(headphones)
        val advanced=vertical().apply { visibility=View.GONE }
        controls.addView(button(getString(R.string.ui_052)) { advanced.visibility=if(advanced.visibility==View.VISIBLE)View.GONE else View.VISIBLE })
        controls.addView(advanced)
        advanced.addView(label(getString(R.string.ui_053),13f,muted))
        advanced.addView(SeekBar(this).apply {
            max=6;progress=settings.getInt("rate_step",2).coerceIn(0,6)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?,progress: Int,fromUser: Boolean) { settings.edit().putInt("rate_step",progress).apply();if(::voice.isInitialized) voice.rate=0.9f+progress*0.1f }
                override fun onStartTrackingTouch(bar: SeekBar?) { }
                override fun onStopTrackingTouch(bar: SeekBar?) { }
            })
        })
        voiceChoice=button(getString(R.string.ui_054)) { chooseVoice() };advanced.addView(voiceChoice)
        advanced.addView(CheckBox(this).apply {
            text=getString(R.string.ui_055);setTextColor(muted);isChecked=false
            setOnCheckedChangeListener { box,checked ->
                if(checked) AlertDialog.Builder(this@MainActivity).setTitle(getString(R.string.ui_056))
                    .setMessage(getString(R.string.ui_057))
                    .setPositiveButton(getString(R.string.ui_058)) { _,_ -> allowNetworkVoice=true;voice.networkAllowed=true }
                    .setNegativeButton(getString(R.string.ui_039)) { _,_ -> box.isChecked=false }.show()
                else {
                    allowNetworkVoice=false
                    if(::voice.isInitialized)voice.networkAllowed=false
                    if(::voice.isInitialized) voice.availableVoices(false).firstOrNull()?.let { voice.selectVoice(it) }
                }
            }
        })
        advanced.addView(label(getString(R.string.ui_059),13f,muted))
        advanced.addView(SeekBar(this).apply {
            max=6;progress=settings.getInt("pitch_step",3).coerceIn(0,6)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?,progress: Int,fromUser: Boolean) { settings.edit().putInt("pitch_step",progress).apply();if(::voice.isInitialized) voice.pitch=.85f+progress*.05f }
                override fun onStartTrackingTouch(bar: SeekBar?) { }
                override fun onStopTrackingTouch(bar: SeekBar?) { }
            })
        })
        advanced.addView(button(getString(R.string.ui_060)) { enqueueVoice("Merhaba! Oyun ve video çevirilerini bu sesle dinleyebilirsin.") })
        val speakerCard=card(root)
        speakerCard.addView(label(getString(R.string.ui_061),11f,mint))
        val speakerContent=vertical().apply { visibility=View.GONE }
        speakerCard.addView(button(getString(R.string.ui_062)) { speakerContent.visibility=if(speakerContent.visibility==View.VISIBLE)View.GONE else View.VISIBLE })
        speakerCard.addView(speakerContent)
        speakerContent.addView(label(getString(R.string.ui_063),13f,muted))
        speakerChoice=button(if(selectedSpeaker==0) getString(R.string.ui_064) else getString(R.string.ui_065, (selectedSpeaker).toString())) {
            AlertDialog.Builder(this).setTitle(getString(R.string.ui_066))
                .setItems((0..16).map { if(it==0) getString(R.string.ui_067) else getString(R.string.ui_068, (it).toString()) }.toTypedArray()) { _,slot ->
                    selectedSpeaker=slot;speakerChoice.text=if(slot==0) getString(R.string.ui_064) else getString(R.string.ui_065, (slot).toString())
                }.show()
        };speakerContent.addView(speakerChoice)
        speakerContent.addView(CheckBox(this).apply {
            text=getString(R.string.ui_069);isChecked=automaticSubtitleNames;setTextColor(muted)
            setOnCheckedChangeListener { _,checked -> automaticSubtitleNames=checked }
        })
        speakerContent.addView(button(getString(R.string.ui_070)) {
            AlertDialog.Builder(this).setTitle(getString(R.string.ui_071))
                .setItems((1..16).map { getString(R.string.ui_068, (it).toString()) }.toTypedArray()) { _,index -> chooseSpeakerVoice(index+1) }.show()
        })
        speakerContent.addView(button(getString(R.string.ui_072)) {
            AlertDialog.Builder(this).setTitle(getString(R.string.ui_073)).setItems((1..16).map { getString(R.string.ui_068, (it).toString()) }.toTypedArray()) { _,index -> chooseSpeakerStyle(index+1) }.show()
        })
        speakerContent.addView(button(getString(R.string.ui_074)) {
            profilePrefs.edit().clear().apply();speakers.clear();message(getString(R.string.ui_075))
        })
        freeze=button(getString(R.string.ui_076)) {
            if(active&&mode==Mode.CAMERA) { frozen=true;message(getString(R.string.ui_077)) }
            else message(getString(R.string.ui_078))
        };root.addView(freeze,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(12) })
        val output=card(root)
        output.addView(label(getString(R.string.ui_079),11f,muted))
        source=label(getString(R.string.ui_080),16f).apply { setTextIsSelectable(true) };output.addView(source)
        output.addView(label(getString(R.string.ui_081),11f,mint))
        result=label(getString(R.string.ui_082),settings.getInt("text_size",22).toFloat()).apply { setTypeface(null,Typeface.BOLD);setTextIsSelectable(true) };output.addView(result)
        output.addView(button(getString(R.string.ui_083)) { enqueueVoice(lastTranslation) })
        row(output,listOf(button(getString(R.string.ui_084)) { copyTranslation(lastTranslation) },button(getString(R.string.ui_085)) {
            if(lastTranslation.isBlank())message(getString(R.string.ui_086))
            else message(if(phraseBook.add(PhraseBook.Phrase(lastSource,lastTranslation))) getString(R.string.ui_087) else getString(R.string.ui_088))
        }))
        row(output,listOf(button(getString(R.string.ui_089)) { translateTypedText() },button(getString(R.string.ui_090)) { showSavedPhrases() }))
        val recent=card(root);recent.addView(label(getString(R.string.ui_091),11f,muted))
        history=label(getString(R.string.ui_092),14f);recent.addView(history)
        row(recent,listOf(button(getString(R.string.ui_093)) { shareHistory() },button(getString(R.string.ui_094)) { log.clear();history.text=getString(R.string.ui_095) }))
        root.addView(button(getString(R.string.ui_096)) {
            try { startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
            catch (_: Exception) { message(getString(R.string.ui_097)) }
        })
        val scroll=ScrollView(this).apply { isFillViewport=true;clipToPadding=false;addView(root) }
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll) { view,insets ->
            val bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);insets
        }
        setContentView(scroll)
        voice=VoicePlayer(this,{
            if(::voice.isInitialized&&voice.ready&&!restoredVoice) {
                restoredVoice=true
                settings.getString("default_voice",null)?.let { name -> voice.availableVoices(false).firstOrNull { it.name==name }?.let { voice.selectVoice(it) } }
            }
            refreshMetrics();if(active&&mode==Mode.MIC) scheduleListening() },
            { if(active&&mode==Mode.MIC&&!offlineMode&&!headphones.isChecked) pauseMicrophone() },{ message(it) })
        voice.rate=.9f+settings.getInt("rate_step",2)*.1f;voice.pitch=.85f+settings.getInt("pitch_step",3)*.05f
        voice.adaptiveSpeed=adaptiveReading
        selectMode(Mode.CAMERA)
        if(savedInstanceState!=null) {
            lastSource=savedInstanceState.getString("last_source").orEmpty();lastTranslation=savedInstanceState.getString("last_translation").orEmpty()
            lastSpeaker=savedInstanceState.getInt("last_speaker",0)
            if(lastTranslation.isNotBlank()) { source.text=lastSource;result.text=lastTranslation }
            savedInstanceState.getStringArrayList("recent_phrases")?.forEach { log.addLast(it) }
            if(log.isNotEmpty())history.text=log.take(4).joinToString("\n\n")
        }
        if(!region.isChecked)subtitleArea.visibility=View.GONE
        if(savedInstanceState!=null)selectMode(Mode.entries.getOrElse(savedInstanceState.getInt("mode",0)) { Mode.CAMERA })
        com.google.mlkit.common.model.RemoteModelManager.getInstance()
            .isModelDownloaded(TranslateRemoteModel.Builder(TranslateLanguage.TURKISH).build())
            .addOnSuccessListener { downloaded -> if(alive&&downloaded)prepareModel() }
        if(!settings.getBoolean("guide_seen",false)) { settings.edit().putBoolean("guide_seen",true).apply();showGuide() }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("mode",mode.ordinal)
        outState.putString("last_source",lastSource.take(12000));outState.putString("last_translation",lastTranslation.take(12000))
        outState.putInt("last_speaker",lastSpeaker)
        outState.putStringArrayList("recent_phrases",ArrayList(log.take(20).map { it.take(3000) }))
        super.onSaveInstanceState(outState)
    }
    private fun copyTranslation(text: String) {
        if(text.isBlank()) { message(getString(R.string.ui_098));return }
        (getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
            .setPrimaryClip(android.content.ClipData.newPlainText(getString(R.string.ui_099),text))
        message(getString(R.string.ui_100))
    }
    private fun translateTypedText() {
        if(!modelReady) { prepareModel();return }
        val field=EditText(this).apply {
            hint=getString(R.string.ui_101);setTextColor(textInk);setHintTextColor(muted)
            minLines=3;maxLines=6;setPadding(dp(20),dp(12),dp(20),dp(12))
            inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_089)).setView(field).setPositiveButton(getString(R.string.ui_102)) { _,_ ->
            val text=field.text.toString().trim()
            if(text.isNotBlank()) { stopSession();source.text=text;accept(text,SystemClock.elapsedRealtime()) }
        }.setNegativeButton(getString(R.string.ui_039),null).show()
    }
    private fun showSavedPhrases() {
        val items=phraseBook.all()
        if(items.isEmpty()) { message(getString(R.string.ui_103));return }
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_104))
            .setItems(items.map { it.translation.take(80) }.toTypedArray()) { _,index ->
                val item=items[index]
                AlertDialog.Builder(this).setTitle(getString(R.string.ui_105)).setMessage(item.source+"\n\n"+item.translation)
                    .setPositiveButton("Sesli oku") { _,_ -> enqueueVoice(item.translation) }
                    .setNeutralButton(getString(R.string.ui_084)) { _,_ -> copyTranslation(item.translation) }
                    .setNegativeButton(getString(R.string.ui_106),null).show()
            }.setNeutralButton(getString(R.string.ui_107)) { _,_ ->
                AlertDialog.Builder(this).setTitle(getString(R.string.ui_108)).setMessage(getString(R.string.ui_109))
                    .setPositiveButton(getString(R.string.ui_110)) { _,_ -> phraseBook.clear();message(getString(R.string.ui_111)) }
                    .setNegativeButton(getString(R.string.ui_039),null).show()
            }.setNegativeButton(getString(R.string.ui_106),null).show()
    }
    private fun showModelStatus() {
        val voices=if(::voice.isInitialized)voice.availableVoices(false).size else 0
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_112))
            .setMessage(getString(R.string.ui_113, (if(offline.ready()) getString(R.string.ui_114) else getString(R.string.ui_115)).toString(), (if(modelReady) getString(R.string.ui_114) else getString(R.string.ui_116)).toString(), (voices).toString()))
            .setPositiveButton(getString(R.string.ui_106),null).show()
    }
    private fun beginSession() {
        sessionStarted=SystemClock.elapsedRealtime();handler.removeCallbacks(sessionTick);handler.post(sessionTick)
        if(keepScreen) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private fun savePreferences() {
        if(!::autoVoice.isInitialized)return
        val area=subtitleArea.normalizedBounds()
        settings.edit().putFloat("area_left",area.left).putFloat("area_top",area.top).putFloat("area_right",area.right).putFloat("area_bottom",area.bottom).putBoolean("auto_voice",autoVoice.isChecked).putBoolean("region",region.isChecked)
            .putInt("speaker",selectedSpeaker).putBoolean("names",automaticSubtitleNames).apply()
    }
    private fun showGuide() {
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_117))
            .setMessage(getString(R.string.ui_118))
            .setPositiveButton(getString(R.string.ui_119),null).show()
    }
    private fun showSettings() {
        val view=vertical().apply { setPadding(dp(20),dp(12),dp(20),dp(12)) }
        view.addView(button(getString(R.string.ui_120)) {
            val values=arrayOf("system","light","dark")
            AlertDialog.Builder(this).setTitle(getString(R.string.ui_121)).setSingleChoiceItems(arrayOf(getString(R.string.ui_122),getString(R.string.ui_123),getString(R.string.ui_124)),values.indexOf(settings.getString("theme","system"))) { dialog,index ->
                stopSession();settings.edit().putString("theme",values[index]).apply();dialog.dismiss();recreate()
            }.show()
        })
        view.addView(button(getString(R.string.ui_125)) {
            val values=arrayOf("system","tr","en")
            AlertDialog.Builder(this).setTitle(getString(R.string.ui_126)).setSingleChoiceItems(arrayOf(getString(R.string.ui_122),getString(R.string.ui_127),getString(R.string.ui_128)),values.indexOf(settings.getString("language","system"))) { dialog,index ->
                stopSession();settings.edit().putString("language",values[index]).apply();dialog.dismiss();recreate()
            }.show()
        })
        view.addView(button(getString(R.string.ui_129)) {
            val sizes=intArrayOf(18,22,26)
            AlertDialog.Builder(this).setTitle(getString(R.string.ui_129)).setSingleChoiceItems(arrayOf(getString(R.string.ui_130),getString(R.string.ui_131),getString(R.string.ui_132)),sizes.indexOf(settings.getInt("text_size",22))) { dialog,index ->
                settings.edit().putInt("text_size",sizes[index]).apply();result.textSize=sizes[index].toFloat();dialog.dismiss()
            }.show()
        })
        view.addView(button(getString(R.string.ui_112)) { showModelStatus() })
        view.addView(button(getString(R.string.ui_136,recognitionModes()[speedMode])) { chooseRecognitionSpeed() })
        view.addView(CheckBox(this).apply {
            text=getString(R.string.ui_139);setTextColor(textInk);isChecked=adaptiveReading
            setOnCheckedChangeListener { _,checked -> adaptiveReading=checked;voice.adaptiveSpeed=checked;settings.edit().putBoolean("adaptive",checked).apply() }
        })
        view.addView(CheckBox(this).apply {
            text=getString(R.string.ui_140);setTextColor(textInk);isChecked=keepScreen
            setOnCheckedChangeListener { _,checked ->
                keepScreen=checked;settings.edit().putBoolean("screen",checked).apply()
                if(active&&checked)window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        })
        view.addView(label(getString(R.string.ui_141),13f,muted))
        view.addView(button(getString(R.string.ui_142)) {
            try { startActivity(Intent("com.android.settings.TTS_SETTINGS")) } catch (_: Exception) { message(getString(R.string.ui_143)) }
        })
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_144)).setView(ScrollView(this).apply { addView(view) }).setPositiveButton(getString(R.string.ui_106),null).show()
    }
    private fun chooseSpeakerStyle(slot: Int) {
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_145, (slot).toString()))
            .setSingleChoiceItems(resources.getStringArray(R.array.speaker_styles),profilePrefs.getInt("style_${slot}",(slot-1)%resources.getStringArray(R.array.speaker_styles).size)) { dialog,index ->
                profilePrefs.edit().putInt("style_${slot}",index).apply();dialog.dismiss()
                enqueueVoice("Bu konuşmacının ton ve okuma tarzı böyle duyulacak.",slot)
            }.setNegativeButton(getString(R.string.ui_106),null).show()
    }
    private fun shareHistory() {
        if(log.isEmpty()) { message(getString(R.string.ui_146));return }
        try { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type="text/plain";putExtra(Intent.EXTRA_TEXT,log.joinToString("\n\n"))
        },getString(R.string.ui_147))) } catch (_: Exception) { message(getString(R.string.ui_148)) }
    }
    private fun speakerVoiceName(slot: Int): String? {
        if(slot==0)return null
        val saved=profilePrefs.getString("voice_${slot}",null)
        val available=voice.availableVoices(allowNetworkVoice)
        if(saved!=null&&available.any { it.name==saved })return saved
        val chosen=available.getOrNull((slot-1)%available.size.coerceAtLeast(1))?.name
        if(chosen!=null)profilePrefs.edit().putString("voice_${slot}",chosen).apply()
        return chosen
    }
    private fun chooseSpeakerVoice(slot: Int) {
        val voices=voice.availableVoices(allowNetworkVoice)
        if(voices.isEmpty()) { message(getString(R.string.ui_149));return }
        val labels=voices.mapIndexed { i,v -> getString(R.string.ui_150, (i+1).toString(), (if(v.isNetworkConnectionRequired) getString(R.string.ui_151) else getString(R.string.ui_152)).toString()) }.toTypedArray()
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_153, (slot).toString()))
            .setItems(labels) { _,index ->
                profilePrefs.edit().putString("voice_${slot}",voices[index].name).apply()
                message(getString(R.string.ui_154, (slot).toString()))
                voice.add("Bu konuşmacının Türkçe sesi böyle duyulacak.",false,voices[index].name)
            }.setNegativeButton(getString(R.string.ui_106),null).show()
    }
    private fun chooseVoice() {
        val voices=voice.availableVoices(allowNetworkVoice)
        if(voices.isEmpty()) { message(getString(R.string.ui_155));return }
        val locale=java.util.Locale.forLanguageTag("tr-TR")
        val labels=voices.mapIndexed { index,v ->
            getString(R.string.ui_156, (index+1).toString(), (v.locale.getDisplayCountry(locale).ifBlank { getString(R.string.ui_127) }).toString(), (if(v.isNetworkConnectionRequired) getString(R.string.ui_151) else getString(R.string.ui_152)).toString())
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_157, (voices.size).toString()))
            .setItems(labels) { _,index ->
                if(voice.selectVoice(voices[index])) { settings.edit().putString("default_voice",voices[index].name).apply();voiceChoice.text=labels[index];message(getString(R.string.ui_158)) }
                else message(getString(R.string.ui_159))
            }.setNegativeButton(getString(R.string.ui_106),null).show()
    }
    private fun recognitionModes()=arrayOf(getString(R.string.ui_133),getString(R.string.ui_134),getString(R.string.ui_135))
    private fun chooseRecognitionSpeed() {
        val choices=recognitionModes()
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_137)).setSingleChoiceItems(choices,speedMode) { dialog,index ->
            stopSession();speedMode=index;settings.edit().putInt("speed_mode",index).apply()
            if(::speedChoice.isInitialized)speedChoice.text=getString(R.string.ui_136,choices[index])
            dialog.dismiss();message(getString(R.string.ui_138,choices[index]))
        }.setNegativeButton(getString(R.string.ui_039),null).show()
    }
    private fun message(text: String) { if(alive&&::status.isInitialized) status.text=text }
    private fun valid(token: Long)=alive&&token==epoch
    private fun selectMode(next: Mode) {
        stopSession();mode=next;updateSpeechDirection()
        tabs.forEachIndexed { index,b ->
            val selected=index==mode.ordinal;b.background=rounded(if(selected) mint else panel,14f)
            b.setTextColor(if(selected) ink else textInk)
        }
        previewBox.visibility=if(mode==Mode.CAMERA) View.VISIBLE else View.GONE
        micHero.visibility=if(mode==Mode.MIC) View.VISIBLE else View.GONE
        region.visibility=if(mode==Mode.CAMERA) View.VISIBLE else View.GONE
        subtitleArea.visibility=if(region.isChecked)View.VISIBLE else View.GONE
        headphones.visibility=if(mode==Mode.MIC) View.VISIBLE else View.GONE
        freeze.visibility=if(mode==Mode.CAMERA) View.VISIBLE else View.GONE
        start.text=when(mode) { Mode.CAMERA->getString(R.string.ui_042);Mode.MIC->getString(R.string.ui_160);Mode.IMAGE->getString(R.string.ui_161) }
        hint.text=when(mode) {
            Mode.CAMERA->getString(R.string.ui_162)
            Mode.MIC->getString(R.string.ui_163)
            Mode.IMAGE->getString(R.string.ui_164)
        }
        if(modelReady) message(getString(R.string.ui_165))
    }
    private fun prepareModel() {
        prepare.isEnabled=false;message(getString(R.string.ui_166))
        translator.downloadModelIfNeeded().continueWithTask { task ->
            if(!task.isSuccessful) throw (task.exception ?: IllegalStateException("Translation download failed"))
            reverseTranslator.downloadModelIfNeeded()
        }.addOnSuccessListener {
            if(alive) { modelReady=true;refreshReadiness();prepare.isEnabled=true;prepare.text=getString(R.string.ui_167);message(getString(R.string.ui_168)) }
        }.addOnFailureListener { if(alive) { prepare.isEnabled=true;message(getString(R.string.ui_169)) } }
    }
    private fun primaryAction(serverChecked: Boolean=false) {
        if(!visible||!alive)return
        if(active) { stopSession();message(getString(R.string.ui_044));return }
        if(!modelReady) { prepareModel();return }
        if(mode==Mode.MIC&&offlineMode&&!offline.ready()) { prepareSpeechModel();return }
        if(proService.enabled&&!serverChecked) {
            if(!proService.signedIn) { message(getString(R.string.account_required));return }
            if(accessChecking)return
            accessChecking=true;val token=epoch;message(getString(R.string.account_checking))
            proService.refresh { ok ->
                accessChecking=false
                if(valid(token)&&visible) {
                    refreshPro()
                    if(ok)primaryAction(true) else message(getString(R.string.account_offline))
                }
            }
            return
        }
        if(!billing.pro) {
            if(mode==Mode.IMAGE&&imageTrial.remaining==0 || mode!=Mode.IMAGE&&trial.remainingMillis==0L) { showPro();return }
            if(!settings.getBoolean("trial_split_started_v2",false)) {
                AlertDialog.Builder(this).setTitle(getString(R.string.pro_title)).setMessage(getString(R.string.pro_detail))
                    .setPositiveButton(getString(R.string.pro_start)) { _,_ -> settings.edit().putBoolean("trial_split_started_v2",true).apply();primaryAction() }
                    .setNegativeButton(getString(R.string.pro_close),null).show();return
            }
        }
        when(mode) {
            Mode.CAMERA->if(has(Manifest.permission.CAMERA)) startCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
            Mode.IMAGE->{ stopSession();photo.launch("image/*") }
            Mode.MIC->{
                AlertDialog.Builder(this).setTitle(getString(R.string.ui_013))
                    .setMessage(if(offlineMode) getString(R.string.ui_170) else getString(R.string.ui_171))
                    .setPositiveButton(getString(R.string.ui_160)) { _,_ -> if(has(Manifest.permission.RECORD_AUDIO)) startMicrophone() else microphonePermission.launch(Manifest.permission.RECORD_AUDIO) }
                    .setNegativeButton(getString(R.string.ui_039),null).show()
            }
        }
    }
    private fun has(permission: String)=ContextCompat.checkSelfPermission(this,permission)==PackageManager.PERMISSION_GRANTED
    @androidx.annotation.OptIn(markerClass=[ExperimentalGetImage::class, androidx.camera.view.TransformExperimental::class])
    private fun startCamera() {
        if(mode!=Mode.CAMERA||!visible||!alive)return
        stopSession();active=true;beginSession();start.text=getString(R.string.ui_172);val token=epoch;message(getString(R.string.ui_173))
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
                                var visibleSubtitle=""
                                if(valid(frameToken)&&active) {
                                    val selected=if(still) text.text else {
                                        val area=subtitleArea.bounds()
                                        val areaLines=text.textBlocks.flatMap { it.lines }.filter { line ->
                                            val box=line.boundingBox
                                            if(box==null||transform==null) false else {
                                                val mapped=RectF(box);transform.mapRect(mapped)
                                                val inArea=!region.isChecked||area.contains(mapped.centerX(),mapped.centerY())
                                                inArea&&subtitleFilter.eligible(line.text)
                                            }
                                        }
                                        visibleSubtitle=areaLines.joinToString("\n") { it.text }
                                        areaLines.filter { line ->
                                            val mapped=RectF(line.boundingBox!!);transform!!.mapRect(mapped)
                                            subtitleFilter.keep(line.text,mapped.centerX()/preview.width.coerceAtLeast(1),
                                                mapped.centerY()/preview.height.coerceAtLeast(1),SystemClock.elapsedRealtime())
                                        }.joinToString("\n") { it.text }
                                    }
                                    source.text=selected.ifBlank { getString(R.string.ui_174) }
                                    if(still) {
                                        stopSession()
                                        if(selected.isBlank()) message(getString(R.string.ui_175)) else accept(selected,SystemClock.elapsedRealtime())
                                    } else {
                                        recent.observeVisibility(visibleSubtitle,SystemClock.elapsedRealtime())
                                        gate.observe(if(selected.isBlank()&&visibleSubtitle.isNotBlank()) visibleSubtitle else selected,SystemClock.elapsedRealtime())?.let {
                                        if(accept(it,SystemClock.elapsedRealtime())) gate.commit(it)
                                        }
                                    }
                                }
                            }.addOnFailureListener { if(valid(frameToken)) message(getString(R.string.ui_176)) }
                             .addOnCompleteListener { proxy.close();if(valid(frameToken))busy=false }
                        }
                    }
                }
                val viewport=preview.viewPort ?: error("Camera preview layout is not ready")
                val group=UseCaseGroup.Builder().setViewPort(viewport).addUseCase(view).addUseCase(analysis).build()
                provider?.unbindAll();provider?.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,group);countLiveUse()
                message(getString(R.string.ui_177))
            } catch (_: Exception) { stopSession();message(getString(R.string.ui_178)) }
        },ContextCompat.getMainExecutor(this))
    }
    private fun accept(text: String,seenAt: Long): Boolean {
        if(text.isBlank()||!modelReady)return false
        if(active&&mode==Mode.CAMERA) {
            if(!recent.shouldSubmit(text,seenAt)) return true
            cameraRevision++;voice.clearWaiting()
            waiting.clear() // One current pending camera line, not a backlog of old screen text.
        }
        val segments=speakers.route(text,selectedSpeaker,mode!=Mode.MIC&&automaticSubtitleNames)
        val editor=profilePrefs.edit()
        speakers.bindings().forEach { (name,slot) -> editor.putString("speaker_${slot}",name) }
        editor.apply()
        var accepted=true
        for(segment in segments) {
            if(!waiting.offer(segment.text,seenAt,segment.slot,segment.name,if(active&&mode==Mode.MIC) speechDisplayRevision else -1)) {
                rejected++;accepted=false;message(getString(R.string.ui_179))
            }
        }
        pumpTranslation();refreshMetrics();return accepted
    }
    private fun pumpTranslation() {
        if(translating||partialBusy||!alive)return
        val line=waiting.take()?:return
        val token=epoch;val fromCamera=active&&mode==Mode.CAMERA;val cameraToken=cameraRevision;translating=true
        val translationTask=if(mode==Mode.MIC&&offlineMode&&line.text==cachedSource&&cachedTranslation.isNotBlank())
            com.google.android.gms.tasks.Tasks.forResult(cachedTranslation) else translateSmart(line.text)
        translationTask.addOnSuccessListener { translated ->
            if(valid(token) && (!fromCamera || cameraToken==cameraRevision&&SystemClock.elapsedRealtime()-line.seenAt<=1500)) {
                if(pendingTrialImage==token) {
                    if(!billing.pro)imageTrial.consume()
                    pendingTrialImage=null;saveTrialUsage(true);refreshPro()
                }
                completed++;lastLag=SystemClock.elapsedRealtime()-line.seenAt
                lastTranslation=translated;lastSource=line.text;lastSpeaker=line.speakerSlot
                if(active&&mode==Mode.MIC) { liveCaption.addLast(translated);while(liveCaption.size>4)liveCaption.removeFirst() }
                if(!(active&&mode==Mode.MIC)||line.displayRevision>=lastSpeechDisplayRevision) {
                    if(active&&mode==Mode.MIC)lastSpeechDisplayRevision=line.displayRevision
                    result.text=if(active&&mode==Mode.MIC) liveCaption.joinToString(" ") else if(line.speakerName.isBlank()) translated else getString(R.string.ui_180, (line.speakerName).toString(), (line.speakerSlot).toString(), (translated).toString())
                }
                log.addFirst("${if(line.speakerName.isBlank()) getString(R.string.ui_068, (line.speakerSlot).toString()) else line.speakerName} • ${line.text}\n${translated}")
                while(log.size>100) log.removeLast()
                history.text=log.take(4).joinToString("\n\n")
                if(autoVoice.isChecked) enqueueVoice(translated,line.speakerSlot)
                if(active&&mode==Mode.MIC&&offlineMode) message(getString(R.string.ui_181))
                else if(active&&mode==Mode.CAMERA) message(getString(R.string.ui_182))
                else if(!active) message(getString(R.string.ui_183))
            }
        }.addOnFailureListener {
            if(valid(token)) { gate.release(line.text);recent.forget(line.text);rejected++;message(getString(R.string.ui_184)) }
        }.addOnCompleteListener { if(valid(token)) { translating=false;refreshMetrics();pumpTranslation();if(mode==Mode.MIC) { scheduleListening();if(pendingPartial.isNotBlank()) schedulePartial(0) } } }
    }
    private fun enqueueVoice(text: String, speakerSlot: Int=lastSpeaker) {
        if(text.isBlank())return
        if(active&&mode==Mode.MIC&&!offlineMode&&!headphones.isChecked&&listening) {
            if(deferredSystemVoice.size<16)deferredSystemVoice.addLast(text to speakerSlot)
            else { rejected++;message(getString(R.string.ui_186)) }
            return
        }
        if(!voice.ready) { message(getString(R.string.ui_185));return }
        val style=if(speakerSlot==0) 0 else profilePrefs.getInt("style_${speakerSlot}",(speakerSlot-1)%resources.getStringArray(R.array.speaker_styles).size)
        if(!voice.add(text, active&&mode==Mode.CAMERA,if(translatingTurkish()) null else speakerVoiceName(speakerSlot),voice.pitch*SpeakerStyle.pitch(style),voice.rate*SpeakerStyle.rate(style))) { rejected++;message(voice.lastFailure ?: getString(R.string.ui_186)) }
        refreshMetrics()
    }
    private fun refreshReadiness() {
        if(!::readiness.isInitialized)return
        readiness.text=getString(R.string.model_readiness, (if(offline.ready()) getString(R.string.ui_114) else getString(R.string.ui_115)).toString(), (if(modelReady) getString(R.string.ui_114) else getString(R.string.ui_116)).toString())
    }
    private fun refreshMetrics() {
        refreshReadiness()
        if(!::metrics.isInitialized)return
        val sounds=if(::voice.isInitialized) voice.count else 0
        metrics.text=getString(R.string.ui_187, (if(active) getString(R.string.ui_188, ((SystemClock.elapsedRealtime()-sessionStarted)/1000).toString()) else "").toString(), (completed).toString(), (waiting.size + if(translating) 1 else 0).toString(), (sounds).toString(), (lastLag/1000.0).toString(), (rejected).toString())
    }
    private fun prepareSpeechModel() {
        stopSession()
        message(getString(R.string.ui_189))
        offline.prepare({ message(it);signalLabel.text=it },{
            message(getString(R.string.ui_190))
            signalLabel.text=getString(R.string.ui_191);refreshReadiness()
        },{ message(it);signalLabel.text=getString(R.string.ui_192) })
    }
    private fun startOfflineMicrophone() {
        if(!visible||!alive||mode!=Mode.MIC)return
        stopSession()
        if(!offline.ready()) { prepareSpeechModel();return }
        if(!modelReady) { prepareModel();return }
        active=true;beginSession();val token=epoch;start.text=getString(R.string.ui_172)
        signal.isIndeterminate=false;signal.progress=0;signalLabel.text=getString(R.string.ui_193)
        source.text=getString(R.string.ui_194);result.text=getString(R.string.ui_195)
        message(getString(R.string.ui_196))
        offline.start(speedMode,{
            if(valid(token)&&active) { countLiveUse();signalLabel.text=packChoice.text;message(getString(R.string.ui_198));handler.post(speechPulse) }
        },{ english,final ->
            if(valid(token)&&active&&mode==Mode.MIC) {
                processLiveSpeech(english,final)
            }
        },{ level -> if(valid(token)&&active) { signal.progress=level;meter.level=level } },{ error ->
            if(valid(token)&&active) { stopSession();message(error) }
        })
    }
    private fun processLiveSpeech(text: String,final: Boolean) {
        if(!active||mode!=Mode.MIC)return
        source.text=text
        if(text!=latestRecognized||final)speechDisplayRevision++
        if(!final&&latestRecognized.isBlank()&&text.isNotBlank())partialUtterance++
        latestRecognized=if(final) "" else text
        val update=liveSegments.update(text,final,SystemClock.elapsedRealtime())
        if(final||update.chunks.isNotEmpty()) partialUtterance++
        val preview=text
        if(preview!=pendingPartial||pendingPartialFinal!=final)partialRevision++
        pendingPartial=preview;pendingPartialFinal=final
        if(final) { handler.removeCallbacks(partialTask);partialScheduled=false }
        update.chunks.forEach { accept(it,SystemClock.elapsedRealtime()) }
        if(preview.isNotBlank()&&(preview!=cachedSource||final)) schedulePartial(80)
    }
    private fun schedulePartial(delay: Long) {
        if(!partialScheduled) { partialScheduled=true;handler.postDelayed(partialTask,delay) }
    }
    private fun translatePartial() {
        if(!alive||!active||mode!=Mode.MIC||pendingPartial.isBlank()||partialBusy||translating||waiting.size>0||!modelReady)return
        val token=epoch;val revision=partialRevision;val utterance=partialUtterance;val text=pendingPartial;val finalPreview=pendingPartialFinal;val previewStarted=SystemClock.elapsedRealtime();val displayRevision=speechDisplayRevision
        partialBusy=true
        val task=if(text==cachedSource&&cachedTranslation.isNotBlank()) com.google.android.gms.tasks.Tasks.forResult(cachedTranslation) else translateSmart(text)
        task.addOnSuccessListener { translated ->
            if(valid(token)) { cachedSource=text;cachedTranslation=translated }
            if(valid(token)&&active&&utterance==partialUtterance&&displayRevision>=lastSpeechDisplayRevision&&finalPreview==pendingPartialFinal&&(pendingPartial==text||pendingPartial.startsWith("$text "))) {
                lastSpeechDisplayRevision=displayRevision
                lastLag=SystemClock.elapsedRealtime()-previewStarted;refreshMetrics()
                result.text=if(finalPreview) translated else getString(R.string.ui_201, (translated).toString())
            }
        }.addOnCompleteListener {
            if(valid(token)) {
                partialBusy=false;pumpTranslation()
                if(revision!=partialRevision&&pendingPartial.isNotBlank()) schedulePartial(100)
            }
        }
    }
    private fun startMicrophone() {
        if(!visible||!alive||mode!=Mode.MIC)return
        if(mode!=Mode.MIC)return
        if(offlineMode) { startOfflineMicrophone();return }
        stopSession()
        if(!SpeechRecognizer.isRecognitionAvailable(this)) { message(getString(R.string.ui_202));return }
        active=true;beginSession();start.text=getString(R.string.ui_172);errors=0;emptySpeechRounds=0;preferSystemSpeech=!localSpeech.isChecked
        message(getString(R.string.ui_203));handler.post(speechPulse);listenOnce()
    }
    private fun canListen()=alive&&active&&mode==Mode.MIC&&!offlineMode&&has(Manifest.permission.RECORD_AUDIO)&&
        (headphones.isChecked||voice.count==0)
    private fun scheduleListening(delay: Long=120) {
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
                    if(current()) { countLiveUse();signalLabel.text=getString(R.string.ui_204);message(getString(R.string.ui_205)) }
                }
                override fun onBeginningOfSpeech() {
                    if(current()) { message(getString(R.string.ui_206));armSpeechDeadline(token,round,6000) }
                }
                override fun onRmsChanged(rmsdB: Float) {
                    if(current()) { signal.progress=((rmsdB+2f)*8f).toInt().coerceIn(0,100);signalLabel.text=getString(R.string.ui_207, (signal.progress).toString()) }
                }
                override fun onBufferReceived(buffer: ByteArray?) { }
                override fun onEndOfSpeech() {
                    if(current()&&!finalRequested) { message(getString(R.string.ui_208));armSpeechDeadline(token,round,1200) }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if(current()) partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                        if(it.isNotBlank()) {
                            val changed=it!=partialText;partialText=it;processLiveSpeech(it,false)
                            if(changed&&!finalRequested) armSpeechDeadline(token,round,2500)
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
                        if(emptySpeechRounds>=3) { stopSession();message(getString(R.string.ui_209)) }
                        else { message(getString(R.string.ui_210));scheduleListening(900) }
                    }
                    else {
                        val languageError=Build.VERSION.SDK_INT>=31 && (error==SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED||error==SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
                        if(onDevice&&languageError) {
                            preferSystemSpeech=true;errors=0
                            message(getString(R.string.ui_211))
                            scheduleListening(1000);return
                        }
                        errors++
                        if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS||languageError||errors>=3) {
                            stopSession();message(getString(R.string.ui_212, (error).toString()))
                        } else { message("Dinleme yeniden deneniyor (kod ${error})…");scheduleListening(1500) }
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE,if(translatingTurkish()) "tr-TR" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)
            })
        } catch (_: Exception) { stopSession();message(getString(R.string.ui_213)) }
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
        message(getString(R.string.ui_214))
        try { speech?.stopListening() } catch (_: Exception) { }
        val fallback=Runnable {
            if(valid(token)&&round==micRound&&active&&mode==Mode.MIC)
                finishSpeechChunk(partialText,token,round,true)
        }
        speechDeadline=fallback;handler.postDelayed(fallback,1200)
    }
    private fun finishSpeechChunk(text: String,token: Long,round: Long,interim: Boolean) {
        if(!valid(token)||round!=micRound||!active||mode!=Mode.MIC)return
        clearSpeechDeadline();micRound++;listening=false
        speech?.cancel();speech?.destroy();speech=null;partialText=""
        if(text.isNotBlank()) {
            source.text=text
            message(if(interim) getString(R.string.ui_215) else getString(R.string.ui_216))
            processLiveSpeech(text,true)
        } else {
            latestRecognized="";liveSegments.reset();pendingPartial="";partialUtterance++
            emptySpeechRounds++
            if(emptySpeechRounds>=2) {
                stopSession();message(getString(R.string.ui_217));return
            }
            message(getString(R.string.ui_218))
        }
        if(text.isNotBlank())emptySpeechRounds=0
        while(autoVoice.isChecked&&deferredSystemVoice.isNotEmpty()) {
            val job=deferredSystemVoice.removeFirst();enqueueVoice(job.first,job.second)
        }
        scheduleListening(120)
    }
    private fun pauseMicrophone() {
        if(latestRecognized.isNotBlank()) { latestRecognized="";liveSegments.reset();pendingPartial="";partialUtterance++ }
        handler.removeCallbacks(restart);clearSpeechDeadline();micRound++;listening=false;partialText=""
        speech?.cancel();speech?.destroy();speech=null
        if(active&&mode==Mode.MIC) message(getString(R.string.ui_219))
    }
    private fun stopSession(keepCurrentVoice: Boolean=false) {
        val now=SystemClock.elapsedRealtime();cameraTrial.stop(now);microphoneTrial.stop(now)
        captureClockStarted=false;pendingTrialImage=null
        saveTrialUsage(true);handler.removeCallbacks(trialTick)
        savePreferences();handler.removeCallbacks(sessionTick);window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        epoch++;active=false;busy=false;accessChecking=false;frozen=false;translating=false;gate.reset();waiting.clear();recent.clear();subtitleFilter.clear()
        provider?.unbindAll();pauseMicrophone();offline.stop();handler.removeCallbacks(speechPulse);latestRecognized="";liveSegments.reset();liveCaption.clear();speechDisplayRevision=0;lastSpeechDisplayRevision=-1;deferredSystemVoice.clear();partialRevision++;partialUtterance++;partialBusy=false;partialScheduled=false;pendingPartial="";pendingPartialFinal=false;cachedSource="";cachedTranslation="";handler.removeCallbacks(partialTask)
        if(::signal.isInitialized) { signal.isIndeterminate=false;signal.progress=0;meter.level=0;signalLabel.text=getString(R.string.ui_022) }
        if(::voice.isInitialized) { if(keepCurrentVoice)voice.clearWaiting() else voice.stop() }
        if(::start.isInitialized)start.text=when(mode) { Mode.CAMERA->getString(R.string.ui_042);Mode.MIC->getString(R.string.ui_160);Mode.IMAGE->getString(R.string.ui_161) }
        refreshMetrics()
    }
    override fun onResume() { super.onResume();visible=true;if(::billing.isInitialized)billing.refresh() }
    override fun onStop() { visible=false;proDialog?.dismiss();stopSession();super.onStop() }
    override fun onDestroy() {
        if(::billing.isInitialized)billing.close();if(::proService.isInitialized)proService.close();alive=false;epoch++;handler.removeCallbacks(restart);clearSpeechDeadline();provider?.unbindAll()
        speech?.destroy();if(::voice.isInitialized)voice.close()
        offline.close();recognizer.close();translator.close();reverseTranslator.close();super.onDestroy()
    }
}
