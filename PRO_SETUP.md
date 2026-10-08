# Pro yapılandırması ve kabul kontrolleri

## Bu pakette çalışanlar
- Kamera ve mikrofon ortak 180 saniyelik yerel Pro denemesi kullanır. Kamera bağlanınca / mikrofon hazır olunca sayım başlar; izin, model indirme, durdurma ve arka plan süresi sayılmaz.
- Süre dolunca yeni canlı giriş ve bekleyen sesler durur. Başlamış ses bitirilir. Fotoğraf, manuel metin ve kayıtlı ifadeler açık kalır.
- Pro seçenekleri Türkçe/İngilizce sunulur. Deneme otomatik ödeme başlatmaz.
- Aynı ekranda duran metnin görünürlüğü her karede yenilenir; sabit metin filtresi onu artık kaybolmuş saymaz. Kamera yalnızca en yeni bekleyen çeviriyi/sesi tutar. Başlamış ses yarıda kesilmez; bu yüzden uzun bir replik sırasında sesin ekrandan geride kalması hâlâ mümkündür. Geçersizleşmiş çeviri sonucu veya 1.5 saniyeden eski kamera sonucu okunmaz. Mikrofon sırası korunur.

## Google Play yapılandırması (henüz yapılmadı)
1. Paket: com.optima.gametranslator. Play Console içinde abonelik ürünü `translator_pro`, temel planlar `monthly` ve `yearly`: ABD fiyatları $4.99/ay, $49.99/yıl.
2. Tüketilmeyen tek seferlik ürün `translator_pro_lifetime`: ABD fiyatı $199. Ürünleri ve temel planları etkinleştir; yerel vergi/para birimi fiyatlarını Console'da kontrol et.
3. Console lisans RSA açık anahtarını app/src/main/res/values/strings.xml içindeki `play_license_key` alanına koy. Bu açık anahtardır; özel imza anahtarı veya servis hesabı sırrı koyma.
4. Üretim için ayrı güvenli imza kullan. Bu ZIP'teki ortak debug anahtarı yalnızca mevcut test kurulumlarının güncellenmesi içindir.
5. Lisans test kullanıcıları ve uygun Play test dağıtımıyla gerçek satın alma/iptal/bekleyen ödeme/geri yüklemeyi doğrula. GitHub APK kurulumu tek başına bu işlemlerin çalıştığını kanıtlamaz.
6. Canlı yayın öncesi sunucuda satın alma token doğrulama, abonelik durumu ve RTDN ekle. Bu paket istemcide RSA imza, PURCHASED durumu, ürün/paket ve acknowledge kontrolü yapar; ödeme düğmesi asla doğrudan Pro açmaz. Sunucu ve Play Console hesabı bu pakette yapılandırılmamıştır.

## Sınırlar
- Pro yetkisi uygulama açılışında, dönüşte ve geri yüklemede Play'den sorgulanır. Kalıcı çevrimdışı Pro önbelleği yok; Play doğrulaması yapılamayan yeni açılışta Pro açılmaz.
- Demo yerel tercihlerdedir. Uygulama verilerini silmek veya yeniden kurmak denemeyi sıfırlayabilir; hesap/sunucu ile kötüye kullanım koruması henüz yok.
- Satın alma ürünleri/anahtar yokken bilgi gösterilir; ödeme başlatılamaz. Ürünler yüklenirken açılan seçenek ekranını tekrar açmak gerekebilir.
- Geri yükleme tamamlandığında ana karttaki Pro durumu güncellenir.

## Telefonda kabul testi
1. Aynı altyazıyı 60 saniye ekranda tut: tek okuma. HUD saati/sayaç tek başına okunmamalı.
2. Farklı altyazıları hızlı geçir: bekleyen eski replikler birikmemeli; başlamış replik tamamlanmalı. Metin en az 1 saniye gerçekten kaybolduktan sonra aynı replik yeniden okunabilir.
3. 60 saniye kamera, durdur, 60 saniye mikrofon, durdur: ortak kalan süre yaklaşık 60 saniye olmalı. Uygulamayı kapatıp açınca korunmalı.
4. Model indir/izin bekle/arka plana geç: süre tüketilmemeli. 180 saniyede canlı giriş durmalı ve Pro ekranı açılmalı; otomatik ödeme olmamalı.
5. İngilizce/açık/koyu tema, fotoğraf, metin, kayıtlı ifadeler ve mevcut sürüm üstüne APK güncellemesini kontrol et.
6. Play yapılandırılınca her planı lisans test hesabıyla satın al; pending/cancel Pro açmamalı, verified/acknowledged purchase açmalı. Yeniden açılış/geri yükleme ve iptal sonrası dönem sonunu doğrula.
