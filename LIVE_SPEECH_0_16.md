# 0.16 — Sürekli dinleme ve dil paketleri

## Değişiklikler

- Vosk ve Android sistem dinlemesi artık ortak ara çeviri akışını kullanır. Sistem modunda eskiden ara İngilizce metin gösteriliyor ama çevrilmiyordu.
- Ara sonuçlar ve kesin parçalar aynı anda aynı ML Kit motoruna gönderilmez. Tek çeviri işi çalışır; kararlı parçalar sırayla, ara önizleme en güncel metinle yürür.
- Kararlı sözcük önekleri, geri düzeltilebilecek son sözcük bırakılarak kısa parçalara ayrılır. Konuşma bitmeden yazılı çeviri ve çevrimdışı modda seslendirme başlar. Cümle sonunda sadece henüz okunmamış bölüm seslendirilir. Yazılı son görünüm tam cümleden tekrar çevrilir; bu tam cümle ikinci kez okunmaz.
- Boş tanıma sonucu da konuşma sınırını sıfırlar; önceki konuşmanın parça sayacı sonraki cümlenin başını kesmez. Tekrarlanan gerçek replikler korunur.
- Tanıma bitiş süreleri ve sistem modunun 5 saniyelik beklemeleri kısaltıldı. Mikrofonun kayıt tamponu küçültüldü; sürekli büyüyen bir ses/metin arşivi oluşturulmaz.
- İngilizce hızlı (40 MB), kapsamlı İngilizce (128 MB), Türkçe (35 MB) konuşma modeli seçilebilir. İlk indirmeden sonra cihazda çalışır. Modeller APK içinde değildir; seçilen modeli indirmeniz gerekir.
- Türkçe konuşma modeli seçilince dinleme Türkçe → İngilizce, seslendirme İngilizce olur. Kamera ve görsel modu İngilizce → Türkçe olarak devam eder. Gerçek ses çeşitliliği telefonda kurulu TTS seslerine bağlıdır.
- 219 tam ifade içeren günlük konuşma, argo, küfür ve oyun ifadeleri dosyası eklendi. İfade desteği kapatılabilir. İngilizce kısaltılmış konuşma biçimleri (gonna, wanna, gotta gibi) genel çeviriden önce açılır. Bir ifadenin bir cümle içindeki rastgele alt parçası sözlükten değiştirilmez.
- Farklı model arşiv kökleri, hem statik hem dinamik graph dosyaları desteklenir; yol kaçışı ve boyut sınırları korunur. Bozuk modelin hazır işareti kaldırılarak yeniden indirme mümkün olur. Bellek yetersizliğinde hızlı paket önerilir.
- Sistem modunda kararlı ara parçaların seslendirilmesi mikrofonu cümle ortasında kapatmasın diye, kulaklık yoksa ses cümle sınırında başlatılır. Sürekli dinleme için Vosk modu önerilir. Sesi kapatınca ertelenmiş sesler de silinir.

## Kurulum ve GitHub

ZIP içeriğini mevcut proje klasörünün üzerine kopyalayın. .github dahil tüm dosyalar kopyalanmalı.

```
git add .
git commit -m "v0.16: streaming speech translation and English Turkish packs"
git push
```

Actions yeşil olduktan sonra Releases → en yeni Translator CI taslağı → ci-download.zip içinden app/build/outputs/apk/debug/app-debug.apk dosyasını alın. VersionCode 20; aynı test imzası korunur. Artifact kotasına takılan eski yöntem kullanılmaz. İmzasız AAB halen yalnızca inceleme içindir.

## Telefonda kontrol

1. Dinle modunda Anahtarsız sürekli dinleme açık olsun. İlk test için İngilizce → Türkçe / Hızlı ve hız ayarından Hızlı seçin.
2. Seçilen konuşma paketini indir/kontrol et, ardından çeviri modelini hazırla.
3. Seslendirme kapalıyken 30–60 saniye normal İngilizce konuşma/video dinletin. Konuşma sürerken ara Türkçe metin görünmeli; cümle sonu beklenmemeli. Kaynak metnin doğruluğunu ayrıca kontrol edin.
4. Kaynak İngilizce yanlış tanınıyorsa Kapsamlı paketi indirip aynı örneği karşılaştırın. Daha büyük model her telefonda daha hızlı değildir; telefon yavaşlıyorsa hızlı pakete dönün.
5. Kulaklıkla seslendirmeyi açın. Kararlı bölümlerin son sonuç gelince tekrar okunmadığını, gerçek tekrarlanan repliğin okunabildiğini kontrol edin.
6. “cover me”, “we are screwed”, “what the hell”, “I wanna go” gibi ifadeleri deneyin. Kaynak yanlış tanınmışsa ifade sözlüğü bunu her zaman düzeltemez.
7. Türkçe paketini seçip indirin. Türkçe bir cümle söyleyin; İngilizce yazı ve telefonda kuruluysa İngilizce ses gelmeli.
8. Dinlerken durdur/başlat, tema değiştir, uygulamadan çık/geri dön ve kamera moduna dön işlemlerini deneyin. Eski oturumdan yazı/ses gelmemeli; kamera tekrar Türkçe seslendirmeli.

## Gerçek sınırlar

Bu sürüm ücretsiz, sunucusuz Vosk + ML Kit kullanır. ML Kit çevrimdışı çevirisi genel ve gündelik kullanım içindir; bütün İngilizce/Türkçe sözcükler, aksanlar ve argo için kusursuzluk garantisi değildir. 219 ifade bir ek sözlüktür; yeni eğitilmiş kapsamlı bir çeviri modeli değildir. Kararlı kısa parçalar düşük gecikme sağlar ama cümle ortasındaki çevirinin dilbilgisi son cümleden farklı olabilir. Birden çok kişinin üst üste konuşması, oyun müziği, hoparlör geri beslemesi ve uzak mikrofon doğruluğu azaltır. Uygulama mikrofonu dinler; telefondaki başka uygulamanın dahili sesini doğrudan yakalamaz. Sıfır gecikme veya her telefonda belirli bir gecikme vaat edilmez.

Kapsamlı İngilizce modelinin üretici kıyaslamaları daha iyi doğruluk gösterir, ancak ölçümler sokak argosu ve sizin cihazınız için garanti değildir. Büyük modellerin hız/bellek maliyeti vardır. Resmî modeller: https://alphacephei.com/vosk/models . ML Kit açıklaması: https://developers.google.com/ml-kit/language/translation . Modeller Apache 2.0 olarak listelenir; indirilen modelin lisans dosyaları korunur.

## Doğrulama

Yerel Python kontrolleri, kaynak/çeviri XML eşleşmesi, ifade dosyasının yapısı, test imzasının aynı kalması ve ZIP bütünlüğü denetlendi. Yeni Kotlin birim testleri kararlı parçaları, tekrarları, boş finali, önek değişimini, sözlük eşleşmesini, model köklerini ve boyut sınırını kapsar. Bu ortamda Android SDK/Kotlin derleyicisi yok; Kotlin testleri, Android derlemesi/lint ve gerçek cihaz gecikmesi GitHub ve telefon testinde doğrulanmalıdır.
