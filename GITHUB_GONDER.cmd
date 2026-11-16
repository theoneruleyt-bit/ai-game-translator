@echo off
setlocal
cd /d "%~dp0"
if not exist ".git" goto hata
findstr /L /C:"versionCode = 48" app\build.gradle.kts >nul
if errorlevel 1 goto hata
findstr /L /C:"signInForReview" app\src\main\java\com\optima\gametranslator\ProService.kt >nul
if errorlevel 1 goto hata
git diff --cached --quiet
if errorlevel 1 (
 echo Baska staged degisiklikler var. Ciktiyi sohbete gonderin.
 goto hata
)
git add -- app/build.gradle.kts app/src/main/java/com/optima/gametranslator/ProService.kt app/src/main/java/com/optima/gametranslator/PlayBilling.kt app/src/main/java/com/optima/gametranslator/MainActivity.kt worker/worker.mjs worker/worker.test.mjs worker/review.test.mjs worker/schema.sql
if errorlevel 1 goto hata
git diff --cached --check
if errorlevel 1 goto hata
git commit -m "feat: add server-authorized Play reviewer access in release 48"
if errorlevel 1 goto hata
git pull --rebase --autostash origin main
if errorlevel 1 goto hata
git push origin main
if errorlevel 1 goto hata
echo KAYNAKLAR GONDERILDI. Ciktiyi sohbete gonderin.
pause
exit /b 0
:hata
echo Islem durdu. Ciktiyi sohbete gonderin. Force push veya stash silme yapmayin.
pause
exit /b 1
