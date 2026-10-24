# 0.15.1 — GitHub indirme düzeltmesi

Actions artifact kotası uygulama kaynak koduyla açılamaz. Bu paket upload-artifact adımlarını kaldırır; kontrollerden sonra ZIP'i GitHub Releases taslağına yükler. Eski artifact veya sürümler silinmez. Taslak yayımlanmaz; depoya yazma yetkisi olan geliştirici indirir.

## Kurulum

Paket içeriğini mevcut depo klasörüne üzerine yazın. .github klasörünün de kopyalandığını kontrol edin.

```
git add .
git commit -m "Fix CI downloads using draft release assets"
git push
```

Yeni çalıştırmanın tamamlanmasını bekleyin. GitHub deponuzda Releases bölümünü açın, en yeni Translator CI taslağına girin ve ci-download.zip dosyasını indirin. Tüm kontroller geçtiyse ZIP içinde app/build/outputs/apk/debug/app-debug.apk bulunur. APK aynı test anahtarıyla imzalanır; versionCode 19'dur. AAB imzasız inceleme çıktısıdır, Play'e gönderilemez.

Kontroller başarısızsa ZIP yalnızca raporları içerir. Kırmızı derleme/lint/native kontrolü gizlenmez. Actions çalıştırmasının Summary bölümünde de durum ve taslak bağlantısı gösterilir. Eski çalıştırmayı yeniden başlatmak eski workflow'u kullanır: bu paketi commit etmek gereklidir.

Draft oluşturma HTTP 403 verirse repo Settings → Actions → General → Workflow permissions veya kuruluş politikası contents: write yetkisini engelliyor olabilir. Workflow zaten gereken yetkiyi ister. Kuruluş kısıtlaması kodla aşılamaz.

Yalnızca varsayılan dal push/manuel çalıştırmaları dosya yükler; PR kontrolleri dosya yüklemez. Ek kişisel erişim anahtarı gerekmez. CI taslaklarını ihtiyaç kalmadığında elle silebilirsiniz.

## Doğrulama sınırı

Yerel Python testleri ve workflow yapısı denetlendi. Android derlemesi, lint, gerçek native kütüphane uyumluluğu ve GitHub yükleme işlemi bu ortamda çalıştırılmadı; yeni Actions çalıştırması doğrulayacak.
