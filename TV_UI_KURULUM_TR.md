# TV Lab arayüz güncellemesi
Yalnız mevcut app/src/debug/java/com/optima/gametranslator/TvLabActivity.kt dosyasını günceller. TV Lab ilk yaması kurulu olmalı. Telefonun ana uygulamasını, release kaynaklarını, deneme haklarını ve TV manifestini değiştirmez.

Eklenenler: kart görünümü, turkuaz kumanda odağı, yoğunluğa uygun dp ölçüleri, telefonda tek sütun / geniş ekranda çok sütun, yazı boyutu/konumu/hizası/arka plan yoğunluğu ve kalıcı görünüm tercihleri. Başlangıç ve durdurma ile örnek ML Kit çeviri akışı korunur.

ZIP içeriğini mevcut depo köküne çıkarın. CMD:
git add app/src/debug/java/com/optima/gametranslator/TvLabActivity.kt TV_UI_KURULUM_TR.md
git commit -m "feat: improve TV Lab cards focus and subtitle appearance"
git pull --rebase --autostash origin main
git push origin main

Pull sırasında CONFLICT veya hata varsa sonraki komutu çalıştırmayın. Force push ve stash temizleme yapmayın.

Kontrol: Actions ana kontrol işi yeşil olduktan sonra test APK’sını kurun. TV Lab örneğini başlatın, bütün görünüm düğmelerini ve sıfırlamayı deneyin. Çıkıp tekrar açınca tercihler korunmalı. Home’a basıldığında örnek durmalı; geri dönünce kendiliğinden başlamamalı. İngilizce/Türkçe arayüz ve küçük/büyük ekranı karşılaştırın.

Bu sürüm için Android derlemesi, emülatör testi ve fiziksel cihazda görsel kontrol burada yapılmadı. Gerçek HDMI yakalama, overlay ve arka plan çevirisi eklenmedi; bu hâlâ örnek metin laboratuvarıdır. Kumanda odağı gerçek Android TV veya emülatörde ayrıca doğrulanmalıdır. TV mağaza yayınına hazır değildir.
