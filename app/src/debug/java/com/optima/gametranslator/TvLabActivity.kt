package com.optima.gametranslator

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.nl.translate.*

/** Debug-only UI/translation experiment. Does not capture another app, HDMI, audio or screen. */
class TvLabActivity: Activity() {
 private val handler=Handler(Looper.getMainLooper())
 private val english get()=resources.configuration.locales[0].language!="tr"
 private fun tr(t: String,e: String)=if(english)e else t
 private val samples=listOf("We need to leave before they find us.","Wait here. I will open the gate.","Stay together and follow the road.")
 private var client: Translator?=null
 private var visible=false
 private var running=false
 private var generation=0L
 private var index=0
 private var size=26f
 private var top=false
 private lateinit var caption: TextView
 private lateinit var source: TextView
 private lateinit var status: TextView
 private lateinit var stage: LinearLayout
 private lateinit var start: Button
 private fun text(value: String,sp: Float=16f)=TextView(this).apply {
  text=value;textSize=sp;setTextColor(Color.WHITE);setPadding(10,8,10,8)
 }
 private fun button(value: String,action: ()->Unit)=Button(this).apply {
  text=value;isFocusable=true;minHeight=64
  setOnFocusChangeListener { _,focused ->
   background=GradientDrawable().apply { setColor(if(focused)Color.rgb(45,111,106) else Color.rgb(27,43,63));cornerRadius=12f;setStroke(if(focused)4 else 1,Color.rgb(100,235,204)) }
   setTextColor(Color.WHITE)
  }
  setOnClickListener { action() }
 }
 override fun onCreate(state: Bundle?) {
  super.onCreate(state)
  WindowCompat.setDecorFitsSystemWindows(window,false)
  size=state?.getFloat("captionSize",26f)?:26f;top=state?.getBoolean("captionTop",false)?:false
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(32,20,32,24);setBackgroundColor(Color.rgb(11,17,29)) }
  root.addView(text("AI Game Translator · TV LAB",25f).apply { setTypeface(null,Typeface.BOLD) })
  root.addView(text(tr("DENEYSEL • Yalnız örnek metin. HDMI ve arka plan çevirisi etkin değil.","EXPERIMENTAL • Sample text only. HDMI and background translation are not enabled.")))
  status=text(tr("Kumanda: yön tuşları ve OK. İlk model hazırlığı internet gerektirebilir.","Remote: arrows and OK. Initial model preparation may require internet."));root.addView(status)
  stage=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;minimumHeight=210;setBackgroundColor(Color.rgb(18,32,49)) }
  source=text("",20f);caption=text(tr("Türkçe örnek altyazı burada görünecek.","Turkish sample subtitles will appear here."),size)
  caption.gravity=Gravity.CENTER;stage.addView(source);stage.addView(caption);root.addView(stage)
  fun row(vararg buttons: Button) {
   val line=LinearLayout(this)
   buttons.forEach { line.addView(it,LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,8,4,8) }) }
   root.addView(line)
  }
  start=button(tr("Örnek çeviriyi başlat","Start sample translation")) { if(running)stop() else begin() }
  row(start,button(tr("Uyumluluk bilgisi","Compatibility information")) { compatibility() })
  row(button(tr("Yazıyı büyüt","Larger subtitles")) { size=(size+2).coerceAtMost(40f);caption.textSize=size },
      button(tr("Yazıyı küçült","Smaller subtitles")) { size=(size-2).coerceAtLeast(18f);caption.textSize=size },
      button(tr("Üst / alt konum","Top / bottom")) { top=!top;placeCaption() })
  row(button(tr("Durdur","Stop")) { stop() },button(tr("Çıkış","Exit")) { finish() })
  root.addView(text(tr("Yakalama API’sinin bulunması HDMI görüntüsüne veya başka uygulamanın üstüne yazı çizimine erişimi kanıtlamaz. Gerçek TV testi gereklidir.","A capture API does not prove HDMI access or permission to draw above another app. Physical TV testing is required.")))
  val scroll=ScrollView(this).apply { addView(root);isFillViewport=true;clipToPadding=true;setBackgroundColor(Color.rgb(11,17,29)) }
  ViewCompat.setOnApplyWindowInsetsListener(scroll) { view,insets ->
   val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
   view.setPadding(bars.left,bars.top,bars.right,bars.bottom);WindowInsetsCompat.CONSUMED
  }
  setContentView(scroll);ViewCompat.requestApplyInsets(scroll);placeCaption();start.requestFocus()
 }
 private fun placeCaption() {
  stage.removeView(caption)
  stage.addView(caption,if(top)0 else stage.childCount)
 }
 private fun begin() {
  if(!visible)return
  running=true;val ticket=++generation;index=0
  start.text=tr("Örneği durdur","Stop sample")
  status.text=tr("İngilizce → Türkçe model hazırlanıyor…","Preparing English → Turkish model…")
  val translator=client?:Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.TURKISH).build()).also { client=it }
  translator.downloadModelIfNeeded().addOnSuccessListener {
   if(current(ticket))next(ticket)
  }.addOnFailureListener {
   if(current(ticket)) { stop();status.text=tr("Model hazırlanamadı. Bağlantıyı kontrol edip yeniden deneyin.","Model preparation failed. Check your connection and retry.") }
  }
 }
 private fun current(ticket: Long)=visible&&running&&ticket==generation
 private fun next(ticket: Long) {
  if(!current(ticket))return
  val original=samples[index%samples.size];source.text=original
  status.text=tr("Örnek metin çevriliyor; canlı ekran yakalanmıyor.","Translating sample text; no live screen is captured.")
  client?.translate(original)?.addOnSuccessListener {
   if(current(ticket)) { caption.text=it;index++;handler.postDelayed({ next(ticket) },3500) }
  }?.addOnFailureListener {
   if(current(ticket)) { stop();status.text=tr("Örnek çevrilemedi. Yeniden başlatabilirsiniz.","Sample translation failed. You can restart.") }
  }
 }
 private fun stop() {
  generation++;running=false;handler.removeCallbacksAndMessages(null)
  if(::start.isInitialized)start.text=tr("Örnek çeviriyi başlat","Start sample translation")
  if(::status.isInitialized)status.text=tr("Durduruldu. Ekran, mikrofon veya HDMI kaydı yapılmıyor.","Stopped. No screen, microphone or HDMI capture is running.")
 }
 private fun compatibility() {
  val tv=(getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType==Configuration.UI_MODE_TYPE_TELEVISION
  val projection=getSystemService(Context.MEDIA_PROJECTION_SERVICE) is MediaProjectionManager
  val overlay=Settings.canDrawOverlays(this)
  status.text=tr("TV arayüzü: $tv • Yakalama servisi: $projection • Üste çizim izni: $overlay\nHDMI: doğrulanmadı. Bu kontrol izin istemez veya kayıt başlatmaz.",
   "TV interface: $tv • Capture service: $projection • Overlay permission: $overlay\nHDMI: unverified. This check does not request permission or start capture.")
 }
 override fun onStart() { super.onStart();visible=true }
 override fun onStop() { visible=false;stop();super.onStop() }
 override fun onSaveInstanceState(state: Bundle) { state.putFloat("captionSize",size);state.putBoolean("captionTop",top);super.onSaveInstanceState(state) }
 override fun onDestroy() { stop();client?.close();client=null;super.onDestroy() }
}
