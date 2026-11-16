# Açık test güncellemesi — 0.33.0

## Bu pakette değişenler
- Varsayılan versionCode 51 → 52; versionName 0.33.0-open-test.
- Gradle releaseVersionCode ve releaseVersionName parametreleriyle sürüm seçimi.
- GitHub imzalı AAB iş akışında sürüm girişleri ve giriş doğrulaması.
- İmzalı derleme akışına mevcut 33 Python testi ve paketlenmiş model/kütüphane doğrulamaları eklendi.
- Paket adı com.optima.gametranslator olarak korundu. Uygulama çeviri davranışında değişiklik yapılmadı.
- ZIP içinde eski AI_Game_Translator_0_31 kopyası bulunmaz; güncel app ve .github tek köktedir.

## Sonraki adımlar
1. Play Console > Uygulama paketi gezgini bölümünden daha önce yüklediğiniz en yüksek sürüm kodunu bulun. 51 veya altındaysa 52 kullanılabilir. Daha yüksekse ondan büyük bir sayı seçin.
2. Bu klasörün içeriğini mevcut GitHub deponuzun köküne aktarın. app, scripts, server ve .github kökte olmalı. Eski imzalama anahtarını ve mevcut GitHub secrets değerlerini koruyun.
3. Actions > Build private Play upload bundle > Run workflow yolunu açın. version_code ve version_name alanlarını doldurun.
4. Mevcut play-release ortamında gereken secrets: PLAY_UPLOAD_STORE_BASE64, PLAY_UPLOAD_STORE_PASSWORD, PLAY_UPLOAD_KEY_ALIAS, PLAY_UPLOAD_KEY_PASSWORD, PLAY_LICENSE_KEY, PLAY_PRIVACY_URL, PLAY_SUPPORT_EMAIL. Anahtar ve şifreleri sohbetten paylaşmayın. Eski sürümde kullandığınız yükleme anahtarını kullanın.
5. İş akışı başarılı olunca GitHub Releases altındaki Play release candidate taslağından app-release.aab ve release-diagnostics.zip dosyalarını indirin. Normal Android CI akışındaki AAB imzasız olabilir; Play için özel imzalı akışı kullanın.
6. Play Console > Test et ve yayınla > Test > Açık test > Yeni sürüm oluştur yolundan imzalı AAB dosyasını ekleyin. Bu iş akışı Play'e otomatik göndermez.
7. Önce dahili testte mevcut kapalı test sürümünün üzerine güncelleyerek açılış, geçmiş/ayarların korunması, mikrofon izni reddetme ve yeniden verme, Dinle başlat/durdur, kamera alan seçimi, model indirme ve satın almayı geri yükleme akışlarını kontrol edin. Ardından açık test sürümünü inceleyip gönderin.

## Açık test erişimi
Yeni kişisel hesaplarda kapalı test şartını tamamlamak ile üretim erişimi onayını almak farklıdır. Açık test, üretim erişimi onaylandıktan sonra açılır. Menü kapalıysa uygulama kontrol panelindeki üretim erişimi başvurusunu kontrol edin.
Resmî kaynak: https://support.google.com/googleplay/android-developer/answer/14151465?hl=tr
Güncelleme: https://support.google.com/googleplay/android-developer/answer/9859350?hl=tr

## Doğrulama ve sınırlar
Bu oturumda 28 sunucu testi ve 5 yerel kütüphane denetim testi geçti. Sherpa AAR sağlama toplamı / 64 bit 16 KB hizalama, Silero VAD, İngilizce noktalama modeli ve OPUS-MT paket kontrolleri geçti.
Android SDK ve Gradle bu ortamda yoktu. Android derlemesi, Kotlin/JUnit, lint, nihai AAB hizalaması ve cihaz testi burada çalıştırılmadı. İmzalı APK/AAB üretilmedi. GitHub iş akışının başarılı olması ve cihaz kontrolleri hâlâ gereklidir. Play Console'un mevcut durumu, gizlilik ve hesap silme bağlantılarının erişilebilirliği doğrulanmadı.

## Sürüm notu önerisi
Açık test sürümü kullanıma sunulmuştur. Oyun çevirisi deneyimiyle ilgili geri bildirimlerinizi paylaşabilirsiniz.
