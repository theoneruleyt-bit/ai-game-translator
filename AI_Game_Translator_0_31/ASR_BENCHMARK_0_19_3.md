# İki örnek üzerinde tanıma regresyon kontrolü

Model: csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-26. Sabit revizyon: 672fbf1b30579d6585301139bb363f42a0ad4a24. Kaynak test_wavs/0.wav ve 1.wav, referans test_wavs/trans.txt. Python Sherpa-ONNX 1.13.8 ile 16 kHz PCM ses 50 ms parçalar halinde beslendi; iki CPU iş parçacığı. Dosya sonuna 2 saniye sessizlik eklendi. Ölçüm sunucu/x86 ortamında yapıldı, telefon mikrofonu kullanılmadı.

Eski hızlı akış: greedy_search, 0.45 saniye sessizlik ve sessizlik aramayan 6 saniye zorunlu bölüm sonu. Yeni karşılaştırma: modified_beam_search, 4 yol, 1.05 saniye sessizlik ve bu 20 saniyeden kısa örneklerde zorunlu süre kesmesi yok. Android'in uzun bölüm kuralı ayrıca sessizlik ister; bu iki kısa örnekte devreye girmez.

| Ses | Ayar | Ses süresi (s) | Bölüm sayısı | Kelime düzenleme hatası / referans kelime | Çözümleme süresi (s) |
|---|---|---:|---:|---:|---:|
| 0.wav | old_fast | 6.625 | 2 | 2/18 | 0.562 |
| 1.wav | old_fast | 16.715 | 3 | 0/48 | 1.480 |
| 0.wav | new_balanced | 6.625 | 1 | 0/18 | 0.638 |
| 1.wav | new_balanced | 16.715 | 1 | 0/48 | 1.500 |

0.wav örneğinde eski kural bir sözcüğü iki bölümde iki ayrı parçaya ayırdı. Yeni sonuçta bu sözcük bütün kaldı. 1.wav eski ayarda üç, yeni ayarda tek bölüm oldu. Referansla karşılaştırma büyük/küçük harf ve noktalama normalleştirilerek standart Levenshtein kelime uzaklığıyla yapıldı. Sıfır hata bu iki hazır örneğe aittir; genel doğruluk, argo, oyun müziği, Türkçe çeviri ve mikrofon kalitesi için başarı oranı değildir. Decode süreleri mobil gecikme değildir. Gerçek cümle bitimini beklemek daha fazla seslendirme beklemesi yaratabilir; kaynak metin ve yazılı önizleme akışı devam eder.

Model/test kaynağı: https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-2023-06-26/tree/672fbf1b30579d6585301139bb363f42a0ad4a24/test_wavs
