# 0.20 — Dinleme ve çeviri hattı incelemesi

Temel: 15 Ekim 2026 tarihinde yüklenen ai-game-translator-main.zip. Bu sürüm ücretsiz İngilizce → Türkçe telefon uygulamasıdır. TV/konsol sesi telefon mikrofonundan alınır; konsolun iç sesini doğrudan yakaladığı iddia edilmez. Google Çeviri'nin özel motoru kullanılmaz.

## Bulunan hatalar ve yeni akış

### Konuşma olmayan seste kelime üretimi
Gerçek Zipformer motoruna 5 saniyelik sıfır örnekli ses verildiğinde eski doğrudan giriş hattı hayali kelimeler üretti. Kelimeleri sonradan sözlükle düzeltmek bu sorunu çözmez. Yeni Silero VAD modeli uygulamayla birlikte gelir. VAD'ye daima tam 512 örneklik pencereler verilir; mikrofonun 800 örneklik okuması veya başka bir parça büyüklüğü pencere sınırlarını değiştirmez.

Beklerken gelen ses ASR'ye verilmez. Konuşma tespit edilince son 400 ms'lik başlangıç tamponu da gönderilir; ilk heceler sadece algılama gecikti diye atılmaz. Başlayan konuşmanın tüm devamı ve bitiş sessizliği bölüm kesinleşene kadar korunur. VAD, dil/kimlik/cinsiyet veya kelime doğruluk puanı değildir. Müzik içindeki vokal, zor aksan veya yoğun gürültü hâlâ yanıltabilir. VAD için ekstra hesap, anahtar veya kullanıcı ücreti yoktur. Ses analizi cihazda yapılır.

### Geçici çevirinin kesinleşmiş cümleyi bekletmesi
Kesin cümleler ve geçici önizleme farklı ML Kit istemcileriyle işlenir. Önizleme meşgul olsa da kesinleşmiş cümle kuyruğu ilerler. Kesin sonuç ikinci defa önizleme çevirisine gönderilmez. Her önizleme istek kimliği taşır; önceki konuşmanın geç gelen sonucu yeni metni veya meşgul durumunu değiştiremez.

Önizlemede 2, kesin çeviride 12 saniyelik uygulama bekleme sınırı vardır. Süre aşımı alttaki native görevin iptal edildiği anlamına gelmez. Bu yüzden native istekler ayrıca sayılır: en fazla bir önizleme ve iki kesin görev çalışabilir; fiziksel iş bitmeden kontenjan açılmaz. Motor takılırsa sınırsız yeni istek oluşturulmaz. Kesin çeviri sınırlı tekrar sonrası başarısız kalırsa kaybolmuş gibi susmak yerine çevrilemeyen kaynak bölüm gösterilir. Bu, her bölümün mutlaka çevrildiği garantisi değildir.

### Hata sonrası konuşmanın karışması
Mikrofon yeniden başlatılırken önizleme kimliği, önbellek, süre bilgisi ve bekleyen UI görevi sıfırlanır. Kesinleşmiş çeviri kuyruğu geçici mikrofon toparlanmasında korunur. Kısa fakat kesinleşmiş bir parça native yakalama hatasından önce dışarı verilir; henüz doğrulanmamış kelimeler kesin sonuçmuş gibi sunulmaz. Kısa parça birleştirme zamanları CPU işlem süresine değil işlenmiş ses örneği sayısına dayanır.

### Yanlış bağlamda hazır sözlük karşılığı
Dinle modunda kısa cümlelere zorunlu oyun/argo karşılığı dayatılmaz. Hazır karşılıklar açıkça açılabilen bir seçenektir ve varsayılanı kapalıdır. Yaygın konuşma kısaltmalarının genişletilmesi ayrı mevcut ayarla sürer. Kamera sözlük davranışı korunur. Eski Vosk dil paketleri ve Android motor testi, yeni canlı motor seçiliyken gizlenir; kullanıcı başka motoru test edip aynı motoru kullandığını sanmaz.

### Seslendirme ve model bütünlüğü
Uzun metinler mümkün olduğunda noktalama/kelime sınırlarında bölünür; Unicode karakterinin UTF-16 çifti iki ayrı parçaya ayrılmaz. Her parçanın kaynak konuşma süresi uzunluğuyla orantılanır. Ses servisi bağlantısı hata verirse uygulama ana iş parçacığında çökmez. Önceki TTS zaman aşımı ve kesilmeden FIFO okuma korunur. Yerel ve isteğe bağlı bulut sesi medya ses kanalını kullanır.

İngilizce model dosyalarının SHA-256 değerleri native model açılmadan önce kontrol edilir; aynı boyutlu bozuk dosya da tespit edilir. Bundled VAD modeli hem CI'da hem başlangıçta hash ile kontrol edilir. Mikrofon durumu düşük seviye, kırpılma ve gerçek işleme kuyruğu gecikmesini gösterir. Bu uyarılar tanıma doğruluk puanı değildir; yalnızca ses yüksekliği düşük diye örnekler kesilip atılmaz.

