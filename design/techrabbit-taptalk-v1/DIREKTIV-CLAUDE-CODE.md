# DIREKTIV TILL CLAUDE CODE — Tap & Talk, Tech Rabbit-design v1

Mål: Bygg in den nya designen på appens enda skärm. Byt bara utseende och animation. Funktion, dikteringslogik, navigation till Guide och Om, samt behörigheter ändras inte.

## 0. Före start
1. Läs appens befintliga huvudskärm och identifiera: dikteringsstart/stopp, textfältets state, Rensa, Skicka (share/val av mottagare), Guide, Om.
2. Notera vilken teknik appen använder (SwiftUI eller React Native). Värdena nedan är i pt och gäller båda.
3. Gör inga ändringar i Speech-, mikrofon- eller delningslogik. Bara vy-lagret.
4. Commit före start så att allt kan backas.

## 1. Mörkt läge
Skärmen är designad endast i mörkt läge. Tvinga mörkt utseende på den här skärmen (SwiftUI: `.preferredColorScheme(.dark)`, RN: fasta färger nedan).

## 2. Färger (tokens)
| Token | Värde | Användning |
|---|---|---|
| bg | #0B0B0B | Skärmbakgrund |
| card | #1A1A1A (fält: 92 % opacitet) | Textfält, knappar, ikonring |
| border | #2E2E2E | Kanter i vila |
| text | #F2F2F2 | Rubrik, inmatad text |
| muted | #A8A8A8 | Ingress |
| faint | #8A8A8A | Platshållare, räknare |
| disabled | #5A5A5A | Inaktiv knapptext |
| green | #1CF038 | Tala-knapp, aktiv kant, TECH RABBIT, Skicka aktiv |
| red | #E0583E | Stoppa-knapp och ringar under inspelning |
| onGreen | #0B0B0B | Ikon/text på grönt |
| plane | #1C1C1C | Bakgrundsmönster |

## 3. Typografi (Archivo, finns redan i Tech Rabbit-apparna)
- Rubrik "Tap & Talk": Archivo 900, 28/32, letterSpacing −0.5.
- "TECH RABBIT": Archivo 700, 12, letterSpacing 0.6, green.
- Guide/Om: 700, 14.
- Ingress: 400, 15/21, muted.
- Textfält: 400, 17/25.
- Status/räknare: 12 (status 700).
- Tala/Stoppa-etikett: 800, 17. Rensa/Skicka: 700, 16.
Alla storlekar ska skala med Dynamic Type.

## 4. Layout (390 × 844, marginal 20)
Uppifrån, under safe area:
1. **Header**, höjd 56, 6 pt under statusraden, gap 12:
   - Ikonring 40×40, card, 1.5 pt green kant, vit kaninsymbol 22 pt (Tech Rabbit-symbolen, inte ny ritning).
   - Kolumn: "Tap & Talk" + "TECH RABBIT".
   - Guide och Om: pill 38 hög, padding 0 14, card, 1 pt border. Träffyta minst 44 (utöka med contentShape/hitSlop). Behåll befintliga actions.
2. **Ingress** 14 pt under headern. Texten oförändrad.
3. **Textfält** 16 pt under, tar resterande höjd (flex). Radie 22, padding 18/18/14, 1.5 pt kant.
   - Platshållare (faint): "Texten du dikterar hamnar här. Du kan rätta den för hand."
   - Fältet ska fortfarande gå att redigera för hand (befintlig TextEditor/TextInput).
   - Sidfot i fältet: vänster status (prick 6 pt + text), höger "N tecken" (räknar textens längd live).
4. **Tala-knapp** 20 pt under fältet, centrerad: yta 132×132 med två ringar (yttre inset 0, inre inset 12, 1.5 pt), knapp 88×88 cirkel. Etikett 10 pt under.
5. **Rensa / Skicka** 16 pt under, två kolumner gap 10, höjd 52, pill, ikon 17 + text. 10 pt till hemindikatorn.

