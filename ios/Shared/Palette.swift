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
    /// Kanten runt kort, fält och tangenter.
    ///
    /// Var 0x2E2E2E, och det gav uppmätt 1,45:1 mot bg och 1,28:1 mot card —
    /// kravet för en komponentgräns är 3:1. Fältets kant var i praktiken
    /// osynlig. 0x707070 klarar 3:1 mot varje yta kanten ligger mot: bg
    /// 3,97:1, card 3,51:1, keyFill 3,13:1.
    ///
    /// Ytorna själva går inte att rädda: card mot bg är 1,13:1 och kan inte
    /// nå 3:1 utan att kortet blir ljusgrått. Det är kanten som ska bära
    /// gränsen, och det är kanten som rättats.
    static let border = 0x707070
    static let text = 0xF2F2F2
    static let muted = 0xA8A8A8
    static let faint = 0x8A8A8A
    /// Texten på en knapp som inte går att trycka på.
    ///
    /// Var 0x5A5A5A: 2,52:1 mot card, mot kravet 4,5:1. 0x848484 ger 4,65:1.
    /// Att avslaget läge nu har nästan samma ljushet som faint är avsiktligt
    /// — läget bärs ändå av ytan, som är card och inte grön.
    static let disabled = 0x848484
    static let green = 0x1CF038
    static let red = 0xE0583E
    static let onGreen = 0x0B0B0B
    static let plane = 0x1C1C1C

    /// Prickens grå i vila, och Rensas kant när knappen är aktiv. §6.
    ///
    /// Var 0x4A4A4A: 2,22:1 mot bg. Både pricken och kanten bär betydelse —
    /// den ena vilolägets, den andra att knappen går att trycka på — så båda
    /// ska klara 3:1. 0x8A8A8A ger 5,70:1 mot bg och 5,04:1 mot card, och
    /// ligger ljusare än border, så den aktiva kanten syns mot den vilande.
    static let dimDot = 0x8A8A8A
    /// Linjen längs det gröna bandets kant. §5.
    static let bandEdge = 0x2A2A2A

    /// Det gröna nedtonat, för en knapp som är nedtryckt eller som lyssnar.
    static let greenDeep = 0x12A426

    /// Varningens gula, och ytan under den.
    ///
    /// Androids `Look` har haft de här två sedan varningsrutan på hemskärmen
    /// ritades; iOS hade dem inte, eftersom ingen yta här behövde dem förrän
    /// felytan i punkt 14. Värdena är hämtade därifrån ordagrant, inte valda
    /// på nytt: filens hela poäng är att de två plattformarna inte ska skilja
    /// sig i värde. Uppmätt ger `warn` mot `warnFill` 11,9:1 och mot `bg`
    /// 12,6:1.
    static let warn = 0xFFC46B
    static let warnFill = 0x1A1407

    /// Tangentens yta i tangentbordet, och linjen runt den.
    ///
    /// Ytan är kvar på 0x242424. Den har 1,27:1 mot tangentbordets botten och
    /// kan inte nå 3:1 utan att tangenterna blir mellangrå, vilket inte är
    /// ett tangentbord längre. Gränsen bärs av linjen i stället: 0x707070 ger
    /// 3,13:1 mot ytan och 3,97:1 mot botten, så tangenten går att skilja
    /// från både sin granne och bakgrunden.
    static let keyFill = 0x242424
    static let keyEdge = 0x707070

    static func ui(_ value: Int) -> UIColor {
        UIColor(red: CGFloat((value >> 16) & 0xFF) / 255,
                green: CGFloat((value >> 8) & 0xFF) / 255,
                blue: CGFloat(value & 0xFF) / 255,
                alpha: 1)
    }
}
