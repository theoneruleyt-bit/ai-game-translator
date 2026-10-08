# AI Game Translator 0.10.1 — Anahtarsız sürekli dinleme

Android 10+ • İngilizce → Türkçe • Kamera, görsel, mikrofon.

## Yenilikler

0.10.1 düzeltmesi: Dinle modundaki kulaklık zorunluluğu kaldırıldı. Türkçe sesli oku seçeneği artık geri kapanmaz; otomatik okuma, ses örneği ve son çeviriyi tekrar okuma hoparlörden de çalışabilecek şekilde düzenlendi. Ses motoru, mikrofon ve kamera işleme kodları değiştirilmedi.

- Azure bağlantısı ve ücretli bulut konuşma bağımlılığı kaldırıldı. Vosk 0.3.75 ve JNA 5.18.1 ile cihaz içi sürekli mikrofon tanıma eklendi.
- AudioRecord 16 kHz mono PCM mikrofonu açık tutar. İngilizce ara metinler, sık güncellemelerde son metni tercih ederek yerel ML Kit çevirisine gönderilir. Ara çeviri değişebilir, seslendirilmez.
- Konuşma duraklamasında tamamlanan replik FIFO çeviri ve Türkçe ses kuyruğuna eklenir. Mikrofon Türkçe okunurken kapanmaz. Gerçek tekrar söylenen replikler korunur.
- `Konuşmayı bitir ve çevir` mevcut repliği elle tamamlar; mikrofonu kapatmaz. Konuşmanın ortasında basarsan kelime kesilebilir. Normal kullanımda doğal duraklamaları bekle.
- Kamera, seçili altyazı alanı, sabit HUD/süre filtresi, görsel çeviri ve konuşmacı profilleri korunur. Sesten otomatik konuşmacı/cinsiyet ayrımı eklenmedi.

## GitHub adımları

1. ZIP'i aç. İçindeki `Android_Game_Translator_0_10_1` klasörünün içeriğini mevcut repo köküne aktar; ayrı alt klasör oluşturma. `app` ve gizli `.github` klasörü dahil bütün mevcut dosyaları üzerine yaz.
2. `StreamingTranslator.kt` ve `SpeechSettings.kt` bu pakette yalnızca geçiş açıklaması içerir. Üzerine yazılmaları önemlidir: eski Azure SDK importlarının repoda kalmasını engeller.
3. Mevcut proje klasöründe terminal aç:

```bash
git add .
git commit -m "v0.10.1: Dinle modunda Turkce sesli okuma duzeltmesi"
git push origin main
```

4. GitHub Actions → Android APK yeşil olduğunda `AI-Game-Translator-0.10.1-voice-fix-debug` artifact'ını indir. İçindeki `app-debug.apk` dosyasını yükle.

Sürüm kodu 11. Sabit yayın imzalama bu sürümde eklenmedi; farklı CI debug imzaları mevcut uygulamanın üzerine yüklemeyi engelleyebilir.

## Telefonda ilk kurulum

1. İnternet açıkken uygulamayı aç. `Çeviri modelini hazırla` düğmesine bas, hazır olmasını bekle. Bu, mevcut İngilizce → Türkçe yerel ML Kit modelidir.
2. **Dinle → İngilizce konuşma modelini indir / kontrol et** düğmesine bas. Yaklaşık 40 MB Vosk modeli resmî alphacephei.com adresinden indirilir, açılır ve yüklenebilirliği doğrulanır. Açılmış model için ek depolama gerekir.
3. `Anahtarsız sürekli dinleme (Vosk)` seçili kalsın. Başlat ve mikrofon izni ver.
4. Bir TV/PC/başka cihazdan İngilizce oyun/video sesi oynat. Kaynak metin ve ara çeviri düğmeye basmadan güncellenmeli; duraklamada tamamlanan replik geçmişe eklenmeli.
5. `Türkçe sesli oku` seçeneğini aç. Hoparlör veya bağlı kulaklık üzerinden tamamlanan replikler okunur. Kulaklık zorunlu değildir. Gerçekten kulaklık kullanıyorsan ilgili kutuyu aç; kutu gerçek ses çıkışını değiştirmez.