## Kullanım
1. APK'yi mevcut uygulamanın üzerine yükleyin. İmza mevcut test anahtarıyla aynıdır; sürüm kodu 31.
2. Dinle → ücretsiz İngilizce canlı motor → Dengeli. Önceki İngilizce model indirmesi korunur.
3. Türkçe sesi dene düğmesini kullanın. Yerel Türkçe TTS paketi gerekir.
4. Kulaklıkla otomatik okumayı açın. Telefon hoparlörünün sesi mikrofona geri girebileceği için kulaklık koşulu korunur; kutucuk gerçek cihaz yönlendirmesini doğrulamaz.
5. Kaynak İngilizce metni, Türkçe çeviriyi ve ses kuyruğunu ayrı değerlendirin. Ara metin değişebilir; yalnızca kesinleşmiş bölüm seslendirilir.

Kamera tanıma hattı ve deneme hakları değiştirilmedi: kamera 3 dakika, dinle 3 dakika, görsel 10 adet. Pro fiyatları aylık 4.99 USD, yıllık 49.99 USD, ömür boyu 199 USD. Ücretsiz motor bu ticari deneme sınırlarını kaldırmaz. İsteğe bağlı Cloud ayarları ayrıca mevcuttur; ücretsiz Dinle modunda o çeviri/ses servisi çağrılmaz.

## Gerçek sınırlar
Kusursuz, sıfır gecikmeli veya her argo/aksanı doğru çeviren bir sistem olduğu iddia edilmez. ML Kit cihaz içi çeviri Google'ın belgesinde basit/gündelik çeviriler için tanımlanır; çevrimiçi Google Çeviri kalitesiyle eşdeğer olduğu varsayılmamalıdır. Kaynak: https://developers.google.com/ml-kit/language/translation . Yanlış İngilizce tanınırsa Türkçe anlam da bozulabilir. Bu sürüm doğal/duygulu yeni TTS veya otomatik konuşmacı kimliği eklemez. Kesintisiz uzun konuşmada bölüm sonunu beklemek sesli okumayı geciktirebilir. Telefon mikrofonu, TV müziği ve gerçek oyun diyalogları kullanıcı cihazında ayrıca doğrulanmalıdır.

## Doğrulama
Tamamlanmış çalıştırmalar ve dosyalar VERIFICATION_0_20.json dosyasında kaydedilir. AUDIO_REGRESSION_0_20.json eski doğrudan giriş sonucudur; AUDIO_REGRESSION_GATE_0_20.json yeni VAD + başlangıç tamponu sonucudur. Testler sabit revizyonlu LibriSpeech kökenli iki resmi kayıt, aynı kaydın azaltılmış seviyesi, tekrarı, sessizlik ve sabit tohumlu beyaz gürültü içerir. İki farklı ses parça boyuyla 12 kontrol yapıldı. Bu bir küçük regresyon kümesidir; genel başarı yüzdesi değildir. Ölçüm Linux CPU ortamındadır; Android mikrofon testi değildir. Türkçe ML Kit çıktısı fiziksel cihaz/emülatörde yerel olarak doğrulanmadı.

Native VAD lisansı MIT; metni APK varlıklarına da eklendi. Kaynak: https://github.com/snakers4/silero-vad . Önceki Apache/ONNX lisans metinleri licenses/ klasöründe korunur. Release AAB mağaza yayını için otomatik hazır sayılmaz; Play imzalama ve gerçek satın alma doğrulaması bu incelemenin kapsamı değildir.


Tekrarlanabilir tanıma kontrolü: Python ortamına `sherpa-onnx==1.13.8` ve `numpy` kurulduktan sonra `python3 scripts/run_asr_regression.py --model-dir MODEL_KLASORU` çalıştırılır. MODEL_KLASORU sabit İngilizce model dosyaları ve aynı resmi revizyondan test_wavs/0.wav, 1.wav ve trans.txt içermelidir. Model SHA-256 değerleri uygulama manifestiyle eşleştirilir. Sesler bu kaynak ZIP'e dahil edilmez. Resmi test kaynağı: https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-26/tree/672fbf1b30579d6585301139bb363f42a0ad4a24/test_wavs

Son çalıştırma: Debug APK ve release AAB üretimi, debug/release lint ve 100 Android birim testi burada başarıyla tamamlandı. 33 Python testi ve 12 sesli kontrol geçti. İmzalı ARM64 test APK'si, doğrulanmış evrensel APK'den yalnızca diğer işlemci kütüphaneleri çıkarılarak üretildi; DEX/kaynaklar/varlıklar aynı kaldı. Yeniden hizalanıp aynı sertifikayla imzalandı; imza ve 16 KB kontrolleri geçti. ARM64 olmayan cihazlar için kaynak projenin GitHub derlemesi kullanılmalıdır. APK üretmek, gerçek cihazda mikrofon/TTS/çeviri kalitesini doğrulamakla aynı şey değildir.