## 5. Bakgrund
Statisk SVG (react-native-svg / SwiftUI Path), pointerEvents none, dold för VoiceOver:
- Plan uppe: path "M0 0 H390 V250 C 320 330, 230 300, 170 210 C 120 130, 70 50, 0 30 Z", linjär gradient #1C1C1C 0.9 → 0.
- Grönt band nere: path "M390 300 C 290 380, 300 540, 190 640 C 120 700, 50 750, 0 844 H390 Z", gradient #1CF038 0 → 0.16.
- Linje längs bandets kant: #2A2A2A, 1.5 pt.
- Radiell glöd bakom knappen: centrum (195,700), radie 230, #1CF038 0.22 → 0.
Skala med "xMidYMid slice". Ingen animation i bakgrunden.

## 6. Lägen
| | Vila (Redo) | Lyssnar | Klar (text finns, ej inspelning) |
|---|---|---|---|
| Fältkant | border | green | border |
| Status | "Redo", prick #4A4A4A | "Lyssnar…" green, prick green | "Klar", prick #A8A8A8 |
| Knapp | green, mikrofonikon onGreen 34 pt | red, vit rundad kvadrat 28×28 r7 | green, mikrofon |
| Etikett | Tala | Stoppa | Tala |
| Ringar | green 0.45, långsam puls | red 0.7, snabb puls | green, långsam puls |
| Rensa | inaktiv | inaktiv | aktiv: text #F2F2F2, kant #4A4A4A |
| Skicka | inaktiv | inaktiv | aktiv: green yta, onGreen text |
| Markör | — | grön markör 2×20, blinkar 450 ms | systemets |

Rensa och Skicka styrs av samma villkor som i dag (text finns och ingen inspelning pågår). Ändra inte deras logik.

## 7. Animation
**Ringpuls** (båda lägen): två ringar, fasförskjutna en halv period. För varje ring: progress p = (tid / period + offset) mod 1. scale = 1 + p × amp. opacity = (1 − p) × maxOp. Linjär, oändlig loop.
- Vila: period 2,8 s, amp 0.22, maxOp 0.8, färg green.
- Lyssnar: period 1,6 s, amp 0.35, maxOp 1.0, färg red.
**Glöd** runt knappen i vila: shadow green, radie 36, opacitet 0.35 + 0.15 × sin(2π × tid/2,8 s). Under inspelning fast red 0.45.
**Lägesbyte**: knappens färg och fältets kant tonas 200 ms ease-out.
**Text** under inspelning: visa transkriptionen som den kommer från Speech (ingen fejkad skrivanimation). Markören står sist.
**Reducera rörelse**: inga pulserande ringar (visa dem stilla, opacitet 0.55 / 0.9), ingen andande glöd, ingen blinkande markör. Lägesbyten utan tona.
SwiftUI: TimelineView(.animation) eller withAnimation(.linear.repeatForever). RN: Reanimated withRepeat(withTiming(..., linear)).

## 8. Haptik
- Tryck på Tala (start): lätt impact.
- Tryck på Stoppa: medium impact.
- Skicka lyckades: success.
- Fel (mikrofon nekad, Speech-fel): warning.
Ingen haptik under pulsen eller medan text strömmar. Följ appens Haptik-inställning om den finns.

## 9. Tillgänglighet
- Knappen: label "Tala" / "Stoppa", hint "Startar diktering" / "Avslutar diktering".
- Status annonseras vid byte (Lyssnar…, Klar). Räknaren läses inte upp vid varje tecken.
- Ringar, bakgrund och glöd: dolda för VoiceOver.
- Alla träffytor minst 44×44.

## 10. Får inte gå sönder
- Diktering, stopp och redigering för hand fungerar som i dag.
- Rensa tömmer fältet och går till Redo.
- Skicka öppnar samma delningsflöde som i dag.
- Guide och Om öppnar samma vyer som i dag.
- Tangentbordet får inte täcka Tala-knappen: när fältet redigeras, scrolla eller lyft innehållet (keyboard avoidance).
- iPhone SE (375 × 667): textfältet krymper, inget annat. Kontrollera att Rensa och Skicka syns.

## 11. Kontroll före commit
1. Bygg och kör på simulator: Redo → Lyssnar → Klar → Rensa → Redo.
2. Jämför mot png/01_vila.png och png/02_lyssnar.png.
3. Testa Reducera rörelse och VoiceOver.
4. Testa iPhone SE och Pro Max.
5. Rapportera ändrade filer och vad som inte kunde verifieras.