Vosk model dosyası: https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip

## Ücretsiz / çevrimdışı kullanım

Azure hesabı, API anahtarı ve dakika başına bulut servis ücreti gerekmez. Vosk dinlemesi ve ML Kit çevirisi cihazda çalışır. Modeller bir kez hazırlandıktan sonra bu yol için internet gerekmez. Seçilen Türkçe TTS sesi de cihazda kurulu olmalı; `İnternet gerektiren sesleri de göster` seçeneğini kapalı tut. Ağ gerektiren TTS seçersen o ses motoru metni kendi hizmetine gönderebilir. Android konuşma test modu da cihazın sistem servisini kullanır ve internet gerektirebilir; ana yöntem Vosk'tur.

Mikrofon telefonun çevresindeki sesi alır. Telefonun iç oyun sesini doğrudan yakalayan ekran yakalama özelliği eklenmedi. Uygulama arka plana geçince veya Durdur'a basılınca kayıt durur; geç kalmış sonuçlar elenir. Aktif model indirmesi de durdurulur; tekrar indirme baştan başlar. Tamamlanmamış konuşma Durdur'a basınca seslendirilmez.

Sesli okuma seçeneği dinleme başlayınca veya kulaklık kutusu değişince kapatılmaz. Hoparlörle okuma sırasında mikrofon açık kaldığından kendi Türkçe sesi tekrar algılayabilir; düşük hoparlör sesi veya kulaklık önerilir. Mikrofon seviyesi gerçek PCM verisinden hesaplanır.

## Kalite ve sınırlar

Küçük İngilizce model oyun özel isimleri, müzik, gürültü, hızlı veya aksanlı konuşmalarda hata yapabilir. İşlem yükü, ısınma ve gecikme telefona bağlıdır. Çeviri doğal konuşma duraklamasını bekleyebilir; hiç duraklamayan uzun konuşmada tamamlanan sesli sonuç geç gelir. Ara metin ve çeviri bu sırada güncellenir. Ses kuyruğu hızlı konuşmaya yetişemezse geride kalabilir; dolarsa durum uyarısı çıkar. Sıfır gecikme veya bulut düzeyinde doğruluk garanti edilmez.

## Cihaz testi

- 60–90 saniye İngilizce videoda düğmeye basmadan yeni kaynak ve ara çeviri geldiğini kontrol et.
- Kulaklıkla Türkçe okunurken yeni İngilizce konuşmanın hâlâ algılandığını doğrula.
- Aynı kısa cümleyi iki kez söyle: ikisi de çevrilmeli.
- Modeller indirildikten sonra uçak modunda (kulaklık bağlantısını koruyarak) test et. Yerel Türkçe ses seçili olsun.
- Durdur ve arka plana geçişte Android mikrofon göstergesinin kapanmasını doğrula.
- Kamera moduna dönüp altyazı çevirisini kontrol et.

## Kontrol durumu

Bu ortamda Gradle ve Android SDK bulunmadığından APK derlemesi, unit testleri ve lint burada çalıştırılmadı. XML/YAML/ZIP ve kod değişiklikleri statik olarak incelendi. Model indirmesi, Vosk native yüklemesi, mikrofon ve TTS eşzamanlılığı gerçek telefonda doğrulanmalıdır. GitHub Actions derleme/unit test/lint kontrolünü çalıştırır.

## Lisanslar

Vosk API ve `vosk-model-small-en-us-0.15` modeli Apache 2.0 lisanslıdır. JNA LGPL 2.1+ veya Apache 2.0 seçenekleriyle dağıtılır. Model bu ZIP'e gömülmedi; uygulama resmî kaynaktan indirir. Model arşivindeki lisans dosyaları çıkarılırken korunur. Yayın öncesi bağımlılık lisans bildirimlerini ve dağıtılan native paketleri ayrıca kontrol et.

Resmî kaynaklar:
- https://alphacephei.com/vosk/models
- https://github.com/alphacep/vosk-android-demo
- https://github.com/alphacep/vosk-api
- https://developers.google.com/ml-kit/language/translation/android
