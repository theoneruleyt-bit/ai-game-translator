# 0.16.1 — Test düzeltmesi

Güncel düzeltme ve commit adımları: [FIX_0_16_1.md](FIX_0_16_1.md).

# 0.16 — Canlı konuşma çevirisi

Güncel kurulum, dil paketleri ve telefon testi: [LIVE_SPEECH_0_16.md](LIVE_SPEECH_0_16.md). APK indirmesi Releases taslağından yapılır.

# 0.15.1 indirme düzeltmesi

Yeni indirme adımları için [CI_DOWNLOAD_FIX.md](CI_DOWNLOAD_FIX.md) dosyasını okuyun. Önceki 0.15 artifact indirme yönergeleri bu pakette geçersizdir.

# AI Game Translator 0.15 — Son kontroller / Sunucusuz

Kamera ve mikrofon ile İngilizce → Türkçe yerel çeviri ve Türkçe seslendirme. Ortak 3 dakikalık aktif demo. Aylık $4.99 / yıllık $49.99 / ömür boyu $199 Pro modeli korunur.

Bu paket hedef Android API seviyesini 36'ya ve AGP'yi 8.10.1'e taşır. İlk sürüm sunucusuzdur: pro_server_url/google_web_client_id boştur. Yerel demo veri silmeyle sıfırlanabilir. Gerçek ödeme Play Console ürünü/lisans anahtarı ve satın alma testleri tamamlanınca etkinleşir.

## GitHub'a aktar
ZIP içindeki Android_Game_Translator_0_15 klasörünün içeriğini mevcut repo köküne aktar. `scripts`, `.github`, `app`, `server`, `signing`, `.gitignore`, Gradle dosyaları ve belgeleri birlikte üzerine yaz. İkinci iç içe proje klasörü oluşturma.

```bash
git add .
git commit -m "v0.15: API 36 and final release/native compatibility checks"
git push
```

Actions yeşil olduktan sonra AI-Game-Translator-0.15-final-checks-debug artifact içindeki APK'yı mevcut uygulamanın üzerine kur. Final-checks-and-unsigned-AAB-0.15 artifact içinde lint, native raporları ve imzasız release bundle bulunur. İmzasız AAB yayımlanacak dosya değildir; üretim imzası daha sonra hazırlanır. Artifact retention 3 gündür.

## Kontroller
Bu ortamda 23 Python testi ve kaynak kontrolleri geçti. Android SDK yok; APK/JUnit/lint/AAB ve gerçek native .so uyumluluğu GitHub'da doğrulanmalı. CI 16 KB ELF/ZIP kontrolü eklendi; başarısızsa rapordaki kütüphane düzeltilmeden mağazaya gidilmez. Kontrol baskılanmaz.

Önceki 0.14'ün yeşil olması yeni 0.15 veya mağaza uygunluğu için onay sayılmaz. Tam inceleme FINAL_REVIEW.md, telefon listesi RELEASE_CHECKS.md, ödeme ayarları PRO_SETUP.md içinde.
