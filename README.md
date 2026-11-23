# Tap&Talk · prototyp

Från Tech Rabbit. Tidigare arbetsnamn: Röstsvar.

Diktera ett svenskt textmeddelande och skicka det i konversationen du redan har öppen. Ett tryck på Tala startar mikrofonen. Du väljer själv när meddelandet ska skickas.

## Prova den flytande knappen på din iPhone redan nu

Det här är ett upplägg av iOS egna delar, att jämföra med medan appen ännu inte
är installerad. Är appen installerad behövs det inte: Tap&Talk-tangentbordet
dikterar direkt i skrivfältet, utan urklipp och utan genvägar.

1. Öppna **Genvägar**, skapa en genväg och döp den till **Tap&Talk**.
2. Lägg till **Diktera text**, välj svenska och låt dikteringen avslutas efter en paus. Lägg därefter till **Kopiera till urklipp**, med den dikterade texten som indata. Välj enbart lokalt urklipp om alternativet visas. Kör genvägen en gång och godkänn efterfrågad åtkomst.
3. Öppna **Inställningar → Hjälpmedel → Tryck → AssistiveTouch**. Aktivera knappen. Under **Dubbeltryck**, bläddra till genvägarna och välj **Tap&Talk**. Om genvägen saknas i listan kan du koppla den till **Tryck på baksidan** under hjälpmedlens tryckinställningar.
4. Öppna en konversation i Messenger, Signal eller Meddelanden. Dubbeltryck på den flytande knappen och tala. Håll sedan fingret i skrivfältet, välj **Klistra in**, läs igenom och tryck på chattens **Skicka**.

Den här varianten kan ställas in direkt på telefonen. Knappen tillhör iOS. Urklippet ersätts när genvägen körs. Vill du diktera direkt i skrivfältet kan du även använda mikrofonen på Apples skärmtangentbord.

## Android · installera testappen

Vill du bygga och köra Android på den här datorn: `bygg.bat` och `starta.bat`,
beskrivna i [KOR-LOKALT.md](KOR-LOKALT.md).

Testbygget heter `TapnTalk-android-test.apk` och byggs med `build_debug.py`
(se **Bygg Android på Windows** nedan). Det är lokalt signerat för test och
kräver Android 11 eller senare samt en installerad taltjänst med stöd för
svenska. Hela testinstruktionen att skicka med filen står i
[TESTA-ANDROID.md](TESTA-ANDROID.md).

1. Öppna APK-filen på telefonen och installera. Android kan be dig godkänna installation från den app som öppnar filen.
2. Öppna **Tap&Talk**. Appens egen skärm är en stor **Tala**-knapp, ett textfält och **Skicka**.
3. Tryck på **Tala**. Första gången frågar Android om mikrofonen — välj **Tillåt**. Säger du nej står det på skärmen vad som fattas, och knappen leder till inställningarna.
4. Diktera på svenska. Texten fylls i fältet medan du talar. Tryck på **Klar** när du är färdig, eller låt taltjänsten själv sluta lyssna.
5. Läs igenom texten och rätta den för hand om du vill. Ett nytt tryck på **Tala om** lägger till mer tal efter den text som redan står där.
6. Tryck på **Skicka**. Androids egen delningsruta öppnas och du väljer själv vilken app texten ska till. Appen skickar aldrig något på egen hand.

Appen begär bara mikrofonen. Den har inget konto, ingen analys och når inget nät själv — men telefonens taltjänst, som gör om talet till text, kan använda internet. Inget spelas in och ingen text sparas; stänger du appen är fältet tomt nästa gång.

### Den flytande knappen

Appens egentliga idé: en liten grön knapp som ligger kvar **över alla appar**.
Ett tryck, du talar, och texten hamnar i det fält du står i. Knappen går att dra
dit du vill och fastnar vid närmaste kant. Du startar och stoppar den med
strömbrytaren högst upp på hemskärmen, eller med **Stäng** i dess egen notis.

Knappen körs som en förgrundstjänst, eftersom Android kräver det av något som
håller mikrofonen igång utanför sin egen skärm. Notisen som följer med ligger på
lägsta vikt, utan ljud och utan bricka.

