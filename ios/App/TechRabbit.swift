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

    static let bg = hex(0x0B0B0B)
    static let card = hex(0x1A1A1A)
    static let border = hex(0x2E2E2E)
    static let text = hex(0xF2F2F2)
    static let muted = hex(0xA8A8A8)
    static let faint = hex(0x8A8A8A)
    static let disabled = hex(0x5A5A5A)
    static let green = hex(0x1CF038)
    static let red = hex(0xE0583E)
    static let onGreen = hex(0x0B0B0B)
    static let plane = hex(0x1C1C1C)

    /// Prickens grå i vila, och Rensas kant när knappen är aktiv. §6.
    static let dimDot = hex(0x4A4A4A)
    /// Linjen längs det gröna bandets kant. §5.
    static let bandEdge = hex(0x2A2A2A)

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

/// En textstorlek ur underlaget som ändå växer med Dynamic Type.
///
/// Underlaget förutsätter Archivo. Typsnittsfilerna finns inte i det här
/// repot, så här används systemtypsnittet i samma vikter och storlekar.
/// Läggs Archivo in är det den här enda raden som byts: `.system(size:…)`
/// blir `.custom("Archivo", size:…)`.
///
/// `@ScaledMetric` räknar om punkterna mot en textstil, så att 17 pt blir
/// större när användaren har ställt upp textstorleken i Inställningar.
/// Radavståndet räknas om i samma takt, därför ett förhållande och inte ett
/// fast tal.
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
            .font(.system(size: size, weight: weight))
            .tracking(tracking)
            .lineSpacing(leading.map { max(0, size * ($0 - 1)) } ?? 0)
    }
}

extension View {
    func trFont(_ size: CGFloat,
                _ weight: Font.Weight,
                relativeTo style: Font.TextStyle = .body,
                tracking: CGFloat = 0,
                lineHeight: CGFloat? = nil) -> some View {
        modifier(TRFont(size, weight, relativeTo: style,
                        tracking: tracking, lineHeight: lineHeight))
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
        Self.bandPath(closed: false).stroke(TR.bandEdge, lineWidth: 1.5)
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
    private var amp: Double { listening ? 0.35 : 0.22 }
    private var maxOp: Double { listening ? 1.0 : 0.8 }
    private var color: Color { listening ? TR.red : TR.green }
    private var restOpacity: Double { listening ? 0.9 : 0.55 }

    var body: some View {
        Group {
            if still {
                ZStack {
                    ring(inset: 0).opacity(restOpacity)
                    ring(inset: 12).opacity(restOpacity)
                }
            } else {
                TimelineView(.animation) { timeline in
                    let t = timeline.date.timeIntervalSinceReferenceDate
                    ZStack {
                        pulse(inset: 0, at: t, offset: 0)
                        pulse(inset: 12, at: t, offset: 0.5)
                    }
                }
            }
        }
        .frame(width: 132, height: 132)
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
            .strokeBorder(color, lineWidth: 1.5)
            .padding(inset)
    }
}

/// Glöden runt knappen. I vila andas den: 0,35 + 0,15 × sin(2π × tid / 2,8).
/// Under inspelning står den still på rött 0,45. Med Reducera rörelse
/// andas den inte.
enum TRGlow {
    static func opacity(listening: Bool, still: Bool, at t: Double) -> Double {
        if listening { return 0.45 }
        guard !still else { return 0.35 }
        return 0.35 + 0.15 * sin(2 * .pi * t / 2.8)
    }

    /// Underlaget anger `box-shadow: 0 0 36px`. CSS räknar radien som hela
    /// oskärpan, SwiftUI som halva, därför 18 och inte 36.
    static let radius: CGFloat = 18
}
