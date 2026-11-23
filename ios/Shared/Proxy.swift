import Foundation

/// Reglerna för hur tangentbordet skriver i ett annat programs fält.
///
/// Ett `UITextDocumentProxy` går bara att skriva i och radera bakåt i. En ny
/// tolkning kan därför inte ersätta den gamla rakt av: tangentbordet håller
/// reda på vad det självt har skrivit, raderar precis så många tecken och
/// lägger till resten.
///
/// Det bygger på ett antagande som kan brista: att det som ligger närmast
/// före markören är vår text. Flyttar användaren markören, markerar text
/// eller skriver själv någon annanstans i fältet stämmer det inte längre, och
/// då skulle `deleteBackward` äta användarens egen text. Reglerna nedan är
/// skilda från tangentbordet just för att den gränsen ska gå att mäta med
/// tester, utan telefon.
enum ProxyWriting {

    /// Hur mycket av vår egen text vi fortfarande äger, och alltså får
    /// radera.
    ///
    /// - Parameters:
    ///   - mine: det dikteringen själv har skrivit sedan den startade.
    ///   - before: texten närmast före markören, så som fältet uppger den.
    ///             Nil betyder att programmet inte lämnar ut den.
    ///   - selected: markerad text, om någon är markerad.
    /// - Returns: `mine` när den fortfarande ligger före markören, annars en
    ///   tom sträng — då äger vi ingenting och raderar ingenting.
    static func owned(mine: String, before: String?, selected: String?) -> String {
        guard !mine.isEmpty else { return "" }
        // Är något markerat raderar deleteBackward markeringen, inte våra
        // tecken. Då håller vi oss borta.
        if let selected, !selected.isEmpty { return "" }
        // Lämnar programmet ingen text ifrån sig finns inget att jämföra
        // med. Då får vår egen bokföring gälla, som förut.
        guard let before else { return mine }
        return before.hasSuffix(mine) ? mine : ""
    }

    /// Vägen från det vi redan skrivit till den nya tolkningen: så många
    /// tecken ska raderas bakåt, och den här texten ska läggas till.
    ///
    /// Den gemensamma början behålls, så att en tolkning som bara växer
    /// aldrig raderar något alls.
    static func plan(text: String, mine: String) -> (erase: Int, insert: String) {
        let shared = text.commonPrefix(with: mine)
        return (erase: mine.count - shared.count,
                insert: String(text.dropFirst(shared.count)))
    }
}
