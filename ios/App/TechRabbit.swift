import SwiftUI

/// Tech Rabbit-designen för dikteringsskärmen, v1.
///
/// Underlaget ligger i `design/techrabbit-taptalk-v1/`. Paragraftecknen i
/// kommentarerna nedan pekar på avsnitt i dess `DIREKTIV-CLAUDE-CODE.md`.
///
/// Allt i den här filen är vy-lager: färger, typsnitt, bakgrund, ikoner och
/// ringarnas puls. Ingen rad här vet något om mikrofonen, taligenkänningen
/// eller delningen. Den ligger skild från `HomeScreen` så att skärmens
/// layout går att läsa utan att ritningarna står i vägen.
enum TR {

    // MARK: - Färger, §2

    // Värdena står i `Shared/Palette.swift`, som alla tre iOS-målen bygger.
    // Här görs de bara om till SwiftUI-färger. Förr stod de bara här, och
    // därför hade tangentbordet ett eget grönt och iMessage-tillägget
    // systemets färger.
    static let bg = hex(Palette.bg)
    static let card = hex(Palette.card)
    static let border = hex(Palette.border)
    static let text = hex(Palette.text)
    static let muted = hex(Palette.muted)
    static let faint = hex(Palette.faint)
    static let disabled = hex(Palette.disabled)
    static let green = hex(Palette.green)
    static let red = hex(Palette.red)
    static let onGreen = hex(Palette.onGreen)
    static let plane = hex(Palette.plane)

    /// Prickens grå i vila, och Rensas kant när knappen är aktiv. §6.
    static let dimDot = hex(Palette.dimDot)
    /// Linjen längs det gröna bandets kant. §5.
    static let bandEdge = hex(Palette.bandEdge)

    /// Fältets yta är card på 92 % opacitet. §2.
    static var fieldFill: Color { card.opacity(0.92) }

    static func hex(_ value: Int) -> Color {
        Color(.sRGB,
              red: Double((value >> 16) & 0xFF) / 255,
              green: Double((value >> 8) & 0xFF) / 255,
              blue: Double(value & 0xFF) / 255,
              opacity: 1)
    }
}

// MARK: - Typografi, §3

/// Archivo, typsnittet underlaget är ritat i. Namnen är typsnittens egna
/// PostScript-namn, avlästa ur name-tabellen i filerna under `App/Fonts/`,
/// inte gissade ur filnamnen — de råkar stämma överens här, men det är
/// PostScript-namnet `Font.custom` frågar efter.
///
/// De fyra vikterna är de fyra underlaget använder: 400 brödtext, 700
/// rubriker och knappar, 800 etiketten under Tala-knappen, 900 appnamnet.
/// Hittas inte filen faller SwiftUI tillbaka på systemtypsnittet av sig
/// själv, så en saknad fil ger en tråkigare skärm men ingen krasch.
enum Archivo {
    static func name(for weight: Font.Weight) -> String {
        switch weight {
        case .black: return "Archivo-Black"        // 900
        case .heavy: return "Archivo-ExtraBold"    // 800
        case .bold, .semibold: return "Archivo-Bold" // 700
        default: return "Archivo-Regular"          // 400
        }
    }
}

/// En textstorlek ur underlaget som ändå växer med Dynamic Type.
///
/// `@ScaledMetric` räknar om punkterna mot en textstil, så att 17 pt blir
/// större när användaren har ställt upp textstorleken i Inställningar.
/// Radavståndet räknas om i samma takt, därför ett förhållande och inte ett
/// fast tal.
///
/// Typsnittet sätts med `fixedSize:` och inte `size:`. `Font.custom(_:size:)`
/// skalar nämligen själv mot Dynamic Type, och punkterna är redan skalade
/// här — annars hade texten vuxit två gånger.
struct TRFont: ViewModifier {
    @ScaledMetric private var size: CGFloat
    private let weight: Font.Weight
    private let tracking: CGFloat
    private let leading: CGFloat?

