# AI Game Translator 0.14 — Son sağlamlaştırma paketi

Android 10+ • Kamera altyazısı / mikrofon konuşması → Türkçe çeviri ve sesli okuma. Açık/koyu tema, Türkçe/İngilizce arayüz, fotoğraf/metin çevirisi ve konuşmacı profilleri korunur.

## Değişiklikler
- Kamera OCR tamamlanması oturum kimliğiyle kontrol edilir. Eski işlem yeni oturumun meşgul durumunu değiştiremez. Altyazı alanı sürüklenince kamera kilitlenmez.
- Arka planda ve yanlış modda gelen izin cevabı canlı kamerayı/mikrofonu başlatamaz. Mevcut ses ve kayıt arka plana geçince durur.
- Demo sayacı geri giden saat değerinde çift tüketim yapmaz. Sunucu tüketimi yerel sayacı geriye alamaz. Durdururken yerel tüketim eşzamanlı kaydedilir.
- Üst üste Billing sorgularında eski satın alma onayı yeni sorguyu geçersiz kılamaz. Pro seçenekleri açıkken fiyat sonuçları güncellenir; ödeme ekranı küçük ekranlarda kaydırılabilir.
- İsteğe bağlı Google hesabı + HTTPS sunucu modu eklendi. Sunucu tarafında Play Developer API, hesap bağlı ödeme doğrulaması/acknowledge, 180 saniyelik ortak deneme defteri ve idempotent kullanım bildirimi bulunur.
- Sunucu hatasında doğrulanmış Pro erişimi uydurulmaz. Hesap modunda erişim kontrolü başarısız olursa canlı kullanım durur. Temel fotoğraf/metin özellikleri açık kalır.

## Varsayılan test paketi
`pro_server_url` ve `google_web_client_id` boş bırakıldı. Bu nedenle hesap girişi zorunlu olmaz; çalıştığını doğruladığın yerel üç dakikalık demo devam eder. Uygulamanın verileri korunarak APK güncellemesi yapılırsa kalan demo korunur. Daha önce demo bitti ise güncelleme süreyi yeniden başlatmaz.

Hesap modu kurulmadan **veri silmeyle deneme sıfırlanması engellenmiş değildir**. Sunucu kodu pakete eklendi, canlıya kurulmadı. Aynı hesap için yalnızca sunucunun aldığı tüketim korunur; ani çökmede son bildirilmemiş bölüm kaybolabilir. Değiştirilmiş istemci veya çoklu hesap koruması tamamlanmış değildir.

Aylık $4.99, yıllık $49.99, ömür boyu $199. Gerçek ödeme için Play ürünleri/test hesapları gerekir. Sunucu modu için OAuth, HTTPS sunucu, kalıcı SQLite ve sunucuda servis hesabı gerekir. Kurulum: `PRO_SETUP.md`, ayrıntılı sunucu belgeleri: `server/README.md`.

## GitHub
ZIP içindeki Android_Game_Translator_0_14 klasörünün **içeriğini** mevcut repo köküne aktar; ikinci proje klasörü oluşturma. `server`, `.github`, `app`, `signing`, `.gitignore`, Gradle dosyaları ve belgeleri birlikte üzerine yaz.

```bash
git add .
git commit -m "v0.14: lifecycle hardening and account-backed Pro verification"
git push
```

Actions'ta önce sunucu testleri, sonra Android assembleDebug/testDebugUnitTest/lintDebug çalışır. Yeşil sonuç sonrası `AI-Game-Translator-0.14-hardening-debug` artifact içindeki APK'yı mevcut kurulumun üzerine yükle. 0.11+ sabit debug imzası korundu; üretim anahtarı değildir.

## Doğrulama durumu
- Bu ortamda 18 Python testinin tamamı geçti: ortak hesap tüketimi, yeniden kurulumda kalıcılık, tekrar/ters sıra bildirim, paralel kullanım, yanlış hesap token'ı, geçerli/pending/paused/on-hold/expired/iade durumları, acknowledge başarısızlığı ve HTTP giriş sınırları.
- XML, kaynak anahtarları, TR/EN formatlar, debug imzası ve ZIP içerikleri statik kontrol edildi.
- Android SDK/Gradle yok: APK derlemesi, Kotlin/JUnit ve lint bu ortamda çalıştırılmadı. Actions doğrulaması gerekiyor.
- Gerçek Google OAuth, servis hesabı, Play test ödemesi ve fiziksel cihaz pil/ısı/uzun oturum testleri yapılmadı. Kabul listesi `RELEASE_CHECKS.md` içinde.
