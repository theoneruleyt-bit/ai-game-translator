# 0.29 — Gelişmiş Dinle çevirisi, deneysel sürüm

Bu paket son kullanıcı yayını için kalite onayı değildir. Kamera, ses tanıma ve seslendirme algoritmaları 0.28 sürümündeki haliyle korunmuştur. Değişiklik Dinle modunun İngilizce → Türkçe metin çevirisi aşamasındadır.

## Telefonda deneme

1. ARM64 test APK’sını mevcut test uygulamasının üstüne kurun. Paket kimliği aynıdır, sürüm kodu 40’tır ve önceki test anahtarıyla imzalanır.
2. Dinle kartında **Gelişmiş dil paketini yönet** düğmesine basın. Wi-Fi üzerinden **1,83 GB** paket indirin. İndirmeden önce alan/bellek uygunluğu kontrol edilir; yaklaşık 4 GB boş alan (model ve çalışma önbelleği) ayırın; kesilirse tekrar basınca kaldığı yerden devam eder. Sonunda tüm dosyanın SHA-256 özeti doğrulanır.
3. **Gelişmiş çeviri — beta** seçeneğini açın. Bu seçenek küçük OPUS-MT seçeneğiyle aynı anda açık olmaz. Sürekli Dinle açık olmalıdır.
4. Dinle başlatılınca model hazırlanır ve kısa bir hız denemesi yapılır. Hazırlık sırasında 3 dakikalık dinleme denemesi işlemez. Durdur, hazırlık sonucunu geçersiz kılar.
5. Model destekleniyorsa GPU, aksi halde CPU denenir. Ekrandaki motor satırına bakın: **Hy-MT2** ve **ML Kit** farklı motorları gösterir. Taslak yazılar hızlı motordan gelir; bunların önbelleği gelişmiş nihai çeviri yerine kullanılmaz.
6. Test için aynı 30–60 saniyelik oyun kaydını önce hafif, sonra gelişmiş modda deneyin. Kaynak metin doğru ama Türkçesi yanlışsa bu çeviri hatasıdır; kaynak metin zaten yanlışsa bu ASR hatasıdır. İsteğe bağlı metin tanılama raporunda kullanılan çeviri motoru da bulunur.

64 bit süreç ve en az 6 GB sınıfı RAM gerekir; hazırlık anında yaklaşık 2,5 GB boş bellek aranır. Bu eşikler başarı garantisi değildir. Samsung marka kontrolü yoktur. S23 Ultra dahil hiçbir fiziksel telefonda bu ortamdan performans veya kalite doğrulaması yapılmamıştır.

## Çalışma biçimi

- Çeviriye özel **Hy-MT2-1.8B INT8**, LiteRT-LM 0.16.1; sunucu veya ücretli anahtar gerektirmez. Apache-2.0 lisansları eklenmiştir. Mobil veri ücreti bağlantı sağlayıcınıza bağlıdır.
- Önceki en fazla iki kabul edilmiş İngilizce replik yalnızca arka plan olarak gönderilir; 20 saniye boşlukta, durdurmada ve yeni oturumda temizlenir. Eski Türkçe hatalar bağlama eklenmez.
- Model şablonunun çok mesajlı konuşma sorunu nedeniyle her çeviride **yeni, tek mesajlı** model konuşması açılır. Geçmiş KV önbelleği sınırsız büyümez.
- Her seferde tek fiziksel çıkarım çalışır. 4,5 saniye sınırında iptal istenir, geç gelen sonuç ekrana/sese yazılmaz. Native iş bitmeden ikinci native iş başlatılmaz. Gecikme veya birikmede hızlı motora dönüş ve görünür durum satırı vardır.
- Hatalı biçim, değişen rakamlar, belirgin tekrar, yabancı alfabe, çevrilmemiş veya kesilmiş görünen çıktı reddedilir. Bu denetimler anlamın doğru olduğunu kanıtlamaz.
- Gelişmiş motor varsayılan olarak kapalıdır. Uygun olmayan cihazlar hafif motorla devam eder. Paket yönetiminden büyük modeli silebilirsiniz.

## Kalite sınırı

Deyimlerde faydalı örnekler görülmüştür; ancak deneylerde yanlış özne, argo yorumu ve emir anlamı hataları da vardır. Paket **kusursuz, Samsung Interpreter ile eşdeğer veya tüm repliklerde daha iyi** olarak sunulmaz. Sayısal metin benzerliği sonuçları ve ham çıktılar `qa/context-translation` klasöründedir. İnsan incelemesi ve gerçek cihazda uzun oturum testi yapılmadan Play sürümünde varsayılan açılmamalıdır.

## GitHub

ZIP içindeki `AI_Game_Translator_0_29` klasörünün içeriğini mevcut deponun köküne uygulayın; `app` klasörü depo kökünde kalmalı. Özel `release.properties` ve özel Play imza dosyalarınızı paylaşmayın. Değişiklikleri gözden geçirip:

```bash
git add .
git commit -m "Add optional on-device advanced Listen translation beta"
git push
```

Actions kontrolü: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:bundleRelease :app:lintRelease`. Mevcut kota sorununu önleyen taslak Release indirme akışı korunur. İmzasız release AAB tek başına Play yüklemesi için hazır değildir; özel Play yapılandırması ve yükleme anahtarı yine gereklidir.