    init(_ size: CGFloat,
         _ weight: Font.Weight,
         relativeTo style: Font.TextStyle = .body,
         tracking: CGFloat = 0,
         lineHeight: CGFloat? = nil) {
        _size = ScaledMetric(wrappedValue: size, relativeTo: style)
        self.weight = weight
        self.tracking = tracking
        self.leading = lineHeight.map { $0 / size }
    }

    func body(content: Content) -> some View {
        content
            .font(.custom(Archivo.name(for: weight), fixedSize: size))
            .tracking(tracking)
            .lineSpacing(leading.map { max(0, size * ($0 - 1)) } ?? 0)
    }
}

extension View {

    /// Ett steg ur typskalan. Det här är vägen skärmarna ska gå: rollen står
    /// i `TypeScale`, inte i vyn.
    func trFont(_ step: TypeScale.Step) -> some View {
        trFont(step.size, step.weight.swiftUI,
               relativeTo: step.style.swiftUI,
               tracking: step.tracking, lineHeight: step.lineHeight)
    }

    func trFont(_ size: CGFloat,
                _ weight: Font.Weight,
                relativeTo style: Font.TextStyle = .body,
                tracking: CGFloat = 0,
                lineHeight: CGFloat? = nil) -> some View {
        modifier(TRFont(size, weight, relativeTo: style,
                        tracking: tracking, lineHeight: lineHeight))
    }

    /// Brödtext, samma som ingressen på startskärmen. §3: 400, 15/21, muted.
    func trBody() -> some View {
        trFont(TypeScale.body)
            .foregroundStyle(TR.muted)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Den mindre raden: förtydliganden och varningar under brödtexten.
    func trFootnote() -> some View {
        trFont(TypeScale.footnote)
            .foregroundStyle(TR.faint)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Rubrik inuti ett kort. Samma vikt och storlek som etiketten under
    /// Tala-knappen, så att de två skärmarna låter lika i tonen.
    func trHeading() -> some View {
        trFont(TypeScale.heading)
            .foregroundStyle(TR.text)
            .fixedSize(horizontal: false, vertical: true)
    }
}

// MARK: - Återanvända former

/// Ett kort i Tech Rabbit-formen: fältets yta, radie 22 och 1,5 punkters
/// kant — samma form som textfältet på startskärmen. Guiden och Om byggs
/// av de här korten i stället för systemets listrader, så att de två
/// skärmarna ser ut att höra till samma app.
struct TRCard<Content: View>: View {
    private let spacing: CGFloat
    private let content: Content

    init(spacing: CGFloat = Metric.gapSM, @ViewBuilder content: () -> Content) {
        self.spacing = spacing
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: spacing) {
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Metric.cardPad)
        .background(TR.fieldFill,
                    in: RoundedRectangle(cornerRadius: Metric.cardRadius))
        .overlay(RoundedRectangle(cornerRadius: Metric.cardRadius)
            .strokeBorder(TR.border, lineWidth: Metric.edge))
    }
}

/// Trycket, rev-ux 4.2 A12: allt tryckbart skalas till 0,97 med `snappy`,
/// och något händer inom 100 ms.
///
/// Med Reducera rörelse påslaget är zoomen borta och trycket tonar i
/// stället. En knapp som inte svarar alls känns trasig, och inställningen
/// gäller rörelse, inte återkoppling.
///
/// `@Environment` läses i en egen vy och inte i stilen: en `ButtonStyle` är
/// ingen vy, och miljön kommer inte in i den.
struct TRPress: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        Face(pressed: configuration.isPressed, label: configuration.label)
    }

    private struct Face<Label: View>: View {
        let pressed: Bool
        let label: Label

        @Environment(\.accessibilityReduceMotion) private var reduceMotion

        var body: some View {
            label
                .scaleEffect(pressed && !reduceMotion ? Move.pressScale : 1)
                .opacity(pressed && reduceMotion ? Move.pressDim : 1)
                .animation(Move.press(reduced: reduceMotion), value: pressed)
        }
    }
}

