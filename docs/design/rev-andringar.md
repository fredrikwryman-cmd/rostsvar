# Ändringslista: Tap&Talk i Revolut-tappning

Status: GODKÄND 25 nov 2026. Punkt 1-22 gäller. Besluten A, B och C avgjorda nedan.
Skapad 25 nov 2026. Skills: rev-ux (beteende) + rev-app-dev (utseende).
Granskad kod: ios/App, ios/Keyboard, ios/Messages, ios/Shared, android/app/src/main/java/com/techrabbit/tapntalk.

## Utgångsläge

Appen har redan ett eget designsystem: TechRabbit-paletten (12 färger i
`ios/Shared/Palette.swift` och `android/.../Look.java`), Archivo i fyra vikter,
mörk bakgrund, platta ytor. Det är närmare rev-app-dev än de flesta appar
skulle vara. Ombyggnaden är därför mest justering, inte omritning.

Två strukturella fakta som styr hela listan:

1. **Androidsidan är native Java med vanliga vyer.** All kod i rev-ux avsnitt 8
   är Expo och React Native och går inte att använda. Androidsidan får
   principerna, men varje animation måste skrivas för hand i ObjectAnimator,
   ValueAnimator och MotionLayout. Räkna med att Android tar ungefär dubbla
   tiden mot iOS per punkt.
2. **Chattdelen i rev-ux (avsnitt 5) gäller inte den här appen.** Tap&Talk har
   ingen tråd. Punkterna som ändå bär över är innehållskortets struktur
   (märke, huvudinnehåll, tid) och att skrivfältets ikon byter när det finns
   text.

## Skyddade tillgångar

| Tillgång | Fil | Används i dag | Får justeras |
|---|---|---|---|
| Appikonen, kanin plus tre staplar | `design/ikon-taptalk.png` | iOS appikon, iMessage-tillägget, Androids launcher och adaptiva ikon, Google Play 512 | Storlek och beskärning per plattform. Motivet rörs inte. |
| Ordmärket Tap&Talk med TECH RABBIT under | `ios/App/HomeScreen.swift` rad 132 och 137 | Hemskärmens huvud | Får storlek och placering enligt P3. Texten ändras inte. |
| TechRabbit-paletten | `ios/Shared/Palette.swift`, `Look.java` | Allt | Se konflikten under Öppna beslut. |
| Archivo 400/700/800/900 | `ios/App/Fonts/` | Allt | Behålls. Bara skalan justeras. |

## Ändringar per skärm

