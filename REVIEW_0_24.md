# 0.24 — Konuşma/deyim paketi ve alternatif yerel model

Sürüm kodu 35. 0.23 üzerine uygulanmıştır.

## Kullanıcının göreceği değişiklik
Dinle ekranındaki **Doğal konuşma ve argo paketi** varsayılan açık gelir. Kaynakta 517 ayrı tam ifade/varyant vardır; bunlar 517 bağımsız dilbilgisi kuralı veya bütün İngilizce değildir. Önceki 219 girdilik kaynak kaynak dosyası korunur; v2 mikrofon paketi yeni ve daha geniş bir kaynak dosyasındadır. Önceki sürümde mikrofon için phrase_overrides varsayılan kapalı olduğundan eski sözlük, Argo desteği açık olsa bile kesin mikrofon çevirisine uygulanmıyordu. Yeni paket ayrı conversation_pack_v2 tercihiyle açık başlar. Üstteki genel konuşma/argo desteği tercihi yine uygulanır.

Örnekler:

| İngilizce | Paketteki doğal Türkçe |
| --- | --- |
| You are pulling my leg. | Benimle dalga geçiyorsun. |
| This place is crawling with cops. | Burası polis kaynıyor. |
| We need to lay low until the heat dies down. | Ortalık yatışana kadar göze batmamamız gerekiyor. |
| I ain't got no money. | Hiç param yok. |
| I'm not buying that bullshit. | O zırvayı yutmam. |

Argo ve küfür içeren bazı kalıplar da kayıtlıdır. Her ifade yalnızca tamamıyla eşleştiğinde kullanılır. Birden fazla cümle varsa hepsi bilinen kalıplara karşılık gelirse birlikte eşleştirilir; bir cümle bilinmiyorsa bütün metin sinirsel motora bırakılır. Olumsuzluk veya başka bir bağlam içeren cümleler pozitif kalıba zorlanmaz. Soru işaretli bir ifade de kayıtlı bildirme cümlesine zorlanmaz; bilinmiyorsa sinirsel motora bırakılır. Genel heat, sick veya I'm down gibi tek başına belirsiz ifadeler yeni zorunlu eşleştirmelere dahil edilmez. Tam kalıplar da her bağlamı bilemez: gerektiğinde bu paket kapatılabilir.

## Gerçek alternatif sinirsel model
**Alternatif OPUS-MT motorunu dene** seçeneği, ücretsiz İngilizce Dinle için APK içindeki Helsinki-NLP/opus-mt_tiny_eng-tur LiteRT modelini açar. Bu gerçek bir önceden eğitilmiş İngilizce → Türkçe çeviri modelidir. Yeni bir argo modelini kendimiz eğitmiş değiliz. Model yaklaşık 20,1 MB; türetilmiş tokenizer tablosu yaklaşık 1,29 MB'dir. Kaynak, lisans, sabit revizyon ve SHA-256 değerleri licenses/OPUS_MT_LOCAL.md içinde verilir.

- Hızlı ML Kit motoru varsayılan kalır; OPUS-MT daha iyi olduğu varsayılmadan açıkça seçilebilir. Model, sırf daha büyük bir paket diye tüm ifadelerde daha kaliteli kabul edilmez.
- 18 yazılı örnekte küçük ve daha büyük bir aday karşılaştırıldı. Her ikisi de bazı deyimleri kelimesi kelimesine çevirdi; büyük adayın bir örnekte kaçınma anlamını tersine çevirdiği de gözlendi. Büyük aday APK'ya eklenmedi. Tüm ham çıktılar verification/translation-observations-0_24.json içindedir; bunlar otomatik bir doğruluk puanı değildir.
- Doğal konuşma paketindeki eşleşmeler seçili sinirsel motordan önce uygulanır. Eşleşme yoksa seçilen motor kullanılır. İngilizce önizlemeler hızlı ML Kit ile devam eder; OPUS-MT seçiliyken kesin çeviri yanlışlıkla önizleme önbelleğinden alınmaz. Böylece gerçekten seçili yerel motora gider.
- Ekranda son çevirinin tam ifade paketinden, OPUS-MT'den veya hızlı yedek motordan geldiği gösterilir.
- Model ve tokenizer APK içindedir; ek indirme, API anahtarı veya sunucu gerektirmez. Bu özellik bir bulut kullanım ücreti oluşturmaz. ML Kit ve tanıma modellerinin önceki ilk hazırlık indirmeleri aynı kalır.