/// En knapp i samma form som Rensa och Skicka: piller, 52 högt, ikon- och
/// textvikt ur §3. Grön när den är vägen vidare, annars kortets yta med
/// kant.
struct TRButton: View {
    let title: String
    var filled = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .trFont(TypeScale.button)
                .foregroundStyle(filled ? TR.onGreen : TR.text)
                .frame(maxWidth: .infinity)
                .frame(height: Metric.controlHeight)
                .background(filled ? TR.green : TR.card, in: Capsule())
                .overlay(Capsule().strokeBorder(
                    filled ? TR.green : TR.dimDot, lineWidth: Metric.hairline))
        }
        .buttonStyle(TRPress())
    }
}

/// En liten etikett, som "kontrollera själv" i guiden.
struct TRBadge: View {
    let text: String

    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .trFont(TypeScale.badge)
            .foregroundStyle(TR.faint)
            .padding(.horizontal, Metric.badgeX)
            .padding(.vertical, Metric.gapTiny)
            .background(TR.card, in: Capsule())
            .overlay(Capsule().strokeBorder(TR.border, lineWidth: Metric.hairline))
            .fixedSize()
    }
}

/// Ramen kring guiden och Om: samma bakgrund som startskärmen, rubriken i
/// Archivo i navigationsraden och en grön Klar-knapp. Innehållet rullar.
struct TRSheet<Content: View>: View {
    private let title: String
    private let close: () -> Void
    private let content: Content

    init(_ title: String,
         close: @escaping () -> Void,
         @ViewBuilder content: () -> Content) {
        self.title = title
        self.close = close
        self.content = content()
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Metric.gapML) {
                    content
                }
                .padding(.horizontal, Metric.side)
                .padding(.top, Metric.gapS)
                .padding(.bottom, Metric.gapHuge)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(TRBackdrop())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Text(title)
                        .trFont(TypeScale.heading)
                        .foregroundStyle(TR.text)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Klar", action: close)
                        .trFont(TypeScale.button)
                        .foregroundStyle(TR.green)
                }
            }
            .toolbarBackground(TR.bg, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
        .preferredColorScheme(.dark)
        .tint(TR.green)
    }
}

// MARK: - Scrollen, §4.3

/// Hur långt sidan är scrollad, läst ur layouten och inte ur en tid.
///
/// Huvudet kollapsar med fingret: står scrollen still står huvudet still, och
/// drar man tillbaka växer det tillbaka. En animation på en tid hade lossnat
/// från fingret i samma stund som någon scrollade snabbt.
struct TRScrollOffset: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

extension View {
    /// Mäter hur långt sidan är scrollad inom koordinatrummet `space`.
    /// Läggs på innehållet inne i en `ScrollView`.
    func trMeasureScroll(in space: String) -> some View {
        background(GeometryReader { inner in
            Color.clear.preference(key: TRScrollOffset.self,
                                   value: -inner.frame(in: .named(space)).minY)
        })
    }
}

// MARK: - Känsligt innehåll

/// Oskärpa över skärmen när appen inte är i förgrunden.
///
/// Utkastet i fältet är transkriptet, och transkriptet är känsligt enligt
/// app-profilen. iOS fotograferar skärmen när appen lämnas, och bilden
/// ligger kvar i app switchern tills appen öppnas igen — synlig för var och
/// en som får telefonen i handen.
///
/// Oskärpan läggs på vid `inactive` och inte först vid `background`:
/// fotografiet tas under övergången. Laget tonar in med samma crossfade som
/// reducerad rörelse använder, och haptiken rörs inte.
struct TRShield: ViewModifier {
    let hidden: Bool

    func body(content: Content) -> some View {
        content
            .blur(radius: hidden ? Metric.shieldBlur : 0)
            .overlay {
                if hidden {
                    TR.bg.opacity(0.55).ignoresSafeArea().allowsHitTesting(false)
                }
            }
            .animation(.easeInOut(duration: Move.swap), value: hidden)
    }
}

extension View {
    /// Döljer skärmen medan den inte är i förgrunden. Ett ställe, inte ett
    /// per vy.
    func trShield(_ hidden: Bool) -> some View {
        modifier(TRShield(hidden: hidden))
    }
}

