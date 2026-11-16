# Pro Server 47 — Dahili test için

Bu paket mevcut ai-game-translator projesine uygulanacak beş tam kaynak dosyasıdır. APK/AAB değildir. Cloudflare Worker 1 sağlık kontrolünün başarılı olması gerekir. JSON anahtarı veya servis sırrı içermez.

1. ZIP içindeki app klasörünü mevcut AI-Game-Translator klasörünün üzerine çıkarın, dosyaları değiştirin. GITHUB_GONDER.cmd dosyasını da bu köke koyun.
2. Bu klasörde açık CMD'de GITHUB_GONDER.cmd çalıştırın. Hata olursa durup çıktıyı gönderin. git add . veya force push kullanmayın; eski yedek klasörlerin silmelerini dahil etmeyin.
3. GitHub Actions → Build private Play upload bundle → main → Run workflow. Başarılı çalıştırmaya ait yeni taslak Release'den AAB indirin.
4. Dahili teste yüklerken 47 (0.31.4-server-pilot) görünmeli. Yalnızca DAHİLİ TEST; henüz üretime/kapalı teste yaymayın.
5. Google Play üzerinden telefondaki uygulamayı güncelleyin.
6. Pro → Google hesabıyla giriş düğmesine basın. Play satın alımlarında kullandığınız, OAuth test kullanıcılarına eklenen Google hesabını seçin.
7. Girişten sonra satın alımlar otomatik sorgulanır. Pro → Satın alma desteği → Yenile'ye bir kez basın; 15 saniye sonra destek penceresini kapatıp tekrar açarak raporu kopyalayın.

Beklenen rapor:
- Installed version: 0.31.4-server-pilot (47)
- Server authority enabled: true
- Configured: true; signed in: true
- HTTP: 200, kayıtlar altında state / result / expiresAt.
Süresi bitmiş eski satın alım Play istemcisinde dönse bile Worker bunu inactive olarak döndürürse Pro açılmaz.

Hatalar:
- sign_in_...: Android OAuth paket/SHA-1, Web Client ID, Google Play Services veya test kullanıcı ayarı incelenir. Bu sürüm Play'den kurulmalıdır; debug anahtarının SHA-1'i henüz OAuth'a eklenmedi.
- HTTP 503 / play_permission_denied: servis hesabının Play izinleri/API erişimi kontrol edilir.
- HTTP 503 / google_service_unavailable: servis hesabı JSON'u ve OAuth erişimi kontrol edilir.
- HTTP 401: Google hesabıyla tekrar giriş gerekir.
- legacy_migration_required: daha önce uygulama hesabına bağlanmadan alınmış AKTİF satın alım. Otomatik sahiplenme yapılmaz. Yeniden ödeme yapmayın; bu geçiş onaylanmadan genel kullanıcılarda etkinleştirmeyin.
- account_mismatch: satın alım başka uygulama hesabına bağlı; aynı Google giriş hesabıyla tekrar deneyin.
Worker CPU/istek kotası veya ağ hatasında pozitif Pro bilgisi korunmaz; yeniden doğrulama gerekir.

Davranış değişiklikleri:
- Pro kararı sunucudan gelir; sunucu modu açıkken yerel imza doğrulamasına geri düşülmez.
- Üyelik kiralaması en çok 60 saniye; canlı kullanımda 30 saniyede bir yenilenir. Ağ gecikmesi erişim süresini uzatmaz.
- Google ID token yalnızca bellekte tutulur; uygulama süreci yeniden açıldığında veya token süresi dolduğunda giriş yeniden gerekir. Kalıcı oturum yenilemesi bu pilotta yoktur.
- Sunucu hatası, bitiş tarihi ve eski satın alım geçiş durumu destek raporuna eklenir. Ham token, tam sipariş kimliği, özel anahtar veya hesap e-postası rapora yazılmaz.
- Kamera/dinle/görsel deneme sayaçları mevcut yerel bağımsız sayaçlardır. /v1/usage çağrısı yapılmaz; hesaplar arasında deneme eşitlemesi yoktur.
- Canlı modlarda başlangıçtaki sunucu kontrolü için Google girişi ve internet gerekir. Aktif kullanımda sunucu yanıtı başarısızsa mevcut güvenlik akışı oturumu durdurur.

Kalan yayın şartları:
Gerçek Google giriş ve Play API testi, iptal/süre bitişi/erişim geri alma, yeni satın alma ve geri yükleme testleri henüz cihazda yapılmadı. Eski aktif ödemelerin sahiplik geçişi, hesap/veri silme akışı, gizlilik politikası ve Data Safety sunucu kullanımına göre tamamlanmalıdır. Bu pilot üretime hazır onayı değildir.

Doğrulama:
ProService.kt ve PlayBilling.kt sözdizimi, yeni MainActivity blokları ve XML yapılandırması yerel olarak kontrol edildi. Android SDK/Gradle bu ortamda yok; derleme, birim testleri ve lint GitHub Actions'ta geçmelidir. Worker'ın önceki 29 örnek testinin geçmesi gerçek Google hizmetine bağlanıldığı anlamına gelmez.
