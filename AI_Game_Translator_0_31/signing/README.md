# Sabit debug test imzası

optima-test.jks yalnızca kişisel/geliştirme test APK'ları için oluşturulmuş açık test anahtarıdır. Parola android, alias optima-test. Gizli üretim anahtarı değildir ve Play Store/release için kullanılmamalıdır.

Bu dosyayı ve debug signingConfig ayarını sonraki geliştirme paketlerinde koru. Böylece 0.11'den sonra aynı applicationId ve yükselen versionCode ile üretilen debug APK'lar aynı imzayı taşır. Eski CI runner imzalı 0.10.1 ile eşleşmez; ilk geçişte mevcut kurulum üzerine yükleme Android tarafından reddedilebilir. Eski uygulamayı kaldırmak verileri ve indirilen modelleri siler; bu işlem otomatik yapılmaz.
