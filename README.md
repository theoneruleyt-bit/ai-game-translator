# AI Game Translator 0.13 — Kamera düzeltmesi ve Pro denemesi

Android 10+ için canlı kamera altyazısı / mikrofon konuşması → Türkçe çeviri ve sesli okuma. Açık/koyu tema ve Türkçe/İngilizce arayüz korunur.

Bu sürüm aynı metnin yeniden okunmasını azaltır: gerçek görünürlük ile sabit metin filtresi ayrıldı. Eski bekleyen kamera sesleri ve geç kalmış çeviri sonuçları elenir; başlamış ses yarıda kesilmez. Ortak 3 dakikalık aktif kullanım demosu ve Pro seçenekleri eklendi.

Plan: aylık $4.99, yıllık $49.99, ömür boyu $199. Google Play ödeme altyapısı eklendi; ürünler ve açık lisans anahtarı yapılandırılmadan ödeme kullanılamaz. Ayrıntılar PRO_SETUP.md içinde. Sunucu doğrulaması yayın öncesi kalan iştir.

## GitHub
ZIP içindeki Android_Game_Translator_0_13 klasörünün içeriğini mevcut repo köküne aktar; ikinci iç içe proje oluşturma. `.github`, `.gitignore`, `app`, `signing`, Gradle dosyaları ve belgeleri birlikte üzerine yaz.

```bash
git add .
git commit -m "v0.13: camera dedup latest speech and shared Pro demo"
git push
```

Actions yeşil olunca AI-Game-Translator-0.13-pro-demo-debug artifact içindeki APK'yı kur. 0.11 ve sonrasının sabit debug imzası korundu. APK normal güncellemeyle kurulursa demo verisi korunur.

```bash
gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Bu teslim ortamında Android SDK/Gradle yok; yerel Android derlemesi, JUnit ve lint çalıştırılmadı. XML/kaynak referansı, paket içeriği, imza dosyasının korunması ve ZIP bütünlüğü statik kontrol edildi. GitHub Actions üç görevi çalıştırır; hata durumunda Android-lint-diagnostics raporunu yükler. Telefon kabul kontrolleri PRO_SETUP.md içinde.
