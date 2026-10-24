# Google Play'e yükleme — 0.30 / kod 41

Bu paket yayın hazırlığıdır. Play Console'a yükleme veya Google incelemesine gönderim yapılmadı. Bu oturumdaki tarayıcı erişimi alan adı yetkilendirme denetiminde engellendi.

## Önce tamamlanacak gerçek bilgiler

| Bilgi | Neden gerekiyor? | Nasıl kullanılır? |
|---|---|---|
| Uygulamanın Play Console kaydı | Doğru uygulama ve paket kimliğini doğrulamak | Paket: `com.optima.gametranslator`; mevcut ad `AI Game Translator` |
| Destek e-postası | Uygulama ve mağaza iletişimi | `PLAY_SUPPORT_EMAIL` |
| Yayınlanmış HTTPS gizlilik politikası | Gerçek veri akışı, erişilebilir politika ve uygulama bağlantısı | `PLAY_PRIVACY_URL`; HTML taslağındaki gerçek iletişim alanlarını tamamlayın |
| Bu uygulamanın herkese açık RSA lisans anahtarı | Sunucusuz Play satın alma imzası doğrulaması | `PLAY_LICENSE_KEY`; parola veya servis hesabı JSON'u değildir |
| Özel yükleme anahtarı | AAB'nin Play'e yüklenebilmesi | Mevcut uygulama kaydının anahtarını kullanın; ilk uygulamaysa yeni ve özel anahtar oluşturun |

Anahtar/parola/Google giriş kodlarını sohbete veya Git'e yazmayın. Açık `signing/optima-test.jks` yalnız test APK güncellemeleri içindir. Google Play App Signing dağıtım anahtarı ile geliştirici upload anahtarı farklıdır. Önceden oluşturulmuş bir uygulamanın anahtarını rastgele değiştirmeyin.

## GitHub üzerinden imzalı paket

1. Bu klasörün içeriğini mevcut deponun köküne uygulayın. Önceki özel dosyaları silmeyin; bu ZIP özel release yapılandırması içermez.
2. Actions'ın normal Android final checks işini yeşil tamamlayın.
3. `play-release` GitHub Environment / Secrets içine şu gerçek değerleri kaydedin:
   - `PLAY_UPLOAD_STORE_BASE64` (özel yükleme anahtarı dosyasının Base64 içeriği)
   - `PLAY_UPLOAD_STORE_PASSWORD`
   - `PLAY_UPLOAD_KEY_ALIAS`
   - `PLAY_UPLOAD_KEY_PASSWORD`
   - `PLAY_LICENSE_KEY` (uygulamanın herkese açık Play RSA anahtarı)
   - `PLAY_PRIVACY_URL`
   - `PLAY_SUPPORT_EMAIL`
4. **Build private Play upload bundle** iş akışını manuel çalıştırın. `playSubmissionBundle` eksik/yanlış yapılandırmayı reddeder; bu korumayı kapatmayın.
5. Başarılı işin oluşturduğu taslak GitHub Release'ten **imzalı `app-release.aab`** dosyasını indirin. Normal `bundleRelease` kontrol çıktısı imzasız olabilir; dosya uzantısının AAB olması tek başına yeterli değildir.

Yerel alternatif: `release.properties.example` dosyasını Git dışında tutulan `release.properties` olarak doldurup `gradle :app:playSubmissionBundle --no-daemon` çalıştırın. Android Studio gerekmez.

## Play Console sırası

1. Uygulama kaydını açın veya ilk kez oluşturun: uygulama, ücretsiz indirme; uygulama içi ürünler ayrıca. İlk yüklemeden sonra paket adını değiştirmeyin.
2. Önce **Test et ve yayınla → Test → Dahili test** bölümünde sürüm oluşturun. Hesabın dili veya arayüz sürümüne göre menü adı değişebilir.
3. Google Play App Signing kurulumunu okuyup hesap sahibi olarak tamamlayın. İmzalı AAB'yi yükleyin. Kod 41 daha önce kullanılmışsa sürüm kodunu artırıp yeniden derleyin. Paket adı, kod 41, hedef API 36 ve Play'in gösterdiği cihaz uyumluluk uyarılarını kontrol edin.
4. Sürüm notları için `RELEASE_NOTES_TR.txt` / `RELEASE_NOTES_EN.txt` dosyalarını kullanın. Lisans testçilerini ve test kullanıcılarını ekleyin. Satın almayı gerçek ücretli hesapla değil uygun Play test hesabıyla sınayın.
5. Ürünleri `PRODUCTS.csv` ile aynı kimlikler/planlarla oluşturun:
   - `translator_pro` aboneliği, `monthly` / P1M: **5,99 USD**.
   - `translator_pro` aboneliği, `yearly` / P1Y: **69,99 USD**.
   - `translator_pro_lifetime` tek seferlik tüketilmeyen ürün: **99,99 USD**.
   CSV bir yapılandırma rehberidir; Play'e otomatik CSV içe aktarma garantisi değildir. Bölgesel fiyatlar/vergi Play tarafından gösterilir. Uygulama fiyatı sabit metinden değil Play ProductDetails'tan alır.
6. Kamera 3 dakika, ayrı Dinle 3 dakika, görsel 10 adet deneme olarak kalır. Bu uygulama içi kota otomatik abonelik başlatmaz; abonelik ücretsiz deneme teklifiyle karıştırmayın.
7. Gerçek uygulama ekran görüntülerini, 512×512 simgeyi, 1024×500 özellik görselini, mağaza açıklamalarını ve destek bağlantılarını ekleyin. Başka uygulamaların görüntülerini kullanmayın. Hedef kitle, reklam, erişim, içerik derecelendirmesi ve veri güvenliği formlarını gerçek davranışa göre tamamlayın.
8. Dahili testte satın alma, iptal/pending, geri yükleme, abonelik bitişi/iade, izin reddi, model indirme iptali/devamı, kulaklıklı/kulaklıksız ses ve uzun oturumları kontrol edin. Play ön lansman raporundaki gerçek hataları düzeltin.
9. Hesap türünüz yeni kişisel hesapsa kapalı testte en az 12 testçinin 14 gün kesintisiz katılımı şartı uygulanabilir. Üretim erişimi için Console'daki gerçek gereksinimi izleyin; dahili test bunun yerine geçmez. Gerekli testler ve formlar tamamlanınca üretim incelemesine gönderin.

## Kullanılan resmî kaynaklar (23 Ekim 2026 kontrolü)

- Hedef API: https://support.google.com/googleplay/android-developer/answer/11926878?hl=en
- İmzalama: https://support.google.com/googleplay/android-developer/answer/9842756?hl=en
- Kişisel hesap testi: https://support.google.com/googleplay/android-developer/answer/14151465
- Veri güvenliği: https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
- ML Kit verileri: https://developers.google.com/ml-kit/android-data-disclosure

Play'e kabul, bu kaynaklara bağlantı vermekle veya birim testleri geçmekle otomatik olarak garanti edilmez. Bu belgeler uygulamaya ait gerçek hesap ve yayın değerlerinin yerine geçmez.
