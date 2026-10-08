# AI Game Translator — Android 0.2 Camera MVP experiment

Ana amaç: telefonu TV/monitöre yönelt → İngilizce ekran altyazısını OCR ile oku → Türkçe çeviri göster → Türkçe sesli oku.
Ek modlar: canlı kamerada bir kareyi oku; galeriden fotoğraf/ekran görüntüsü seç ve çevir. Video dosyası veya oyunun sesini dinleme bu sürümde yoktur. Kaynak dil bu deneyde İngilizce, hedef Türkçe.

## İçerik ve sınırlar
CameraX arka kamera önizlemesi + ML Kit cihazda Latin OCR + EN→TR yerel çeviri + Android Türkçe TextToSpeech. Gerçek sağlayıcı entegrasyonları vardır; demo çeviri kullanılmaz. Ancak APK derlemesi ve cihaz sonucu henüz doğrulanmadı. Bu bir teknik deney; ticari üstünlük iddiası değildir.

İlk model indirme internet/veri gerektirir. Sonraki çeviri yerel çalışır; Türkçe sesin çevrimdışı çalışması telefonun kurulu TTS motoru/ses paketine bağlıdır. Görüntüler uygulama tarafından sunucuya yüklenmez veya dosyaya kaydedilmez. ML Kit/Google bağımlılıklarının kendi veri davranışı mağaza gizlilik incelemesinde ayrıca kontrol edilecek. Mikrofon, ekran paylaşımı, overlay ve genel galeri erişimi izni istenmez; seçilen görsele Android picker üzerinden erişilir.

Canlı modda alt %45 metin filtresi varsayılan açık. Bu oran kamera görüntüsüne göre hesaplanır; telefonun kadrajına TV altyazısını alt tarafta yerleştir. İşareti kaldırınca tüm görüntüdeki yazılar okunur. Bu bir kullanıcı tarafından çizilen alan seçimi değildir. Galeri ve tek kare modu bütün görüntüyü okur.

600 ms metin kararlılığı ve ardışık tekrar bastırma vardır. 1.5 sn metinsiz gözlem sonrasında aynı replik yeniden okunabilir. OCR hatalarında bu filtre yeterli olmayabilir. Ses kuyruğu büyümez; yeni çeviri eski okumayı keser. Bu nedenle hızlı repliklerde okuma yarıda kalabilir. Model bağlam/argo kalitesi ve kameranın parlaması, açı, ekran titreşimi gerçek cihazda ölçülmeli.

## Önceki paketin yerine kurulum
0.1 paketinin yanlış ekran yakalama akışını bu paket değiştirir. ZIP içeriğini depo köküne kopyala, sorulursa aynı dosyaları değiştir. `.github` klasörünü de kopyala. Eski `app/src/main/java/com/optima/gametranslator/CaptureService.kt` dosyasını sil; yeni manifest bu servisi kullanmaz. Keystore veya kullanıcıya ait farklı dosyaları silme.

Depo klasöründe:

```bash
git add .
git commit -m "Switch to live camera OCR translation and Turkish speech"
git push
```

GitHub → Actions → Android APK → başarılı run → Artifacts → AI-Game-Translator-0.2-camera-debug. Arşivden app-debug.apk çıkarıp telefona kur. Aynı applicationId, versionCode=2 ile önceki debug APK üzerine güncellenebilir (aynı imza kullanılırsa).

Wrapper bu pakette yok. Workflow Gradle 8.11.1 kurar; JDK17, AGP8.9.2, Kotlin2.1.20, compile/target35, min29. Mağaza yayını öncesi hedef SDK güncel gerekliliklere göre tekrar seçilecek. Android Studio/Hermes gerektirmeyen GitHub akışı korunur.

## İlk telefon testi
1. EN→TR modelini hazırla; indirme bitmesini bekle.
2. Canlı kamerayı başlat; kamera iznini ver.
3. TV/monitörde İngilizce altyazı aç. Kamerayı net, sabit, yazılar büyük görünecek şekilde tut.
4. Türkçe metin ve ses kontrolü. Ses çıkmazsa Android metinden konuşma ayarlarına Türkçe ses yükle. Ses kutusuyla otomatik okumayı kapatabilirsin.
5. Tek replik ekranda 10 sn kalınca tekrar tekrar okunmamalı. Farklı replik gelince güncellenmeli.
6. Durdur; yeniden başlat. Uygulamadan ayrılınca kamera ve ses durur.
7. Kareyi dondur ve çevir ile bir sonraki kareyi oku; galeri modunda fotoğraf ve ekran görüntüsünü ayrı ayrı dene.
8. İzni reddet, modeli internetsiz hazırla, ekranı döndür ve uygulamayı arka plana al: hata/yaşam döngüsü sonuçlarını kaydet.

Bu deneyde rotasyon sonrası Activity yeniden oluşturulur ve canlı oturum durur; model hazırlama düğmesine yeniden basıp başlatılabilir. Kalıcı oturum/profil ürün aşamasında eklenecek.

## Doğrulama
Bu ortamda Android SDK/Gradle/Kotlin compiler yok; APK build/lint/unit test çalıştırılamadı. Manifest XML ve ZIP bütünlüğü kontrol edildi. 4 davranış testi CI'ya eklendi: geçici OCR, tekrar bastırma, gerçek tekrarı yeniden okuma, boş kare olmadan yeni replik. CI build+test+lint koşar; ardından cihaz testi zorunludur. Başarılı derleme çeviri kalitesi kanıtı değildir.

Resmi kaynaklar:
https://developer.android.com/media/camera/camerax/analyze
https://developer.android.com/jetpack/androidx/releases/camera
https://developers.google.com/ml-kit/vision/text-recognition/v2/android
https://developers.google.com/ml-kit/language/translation/android
