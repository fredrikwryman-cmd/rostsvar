# Tap & Talk · prototyp

Från Tech Rabbit. Tidigare arbetsnamn: Röstsvar.

Diktera ett svenskt textmeddelande och skicka det i konversationen du redan har öppen. Ett tryck på Tala startar mikrofonen. Du väljer själv när meddelandet ska skickas.

## Prova den flytande knappen på din iPhone redan nu

1. Öppna **Genvägar**, skapa en genväg och döp den till **Tap & Talk**.
2. Lägg till **Diktera text**, välj svenska och låt dikteringen avslutas efter en paus. Lägg därefter till **Kopiera till urklipp**, med den dikterade texten som indata. Välj enbart lokalt urklipp om alternativet visas. Kör genvägen en gång och godkänn efterfrågad åtkomst.
3. Öppna **Inställningar → Hjälpmedel → Tryck → AssistiveTouch**. Aktivera knappen. Under **Dubbeltryck**, bläddra till genvägarna och välj **Tap & Talk**. Om genvägen saknas i listan kan du koppla den till **Tryck på baksidan** under hjälpmedlens tryckinställningar.
4. Öppna en konversation i Messenger, Signal eller Meddelanden. Dubbeltryck på den flytande knappen och tala. Håll sedan fingret i skrivfältet, välj **Klistra in**, läs igenom och tryck på chattens **Skicka**.

Den här varianten kan ställas in direkt på telefonen. Knappen tillhör iOS. Urklippet ersätts när genvägen körs. Vill du diktera direkt i skrivfältet kan du även använda mikrofonen på Apples skärmtangentbord.

## Android · installera testappen

Testbygget heter `Tap-and-Talk-android-test.apk` och byggs med `build_debug.py`
(se **Bygg Android på Windows** nedan). Det är lokalt signerat för test och
kräver Android 11 eller senare samt en installerad taltjänst med stöd för
svenska. Hela testinstruktionen att skicka med filen står i
[TESTA-ANDROID.md](TESTA-ANDROID.md).

1. Öppna APK-filen på telefonen och installera. Android kan be dig godkänna installation från den app som öppnar filen.
2. Öppna **Tap & Talk**, tillåt **visning över appar** och **mikrofon**, och tryck på **Starta flytande knapp**. På vissa telefoner behöver du välja Tap & Talk i systemets applista.
3. För direkt infogning, välj **Aktivera tangentbord** och aktivera Tap & Talk. Gå tillbaka och välj det via **Välj tangentbord**. Det lilla rösttangentbordet har en knapp för att byta tillbaka.
4. Öppna en chatt, tryck i skrivfältet så att Tap & Talks tangentbord visas, tryck på den flytande **Tala**-knappen och diktera. Pausa eller tryck på **Klar**. Välj **Infoga i chatten**, granska utkastet i chatten och tryck på chattens skickaknapp.
5. Om du behåller ditt vanliga tangentbord använder du **Kopiera** och sedan **Klistra in** i chatten. Du kan också kopiera om skrivfältet har bytts under dikteringen.
6. Dra knappen till önskad plats. Stäng av den i Tap & Talk eller via appens avisering.

Android och den öppna appen styr var överlagringen får visas. Infoga visas endast för ett aktivt textfält i Tap & Talks tangentbord. Appen läser inga chattloggar och sparar ingen meddelandehistorik. Det du kopierar överlämnas till systemets urklipp. Telefonens taltjänst kan använda nätet.

## iMessage · inbyggt tillägg

Källkoden under **ios** innehåller en iPhone-app och ett tillägg för Meddelanden.
När tillägget är installerat öppnas det från iMessage-apparna i konversationen.

**Tala → Klar → läs → Skicka** använder svensk taligenkänning och skickar texten
med `MSConversation.sendText` efter ditt tryck. **Tala om** ersätter utkastet.
**Avbryt** stänger mikrofonen och rensar utkastet utan att något skickas.
Funktionen väljer igenkänning på enheten när den svenska igenkännaren stöder
det. Annars kan Apples taltjänst använda nätet.

Mikrofonen är igång bara mellan Tala och Klar. Den stängs också när du avbryter,
när tillägget fälls ihop till den smala listen, när du lämnar Tap & Talk och efter
55 sekunder. Utkastet rensas när du lämnar Tap & Talk och när konversationen byts.
Inget spelas in och ingen meddelandehistorik sparas.

### Bygg iOS

Bygget körs på en macOS-byggare hos GitHub Actions. Ingen Mac behövs lokalt.

