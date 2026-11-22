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

## 2 · Appens egen skärm

Öppna **Tap & Talk**. Du ser fyra saker:

- en strömbrytare för den **flytande knappen**, högst upp,
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

## 6 · Guiden

Det här är kärnan: diktera **där du redan skriver**, utan att gå via appen. Två
vägar finns, och guiden slår på båda.

**Guiden** öppnas av sig själv första gången du startar appen, och finns annars
bakom **Guide**-knappen uppe till höger på hemskärmen. Den tar dig genom fem
steg och bockar av dem när de är gjorda:

1. **Tillåt mikrofonen.** Frågan ställs i appen, eftersom varken ett
   tangentbord eller en tjänst får fråga själv.
2. **Visa över andra appar.** Det här är vad den flytande knappen behöver för
   att alls få synas ovanpå andra appar.
3. **Låt appen skriva i fältet.** Knappen öppnar tillgänglighetsinställningarna.
   Slå på **Tap & Talk · skriv i fältet**. Android varnar att tjänsten kan läsa
   vad som visas — det är standardvarningen. Tap & Talk läser bara det fält du
   står i, för att kunna skriva din text där. Samma mening står i Androids egen
   ruta; **läs den gärna och säg om den duger.**
4. **Slå på tangentbordet.** Knappen öppnar Androids lista över tangentbord.
   Slå på **Tap & Talk · rösttangentbord**.
5. **Byt till Tap & Talk.** Knappen öppnar tangentbordsväljaren.

Lämna aldrig guiden för att leta själv: varje steg har en knapp som öppnar rätt
ruta. Kommer du tillbaka till appen ska steget vara bockat av **utan** att du
trycker på något.

Sist i guiden finns ett **provfält**. Diktera en mening där; står det
*"Det fungerar"* är allt på plats. Så länge något steg fattas står en gul
varning kvar på hemskärmen, som säger just vad som fattas, med en knapp
tillbaka till guiden.

## 7 · Den flytande knappen

Slå på **Flytande knapp** med strömbrytaren högst upp på hemskärmen. En liten
grön knapp lägger sig över skärmen och stannar där även när du lämnar appen.

1. Dra den dit du vill. Den ska **fastna vid närmaste kant** när du släpper.
2. Öppna en chatt och tryck i skrivfältet.
3. Tryck på bubblan. Den blir mörkare, texten byts till **Klar** och den ska
   **röra sig med din röst**.
4. Säg en mening. Texten ska hamna i chattens fält, **efter** det som redan
   står där.
5. Tryck på bubblan igen om du vill avsluta tidigare.
6. Stäng bubblan med strömbrytaren i appen, eller med **Stäng** i notisen.

Hittar bubblan inget textfält hamnar texten i **urklipp** och en notis säger
det. Håll då in i fältet och välj **Klistra in**. Det som du har sagt ska
aldrig försvinna — **om det gör det är det det viktigaste du kan rapportera.**

Notisen ska vara tyst och diskret: inget ljud, ingen bricka, ingen ruta som
hoppar fram.

## 8 · Rösttangentbordet

Reserven, för fält där bubblan inte kommer åt. I chatten:

1. Tryck på den breda gröna **Tala**-knappen längst ned.
2. Diktera. Texten ska skrivas **rakt in i chattens skrivfält medan du talar**,
   först som preliminär text och sedan som vanlig text.
3. Rätta med tangenterna. **⇧** är skift, två tryck låser versaler, **123**
   växlar till siffror och skiljetecken, **⌫** raderar och fortsätter radera om
   du håller kvar fingret.
4. **⌨** tar dig tillbaka till ditt vanliga tangentbord.
5. Läs texten och tryck på **chattens egen skickaknapp**. Tangentbordet skickar
   aldrig något själv.

## 9 · Det vi särskilt vill veta

- Blev **å, ä och ö** rätt?
- Fylldes texten **löpande** medan du talade, eller först efteråt?
- Gick det att **rätta texten för hand** efter dikteringen?
- Hamnade texten rätt i appen du valde i delningsrutan?
- Hur snabbt hörde taltjänsten att du slutat tala?
- Vad hände när du **lämnade appen mitt i** en diktering? Mikrofonen ska stängas
  och raden överst ska säga det.
- I tangentbordet: hamnade texten i **rätt** fält, och blev det rätt när du
  **skrev med tangenterna mitt i** en diktering? Ingen text ska dyka upp två
  gånger och ingenting ska försvinna.
- Fungerade **⌨** för att komma tillbaka till ditt vanliga tangentbord?
- I guiden: bockades varje steg av **av sig själv** när du kom tillbaka från
  Androids inställningar, utan att du behövde trycka vidare?
- Bubblan: gick den att dra, fastnade den vid kanten, och låg den kvar där du
  lämnade den nästa gång du slog på den?
- Hamnade texten från bubblan i **rätt** fält, och **efter** den text som redan
  stod där?
- Tappade du någon gång en mening du hade sagt? Vad stod det då på skärmen?
- Höll telefonen bubblan vid liv, eller stängdes den av systemet när du
  använde andra appar en stund?
- Var notisen tyst och diskret, och gick **Stäng** i den att använda?

---

## Bra att veta

- Appen begär mikrofonen, rätten att **visa över andra appar** och
  tillgängligheten som gör att den kan skriva i fältet. Ingen inloggning, ingen
  analys, inget konto, ingen plats, inga kontakter.
- Telefonens taltjänst gör om talet till text och **kan använda internet**.
  Själva appen når inget nät.
- Inget spelas in och ingen text sparas. Stänger du appen är fältet tomt nästa
  gång du öppnar den.
- Texten lämnar appen först när du trycker på Skicka och väljer en app.
- Rösttangentbordet skriver bara i det fält du själv står i. I lösenordsfält är
  dikteringen avstängd; där fungerar bara tangenterna.
- Tillgänglighetstjänsten läser **bara** det fält som har skrivfokus, och bara
  i det ögonblick bubblan ska skriva. Den läser inga andra fält, loggar
  ingenting och sparar ingenting.

## Om något inte fungerar

Skriv vilken telefon och Android-version du har, vad som stod på raden överst
och vad du såg. Skärminspelning är guld värt. Att något **inte** går är också
ett resultat — det här är ett testbygge och ingenting har ännu provats på en
fysisk telefon.