// MARK: - Bakgrunden, §5

/// Det mörka planet uppe, det gröna bandet nere med en linje längs kanten,
/// och glöden bakom knappen.
///
/// Ritad i underlagets 390 × 844 och skalad "xMidYMid slice", alltså så att
/// den täcker hela skärmen och skär bort det som hamnar utanför. Ingen
/// animation, ingen träffyta, och dold för VoiceOver.
struct TRBackdrop: View {
    private static let w: CGFloat = 390
    private static let h: CGFloat = 844

    var body: some View {
        GeometryReader { geo in
            let k = max(geo.size.width / Self.w, geo.size.height / Self.h)
            ZStack {
                TR.bg
                plane
                band
                edge
                glow
            }
            .frame(width: Self.w, height: Self.h)
            .scaleEffect(k)
            .frame(width: geo.size.width, height: geo.size.height)
            .clipped()
        }
        .ignoresSafeArea()
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    private var plane: some View {
        Self.planePath.fill(
            LinearGradient(
                colors: [TR.plane.opacity(0.9), TR.plane.opacity(0)],
                startPoint: UnitPoint(x: 0, y: 0),
                endPoint: UnitPoint(x: 0.6, y: 1)))
    }

    private var band: some View {
        Self.bandPath(closed: true).fill(
            LinearGradient(
                colors: [TR.green.opacity(0), TR.green.opacity(0.16)],
                startPoint: UnitPoint(x: 1, y: 0),
                endPoint: UnitPoint(x: 0, y: 1)))
    }

    private var edge: some View {
        Self.bandPath(closed: false).stroke(TR.bandEdge, lineWidth: Metric.edge)
    }

    /// Radiell glöd med centrum (195, 700) och radie 230.
    private var glow: some View {
        Rectangle().fill(
            RadialGradient(
                colors: [TR.green.opacity(0.22), TR.green.opacity(0)],
                center: UnitPoint(x: 195 / Self.w, y: 700 / Self.h),
                startRadius: 0,
                endRadius: 230))
    }

    // "M0 0 H390 V250 C 320 330, 230 300, 170 210 C 120 130, 70 50, 0 30 Z"
    private static var planePath: Path {
        Path { p in
            p.move(to: CGPoint(x: 0, y: 0))
            p.addLine(to: CGPoint(x: w, y: 0))
            p.addLine(to: CGPoint(x: w, y: 250))
            p.addCurve(to: CGPoint(x: 170, y: 210),
                       control1: CGPoint(x: 320, y: 330),
                       control2: CGPoint(x: 230, y: 300))
            p.addCurve(to: CGPoint(x: 0, y: 30),
                       control1: CGPoint(x: 120, y: 130),
                       control2: CGPoint(x: 70, y: 50))
            p.closeSubpath()
        }
    }

    // "M390 300 C 290 380, 300 540, 190 640 C 120 700, 50 750, 0 844"
    // Ytan stänger med "H390 Z"; linjen längs kanten gör det inte.
    private static func bandPath(closed: Bool) -> Path {
        Path { p in
            p.move(to: CGPoint(x: w, y: 300))
            p.addCurve(to: CGPoint(x: 190, y: 640),
                       control1: CGPoint(x: 290, y: 380),
                       control2: CGPoint(x: 300, y: 540))
            p.addCurve(to: CGPoint(x: 0, y: h),
                       control1: CGPoint(x: 120, y: 700),
                       control2: CGPoint(x: 50, y: 750))
            guard closed else { return }
            p.addLine(to: CGPoint(x: w, y: h))
            p.closeSubpath()
        }
    }
}

// MARK: - Ikonerna

/// En form ritad i underlagets 24 × 24-rutnät och skalad till den storlek
/// den får. Streckbredden skalas med, så att formen ser likadan ut hur stor
/// den än ritas.
struct TRGlyph: Shape {
    let draw: (inout Path) -> Void

    func path(in rect: CGRect) -> Path {
        var p = Path()
        draw(&p)
        let k = min(rect.width, rect.height) / 24
        return p.applying(CGAffineTransform(scaleX: k, y: k))
    }
}

enum TRIcon {

