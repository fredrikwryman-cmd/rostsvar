# App-profil

Tap&Talk (Rostsvar). Skriven 25 nov 2026 enligt rev-ux avsnitt 1, som
underlag för ombyggnaden i `rev-andringar.md`.

Arketyp: Röst/diktering
Huvudvärde: Tala-knappen
Primärt objekt: det dikterade utkastet
Primär handling: Skicka
Snabbhandlingar (max 4): Tala, Rensa, Skicka. Fler finns inte och fler ska
inte in: Guide och Om är knappar i huvudet, inte handlingar på objektet.
Händelseström: INGEN. Se beslut C i `rev-andringar.md`.
Tillstånd på objektet: Redo, Lyssnar, Tolkar, Klar. Samma fyra ord på båda
plattformarna, och de står i fältets fot.
Känsligt innehåll: transkriptet, alltså utkastet i fältet. Blurras i app
switchern på båda plattformarna.
Oåterkalleliga handlingar: inga. Rensa tar bort utkastet, men utkastet
existerar bara i minnet så länge skärmen är öppen, så det finns ingenting att
varna för som inte redan är borta vid nästa start.
Firaögonblick: inga. Appen används av någon som behöver den för att skriva,
flera gånger om dagen. Konfetti på rutin är ett irritationsmoment.
Ton: lugn/precis. Rörelsen går i `smooth`, `snappy` bara på tryck.

## Varför händelseströmmen inte finns

Beslut C: appen sparar ingen dikterad text, och det är avsiktligt. Det är
grunden för integritetspolicyn och Data Safety-svaren i direktiv 28, och en
historik skulle kräva lagring på telefonen. Medvetet avsteg från rev-ux
avsnitt 2.1 punkt 3 och 2.2.

Det som lagras är guidens visat-flagga (`guiden_visad`) och bubblans x och y
på Android. Inget annat, och `allowBackup` är av.

## Varför Tala-knappen är huvudvärdet

Arketypens huvudvärde är "inspelningsknapp/senaste text". Senaste text finns
inte här, av samma skäl som händelseströmmen inte finns. Kvar står knappen,
och den är redan störst på skärmen: 88 punkters skiva i en 132 punkters ram.

## Låsta beslut

| Beslut | Innebörd | Varifrån |
|---|---|---|
| Ett grönt | `#1CF038` till allt. `#1ED63C` införs inte. | Beslut B i `rev-andringar.md` |
| Kantfärg delad | `#707070` på fält och tangenter, där 3:1 gäller för komponentgränser. `rgba(255,255,255,0.08)` bara mellan listrader. | Beslut A, och direktiv 22 |
| Ingen lagring | Ingen dikterad text sparas. | Beslut C, direktiv 28 |
| Mörkt läge bara | Skärmarna är ritade för mörkt läge och tvingar `.dark`. | Tech Rabbit-underlaget §1 |
| Hörs något levereras det | Ett tomt slutresultat får aldrig skriva över text som hörts. | Direktiv 24, `Dictation.ending` |
| Enhetstolkning först | Tolkning på telefonen när den klarar det, annars taltjänsten. | Direktiv 29, `Speech.java` |
| Mikrofonen är en spärr | En yta i taget håller mikrofonen, process-vitt. | `Mic.java` |
| Archivo i fyra vikter | 400, 700, 800, 900. Bara skalan justeras. | Tech Rabbit-underlaget §3 |

## Skyddade tillgångar

| Tillgång | Fil | Används i dag | Får justeras |
|---|---|---|---|
| Appikonen, kanin plus tre staplar | `design/ikon-taptalk.png` | iOS appikon, iMessage-tillägget, Androids launcher och adaptiva ikon, Google Play 512 | Storlek och beskärning per plattform. Motivet rörs inte. |
| Ordmärket Tap&Talk med TECH RABBIT under | `ios/App/HomeScreen.swift` | Hemskärmens huvud | Storlek och placering. Texten ändras inte. |
| Kaninsymbolen som vektor | `TRIcon.rabbit` i `ios/App/TechRabbit.swift` | Ikonringen i huvudet | Storlek och färg. Formen är hämtad ur underlagets källfil och ritas inte om. |
| Bakgrunden: planet, det gröna bandet, glöden | `TRBackdrop` i `ios/App/TechRabbit.swift` | Alla appens skärmar | Inget. Ritad i 390 × 844 och skalad. |
| Egenritade ikoner: mikrofon, papperskorg, pappersplan | `TRIcon` i `ios/App/TechRabbit.swift` | Tala, Rensa, Skicka | Storlek och färg. Inget generiskt ikonpaket. |
| TechRabbit-paletten | `ios/Shared/Palette.swift`, `Look.java` | Allt | Beslut A och B gäller. I övrigt orörd. |
| Archivo 400/700/800/900 | `ios/App/Fonts/` | Appmålet | Behålls. Bara skalan. |

## Ytor profilen gäller

Fem ytor i två appar, samma profil på alla:

- iOS: hemskärmen, guiden, Om, tangentbordet, iMessage-tillägget.
- Android: hemskärmen, guiden, bubblan, tangentbordet, skrivtjänsten.

Tangentbordet och iMessage-tillägget har inget huvudvärde av egen kraft: de
visar samma utkast och samma fyra tillstånd, i mindre format.
