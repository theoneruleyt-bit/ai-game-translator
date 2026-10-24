# Telefon ve yayın öncesi kabul kontrolleri

Bu liste hazırlandı; fiziksel cihaz testleri tamamlanmış olarak işaretlenmedi.

| Kontrol | Beklenen davranış |
|---|---|
| Mevcut APK üzerine güncelleme | Kurulum olur; tema, dil, modeller, ses profilleri ve kalan demo korunur. |
| Aynı altyazı 60 saniye | Tek okuma; sabit saat/HP/FPS/sayaç takılma oluşturmaz. |
| Hızlı altyazı değişimi | Eski bekleyen ses birikmez; başlamış replik tamamlanır. Uzun replikte sıfır ses gecikmesi garantisi yoktur. |
| Kamera alanını canlı sürükleme | OCR yeniden ilerler; meşgul durumda takılı kalmaz. |
| Kamera/mikrofon 10 kez başlat/durdur ve geçiş | Önceki oturum sonuçları yeni ekrana gelmez; kaynaklar serbest kalır. |
| İzni reddet / izin ekranında arka plana geç | Süre sayılmaz; arka planda kayıt başlamaz. Yeniden öne gelip Başlat'a basılabilir. |
| Arka plan / ekran kilidi / tema-dil değişimi | Kamera, mikrofon ve TTS durur; süre korunur. Döndükten sonra kullanıcı yeniden başlatır. |
| Yerel model yokken ağ kesilmesi | İndirme anlaşılır hata verir; demo tüketilmez; bağlantı gelince tekrar denenebilir. |
| Modeller hazırken interneti kapatma (varsayılan test) | Kamera, Vosk ve yerel çeviri çalışır. Ağ gerektiren TTS motorları başarısız olabilir; yerel Türkçe ses kullanılmalı. |
| 60 saniye kamera + 60 saniye mikrofon | Kamera ve mikrofonun her birinde yaklaşık 120 saniye kalır; durdurulan aralık tüketilmez. |
| Demo bitişi | İlgili canlı giriş durur, Pro ekranı açılır; diğer modun ayrı sayacı korunur. Görsel hakkı 10 başarılı çeviridir. Otomatik ödeme yoktur. |
| Türkçe/İngilizce + açık/koyu + büyük yazı | Etiketler görünür, Pro seçenekleri kaydırılır, önemli düğmeler kesilmez. |
| Ses örnekleri / uzun replik / kulaklık | Türkçe yerel ses okunur; başlamış replik kesilmez; hoparlörün mikrofon tarafından tekrar algılanması için kulaklıkla kıyasla. |
| Uzun oturum | Pil/ısı değerlendirmesi için yetkili Pro test hesabıyla 15–30 dakika kamera ve mikrofon ayrı ayrı; çökme/artan kuyruk olmamalı. 3 dakikalık debug demo kendi başına bunu sağlayamaz. |
| Hesap modu etkin, ağ kesildi | Servis hatası canlı kullanımı durdurur. Bağlantı dönünce Başlat ile tekrar kontrol edilir. |
| Aynı Google hesabıyla yeniden kurulum | Sunucunun aldığı deneme tüketimi geri gelir. Bildirilmemiş son bölüm kalıcılık garantisi taşımaz. |
| Play lisans testi | Pending/cancel Pro açmaz; doğru hesaba bağlı verified+acknowledged ödeme açar. Refund/expiry tekrar sorguda erişimi kaldırır; iptal edilmiş ödenmiş dönem bitene kadar korunur. |

Play ürünleri ve sunucu kurulmadan son dört kontrolün tamamı doğrulanamaz. Test sonucunu cihaz modeli, Android sürümü, TTS motoru ve mod ile kaydet.
