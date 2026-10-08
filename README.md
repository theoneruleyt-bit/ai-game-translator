# AI Game Translator 0.12 — Yeni görünüm, tema ve dil

Android 10+ · İngilizce → Türkçe · Cihaz içi kamera, görsel ve canlı ses çevirisi.

## Yeni görünüm

- Açık / Koyu / Sistem teması: ekran, kartlar, yazılar, düğmeler, ayarlar ve Android diyalogları birlikte değişir.
- Türkçe / English / Sistem arayüz dili. Yardım, durum/hata mesajları, indirme ilerlemesi ve profil tarzları dahil 233 ortak metin iki dilde native Android kaynaklarına taşındı.
- İngilizce arayüz, çeviri dilini değiştirmez. Kaynak İngilizce, çıktı ve ses Türkçe kalır. Ses örnekleri bu nedenle Türkçe okunur. Eski oturum geçmişindeki etiketler kayıt anındaki dilini koruyabilir.
- Daha sade kart düzeni, ince kenarlıklar, dokunma geri bildirimi, daha güçlü okunabilirlik. Uzun ses ve konuşmacı ayarları açılır bölümlerde.
- Mikrofon görseli gerçek PCM seviyesine bağlıdır; boşta ses algılanıyormuş gibi animasyon göstermez.
- Android sistem çubukları / ekran kesikleri için inset düzenlemesi, açık temada uygun durum çubuğu ikonları.

## Faydalı ekler

- Ana ekranda konuşma ve çeviri modelinin hazır durumu; ayrıntılı çevrimdışı durum ekranı.
- Kaynak/çeviri metnini seçme ve çeviriyi tek dokunuşla kopyalama.
- İngilizce metin yazma / yapıştırma ve mevcut yerel motorla çevirme.
- Kaydedilen replikler: açıkça Kaydet'e bastığın son 100 kaynak/çeviri çifti cihazda tutulur; tekrar okuma, kopyalama ve topluca silme. Çiftler yerel uygulama verisidir, sunucuya gönderilmez. Oturum geçmişi ayrıca paylaşılabilir.
- Çeviri yazı boyutu: Kompakt / Normal / Büyük. Telefonun sistem font ölçeği de uygulanır.
- Tema/dil değişince aktif çeviri güvenli şekilde durur; mevcut son çeviri ve son 20 oturum girdisi boyut sınırlarıyla ekranın yeniden oluşturulmasına taşınır. Kaydedilen replikler uygulama yeniden açıldığında da korunur.
- Seçilen altyazı çerçevesi cihazda saklanır ve yeniden açılışta geri yüklenir.

## Bulunan ve düzeltilen durumlar

- Kayıtlı altyazı alanı filtresi kapalıyken çerçevenin görünür kalabilmesi düzeltildi.
- İptal edilen sürükleme hareketinin alanı geçersiz/küçük bırakması düzeltildi; önceki alan geri gelir.
- İnternet sesi engeli veya hazır olmayan TTS durumunun yanlışlıkla kuyruk dolu diye gösterilmesi düzeltildi.
- İlk otomatik ses seçiminde indirilmemiş Türkçe sesler listeden çıkarıldı.
- İndirilmiş Türkçe çeviri modeli açılışta kontrol edilir; varsa mevcut motor hazır hâle getirilir. İlk kurulumda kullanıcı model indirme düğmesini kullanır.

Vosk kayıt/segment algoritması, Hızlı/Dengeli/Tam cümle ayarları ve ses kuyruğu sırası korunur. Yeni bir gecikme veya konuşma doğruluğu garantisi verilmez. Kamera OCR ve HUD/süre filtresi korunur. Otomatik konuşmacı tanıma, yeni gerçek ses modeli, bulut hesabı veya Play Store ödeme/yayın özellikleri eklenmedi.

## GitHub

ZIP içindeki Android_Game_Translator_0_12 klasörünün tüm içeriğini mevcut repo köküne aktar. `.github`, `.gitignore`, `app`, `signing` ve Gradle dosyalarını üzerine yaz. İç içe ikinci proje klasörü oluşturma.

```bash
git add .
git commit -m "v0.12: profesyonel arayuz, tema, English ve kaydedilen replikler"
git push origin main
```