| Skärm | I dag | Ny version | Skill och avsnitt | Prioritet |
|---|---|---|---|---|
| Alla | Ingen app-profil finns | `docs/design/app-profil.md` skrivs. Arketyp: Röst/diktering. Huvudvärde: Tala-knappen. Primärt objekt: det dikterade utkastet. Primär handling: Skicka. | rev-ux 1 | P1 |
| Alla, iOS | Fyra haptikfunktioner inbakade i `HomeScreen.swift` rad 648-651, ingen avstängning | Egen fil `ios/Shared/Haptics.swift` med hela kartan, 80 ms-spärr och av-knapp i inställningar | rev-ux 7 | P2 |
| Alla, Android | Ingen haptik alls | Samma karta via `performHapticFeedback` och `VibrationEffect`, egen fil, samma av-knapp | rev-ux 7 | P2 |
| Alla | Inget reducerat rörelseläge | Förflyttning, zoom och puls ersätts med crossfade 160 ms när Reduce Motion är på. Haptiken ligger kvar. | rev-ux 2.5 | P2 |
| Alla | Appen syns i app switchern | Blur-lager vid `inactive` och `background`. Transkript räknas som känsligt enligt profiltabellen. | rev-ux 2.4 | P2 |
| Alla | Hårdkodade mått i varje vy | Mått och radier flyttas till temat tillsammans med färgerna | rev-app-dev 4 | P3 |
| Hemskärm, iOS | Huvud 56 pt högt, rullar med innehållet | Huvudet kollapsar med scrollen: ordmärket krymper och tonar, liten version tonar in i toppfältet vid 60 procent | rev-ux 4.3 | P1 |
| Hemskärm, iOS | Toppfältet har ingen avgränsning | Glas och avdelare tonar in först när innehåll ligger under, scroll över 4 pt | rev-ux 4.3 | P3 |
| Hemskärm, båda | Tala-knappen 88 pt i en 132 pt ram, statisk ring | Knappen är huvudvärdet enligt profilen: puls i vila dämpas, tryck ger scale 0.97 med `snappy`, inspelning start ger `medium`, stopp ger `light` | rev-ux 4.2 A12, 7 | P1 |
| Hemskärm, båda | Rensa och Skicka ligger i en rad under fältet | Oförändrad funktion. Skicka blir enda gröna primärknappen på skärmen, 52 pt hög pill. Rensa blir sekundär utan grön kant. | rev-app-dev 3.1, 3.3 | P3 |
| Hemskärm, båda | Teckenräknaren står till höger i statusraden | Oförändrad, men siffran rullar en siffra i taget i stället för att bytas | rev-ux 4.2 A10 | P2 |
| Hemskärm, båda | Textfältet har alltid samma ikonrad | Ikonen till höger byter till Skicka-pil i accentfärg när fältet har text, crossfade plus scale 0.6 till 1 på 160 ms | rev-ux 5.4 | P2 |
| Hemskärm, båda | Delning öppnar systemets delningsark direkt | Oförändrad funktion. Arket får sidan bakom att sjunka till 0.94 och mörkna. | rev-ux 4.1 | P2 |
| Hemskärm, båda | Ingen laddnings- eller felyta, bara meddelanden ur Notice | Oförändrade texter. Tomt läge får en rad som förklarar värdet, fel får vad hände, vad det betyder och vad man gör nu. | rev-ux 2.2 | P2 |
| Guiden, båda | Fem steg, varje steg byter innehåll på plats | Steget som lämnas glider ut åt vänster, nästa in från höger. Ordning in: rubrik, innehåll med 35 ms stagger, knapp sist. | rev-ux 4.1 | P2 |
| Guiden, båda | Checkarna uppdateras tyst när man kommer tillbaka | Checken som slår om får `success` och en bock som växer fram | rev-ux 7 | P2 |
| Guiden, iOS | Pekar fel på några ställen i Inställningar | Rättas när Fredrik kört igenom appen och sagt var det skiljer | utanför skillarna | P1 |
| Tangentbordet, iOS | Statusrad plus tangenter, egen layout | Oförändrad funktion och layout. Tangenttryck får `selection`. Statusraden byter innehåll med crossfade 160 ms i stället för att hoppa. | rev-ux 7, 4.2 A8 | P2 |
| iMessage-tillägget, iOS | Utkastet visas och skickas | Oförändrad funktion. Utkastet blir ett innehållskort: märke, texten, tid. | rev-ux 5.3 | P3 |
| Bubblan, Android | Tre lägen: vila, lyssnar, tolkar. Byter färg på plats. | Lägesbytet blir en yteffekt som växer över knappen, `medium` vid byte. Ingen navigering. | rev-ux 4.2 A8 | P2 |
| Bubblan, Android | Går att dra, stannar där man släpper | Fortsätter med fysik efter släpp och snäpper till närmaste kant, `soft` när den låser | rev-ux 3, 4.2 A13 | P2 |

## Avgjorda beslut

**A. Kantfärg: delad lösning.** `#707070` behålls på fältets och tangenternas
kanter, där kravet 3:1 för komponentgränser gäller och där direktiv 22 höjde
den med flit. Avdelaren `rgba(255,255,255,0.08)` används bara mellan listrader,
där inget sådant krav finns.

**B. Ett grönt.** `#1CF038` används till allt, som i dag. Skillens uppdelning i
`#1ED63C` som allmän accent införs inte.

**C. Händelseströmmen stryks.** Appen sparar ingen dikterad text, och det är
avsiktligt: det är grunden för integritetspolicyn och Data Safety-svaren i
direktiv 28. En historik skulle kräva lagring på telefonen och riva den
positionen. Hemskärmen får ingen händelseström. Medvetet avsteg från rev-ux
avsnitt 2.1 punkt 3 och 2.2, med integriteten som skäl.

## Ordning när listan är godkänd

1. App-profilen.
2. Tokens och temafil, båda plattformarna.
3. Gemensamma delar: haptikfilen, reducerad rörelse, app switcher-blur.
4. Hemskärmen på iOS, sedan Android.
5. Guiden, tangentbordet, bubblan, iMessage-tillägget.

Varje skärm ska fungera exakt likadant efteråt som före.
