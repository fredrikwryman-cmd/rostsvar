# Kör Tap&Talk för Android på den här datorn

Allt sker lokalt. Inget moln, inga krediter, ingen GitHub Actions-körning.

## De två kommandona

Öppna **cmd** i projektmappen.

```
bygg.bat
```

Bygger debug-APK:n och skriver ut sökvägen till den färdiga filen. Tar under en
minut. Resultatet hamnar i `android\build\TapnTalk-android-test.apk`.

```
starta.bat
```

Skapar emulatorn första gången, startar den, väntar på att Android blir klart,
installerar den senast byggda APK:n och startar appen.

Kör `bygg.bat` igen efter varje kodändring, sedan `starta.bat` för att lägga
den nya APK:n på emulatorn. Emulatorn behöver inte startas om.

## Vad som används

| Del | Vad |
| --- | --- |
| JDK | Android Studios egen (`jbr`), annars den i PATH |
| SDK | `%LOCALAPPDATA%\Android\Sdk` |
| Bygget | `android\build_debug.py` — aapt2, javac, d8, zipalign, apksigner |
| Emulator | AVD `TapnTalk_API35`, Android 15, google_apis, x86_64 |
| Paket-ID | `com.techrabbit.tapntalk` |

Bygget går **inte** genom Gradle. Det finns ingen wrapper i repot, och
`build_debug.py` anropar verktygen direkt — därför laddas ingenting ner vid
varje körning.

## När något går fel

**"hittar inte Android SDK med platforms;android-35"**
`ANDROID_HOME` pekar fel. Sätt den till din SDK-mapp, eller hämta plattformen:

```
"%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --install "platforms;android-35"
```

**"hittar ingen javac"**
Installera en JDK 17 eller senare, eller sätt `JAVA_HOME` till en som finns.
Android Studios egen ligger i `%ProgramFiles%\Android\Android Studio\jbr`.

**"systembilden saknas"**
Hämta den:

```
"%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --install "system-images;android-35;google_apis;x86_64"
```

**Emulatorn blir inte klar inom tre minuter**
Stäng emulatorfönstret och kör `starta.bat` igen. Går det inte andra gången:
starta emulatorn från Android Studio (Device Manager) och kör sedan
`starta.bat`, som då bara installerar.

**Emulatorn startar långsamt eller inte alls**
Hårdvaruacceleration saknas. Slå på **Virtual Machine Platform** och
**Windows Hypervisor Platform** under *Slå på eller av Windows-funktioner*,
och starta om datorn.

**Installationen misslyckas med `INSTALL_FAILED_UPDATE_INCOMPATIBLE`**
En äldre APK med annan signatur ligger kvar. `starta.bat` avinstallerar och
provar igen av sig själv. Gör det för hand om det behövs:

```
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" uninstall com.techrabbit.tapntalk
```

**Appen kraschar eller beter sig fel**
Felkoderna från taltjänsten loggas under taggen `TapnTalk`:

```
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" logcat -s TapnTalk
```

## Det emulatorn inte kan visa

Mikrofonen, taligenkänningen och bubblan över andra appar beter sig inte som på
en telefon. Emulatorn har ingen svensk taltjänst installerad som standard, så
dikteringen svarar troligen `Taltjänsten har inte svenska installerad`. Layout,
färger, kontrast, tangentbordet och guidens flöde går att se; själva
dikteringen gör det inte.
