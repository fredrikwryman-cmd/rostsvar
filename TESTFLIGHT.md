# Tap & Talk till TestFlight utan Mac

Allt bygge sker på en macOS-byggare hos GitHub Actions. Du behöver ingen Mac.
Det här dokumentet listar exakt de steg som måste göras i Apples webbgränssnitt,
och vad som sedan sker av sig självt.

Bundle-ID som används:

| Del | Bundle-ID |
| --- | --- |
| Appen | `se.rostsvar.mobile` |
| iMessage-tillägget | `se.rostsvar.mobile.Messages` |

## Steg 1 · App Store Connect-nyckel (gör en gång)

1. Gå till [App Store Connect → Användare och åtkomst → Integrationer → App Store Connect API](https://appstoreconnect.apple.com/access/integrations/api).
2. Välj fliken **Team Keys** och tryck på plus.
3. Namn: `Tap and Talk CI`. Åtkomst: **App Manager**.
4. Tryck på **Generate**. Ladda ner `.p8`-filen direkt — den går bara att hämta en gång.
5. Anteckna **Key ID** (tio tecken) och **Issuer ID** (ett UUID högst upp på sidan).

## Steg 2 · Team ID

Gå till [Apple Developer → Membership details](https://developer.apple.com/account#MembershipDetailsCard)
och anteckna **Team ID** (tio tecken).

## Steg 3 · Appposten i App Store Connect

1. Gå till [App Store Connect → Appar](https://appstoreconnect.apple.com/apps) och tryck på plus → **Ny app**.
2. Plattform: **iOS**. Namn: `Tap & Talk`. Primärt språk: **Svenska**.
3. Bundle-ID: välj `se.rostsvar.mobile` i listan.
   *Står det inte i listan:* gå till
   [Certificates, Identifiers & Profiles → Identifiers](https://developer.apple.com/account/resources/identifiers/list),
   tryck på plus, välj **App IDs → App**, beskrivning `Tap and Talk`, Bundle ID
   **Explicit** `se.rostsvar.mobile`. Upprepa för `se.rostsvar.mobile.Messages`.
   Ingen extra capability behöver kryssas i. Gå sedan tillbaka till punkt 1.
4. SKU: `rostsvar`. Användaråtkomst: **Full åtkomst**.

## Steg 4 · Lägg in uppgifterna i GitHub

I repots **Settings → Secrets and variables → Actions**:

Under fliken **Secrets**, tryck på *New repository secret* tre gånger:

| Namn | Värde |
| --- | --- |
| `APP_STORE_CONNECT_KEY_ID` | Key ID från steg 1 |
| `APP_STORE_CONNECT_ISSUER_ID` | Issuer ID från steg 1 |
| `APP_STORE_CONNECT_PRIVATE_KEY` | Hela innehållet i `.p8`-filen, inklusive raderna `-----BEGIN PRIVATE KEY-----` och `-----END PRIVATE KEY-----` |

Under fliken **Variables**, tryck på *New repository variable*:

| Namn | Värde |
| --- | --- |
| `APPLE_TEAM_ID` | Team ID från steg 2 |

Nyckeln är en hemlighet. Den syns aldrig i loggen, skrivs bara till byggarens
egen disk under körningen och raderas i sista steget. Lägg aldrig `.p8`-filen
i repot — `.gitignore` stoppar den.

## Steg 5 · Kör uppladdningen

GitHub → **Actions** → *Skicka Tap & Talk till TestFlight* → **Run workflow**.
Ange ett byggnummer som är högre än förra uppladdningen (första gången `1`).

Arbetsflödet arkiverar, låter Xcode skapa certifikat och provisioneringsprofiler
själv, validerar paketet och laddar upp det. Bearbetningen i App Store Connect
tar oftast 5–20 minuter efter att körningen blivit grön.

## Steg 6 · Intern testning

1. [App Store Connect → Tap & Talk → TestFlight](https://appstoreconnect.apple.com/apps).
2. Under **Interna tester**, skapa gruppen `Fredrik` och lägg till dig själv.
3. När bygget är färdigbearbetat, lägg till det i gruppen.
4. Installera **TestFlight** från App Store på iPhone och öppna inbjudan.

Interna testare kräver ingen granskning från Apple. Exportefterlevnad är redan
besvarad i appens Info.plist (`ITSAppUsesNonExemptEncryption = false`), så den
frågan ska inte dyka upp.

## Steg 7 · Appens skärm, guiden och tangentbordet

Det här är huvudvägen, och därför det första som provas.

Först appens egen dikteringsskärm:

1. Öppna **Tap & Talk** och stäng guiden. Tryck på **Tala** och säg en mening
   med å, ä och ö. Kontrollera att texten fylls på medan du talar.
2. Tryck på **Klar**. Kontrollera att slutresultatet ersätter delresultaten och
   att den orangea mikrofonpricken i statusraden slocknar.
3. Rätta texten för hand. Tryck på **Tala om** och säg en mening till —
   kontrollera att den nya texten läggs **efter** den du rättade.
4. Tryck på **Skicka** och kontrollera att iOS delningsruta öppnas och att
   Messenger, Signal och Meddelanden går att välja. Skicka till dig själv.
5. Tryck på **Rensa** och kontrollera att fältet blir tomt.
6. Tryck på **Tala**, lämna appen medan mikrofonen är på, och gå tillbaka.
   Kontrollera att texten som hunnit höras står kvar och att beskedet säger att
   mikrofonen stängdes när du lämnade appen. **Inget som sagts får försvinna.**
7. Tryck på **Tala** och säg ingenting. Kontrollera att beskedet säger att
   inget hördes — och att det **inte** säger det om det redan står text i
   fältet från förra omgången.

Sedan guiden och tangentbordet:

1. Öppna **Tap & Talk**. Guiden ska visa sig av sig själv vid första start.
2. Steg 1, tryck på **Tillåt mikrofonen** och svara ja på båda frågorna
   (mikrofon och taligenkänning). Kontrollera att steget blir grönt.
3. Steg 2 och 3, tryck på **Öppna inställningarna**, lägg till tangentbordet
   och slå på **Tillåt fullständig åtkomst**. Gå tillbaka till appen och
   kontrollera att guiden ser likadan ut — de stegen är märkta *kontrollera
   själv*, och appen ska inte påstå att de är klara.
4. Steg 5, tryck i provfältet, håll in **🌐** och välj **Tap & Talk**. Tryck på
   **Tala**, säg en mening med å, ä och ö, och tryck på **Klar**.
5. Kontrollera att texten kom in i fältet medan du talade, att kvittot visar
   att texten kom in, och att å, ä och ö blev rätt.
6. Prova tangenterna: skift, backsteg, siffror och skiljetecken, **⌫**,
   mellanslag, retur och **⌨** tillbaka till föregående tangentbord.
7. Öppna **Messenger**, **Signal** och **Mejl**, byt till Tap & Talk och
   diktera i varje. Kontrollera att texten hamnar i rätt fält och att din
   egen redan skrivna text inte rörs.
8. Stäng av **Tillåt fullständig åtkomst** igen och kontrollera att
   tangentbordet säger det i klartext i stället för att tiga, och hänvisar
   till appen. Slå på den igen efteråt.

## Steg 8 · Genvägen i Meddelanden

Gör provet i en konversation med dig själv. Det här är tillägget, inte
huvudvägen: samma diktering, men bara i Meddelanden.

1. Öppna **Meddelanden**, välj konversationen, tryck på plustecknet vid
   skrivfältet och välj **Tap & Talk**.
2. Tryck på **Tala**, säg en mening med å, ä och ö.
3. Tryck på **Klar**. Kontrollera att den orangea mikrofonpricken i statusraden
   slocknar.
4. Läs utkastet. Tryck på **Avbryt** och kontrollera att utkastet försvinner och
   att inget hamnade i chatten.
5. Diktera igen och tryck på **Skicka**. Kontrollera att det blev **ett** enda
   meddelande.
6. Lämna Tap & Talk, öppna en annan konversation och öppna Tap & Talk där.
   Kontrollera att utkastfältet är tomt.

## Steg 9 · AssistiveTouch, att jämföra med

Tillägget i steg 8 fungerar **bara i Meddelanden**. iOS släpper inte in ett
iMessage-tillägg i andra chattappar. Det är tangentbordet i steg 7 som löser
Messenger och Signal, och det provas där. Upplägget nedan bygger i stället på
iOS egna delar: en genväg som dikterar och kopierar, kopplad till den flytande
AssistiveTouch-knappen. Ingenting av Tap & Talks egen kod är inblandad — det är
ett alternativ att jämföra tangentbordet med, inte appen vi bygger.

Ställ in en gång:

1. Öppna **Genvägar**, tryck på plus och döp genvägen till **Tap & Talk**.
2. Lägg till åtgärden **Diktera text**. Välj språk **Svenska** och låt
   dikteringen avslutas **efter en paus**.
3. Lägg till **Kopiera till urklipp** med den dikterade texten som indata.
   Visas valet **Lokalt urklipp**, slå på det — annars kan texten hoppa till
   dina andra Apple-enheter.
4. Kör genvägen en gång från Genvägar och godkänn åtkomsten den ber om.
5. Öppna **Inställningar → Hjälpmedel → Tryck → AssistiveTouch** och slå på
   knappen. Under **Dubbeltryck**, bläddra till genvägarna och välj
   **Tap & Talk**. Saknas den i listan, koppla den i stället till
   **Tryck på baksidan** under samma tryckinställningar.

Så här provar du, i **Messenger** och sedan i **Signal**:

1. Öppna en chatt.
2. Dubbeltryck på den flytande AssistiveTouch-knappen och säg din mening på
   svenska med å, ä och ö.
3. Håll fingret i chattens skrivfält och välj **Klistra in**.
4. Läs texten och tryck på **chattens egen skickaknapp**.

Kontrollera samma saker som i Meddelanden: att å, ä och ö blev rätt, att texten
hamnade i rätt fält, och att **ett** tryck på skickaknappen gav **ett**
meddelande. Kontrollera dessutom att inget klistras in av sig självt när du
byter chatt — urklippet ligger kvar tills du klistrar in det eller kopierar
något annat.

Två skillnader mot Meddelanden som är värda att notera i rapporten:

- Texten går via iOS urklipp, så den kan nås av andra appar. I tillägget i
  Meddelanden rör texten aldrig urklippet.
- Det finns ingen **Avbryt**. Vill du ångra dig låter du bli att klistra in.

## Om något fryser

Går uppladdningen fel laddas loggarna upp som artefakter på körningen
(*Tap-and-Talk-testflight-loggar*). Vanligaste orsakerna: byggnumret är inte högre
än förra, appposten i steg 3 saknas, eller nyckeln har fel roll.
