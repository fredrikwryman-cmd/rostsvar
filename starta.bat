@echo off
rem Startar emulatorn och lagger den senast byggda APK:n pa den.
rem Skapar AVD:n forsta gangen. Ett kommando, inget moln.
rem
rem   starta.bat              normal start
rem   starta.bat nollstall    glom fonstrets lage och storlek forst
setlocal enabledelayedexpansion

set "HERE=%~dp0"
set "AVD=TapnTalk_API35"
set "IMAGE=system-images;android-35;google_apis;x86_64"
set "APK=%HERE%android\build\TapnTalk-android-test.apk"
set "AVDDIR=%USERPROFILE%\.android\avd\%AVD%.avd"

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
set "FRESH="
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
  set "FRESH=1"
)

rem --- grafik och minne ---
rem avdmanager skapar AVD:n med hw.gpu.enabled = no. Mjukvarurendering pa en
rem 1080x2400-skarm gor att emulatorn fryser under boot. Och 1536M racker inte
rem for API 35. Bada rattas har, en gang, pa en nyskapad AVD.
if defined FRESH (
  if exist "%AVDDIR%\config.ini" (
    findstr /v /b /c:"hw.gpu.enabled" /c:"hw.gpu.mode" /c:"hw.ramSize" /c:"hw.keyboard " "%AVDDIR%\config.ini" > "%AVDDIR%\config.new"
    >> "%AVDDIR%\config.new" echo hw.gpu.enabled = yes
    >> "%AVDDIR%\config.new" echo hw.gpu.mode = auto
    >> "%AVDDIR%\config.new" echo hw.ramSize = 2048M
    >> "%AVDDIR%\config.new" echo hw.keyboard = yes
    move /y "%AVDDIR%\config.new" "%AVDDIR%\config.ini" >nul
    echo Grafik pa, minne 2048M.
  )
)

rem --- fonstret ---
rem Skarmen ar 1080x2400. I 1:1 blir fonstret hogre an de flesta
rem datorskarmar och hamnar halvt utanfor. 0,33 ger 356x792.
if defined FRESH goto setwindow
if /i "%~1"=="nollstall" goto setwindow
goto window_done
:setwindow
> "%AVDDIR%\emulator-user.ini" (
  echo window.x = 40
  echo window.y = 20
  echo window.scale = 0.330000
  echo resizable.config.id = -1
  echo posture = 0
)
echo Fonstret stallt till 33 procent, uppe till vanster.
:window_done

rem --- starta emulatorn om ingen redan kor ---
"%ADB%" start-server >nul 2>nul
set "RUNNING="
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
rem Kall forsta start pa API 35 tar ofta mer an tre minuter, darav sex.
set /a TRIES=0
:wait
set "BOOT="
for /f "tokens=*" %%B in ('"%ADB%" shell getprop sys.boot_completed 2^>nul') do set "BOOT=%%B"
echo !BOOT! | findstr /c:"1" >nul
if not errorlevel 1 goto booted
set /a TRIES+=1
if !TRIES! GEQ 180 (
  echo FEL: emulatorn blev inte klar inom sex minuter.
  echo Stang emulatorfonstret och kor: starta.bat nollstall
  echo Hjalper inte det, starta emulatorn fran Android Studio, Device Manager.
  exit /b 1
)
ping -n 3 127.0.0.1 >nul
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
