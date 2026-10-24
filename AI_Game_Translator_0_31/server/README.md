> 0.16.3 Android istemcisindeki deneme hakları cihazda bağımsız tutulur. Aşağıdaki eski ortak kullanım uç noktası yeni istemciden çağrılmaz; yeni haklar hesaplar arasında eşitlenmez. Pro doğrulama entegrasyonu korunur.

# Hesap denemesi ve Play doğrulama servisi

Bu kod APK içine gömülen bir sunucu değildir; ayrı HTTPS sunucusuna kurulmalıdır. Varsayılan ZIP'te URL/OAuth alanları boş, dolayısıyla uygulamanın yerel test akışı değişmez. Hesap girişini ve bu servisi etkinleştirmeden yerel veri silme denemeyi sıfırlayabilir.

## Kurulum

1. Google Cloud projesinde Web OAuth client oluştur. Android OAuth client için paket `com.optima.gametranslator` ve ilgili APK imzasının SHA-1 değerini tanımla. Google giriş ekranını/test kullanıcılarını yapılandır. `google_web_client_id` için Web client ID kullan; Android client ID koyma.
2. Play Developer API'yi etkinleştir. Sunucunun servis hesabına yalnızca gereken Play uygulaması/abonelik ve sipariş erişimlerini ver. Kimlik bilgisi yalnızca sunucuda ADC üzerinden okunur. `google-service-account.json` dosyasını APK'ya, ZIP'e veya GitHub'a koyma.
3. HTTPS alan adı, kalıcı Linux sunucusu ve kalıcı veri diski hazırla. `server/` klasörünü `/opt/translator/server` altına aktar. Ayrı `translator` servis kullanıcısı ve yazılabilir `/var/lib/translator` dizini oluştur. SQLite dosyasını düzenli yedekle; bu dosyanın silinmesi deneme kayıtlarını sıfırlar.
4. Python 3.11+ ortamında:

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python -m unittest discover -s . -p 'test_*.py' -v
```

5. `env.example` değerlerini `/etc/translator/server.env` içine koy. Servis kimlik dosyasının izinlerini yalnızca servis kullanıcısına aç. `translator.service.example` ve `nginx.conf.example` içindeki dizin, hostname ve TLS sertifikasını uyarlayıp servisi başlat. Örnekler otomatik olarak kurulmaz.
6. APK kaynaklarında `app/src/main/res/values/strings.xml`:
   - `pro_server_url`: `https://YOUR_API_HOST` (sonuna `/v1` ekleme)
   - `google_web_client_id`: aynı Web OAuth client ID
   - Sunucu modunda `play_license_key` Pro açmak için kullanılmaz; istemci imza kontrolüne geri düşülmez.
7. Yeniden GitHub derlemesi al. Pro kartında Google hesabıyla giriş düğmesi belirir. Aynı hesapla giriş, kamera/mikrofon kullanımını başlatma, durdurma ve yeniden kurulumdan sonra geri yüklemeyi doğrula.
8. Aylık/yıllık/ömür boyu ürünleri PRO_SETUP.md'ye göre tanımladıktan sonra Play lisans test hesaplarıyla gerçek satın alma, acknowledge, pending, iade ve dönem sonunu test et.

## Protokol

Her POST isteği `Authorization: Bearer <Google ID token>` taşır. Sunucu imza/issuer/audience/expiry doğrular; hesap anahtarı doğrulanmış `sub` değerinin SHA-256 özetidir. İstemci `pro` bayrağı sunucu tarafından kabul edilmez.

- `/v1/state`: `{"purchases":[{"product":"translator_pro","token":"..."}]}`. Google Play Developer API sonucu, doğru ürün/temel plan, hesap bağı, geçerli bitiş zamanı ve acknowledge sonrası Pro döner. Ömür boyu satın alım tüketilmez. Aktif, grace period ve iptal edilmiş ama ödenmiş dönemi bitmemiş abonelik geçerlidir; pending/on hold/paused/expired ve iade geçersizdir.
- `/v1/usage`: `{"session":"UUID","elapsedMillis":5000}`. Bu oturumdaki gerçek aktif kullanımın biriken süresi. Aynı bildirimin tekrar gönderilmesi veya eski bildirimin geç gelmesi çift tüketim yapmaz. Farklı cihaz/oturumlar aynı hesabın ortak 180 saniyesine eklenir.
- `/health`: yalnızca çalışma kontrolü; kimlik veya satın alma verisi vermez.

Satın alım, BillingFlow içindeki SHA-256 `obfuscatedAccountId` ile hesaba bağlanır. Başka hesabın token'ı ile geri yükleme reddedilir. Önceki istemci modunda hesaba bağlanmadan alınmış gerçek ödemeler otomatik göç ettirilmez; böyle ödemeler varsa sunucu modunu herkese açmadan önce doğrulanmış sahiplik göçü tasarlanmalıdır.

## Bağlantı ve sınırlar

- Pro erişim cevabı en fazla 5 dakika geçerlidir; abonelik bitişine daha az varsa o süreyi geçmez. Canlı kullanımda sunucu durumu 60 saniyede bir yeniden sorgulanır. Sunucu hatası erişim vermez; canlı kullanım durur. Kalıcı çevrimdışı Pro yoktur.
- Google ID token bellekte tutulur; uygulama yeniden açılınca veya token süresi dolunca kullanıcı yeniden giriş yapar. Token/ödeme sırrı loglanmaz. Sunucu yalnızca hesap özeti, oturum ve tüketilen süreyi saklar; kamera metni, mikrofon sesi ve çeviri geçmişi gönderilmez.
- Kullanım normalde 5 saniyede bir ve durdururken bildirilir. Ani çökme/veri silme veya ağ kopmasında sunucunun henüz almadığı son bölüm kaybolabilir. Ağ gecikmesi bu aralığı uzatabilir. Bu bir değiştirilmiş APK/çoklu Google hesabı/cihaz bütünlüğü koruması değildir; tam kötüye kullanım koruması için attestation ve daha sıkı sunucu oturumları gerekir.
- RTDN push alıcısı bu pakette yoktur. Durum aktif kullanımda periyodik Play sorgusuyla doğrulanır. Play, yeni üretim hesapları, HTTPS, OAuth ve servis hesabı doğrulaması bu ortamda denenmemiştir.
- Üretim yayını öncesi verilerin saklama/silme politikasını ve Google hesabıyla giriş açıklamasını kullanıcıya sun. Hesap kaydı silme ve gizlilik metinleri yayın hazırlığına dahildir; bu paket tamamlanmış bir hukuki politika sunmaz.

## Resmî API kaynakları

- https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation
- https://google-auth.readthedocs.io/en/latest/reference/google.oauth2.id_token.html
- https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2
- https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.products
