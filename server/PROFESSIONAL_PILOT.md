# Profesyonel hizmet — özel pilot kurulumu

Bu hizmet test hesabı içindir. Ortak pilot erişim kodu, üretimde kullanıcı kimliği doğrulama ve ücretli dakika hakkının yerine geçmez. İnternete açık son ürün olarak yayınlamayın. Mevcut satın alma doğrulama server/app.py hizmetinden ayrı portta çalışır.

1. Google Cloud projesi ve faturalandırma oluşturun. Cloud Translation, Cloud Text-to-Speech API'lerini ve seçili Gemini-TTS modeli için belgede gereken hizmet/izinleri etkinleştirin. Bölge, kota ve ücretleri kontrol edin.
2. Ayrı servis kimliği oluşturun; yalnız çeviri/TTS kullanma ve gereken proje tüketici izinlerini verin. Tercihen yönetilen ortamda ADC kimliği kullanın. Servis hesabı dosyasını GitHub'a veya APK'ya koymayın.
3. Python 3.12 ortamında paketleri yükleyin:
   `python3 -m pip install -r requirements.txt`
4. Ortam değişkenleri: GOOGLE_CLOUD_PROJECT, PILOT_TOKEN (rastgele en az 32 karakter), PILOT_BUDGET_DB (kalıcı yazılabilir dosya), PILOT_DAILY_CHAR_LIMIT (varsayılan 20000), PILOT_TTS_MODEL (varsayılan gemini-2.5-flash-tts; alternatif wavenet). Token örneği üretimi: `python3 -c "import secrets; print(secrets.token_urlsafe(32))"`.
5. server klasöründe `python3 professional.py`. Yalnız 127.0.0.1:8081 dinler. TLS ters vekilini /v1/translate ve /v1/speak yollarını bu porta iletecek şekilde kurun; HTTPS, gövde sınırı, bağlantı/istek sınırı ve günlüklerde Authorization/gövde kapatma zorunludur.
6. Telefonun Ayarlar → Doğal internet sesi bölümüne HTTPS kök adresini ve pilot kodunu girin. Kod sağlayıcı anahtarı değildir. İlk replik gerçek hizmet bağlantısını sınar; yapılandırma mesajı bağlantı testi geçti anlamına gelmez.

Bütçe global/günlüktür ve SQLite üzerinden kalıcıdır; çeviri ve ses üretimi metin karakterlerini ayrı ayrı tüketir. Hatalı sağlayıcı çağrıları da rezervasyonu geri almaz. Bu sınır tam dolar bütçesi değildir; bulut konsolunda bütçe uyarıları ve API kotaları da ayarlanmalıdır. Çoklu sunucu/üretim için ortak kota deposu, kullanıcı kimliği, kısa ömürlü erişim, satın alma doğrulama ve dakika bazlı hak gerekir.

Gemini-TTS duygu/stil ve tempo için doğal dil talimatı alır; kesin hız çarpanı garantisi yoktur. Üretilen konuşmanın yazıya sadakati ve gecikmesi gerçek örneklerle ölçülmelidir. Wavenet doğrudan speakingRate kullanır ama duygulu performans sunmaz. Her iki model de bu pilotta repliğin tamamını üretip sonra oynatır; gerçek ses akışı sonraki entegrasyondur. Sağlayıcı isteği zaman aşımı 10 saniye, Android okuma zaman aşımı 12 saniyedir. Argo için sağlayıcı içerik sınırları sonucu etkileyebilir; hata durumunda yerel ses denenir.

Resmî API kaynakları:
- https://docs.cloud.google.com/text-to-speech/docs/gemini-tts
- https://docs.cloud.google.com/translate/docs/reference/rest/v3/projects/translateText
- https://docs.cloud.google.com/text-to-speech/docs/reference/rest/v1/text/synthesize
