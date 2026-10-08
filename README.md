# AI Game Translator 0.4 — Kamera, Dinle, Görsel

Oyun ve video için Android kamera altyazı çevirisi, mikrofon konuşma çevirisi deneyi, fotoğraf/ekran görüntüsü modu. Kaynak İngilizce, hedef Türkçe.

## Replik ve ses düzeltmesi
0.3 yeni çeviri gelince QUEUE_FLUSH ile mevcut okumayı kesiyordu; bekleyen altyazının sadece en yenisini tutuyor, eski sonuçları yaş sınırıyla eliyordu. 0.4 alınan çevirileri FIFO sırasına koyar. Kabul edilen replikler yeni replikle değiştirilmez veya zaman yüzünden silinmez. Ses motoru bir repliği bitirdikten sonra sıradakini okur; yeni çeviri mevcut sesi kesmez. Açık Durdur, mod değişimi, ses kutusunu kapatma veya uygulamadan ayrılma sesi bilerek durdurur.

Sınır: çeviri kuyruğu 32, ses kuyruğu 64 parça. Dolduğunda yeni kayıt alınmayabilir; sıra/işlem uyarısı sayacı ve mesajı görünür. Kaynak metnin OCR veya konuşma tanımada hiç algılanmaması bu sayaçla ölçülemez; sayaç benzersiz atlanan replik sayısı değil, hata/dolu kuyruk olay sayısıdır. Çok hızlı konuşmada okumayı hiç atlamamak ve aynı anda sıfır gecikme sağlamak mümkün değildir; yeni kuyruk tamamlamayı önceler ve gecikme artabilir. Okuma hızı 0.9–1.5 aralığında ayarlanır. OCR kararlılık filtresi hâlâ iki gözlem ister; çok kısa veya benzer replikler kaçabilir.

## Dinle modu — deneysel
RECORD_AUDIO izniyle telefon mikrofonundan İngilizce oyun/video konuşması alınır. Android SpeechRecognizer kısmi metni gösterir, kesinleşmiş konuşma sonucunu çevirir. Konuşma bitişine/model yanıtına bağlı gecikme vardır; sözcük sözcük anlık dublaj veya garantili kesintisiz streaming değildir. Android bu API'yi kesintisiz tanıma için tasarlamaz; oturumlar yeniden başlatılırken kısa boşluklar oluşabilir.

Android31+ ve uygun servis varsa önce cihazdaki tanıma kullanılır. İngilizce desteklenmiyorsa kullanıcıya gösterilerek sistem konuşma servisine dönülür; internet kullanabilir. İlk başlatmada bunu anlatan onay ekranı vardır. Ses dosyası uygulamada kaydedilmez; sistem servisi sesi işlemek üzere dışarı gönderebilir. Play gizlilik/veri güvenliği incelemesi yayın öncesi gerekir.

Telefonun kendi Türkçe sesini yeniden tanımaması için varsayılan mod Türkçe okurken mikrofonu durdurur. Bu sırada oyundaki konuşma yakalanmaz. Türkçe sesi gerçekten kulaklıkla dinliyorsan 'Türkçe sesi kulaklıkla dinliyorum' seçeneği okuma boyunca mikrofonu açık tutar. Kulaklık bağlı olmadan bunu açmak çeviri geri beslemesine neden olabilir. Bluetooth kulaklığın mikrofon yönlendirmesi oyun sesini iyi duymayabilir; cihaz/rota testi gerekir. Bu ilk deney dijital oyun sesi yakalama veya yankı giderme motoru içermez.

## Arayüz / simge
Koyu lacivert ve mint tema, Kamera / Dinle / Görsel sekmeleri, büyük önizleme, net kaynak/çeviri kartı, son dört replik, sıra sayıları ve son çeviri gecikmesi. Model hazırlama tek başlangıç adımıdır. Yeni imagegen simgesi adaptive Android launcher ve ekran marka alanına entegre edilir. Son kullanıcı düzeltmesi: oyun ve video canlı çeviri odağı.

Android TTS'nin kurulu çevrimdışı Türkçe seslerinden kaliteye göre tercih yapılır. Hız ayarı ve ses ayarlarına erişim eklenir. Yeni premium doğal TTS veya karakter aksanı eklenmedi; ses kalitesi kurulu motora bağlıdır. Bulut sesi/API anahtarı fiyat seçimi yapılmadan gömülmez.

## GitHub yükleme
ZIP içeriğini mevcut depo köküne kopyala; aynı dosyaları değiştir, .github klasörünü de taşı.
Eski 0.3'ten şu iki dosyayı sil (yeni FIFO sınıfı bunların yerini alır):
- app/src/main/java/com/optima/gametranslator/LatestLine.kt
- app/src/test/java/com/optima/gametranslator/LatestLineTest.kt
0.1'den CaptureService.kt kaldıysa onu da sil.

```bash
git add .
git commit -m "Add microphone translation, FIFO speech and redesigned UI"
git push
```

Actions → Android APK → başarılı çalışma → Artifacts → AI-Game-Translator-0.4-studio-debug → app-debug.apk. versionCode4. minAndroid29, compile/target35; hedef SDK mağaza yayını öncesi yeniden doğrulanır. GitHub→APK/AAB akışı, Android Studio/Hermes gerektirmeden korunur. Gradle wrapper yok; workflow Gradle8.11.1 kurar.

## Telefon kabul testi
1. Kamera modunda model hazırla. Birkaç replik peş peşe gelirken Türkçe ses yarıda kesilmeden sırayla okunmalı; hız/ sıra gecikmesini kontrol et.
2. Sabit tek replikte tekrarlı okuma olmamalı. Son çeviriyi tekrar oku bilinçli olarak yeni bir ses kaydı ekler.
3. Dinle modunda sessiz ortamda İngilizce konuşma/videosu dene. Mikrofon izni, kısmi İngilizce metin, kesin Türkçe sonuç ve ses ayrı kontrol edilir.
4. Kulaklıksız testte ses sırasında mikrofon durmalı, sonra yeniden açılmalı. Kulaklıkla testte kendi çevirisini tekrar yakalamamalı.
5. Fotoğraf / ekran görüntüsü ve kamera tek kare modunu kontrol et.
6. Durdur, mod değiştir, uygulamadan ayrıl: eski sonuç veya ses sonra gelmemeli.
7. İnternet yok, İngilizce tanıma paketi yok, Türkçe TTS yok: açık hata olmalı. Cihaz modeli/Android sürümünü bildir.

## Doğrulama
14 unit test bildirimi (10 metin kararlılığı + 4 FIFO davranışı). Kamera/ses/izin yaşam döngüsü cihaz testi ister. Bu ortamda Android SDK/Gradle/Kotlin compiler yok: APK, test ve lint çalıştırılmadı. XML/YAML/ZIP ve kaynak akış kontrolleri yapılır; yeni GitHub CI sonucu beklenir. Önceki 0.3 kullanıcı telefonunda çalıştı; 0.4 henüz doğrulanmadı.

Resmi kaynaklar:
https://developer.android.com/reference/android/speech/SpeechRecognizer
https://developer.android.com/reference/android/speech/tts/TextToSpeech
https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener

## Simge tasarımı
Yerleşik imagegen ile kaynak simgeye düzenleme. Prompt özeti: aynı lacivert/mint konuşma balonunu koru; video oynatma işareti, iki altyazı çizgisi ve küçük oyun yön tuşuyla oyun/video canlı çevirisini anlat; yazısız, merkezi güvenli alanda, küçük boyutta okunaklı Android simgesi.