Texten skrivs in av en tillgänglighetstjänst — det är enda vägen för en app att
skriva i en annan app på Android. Tjänsten frågar systemet vilket fält som har
skrivfokus och lägger texten efter den som redan står där. Den läser inget
annat, loggar ingenting, sparar ingenting och når inget nät. Hittas inget fält
hamnar texten i **urklipp** med en notis om att den kan klistras in, så att det
som sagts aldrig går förlorat.

### Rösttangentbordet

Reserven, för de fält bubblan inte kommer åt, och för den som hellre skriver än
dikterar. En vanlig svensk layout med å, ä och ö, siffror och skiljetecken, och
längst ned en bred grön **Tala**-knapp.

Appen slår inte på det åt dig — Android kräver att du gör det själv — men
**kom igång-guiden** öppnar rätt systemruta med ett tryck och läser av när
steget är gjort. Den visas vid första starten och nås sedan med **Guide**-knappen
uppe till höger på hemskärmen. Så länge något steg fattas står en varning kvar på
hemskärmen med en väg tillbaka till guiden.

I chatten: tryck i skrivfältet, tryck på **Tala** och diktera. Texten skrivs rakt
in i fältet medan du talar, som preliminär text, och blir vanlig text när
taltjänsten är klar. Rätta med tangenterna, tryck på **⌨** för att gå tillbaka
till ditt vanliga tangentbord, och skicka med chattens egen knapp.

Tangentbordet loggar ingenting och når inget nät. I lösenordsfält är dikteringen
avstängd; där fungerar bara tangenterna.

### Kom igång-guiden

Fem steg, ett i taget, var och en med skälet till att Android kräver det:

1. **mikrofontillståndet**, som frågas i appen eftersom varken ett tangentbord
   eller en tjänst får fråga själv,
2. **visa över andra appar**, som bubblan behöver för att alls få synas,
3. **tillgängligheten**, som bubblan behöver för att få skriva i fältet,
4. **tangentbordslistan** och
5. **tangentbordsväljaren**.

Varje steg bockas av när appen själv ser att det är gjort: tillståndet via
`checkSelfPermission`, bubblans rätt via `Settings.canDrawOverlays`,
tillgängligheten via `AccessibilityManager.getEnabledAccessibilityServiceList`,
tangentbordet via `getEnabledInputMethodList` och valet via
`Settings.Secure.DEFAULT_INPUT_METHOD`. Läget läses om varje gång appen kommer
fram igen, så guiden följer med utan att man trycker vidare. Sist ett provfält
där man dikterar och får kvittot att det fungerar. Så länge något steg fattas
står en varning kvar på hemskärmen, som namnger just det som fattas.

## iPhone · appens dikteringsskärm

Appens startskärm är en dikteringsskärm med samma uppbyggnad som Androids
`Home.java`: stor **Tala**-knapp som ett tryck startar och ett nytt tryck
avslutar, ett fält som fylls löpande medan man talar, texten redigerbar för hand
efteråt, **Skicka** som lämnar texten till iOS egna delningsruta
(`UIActivityViewController`), och **Rensa** som tömmer fältet. Guiden och Om
ligger som knappar högst upp.

Utseendet är Tech Rabbit-designen v1. Hela underlaget ligger i repot under
`design/techrabbit-taptalk-v1/`: direktivet med färgtokens, layout i punkter,
de tre lägena, animation, haptik och tillgänglighet, de två skärmbilderna och
källfilerna. Ritningarna — färger, typsnittsskala, bakgrund, ikoner och
ringarnas puls — ligger samlade i `ios/App/TechRabbit.swift`, så att
`HomeScreen.swift` bara handlar om layout och förlopp.

Skärmen är mörk och bara mörk, som underlaget. Svart `#0B0B0B`, grönt
`#1CF038` på Tala-knappen och det som är aktivt, rött `#E0583E` medan
mikrofonen är på. Knappen är en cirkel med två ringar som pulsar: långsamt i
vila, snabbare under inspelning. Fältets fot visar läget — Redo, Lyssnar…,
Tolkar…, Klar — och hur många tecken texten är. Med **Reducera rörelse**
påslaget står ringarna stilla, glöden slutar andas, markören slutar blinka och
lägesbytena tonar inte.

