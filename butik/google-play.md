# Google Play — butikstexter för Tap&Talk

Fälten under **Grow → Store presence → Main store listing** i Play Console.
Svenska (sv-SE) är standardspråket, engelska (en-US) översättningen.
Utvecklarnamn: **TechRabbit**. Paketnamn: `com.techrabbit.tapntalk`.
Versionsnamn 0.11, versionCode 11. Texterna beskriver Androidappen som den
är i dag; inget nedan är planerat eller på väg.

---

## Svenska (sv-SE)

### Apptitel (max 30 tecken)

```
Tap&Talk
```

### Kort beskrivning (max 80 tecken)

```
Diktera svenska direkt i fältet. Ingen text sparas, inget konto, ingen server.
```

### Fullständig beskrivning (max 4000 tecken)

```
Tap&Talk gör tal till text på svenska. Appen sparar ingenting och når inget nät på egen hand.

INGEN TEXT SPARAS

Det du säger blir text i fältet du står i, och där slutar det. Ljudet sparas aldrig. Den dikterade texten läggs inte i någon fil, databas eller inställning. Appen har inget konto, ingen inloggning, ingen analys, ingen kraschrapportering, inga annonser och inga tredjepartsbibliotek. Den har inte ens rätten att nå internet begärd i sin manifestfil.

Appen ber telefonen tolka talet på enheten först. Klarar telefonen det lämnar ljudet den aldrig. Saknas enhetstolkning går omgången över till telefonens vanliga taltjänst, och den kan använda internet — på de flesta telefoner är det Googles. Enhetstolkning kräver Android 13 eller senare och att svenska är hämtad i telefonens taltjänst.

TRE SÄTT ATT DIKTERA

Den flytande knappen ligger kvar över alla appar. Ett tryck, du talar, och texten hamnar i det fält du står i, efter den text som redan står där. Knappen går att dra dit du vill och fastnar vid närmaste kant. Du startar och stoppar den med strömbrytaren på appens hemskärm.

Rösttangentbordet är reserven, för fält den flytande knappen inte kommer åt. Vanlig svensk layout med å, ä och ö, siffror och skiljetecken, och längst ned en bred grön Tala-knapp. Texten skrivs rakt in i fältet medan du talar, och du rättar den med tangenterna.

Appens egen skärm är en stor Tala-knapp, ett textfält och Skicka. Läs igenom texten, rätta den för hand, och tryck på Skicka. Androids egen delningsruta öppnas och du väljer själv vilken app texten ska till. Appen skickar aldrig något på egen hand.

KOM IGÅNG-GUIDEN

Fem steg, ett i taget, var och ett med skälet till att Android kräver det: mikrofonen, visa över andra appar, tillgängligheten, tangentbordslistan och tangentbordsväljaren. Varje steg bockas av när appen själv ser att det är gjort, och sist finns ett provfält där du dikterar och får kvittot att det fungerar. Så länge något steg fattas står en varning kvar på hemskärmen och namnger just det som fattas.

VARFÖR APPEN BER OM TILLGÄNGLIGHETSRÄTTEN

Det är enda vägen för en app att skriva i ett annat programs textfält på Android. Tjänsten gör exakt en sak: när dikteringen är klar frågar den systemet vilket fält som har skrivfokus och lägger texten efter den som redan står där. Den läser inga andra fält, läser inte vad du skriver för hand, loggar ingenting, sparar ingenting och når inget nät. Hittas inget fält hamnar texten i urklipp med en notis om att den kan klistras in, så att det som sagts aldrig går förlorat.

Den flytande knappen körs som en förgrundstjänst, eftersom Android kräver det av något som håller mikrofonen igång utanför sin egen skärm. Notisen som följer med ligger på lägsta vikt, utan ljud och utan bricka.

I LÖSENORDSFÄLT ÄR DIKTERINGEN AVSTÄNGD

Tangentbordet vägrar diktera i lösenords-, siffer- och telefonfält. Tala-knappen går inte att trycka på där, så inget tal från ett lösenordsfält kan nå någon taltjänst.

KRAV

Android 11 eller senare, och en installerad taltjänst med stöd för svenska. Appen begär mikrofonen, rätten att visa över andra appar och tillgänglighetsrätten. Inget annat.

Appen är mörk och bara mörk. Reducera rörelse respekteras.

Utvecklad av AiM Studios under namnet TechRabbit.
```

### Kategori

- **App category:** Verktyg (Tools)
- **Tags:** föreslaget *Tillgänglighet*, *Produktivitet och verktyg*
- Motivering: appen är ett dikteringsverktyg utan eget innehåll och utan
  kommunikationsfunktion. Den skickar inga meddelanden själv; den lämnar text
  till delningsrutan. *Kommunikation* vore därför fel.

### Kontaktuppgifter

- **E-post:** info@aimstudios.se — samma adress som integritetspolicyn anger
  som ansvarig kontakt.
- **Webbplats:** https://fredrikwryman-cmd.github.io/rostsvar/
- **Telefon:** anges inte (frivilligt fält i Play Console)
- **Integritetspolicy:** https://fredrikwryman-cmd.github.io/rostsvar/

