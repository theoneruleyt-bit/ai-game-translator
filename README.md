# Android 0.6 — Altyazı alanı ve HUD filtresi

0.5 konuşma bekleme düzeltmelerini, Kamera/Dinle/Görsel modlarını ve oyun/video simgesini içerir. Yeni özellik yalnızca canlı kameraya uygulanır.

Kamera önizlemesindeki yeşil çerçeveyi parmağınla bir köşeden karşı köşeye sürükleyerek yeniden çiz. Yalnızca altyazıyı içine al; dakika, video adı, kanal/oyuncu adı ve HUD simgelerini dışarıda bırak. Seçili alan varsayılan açık. Çerçeve sıfırlanabilir veya 'Yalnızca seçili altyazı alanını oku' kapatılarak tüm görüntü okunabilir. Alan değişiminde eski çeviri/ses temizlenir; kamera devam eder. Seçeneği açıp kapatınca kamera yeniden başlatılır.

OCR satırları CameraX ortak ViewPort ve CoordinateTransform ile önizlemeye eşlenir; satır merkezi alan içindeyse alınır. Telefonun açı/rotasyon/cihaz varyasyonlarına göre gerçek test gerekir. Galeri ve tek karede alan/HUD filtresi uygulanmaz.

Ek filtreler: tek başına dakika/süre, sayısal sayaç, HP/MP/XP/FPS/PING/AMMO + sayı satırları atlanır. Aynı konumda 8 saniye değişmeyen satır katkısı durdurulur; bu satır başlangıçta bir kez okunabilir. İsimlerin kesin olarak ayırt edildiği iddia edilmez. Aynı bölgede uzun süre duran gerçek altyazı da sabit sayılabilir; bu yüzden temel çözüm doğru alan seçimidir. Ekrandan ayrılıp dönen metin yeniden değerlendirilebilir.

## Yükleme
ZIP içeriğini depo köküne kopyala ve dosyaları değiştir; .github klasörünü de taşı. 0.5'ten yeni silinecek dosya yok.

```bash
git add .
git commit -m "Add selectable subtitle area and HUD filtering"
git push
```

Actions → yeşil çalışma → Artifacts → AI-Game-Translator-0.6-subtitle-area-debug → app-debug.apk.

## Test
Modeli hazırla, kamerayı başlat. Yeşil çerçeveyi yalnızca altyazının bulunduğu satırlara çiz. Video süre/isim alanını ve oyun HUD'ını dışarıda bırak. Akan altyazıda İngilizce kaynak alanının bunları içermediğini kontrol et. İki farklı repliğin değişmesini ve tekrar döngüsü olmamasını dene. Dinle, galeri ve tek kareyi ayrıca kontrol et.

22 test bildirimi: önceki 18 + dört HUD/sabit satır testi. XML/YAML/ZIP kontrolü yapılır; bu ortamda SDK/Gradle/Kotlin olmadığı için derleme/test/lint ve cihaz eşleme doğrulanmadı. Yeni CI sonucu gerekir. Mikrofon modunun sistem konuşma servisi, kulaklık ve bekleme sınırları aynen geçerlidir; kesintisiz streaming garantisi yok.

Teknik kaynak: https://developer.android.com/reference/androidx/camera/view/transform/CoordinateTransform