Knappen heter **Tala** när fältet är tomt, **Tala om** när det redan finns
text, **Klar** medan mikrofonen är på och **Tillåt mikrofon** när den är
nekad i Inställningar. Klar är ordet för att stänga mikrofonen på alla sex ytorna:
appen, tangentbordet, iMessage-tillägget, Androids hemskärm, bubblan och
rösttangentbordet. Det var **Stoppa** här fram till granskningen, medan de
fem andra sa Klar och Om-skärmen instruerade "Tryck på Klar".
Tala om är en medveten avvikelse från underlagets §6,
som säger Tala i båda lägena: ordet säger att nästa diktering läggs efter
texten som redan står där, och det är samma ord som Android använder.

Texten är satt i **Archivo**, typsnittet underlaget är ritat i. De fyra
vikterna ligger i `ios/App/Fonts/` och räknas upp i `UIAppFonts`:
`Archivo-Regular` (400) för brödtext, `Archivo-Bold` (700) för rubriker och
knappar, `Archivo-ExtraBold` (800) för etiketten under Tala-knappen och
`Archivo-Black` (900) för appnamnet. Namnen i koden är typsnittens egna
PostScript-namn, avlästa ur filernas name-tabeller. Storlekarna står i
underlagets punkter men skalas med Dynamic Type, en gång och inte två:
`TRFont` räknar om punkterna med `@ScaledMetric` och sätter sedan typsnittet
med `fixedSize:`, eftersom `Font.custom(_:size:)` annars hade skalat en
andra gång.

Guiden och Om är ritade i samma form: samma bakgrund, samma kort med radie
22 och 1,5 punkters kant, samma pillerknappar och samma typsnitt. De delar
`TRCard`, `TRButton`, `TRBadge` och `TRSheet` med startskärmen. Innehållet,
de fem stegen och allt som läses av är oförändrat; bara utseendet är bytt.
Två ställen använder fortfarande systemets typsnitt med flit: vägarna genom
Inställningar är satta i monospace så att leden går att följa med ögat, och
provfältet faller tillbaka på systemtypsnittet om Archivo inte skulle laddas.

Taligenkänningen är den delade `ios/Shared/Dictation.swift`, samma som
tangentbordet och iMessage-tillägget. Ingen egen kopia, så avslutsregeln i
`Dictation.ending` gäller också här: har något hörts levereras det, och ett tomt
slutresultat skriver aldrig över det.

Reglerna ligger i `ios/App/HomeState.swift`, skilda från skärmen just för att
gå att mäta utan telefon. Där bor också grunden: texten som stod i fältet när
dikteringen startade. Delresultaten ersätter varandra ovanpå den, så texten
växer medan man talar utan att stapla sig, och en text man rättat för hand är
den nästa diktering läggs efter. Skärmen skiljer dessutom "det står text i
fältet" från "något hördes just nu", så att en omgång som inte hörde något
aldrig rapporteras som lyckad.

Lämnas appen med mikrofonen öppen avslutas dikteringen i stället för att
avbrytas: det som hörts levereras och står kvar, och beskedet säger att
mikrofonen stängdes när appen lämnades.

## iPhone · tangentbordet

Tap&Talk är också ett tangentbord på iPhone, så dikteringen går att använda i
varje app där man skriver och inte bara i Meddelanden. Svensk layout med å, ä
och ö, siffror och skiljetecken i två uppsättningar, skift med lås, backsteg,
mellanslag, retur och en **⌨**-knapp tillbaka till föregående tangentbord.
Längst ned en bred grön **Tala**-knapp.

Mikrofonen kräver **Tillåt fullständig åtkomst** (`RequestsOpenAccess`). Ett
tangentbord får inte be om det själv, så saknas åtkomsten säger tangentbordet
det i klartext och hänvisar till appen, som visar vägen steg för steg.

Dikteringen skriver löpande medan man talar. Ett `textDocumentProxy` går bara
att skriva i och radera bakåt i, så tangentbordet håller reda på vad det självt
har skrivit: den gemensamma början av gamla och nya tolkningen behålls, resten
raderas tecken för tecken och det nya läggs till. Användarens egen text rörs
aldrig.

### Paket-id börjar på com., inte se.