---

## English (en-US)

### App title (max 30 characters)

```
Tap&Talk
```

### Short description (max 80 characters)

```
Dictate Swedish straight into the field. No text stored, no account, no server.
```

### Full description (max 4000 characters)

```
Tap&Talk turns speech into Swedish text. It stores nothing and reaches no network on its own.

NO TEXT IS STORED

What you say becomes text in the field you are in, and that is where it ends. The audio is never saved. The dictated text is not written to any file, database or setting. The app has no account, no sign-in, no analytics, no crash reporting, no ads and no third-party libraries. It does not even declare internet permission in its manifest.

The app asks the phone to recognise speech on the device first. If the phone can, the audio never leaves it. If on-device recognition is missing, the round falls back to the phone's ordinary speech service, which may use the internet — on most phones that is Google's. On-device recognition needs Android 13 or later and Swedish downloaded in the phone's speech service.

THREE WAYS TO DICTATE

The floating button stays on top of every app. One tap, you speak, and the text lands in the field you are in, after the text already there. Drag the button wherever you like and it settles against the nearest edge. You start and stop it from the switch on the app's home screen.

The voice keyboard is the fallback, for fields the floating button cannot reach. An ordinary Swedish layout with å, ä and ö, numbers and punctuation, and a wide green Speak key along the bottom. The text is written straight into the field as you talk, and you correct it with the keys.

The app's own screen is a large Speak button, a text field and Send. Read the text, correct it by hand, and press Send. Android's own share sheet opens and you choose which app the text goes to. The app never sends anything by itself.

THE SETUP GUIDE

Five steps, one at a time, each with the reason Android requires it: the microphone, display over other apps, accessibility, the keyboard list and the keyboard picker. Each step is ticked off when the app sees for itself that it is done, and the last step is a test field where you dictate and get proof that it works. As long as a step is missing, a warning stays on the home screen naming exactly what is missing.

WHY THE APP ASKS FOR THE ACCESSIBILITY PERMISSION

It is the only way for an app to write into another app's text field on Android. The service does exactly one thing: when dictation finishes, it asks the system which field has input focus and appends the text after what is already there. It reads no other fields, does not read what you type by hand, logs nothing, stores nothing and reaches no network. If no field is found, the text goes to the clipboard with a notice that it can be pasted, so what was said is never lost.

The floating button runs as a foreground service, because Android requires that of anything holding the microphone outside its own screen. The notice that comes with it is at the lowest importance, with no sound and no badge.

DICTATION IS OFF IN PASSWORD FIELDS

The keyboard refuses to dictate in password, number and phone fields. The Speak key cannot be pressed there, so no speech from a password field can reach any speech service.

REQUIREMENTS

Android 11 or later, and an installed speech service that supports Swedish. The app asks for the microphone, display over other apps and the accessibility permission. Nothing else.

The app is dark and only dark. Reduce motion is respected.

Made by AiM Studios under the name TechRabbit.
```

### Category

- **App category:** Tools
- **Tags:** suggested *Accessibility*, *Productivity & tools*

### Contact details

- **Email:** info@aimstudios.se
- **Website:** https://fredrikwryman-cmd.github.io/rostsvar/
- **Phone:** not given (optional in Play Console)
- **Privacy policy:** https://fredrikwryman-cmd.github.io/rostsvar/

---

## Det här kan bara Fredrik fylla i

1. **Supportadressen.** `info@aimstudios.se` är tagen ur
   `juridik/integritetspolicy.md`, där AiM Studios står som ansvarig. Är det
   rätt adress för butikssupport, eller ska det vara en egen
   TechRabbit-adress? Play visar adressen publikt i butiken.
2. **Kategorin.** *Verktyg* är förslaget och motiveringen står ovan. Vill du
   hellre ligga i *Kommunikation* eller under *Tillgänglighet* är det ett
   affärsval, inte ett faktafel.
3. **Webbplatsen i butiken.** Just nu pekar den på policysidan på GitHub
   Pages. Ska en riktig produktsida på aimstudios.se stå där i stället?
4. **Skärmbilder.** Play kräver minst 2, högst 8, per formfaktor: telefon
   (minst 1080 px på kortaste sidan), och 7-tums och 10-tums tablet om appen
   ska visas där. Finns inte — appen har ännu inte körts på telefon.
5. **Funktionsgrafik (feature graphic).** 1024 × 500 px, obligatorisk. Finns
   inte.
6. **Appikonen i butiken.** 512 × 512 px finns som
   `design/ikon-google-play-512.png` och behöver bara laddas upp.
7. **Innehållsklassificering (content rating).** Formuläret måste fyllas i
   och väntar enligt direktiv 36 till efter telefontestet.
8. **Data Safety-formuläret.** Svaren ligger färdiga i
   `juridik/data-safety.md`, med punkt 1 som det avgörande valet.
9. **Marknadsföringsvideo (frivillig).** Google kräver dessutom ofta en kort
   demonstrationsvideo för tillgänglighetstjänsten och för
   förgrundstjänsten med mikrofonändamål. Den finns inte.