    /// Tech Rabbits kaninsymbol: två öron och ett huvud. Formen är hämtad
    /// ordagrant ur underlagets egen källfil
    /// (`source/TR Tap Talk Live.dc.html`), inte ritad på nytt.
    static func rabbit(_ size: CGFloat, color: Color) -> some View {
        TRGlyph { p in
            // Vänster öra.
            p.move(to: CGPoint(x: 9.2, y: 10.5))
            p.addCurve(to: CGPoint(x: 8.1, y: 3.6),
                       control1: CGPoint(x: 8.0, y: 8.6),
                       control2: CGPoint(x: 7.6, y: 5.3))
            p.addCurve(to: CGPoint(x: 10.0, y: 3.6),
                       control1: CGPoint(x: 8.5, y: 2.5),
                       control2: CGPoint(x: 9.7, y: 2.5))
            p.addCurve(to: CGPoint(x: 9.7, y: 10.5),
                       control1: CGPoint(x: 10.5, y: 5.2),
                       control2: CGPoint(x: 10.3, y: 8.2))
            p.closeSubpath()
            // Höger öra.
            p.move(to: CGPoint(x: 14.8, y: 10.5))
            p.addCurve(to: CGPoint(x: 15.9, y: 3.6),
                       control1: CGPoint(x: 16.0, y: 8.6),
                       control2: CGPoint(x: 16.4, y: 5.3))
            p.addCurve(to: CGPoint(x: 14.0, y: 3.6),
                       control1: CGPoint(x: 15.5, y: 2.5),
                       control2: CGPoint(x: 14.3, y: 2.5))
            p.addCurve(to: CGPoint(x: 14.3, y: 10.5),
                       control1: CGPoint(x: 13.5, y: 5.2),
                       control2: CGPoint(x: 13.7, y: 8.2))
            p.closeSubpath()
            // Huvudet: ellips med centrum (12, 15), rx 4.6, ry 4.3.
            p.addEllipse(in: CGRect(x: 12 - 4.6, y: 15 - 4.3,
                                    width: 9.2, height: 8.6))
        }
        .fill(color)
        .frame(width: size, height: size)
    }

    /// Mikrofonen i Tala-knappen. Halvcirkeln under kapseln är ritad med två
    /// bézierkurvor i stället för en båge: en båge hade varit beroende av
    /// åt vilket håll SwiftUI räknar vinklar, och den skillnaden syns först
    /// på en telefon.
    static func mic(_ size: CGFloat, color: Color) -> some View {
        TRGlyph { p in
            p.addRoundedRect(in: CGRect(x: 9, y: 3, width: 6, height: 11),
                             cornerSize: CGSize(width: 3, height: 3))
            let k: CGFloat = 0.5523 * 6.5
            p.move(to: CGPoint(x: 5.5, y: 11))
            p.addCurve(to: CGPoint(x: 12, y: 17.5),
                       control1: CGPoint(x: 5.5, y: 11 + k),
                       control2: CGPoint(x: 12 - k, y: 17.5))
            p.addCurve(to: CGPoint(x: 18.5, y: 11),
                       control1: CGPoint(x: 12 + k, y: 17.5),
                       control2: CGPoint(x: 18.5, y: 11 + k))
            p.move(to: CGPoint(x: 12, y: 17.5))
            p.addLine(to: CGPoint(x: 12, y: 21))
        }
        .stroke(color, style: stroke(size))
        .frame(width: size, height: size)
    }

    /// Papperskorgen på Rensa. "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13"
    static func trash(_ size: CGFloat, color: Color) -> some View {
        TRGlyph { p in
            p.move(to: CGPoint(x: 4, y: 7))
            p.addLine(to: CGPoint(x: 20, y: 7))
            p.move(to: CGPoint(x: 9, y: 7))
            p.addLine(to: CGPoint(x: 9, y: 4))
            p.addLine(to: CGPoint(x: 15, y: 4))
            p.addLine(to: CGPoint(x: 15, y: 7))
            p.move(to: CGPoint(x: 6, y: 7))
            p.addLine(to: CGPoint(x: 7, y: 20))
            p.addLine(to: CGPoint(x: 17, y: 20))
            p.addLine(to: CGPoint(x: 18, y: 7))
        }
        .stroke(color, style: stroke(size))
        .frame(width: size, height: size)
    }

