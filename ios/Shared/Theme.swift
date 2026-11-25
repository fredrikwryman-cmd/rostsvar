import SwiftUI
import UIKit

/// Appens mått, radier, typskala och rörelsetider på ett ställe.
///
/// Färgerna har legat samlade sedan direktiv 23, i `Palette`. Måtten har
/// inte: `20` för sidmarginalen stod på fem ställen i tre filer, `22` för
/// kortets radie på sex, och tangentbordet hade sin egen uppsättning. Det
/// gick att ändra en skärm och tro att man ändrat alla.
///
/// Filen ligger i `Shared` av samma skäl som `Palette`: alla tre målen
/// bygger den, så appen, tangentbordet och iMessage-tillägget mäter i samma
/// enheter. Androids `Look.java` har samma värden skrivna i Java.
///
/// Värdena är de som redan användes. Den här filen flyttar dem, den ändrar
/// dem inte.
enum Metric {

    // MARK: - Luft

    /// Sidmarginalen på startskärmen och i arken.
    static let side: CGFloat = 20
    /// Kortets inre luft.
    static let cardPad: CGFloat = 18
    /// Fältets inre luft: lite tätare nedtill, där foten står.
    static let fieldTop: CGFloat = 18
    static let fieldBottom: CGFloat = 14

    static let gapHair: CGFloat = 2
    static let gapTiny: CGFloat = 4
    static let gapXS: CGFloat = 6
    static let gapS: CGFloat = 8
    static let gapSM: CGFloat = 10
    static let gapM: CGFloat = 12
    static let gapML: CGFloat = 14
    static let gapL: CGFloat = 16
    static let gapXL: CGFloat = 18
    static let gapXXL: CGFloat = 20
    static let gapHuge: CGFloat = 28

    // MARK: - Radier

    /// Kort, textfält och iMessage-tilläggets utkastyta.
    static let cardRadius: CGFloat = 22
    /// Tangenten i tangentbordet, och stoppikonen i Tala-knappen.
    static let keyRadius: CGFloat = 7
    /// Tangentbordets Tala-knapp.
    static let micKeyRadius: CGFloat = 12

    // MARK: - Mått

    /// Minsta träffyta. 44 × 44 är golvet, inte ett riktmärke.
    static let touch: CGFloat = 44
    /// Pillerknappen i huvudet. Syns som 38, träffytan är `touch`.
    static let pillHeight: CGFloat = 38
    /// Primär- och sekundärknapp: piller, 52 högt.
    static let controlHeight: CGFloat = 52
    /// Ikonringen med kaninsymbolen, och symbolen i den.
    static let markSize: CGFloat = 40
    static let markIcon: CGFloat = 22
    /// Tala-knappens skiva, och ramen med ringarna runt den.
    static let micDisc: CGFloat = 88
    static let micRing: CGFloat = 132
    /// Ikonen inne i Tala-knappen, och stoppikonen när den lyssnar.
    static let micIcon: CGFloat = 34
    static let micStop: CGFloat = 28
    /// Ikonen i Rensa och Skicka.
    static let actionIcon: CGFloat = 17
    /// Etikettens luft i sidled.
    static let badgeX: CGFloat = 9
    /// Rutan kring en upplysning inne i ett kort.
    static let noteRadius: CGFloat = 14
    /// Glöden bakom Tala-knappen. Underlaget anger `box-shadow: 0 0 36px`;
    /// CSS räknar radien som hela oskärpan, SwiftUI som halva.
    static let glowRadius: CGFloat = 18
    /// Oskärpan över skärmen när appen inte är i förgrunden.
    static let shieldBlur: CGFloat = 24
    /// `TextEditor` lägger på eget inre utrymme. Det dras bort med de här
    /// två, så att texten står i linje med fältets egen `cardPad` och inte
    /// hoppar när läsytan byts mot redigeraren.
    static let editorInsetX: CGFloat = -5
    static let editorInsetY: CGFloat = -8
    /// Huvudets höjd på startskärmen.
    static let headerHeight: CGFloat = 56
    /// Toppfältet: den smala raden som ligger kvar när huvudet kollapsat.
    static let barHeight: CGFloat = 44
    /// Hur långt man ska behöva scrolla för att huvudet är helt kollapsat.
    static let collapse: CGFloat = 72
    /// Hur mycket ordmärket krymper på vägen.
    static let collapseScale: CGFloat = 0.18
    /// Vid hur stor del av kollapsen den lilla versionen tonar in. rev-ux
    /// 4.3 punkt 1: ca 60 procent.
    static let barReveal: Double = 0.6
    /// Toppfältet får glas och avdelare först när innehåll ligger under det.
    /// rev-ux 4.3 punkt 2: scroll över 4 pt.
    static let glassFrom: CGFloat = 4
    /// Och är helt påslaget här.
    static let glassFull: CGFloat = 24
    /// Textfältets minsta höjd.
    static let fieldMinHeight: CGFloat = 96
    /// Lägesprickan i fältets fot, och snurran som ersätter den.
    static let dot: CGFloat = 6
    static let spinner: CGFloat = 10

