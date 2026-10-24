# Google Play hazırlığı — 23 Ekim 2026

Durum: RELEASE CANDIDATE. Genel yayına gönderilmeye hazır olarak onaylanmadı.

Kod 41, hedef API 36, minimum Android 10. Google Play Billing 8.3.0.
Paket adı com.optima.gametranslator; Play'de ilk yayından sonra değiştirilemez.

1. Play Console hesabı kişisel mi kuruluş mu kontrol edin. Yeni kişisel hesaplarda üretim erişimi için 12 kişinin 14 gün kesintisiz katıldığı kapalı test şartı olabilir. İç test bu şartın yerine geçmez.
2. Uygulama oluşturun: önerilen mevcut ad AI Game Translator; Uygulama; kategori Araçlar (Utilities önerisi); ücretsiz indirme, uygulama içi ürünler. Mevcut ad ve PALA Software marka ismi kullanıcı tarafından doğrulanmalı.
3. Destek e-postasını ve herkese açık, HTTPS gizlilik politikası adresini kesinleştirin. privacy-policy-DRAFT.html içindeki açık alanlar tamamlanmadan yayımlamayın.
4. Play Console'daki BU UYGULAMAYA AİT herkese açık RSA lisans anahtarını alın. Bu servis hesabı anahtarı ya da Google şifresi değildir.
5. Özel upload anahtarı ve Google Play App Signing kullanın. signing/optima-test.jks açık test anahtarıdır; üretimde kullanmayın. Özel anahtarı Git'e veya kaynak ZIP'e koymayın. Google Play'in app signing anahtarı ile upload anahtarı farklı görevlerdedir.
6. release.properties.example dosyasını release.properties olarak kopyalayıp değerleri özel olarak doldurun. GitHub için eşdeğer repo secrets/env kullanın; özel dosyayı repoya koymayın.
7. `gradle :app:playSubmissionBundle --no-daemon` gerçek yükleme için yapılandırmayı kontrol eder ve imzalı bundle üretir. Normal `:app:bundleRelease` bu yapılandırma olmadan yalnız İMZASIZ denetim paketi üretir; yüklenemez.
8. İlk imzalı AAB'yi iç test kanalına yükleyin, ürünleri ve lisans testçilerini yapılandırın. Pending/cancel/restore/yenileme/bitiş testlerini gerçek test hesabıyla tamamlayın.
9. Gerçek telefon ekran görüntülerini ekleyin. Maket ekran görüntüsü kullanılmadı; henüz gerçek ekran görüntüsü teslim edilmedi. En az kamera, dinle, görsel, ayarlar ekranlarını uygulamadan çekin; içerikte izinli/size ait görüntüler kullanın.
10. Veri güvenliği, hedef kitle, reklam, erişim ve içerik derecelendirme formlarını aşağıdaki gerçek davranışa göre doldurun. Bu formların gönderimi yapılmadı.
11. Play ön lansman raporunu inceleyin. Fiziksel cihaz test tablosu tamamlanmadan genel yayına geçmeyin.

Fiyatlar: aylık 5.99 USD; yıllık 69.99 USD; ömür boyu tek ödeme 99.99 USD. Yerel vergi/kur/mağaza fiyatı Play ekranında gösterilir. Üç dakikalık kamera ve ayrı üç dakikalık Dinle, 10 görsel hakkı uygulama içi ölçülür; otomatik yenilenen bir ücretsiz abonelik denemesi değildir.

Sunucusuz doğrulama: Play sorgusu + lisans imzası doğrulaması var. Yetki sunucusu zorunlu değil. İstemci tabanlı yaklaşım, sunucu tabanlı ödeme/iadeye karşı aynı güvenlik ve anlık iptal garantisini sağlamaz. Bu sürüm bulut hizmeti veya hesap sunucusu kurmaz.

Önceki manuel test APK'sı ve Play kurulumunun imzaları Google Play App Signing nedeniyle farklı olabilir; aynı paket için doğrudan üzerine kurulum garanti değildir. Gerekirse önce çeviri geçmişini paylaşarak yedekleyin; uygulamayı silmek yerel demo/geçmişi silebilir.

Resmî kaynaklar:
https://support.google.com/googleplay/android-developer/answer/14151465
https://developer.android.com/google/play/requirements/target-sdk
https://developer.android.com/google/play/billing/deprecation-faq
https://developers.google.com/ml-kit/android-data-disclosure
https://support.google.com/googleplay/android-developer/answer/9866151
https://developer.android.com/google/play/billing/test

## GitHub üzerinden imzalı AAB

`.github/workflows/play-upload-bundle.yml` yalnız elle başlatılır. GitHub Settings → Environments altında `play-release` ortamını ve şu secrets değerlerini ekleyin:
PLAY_UPLOAD_STORE_BASE64, PLAY_UPLOAD_STORE_PASSWORD, PLAY_UPLOAD_KEY_ALIAS, PLAY_UPLOAD_KEY_PASSWORD, PLAY_LICENSE_KEY, PLAY_PRIVACY_URL, PLAY_SUPPORT_EMAIL.

Base64 bir şifreleme yöntemi değildir: upload anahtarı yalnız secret olarak tutulmalıdır. Workflow sonunda özel anahtar silinir. İmzalı AAB depo Releases bölümünde TASLAK olarak çıkar; Play Console'a otomatik gönderilmez. Ortama erişimi uygun yetkililerle sınırlayın. `release.properties` dosyasını veya özel anahtarı kaynak paketine koymayın.

## İçerik ve erişim formları

Mağaza açıklamalarında sıfır gecikme, kusursuz/argo kapsamı, duygu veya konuşmacı kimliği garantisi yoktur. Hedef kitle ve IARC yanıtları uygulama içeriğine göre tamamlanmalı; çeviri aracının argo/küfür üretmesi mümkündür. Çocuklara yönelik tasarlandığı iddia edilmez; yaş seçimi otomatik doldurulmadı. Varsayılan yerel modda giriş hesabı gerekmez, fakat modeller için internet gerekir. Pro incelemesi için Google Play test satın alımları kullanılmalı; incelemeciye gerçek kişi şifresi verilmez.

Play License key tek satır ve eksiksiz base64 değer olmalı. App Signing sertifikası / SHA parmak izi, Play lisans RSA anahtarının yerine kullanılamaz. Özel anahtar/parolaları sohbet mesajına veya kaynak dosyaya yazmayın.

Ömür boyu ürününde BUY satın alma seçeneğini etkinleştirin ve backward/legacy compatible olarak işaretleyin; kiralama veya ön sipariş yapılandırmayın. Tek seferlik ürünün offer token değeri de ödeme akışına aktarılır.

Sunucusuz deneme sınırları cihazın yerel verilerinde tutulur. Kullanıcı veri temizleme veya yeniden kurma ile bu sayaçları sıfırlayabilir; hesap başına tek deneme ve cihazlar arası aynı sayaç garantisi yoktur. Bunu engellemek için ayrıca hesap/sunucu veya doğrulanmış cihaz temelli bir yetki sistemi gerekir; bu paket böyle bir sistemi kurmaz.
