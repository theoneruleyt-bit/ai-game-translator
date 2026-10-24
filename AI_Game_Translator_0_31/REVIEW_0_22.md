# 0.22 — Daha erken yazılı çeviri ve kalıcı kayıtlar

Sürüm kodu 33. 0.21 üzerine uygulanmıştır.

## Dinleme gecikmesi
- Değişen konuşma için yazılı önizlemenin azami birikme penceresi 900 ms'den 450 ms'ye, sabit kalan metnin bekleme penceresi 250 ms'den 180 ms'ye indirildi. Yeni istekler arasında en az 350 ms bırakılır. Bunlar yazılım zamanlama pencereleridir; uçtan uca gecikme ölçümü değildir.
- Önceki kesin çeviri sürerken yeni önizlemeyi durduran uygulama koşulu kaldırıldı. ML Kit önizleme istemcisi ayrıdır ve aynı anda en fazla bir fiziksel önizleme isteği çalışır. Kesin çeviri kapasitesi ayrıca korunur. Geciken eski önizleme başka konuşmaya yazılamaz; mevcut oturum/konuşma/sürüm kontrolleri devam eder.
- Çeviri modelleri hazırlanırken önizleme istemcileri de hazırlanır. Model hazır olmadan dinleme başlamaz.
- Ücretsiz streaming ASR için normal bitiş sessizliği Hızlı 0,55 sn, Dengeli 0,70 sn, Tam cümle 1,05 sn oldu. Önceki değerler 0,85 / 1,05 / 1,25 sn idi. Uzun konuşma için duraklama gerekliliği ve 18/20/25 sn eşikleri değişmedi.
- Seslendirme kesinleşen metni bekler. Değiştirilebilir önizlemeler seslendirilmez ve geçmişe yazılmaz. Sırf kelime sayısı arttı diye konuşma bölünmez. Kısa tanınmış parçaların devamla birleşmesi için 650 ms güvenlik penceresi korunur.

## En alttaki çeviri kayıtları
- Kayıt kartı ana sayfanın en altına taşındı. En yeni 6 kayıt; tarih/saat, konuşmacı, kaynak metin ve çeviriyle gösterilir.
- Tüm kayıtları görüntüle seçeneği son 100 kesin çeviriyi kaydırılabilir ve seçilebilir olarak açar. Paylaşma ve temizleme düğmeleri korunur.
- Kayıtlar cihazdaki uygulama tercihlerine yazılır; uygulama kapatılıp açılsa da korunur. Temizleme kayıtları kalıcı olarak siler. Önceki activity durumunda bulunan kayıtlar, yeni kalıcı kayıt alanı yoksa taşınır; iki kez eklenmez. En fazla 100 kayıt, kayıt başına en fazla 3000 karakter tutulur. Geçersiz eski JSON verisi açılışı durdurmaz.
- Kamera, fotoğraf ve yazılı çeviri sonuçları da bu aynı kayıt alanına girer. Kamera OCR/filtreleme ve ses kuyruğu kuralları değiştirilmedi.

## Telefon denemesi
1. Aynı imzalı 0.21 test sürümünün üzerine APK'yı yükleyin; uygulamayı silmeyin.
2. Ücretsiz Dinle → Dengeli, kaynak İngilizce/hedef Türkçe, otomatik okuma açık kullanın. Önizleme konuşma sürerken görünmeli; kesin çeviri daha sonra düzeltilebilir. Ses kesinleşen cümleyi izler.
3. İki ayrı cümleyi kısa arayla, ardından 10 sn sessizlikle deneyin. Önceki cümle çevrilirken yeni yazının donmadığını, kesin kayıtların çoğalmadığını kontrol edin. Kulaklıklı ve hoparlörlü denemeyi ayrı yapın.
4. En alta kaydırın: son 6 kayıt, Tüm kayıtları görüntüle ve paylaşma seçeneklerini kontrol edin. Uygulamayı kapatıp yeniden açın; kayıtlar kalmalı. Temizledikten sonra tekrar açın; boş kalmalı.
5. Kamera modunda aynı altyazının sabit kalması ve değişmesi senaryosunu tekrar deneyin.

## Doğrulama ve sınırlar
Çalıştırılan kontroller VERIFICATION_0_22.json içine kaydedilir. Önceki sürüm raporları tarihsel kayıttır. Bu sürümde gerçek telefon, Türkçe ML Kit doğruluğu, akustik yankı ve cihaz üzerindeki uçtan uca gecikme ölçülmedi. Daha kısa bekleme süreleri, her cihazda belirli bir saniye değerine veya sıfır gecikmeye karşılık gelmez. Uzun cümlelerin kesin seslendirmesi konuşma bitişini bekler. Yavaş cihaz/model yüklemesi ve kaynak gürültüsü gecikmeye veya hataya neden olabilir.

0.21 yankı azaltma ve canlı ses eşitleme davranışı korunur; fiziksel cihaz desteğine bağlı sınırlar REVIEW_0_21.md içinde açıklanmıştır. Ücretsiz mod sunucu/anahtar gerektirmez. Deneme hakları ve fiyatlar değiştirilmedi. Bu, aynı test anahtarıyla imzalı ARM64 test APK'sıdır; mağaza yayını değildir.
