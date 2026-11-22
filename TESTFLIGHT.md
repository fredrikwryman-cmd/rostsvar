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

## Steg 7 · Första provet på iPhone

Gör provet i en konversation med dig själv.

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

## Om något fryser

Går uppladdningen fel laddas loggarna upp som artefakter på körningen
(*Tap-and-Talk-testflight-loggar*). Vanligaste orsakerna: byggnumret är inte högre
än förra, appposten i steg 3 saknas, eller nyckeln har fel roll.
