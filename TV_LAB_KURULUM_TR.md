# TV LAB — İlk deneysel entegrasyon

Bu yama mevcut depoya eklenir. app/src/debug dizinini içerir; release sürümü ve telefonun mevcut MainActivity kodu değiştirilmez. app/src/debug/AndroidManifest.xml zaten varsa activity ve uses-feature tanımlarını mevcut debug manifestiyle birleştirin; mevcut tanımları silmeyin.

## Eklenenler
- Test APK’sında ayrı Translator TV Lab simgesi; Android TV Leanback ve telefondan açılabilir test ekranı.
- Kumandanın yön/OK tuşlarıyla odaklanabilen düğmeler; kaydırılabilen büyük ekran yerleşimi.
- ML Kit ile üç örnek İngilizce cümlenin gerçek Türkçe çevirisi. İlk model hazırlığı internet gerektirebilir. Kullanıcının gerçek konuşması veya oyun görüntüsü işlenmez; demo deneme hakkı tüketmez.
- Yazı büyütme/küçültme, üst/alt yerleşim ve durdurma.
- API/TV/üst katman izni durum bilgisi; hiçbir ekran yakalama veya üst katman izni istemez.
- Arka plana geçince demo durur; geç gelen sonuçlar ekrana yazılmaz.

## Henüz yok / doğrulanmadı
HDMI yakalama, gerçek ekran OCR’si, başka uygulamanın üstüne altyazı, arka plan servisi, TV seslendirmesi ve telefon–TV aktarımı bu ilk yamada uygulanmadı. Yakalama servisinin varlığı HDMI uyumluluğunu göstermez. Telefon kamera/Dinle özelliklerinin çalışması bu TV özelliklerini doğrulamaz. TV mağaza yayınına hazır bir sürüm değildir.

## Kurulum
ZIP içeriğini mevcut Git deposunun köküne çıkarın. CMD’de:

git add app/src/debug TV_LAB_KURULUM_TR.md
 git commit -m "feat: add debug-only TV translation lab"
 git push origin main

İlk satırdan önce git status ile devam eden rebase olmadığını kontrol edin. Ana Android final checks işi debug kaynaklarını derler. Yeşil olduğunda Releases taslağından APK’yı indirin. Telefonda Translator TV Lab simgesini açıp örnek çeviriyi test edebilirsiniz; mevcut AI Game Translator simgesi kendi akışını sürdürür.

## Emülatör / cihaz kontrolü
1. Android TV API 29+ emülatörde veya gerçek cihazda test APK’sını kurun.
2. TV ana ekranından TV Lab simgesini açın; başlangıç odağını kontrol edin.
3. Yön tuşları, OK ve Back ile bütün düğmelere ulaşın.
4. Örneği başlatın, Türkçe sonuçların sırayla geldiğini kontrol edin.
5. Yazı boyutu ve üst/alt konumu değiştirin.
6. Home’a basın: demo durmalı. Geri dönünce otomatik başlamamalı.
7. Ağ yok ve model yok durumunda hata görünmeli; tekrar denenebilmeli.
8. Bu kontroller HDMI, ekran yakalama veya overlay desteğini doğrulamaz.

## Bu yamanın doğrulaması
XML dosyaları ayrıştırıldı ve ZIP CRC kontrolü yapıldı. Bu ortamda tam Android derlemesi, emülatör ve fiziksel TV testi çalıştırılmadı. GitHub derlemesi ve sonraki cihaz kontrolleri bekleniyor. Debug dışına taşımadan önce gerçek görüntü/üst katman prototipi, erişim/Pro denetimi ve TV yayın gereklilikleri tamamlanmalıdır.
