# 0.28 doğrulama

179 Kotlin/JUnit testi ve 33 Python testi geçti. Debug/release APK/AAB derlemesi geçti. Lint: debug 0 hata/67 uyarı; release 0 hata/66 uyarı. Mevcut uyarılar bastırılmadı.

Somut hata yeniden üretimi: 1647 büyük harfli native noktalama girdisinin hiçbirine eski giriş biçiminde noktalama eklenmedi. Aynı cümlelerin normalleştirilmiş gerçek native tahminleri, Türkçe cihaz dili (Locale.ROOT), yeni/taze ASR kararlılığı, anlam grubu imleci ve StreamingTurns ile Kotlin testlerinde oynatıldı; ara teslim gerçekleşti ve endpointte bütün kaynak kelimeler korundu. Bu test yazılı akış/noktalama regresyonudur, Türkçe çeviri kalitesi testi değildir. 5 ek önce/sonra native örneğinde üç transkript 0.27'de gerçek ASR ile çıkarılmıştı. JNI AddPunctuationWithCase girişi Python API ile eşleştirildi.

Diğer yeni regresyonlar: sözcük yeniden yazımı sırasında tamamlanan taslağın gösterilmesi; yeni endpoint/sonuç üstüne eski taslak yazılmaması; onaylı kısa parçanın sonsuza dek beklememesi; tanılamanın varsayılan kapalı ve sınırlı olması, kapatılınca silinmesi.

İmza/zipalign/retained payload birebirliği geçti. ELF 16 KB: evrensel APK 36, ARM64 APK 8, release AAB 36 kütüphane geçti. Bundletool 1.18.2 yapısal doğrulaması geçti; release AAB imzasız denetim çıktısıdır. Kamera analizi/startCamera, VoicePlayer, TranslationRuntime, FreeStreamingSpeech ve model seçimi kaynakları 0.27 ile aynı olduğu doğrulandı.

Fiziksel telefon, gerçek oyun sesleri, gerçek zamanlı gecikme ve Türkçe anlam doğruluğu: ÇALIŞTIRILMADI. Bu sürüm eksiksiz tercüman kalitesini kanıtlamaz. Kullanıcının sorunlu 20–30 saniyelik klibi ve isteğe bağlı Dinle tanılaması, kalan sorunu kaynak tanıma/çeviri/konuşma kuyruğu olarak ayırmak için gereklidir. Kaynakta önceki sürümlerin raporları tarihsel kayıt olarak korunur.
