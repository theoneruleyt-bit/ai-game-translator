# 0.17 — Ses akışı, otomatik tempo ve doğal ses pilotu

## Telefonda hemen kullanılabilen değişiklikler

- Mikrofon yakalama ayrı öncelikli iş parçacığında; çözümleme ses okumayı bekletmez. İki saniyelik tampon dolarsa tanıma durur ve hata gösterir; ses kaybı gizlenmez. Çok yavaş cihazda model değiştirmek gerekebilir.
- Otomatik ses hızı artık yalnız kuyruk adedine değil, metnin hece/kelime ve noktalama tahminine, bekleyen ses süresine, repliğin yaşına ve ilk geçici tanıma ile kesin sonuç arasındaki tahmini kaynak süresine bakar. Bu kaynak süresi gerçek kelime zaman damgası değildir.
- Tamamlanan sesin gerçek oynatma süresi tahmini kalibre eder. Hız 0,85–1,50 aralığında yumuşak değişir; çalışan replik yeni çeviri nedeniyle kesilmez. Manuel tempo seçimi otomatiği kapatır.
- Ayarlar → Seslendirme temposu: Otomatik, Yavaş, Normal, Hızlı, Çok hızlı. Manuel hız/perde değişikliği bir sonraki replikten uygulanır.
- Ekranda mevcut tempo ve tahmini bekleyen ses süresi görünür.
- Kamera 3 dakika, dinleme 3 dakika, görsel 10 adet; Pro fiyatları korunur. Aynı debug imzası, versionCode 24.

## Yapılandırma gerektiren pilot

Ayarlar → Doğal internet sesi · Pilot bağlantı. Hizmet adresi HTTPS olmalı; erişim kodu yalnız açık uygulama oturumunda tutulur. Çeviri metni internet hizmetine gönderilir. Sağlayıcı anahtarları APK'ya girmez.

Paket içinde çalışan istemci ve Python hizmeti vardır: Translation LLM metin çevirisi; Gemini-TTS kadın Kore / erkek Puck sesleri; doğal, sakin, enerjik, gergin ve meraklı anlatım profilleri. Doğal profil duyguyu yalnız metin belirgin biçimde ima ederse kullanması için modele talimat verir. Kaynak sesin duygusunu ölçmez. Bu profiller ses klonlama veya canlı konuşmacı kimliği tespiti değildir. Wavenet alternatifi duygu talimatı desteklemez.

Ücretli bulut çevirisi yalnız kesinleşen replik için çağrılır; geçici yazılı önizleme mevcut cihaz içi motorda kalır. Hizmet hatasında kullanıcı bilgilendirilir ve cihaz içi motor denenir. Uzun metin doğal ses için parçalara ayrılır; gerçek ses üretimi ve bağlantı gecikmesi hâlâ vardır.

**Hizmet hesabı, API'ler, faturalandırma ve HTTPS sunucu kurulmadı. Bunlar olmadan doğal internet sesi etkinleşmez.** Canlı bulut STT, dijital oyun sesi yakalama, uzun konuşma bağlamı/glossary ve kaynak duygusu tespiti henüz bu pakette yoktur. Bu sürüm GTA 6 kalitesinin doğrulandığı son ürün değildir.

## GitHub kurulumu

ZIP içindeki Android_Game_Translator_0_17 klasörünün içeriğini mevcut depo köküne kopyalayın. Kendi .git klasörünüzü ve üretim anahtarlarınızı koruyun.

```bat
git add .
git commit -m "Add separate audio capture, adaptive tempo and expressive voice gateway pilot"
git push
```

Actions: debug APK, unit test, debug/release lint, unsigned AAB ve 16 KB native kontrolleri. Başarılı çalışmada Releases içindeki özel draft paketinden APK alınır. Yeni sürümün CI sonucu henüz bilinmiyor.

## Doğrulama

Yerelde 25 Python servis testi + 5 native denetleyici testi geçti. Android SDK/Gradle bu ortamda olmadığı için Kotlin testleri ve APK derlemesi yerelde çalıştırılmadı. Beş yeni Kotlin tempo testi Actions'ta çalışacak. Gerçek Google hizmeti ve gerçek telefon sesi test edilmedi.

Telefon testi: hızlı modelle 5 dakika konuşma; replik kaybı/tekrar ve birikme; otomatik/manuel hız karşılaştırması; durdur-başlat; kulaklıkla ses geri beslemesi; kamera sabit altyazı. Telefon hoparlörü mikrofona dönerse tanıma kendi Türkçe sesini yakalayabilir, kulaklık önerilir.