Actions → Android APK yeşil olunca `AI-Game-Translator-0.12-design-debug` artifact'ını indir. İçindeki APK'yı kur.

Sürüm kodu 13. 0.11'deki aynı test anahtarı byte olarak korunmuştur; 0.11 APK'nın üzerine güncellenebilmesi beklenir. Anahtar değiştirildiyse veya önceki kurulum daha eski CI runner imzalıysa eşleşmeyebilir. `signing` klasörünü koru. Açık test anahtarı üretim/Play Store imzası değildir.

## Ayarlar

Ayarlar → Görünüm → Açık / Koyu / Sistem.
Ayarlar → Arayüz dili → Türkçe / English / Sistem.
Settings → Appearance / Interface language, İngilizce menüde aynı seçeneklerdir.
Tema/dil değişiminden sonra aktif kamera/dinlemeyi yeniden başlat.
Ses seçeneklerini göster / gizle → Türkçe ses, tempo ve ton.
Konuşmacı ayarlarını göster / gizle → 16 profil ve ses/ton atama.

## Çevrimdışı kullanım

Çeviri modelini hazırla, Dinle bölümünde yaklaşık 40 MB İngilizce Vosk modelini bir kez indir. Yerel Türkçe TTS sesini kur. Bunlar hazırsa Vosk dinleme ve ML Kit çevirisi için internet/API anahtarı gerekmez. İnternet gerektiren TTS sesi veya Android konuşma test modu seçilirse ilgili servis ağ kullanabilir. Telefonun iç oyun sesini doğrudan yakalama eklenmedi; mikrofon çevrede duyulan sesi alır. Kulaklık geri beslemeyi azaltır. Arka plana geçiş veya Durdur aktif oturumu kapatır.

Kaydedilen replikler uygulamanın özel verilerinde tutulur. Uygulama verisini silmek veya uygulamayı kaldırmak modelleri, tercihleri ve kayıtlı replikleri silebilir. Oturum geçmişi kalıcı kayıt değildir; önemli çeviride Kaydet'e bas.

## Doğrulama durumu

XML/YAML/ZIP, TR/EN kaynak eşleşmesi ve format yer tutucuları, renk kontrastları, resource referansları ve sabit imza kontrolü statik olarak yapılmıştır. Bu ortamda Android SDK/Gradle/emülatör olmadığı için APK derlemesi, unit test/lint, gerçek ekran görüntüsü ve cihaz ses/kamera testi çalıştırılmadı. GitHub Actions bu paketin mevcut unit testlerini ve lint'i çalıştırır.

Cihaz kontrolü:
1. Açık/koyu/sistem temalarını ve TR/EN arayüzlerini tek tek aç. Diyalogları, uzun İngilizce etiketleri, kaydırmayı ve sistem çubuklarını kontrol et.
2. Tema değişince son çevirinin ve seçili modun korunmasını; arka planda mikrofonun kapanmasını kontrol et.
3. Kamera çerçevesini çiz, yeniden aç; aynı alan geri gelmeli. Filtre kapalıyken çerçeve gizlenmeli.
4. Vosk dinleme, Türkçe okuma ve 16 konuşmacı profilini dene. Bir önceki videoyla gecikmeyi karşılaştır.
5. Bir repliği kaydet, uygulamayı kapat/aç; Kaydedilenler'den oku ve kopyala. Boş kayıt listesi ve toplu silmeyi dene.
6. İngilizce metin yapıştırıp çevir. Kaynak/çıktı metin seçimi, büyük yazı boyutu ve uçak modunu dene.
7. 0.11'den 0.12'ye aynı imzalı güncellemeyi doğrula.

Benzer ürünlerdeki çevrimdışı paket, geçmiş ve metin işlemleri örnek alınarak bu uygulamanın cihaz içi akışına uygun özellikler seçildi:
https://www.microsoft.com/en-us/translator/apps/features/
https://support.google.com/translate/answer/6142480
https://support.google.com/translate/answer/6142473

Bağımlılıklar: Vosk API ve küçük İngilizce model Apache 2.0; JNA LGPL 2.1+ / Apache 2.0 seçeneklidir. Mevcut ML Kit, CameraX ve Android TTS kullanımı sürer. Model arşivindeki lisans dosyaları korunur.