    /// Pappersplanet på Skicka. "M4 12l16-8-6 16-2-6z"
    static func plane(_ size: CGFloat, color: Color) -> some View {
        TRGlyph { p in
            p.move(to: CGPoint(x: 4, y: 12))
            p.addLine(to: CGPoint(x: 20, y: 4))
            p.addLine(to: CGPoint(x: 14, y: 20))
            p.addLine(to: CGPoint(x: 12, y: 14))
            p.closeSubpath()
        }
        .stroke(color, style: stroke(size))
        .frame(width: size, height: size)
    }

    private static func stroke(_ size: CGFloat) -> StrokeStyle {
        StrokeStyle(lineWidth: 2.2 * size / 24, lineCap: .round, lineJoin: .round)
    }
}

// MARK: - Ringarna och glöden, §7

/// De två ringarna runt Tala-knappen.
///
/// För varje ring: p = (tid / period + förskjutning) mod 1, skala
/// 1 + p × amp, opacitet (1 − p) × maxOp. Ringarna ligger en halv period
/// isär. Linjärt, i all oändlighet.
///
/// Med Reducera rörelse påslaget står de stilla. Underlaget säger stilla och
/// inte borta, eftersom ringarna är en del av formen och inte bara en
/// effekt.
struct TRRings: View {
    let listening: Bool
    let still: Bool

    private var period: Double { listening ? 1.6 : 2.8 }
    /// Vilopulsen är dämpad, punkt 9 i ändringslistan: ringarna växte 22 %
    /// och tonade från full styrka på en skärm där ingenting hände.
    /// Lyssnarläget är orört — där är pulsen beskedet om att mikrofonen är
    /// på.
    private var amp: Double { listening ? 0.35 : 0.14 }
    private var maxOp: Double { listening ? 1.0 : 0.5 }
    private var color: Color { listening ? TR.red : TR.green }
    private var restOpacity: Double { listening ? 0.9 : 0.45 }

    var body: some View {
        Group {
            if still {
                ZStack {
                    ring(inset: 0).opacity(restOpacity)
                    ring(inset: Metric.gapM).opacity(restOpacity)
                }
            } else {
                TimelineView(.animation) { timeline in
                    let t = timeline.date.timeIntervalSinceReferenceDate
                    ZStack {
                        pulse(inset: 0, at: t, offset: 0)
                        pulse(inset: Metric.gapM, at: t, offset: 0.5)
                    }
                }
            }
        }
        .frame(width: Metric.micRing, height: Metric.micRing)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    private func pulse(inset: CGFloat, at t: Double, offset: Double) -> some View {
        let p = (t / period + offset).truncatingRemainder(dividingBy: 1)
        return ring(inset: inset)
            .scaleEffect(1 + p * amp)
            .opacity((1 - p) * maxOp)
    }

    private func ring(inset: CGFloat) -> some View {
        Circle()
            .strokeBorder(color, lineWidth: Metric.edge)
            .padding(inset)
    }
}

/// Glöden runt knappen. I vila andas den: 0,35 + 0,15 × sin(2π × tid / 2,8).
/// Under inspelning står den still på rött 0,45. Med Reducera rörelse
/// andas den inte.
enum TRGlow {
    static func opacity(listening: Bool, still: Bool, at t: Double) -> Double {
        if listening { return 0.45 }
        guard !still else { return 0.3 }
        // Andningen är dämpad med vilopulsen, punkt 9: ±0,06 i stället för
        // ±0,15 runt ett lägre medel.
        return 0.3 + 0.06 * sin(2 * .pi * t / 2.8)
    }

    /// Värdet står i `Metric.glowRadius`, med skälet till halveringen.
    static let radius: CGFloat = Metric.glowRadius
}
