# Derleme hata düzeltmeleri

## Düzeltilen doğrulanmış sorunlar
1. android-ci.yml içindeki APK/AAB kontrolleri derleme başarısız olsa da always() ile çalışıyordu. Bu, birincil hatanın ardından ek dosya bulunamadı hataları üretiyordu. Artık bu kontroller yalnızca Android derleme adımı başarılıysa çalışır. Gerçek hizalama hataları başarısız sonuç vermeye devam eder.
2. Gradle günlüğü indirilebilir pakete eklenmiyordu. Artık pipefail ve tee ile gerçek çıkış kodu korunarak ci-logs/gradle.log kaydedilir; hata durumunda da ci-download.zip içine alınır.
3. Doğrudan indirilebilir ek günlük ve rapor çıktısı yoktu. Her iki iş akışında Actions Artifacts altında diagnostics paketi sunulur. Artifact kotası doluysa bu yardımcı yükleme asıl derlemeyi başarısız saydırmaz; genel CI'nin mevcut taslak sürüm ZIP indirmesi korunur.
4. Python sürümü koşucu güncellemelerine bağlıydı. Her iki iş akışı Python 3.12 kullanır.
5. check_native_pages.py eksik/bozuk AAB için rapor yazamadan traceback ile sonlanıyordu. Artık açıklayıcı JSON raporu yazar ve başarısız çıkış kodunu korur. Eksik dosya hiçbir zaman hizalama kontrolünden geçmiş sayılmaz.
6. İmzalı Play derlemesinde yapılandırma doğrulaması ayrı bir erken adım haline getirildi.

## Doğrulananlar
- 28 sunucu testi ve 9 script testi geçti: toplam 37.
- Yeni testler: eksik AAB, bozuk AAB, başarısız derlemede günlüğün korunması ve yarım AAB'nin yayımlanmaması, başarılı denilen pakette zorunlu çıktıların yokluğu.
- Her iki workflow YAML olarak ayrıştırıldı ve toplam 23 shell bloğu bash -n ile doğrulandı.
- Sherpa, VAD, noktalama modeli ve OPUS-MT paket kontrolleri geçti.

## Gerçek Android derlemesi
Android SDK 36, Gradle 8.13 ve JDK 17 ile doğrulandı:
- Debug APK üretildi.
- Release AAB üretildi (özel yükleme anahtarı olmadığı için mağaza imzası yoktur).
- Debug 199 / Release 199 JUnit testi: 0 başarısızlık, 0 hata.
- Debug ve Release lint: 0 hata; sırasıyla 73 ve 72 uyarı mevcut. Uyarılar hata olarak gizlenmedi veya bastırılmadı.
- APK ve AAB içindeki 38'er yerel kütüphane 16 KB ELF kontrolünden geçti.
- APK ZIP 16 KB hizalama kontrolünden geçti.
- Release küçültme, gömülü mapping ve zorunlu JNI sınıfları kontrolünden geçti.
- Kanıt raporları verification/ci-fix/ klasöründedir.

İlk tam komut bağımlılık indirme nedeniyle süre sınırına ulaştı; tamamlanan Debug APK ve 199 testin ardından kalan release ve lint görevleri ayrı komutta BUILD SUCCESSFUL ile tamamlandı.

## Sınırlar
Paylaşılan GitHub günlüğü yalnızca son AAB kontrol hatasını içeriyordu; önceki GitHub derlemesinin kesin hata nedeni bilinmiyor. Bu kaynak paketi burada derlendi ve yukarıdaki kontroller geçti; GitHub hesabının izinleri, özel Secrets değerleri ve cihazdaki çalışma davranışı burada doğrulanmadı. İmzalı Play AAB'si mevcut yükleme anahtarınızla GitHub üzerinden oluşturulmalıdır. Bu ZIP kaynak projesidir; derleme ürünleri ZIP'e eklenmemiştir.

## Yükleme
ZIP'i çıkarın. app ve .github içeren ai-game-translator-main klasörünün İÇERİĞİNİ mevcut deponuzun köküne kopyalayın. Windows'ta .github klasörünü de kopyaladığınızdan emin olun. Mevcut release.properties, yükleme anahtarı ve GitHub Secrets değerlerini koruyun.

Mevcut klonlanmış deponuzda PowerShell:

    git add .
    git commit -m "Fix cascading CI errors and preserve build diagnostics"
    git push

Actions > Android final checks and draft download çalıştırmasını açın.
Başarısızsa sayfanın altındaki Artifacts bölümünden android-diagnostics dosyasını indirin; ci-logs/gradle.log asıl Gradle hatasını içerir. Artifact kotası doluysa GitHub Releases altındaki Translator CI taslağında ci-download.zip dosyasını kullanın. Derleme başarılıysa imzalı mağaza paketi için Build private Play upload bundle akışını çalıştırın.
