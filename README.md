# AI Game Translator 0.9 — Sürekli canlı ses çevirisi

Android 10+ · İngilizce → Türkçe · Kamera, görsel ve sürekli mikrofon çevirisi.

## Bu sürümde

- Azure Speech SDK 1.52.0 ile tek, sürekli mikrofon bağlantısı kullanılır. Android SpeechRecognizer oturumları yeniden başlatılarak canlı akış taklit edilmez.
- İngilizce kaynak ve Türkçe ara çeviri konuşma sürerken ekranda güncellenir. Ara sonuçlar değişebilir. Yalnızca tamamlanan sonuçlar geçmişe ve ses kuyruğuna eklenir.
- SDK sonuç kimliğiyle yinelenen teslimatlar engellenir. Gerçekten tekrar söylenen aynı kelimeler, yeni sonuç kimliğiyle korunur.
- Türkçe okuma mevcut telefon TTS motorundan yapılır. Bu sürüm Azure bulut sesi veya otomatik sesten konuşmacı/cinsiyet tanıma eklemez. Mevcut 8 konuşmacı profili ve Türkçe ses seçimi korunur.
- Kamera OCR, altyazı alanı, sabit HUD/süre filtresi ve görsel çeviri korunur.
- Bulut modunda yerel ML Kit çeviri modelinin indirilmesi gerekmez; kamera/görsel ve Android test modu için gerekir.

## GitHub'dan APK alma

1. ZIP'i aç. İçindeki proje dosyalarını mevcut reponun köküne taşı; ikinci bir proje klasörü içine yerleştirme. `app`, `.github`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` ve README repo kökünde olmalı. Gizli `.github` klasörünü de aktar.
2. Değişiklikleri commit/push yap. Web yüklemesi kullanıyorsan `.github/workflows/android-ci.yml` dosyasını da güncelle.
3. Actions → Android APK işini aç. İşlem `assembleDebug`, `testDebugUnitTest`, `lintDebug` çalıştırır.
4. Yeşil olduğunda `AI-Game-Translator-0.9-streaming-speech-debug` artifact'ını indir. İçindeki `app-debug.apk` dosyasını telefona aktar ve yükle.

Sürüm kodu 9'dur. CI debug APK'larının imzası runner'a göre değişebilir; bu paket sabit yayın imzası kurmaz. Güncelleme reddedilirse mevcut imza ile yeni imzanın eşleşmesi ayrıca çözülmelidir.

## Azure hesabını bağlama — zorunlu

1. https://portal.azure.com adresinde Azure aboneliğinle giriş yap.
2. Bir **Speech / Azure AI Speech** kaynağı oluştur. Kaynak bölgesini seçerken fiyatlandırma ve kotanı kontrol et. Ücretsiz kullanım olduğunu varsayma; sürekli akış, hesabının tarife ve kullanımına göre ücret oluşturabilir.
3. Kaynak → **Keys and Endpoint / Anahtarlar ve uç nokta** bölümünden bir Speech anahtarı ve bölge kimliğini al (örnek `westeurope` veya `eastus`). Bölge kimliği kullan; portalda görünen açıklama veya URL kullanma.
4. Telefonda **Dinle → Azure bağlantı ayarları** bölümüne anahtar ve bölgeyi gir, Kaydet'e bas. Kaydetme bağlantıyı doğrulamaz; doğrulama Başlat ile yapılır.
5. `Sürekli canlı çeviri (Azure)` seçili olsun. Dinlemeyi başlat ve mikrofon izni ver. Konuşma sırasında kaynak ve ara çeviri görünmeli, biten cümle geçmişe eklenmeli.

Anahtar chat'e gönderilmez, GitHub'a/CI secrets'a/koda gömülmez. Bu kişisel test sürümünde anahtar cihazda Android Keystore AES/GCM ile şifreli tutulur. Boş anahtar alanı mevcut anahtarı korur. Ayarlarda Kaydı sil bağlantı bilgilerini siler. Uygulama verileri silinirse tekrar girmen gerekir. Başkalarına dağıtılacak ürün için kullanıcı kimliği ve kısa ömürlü servis belirteci sağlayan bir sunucu gerekir; ortak abonelik anahtarı APK'ya eklenmemelidir.

## Türkçe ses ve sürekli dinleme

- Türkçe sesi kulaklıkla dinle. `Türkçe sesi kulaklıkla dinliyorum` kutusunu yalnızca gerçekten kulaklık kullanıyorsan aç; ardından `Türkçe sesli oku` seç.
- Kulaklık seçili değilse canlı akış başlangıcında sesli okuma kapanır, metin çevirisi sürer. Mikrofon Türkçe okuma için durdurulmaz. Kutuyu açmak sistem ses çıkışını değiştirmez; kulaklığın bağlı ve doğru çıkışta olduğundan emin ol.
- Mikrofon bir TV/PC/başka cihazdan duyulan sesi alır. Bu sürüm telefonun iç oyun sesini doğrudan yakalamaz.
- Uygulama arka plana geçtiğinde veya Durdur'a basıldığında oturum kapanır. Bağlantı kesilince otomatik ücretli yeniden bağlanma döngüsü çalışmaz; durumu kontrol edip tekrar başlat.
- Gerçek gecikme internet, konuşma bitişi, gürültü ve servis yanıtına bağlıdır. Sıfır gecikme garanti edilmez. Konuşma çok hızlıysa ses kuyruğu geride kalabilir; metin geçmişi korunur.
- Sinyal çubuğu bulut modunda bağlantı faaliyetini gösterir, ölçülmüş ses seviyesi değildir. Gecikme metrikleri bulut için ölçüm iddiasında bulunmaz.

## Telefonda doğrulama

1. 60–90 saniye İngilizce oyun/video konuşması oynat. Düğmeye basmadan ara çevirilerin değiştiğini ve tamamlanan repliklerin sırayla geçmişe geçtiğini kontrol et.
2. Kulaklıkla sesli okumayı aç. Türkçe okunurken yeni İngilizce kaynakların gelmeye devam ettiğini doğrula.
3. Aynı cümleyi iki kez söyle: ikisi de çevrilmeli. Tek SDK sonucu iki kez seslendirilmemeli.
4. İnterneti kes: uygulama bağlantı uyarısı göstermeli ve kendiliğinden yeniden bağlanmamalı. İnterneti açıp Başlat'a basarak yeni oturum aç.
5. Durdur'a bas/uygulamayı arka plana al: mikrofon oturumu sonlanmalı, geç kalmış sonuçlar ekrana yazılmamalı.
6. Kamera moduna dön, yerel modeli hazırla; çalışan altyazı alanı ve sabit sayaç filtresini kontrol et.

## Doğrulama sınırı

Bu paket burada Android SDK/Gradle olmadığı için derlenmedi; unit testleri ve lint çalıştırılmadı. XML/YAML ve ZIP yapısı statik olarak kontrol edildi. Azure anahtarı sağlanmadığı için canlı servis ve telefondaki mikrofon/TTS eşzamanlı çalışması henüz test edilmedi. GitHub Actions yeşili derlemeyi doğrular; yukarıdaki cihaz testi gerçek canlı çeviriyi doğrular.

Resmî belgeler:
- https://learn.microsoft.com/en-us/azure/ai-services/speech-service/how-to-translate-speech
- https://learn.microsoft.com/en-us/azure/ai-services/speech-service/get-started-speech-translation
- https://azure.microsoft.com/tr-tr/pricing/details/speech/