    // MARK: - Linjer

    // MARK: - Tangentbordets egna mått

    /// Luften mellan två tangenter, och mellan två rader.
    static let keyGap: CGFloat = 5
    static let rowGap: CGFloat = 6
    /// Tangentbordets egen marginal, som är trängre än appens: varje punkt
    /// här är en punkt mindre tangent.
    static let boardInsetX: CGFloat = 4
    static let boardInsetY: CGFloat = 6
    /// Tala-tangentens höjd i tangentbordet.
    static let micKeyHeight: CGFloat = 50
    /// Utkastytans minsta höjd i iMessage-tillägget.
    static let draftMinHeight: CGFloat = 120
    /// Tangentbordets höjd följer textstorleken, men inte obegränsat: ett
    /// tangentbord som tar halva skärmen hjälper ingen.
    static let boardMin: CGFloat = 296
    static let boardMax: CGFloat = 430

    /// Avdelare och kanter som inte bär en komponentgräns.
    static let hairline: CGFloat = 1
    /// Kanten runt kort, fält och ringar. Bär gränsen, därför tjockare.
    static let edge: CGFloat = 1.5

    /// Avdelaren mellan listrader. Beslut A: bara där, aldrig som
    /// komponentgräns — en gräns ska klara 3:1 och det gör inte den här.
    static let dividerAlpha: Double = 0.08
}

/// Rörelsens tider och den enda kontrollen för reducerad rörelse.
///
/// Förr frågade varje vy själv: `HomeScreen` hade sin `fade`, ringarna sin
/// `still`, markören sin egen gren. Tre svar på samma fråga, och de kunde
/// skilja sig. Nu svarar den här filen.
///
/// Med Reducera rörelse påslaget ersätts förflyttning, zoom och puls med en
/// crossfade på 160 ms. Haptiken ligger kvar: den är inte rörelse på skärmen
/// och den är ofta hela bekräftelsen för den som slagit på inställningen.
enum Move {

    /// Tryck ned.
    static let tap: Double = 0.09
    /// Ikonbyte, chip, text. Också crossfadens längd vid reducerad rörelse.
    static let swap: Double = 0.16
    /// Lägesbyte på skärmen.
    static let state: Double = 0.2
    /// Sidbyte.
    static let push: Double = 0.32
    /// Ark upp och ned.
    static let sheet: Double = 0.38
    /// Förskjutning mellan rader som kommer in efter varandra.
    static let stagger: Double = 0.035

    /// Tryckets nedskalning.
    static let pressScale: CGFloat = 0.97
    /// Tryckets dämpning när rörelse ska vara reducerad: ingen zoom, bara
    /// en ton. Något måste hända inom 100 ms, annars känns knappen död.
    static let pressDim: Double = 0.8

    /// Knappar, chips och ikoner. rev-ux avsnitt 3: damping 24,
    /// stiffness 340 — i SwiftUI samma kurva som response 0,25 och
    /// dampingFraction 0,8.
    static var snappy: Animation { .spring(response: 0.25, dampingFraction: 0.8) }

    /// Animationen för ett tryck.
    static func press(reduced: Bool) -> Animation {
        reduced ? .easeInOut(duration: swap) : snappy
    }
    /// Sidan bakom ett ark.
    static let recedeScale: CGFloat = 0.94
    static let recedeDim: Double = 0.45

    /// Animationen för ett lägesbyte.
    ///
    /// Vid reducerad rörelse en crossfade på `swap`, annars skärmens vanliga
    /// toning. Aldrig `nil`: ett byte utan animation hoppar, och ett hopp är
    /// också rörelse.
    static func change(reduced: Bool) -> Animation {
        reduced ? .easeInOut(duration: swap) : .easeOut(duration: state)
    }

    /// Animationen för något som rör sig i rummet: glider, växer, flyger.
    /// Vid reducerad rörelse byts förflyttningen mot samma crossfade.
    static func travel(reduced: Bool) -> Animation {
        reduced ? .easeInOut(duration: swap) : .spring(response: 0.32, dampingFraction: 0.86)
    }

    /// Övergången för något som kommer in eller går ut.
    static func appear(reduced: Bool) -> AnyTransition {
        reduced ? .opacity : .opacity.combined(with: .move(edge: .bottom))
    }

    /// Sant när en puls, en andning eller en blinkande markör ska stå still.
    /// Det som står still ska stå still, inte försvinna: formen är en del av
    /// skärmen.
    static func paused(reduced: Bool) -> Bool { reduced }
}

