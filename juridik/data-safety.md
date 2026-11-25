# Google Play Data Safety — svaren för Tap&Talk

Underlag för formuläret under Policy → App content → Data safety i Play
Console. Varje svar nedan följer av koden. Punkter märkta **[FREDRIK]** kan
koden inte avgöra.

Appens versionsnamn när det här skrevs: 0.10, versionCode 10.

Sedan direktiv 29 begär appen tolkning på enheten först, på båda
plattformarna. Går det lämnar inget ljud telefonen. Den vanliga taltjänsten
används bara när telefonen saknar enhetstolkning, eller när den visar sig
inte klara svenska. Svaren nedan utgår från att reservvägen finns och
används på en del telefoner, för det gör den.

## 1. Data collection and security

**Does your app collect or share any of the required user data types?**
→ **No**, under förutsättning att Play räknar Androids systemtaligenkänning
som en tjänst på enheten och inte som appens egen insamling.
**[FREDRIK]** Det här är hela avgörandet i formuläret. Appen skickar inget
själv, och frågar efter enhetstolkning först, men reservvägen lämnar ljudet
till systemets `SpeechRecognizer`, som på de flesta telefoner är Googles och
kan tolka i molnet. Play har inget uttryckligt svar för det fallet. Två
vägar:

- Svara **No**, och motivera i policyn att behandlingen sker hos
  operativsystemets taltjänst under Googles eget ansvar.
- Svara **Yes** och deklarera **Audio → Voice or sound recordings** samt
  **Personal info → Other** (den dikterade texten) som *Collected: No,
  Shared: Yes*, ändamål *App functionality*, *Optional: No*.

Det säkra valet mot granskning är det andra. Väljs det gäller punkterna 2–5
nedan; väljs det första hoppar formuläret över dem.

**Is all of the user data collected by your app encrypted in transit?**
→ **Yes**, om något alls deklareras. Appen har ingen egen transport, och den
som sker går via Googles eller Apples egna krypterade anrop.

**Do you provide a way for users to request that their data is deleted?**
→ **No**. Det finns inget konto och ingen lagring hos oss att radera.
Avinstallation tar bort det lilla som ligger på telefonen.

## 2. Om Audio deklareras (bara vid svaret Yes ovan)

- Data type: **Voice or sound recordings**.
- Collected: **No**. Appen sparar inget ljud, varken i fil, minne mellan
  omgångar eller någon annanstans.
- Shared: **Yes** — ljudet lämnas vidare till telefonens taltjänst när
  enhetstolkning saknas. Finns enhetstolkning delas ingenting.
- Processed ephemerally: **Yes**.
- Required or optional: **Required** för dikteringen; appen har ingen annan
  funktion.
- Purpose: **App functionality** enbart. Inte analys, inte annonser, inte
  personalisering, inte kontohantering, inte bedrägeriskydd.
- Is this data collected for advertising or marketing? **No**.

## 3. Om den dikterade texten deklareras

- Data type: närmast **Personal info → Other** (fritext: "dikterad text").
- Collected: **No**. Texten går till textfältet och ingen annanstans.
- Shared: **Yes** om omgången gick på den molnbaserade reservvägen, eftersom
  texten kommer därifrån. Vid enhetstolkning delas ingenting.
- Processed ephemerally: **Yes**.
- Purpose: **App functionality**.
**[FREDRIK]** Play har ingen egen kategori för dikterad text. Om *Other* känns
fel passar **Messages → Other in-app messages** sämre men finns som
alternativ.

## 4. Data types som ska stå som nej, var och en

Ingen av dessa behandlas av appen, och inget i koden rör dem:

- Location (approximate, precise) — **No**
- Personal info: name, email, user IDs, address, phone number, race and
  ethnicity, political or religious beliefs, sexual orientation — **No**
- Financial info (allt) — **No**
- Health and fitness — **No**
- Messages: emails, SMS, other in-app messages — **No**
- Photos and videos — **No**
- Files and docs — **No**
- Calendar — **No**
- Contacts — **No**
- App activity: interactions, in-app search history, installed apps, other
  user-generated content, other actions — **No**
- Web browsing history — **No**
- App info and performance: crash logs, diagnostics, other — **No**.
  Appen har ingen kraschrapportering och inget analysbibliotek.
- Device or other IDs — **No**. Inget annonsid, inget enhetsid.

## 5. Security practices

- **Data is encrypted in transit** → Yes (se punkt 1).
- **You provide a way for users to request that their data is deleted** → No.
- **Committed to follow the Play Families Policy** → **[FREDRIK]** bara
  relevant om appen ska rikta sig till barn. Koden säger ingenting om
  målgrupp. Förslag: nej.
- **Independent security review** → No. Ingen extern granskning är gjord.

## 6. Privacy policy URL

→ `https://fredrikwryman-cmd.github.io/rostsvar/`

Publicerad i direktiv 36. Sidan byggs ur `juridik/integritetspolicy.md` och
ligger som `docs/index.html`, utgiven med GitHub Pages från `docs/` på main.
**[FREDRIK]** Ska policyn på sikt ligga på aimstudios.se i stället byts adressen
i båda butikerna när den sidan finns.

## 7. Övriga deklarationer som gränsar till det här

- **Permissions: SYSTEM_ALERT_WINDOW.** Play frågar varför appen behöver visa
  något över andra appar. Svar: den flytande Tala-knappen är appens
  huvudfunktion, och användaren slår på rätten för hand via guiden.
- **Accessibility API.** Play kräver en motivering och kan kräva en
  demonstrationsvideo. Svar: tillgänglighetstjänsten är enda vägen att skriva
  den dikterade texten i ett annat programs textfält. Den läser bara det
  fokuserade fältet, bara på begäran, och sparar och skickar ingenting.
  **[FREDRIK]** Google kräver ofta en kort video som visar funktionen. Den
  finns inte ännu.
- **Foreground service: microphone.** Deklareras med ändamålet diktering
  utanför appens egen skärm. Play begär en beskrivning och ibland video.
  **[FREDRIK]** Samma videofråga som ovan.
- **Data Safety-svaren måste stämma med integritetspolicyn.** Det är den
  vanligaste orsaken till avslag. Väljs **Yes** i punkt 1 ska policyn säga att
  ljud delas med taltjänstens leverantör, vilket den gör, med tillägget att
  det bara gäller när enhetstolkning saknas.
