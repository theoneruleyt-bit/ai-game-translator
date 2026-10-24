# 0.31 düzeltme paketi

- Kısa İngilizce parçalar önceki konuşma bağlamından bağımsız çevrilir. Aşırı genişleyen kısa çıktı reddedilir; bu anlam doğruluğu garantisi değildir.
- And/but/or/because tek başına kaldığında ücretsiz sürekli Dinle akışında sonraki onaylı parçayla birleşebilir. Devam gelmezse taslak temizlenir; kayıt ve ses oluşmaz. Gerçek tam replik tekrarları korunur.
- Gelişmiş modelin bağlamlı isteminde temel çeviri talimatları da korunur.
- Dinle düğmesi bütün motorlarda Dinlemeyi durdur / Stop listening olarak düzeltildi.
- Kaydırmada sistem çubuklarıyla çakışma için içerik kırpma ve pencere boşlukları düzeltildi.
- Kamera çeviri motoru ve deneme/satın alma sınırları korunur. Eski geçmiş otomatik silinmez.

## Kontroller
33 Python testi geçti. Kritik kaynak/bağlaç kontrolleri gerçek Kotlin 2.2.21 ile derlenip çalıştırıldı. Tam Android assemble, JUnit ve lint bu ortamda çalıştırılmadı; APK üretilmedi. Önceki yarım oturumun sonuçları başarı olarak kullanılmadı.

## GitHub
Bu klasörün içeriğini mevcut deponun köküne yükleyin; app ve .github kökte olmalı.
Commit mesajı: fix: guard listen fragments and correct controls and insets
Actions: Android final checks and draft download.
Başarılı derlemenin CI indirme paketi Releases altındaki taslakta bulunur; Actions artifact kotasına bağlı değildir.

Telefonda tek And + sessizlik, And + devam, iki kez gerçek Watch out!, Dinle düğmesi ve baştan sona kaydırmayı test edin. Eski hatalı kayıtlar korunmuştur; yeni kayıtlarla değerlendirin. Mağaza yüklemesine geçmeden bu kontroller tamamlanmalıdır.
