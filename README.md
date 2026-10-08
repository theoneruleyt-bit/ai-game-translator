# Android 0.3 — Akan altyazı düzeltmesi

Ana akış: arka kamera → İngilizce görüntü yazısı → yerel Türkçe çeviri → Android Türkçe sesli okuma. Fotoğraf/ekran görüntüsü ve tek kare modu korunur. Oyun sesini dinleme eklenmedi.

## Düzeltilen nedenler
0.2 her 450 ms'de OCR yapıp 600 ms birebir aynı metin bekliyordu; pratikte en az üç eşleşen gözlem ve yaklaşık 900 ms gerekiyordu. Dahası çeviri bitince mevcut OCR metni değişmişse sonuç tamamen atılıyordu. Akan altyazıda bu iki koşul çeviri/ses kaybına neden olabilir.

0.3 minimum OCR aralığı 150 ms (gerçek hız cihaz/OCR süresine bağlı). En az iki uyumlu gözlem ve 150 ms kararlılık gerekir. Uzun metinde küçük harf OCR hatalarına tolerans; noktalama/büyük harf farklılıklarını normalleştirme. Sayı değişiklikleri ve kısa kelime değişiklikleri ayrı metin sayılır. Bu sezgisel filtre çok benzer iki gerçek repliği birleştirebilir; cihaz testi gerekir.

Çeviri işlenirken altyazı değişse de sonuç 3.5 sn yaş sınırı içinde gösterilip okunabilir. İşlenmekte olan bir çevirinin yanında yalnızca en yeni kararlı altyazı bekler; eski bekleyen kayıt değiştirilebilir. Bekleyen kayıt 3 sn sonra geçersiz olur. Bu sınırlı kuyruk her repliğin çevrileceği anlamına gelmez; hızlı akışta aradaki replikler atlanabilir. Anlık/sıfır gecikme vaadi yoktur.

Canlı kaynak metin her başarılı OCR'da gösterilir. Böylece yakalama mı çeviri mi başarısız ayrıştırılabilir. Alt %45 filtresi başlangıçta kapalı; TV kadrajı nedeniyle altyazı kaçırılmasın. İsteğe bağlı açılabilir. TTS halen cihazın Türkçe sesi; doğal bulut ses sağlayıcısı henüz eklenmedi. Yeni sonuç eskisini keser, ses kuyruğu birikmez.

## Yükleme
ZIP içeriğini mevcut depo köküne kopyala, aynı dosyaları değiştir. .github klasörünü de kopyala. 0.1'den kalmış CaptureService.kt varsa sil.

```bash
git add .
git commit -m "Fix live subtitle detection and translation delivery"
git push
```

GitHub Actions → Android APK → yeşil çalışma → Artifacts → AI-Game-Translator-0.3-live-debug → app-debug.apk. VersionCode3; aynı imzalı kurulum üzerine güncellenir.

## Gerçek kullanım testi
1. Modeli hazırla; kamerayı başlat. Alt %45 seçeneğini ilk testte kapalı tut.
2. Videoyu duraklatmadan 2–3 saniye görünen İngilizce altyazılarla dene. Kaynak metin, Türkçe sonuç, ses ayrı ayrı takip et.
3. Aynı replik 10 sn kalınca sürekli okunmamalı.
4. Hızlı altyazıda replik atlama, kesilen ses veya gecikme varsa not et; cihaz modeli/Android sürümüyle bildir.
5. Galeriden fotoğraf/ekran görüntüsü ve tek kare modunu tekrar dene.
6. Durdur veya uygulamadan ayrıl; bekleyen çeviri sonradan ses çıkarmamalı.

13 davranış testi CI'ya dahil: hızlı replik, tek kare gürültü, noktalama/harf jitter, sayı farkı, kısa kelime farkı, tekrar/boşluk, yeni replik, başarısız çeviri yeniden denemesi; kuyruk değiştirme/yaş sınırı/durdurma. Bu ortamda SDK/Gradle/Kotlin compiler bulunmadığından testler ve APK derlemesi çalıştırılmadı. Yeni CI ve gerçek telefon testi gerekli. Önceki 0.2 kullanıcı tarafından yeşil/telefon çalışıyor olarak doğrulandı; yeni canlı akış henüz doğrulanmadı.

İlk model indirimi internet gerektirir. OCR ve çeviri yerel; Android TTS motoru çevrimdışı Türkçe sese göre değişebilir. Görüntü kayıt/yükleme eklenmedi. Uygulamadan ayrılınca kamera/ses durur.
