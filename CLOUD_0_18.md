# 0.18 — Gerçek mikrofon akışı / Google STT V2

Dinle kartında Google canlı dinleme varsayılan olarak seçilir. Önceki Vosk/sistem motoruna sessizce geri dönüş yapılmaz. Kurulum yoksa hizmet bağlama ekranı açılır. Kamera motoru korunur.

Android mikrofonu: 16 kHz mono PCM, 50 ms parçalar, TLS WebSocket. Python köprü: Google Speech-to-Text V2 chirp_3 gRPC streaming. Geçici sonuç yazılı önizlemede düzeltilir; kesin sonuç çeviri/ses kuyruğuna girer. Nihai olay kimlikleri aynı bağlantıda tekrarı engeller. Geçici sonuç tahmin olduğundan yanlış olabilir; metinde geçici etiketi korunur. Modelin olmayan kelimeleri asla üretmeyeceği garanti edilmez.

Telefon hoparlöründeki Türkçe sesin tekrar tanınmasını azaltmak için canlı bulut dinlemede kulaklık seçeneği kapalıysa otomatik seslendirme kapatılır. Gerçek kulaklık kullanıp seçeneği açın; checkbox donanımı algılamaz. İlk test yazılı çeviriyle yapılmalıdır.

## Gerekli kurulum

1. Google Cloud hesabı/proje, faturalandırma, Speech-to-Text V2, Translation, Text-to-Speech API'leri. Projedeki chirp_3 modelinin seçilen bölge/dilde kullanılabilirliğini doğrulayın; varsayılan SPEECH_REGION=us. Servis kimliğine belgelerde gereken Speech Client ve çeviri/TTS tüketim izinlerini verin. ADC kullanın; anahtar dosyasını APK'ya, ZIP'e veya GitHub'a koymayın.
2. server/requirements.txt bağımlılıklarını kurun. PILOT_TOKEN (32+ rastgele karakter), GOOGLE_CLOUD_PROJECT, SPEECH_REGION, kalıcı SPEECH_BUDGET_DB ve PILOT_BUDGET_DB ayarlayın. PILOT_TOKEN iki hizmette aynı olmalı.
3. İki ayrı işlem çalıştırın: `python3 professional.py` (127.0.0.1:8081) ve `python3 streaming.py` (127.0.0.1:8082).
4. Tek HTTPS alanında /v1/translate ve /v1/speak yollarını 8081'e; /v1/listen yolunu 8082'ye WebSocket Upgrade ile iletin. Ters vekil okuma süresi 300 saniye, bağlantı/istek sınırları ve hassas başlık/gövde günlüklerini kapatma gerekir. Güvenlik duvarında bu yerel portları açmayın.
5. Ayarlar → Doğal internet sesi / Dinle → Canlı dinleme hizmetini bağla: HTTPS kök adresi ve pilot erişim kodu. Kod yalnız uygulama oturumunda tutulur. Sağlayıcı anahtarı telefona girilmez.
6. Dinle → Google canlı dinleme seçili → Başlat. Yazılı ve ardından kulaklıkla sesli çeviriyi aynı kayıt üzerinde ölçün.

## Önemli sınırlar

Bu özel pilot için hazır bağlantı kodudur; servis henüz konuşlandırılmadı ve gerçek Google akışı test edilmedi. Genel kullanıcıya açmadan kullanıcı kimliği, kısa ömürlü erişim, satın alma hakkı ve kişi başına kota gerekir. Ortak pilot kodunu müşterilere dağıtmayın.

Varsayılan sunucu kotası günde toplam 600 saniye ses; SPEECH_DAILY_SECONDS ile değişir. Kota sessiz PCM dahil gelen bütün ses baytlarını sayar ve ayrı SQLite dosyasında kalıcıdır. Yerel kamera 3 dk / dinleme 3 dk / görsel 10 adet deneme ve Pro fiyatları korunur; bulut kullanımı Pro için sınırsız değildir.

Akış servis sınırını aşmamak için 4 dakikada bağlantı yenilenir. Bu ilk sürümde yenileme/bağlantı kopması sırasında kısa ses boşluğu olabilir; kayıpsız sonsuz akış vaat edilmez. Tampon birikmesi sınırlandırılır; bozulmuş ses bağlantısı yeniden açılır. Kamera algoritması değiştirilmedi. Müzik ve üst üste konuşan kişiler modelin doğruluğunu etkiler; otomatik gerçek kişi/gender/duygu tespiti yoktur. Kesinleşen repliği bekleyen seslendirme sıfır gecikmeli değildir; bulut çeviri ve mevcut senkron TTS gecikmesi ayrıca ölçülmelidir.

## GitHub
ZIP içindeki klasörün içeriğini mevcut depo köküne kopyalayın:
```
git add .
git commit -m "Add Google STT V2 microphone streaming gateway"
git push
```
28 Python servis + 5 denetleyici testi yerelde geçti. Bunlar gerçek Google hesabını kullanmaz. Android SDK/Gradle burada yok; APK, Kotlin testleri ve lint Actions'ta doğrulanacak.

Resmî kaynaklar:
https://docs.cloud.google.com/speech-to-text/docs/streaming-recognize
https://docs.cloud.google.com/speech-to-text/docs/models/chirp-3
https://websockets.readthedocs.io/en/15.0.1/reference/sync/server.html
