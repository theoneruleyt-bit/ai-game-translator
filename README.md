# Güncel: 0.31-play-rc (42)

Önce UPDATE_0_31_TR.md ve VALIDATION_0_31.json dosyalarını okuyun. Bu paket kaynak düzeltmesidir; Android derlemesi henüz doğrulanmadı.

# 0.30 — Play release candidate

See [PLAY_YUKLEME_ADIMLARI_TR.md](play-store/PLAY_YUKLEME_ADIMLARI_TR.md). This source is prepared for release checks. A signed upload bundle requires the real private Play configuration. Store prices: USD 5.99 monthly, 69.99 yearly, 99.99 lifetime; displayed prices come from Google Play ProductDetails.

# 0.29 context translation beta

Read [UPDATE_0_29_TR.md](UPDATE_0_29_TR.md) for installation, the optional 1.83 GB model download, phone requirements, and limitations. This is an opt-in experiment, not a claim of professional translation accuracy or Play publication readiness. Camera/ASR/TTS remain on the existing paths.

# AI Game Translator 0.28 — Dinle takılma düzeltmesi

Android 10+, API 36, sürüm kodu 39. Bu sürüm doğrulanmış akış hatalarını düzeltir; kusursuz oyun tercümesi veya telefon üzerinde ölçülmüş düşük gecikme sertifikası değildir.

## Bulunan somut hatalar

1. Sherpa Zipformer metni BÜYÜK HARF ile veriyor. Noktalama/case modeli bu girdilerde ara cümle işaretleri üretmiyordu. Önceki küçük harfli testler bunu kaçırdı. Artık model girdisi Locale.ROOT ile normalleştirilir. Aynı native modelle önce/sonra kanıtı: `verification/punctuation-case-0_28.json` (3 gerçek native ASR transkripti ve 2 yazılı örnek). Android JNI ve Python aynı AddPunctuationWithCase girişini kullanır.
2. Kelimeler düzeltilirken tamamlanmış Türkçe taslak sadece eski İngilizce önek birebir aynıysa gösteriliyordu. Sürekli yeniden yazılan metin ekranı boş/takılmış gösterebiliyordu. Artık aynı konuşma parçasındaki en son tamamlanan taslak gösterilir, daha yeni kesin sonuçların üstüne eski taslak yazılamaz. Taslak etiketi korunur; kesinleşmeden seslendirilmez.
3. Onaylanmış kısa parça, henüz onaylanmamış devam metni yüzünden süresiz bekleyebiliyordu. Bekleme en fazla 1.25 saniye; bu süre sadece zaten onaylanmış kısa parçaya uygulanır. Onaysız kelimeler süre doldu diye seslendirilmez.

Hazır ifade karşılıkları yeni/varsayılan ayarlarda artık kapalıdır. Kullanıcının açıkça kaydettiği seçim korunur. Bunlar geniş bir dil modeli değildir; bağlama göre anlamı değiştirebilir. İlk karşılaştırmada hazır ifade paketi ve deneysel OPUS-MT kapalı, anlam grubu modu açık, hız Dengeli olsun. Bu sürüm yeni çeviri modeli veya yeni ücretli servis eklemez. Google ML Kit çevrimdışı çevirisi oyun argo ve bağlamında sınırlıdır.

Gelişmiş İngilizce (190 MB) ve Hafif İngilizce (73 MB) seçenekleri/önbellekleri korunur. 0.27 modelleri yeniden indirilmez. Telefon ağır kalırsa aynı kaydı iki modelle karşılaştırın. Kamera, görsel, Türkçe Android TTS ve Pro akışı korunur. Test APK'sı önceki açık test sertifikasıyla imzalıdır.

## Telefon üzerinde kısa teşhis

Ayarlar → Dinle tanılamasını tut → Dinle modunda 20–30 saniye sorunlu kaydı çalın → durdurun → Ayarlar → Dinle tanılamasını paylaş.

Tanılama isteğe bağlı ve varsayılan kapalıdır. Yalnız en son 128 metin/zaman olayı bellekte kalır; ses kaydedilmez. Otomatik gönderim yoktur. Kapatınca veya uygulama yeniden oluşturulunca kayıt silinir. Paylaşım metin içerir; kişisel konuşmalar yerine izinli bir test kaydı kullanın. `asr_draft`/`asr_confirmed`, `translation_start`/`translation_draft`/`translation_final`/`translation_error`, `voice_queue` aşamalarını ayırır. voice_queue gerçek hoparlör oynatma zamanı değildir; sourceAgeMs tanımadan önceki gecikmeyi ölçmez.

Kaynak İngilizce yanlışsa ASR/mikrofon; kaynak doğru ve Türkçe yanlışsa çeviri modeli; Türkçe hazır olup ses gecikiyorsa ses kuyruğu incelenir. Gerçek oyun klibi ve bu rapor olmadan kalan sorunun cihazdaki kök nedeni kesinleşmez.

## Kontroller ve GitHub

```bash
gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:bundleRelease :app:lintRelease --no-daemon --continue
git add .
git commit -m "Fix uppercase ASR punctuation, rewritten live previews and held confirmed fragments"
git push
```

Güncel sonuçlar: `VERIFICATION_0_28.md`, `RELEASE_AUDIT_0_28.json`. Önceki raporlar tarihseldir. 0.27 ASR karşılaştırması, 0.28 Türkçe çeviri kalite testi olarak sunulmaz.

Play yayını: [play-store/BASLA.md](play-store/BASLA.md). Gerçek özel imza, lisans anahtarı, destek/politika URL'si, cihaz ve mağaza testleri henüz tamamlanmış sayılmaz. Deneme hakları kamera 180 saniye, Dinle 180 saniye, görsel 10 adet; Pro aylık $4.99, yıllık $49.99, ömür boyu $199 olarak korunur.

Teknik sınırın resmî kaynağı: https://developers.google.com/ml-kit/language/translation — cihaz içi model günlük/basit çeviriler içindir; özel kullanım kalitesi ayrıca değerlendirilmelidir.