Appen heter `com.techrabbit.tapntalk`, tangentbordet
`com.techrabbit.tapntalk.Keyboard` och iMessage-tillägget
`com.techrabbit.tapntalk.Messages`. Prefixet är inte en smakfråga. Ett
paket-id som börjar på `se.` gör att iOS inte visar tangentbordet
under *Lägg till nytt tangentbord*: tillägget installeras och signeras, men
listan hoppar tyst över det, och appen får inget besked. Kankoda (KeyboardKit)
mätte det i testappar där bara prefixet skilde — `com.`, `eu.`, `de.`, `da.` och
`dk.` visas, `se.` visas inte — och anmälde det till Apple som **FB13611131**,
ännu inte rättat. `se` är också språkkoden för nordsamiska, och samiska
tangentbord kom i iOS 17.2, där felet först dök upp.

Det här var orsaken till att tangentbordet inte gick att lägga till i version
1.0 (1). Android har inte felet, men paketnamnet flyttade med ändå, till
`com.techrabbit.tapntalk`: ett paketnamn går inte att byta efter publicering på
Google Play, och ingenting är publicerat än. Båda plattformarna heter därmed
samma sak. Priset togs nu, medan det är gratis.

### Kom igång-guiden på iPhone

Appen har samma femstegsguide som Android, i samma ordning: mikrofonen, lägg
till tangentbordet, **Tillåt fullständig åtkomst**, byt till Tap&Talk, och ett
provfält. Varje steg som görs i Inställningar visar hela vägen dit i klartext
(`Inställningar → Allmänt → Tangentbord → Tangentbord → …`) och har en knapp som
öppnar Inställningar. Guiden visas vid första start och öppnas sedan från
hemskärmen.

Skillnaden mot Android är vad appen får veta. Android kan läsa av alla fem
stegen; iOS kan bara läsa av mikrofonen och taligenkänningen, genom samma
`Dictation` som tilläggen använder. Om tangentbordet är tillagt, har fullständig
åtkomst eller är framme just nu säger iOS ingenting till appen. De fyra stegen
är därför märkta **kontrollera själv** och räknas aldrig som klara — ett
påstående appen inte kan backa upp hör inte dit. Mikrofonsteget läses om vid
`onAppear` och vid `UIApplication.didBecomeActiveNotification`, eftersom
användaren kommer tillbaka från Inställningar utan att appen får något resultat.

Provfältet är ett `UITextView`, inte ett `TextField`, av ett enda skäl: det kan
läsa `textInputMode` och rapportera vilket språk tangentbordet uppger. Vilket
tangentbord som skrev säger iOS inte, så kvittot redovisar det som går att mäta
— att texten kom in, och språket — och säger sedan exakt vad användaren själv
ska titta efter: den svarta raden med den gröna **Tala**-knappen.

## iMessage · genvägen i Meddelanden

Källkoden under **ios** innehåller en iPhone-app, ett tangentbord och ett tillägg
för Meddelanden. Tangentbordet är huvudvägen och fungerar i varje app;
iMessage-tillägget är en genväg i Meddelanden och pekas inte ut som förstaval,
varken i appen eller i tillägget, som bär en rad om var huvudvägen går. När
tillägget är installerat öppnas det från iMessage-apparna i konversationen.

**Tala → Klar → läs → Skicka** använder svensk taligenkänning och skickar texten
med `MSConversation.sendText` efter ditt tryck. **Tala om** ersätter utkastet.
**Avbryt** stänger mikrofonen och rensar utkastet utan att något skickas.
Funktionen väljer igenkänning på enheten när den svenska igenkännaren stöder
det. Annars kan Apples taltjänst använda nätet.

Mikrofonen är igång bara mellan Tala och Klar. Den stängs också när du avbryter,
när tillägget fälls ihop till den smala listen, när du lämnar Tap&Talk och efter
55 sekunder. Utkastet rensas när du lämnar Tap&Talk och när konversationen byts.
Inget spelas in och ingen meddelandehistorik sparas.

**Texten kastas inte när panelen fälls ihop.** Utkastet rensas fortfarande — ingen
text ska ligga kvar i någons meddelandepanel — men det som hörts läggs först i
urklipp, och nästa gång du öppnar Tap&Talk står det att texten finns där:
*"Texten ligger i urklipp — håll in i fältet och välj Klistra in."* Samma mönster
som bubblan på Android använder när texten inte kan skrivas i ett fält. Urklippet
är lokalt, så texten hoppar inte vidare till dina andra Apple-enheter.

