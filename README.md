# AI Game Translator 0.11 — Birleştirilmiş geliştirme paketi

Android 10+ · İngilizce → Türkçe · Anahtarsız cihaz içi çeviri.

## Gecikme düzenlemeleri

- Mikrofon okuma parçası 100 ms yerine 50 ms. Kayıt tamponu taşmaya karşı korunur; mikrofon okumak için tamamen dolması beklenmez.
- Varsayılan Hızlı mod: modelin endpoint sessizlik kuralları 0.25 / 0.60 / 0.90 saniye; uzun kesintisiz konuşma bölümü üst sınırı 4 saniye. Dengeli mod 0.40 / 0.80 / 1.20 ve 8 saniye; Tam cümle modu 0.60 / 1.0 / 1.80 ve 20 saniye. Bunlar karar kurallarıdır; uçtan uca gecikme garantisi değildir.
- Ara çeviri zamanlayıcısı 400 ms yerine Hızlı modda 100 ms. Tek ara çeviri işlemi sürerken yalnızca en güncel metin bekletilir.
- Aynı kaynak metnin ara çevirisi hazırsa tamamlanan sonuç için ikinci kez hesaplanmaz. Farklı/revize edilmiş metin için yeniden çeviri yapılır.
- Ses kuyruğu büyüdüğünde sıradaki repliklerin okuma hızı kontrollü artırılır. Mevcut okuma kesilmez, mikrofon replikleri kuyruktan atılmaz. Kuyruk kapasitesi aşılırsa açık uyarı gösterilir.

Hızlı mod daha kısa bağlam kullanır ve bazen cümlenin ortasında bölüm çıkarabilir. Tam cümle daha iyi bağlam sağlar ama bekleme artar. Ara çeviri değişebilir; yanlış/revize edilebilir ara metinler otomatik seslendirilmez. Uzun kesintisiz konuşmada 4 saniyelik bölüm sınırı hâlâ birkaç saniye bekleme yaratabilir. Sıfır gecikme ve 1 saniyelik kesin hedef vaat edilmez. Gerçek telefonla karşılaştırmalı test gerekir.

## Birleştirilen diğer çalışmalar

- 16 konuşmacı profili (önceden 8), profil başına gerçek Türkçe TTS sesi seçimi.
- Altı ton/tempo tarzı: Doğal, Daha kalın ton, Daha ince ton, Sakin, Enerjik, Anlatıcı. Bunlar mevcut sesin varyasyonlarıdır; yeni gerçek kişi, erkek/kadın sesi veya aksan değildir. Gerçek Türkçe ses sayısı telefonun TTS motoruna bağlıdır.
- İsimli altyazıda konuşmacı → profil eşleştirmesi; mikrofon modunda elle konuşmacı seçimi. Sesten otomatik kişi/cinsiyet ayrımı veya ses klonlama eklenmedi.
- Ayarlar: Hızlı/Dengeli/Tam cümle, kuyrukta kontrollü hızlandırma, aktif çeviride ekranı açık tutma. Sesli okuma, hız, ton, seçili konuşmacı, altyazı filtresi ve uygun yerel ses tercihi kaydedilir. İnternet sesleri ve kulaklık beyanı her açılışta yeniden seçilir.
- İlk kullanım rehberi, her zaman açılabilir yardım, oturum süresi, çeviri işlemi süresi. Gösterilen çeviri işlemi süresi tüm konuşma tanıma ve ses bekleme gecikmesi değildir.
- Oturum geçmişinde en fazla 100 replik saklanır, son dört replik gösterilir; paylaş ve temizle düğmeleri eklendi. Paylaşım yalnızca Android seçim ekranını açar; kullanıcı uygulamayı ve alıcıyı seçer. Geçmiş disk üzerinde kalıcı olarak tutulmaz.
- Sabit debug test imzası: sonraki test APK'larında aynı anahtarla güncelleme mümkün olur. Play Store üretim imzası değildir.
- Kamera altyazı alanı, sabit HUD/süre filtresi ve görsel çeviri korunur. Play Store yayın, mağaza materyalleri ve ödeme sistemi bu paket kapsamında değildir.

## GitHub

