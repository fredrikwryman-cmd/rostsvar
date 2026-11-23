import SwiftUI

/// Appens utseende på ett ställe: svart bakgrund och grön accent, samma linje
/// som Androids `Look.java` och som tangentbordet. Grönt är kupolens färg i
/// appikonen.
///
/// Värdena står som hextal, exakt de samma som på Android-sidan, så att de
/// går att jämföra rad för rad i stället för att översättas för hand.
enum Look {

    static let bg = hex(0x000000)
    static let ink = hex(0xF2F4F3)
    static let dim = hex(0x9AA3A0)
    static let green = hex(0x3FE08A)
    static let greenDeep = hex(0x16A35A)
    /// Mörk text på grön knapp.
    static let darkInk = hex(0x05140C)
    static let field = hex(0x111315)
    static let border = hex(0x2A2F31)
    static let off = hex(0x3A3F42)
    /// Varningens gula, används bara när något fattas.
    static let warn = hex(0xFFC46B)

    static func hex(_ value: Int) -> Color {
        Color(.sRGB,
              red: Double((value >> 16) & 0xFF) / 255,
              green: Double((value >> 8) & 0xFF) / 255,
              blue: Double(value & 0xFF) / 255,
              opacity: 1)
    }
}