Två fall räddas med flit inte. **Avbryt** är ditt eget beslut att kasta texten,
och då ska den inte ligga kvar i urklipp bakom din rygg. Och under en pågående
skickning är texten redan överlämnad till Meddelanden, så urklippet hade bara
varit en kopia av det som är på väg.

### Bygg iOS

Bygget körs på en macOS-byggare hos GitHub Actions. Ingen Mac behövs lokalt.

- **Actions → Bygg Tap&Talk för iPhone (osignerat) → Run workflow.** Bygger app
  och tillägg osignerat för iPhone och lägger en osignerad IPA samt bygglogg som
  artefakter på körningen. Startas för hand och körs även vid push till `main`.
- **Actions → Skicka Tap&Talk till TestFlight → Run workflow.** Arkiverar
  signerat och laddar upp. Kräver tre hemligheter och en variabel i repot.
  Stegen står i [TESTFLIGHT.md](TESTFLIGHT.md).
- **Actions → Bygg Tap&Talk som Ad Hoc (utan granskning) → Run workflow.**
  Vägen förbi väntan på Apples Beta App Review. Tar byggnummer, UDID och namn
  som indata, registrerar telefonen hos Apple om den saknas, arkiverar och
  exporterar ad hoc-signerat, och lägger paketet som artefakten
  `TapnTalk-adhoc-IPA`. Laddar aldrig upp något till App Store Connect.
  Enheten registreras av `ios/Tools/registrera_enhet.py` mot App Store
  Connect-API:t, med samma hemligheter som TestFlight-flödet. Nyckeln behöver
  rollen App Manager eller Admin för att få registrera enheter.

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

Resultatet hamnar i `build/TapnTalk-android-test.apk`. Byggskriptet skapar en lokal testnyckel vid första körningen. En lokalt ombyggd APK kan därför ha en annan signatur än den medföljande.

Appikonen skapas av `android/tools/make_icons.py` (Python med Pillow) och ligger färdig i repot. Den ritar samma kaninmikrofon som iOS-ikonen, men i kvadratisk variant och i Androids lager: en bakgrund, en symbol och en helbild per täthet. Kör skriptet igen bara om grafiken ska bytas.

**Actions → Bygg Tap&Talk för Android (test-APK) → Run workflow** gör samma bygge på en Linux-byggare och lägger APK:n som artefakt. Det bygget använder Gradle och Androids vanliga felsökningsnyckel.

## Verifieringsstatus

Håll isär de tre stegen. En rad ändras först när steget faktiskt är gjort.

| Del | Kompilerat | Uppladdat till TestFlight | Provat på iPhone |
| --- | --- | --- | --- |
| iOS-app och iMessage-tillägg | nej, ändrade sedan senaste bygget | nej | nej |
| iOS-tangentbord | ja, men paket-id ändrat sedan dess | nej | ja, visades inte i listan |
| iOS kom igång-guide | nej | nej | nej |
| iOS dikteringsskärm | nej | nej | nej |
| iOS Tech Rabbit-design | nej | nej | nej |
| Android | ja | — | nej |

**iOS, uppmätt.** Osignerat Release-bygge för iPhone på macOS-byggare med
Xcode 16.4 och iOS 18.5 SDK. Noll varningar. Tillägget bäddas in i appen som
`PlugIns/RostsvarMessages.appex` med rätt bundle-ID och
`com.apple.message-payload-provider`. App- och iMessage-ikoner kompilerar utan
ozonerade platser. 16 tester gröna på iPhone-simulator.

**iOS, ännu inte mätt.** Tangentbordstillägget `com.techrabbit.tapntalk.Keyboard`,
den delade dikteringen i `ios/Shared/Dictation.swift`, de sex testerna i
`DictationEndingTests`, kom igång-guiden i `ios/App/Onboarding.swift`,
dikteringsskärmen i `ios/App/HomeScreen.swift` med `HomeState` och dess sexton
tester, och Tech Rabbit-designen i `ios/App/TechRabbit.swift` är skrivna men
**inte kompilerade** — det kräver en Mac eller en körning av iOS-flödet, och
ingen körning har startats. Siffrorna ovan gäller bygget före de ändringarna.