ZIP içindeki Android_Game_Translator_0_11 klasörünün tüm içeriğini mevcut repo köküne aktar. Gizli `.github`, `.gitignore`, `app`, `signing` ve Gradle dosyalarının üzerine yaz. İç içe proje klasörü oluşturma.

```bash
git add .
git commit -m "v0.11: hizli dinleme, 16 konusmaci profili ve birlesik iyilestirmeler"
git push origin main
```

Actions → Android APK yeşil olunca `AI-Game-Translator-0.11-complete-debug` artifact'ını indir, APK'yı telefona kur. `.gitignore` yalnızca bu test jks dosyasını istisna tutar; başka anahtarlar ignored kalır.

Sürüm kodu 12. Önceki APK farklı CI debug imzasına sahipse 0.11 ilk geçişte üzerine kurulamayabilir. Sabit test imzası yalnızca bu sürümden sonraki paketlerde sorunu önler. Eski uygulamayı kaldırmak tercihlerini ve modellerini siler; gerekirse yeniden indir. `signing/README.md` detayları açıklar. Bu test anahtarını değiştirme ve üretim imzası olarak kullanma.

## İlk kullanım

1. Çeviri modelini hazırla.
2. Dinle → İngilizce konuşma modelini indir / kontrol et. Yaklaşık 40 MB resmi Vosk modeli cihazda açılır; ek depolama gerekir. Azure/API anahtarı gerekmez.
3. Ayarlar → Dinleme: Hızlı seçili olsun. Başlat, mikrofon izni ver. Başka cihazdan İngilizce video/oyun sesi oynat.
4. Türkçe sesli oku seçili olsun. Kulaklık önerilir, zorunlu değildir. Hoparlör sesi mikrofona geri karışabilir.
5. Gerçek sesler için Türkçe ses seç / Konuşmacı seslerini ayarla. Ek ton varyasyonları için Konuşmacı tonunu / tarzını ayarla.

Modeller ve yerel Türkçe TTS hazırsa internet gerekmez. İnternet sesi veya Android konuşma test modu seçilirse ilgili hizmet ağ kullanabilir. Telefonun iç oyun sesini doğrudan yakalama özelliği yok; çevrede duyulan ses mikrofondan alınır. Arka plana geçince ve Durdur'a basılınca kamera/mikrofon, zamanlayıcılar ve ses kuyruğu durur. Model indirmesi de iptal edilir.

## Doğrulama

Bu ortamda Android SDK/Gradle yok. Derleme, unit testleri, lint ve gerçek cihaz gecikmesi burada çalıştırılmadı. XML/YAML/ZIP, debug anahtarı ve statik kod kontrolleri yapıldı. GitHub Actions unit testleri ve lint'i çalıştırır; cihaz testi aşağıdadır.

- Aynı 60–90 saniyelik video üzerinde Hızlı ve Dengeli modları karşılaştır. Ekrandaki ilk ara çeviri ve ilk Türkçe sesin ne zaman başladığını ayrı değerlendir.
- Müzik, hızlı replik, duraklamalı konuşma ve uzun kesintisiz konuşmayı dene.
- Türkçe okuma sırasında yeni İngilizce repliklerin alındığını ve mevcut sesin yarıda kesilmediğini doğrula.
- 1, 8, 9 ve 16. profilin gerçek ses ve tarz seçimini dene; mikrofon kişi seçimi elle yapılır.
- Durdur, arka plana geçiş, mikrofon izni reddi, uçak modu ve yeniden açılışta tercihlerin korunmasını kontrol et.
- Kamera ve görsel çeviriyi kontrol et. Sabit test imzasıyla sonraki yükselen sürüm kodlu APK'nın üzerine kurulabildiğini doğrula.

Vosk API ve İngilizce küçük model Apache 2.0 lisanslıdır. JNA LGPL 2.1+ / Apache 2.0 seçeneklidir. Model lisans dosyaları çıkarılırken korunur. Yeni doğal sesler cihazın TTS paketlerinden gelir; bu pakete lisanssız ses modeli eklenmedi.

Resmî kaynaklar:
https://alphacephei.com/vosk/models
https://github.com/alphacep/vosk-api
https://developer.android.com/reference/android/speech/tts/TextToSpeech
https://developers.google.com/ml-kit/language/translation/android
