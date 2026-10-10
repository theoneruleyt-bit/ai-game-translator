# 0.15 son inceleme — Sunucusuz ilk sürüm

## Sonuç
0.14 için kullanıcının bildirdiği GitHub sonucu yeşildi. Son kaynak incelemesinde API 35 hedefi mağaza için güncel değildi. 0.15 compile/target API 36 ve bunu destekleyen AGP 8.10.1 kullanır; Gradle 8.11.1 ve JDK 17 korunur. Önceki debug imzası korunur.

**0.15 henüz Android'de derlenmedi; bu ortamda SDK/Gradle yok. Mağazaya hazır olduğu onaylanmadı.** Yeni GitHub sonucu ve telefon testleri gerekiyor.

## Kaynaktan doğrulananlar
- Kamera ve mikrofon izinleri gerektiğinde istenir; galeri için geniş depolama izni yoktur.
- Kamera/metin ve mikrofon/Vosk/ML Kit çeviri yereldir. Sunucu/OAuth alanları boştur; hesap girişi zorunlu olmaz. Varsayılan demo telefonda kalır.
- Kamera için önceki oturum çıktıları elenir; bekleyen sesler en yeni replikle değiştirilir; başlanmış ses korunur.
- Arka plan kaydı veya foreground service yoktur; onStop kamera, mikrofon, TTS ve sayacı durdurur.
- Ortak demo 180 saniye, indirme/izin bekleme dışında gerçek oturum hazır olduğunda başlar. Güncelleme demo süresini sıfırlamaz. Veri silme/yeniden kurulum sıfırlayabilir.
- Pro ödeme düğmesi doğrudan erişim açmaz. PURCHASED durumu, izin verilen ürün/paket, RSA imzası ve acknowledge kontrolü vardır. Açık lisans anahtarı boş olduğundan gerçek ödeme şimdi etkin değildir.
- Yerel Pro durumu aktif oturumda 60 saniyede bir Play üzerinden yeniden sorgulanır. Ağ hatasında yeni erişim verilmez; başarılı boş sorgu erişimi kaldırır. Yeni açılışta kalıcı çevrimdışı Pro önbelleği yoktur.
- Aylık $4.99, yıllık $49.99, ömür boyu $199 korunur. Mağaza fiyatı ürün detaylarından alınır; Play ekranındaki yerel fiyat ödeme için belirleyicidir.
- Otomatik demo ücretlendirmesi yoktur; fotoğraf/manual metin/kayıtlı ifadeler erişilebilir kalır.
- Türkçe/İngilizce kaynak anahtarları ve formatları, isteğe bağlı ayarların boşluğu ve debug imzası kontrol edildi.

## Gerçekte çalıştırılan kontroller
- 18 sunucu politika/HTTP testi geçti. Sunucu ilk sürümde etkin değildir.
- 5 yeni native uyumluluk denetleyici testi geçti: 16 KB/4 KB, offset uyuşmazlığı, bozuk ELF ve APK raporu. Bunlar denetleyiciyi doğrular; uygulamanın kendi .so dosyalarını doğrulamış değildir.
- Kaynak XML/referans/yerelleştirme ve ZIP bütünlüğü kontrol edildi.

## GitHub'da yeni kontroller
1. Debug APK, Kotlin JUnit, debug lint; unsigned release AAB ve release lint.
2. APK içindeki gerçek 64 bit .so dosyalarının ELF PT_LOAD alignment değerleri.
3. Debug APK ZIP alignment: zipalign -c -P 16.
4. Release AAB içindeki gerçek 64 bit .so dosyalarının ELF kontrolü.
5. native-pages-debug.json ve native-pages-release.json raporları, lint raporları ve imzasız AAB artifact'a eklenir.

**16 KB kontrolü başarısızsa uyumsuz kütüphane adını raporlar. Bu durumda bağımlılık güncellemesi veya ilgili native kodun yeniden derlenmesi gerekir; kontrolü kapatıp mağazaya gönderme.** API/AGP yükseltmek veya ZIP'i hizalamak, önceden derlenmiş 4 KB ELF dosyasını tek başına düzeltmez. ELF ve ZIP kontrolleri de 16 KB cihazda kamera/Vosk/TTS çalışma testinin yerine geçmez.

## Kullanıcının telefonda tamamlaması gerekenler
- Aynı altyazı 60 saniye, hızlı replikler, canlı ROI sürükleme.
- Kamera/dinleme geçişleri, izin reddi, ekran kilidi/arka plan, 180 saniye demo sınırı.
- Modeller kurulu çevrimdışı kullanım, yerel Türkçe ses; uzun oturumda ısı/pil.
- Android 16'da arayüz/insets/kamera/mikrofon; mümkünse 16 KB cihaz veya emulator.

## Play Console açıldıktan sonra
- Üretim upload anahtarı + Play App Signing; APK test anahtarını üretimde kullanma.
- Ürünler, açık lisans anahtarı, lisans test hesabıyla satın alma/pending/iptal/refund/restore.
- Gerçek yayın imzalı AAB ve Play'in native uyumluluk sonucu.
- Geliştirici kimliği, destek/gizlilik iletişimi ve yayımlanmış politika. Data Safety cevapları seçilen konuşma/TTS motorlarının gerçekten yaptığı ağ işlerine göre gözden geçirilir.
- Mağaza görselleri, test dağıtımı ve gerekli hesap test süreçleri. Henüz uygulama yayımlanmaz veya hesabınızda işlem yapılmaz.

## Resmî kaynaklar (11 Ekim 2026 kontrolü)
- https://developer.android.com/google/play/requirements/target-sdk
- https://developer.android.com/build/releases/agp-8-10-0-release-notes
- https://developer.android.com/guide/practices/page-sizes
- https://developer.android.com/google/play/billing/integrate
