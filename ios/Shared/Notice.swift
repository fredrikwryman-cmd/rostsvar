import Foundation

/// Beskeden om taltjänsten på iOS, på ett ställe.
///
/// Motsvarigheten på Android heter `Notices` och säger samma saker. De två
/// plattformarna kan inte dela en fil, men de ska inte säga olika saker om
/// samma fel — och det gjorde de: iOS-tangentbordet och appen hade var sin
/// mening om nätet, och Androids bubbla hade ingen gren för nätet alls och
/// föll till ett felnummer.
///
/// Varje mening säger tre saker: vad som hände, vad det betyder, och vad
/// användaren gör nu. Ingen av dem visar en felkod.
///
/// Ligger skild från `Dictation` av ett enda skäl: texterna ska gå att mäta
/// med tester utan telefon. `NoticeTests` gör det.
enum Notice {

    /// Nätet gick inte att nå.
    ///
    /// Nätet är inte hela sanningen, och det är därför meningen är längre än
    /// ett felmeddelande: svensk diktering på enheten finns på de flesta
    /// iPhone-modeller, den kräver bara att språket är tillagt. Vägen dit är
    /// samma väg guiden redan lär ut.
    static let offline =
        "Taltjänsten nådde inte nätet, så talet kunde inte tolkas. Svenska går "
        + "att diktera utan nät när språket finns under Inställningar → Allmänt "
        + "→ Tangentbord → Tangentbord. Tryck på Tala och prova igen."

    /// Taligenkänningen svarar inte alls på svenska just nu.
    static let unavailable =
        "Svensk taligenkänning är inte tillgänglig just nu. Lägg till svenska "
        + "under Inställningar → Allmänt → Tangentbord → Tangentbord, eller "
        + "prova igen om en stund."

    /// Mikrofonen fanns inte att läsa.
    static let noMicrophone =
        "Ingen mikrofon gick att läsa. Stäng andra appar som spelar in och "
        + "tryck på Tala igen."

    /// Mikrofonen vägrade starta.
    static let micWontStart =
        "Mikrofonen kunde inte starta. Tryck på Tala och prova igen."

    /// Ingenting hördes.
    static let noSpeech = "Hörde inget. Tryck på Tala och prova igen."

    /// Talet hördes men gick inte att tolka.
    static let unclear =
        "Taltjänsten kunde inte tolka talet. Tryck på Tala och prova igen."

    /// Taltjänsten svarade inte inom tidsgränsen och ingenting hade hörts.
    static let noAnswer =
        "Taltjänsten svarade inte. Tryck på Tala och prova igen."

    /// Taltjänsten slutade med fel, men något hade hörts och står kvar.
    /// Avslutsregeln: det som en gång hörts kastas aldrig, och användaren får
    /// aldrig höra att inget uppfattades när något gjorde det.
    static let salvaged = "Taltjänsten blev inte klar, men det här hördes."

    /// Felen från taltjänsten är inte skrivna för någon att läsa. Det här är.
    static func speechFailure(_ error: Error) -> String {
        let problem = error as NSError
        // 1110 betyder "no speech detected" och är det vanligaste av dem alla.
        if problem.code == 1110 || problem.code == 203 { return noSpeech }
        if problem.domain == NSURLErrorDomain { return offline }
        return unclear
    }
}
