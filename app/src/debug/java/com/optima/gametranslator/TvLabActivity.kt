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
 private val prefs by lazy { getSharedPreferences("tv_lab_ui",MODE_PRIVATE) }
 private var shade=190
 private var centered=true
 private var size=26f
 private var top=false
 private lateinit var caption: TextView
 private lateinit var source: TextView
 private lateinit var status: TextView
 private lateinit var stage: LinearLayout
 private lateinit var start: Button
 private fun dp(v: Int)=(v*resources.displayMetrics.density).toInt()
 private fun surface(color: Int)=GradientDrawable().apply { setColor(color);cornerRadius=dp(16).toFloat();setStroke(dp(1),Color.rgb(44,65,85)) }
 private fun text(value: String,sp: Float=16f)=TextView(this).apply {
  text=value;textSize=sp;setTextColor(Color.rgb(234,242,250));setPadding(dp(12),dp(10),dp(12),dp(10))
 }
 private fun button(value: String,action: ()->Unit)=Button(this).apply {
  text=value;isAllCaps=false;textSize=16f;isFocusable=true;minimumHeight=dp(58)
  setPadding(dp(14),dp(10),dp(14),dp(10));setTextColor(Color.WHITE)
  backgroundTintList=null
  fun paint(focused: Boolean) { background=surface(if(focused)Color.rgb(20,111,99) else Color.rgb(25,43,63)).apply { setStroke(dp(if(focused)3 else 1),if(focused)Color.rgb(100,235,204) else Color.rgb(56,80,104)) } }
  paint(false);setOnFocusChangeListener { _,focused -> paint(focused) }
  setOnClickListener { action() }
 }
 private fun saveAppearance() { prefs.edit().putFloat("size",size).putBoolean("top",top).putInt("shade",shade).putBoolean("centered",centered).apply() }
 private fun appearance() {
  caption.textSize=size;caption.gravity=if(centered)Gravity.CENTER else Gravity.START
  caption.background=surface(Color.argb(shade,3,9,18));saveAppearance()
 }
 override fun onCreate(state: Bundle?) {
  super.onCreate(state)
  WindowCompat.setDecorFitsSystemWindows(window,false)
  size=state?.getFloat("captionSize")?:prefs.getFloat("size",26f);top=state?.getBoolean("captionTop")?:prefs.getBoolean("top",false)
  shade=prefs.getInt("shade",190);centered=prefs.getBoolean("centered",true)
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(24),dp(20),dp(24),dp(24));background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.rgb(8,16,29),Color.rgb(18,37,54))) }
  root.addView(text("TV LAB  /  AI Game Translator",28f).apply { setTypeface(null,Typeface.BOLD) })
  root.addView(text(tr("DENEYSEL • Yalnız örnek metin. HDMI ve arka plan çevirisi etkin değil.","EXPERIMENTAL • Sample text only. HDMI and background translation are not enabled.")))
  status=text(tr("Kumanda: yön tuşları ve OK. İlk model hazırlığı internet gerektirebilir.","Remote: arrows and OK. Initial model preparation may require internet."));root.addView(status)
  stage=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;minimumHeight=dp(230);setPadding(dp(12),dp(12),dp(12),dp(12));background=surface(Color.rgb(18,32,49)) }
  source=text(tr("ÖRNEK ALTYAZI • İNGİLİZCE → TÜRKÇE","SAMPLE SUBTITLES • ENGLISH → TURKISH"),18f);caption=text(tr("Türkçe örnek altyazı burada görünecek.","Turkish sample subtitles will appear here."),size)
  caption.gravity=Gravity.CENTER;stage.addView(source);stage.addView(caption);root.addView(stage)
  fun row(vararg buttons: Button) {
   val count=if(resources.configuration.screenWidthDp<600)1 else 3
   buttons.toList().chunked(count).forEach { group ->
    val line=LinearLayout(this)
    group.forEach { line.addView(it,LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(dp(4),dp(6),dp(4),dp(6)) }) }
    root.addView(line)
   }
  }
  root.addView(text(tr("ÖNİZLEME","PREVIEW"),12f).apply { setTextColor(Color.rgb(100,235,204)) })
  start=button(tr("Örnek çeviriyi başlat","Start sample translation")) { if(running)stop() else begin() }
  row(start,button(tr("Uyumluluk bilgisi","Compatibility information")) { compatibility() })
  row(button(tr("Yazıyı büyüt","Larger subtitles")) { size=(size+2).coerceAtMost(40f);appearance() },
      button(tr("Yazıyı küçült","Smaller subtitles")) { size=(size-2).coerceAtLeast(18f);appearance() },
      button(tr("Üst / alt konum","Top / bottom")) { top=!top;placeCaption();saveAppearance() })
  root.addView(text(tr("ALTYAZI GÖRÜNÜMÜ","SUBTITLE APPEARANCE"),12f).apply { setTextColor(Color.rgb(100,235,204)) })
  row(button(tr("Arka planı değiştir","Change background")) { shade=if(shade==190)240 else if(shade==240)110 else 190;appearance() },
      button(tr("Hizalamayı değiştir","Change alignment")) { centered=!centered;appearance() },
      button(tr("Görünümü sıfırla","Reset appearance")) { size=26f;shade=190;centered=true;top=false;appearance();placeCaption() })
  row(button(tr("Durdur","Stop")) { stop() },button(tr("Çıkış","Exit")) { finish() })
  root.addView(text(tr("Yakalama API’sinin bulunması HDMI görüntüsüne veya başka uygulamanın üstüne yazı çizimine erişimi kanıtlamaz. Gerçek TV testi gereklidir.","A capture API does not prove HDMI access or permission to draw above another app. Physical TV testing is required.")))
  val scroll=ScrollView(this).apply { addView(root);isFillViewport=true;clipToPadding=true;setBackgroundColor(Color.rgb(11,17,29)) }
  ViewCompat.setOnApplyWindowInsetsListener(scroll) { view,insets ->
   val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
   view.setPadding(bars.left,bars.top,bars.right,bars.bottom);WindowInsetsCompat.CONSUMED
  }
  setContentView(scroll);ViewCompat.requestApplyInsets(scroll);placeCaption();appearance();start.requestFocus()
 }
 private fun placeCaption() {
  stage.removeView(caption)
  stage.gravity=if(top)Gravity.TOP else Gravity.BOTTOM
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

