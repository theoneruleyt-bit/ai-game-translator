# 0.23 — Uzun konuşma sürerken anlam gruplarını çevirme

Sürüm kodu 34. 0.22 üzerine uygulanmıştır.

## Değişen davranış
Önceki sürüm seslendirmek için ASR motorunun kesin konuşma sonunu bekliyordu. Artık ücretsiz İngilizce streaming dinlemede **Akıllı konuşma bölümlendirme** varsayılan açıktır. Konuşmacının değişmesi veya tüm uzun konuşmanın bitmesi gerekmeden, kararlı cümle/anlam grupları çeviriye ve seslendirme kuyruğuna gönderilebilir.

- APK içinde 7.490.500 baytlık int8 CNN-BiLSTM İngilizce noktalama modeli ve 149.430 baytlık BPE sözlüğü bulunur. Sunucu, anahtar veya yeni bir ücretli servis kullanılmaz. Modelin SHA-256 değerleri hem CI'da hem native yüklemeden önce doğrulanır. Model ve lisans kökeni licenses/PUNCTUATION_MODEL.md içindedir.
- Model nokta/soru/ünlem veya uygun virgül/anlam grubu tahminini yapar. Devam eden konuşmada en az 2 kelimelik devam bağlamı ve aynı sınırın en az 280 ms kararlı kalması aranır. Model analizi en çok 300 ms aralıklarla yenilenir; eklenen yeni ASR kelimeleri, önceki kararlı iç sınırı otomatik olarak geçersiz kılmaz.
- Kısa duraklamada, VAD sessizlik bildirdikten sonra en az 120 ms ve metnin en az 300 ms kararlı olması aranır. Eksik bağlaç/edat sonları ve bazı bağımlı başlangıçlar erken okumayı engeller. Noktalama bulunamazsa tamamlanmış yüklem/anlam grubu için ihtiyatlı İngilizce kuralları kullanılır. VAD'nin kendi gecikmesi bu değerlerin dışındadır; bunlar toplam uçtan uca gecikme değildir.
- Tanıma motoru her çeviri bölümünde sıfırlanmaz. Tam konuşma bağlamı tutulur; ayrı bir imleç daha önce gönderilmiş kelimeleri izler. Native motor en sonunda bütün metni verince yalnızca henüz çevrilmemiş devam işlenir. Yeni konuşmadaki gerçek tekrarlar korunur.
- ASR daha önce okunmuş başlangıcı düzeltirse henüz okunmamış sağ bağlamla hizalama yapılır. Belirsiz hizalamada yeni erken kesimler durdurulup native kesin sonuç beklenir; daha önce söylenmiş sesi geri almak mümkün değildir. Noktalama çıktısı kaynak kelimeleri değiştirirse bu çıktı kullanılmaz.
- Yalnızca analiz penceresi 96 kelimeyle sınırlandırılır; kelime sayısı veya süre tek başına zorla konuşma kesmez. Uygun sınır tahmin edilemiyorsa normal ASR bitişi beklenebilir.
- Model yüklenemezse dinleme durmaz: durum etiketi uyarır ve duraklama/kararlı metin kuralları kullanılır. Ayar kapatılırsa eski native konuşma sonunu bekleyen akışa dönülür. Ayarı değiştirmek mevcut dinlemeyi durdurur; yeniden Başlat seçilir.

## Korunanlar
Kamera OCR ve altyazı filtreleri, yankı azaltma, mevcut sesler ve otomatik hız, 0.21 canlı ses eşitleme politikası ile 0.22 kalıcı kayıtlar korunur. Canlı ses eşitleme açıkken çok eski başlamamış okumalar atlanabilir; yazılı kayıtlar korunur. Deneme hakları kamera 3 dk / dinle 3 dk / görsel 10 adet ve fiyatlar aylık 4,99 USD / yıllık 49,99 USD / ömür boyu 199 USD olarak değişmeden kalır. Kamera işleme ve paylaşılan ses oynatıcı kaynaklarının 0.22 ile aynı olduğu ayrıca doğrulanır.

## Telefonda deneme
1. Aynı imzalı 0.22 APK üzerine kurun. Uygulamayı silmeyin.
2. Ücretsiz İngilizce Dinle → Dengeli, Türkçe hedef, Otomatik okuma ve Akıllı konuşma bölümlendirme açık. Model durumu **Cihaz içi noktalama modeli hazır** olmalı. Türkçe sesi dene ile TTS'yi kontrol edin.
3. Aynı kişinin 20–30 saniyelik İngilizce anlatımını kullanın: birbiri ardına birkaç cümle ve kısa doğal duraklamalar olsun. Konuşma hâlâ sürerken tamamlanan gruplar yazıya/kayıtlara ve okuma kuyruğuna girmeli.
4. Sonunda 10 saniye sessizlik bırakın: tanıma motoru kesin sonucu gönderdiğinde önceden okunan başlangıç tekrar eklenmemeli. Gerçekten iki kez söylenen bir replik ise iki kez çevrilebilmeli.
5. Kulaklıkla ve telefon hoparlörüyle ayrı deneyin. Mikrofon seviyesi bozulma uyarısı vermesin. Kamera modunu da önceki gibi kontrol edin.

## Doğrulama ve sınırlar
Güncel kontrol sonuçları VERIFICATION_0_23.json içinde bulunur. Linux Sherpa 1.13.8 ile aynı gerçek modelden 1647 yerel noktalama tahmini üretildi. İki kesintisiz İngilizce metnin bu çıktıları Kotlin bölümleme testlerinde yeniden oynatılır: native bitişten önce bölüm çıkması ve en sondaki tam metinden sonra kelime tekrarı/kaybı olmaması doğrulanır. Bu, gerçek mikrofon veya Türkçe çeviri doğruluk testi değildir. Kaynak model olmadan yalnızca elle yazılmış örneklerle sınırlı kalınmaz; tekrar üretim betiği scripts/generate_clause_fixtures.py içindedir.

Gerçek Android cihazda mikrofon, model yükleme, yankı, PS5/Xbox sesi, Türkçe ML Kit doğruluğu ve uçtan uca gecikme bu ortamda ölçülmedi. Noktalama ve anlam grubu sınırı bir tahmindir; tam dilbilgisel çözümleyici veya kusursuz simultane tercüman değildir. Yanlış sınır tahmini, ASR düzeltmesi, özel isim, argo, gürültü ve cümleler arası bağlama bağımlı ifadeler hataya neden olabilir. İngilizce ve Türkçe sözdizimi farklı olduğundan bir miktar devam bağlamını beklemek gerekir. Her konuşmada sıfır gecikme veya tam anlam eşleşmesi garanti edilmez. Eski/bulut/Türkçe dinleme motorlarına bu İngilizce model eklenmedi; özellik ücretsiz İngilizce ana akışına yöneliktir.

Bu ARM64 APK önceki test anahtarıyla imzalıdır; Google Play yayın paketi değildir. Önceki sürüm raporları tarihsel kayıt olarak tutulur.
