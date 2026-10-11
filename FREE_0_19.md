# 0.19 — Ücretsiz cihaz içi İngilizce dinleme pilotu

Yeni varsayılan dinleme motoru Sherpa-ONNX 1.13.8 + İngilizce streaming Zipformer int8 modelidir. Mikrofon sürekli ayrı iş parçacığında yakalanır; ara sonuçlar mevcut canlı yazılı çeviri hattına, tamamlanan bölümler seslendirme kuyruğuna gönderilir. Sessizlikte oturum kendiliğinden sonlandırılmaz. Yakalama hatası yeniden başlatılır. Kamera hattı değiştirilmedi.

## Kurulum
1. Bu klasörün içeriğini mevcut GitHub deposunun köküne kopyalayın. app/libs içindeki AAR dosyasını da ekleyin.
2. Aşağıdaki commit ile gönderin; Actions tamamlanınca yeni APK'yi mevcut indirme akışından yükleyin.
3. Dinle ekranında ücretsiz İngilizce akış seçeneğini açık bırakın. Model indirme düğmesini kullanın: ilk kullanımda yaklaşık 73 MB internet indirmesi gerekir. Dosyalar sabit revizyondan SHA-256 doğrulanarak indirilir.
4. Model hazır mesajından sonra Başlat'a basın ve mikrofon iznini verin. Türkçe ML Kit çeviri modeli de ilk kullanımda indirilebilir.
5. Sesli çeviri için kulaklık kullanın ve kulaklık seçeneğini işaretleyip otomatik okumayı açın. Kulaklık seçimi kullanıcı beyanıdır; donanım algılama değildir. Aksi halde çeviri sesi mikrofona geri girer; bu nedenle ücretsiz sürekli dinleme başlarken otomatik okuma kapatılır.

```bash
git add .
git commit -m "Add free on-device English streaming recognition"
git push
```

## Ücret ve sınırlar
Ücretsiz Dinle modunda ses tanıma cihazda, çeviri ML Kit ile yapılır. Profesyonel sunucunun ücretli çeviri ve ses API'leri bu modda kullanılmaz; sunucu ve API anahtarı gerekmez. İsteğe bağlı Cloud modu ayrı ve ücretlidir. Android TTS kullanılır; ses kalitesi cihazın kurulu motoruna bağlıdır. Daha önce ağ sesi izni verilmişse seçilen TTS sesi internet isteyebilir; tamamen çevrimdışı kullanımda yerel Türkçe sesi seçin.

Mevcut ticari haklar korunur: Kamera 3 dakika, Dinle 3 dakika, görsel 10 adet; Pro aylık 4.99 USD, yıllık 49.99 USD, ömür boyu 199 USD. Ücretsiz motor, Pro deneme sınırlarını kaldırmaz. Sürüm kodu 27, sürüm adı 0.19-free-streaming.

## Telefon doğrulaması zorunlu
Bu bir yeni motor pilotudur. GTA veya yoğun argo için doğruluk ve sıfır gecikme garanti edilmez. Model İngilizce LibriSpeech kökenlidir; özel oyun/argo modeli değildir. Ücretsiz Sherpa modunda Türkçe ses tanıma modeli eklenmedi; ters yön için önceki motor seçenekleri gerekir. Doğal/duygulu yeni bir cihaz içi ses motoru bu pakette yoktur.

Yavaş cihazda 2 saniyelik ses kuyruğu dolarsa oturum görünür hata ile yeniden başlatılır; o aralıktaki ses kaybolabilir. Telefon testinde 10 dakikalık hızlı diyalogda durma, hatalı kelime, gecikme ve pil/ısınma ölçülmelidir. Daha ağır model seçimi gerçek cihaz ölçümünden sonra yapılmalıdır. Konuşmacı kimliği veya otomatik cinsiyet tespiti bu yeni motor tarafından sağlanmaz.

## Doğrulama
Yerelde 33 Python testi geçti. Sabit AAR SHA-256, arşiv bütünlüğü ve 64 bit native dosyaların 16 KB ELF hizalaması kontrolü geçti. Kaynak XML dosyaları parse edildi. Android SDK/Gradle burada bulunmadığından Kotlin derlemesi, Android testleri ve cihaz doğrulaması yapılmadı. CI tam debug/release derleme ve lint kontrollerini çalıştırır.

Lisans metinleri ve model revizyonu licenses/ klasöründedir. Eski CLOUD_0_18 belgesi yalnızca isteğe bağlı Cloud seçeneği için geçerlidir; varsayılan ücretsiz modun sunucuya ihtiyacı yoktur.
