# 0.16.3 — Ayrı deneme hakları

| Özellik | Ücretsiz deneme | Pro |
|---|---|---|
| Kamera | Toplam 3 dakika | Sınırsız |
| Sürekli dinleme | Ayrı toplam 3 dakika | Sınırsız |
| Görsel çeviri | 10 görsel | Sınırsız |

Kamera kullanımı dinleme süresini azaltmaz. Bir mod bitince yalnızca o mod Pro ister; diğer kalan haklar kullanılabilir. Süre yalnızca kameranın/mikrofonun gerçekten başladığı aktif kullanımda ilerler. Model indirme, izin bekleme, durdurma ve uygulamadan çıkma süre tüketmez. Kullanım cihazda kaydedilir.

Görsel seçimini iptal etmek, yazı bulunamaması veya çeviri hatası hak tüketmez. Bir görselden en az bir başarılı çeviri sonucu alınırsa bir hak düşer; aynı görselin çok satırlı çevirisi birden fazla hak tüketmez. Aynı dosyayı yeniden seçip başarılı çeviri yaptırmak yeni bir kullanımdır. Kameradaki dondurma işlemi kamera süresine dahildir; galeri görsel hakkını tüketmez.

Elle metin çevirisi ve kayıtlı ifadeler ücretsiz kalır. Fiyatlar değişmedi: aylık $4.99, yıllık $49.99, ömür boyu $199. Deneme otomatik ücretlendirilmez.

## Eski sürümden geçiş

Eski ortak sayaç hangi modda harcandığını kaydetmediği için iki moda paylaştırılmaz. Yeni ayrı haklar bu güncellemede bir defaya mahsus 3 + 3 dakika ve 10 görsel olarak başlar. Sonraki güncellemeler yeni sayaçları korur. Uygulamayı kaldırıp kurmak veya verilerini silmek sunucusuz sayaçları sıfırlayabilir; bu bir sunucusuz sürüm sınırıdır.

Yeni haklar cihazda tutulur. Önceki opsiyonel sunucunun tek ortak trialUsedMillis değeri yeni iki süreye uygulanmaz ve o eski kullanım uç noktasına veri gönderilmez. Sunucu sonradan etkinleştirilirse mevcut Pro satın alma doğrulaması çalışır, ancak bu yeni deneme hakları hesaplar arasında eşitlenmez. İlk sürüm için sunucu ayarları boş kalır.

## Kurulum

Paket içeriğinin tamamını mevcut proje klasörüne üzerine kopyalayın:

```
git add .
git commit -m "v0.16.3: separate camera listening and image trials"
git push
```

Actions yeşil sonrası Releases → en yeni Translator CI taslağı → ci-download.zip içindeki APK'yı kurun. VersionCode 23, test imzası korunur. Dinleme düzeltmesi 0.16.2'den korunur; kamera çeviri işlemi değişmez.

## Kontrol

Kamera 30 saniye çalışsın: kamera 150 saniye, dinle 180 saniye kalmalı. Dinleme 20 saniye çalışsın: kamera 150 saniye, dinle 160 saniye kalmalı. Bir görsel başarıyla çevrilince görsel 9/10 olur. İptal ve boş OCR sonucu sayacı değiştirmemeli. Uygulamayı kapatıp açınca haklar korunmalı. 10. görsel başarılı çevrilmeli, 11. seçim Pro ekranına yönlendirmeli. Kamera süresi bitince dinleme ve görsel hakları kullanılabilmeli.

Yerel Python ve paket/ kaynak kontrolleri yapıldı. Yeni Kotlin testleri bağımsız süre, yeniden yükleme ve 10/11 görsel sınırını kapsar. Android derlemesi/Kotlin testleri ve cihaz davranışı GitHub çalıştırması ve telefon testiyle doğrulanacak.