/// Typskalan: storlek, vikt, textstil, spärrning och radhöjd per roll.
///
/// Rollerna är de som faktiskt finns på skärmarna. Vikterna är Archivos
/// fyra. Textstilen är den Dynamic Type räknar mot, så att 15 punkter blir
/// större när användaren ställt upp textstorleken.
enum TypeScale {

    /// Archivos fyra vikter, oberoende av UIKit och SwiftUI, så att filen
    /// går att bygga i alla tre målen.
    enum Weight {
        case regular, semibold, bold, heavy, black

        var ui: UIFont.Weight {
            switch self {
            case .regular: return .regular
            case .semibold: return .semibold
            case .bold: return .bold
            case .heavy: return .heavy
            case .black: return .black
            }
        }

        var swiftUI: Font.Weight {
            switch self {
            case .regular: return .regular
            case .semibold: return .semibold
            case .bold: return .bold
            case .heavy: return .heavy
            case .black: return .black
            }
        }
    }

    enum Style {
        case title, body, subheadline, caption, caption2

        var ui: UIFont.TextStyle {
            switch self {
            case .title: return .title1
            case .body: return .body
            case .subheadline: return .subheadline
            case .caption: return .caption1
            case .caption2: return .caption2
            }
        }

        var swiftUI: Font.TextStyle {
            switch self {
            case .title: return .title
            case .body: return .body
            case .subheadline: return .subheadline
            case .caption: return .caption
            case .caption2: return .caption2
            }
        }
    }

    struct Step {
        let size: CGFloat
        let weight: Weight
        let style: Style
        let tracking: CGFloat
        let lineHeight: CGFloat?
        /// Största punktstorlek steget får växa till.
        ///
        /// Dynamic Type ska följas, men en tangent har en fast bredd. Utan
        /// tak växte texten ur sin tangent vid de största inställningarna,
        /// och då stod det "Mellansl…" på mellanslagstangenten.
        let cap: CGFloat

        init(_ size: CGFloat,
             _ weight: Weight,
             _ style: Style = .body,
             tracking: CGFloat = 0,
             lineHeight: CGFloat? = nil,
             cap: CGFloat = .greatestFiniteMagnitude) {
            self.size = size
            self.weight = weight
            self.style = style
            self.tracking = tracking
            self.lineHeight = lineHeight
            self.cap = cap
        }

        /// Samma steg som UIKit-typsnitt, skalat mot sin textstil.
        func uiFont() -> UIFont {
            UIFontMetrics(forTextStyle: style.ui)
                .scaledFont(for: .systemFont(ofSize: size, weight: weight.ui),
                            maximumPointSize: cap)
        }
    }

    /// Ordmärket Tap&Talk i huvudet.
    static let appName = Step(28, .black, .title, tracking: -0.5, lineHeight: 32)
    /// TECH RABBIT under ordmärket.
    static let wordmark = Step(12, .bold, .caption, tracking: 0.6)
    /// Rubrik i ett kort, etiketten under Tala-knappen, arkets titel.
    static let heading = Step(17, .heavy)
    /// Utkastet i fältet, och platshållaren i det.
    static let draft = Step(17, .regular, .body, lineHeight: 25)
    /// Knapptext.
    static let button = Step(16, .bold)
    /// Brödtext.
    static let body = Step(15, .regular, .subheadline, lineHeight: 21)
    /// Brödtext utan radhöjd: en rad som inte bryts.
    static let bodyLine = Step(15, .regular, .subheadline)
    /// Brödtext i fet vikt: versionsnumret, guidens svar.
    static let bodyStrong = Step(15, .bold, .subheadline, lineHeight: 21)
    static let bodyStrongLine = Step(15, .bold, .subheadline)
    /// Siffran framför ett steg i guiden och i Om.
    static let stepNumber = Step(15, .black, .subheadline)
    /// Pillerknappen i huvudet.
    static let pill = Step(14, .bold, .subheadline)
    /// Lägesordet i fältets fot.
    static let chip = Step(12, .bold, .caption2)
    /// Teckenräknaren.
    static let counter = Step(12, .regular, .caption2)
    /// Den mindre raden under brödtexten.
    static let footnote = Step(12, .regular, .caption)
    /// Etiketten "kontrollera själv" i guiden.
    static let badge = Step(11, .bold, .caption2)

    // Tangentbordets egna steg. De har tak, eftersom en tangent har en
    // fast bredd. Tangentbordet ritar i systemtypsnittet och inte i
    // Archivo: tilläggets paket registrerar inga egna typsnitt.
    /// Statusraden över tangenterna.
    static let boardStatus = Step(12, .regular, .caption, cap: 20)
    /// Tala-tangenten.
    static let boardMic = Step(20, .semibold, .body, cap: 28)
    /// En vanlig bokstavstangent.
    static let boardKey = Step(19, .regular, .body, cap: 26)
    /// En tangent med ord i stället för en bokstav: shift, siffror, Klar.
    static let boardAccent = Step(16, .regular, .body, cap: 22)
}
