# AI Game Translator Android 0.8 — Birleşik düzenleme

0.7 dinleme düzeltmeleri, kurulu Türkçe ses seçimi, hız/ton kontrolleri ve 0.6 kamera altyazı alanı/HUD filtreleri bu pakette birlikte bulunur. Önceki kamera görsel/simge tasarımı korunur.

## Bu pakette çalışan kod olarak eklenen kapsam
- Kamera altyazı bölgesi; süre/HUD filtreleri; galeri ve tek kare.
- Mikrofon sistem tanıması, stopListening ile sonuç bekleme, ses düzeyi ve kısa sistem konuşma testi. Cihaz sonucu henüz doğrulanmadı.
- Gerçek kurulu Türkçe sesleri seçme ve ses önizlemesi. İnternet sesleri açık onaya bağlı.
- Sekiz konuşmacı profili. Jason: / [Lucia] gibi açık altyazı etiketlerinden isim eşleştirme; aynı ad aynı slotu kullanır. İsim/gerçek ses ataması uygulama ayarlarında saklanır. İsim içermeyen konuşmalar seçili elle profilden okunur.
- Mikrofon modunda kişi kimliği verilmediğinden 'Etiketsiz konuşmacı' elle seçilir. Mikrofon kayıtlarından otomatik erkek/kadın veya 3–4 kişi ayrıştırması YOKTUR.

Kuyruktaki her ses parçası kendi ses seçimini taşır. Ses, sıradaki replik başlamadan değiştirilir; yeni konuşmacı mevcut repliği yarıda kesmez. Canlı kamera en güncel bekleyen metin/sesi tutar; kaynak okuma hızından hızlıysa ara replikler yine atlanabilir. Sekizden fazla etiket seçili genel/elle profile döner. Unknown isim etiketi veya HUD metni yanlış konuşmacı sayılabilir; elle alan ve otomatik isim anahtarıyla kontrol edilir.

## Konuşmacı seslerini kur
1. Modeli hazırla. Türkçe ses seç menüsünde kaç gerçek ses bulunduğunu kontrol et.
2. Konuşmacı seslerini ayarla → Konuşmacı1/2/3/4… → gerçekten mevcut Türkçe ses seç. Önizleme oynatılır.
3. İsimli altyazıda konuşmacı adlarını eşleştir açıkken Jason: Hello ve Lucia: Wait örneklerini kamera/görselde dene. Sonuç kartı ilgili profil numarasını gösterir.
4. Aynı ad tekrar geldiğinde aynı profil/ses kullanılmalı. Ses atamalarını kaydettiğin için sonraki açılışta da isim/slot korunur.
5. Etiketsiz veya mikrofon konuşmasında elle profili seç. Bu otomatik kişi tespiti değildir.

İlk kullanımda gerçek mevcut sesler profillere sırayla dağıtılır. Telefondaki ses sayısı profil sayısından azsa sesler tekrar kullanılır; olmayan erkek/kadın/aksan sesleri üretilmez. Gerçek cinsiyet/aksan TTS ses metadata'sında güvenilir biçimde sunulmadığından kullanıcı duyup seçim yapar. Ton değiştirme gerçek aksan değildir. Yeni doğal AI TTS motoru entegre edilmedi; doğallık cihazın motoruna bağlıdır.

## GitHub
ZIP içeriğini depo köküne kopyala, dosyaları değiştir (.github dahil). 0.7'den silinecek dosya yok; 0.6'dan doğrudan da güncelleyebilirsin.

```bash
git add .
git commit -m "Combine speech fixes with persistent speaker voice profiles"
git push
```

Actions → yeşil çalışma → Artifacts → AI-Game-Translator-0.8-speaker-profiles-debug → app-debug.apk. versionCode8. Önceki debug imzası değişmişse üzerine kurulum yapılamaz; kaldırıp kurmak ayarları siler. Sabit CI imzalama henüz kurulmadı.

## Telefon testi sırası
1. Kamera/görsel: çalışan alan/HUD akışı aynı sonucu vermeli.
2. Dinle: sistem tanıması (cihazda seçeneği kapalı), internet açık, otomatik ses kapalı. İngilizce kaynak ve Türkçe sonuç ayrı kontrol edilir. Olmazsa kısa konuşma testi sonucu, durum mesajı, telefon modeli/Android sürümü bildirilir.
3. Ses seçimi ve sekiz profil: gerçek ses sayısı, farklı seslerin duyulması, adların tutarlı eşleşmesi, arka plana geçince durması.
4. Gerçek mikrofon konuşmacı ayrıştırması bu testin kapsamı değildir.

## Sonraki gerçek geliştirme kapıları
1. Bu cihazda dinlemenin çalışması doğrulanmalı. SpeechRecognizer oturumları garantili sürekli streaming değildir; konuşma oturumları arasında boşluklar olabilir. Kulaklıksız okuma mikrofonu durdurur.
2. Doğal Türkçe çoklu ses için gerçek TTS sağlayıcısı/modeli, dil/aksan kapsamı, kalite, gecikme ve maliyet ölçülür. API anahtarı uygulamaya gömülmez; cloud özelliği açık onay/veri akışı gerektirir.
3. Mikrofondan otomatik konuşmacı ayrıştırma için VAD + streaming ASR + diarization motoru gerekir. timestamp, speaker_id, confidence olayları taşımalı. TTS ses seçimi artık bu kimliğe bağlanabilecek şekilde düzenlenmiştir. Üst üste konuşma ve konuşmacı kimlik değişimleri ölçülmeden 3–4 veya sınırsız kişi desteği iddia edilmez. Bu motor bu ZIP'te yoktur.

## Doğrulama
28 test bildirimi: önceki22 + 6 konuşmacı ad/slot/kapasite/kalıcılık testleri. XML/YAML/ZIP ve kaynak akışı kontrol edilir. Ortamda Android SDK/Gradle/Kotlin yok: yeni build/test/lint ve cihaz işleyişi çalıştırılmadı. Yeni CI sonucu gerekli. Cihazda iyi çalışan 0.6 kamera kaynak dosyaları korunmuştur.