Till det hör nu också hela premiumgranskningens punkt 1 till 14: den delade
paletten i `ios/Shared/Palette.swift`, beskeden i `ios/Shared/Notice.swift` och
`Notices.java`, statusraden i Androids rösttangentbord, rullningen på
startskärmen, Rensa på Android, den tryckbara steglistan i guiden, det
redigerbara iMessage-utkastet och `NoticeTests`. Ingenting av det är
kompilerat, och ingen rad av det är provad på telefon.

Designen är dessutom av sin natur svår att mäta utan telefon. Pulsens takt,
glöden, den blinkande markören, haptiken, hur bakgrundens kurvor skärs av på en
smalare skärm och om tangentbordet lämnar Tala-knappen i fred — allt det syns
först på en riktig iPhone. Till det hör nu också om Archivo verkligen laddas:
att `UIAppFonts` stämmer och att de fyra PostScript-namnen träffar rätt syns
bara när appen kör. Gör de inte det byter SwiftUI tyst till systemtypsnittet,
så skärmen ser rätt ut i formen men fel i bokstäverna.

**iOS, uppmätt på telefon.** Version 1.0 (1) installerades som ad hoc-bygge på
en iPhone. Alla tre paketen fanns med och var signerade, men tangentbordet gick
inte att lägga till i `Inställningar → Allmänt → Tangentbord → Tangentbord →
Lägg till nytt tangentbord`. Orsaken var paket-id:ts `se.`-prefix, se *Paket-id
börjar på com., inte se.* ovan. Prefixet är rättat i `ios/project.yml` och
`MARKETING_VERSION` är höjd till 1.1, men rättningen är inte provad på telefon:
det kräver ett nytt bygge, nya App ID:n hos Apple och en ny installation.

`ios/App/Look.swift` är kvar men används inte av någon fil: dikteringsskärmen
var dess enda användare, och guiden och Om ritas nu med Tech Rabbit-tokenen
i `TechRabbit.swift`.

**Android, uppmätt vid versionCode 6.** Lokalt bygge med Build Tools 36.0.0 mot
Platform 35, utan fel. Sedan dess är versionen höjd till **7 / 0.7** utan nytt
bygge, så siffrorna nedan gäller paketet med versionCode 6.
`TapnTalk-android-test.apk` är 158 109 byte, versionCode 6, med en
`classes.dex` på 54 544 byte. Paketet begär `RECORD_AUDIO`, `SYSTEM_ALERT_WINDOW`,
`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE` och `POST_NOTIFICATIONS` —
inget nät, ingen lagring, ingen plats. Det har en startbar aktivitet,
`Home`, kom igång-guiden `Guide`, bubblans förgrundstjänst `Bubble` med
`foregroundServiceType` mikrofon, skrivtjänsten `Writer` bunden med
`BIND_ACCESSIBILITY_SERVICE` och beskriven av `res/xml/writer.xml`,
rösttangentbordet `VoiceKeys` bundet med `BIND_INPUT_METHOD` och beskrivet av
`res/xml/method.xml`, och en lagerikon i fem tätheter. Signaturen verifierad med
apksigner (v3).

Den mätningen är äldre än paketbytet. Den gjordes på versionCode 6, när paketet
hade `se.`-prefix; klasserna låg då under det paketnamnet och heter i paketet nu
`com.techrabbit.tapntalk.Home` och så vidare. versionCode står på 10 och
visningsnamnet är `Tap&Talk`. Ingen ny APK är byggd, så siffrorna ovan gäller
inte det nuvarande bygget.

**Ingenting är provat på en fysisk telefon.** Taligenkänning, mikrofonens
beteende i Meddelanden och att ett tryck på Skicka ger exakt ett meddelande går
bara att avgöra på enheten. Stöd i en viss version av Messenger, Signal eller
Meddelanden behöver också bekräftas där.

### Vad testerna mäter

`ios/Tests` provar reglerna i `DraftState` och i `Dictation`, samma typer som
vykontrollern och tangentbordet använder:

- mikrofonen är öppen bara under inspelning, aldrig medan behörighet frågas,
  efter Klar, efter Avbryt, när Tap&Talk lämnas eller vid konversationsbyte
- Avbryt rensar utkastet både under och efter diktering, och Skicka går då inte
  att trycka på
