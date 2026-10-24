# Veri güvenliği çalışma taslağı

Bu form gönderilmedi. Son karar, mağazaya yüklenecek yapılandırma ve üçüncü taraf SDK davranışları doğrulanarak verilmelidir. Kaynak incelemesi dinamik ağ trafiği denetiminin yerine geçmez.

| Veri / işlem | İncelenen davranış | Formda değerlendirme |
|---|---|---|
| Kamera görüntüleri | CameraX/OCR cihazda; ham video kaydı yok | Yalnız cihazda işlenen görüntü ağda toplanmış sayılmaz; bulut seçeneklerini ayrıca değerlendirin |
| Ücretsiz Dinle sesi | AudioRecord + yerel Sherpa; ham kayıt kalıcı dosyaya yazılmaz | Varsayılan yerel akışta sunucuya ses gönderimi bulunmadı |
| İsteğe bağlı bulut ses/metin | Kullanıcının yapılandırdığı HTTPS/WSS hizmetine gider | Etkin olduğunda ses ve diğer kullanıcı içeriği aktarımı değerlendirilmelidir; yalnız varsayılanı beyan etmek yeterli değildir |
| Android konuşma/TTS | Kullanıcının seçtiği sistem sağlayıcısı ağ kullanabilir | Sağlayıcı koşulları ayrıca geçerli; tüm seslerin çevrimdışı olduğunu söylemeyin |
| ML Kit kurulum/cihaz kimlikleri | SDK tanılama ve analitik amacıyla toplayabilir | Device or other IDs; zorunlu/isteğe bağlı ve kullanım amacını SDK sürümüne göre doğrulayın |
| ML Kit gecikme/hata/kullanım olayları | SDK performans ve kullanım metrikleri | App info and performance → Diagnostics / Other app performance; App activity → App interactions eşlemesini değerlendirin |
| Dil ayarları / model indirme | ML Kit kaynak-hedef dilleri ve model olayları; Google/Hugging Face bağlantıları | Analitik/tanılama ve sunucu bağlantısının ağ metaverisi (IP dahil) sağlayıcı politikalarına tabi |
| Satın alma | Google Play işleme; varsayılanda uygulama ödeme kartı bilgisi almaz | Satın alma geçmişi ve Play Billing SDK açıklamalarını değerlendirin; kart bilgisi topluyor diye işaretlemeyin |
| Geçmiş/favoriler | SharedPreferences, cihazda; kullanıcı paylaşabilir/silebilir | Salt yerel saklama otomatik ağ paylaşımı değildir; paylaşım kullanıcı başlatır |
| Hesap sunucusu | Varsayılan boş/devre dışı | Bu sürümde hesap oluşturma yok. Sonradan etkinleştirilirse hesap silme akışı, dış silme URL'si ve beyanlar ayrıca tamamlanmalı |

ML Kit belgelerine göre SDK metrikleri HTTPS ile aktarılır; üçüncü taraf paylaşım değerlendirmesi Play'in hizmet sağlayıcı istisnalarına ve gerçek yapılandırmaya göre yapılmalıdır. Genel "veri toplanmıyor" yanıtı bu SDK'larla desteklenmiyor.

Reklam SDK'sı ve AD_ID izni doğrudan kaynakta bulunmadı. Son birleştirilmiş manifest raporu ayrıca kontrol edilir. Reklam yok beyanını son paketle doğrulayın. Hassas izinler kamera ve mikrofon; geniş fotoğraf/depolama izni yok, seçim sistem dosya seçicisiyle yapılır.

Gizlilik politikası hem uygulamada hem Play'de erişilebilir olmalı; destek kimliği ve irtibat bilgisi gerçek olmalı. Kullanıcı ses/görüntülerini model eğitimi için kullandığımız iddiası bulunmamalı; mevcut geliştirici akışında böyle bir kullanım tespit edilmedi.


## 0.30 ek doğrulama (23 Ekim 2026)

- Kullanıcı gelişmiş çeviri ve kameranın çalıştığını bildirdi; bu bir cihaz kullanım bildirimidir. Satın alma/abonelik/iade, farklı cihaz ve ağ trafiği testleri yerine geçmez.
- İsteğe bağlı Hy-MT2: model indirilir; çeviri girdisi model deposuna gönderilmez. İki kısa önceki kaynak satır RAM bağlamında tutulur; kalıcı model eğitimi yapılmaz.
- Kurulum/cihaz kimlikleri, SDK performansı ve olaylar nedeniyle genel “veri toplanmıyor” işaretlenmemelidir. ML Kit belgeleri yalnız güncel SDK sürümlerini tarif eder; kullanılan sürüm ve son ağ davranışı ayrıca doğrulanmalıdır.
- Bulut seçeneği uygulamada hâlâ erişilebilir. Veri güvenliği beyanında yalnız varsayılan yerel modu dikkate almayın: kullanıcı bulutu açınca ses/metin dış hizmete aktarılabilir. Ağ üzerinde geçici işleme de form kapsamındadır.
- İç test yalnız bu kanalda aktifse Data safety bölümü istisnası bulunur; kapalı/açık/üretim aşamasında form tamamlanmalıdır. İstisna gizlilik politikasını ve gerçek veri davranışını doğru açıklama sorumluluğunu kaldırmaz.
- Form, bu ortamdan gönderilmedi; varsayımları gerçek dışı “Hayır” yanıtlarına dönüştürmeyin.

Resmî kaynaklar (kontrol: 23 Ekim 2026):
https://developers.google.com/ml-kit/android-data-disclosure
https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