## Sınır ve düşüş davranışı
Yerel modelin bu uygulamadaki dar tokenizer adaptörü ASCII İngilizce girişini destekler. 684 gerçek SentencePiece kodlamasıyla birebir eşleştirilerek test edilmiştir. Desteklenmeyen Unicode, bilinmeyen token, 256 tokeni aşan giriş, boş çıktı, bellek/çalışma zamanı hatası ve bitiş tokeni olmaması halinde ML Kit'e dönülür. Türkçe çıktı karakterleri korunur. Girdi sessizce kesilmez; bitmemiş hedef cümle başarılı sonuç gibi sunulmaz.

Yerel çıkarım aynı anda bir fiziksel istek çalıştırır. İlk model yükleme sonrasında native adımlar arasında 2,5 saniyelik işbirlikçi süre kontrolü yapılır; tek bir native çağrının bu süreyi aşamayacağı garanti edilmez. Yavaş telefonda OPUS-MT seçeneği gecikmeyi artırabilir. Model seçeneği veya paket tercihi değiştirildiğinde mevcut dinleme durur; tekrar Başlat seçilir. OPUS-MT bir karşılaştırma alternatifidir, ML Kit'ten her durumda üstün bir motor olduğu iddiası değildir.

## Korunanlar
0.23 akıllı konuşma bölümlendirme, mikrofon yankı azaltma, otomatik okuma hızı ve 0.22 kayıtlar korunur. Kamera çevirisi eski motor ve eski sözlük akışını kullanır; OCR, altyazı filtreleri, VoicePlayer ve EchoReference kaynakları değiştirilmez. Deneme hakları ve fiyatlar değiştirilmedi. Aynı test anahtarı ve artan sürüm kodu sayesinde önceki test APK'sı üzerine kurulabilir.

## Telefonda karşılaştırma
1. Önceki APK üzerine kurun; uygulamayı silmeyin. Ücretsiz İngilizce Dinle, Türkçe hedef, Argo desteği ve Doğal konuşma ve argo paketi açık olsun.
2. Aynı cümleyi önce yalnızca hızlı motorla, sonra Alternatif OPUS-MT açıkken deneyin. Son motor etiketi hangi yolun kullanıldığını belirtir. Bilinen kalıplar her iki seçimde de paket karşılığını kullanır.
3. İngilizce kaynak yazıyı da kontrol edin. Kaynak ASR yanlış duyduysa yeni çeviri modeli doğru kelimeyi geri getirmeyi garanti etmez. Örneği Metin yaz alanında da denemek tanıma hatasıyla çeviri hatasını ayırır.
4. Deyim, olumsuz cümle, özel isim ve sayıyı ayrı ayrı karşılaştırın. Örneğin You are pulling my leg / You are not pulling my leg / He did not kill the guard, he only took his keys.
5. OPUS-MT gecikme veya daha kötü anlam verirse kapatın; konuşma paketi açık kalabilir. Kamera ve kayıtların önceki davranışını da kontrol edin.

## Gerçek doğrulama kapsamı
Güncel sonuçlar VERIFICATION_0_24.json içindedir. Android birim testleri, 684 native tokenizer eşleşmesi, Türkçe Unicode/olumsuzluk detokenizasyonu ve paketin olumsuz veya bilinmeyen bağlamı zorla değiştirmemesi test edilir. LiteRT modeli Linux CPU üzerinde gerçek çıkarımla 18 yazılı İngilizce örnekte çalıştırıldı. Android derlemesi/lint ve native ELF/ZIP hizalaması ayrıca kontrol edilir.

Fiziksel Android cihaz, gerçek mikrofon, Android LiteRT uçtan uca hızı ve ML Kit ile karşılaştırmalı çeviri doğruluğu bu ortamda test edilmedi. Tüm İngilizce argoyu, bağlamı veya özel isimleri eksiksiz kapsama; Google Translate ile aynı kalite; kusursuz ya da sıfır gecikmeli çeviri garantisi verilmez. Kaynak raporlarında bu sınırlar açıkça tutulur. Önceki sürümlerin raporları tarihsel kayıttır.

Bu ARM64 APK test paketidir; Google Play yayın paketi değildir.
