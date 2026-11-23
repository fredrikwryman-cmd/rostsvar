@echo off
rem Startar emulatorn och lagger den senast byggda APK:n pa den.
rem Skapar AVD:n forsta gangen. Ett kommando, inget moln.
setlocal enabledelayedexpansion

set "HERE=%~dp0"
set "AVD=TapnTalk_API35"
set "IMAGE=system-images;android-35;google_apis;x86_64"
set "APK=%HERE%android\build\TapnTalk-android-test.apk"

if not defined ANDROID_HOME set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
set "EMU=%ANDROID_HOME%\emulator\emulator.exe"
set "AVDMAN=%ANDROID_HOME%\cmdline-tools\latest\bin\avdmanager.bat"

if not exist "%ADB%" (echo FEL: hittar inte adb i %ANDROID_HOME%\platform-tools. & exit /b 1)
if not exist "%EMU%" (echo FEL: hittar inte emulatorn i %ANDROID_HOME%\emulator. & exit /b 1)

if not exist "%APK%" (
  echo FEL: ingen APK byggd an. Kor bygg.bat forst.
  exit /b 1
)

if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
  set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
)

rem --- AVD:n, om den inte redan finns ---
"%EMU%" -list-avds 2>nul | findstr /x /c:"%AVD%" >nul
if errorlevel 1 (
  echo Skapar emulatorn %AVD% ...
  if not exist "%ANDROID_HOME%\system-images\android-35\google_apis\x86_64" (
    echo FEL: systembilden saknas. Hamta den med:
    echo   "%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat" --install "%IMAGE%"
    exit /b 1
  )
  echo no | "%AVDMAN%" create avd -n "%AVD%" -k "%IMAGE%" -d pixel_6 --force
  if errorlevel 1 (echo FEL: gick inte att skapa AVD:n. & exit /b 1)
)

rem --- starta emulatorn om ingen redan kor ---
"%ADB%" start-server >nul 2>nul
for /f "tokens=*" %%L in ('"%ADB%" devices ^| findstr /r /c:"emulator-.*device"') do set "RUNNING=%%L"
if defined RUNNING (
  echo Emulatorn kor redan.
) else (
  echo Startar emulatorn. Forsta starten tar ett par minuter ...
  start "" "%EMU%" -avd "%AVD%" -no-snapshot-save -no-boot-anim
)

echo Vantar pa att Android ska bli klart ...
"%ADB%" wait-for-device
if errorlevel 1 (echo FEL: emulatorn svarade inte. & exit /b 1)

rem Vantar pa att systemet verkligen bootat, inte bara att adb svarar.
set /a TRIES=0
:wait
set "BOOT="
for /f "tokens=*" %%B in ('"%ADB%" shell getprop sys.boot_completed 2^>nul') do set "BOOT=%%B"
echo !BOOT! | findstr /c:"1" >nul
if not errorlevel 1 goto booted
set /a TRIES+=1
if !TRIES! GEQ 90 (
  echo FEL: emulatorn blev inte klar inom tre minuter.
  echo Stang den, kor starta.bat igen, eller starta den fran Android Studio.
  exit /b 1
)
"%ADB%" shell sleep 2 >nul 2>nul || ping -n 3 127.0.0.1 >nul
goto wait

:booted
echo Installerar APK:n ...
"%ADB%" install -r "%APK%"
if errorlevel 1 (
  echo.
  echo Gick inte att uppgradera. Provar att avinstallera forst.
  "%ADB%" uninstall com.techrabbit.tapntalk >nul 2>nul
  "%ADB%" install "%APK%"
  if errorlevel 1 (echo FEL: installationen gick inte igenom. & exit /b 1)
)

echo Startar appen ...
"%ADB%" shell monkey -p com.techrabbit.tapntalk -c android.intent.category.LAUNCHER 1 >nul 2>nul

echo.
echo KLAR. Tap&Talk ligger pa emulatorn.
echo Loggen ser du med:
echo   "%ADB%" logcat -s TapnTalk
endlocal
