import UIKit

/// Tech Rabbit-paletten som råa värden, §2 i underlaget.
///
/// Låg förut bara i `TechRabbit.swift`, som bara appmålet bygger. Därför hade
/// tangentbordet en egen uppsättning färger med ett annat grönt, och
/// iMessage-tillägget använde systemets `.systemBackground` och `.systemBlue`
/// — tre ytor i samma app som såg ut att komma från tre olika appar.
///
/// Här står värdena en gång. `TR` i `TechRabbit.swift` gör dem till SwiftUI-
/// färger för appen, och de två tilläggen läser dem som `UIColor` genom
/// `ui(_:)`. Androids `Look` har samma värden skrivna i Java; de två
/// plattformarna kan inte dela en fil, men de ska inte skilja sig i värde.
enum Palette {

    static let bg = 0x0B0B0B
    static let card = 0x1A1A1A
    static let border = 0x2E2E2E
    static let text = 0xF2F2F2
    static let muted = 0xA8A8A8
    static let faint = 0x8A8A8A
    static let disabled = 0x5A5A5A
    static let green = 0x1CF038
    static let red = 0xE0583E
    static let onGreen = 0x0B0B0B
    static let plane = 0x1C1C1C

    /// Prickens grå i vila, och Rensas kant när knappen är aktiv. §6.
    static let dimDot = 0x4A4A4A
    /// Linjen längs det gröna bandets kant. §5.
    static let bandEdge = 0x2A2A2A

    /// Det gröna nedtonat, för en knapp som är nedtryckt eller som lyssnar.
    static let greenDeep = 0x12A426

    /// Tangentens yta i tangentbordet, och linjen runt den. Samma värden som
    /// tangentbordet redan hade (white 0,14 och 0,22), flyttade hit utan att
    /// ändras: att ytan bara har 1,35:1 mot tangentbordets botten är punkt 19
    /// i granskningen och hör inte till det här passet.
    static let keyFill = 0x242424
    static let keyEdge = 0x383838

    static func ui(_ value: Int) -> UIColor {
        UIColor(red: CGFloat((value >> 16) & 0xFF) / 255,
                green: CGFloat((value >> 8) & 0xFF) / 255,
                blue: CGFloat(value & 0xFF) / 255,
                alpha: 1)
    }
}
