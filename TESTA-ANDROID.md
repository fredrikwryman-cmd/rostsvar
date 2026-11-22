# Tap & Talk · testinstruktion för Android

Tack för att du provar. Appen heter **Tap & Talk** och kommer från Tech Rabbit.
Du dikterar svenska, läser texten och väljer själv vart den ska. **Appen skickar
aldrig något själv.**

Filen du fått heter `Tap-and-Talk-android-test.apk`. Den är signerad med en
lokal testnyckel, inte via Google Play. Kräver **Android 11 eller senare** och en
taltjänst med svenska installerad.

---

## 1 · Installera

1. Öppna APK-filen på telefonen, till exempel från Filer eller din
   nedladdningsmapp.
2. Android frågar om den app som öppnar filen får installera appar. Godkänn.
3. Får du varningen *Okänd app* eller *Play Protect*, välj **Installera ändå**.
   Appen är inte granskad av Google eftersom den inte ligger i Play.

Har du en tidigare version installerad uppgraderas den. Du behöver inte
avinstallera först.

## 2 · Hela appen är en skärm

Öppna **Tap & Talk**. Du ser tre saker:

- en stor grön **Tala**-knapp,
- ett textfält där texten hamnar,
- en **Skicka**-knapp, som är släckt tills det finns text.

Raden överst säger alltid vad som gäller just nu. Det är den du läser om något
inte blir som du väntar dig.

## 3 · Mikrofonen

Tryck på **Tala** första gången. Android frågar om mikrofonen — svara
**Medan appen används**.

Svarar du nej står det på skärmen att mikrofonen behövs. Säger du nej en gång
till slutar Android fråga; då byter knappen text till **Tillåt mikrofon** och
tar dig till appens inställningar. **Prova gärna båda vägarna**, det är ett av
de lägen vi vill ha provat.

## 4 · Diktera

1. Tryck på **Tala**.
2. Säg en mening på svenska med **å, ä och ö**, till exempel
   *"Jag är på väg och hör av mig i morgon."*
3. Texten ska fyllas i fältet **medan du talar**.
4. Tryck på **Klar**, eller vänta tills taltjänsten själv slutar lyssna.
5. Rätta texten för hand om du vill — fältet är ett vanligt skrivfält.
6. Vill du lägga till mer: tryck på **Tala om**. Det nya talet läggs **efter**
   det som redan står där, inget skrivs över.

## 5 · Skicka

Tryck på **Skicka**. Androids egen delningsruta öppnas. Välj Messenger, Signal,
Meddelanden, e-post eller vad du vill — texten följer med in i den appen, där du
trycker på appens egen skickaknapp.

Gör gärna första försöket i en chatt med dig själv.

## 6 · Det vi särskilt vill veta

- Blev **å, ä och ö** rätt?
- Fylldes texten **löpande** medan du talade, eller först efteråt?
- Gick det att **rätta texten för hand** efter dikteringen?
- Hamnade texten rätt i appen du valde i delningsrutan?
- Hur snabbt hörde taltjänsten att du slutat tala?
- Vad hände när du **lämnade appen mitt i** en diktering? Mikrofonen ska stängas
  och raden överst ska säga det.

---

## Bra att veta

- Appen begär **bara mikrofonen**. Ingen inloggning, ingen analys, inget konto.
- Telefonens taltjänst gör om talet till text och **kan använda internet**.
  Själva appen når inget nät.
- Inget spelas in och ingen text sparas. Stänger du appen är fältet tomt nästa
  gång du öppnar den.
- Texten lämnar appen först när du trycker på Skicka och väljer en app.

## Om något inte fungerar

Skriv vilken telefon och Android-version du har, vad som stod på raden överst
och vad du såg. Skärminspelning är guld värt. Att något **inte** går är också
ett resultat — det här är ett testbygge och ingenting har ännu provats på en
fysisk telefon.
