@echo off
where gradle >nul 2>nul
if %errorlevel%==0 (gradle %*) else (echo Gradle is required. Open android/ in Android Studio.)