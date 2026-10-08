# Pro ve demo yapılandırması

## Ürünler
- Abonelik ürünü: `translator_pro`; temel plan `monthly`: ABD $4.99/ay, `yearly`: ABD $49.99/yıl.
- Tüketilmeyen tek seferlik ürün: `translator_pro_lifetime`: ABD $199.
- Kamera ve dinleme için ortak, aktif kullanımda tüketilen 180 saniyelik uygulama demosu. Google Play abonelik denemesi değildir; otomatik ödeme başlatmaz.
- Durdurma, indirme, izin bekleme ve arka plan süreleri sayılmaz. Süre bitince yeni canlı giriş ve bekleyen sesler durur; başlamış replik tamamlanır. Fotoğraf, manuel metin ve kayıtlı ifadeler açık kalır.

## İki çalışma modu
1. Varsayılan yerel test: sunucu URL/OAuth alanları boş. Play istemci imzası için `play_license_key` açık RSA anahtarı gerekir. Doğrulanmamış veya pending ödeme Pro açmaz. Yerel demo veri silmeyle sıfırlanabilir.
2. Hesaplı sunucu: `pro_server_url` + `google_web_client_id` doldurulur. Kullanıcı Pro kartından Google hesabıyla giriş yapar. Ödeme akışı hesap hash'ini Play'e iletir. Erişimi yalnızca sunucu Play API doğrulaması ve acknowledge açar. İstemci RSA kontrolüne geri düşülmez. Ayrıntılı kurulum `server/README.md`.

## Yayın/test hazırlığı
- Play Console ürün/temel plan/yerel fiyatları etkinleştir, lisans test kullanıcılarıyla uygun test dağıtımını hazırla. APK'yı GitHub'dan indirmek tek başına gerçek ödeme testi değildir.
- Üretim için ayrı güvenli imza kullan; ortak debug anahtarını üretimde kullanma. Android OAuth imza parmak izi ilgili dağıtımla eşleşmeli.
- Servis hesabı dosyası yalnızca sunucuda kalır. APK/GitHub'a özel anahtar veya JSON servis hesabı koyma.
- Gerçek ödeme, iptal, pending, refund, abonelik dönem sonu ve geri yüklemeyi test et. Sunucu modundan önce alınmış hesapsız gerçek satın almaların göçü ayrı doğrulanmalı.
- Hesap modunda kalıcı çevrimdışı Pro ve RTDN push alıcısı yok; aktif kullanımda periyodik Play doğrulaması var. Sunucu kullanım bildirimi istemciye dayanır; kötü niyetli değiştirilmiş APK/çoklu hesap engeli değildir.
