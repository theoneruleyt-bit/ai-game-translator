# 0.21 — Canlı dinleme ve ses eşitleme

Sürüm kodu 32. Bu güncelleme 0.20 kaynakları üzerine uygulanmıştır.

## Değişiklikler
- Ücretsiz canlı dinleme ve bulut dinlemede kulaklık zorunluluğu kaldırıldı. Bağlantı bilgisi otomatik gösterilir; hoparlör veya kulaklık yönlendirmesi zorlanmaz.
- Üç doğrudan mikrofon motorunda cihaz destekliyorsa AudioRecord oturumuna AcousticEchoCanceler eklenir. Yerel TTS motorunun PCM geri çağrıları ayrıca 16 kHz referansa dönüştürülür. Mikrofon verisinde güçlü korelasyonla eşleşen referans ölçeklenerek çıkarılır; eşleşmeyen ses geçirilir. Mikrofonun tamamı susturulmaz.
- Ücretsiz streaming motorunun kesin sonuçlarına oturum ve konuşma kimliği eklendi. Aynı kesin olay yeniden gelirse işlenmez. Farklı konuşmalarda gerçekten tekrarlanan kelimeler engellenmez.
- Yeni Canlı ses eşitleme ayarı varsayılan açıktır. Bekleyen ses yükü 5 saniyeyi geçip birden fazla başlamamış cümle birikirse en yenisi tutulur. Başlanmış cümlenin kalan parçaları korunur. Başlamamış 6 saniyeden eski okumalar veya kaynak 2,5 saniyedir sessizken 2 saniyeden eski okumalar atlanır. Atlanan ses sayısı gösterilir; yazılı çeviri ve geçmiş korunur. Bu ayarı kapatmak bu atlama kurallarını devre dışı bırakır, ancak yeniden gecikme birikebilir.
- Canlı eşitleme ile otomatik okuma hızı metin uzunluğu, kaynak süre ve kuyruk yaşına göre kademeli olarak 2× sınırına kadar uyarlanır. Elle hız ayarı ve kamera modundaki 1,5× sınırı korunur. Her motorun gerçek okuma süresi farklıdır.
- Kamera OCR, altyazı bölgesi ve sabit öğe filtreleri değiştirilmedi. Paylaşılan ses oynatıcıdaki canlı kuyruk kuralları yalnızca etkin Dinle modunda uygulanır.

## Telefonda deneme
1. Önceki test APK'sının üzerine yeni APK'yı yükleyin. Aynı test imzası korunmuştur; uygulamayı silmek gerekmez. Farklı imzalı bir sürüm yüklüyse Android güncellemeyi reddedebilir.
2. Ücretsiz Dinle → Dengeli → kaynak İngilizce, hedef Türkçe → Türkçe sesi dene. Otomatik okuma ve Canlı ses eşitleme açık olsun.
3. TV/oyun sesiyle önce kulaklıkla, sonra telefon hoparlörüyle orta ses düzeyinde deneyin. Bir cümle, kısa duraklama, ikinci cümle ve en az 10 saniye sessizlik kullanın. Yazı, okuma ve atlanan ses sayısını karşılaştırın.
4. Gerçekten iki kez söylenen cümlenin iki kez çevrildiğini; tek söylenen cümlenin seslendirmeden dolayı sürekli geri dönmediğini kontrol edin. Kulaklığı çıkarıp yeniden bağlamayı da deneyin.
5. Kamera modunda aynı altyazının sabit kalması ve değişmesi senaryosunu tekrar deneyin.

## Doğrulama ve sınırlar
Güncel çalıştırılan kontroller VERIFICATION_0_21.json içinde kaydedilir. 0.20 adlı raporlar geçmiş sürüme aittir; bu sürümde yeniden çalıştırılmış sayılmaz.

Gerçek telefon, hoparlör-mikrofon akustiği, Bluetooth geçişi ve cihaz içi Türkçe çeviri doğruluğu bu ortamda sınanamadı. Referans azaltma sentetik PCM ile test edilir; oda yankısı, bozulma, başka konuşmacı ve cihaz işleme farklılıklarında tam yankı giderme garantisi vermez. Bazı TTS motorları PCM geri çağrısı sağlamaz. İsteğe bağlı bulut MP3 seslendirmesi PCM referansını sağlamaz; bu yolda donanım yankı giderme desteği önemlidir. Eski Android SpeechRecognizer seçeneğinin yarı çift yönlü çalışma sınırı korunur; yeni davranışın ana yolu ücretsiz doğrudan mikrofon motorudur.

Tanıma ve ML Kit çeviri modelleri bu sürümde değiştirilmedi. Tam doğruluk, her oyunda argo bağlamını çözme veya sıfır gecikme garanti edilemez. Sesli çıkış kaynaktan yavaşsa hem her cümleyi seslendirmek hem de sürekli canlı kalmak mümkün değildir; yeni ayar bu durumda yazıyı koruyup eski sesli okumayı atlamayı seçer. Kaynak konuşma bitince başlamış cümlenin tamamlanması normaldir.

Ücretsiz mod sunucu/anahtar gerektirmez. Deneme sınırları kamera 3 dk, dinle 3 dk, görsel 10 adet; planlar aylık 4,99 USD, yıllık 49,99 USD, ömür boyu 199 USD olarak değişmeden korunur. Bu APK test anahtarıyla imzalıdır; mağaza yayın paketi değildir.
