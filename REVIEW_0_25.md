# AI Game Translator 0.25 — Dinle düzeltmeleri

Sürüm kodu 36. Kamera OCR ve okuma politikası, kayıtlar, Pro fiyatları ve deneme hakları korunur. Aynı test imzası kullanılır.

## Yapılan değişiklikler

- VAD konuşma tespiti olmadan ASR metni kabul edilmez. Dijital sessizlik / birkaç PCM kuantalama adımı konuşma başlangıcı sayılmaz; düşük sesli konuşmayı kesen yüksek bir ses eşiği yoktur.
- Son VAD konuşmasından sonra 700 ms boyunca geciken son kelime düzeltmelerine izin verilir. Ardından metin dondurulur. Motor endpoint üretmese bile 1100 ms VAD sessizliğinde bağlam sıfırlanır. Kesintisiz gerçek konuşmada süre sınırı yoktur. Bu süreler çeviriyi başlatmadan önce bekleme süresi değildir; akıllı bölümleme devam eder.
- Daha önce okunmuş metin ASR tarafından yeniden yazıldığında, belirsiz hizalama okuma imlecini geriye götüremez. Benzersiz üç sözcüklü konuşulmuş son bölüm veya mevcut sağ bağlam varsa yeni devam korunur. Güvenilir hizalama olmayan kısalmış yeniden yazımda tekrar okumamak için belirsiz bölüm tutulur; bunun bazı yeniden yazımlarda kelime atlama riski vardır.
- Dinle modunda apostrofsuz olumsuzluklar (`dont`, `cant`) ve genişletilmiş biçimler bütün ifade düzeyinde eşleştirilir. `tryna`, `hafta`, `oughta`, `c'mon` normalleştirilir. `ain't` gibi bağlama göre değişen biçimler körlemesine değiştirilmez. Belirsiz sözlük eşleşmesi rastgele seçilmez. Düzenli ifadeler ve sözlük takma adları önbelleğe alınır.
- Kamera sözlük eşleştirmesi ve normalleştirmesi değişmez. 517 ifade kaynaklı paket ve isteğe bağlı OPUS-MT motoru korunur; bu sürüm yeni eğitilmiş bir tanıma/çeviri modeli değildir.

## Doğrulama ve sınırlar

Güncel çalıştırma sonuçları VERIFICATION_0_25.json dosyasındadır. Önceki sürüm raporları tarihsel kayıttır.

Ham ses regresyonu iki sabit İngilizce kaydın normal/kısık/tekrarlı biçimleri, uzun sessizlikten sonra yeniden konuşma durumları ile sentetik sessizlik, beyaz gürültü ve darbeli gürültüyü, 50/120 ms karelerle ve her kayıttan sonra 10 sn sessizlikle işler. Bu, cihaz üzerinde GTA/argo doğruluk ölçümü değildir. Android telefon, Bluetooth, oda yankısı ve TV oyun sesiyle uçtan uca ölçüm yapılmamıştır. Müzik/efekt VAD'yi yanıltabilir; bu önlemler her gürültüde uydurma kelime olmayacağını garanti etmez. Donanım AEC/yankı temizlemesi önceki sürümdeki gibidir.

## Kurulum

ARM64 test APK'sını mevcut 0.24 kurulumunun üzerine yükleyin. Uygulamayı silmek gerekmez. Ücretsiz Dinle → Dengeli, doğal konuşma paketi ve akıllı bölümleme açık olarak deneyin. İlk karşılaştırmayı sesli okuma kapalı yapın; sonra Türkçe okumayı açıp son replikten sonra 10 saniye sessizlik bırakın. İngilizce kaynak metin yanlışsa sorun tanımadadır; İngilizce doğru ama Türkçe yanlışsa çeviri motorundadır. Kayıtlar bölümünde ikisini birlikte görebilirsiniz.

Kaynak ZIP içeriğini mevcut GitHub depo köküne kopyalayın, ardından:

```bash
git add .
git commit -m "Fix silent speech turns and replayed dialogue; normalize colloquial English"
git push
```
