@echo off
rem Bygger Tap&Talks debug-APK lokalt. Ett kommando, inget moln, inga krediter.
rem
rem Bygget gar inte genom Gradle. android\build_debug.py kallar aapt2, javac,
rem d8, zipalign och apksigner direkt, och behover darfor ingen wrapper och
rem ingen nedladdning vid varje korning.
setlocal

set "HERE=%~dp0"

if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if not exist "%ANDROID_HOME%\platforms\android-35\android.jar" (
  echo FEL: hittar inte Android SDK med platforms;android-35.
  echo Letade i: %ANDROID_HOME%
  echo Satt ANDROID_HOME till din SDK-mapp, eller kor:
  echo   "%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat" --install "platforms;android-35"
  exit /b 1
)

rem Android Studios egen JDK om den finns, annars den i PATH.
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\javac.exe" (
  set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
  set "PATH=%JAVA_HOME%\bin;%PATH%"
)
where javac >nul 2>nul
if errorlevel 1 (
  echo FEL: hittar ingen javac. Installera en JDK 17 eller senare,
  echo eller satt JAVA_HOME till en.
  exit /b 1
)

where py >nul 2>nul
if errorlevel 1 (set "PY=python") else (set "PY=py")

echo Bygger Tap^&Talk for Android ...
"%PY%" "%HERE%android\build_debug.py"
if errorlevel 1 (
  echo.
  echo FEL: bygget gick inte igenom. Raderna ovan sager var.
  exit /b 1
)

set "APK=%HERE%android\build\TapnTalk-android-test.apk"
if not exist "%APK%" (
  echo FEL: bygget sa inget men lamnade ingen APK.
  exit /b 1
)
echo.
echo KLAR. APK:n ligger i:
echo   %APK%
for %%F in ("%APK%") do echo   %%~zF byte, %%~tF
echo.
echo Installera den pa en emulator eller telefon med starta.bat
endlocal
