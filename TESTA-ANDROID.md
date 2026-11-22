# Tap & Talk · testinstruktion för Android

Tack för att du provar. Appen heter **Tap & Talk** och kommer från Tech Rabbit.
Den dikterar svenska och lägger texten i chattens skrivfält. **Appen skickar
aldrig något själv** — du läser texten och trycker på chattens egen skickaknapp.

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

## 2 · Ge de två behörigheterna

Öppna **Tap & Talk**. Statusraden överst visar vad som fattas.

1. **Tillåt visning över appar** — leder till en systeminställning. Slå på den
   för Tap & Talk och gå tillbaka. Utan den kan knappen inte visas ovanpå
   Messenger eller Signal.
2. **Tillåt mikrofon** — svara **Medan appen används**.

När båda raderna visar *tillåtet* respektive *tillåten*, tryck på
**Starta flytande knapp**. En liten **Tala**-knapp lägger sig överst på skärmen.
Du kan dra den dit du vill.

## 3 · Välj Tap & Talk som tangentbord

Det här behövs för alternativet **Infoga i chatten**. Hoppar du över det får du
i stället kopiera och klistra in för hand, vilket också fungerar.

1. Tryck på **Aktivera tangentbord** och slå på **Tap & Talk** i listan.
   Android varnar att ett tangentbord kan läsa det du skriver. Det är
   standardvarningen för alla tangentbord.
2. Gå tillbaka och tryck på **Välj tangentbord**. Välj **Tap & Talk**.
3. Tangentbordet är avsiktligt litet: bara **Tala** och **Byt tangentbord**.
   Knappen **Byt tangentbord** tar dig tillbaka till ditt vanliga.

---

## 4 · Testa i Messenger och sedan i Signal

Gör exakt samma sak i båda apparna. Gör gärna första försöket i en chatt med
dig själv.

1. Öppna en chatt och tryck i skrivfältet så att tangentbordet visas.
2. Tryck på den flytande **Tala**-knappen.
3. Säg en mening på svenska med **å, ä och ö**, till exempel
   *"Jag är på väg och hör av mig i morgon."*
4. Tryck på **Klar**, eller vänta tills taltjänsten själv slutar lyssna.
5. Texten visas. Tryck på **Infoga i chatten**.
6. Läs texten i skrivfältet och tryck på **chattens egen skickaknapp**.

Syns inte **Infoga i chatten**, utan bara **Kopiera** och **Kasta**? Då är inte
Tap & Talk valt som tangentbord i just det fältet. Tryck på **Kopiera**, håll
fingret i skrivfältet och välj **Klistra in**.

### Berätta för varje app

- Visades den flytande knappen ovanpå chatten?
- Blev å, ä och ö rätt?
- Hamnade texten i rätt fält?
- Gav **ett** tryck på chattens skickaknapp **ett** meddelande?

## 5 · Två saker vi särskilt vill veta

**Avbryt.** Tryck på **Tala**, börja säga något och tryck på **Avbryt** medan den
lyssnar. Knappen ska krympa ihop och chatten ska vara helt orörd — ingen text i
skrivfältet, inget skickat. Prova också **Kasta** efter att texten visats.

**Byte av konversation mitt i dikteringen.** Tryck på **Tala**, börja diktera,
och byt till en **annan chatt** innan du trycker på Klar. När texten sedan visas
ska **Infoga i chatten** vara borta, eller ge meddelandet *"Skrivfältet har
ändrats. Kopiera vid behov."* Det viktiga: **texten får inte hamna i den nya
chatten av sig själv.** Hamnar den ändå där, säg till direkt — det är en bugg vi
vill veta om omgående.

## 6 · Stänga av

Tryck på **Stäng av knappen** i Tap & Talk, eller använd appens avisering.

---

## Bra att veta

- Appen läser inga chattloggar och sparar ingen meddelandehistorik.
- Telefonens taltjänst gör om talet till text och kan använda internet.
- **Kopiera** lägger texten i systemets urklipp, där andra appar kan nå den.
  På Android 13 och senare märks den som känslig så den inte förhandsvisas.
- Android och den öppna appen bestämmer var en överlagring får visas. Om en app
  vägrar visa knappen är det ett resultat vi vill ha, inte ett misslyckande.
- Dikteringen stoppas automatiskt efter 60 sekunder.

## Om något inte fungerar

Skriv vilken telefon och Android-version du har, i vilken app det hände och vad
du såg. Skärminspelning är guld värt. Att något **inte** går är också ett
resultat — det här är första testbygget och ingenting har ännu provats på en
fysisk telefon.