- ett byte av konversation rensar utkastet, även mitt i en inspelning
- ett andra tryck på Skicka kan inte ge ett andra meddelande
- ett misslyckat skick behåller utkastet; ett lyckat rensar det

`DraftRescueTests` mäter urklippsräddningen, elva stycken:

- hörd text räddas när panelen fälls ihop, också mitt i en inspelning
- räddningen nollar ändå utkastet, och texten som räddas är putsad
- tomt utkast och bara blanktecken räddar ingenting
- ingenting räddas under en pågående skickning, och skickningen avbryts inte
- Avbryt kastar texten med flit: den hamnar aldrig i urklipp bakom ryggen på
  den som ångrade sig
- räddningen sker en gång, och att läsa texten ändrar inte utkastet

`HomeStateTests` mäter dikteringsskärmens egna regler, sexton stycken:

- delresultaten ersätter varandra och staplas aldrig
- det som dikteras läggs efter texten som redan stod i fältet
- en tom tolkning raderar aldrig det som hörts
- en text användaren rättat för hand är den nästa diktering läggs efter
- mikrofonen är öppen bara under inspelning, aldrig medan behörighet frågas och
  aldrig medan taltjänsten slutför
- Skicka och Rensa väntar tills mikrofonen är stängd, och fältet går inte att
  skriva i medan den är på
- "det står text i fältet" skiljs från "något hördes just nu", så att en omgång
  som inte hörde något aldrig rapporteras som lyckad
- Rensa glömmer också grunden, så nästa diktering börjar om från tomt

`ProxyWritingTests` mäter gränsen för när tangentbordet får radera i ett annat
programs fält, tretton stycken. Backsteget är den farliga delen: tangentbordet
byter ut en tolkning mot nästa genom att radera bakåt från markören, och ligger
inte dess egen text där längre raderar det användarens.

- vår egen text närmast före markören får bytas ut
- flyttad markör: tangentbordet äger ingenting och raderar ingenting
- markerad text: likaså, eftersom backsteget hade ätit markeringen
- lämnar programmet ingen text ifrån sig gäller tangentbordets egen bokföring
- en växande tolkning raderar ingenting, en ändrad svans bara svansen
- å, ä och ö räknas som ett tecken var

`DictationEndingTests` mäter rättningen av buggen där texten försvann:

- ett slutresultat levererar texten
- ett **fel** efter att något hörts behåller texten, och beskedet får inte säga
  att inget uppfattades
- uteblivet svar från taltjänsten levererar ändå det som hörts
- bara när ingenting hörts rapporteras skälet

Testerna rör inte mikrofonen, taligenkänningen eller Meddelanden. De mäter
reglerna, inte enheten.

### Mikrofonen delas av tre ytor

Bubblan, rösttangentbordet och hemskärmen ligger i samma process på Android och
vet inget om varandra. Startar två av dem taligenkänningen samtidigt svarar
Android med upptaget på den andra. `Mic` är en spärr som gör att den som är sist
ute får veta det i förväg i stället: *"Mikrofonen används av Tap&Talk någon
annanstans."* Spärren går att ta över när den är över 70 sekunder gammal, så att
en yta som dödas av systemet inte kan låsa mikrofonen för resten av processens
liv — gränsen ligger över dikteringens egen längsta omgång.

Alla tre ytorna håller dessutom omgången öppen efter att mikrofonen stängts,
eftersom taltjänsten skriver sin sista tolkning efter att talet tagit slut. Ett
nytt tryck under tiden svarar *"Texten tolkas. Vänta ett ögonblick."* i stället
för att starta en andra omgång som den gamla meningen hade skrivit sig in i.
Svarar taltjänsten inte alls går en gräns på åtta sekunder: det som hunnit höras
levereras ändå. På iPhone gör `Dictation` samma sak med fem sekunder, och
avslutar dessutom dikteringen när ett samtal eller en annan app tar mikrofonen,
så att texten inte ligger och väntar på en session som redan är riven.

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
- [Androids taligenkänning, SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer)
- [Extrafälten till RecognizerIntent, bland annat språkval](https://developer.android.com/reference/android/speech/RecognizerIntent)
- [Att skicka enkelt innehåll vidare med ACTION_SEND](https://developer.android.com/training/sharing/send)
