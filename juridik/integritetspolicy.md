# Integritetspolicy för Tap&Talk

Senast ändrad 25 november 2026. Gäller Tap&Talk för Android och iOS.

Ansvarig för appen är AiM Studios. Frågor och begäranden går till
info@aimstudios.se.

Den här texten beskriver vad appen gör, och ingenting annat. Varje påstående
nedan går att läsa i appens källkod.

## Kort version

Tap&Talk är ett verktyg som gör tal till text. Appen har inget konto, ingen
inloggning, ingen server och inga annonser. Den skickar ingenting någonstans
av egen kraft. Taligenkänningen är den enda delen som kan nå internet, och
den sköts av operativsystemet, inte av oss. Appen ber om tolkning på
telefonen först, på både Android och iPhone. Molnet används bara när
telefonen saknar stödet.

## Vilka uppgifter som behandlas

Tre saker, och bara under den stund de används:

**Ljudet från mikrofonen.** Inspelningen startar när du trycker på Tala och
slutar när omgången är klar. Appen sparar inte ljudet.

**Den text taligenkänningen lämnar tillbaka.** Texten går direkt till det
textfält du står i, eller till utkastet på appens egen skärm.

**Innehållet i det fält du dikterar till.** Appen läser den text som redan
står i fältet, av ett enda skäl: den nya texten ska läggas efter den gamla,
inte över den. Innehållet lämnar aldrig fältet.

Appen behandlar inget namn, ingen e-postadress, inget telefonnummer, ingen
plats, inga kontakter, inga bilder, inget annonsid och inget som identifierar
dig eller din telefon.

## Ljudet och tredje part

Tap&Talk har ingen egen taligenkänning. Appen lämnar ljudet till den tjänst
som redan finns i telefonen, och begär tolkning på enheten först. Lyckas det
lämnar ljudet aldrig telefonen.

**Android.** Appen frågar systemet om enhetstolkning finns
(`isOnDeviceRecognitionAvailable`) och tar den när svaret är ja, med
`createOnDeviceSpeechRecognizer`. Det kräver Android 13 eller senare och att
svenska är hämtad i telefonens taltjänst. Då sker tolkningen på telefonen och
inget ljud skickas vidare.

Saknas enhetstolkningen, eller visar den sig under dikteringen inte klara
svenska, går omgången över till telefonens vanliga taltjänst. På de flesta
telefoner är det Googles, och då skickas ljudet till Googles servrar, varpå
Googles villkor och integritetspolicy gäller för behandlingen. Alternativet
hade varit att vägra diktera, och det är ingen hjälp för den som behöver
appen.

**iOS.** Samma ordning. Appen använder Apples taligenkänning
(`SFSpeechRecognizer`) och begär tolkning på enheten när telefonen stöder det
(`requiresOnDeviceRecognition` sätts efter `supportsOnDeviceRecognition`). Gör
den inte det skickas ljudet till Apples servrar, och då gäller Apples villkor.

Vi är inte mottagare av ljudet i något av fallen. Det finns ingen server som
tillhör AiM Studios inblandad.

## Inget konto, ingen spårning

Appen kräver inget konto och går inte att logga in i. Den innehåller ingen
analys, ingen statistikinsamling, ingen krasch­rapportering, inga annonser och
inga tredjepartsbibliotek alls. Androidversionen har inte ens rätten att nå
internet begärd i sin manifestfil, och koden gör inga nätverksanrop på någon av
plattformarna.

## Tillgänglighetstjänsten på Android

För att kunna skriva i ett annat program behöver appen Androids
tillgänglighetsrätt. Det är den enda vägen som finns; utan den kan den
flytande knappen inte leverera texten.

Tjänsten gör exakt en sak: när dikteringen är klar frågar den systemet vilket
textfält som har skrivfokus och lägger texten efter den som redan står där.
Den prenumererar inte på innehåll, läser inga andra fält, läser inte vad du
skriver för hand, loggar ingenting, sparar ingenting och når inget nät. Den är
också avgränsad så att bara Android får koppla upp sig mot den.

Appen tittar dessutom på vilket program som äger det fokuserade fältet, men
bara för att jämföra: har du bytt app medan texten tolkades ska den inte
skrivas in i ett fält du inte talade till. Programnamnet sparas inte.

## Tangentbordet

Rösttangentbordet är reserven när den flytande knappen inte passar. Det
skriver i fältet med den vanliga tangentbordsvägen, alltså utan
tillgänglighetsrätt.

Tangentbordet läser texten närmast före markören, och enbart för att veta hur
mycket av sin egen senaste diktering det får radera när tolkningen ändras
mitt i. Ingenting av det som skrivs eller läses lagras, loggas eller skickas
vidare.

I lösenordsfält är dikteringen avstängd. Tangentbordet vägrar också diktera i
siffer- och telefonfält. Tala-knappen går inte att trycka på där, så inget tal
från ett lösenordsfält kan nå någon taltjänst.

På iOS kräver tangentbordet "fullständig åtkomst". Det är Apples krav för att
ett tangentbord alls ska få använda mikrofonen; utan den rätten visar
tangentbordet att mikrofonen är låst och gör ingenting mer. Appen använder
inte rätten till något annat.

## Lagring och lagringstid

Ljudet sparas aldrig. Den dikterade texten sparas inte i någon fil, databas
eller inställning.

Två saker sparas på telefonen, och ingen av dem innehåller något du sagt:

- Att du har sett kom igång-guiden.
- Var på skärmen du har dragit den flytande knappen.

Båda ligger i appens egen privata inställningsfil och tas bort när du
avinstallerar appen. Säkerhetskopiering av appens data är avstängd på Android.

Dikterad text kan i två fall hamna i telefonens urklipp:

- På Android, när texten inte går att leverera till något fält — till exempel
  när du bytt app under tolkningen eller när tillgänglighetstjänsten är
  avstängd. Appen säger till när det händer, så att du kan klistra in texten.
- På iOS, när iMessage-panelen fälls ihop mitt i en diktering.

Urklippet tillhör telefonen och styrs av dig. På iOS läggs texten dit med
flaggan "bara den här enheten", så att den inte går vidare till dina andra
Apple-enheter. Appen tömmer inte urklippet; nästa sak du kopierar skriver över
texten.

På appens egen skärm finns en Skicka-knapp som lämnar utkastet till systemets
delningsruta. Den gör ingenting av sig själv: du väljer själv vart texten går,
och då gäller villkoren för den app du valde.

Felloggen innehåller bara taltjänstens felkod, aldrig text eller ljud.

## Dina rättigheter

Eftersom appen inte samlar in eller lagrar några personuppgifter hos oss finns
det inget register hos AiM Studios att begära ut, rätta eller radera. Du har
ändå rätt enligt dataskyddsförordningen (GDPR) att få veta det, och att höra
av dig med frågor. Vill du ta bort det lilla som ligger på telefonen räcker det
att avinstallera appen.

Behandlingen hos Google eller Apple, i de fall telefonen saknar
enhetstolkning och ljudet går till deras servrar, sker under deras eget
ansvar. Rättigheter som rör den behandlingen vänder du
dig till dem med. Båda beskriver den i sina egna integritetspolicyer.

Anser du att något har gått fel kan du klaga hos Integritetsskyddsmyndigheten
(IMY).

## Barn

Appen riktar sig inte till barn och samlar inte in uppgifter om ålder.

## Ändringar

Ändras appen så att den här texten inte längre stämmer uppdateras texten, och
datumet högst upp byts.

## Kontakt

info@aimstudios.se
