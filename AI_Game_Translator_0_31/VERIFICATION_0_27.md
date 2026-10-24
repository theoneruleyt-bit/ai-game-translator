# 0.27 doğrulama

- Temiz debug APK + release AAB derlemesi: geçti.
- 172 Kotlin/JUnit testi: geçti (0 hata).
- 28 sunucu politikası ve 5 native denetim Python testi: geçti.
- Debug/release lint: 0 hata, 66 uyarı. Uyarılar bastırılmadı.
- Sherpa AAR, Silero VAD, noktalama ve yerel çeviri dosya sağlama toplamları: geçti.
- Yeni motor kataloğundaki 8 dosya kaydı gerçek indirilen dosyaların SHA-256/boyutlarıyla eşleşiyor.
- ARM64 test APK: önceki test sertifikası, zipalign 16 KB, evrensel APK'dan korunan tüm payload dosyaları birebir eşleşiyor.
- ELF 16 KB kontrolü: evrensel APK 36, ARM64 APK 8, AAB 36 kütüphane; geçti.
- Bundletool 1.18.2 yapısal AAB denetimi: geçti. AAB imzasız denetim çıktısıdır, mağaza yüklemesi değildir.
- 12 koşul × 2 yerel motorla gerçek native ASR karşılaştırması; ayrıca eski modelde 18 regresyon koşulu tamamlandı. Ham çıktılar verification altında.

Yeni 8 birim testi: model seçimi/önbellek koruma/pinlenmiş metadata; taze decoder ile eski callback ayrımı; değişen kelime kuyruğu; metin geri çekilmesi; noktalama/büyük harf eşdeğerliği ve tur sıfırlama; erken konuşma onayı; kısa yanıtın finalde gönderilmesi. İlk model yükleme ve mikrofon/TTS davranışı Android cihazda test edilmedi.

Ölçümler ASR içindir. Türkçe çeviri doğruluğu, GTA/oyun sokak dili, gerçek telefon gecikmesi ve hoparlör yankısı için kalite garantisi vermez. Gelişmiş modelin kısık kitap kayıtlarında daha kötü olduğu raporda korunmuştur. Cihazda iki motorla aynı kayıt denenmelidir. Google Play genel yayını için önceki özel yapılandırma ve cihaz/satın alma testleri hâlâ gereklidir.