- **Actions → Bygg Tap & Talk för iPhone (osignerat) → Run workflow.** Bygger app
  och tillägg osignerat för iPhone och lägger en osignerad IPA samt bygglogg som
  artefakter på körningen. Startas för hand och körs även vid push till `main`.
- **Actions → Skicka Tap & Talk till TestFlight → Run workflow.** Arkiverar
  signerat och laddar upp. Kräver tre hemligheter och en variabel i repot.
  Stegen står i [TESTFLIGHT.md](TESTFLIGHT.md).

Ikonerna skapas av `ios/Tools/make_icons.py` (Python med Pillow) och ligger
färdiga i repot. Kör skriptet igen bara om grafiken ska bytas.

Lokalt på en Mac med Xcode och XcodeGen, i mappen `ios`:

```sh
xcodegen generate
xcodebuild -project Rostsvar.xcodeproj -scheme Rostsvar -configuration Release   -sdk iphoneos -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO build
```

Xcode-projektet genereras ur `ios/project.yml` och ligger inte i repot.

### Bygg Android på Windows

Öppna mappen `android` i Android Studio och bygg appmålet, eller använd det medföljande byggskriptet. Skriptet behöver Python 3, JDK med javac och keytool i PATH, miljövariabeln ANDROID_HOME, Android SDK Platform 35 och Build Tools 36.0.0. Kör från `android`:

```powershell
python build_debug.py
```

Resultatet hamnar i `build/Tap-and-Talk-android-test.apk`. Byggskriptet skapar en lokal testnyckel vid första körningen. En lokalt ombyggd APK kan därför ha en annan signatur än den medföljande.

## Verifieringsstatus

Håll isär de tre stegen. En rad ändras först när steget faktiskt är gjort.

| Del | Kompilerat | Uppladdat till TestFlight | Provat på iPhone |
| --- | --- | --- | --- |
| iOS-app och iMessage-tillägg | ja | nej | nej |
| Android | ja | — | nej |

**iOS, uppmätt.** Osignerat Release-bygge för iPhone på macOS-byggare med
Xcode 16.4 och iOS 18.5 SDK. Noll varningar. Tillägget bäddas in i appen som
`PlugIns/RostsvarMessages.appex` med rätt bundle-ID och
`com.apple.message-payload-provider`. App- och iMessage-ikoner kompilerar utan
ozonerade platser. 16 tester gröna på iPhone-simulator.

**Android.** Koden har kompilerats och den paketerade APK-filens signatur har
verifierats.

**Ingenting är provat på en fysisk telefon.** Taligenkänning, mikrofonens
beteende i Meddelanden och att ett tryck på Skicka ger exakt ett meddelande går
bara att avgöra på enheten. Stöd i en viss version av Messenger, Signal eller
Meddelanden behöver också bekräftas där.

### Vad testerna mäter

`ios/Tests` provar reglerna i `DraftState`, samma typ som vykontrollern använder:

- mikrofonen är öppen bara under inspelning, aldrig medan behörighet frågas,
  efter Klar, efter Avbryt, när Tap & Talk lämnas eller vid konversationsbyte
- Avbryt rensar utkastet både under och efter diktering, och Skicka går då inte
  att trycka på
- ett byte av konversation rensar utkastet, även mitt i en inspelning
- ett andra tryck på Skicka kan inte ge ett andra meddelande
- ett misslyckat skick behåller utkastet; ett lyckat rensar det

Testerna rör inte mikrofonen, taligenkänningen eller Meddelanden. De mäter
reglerna, inte enheten.

### Första provet på telefonen

Gör det i en konversation med dig själv. Diktera en kort mening med å, ä och ö.
Kontrollera att Avbryt lämnar chatten orörd, att mikrofonpricken slocknar när du
trycker på Klar, att ett appbyte inte lägger text i ett annat fält automatiskt
och att ett enda tryck på Skicka ger ett enda meddelande. Hela provlistan står i
[TESTFLIGHT.md](TESTFLIGHT.md).

## Plattformskällor

- [Apples guide till AssistiveTouch](https://support.apple.com/sv-se/111794)
- [Koppla genvägar till den flytande knappen](https://tidbits.com/2022/07/22/use-assistivetouch-for-customizable-shortcuts-on-the-iphone-and-ipad/)
- [Apples guide till diktering](https://support.apple.com/sv-se/guide/iphone/iph2c0651d2/ios)
- [Skicka text i en iMessage-konversation](https://developer.apple.com/documentation/messages/msconversation/sendtext(_:completionhandler:))
- [Androids fönstertyper](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)
- [Androids tangentbordsgränssnitt](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method)
