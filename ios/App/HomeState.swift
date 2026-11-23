import Foundation

/// Läget på dikteringsskärmen, skilt från själva skärmen så att reglerna går
/// att mäta med tester utan telefon. Samma uppbyggnad som Androids
/// `Home.java`, där `base` är texten som redan stod i fältet när dikteringen
/// startade och allt som hörs läggs efter den.
///
/// En regel bär hela typen, och det är samma regel som i `Dictation.ending`:
/// det som en gång hörts kastas aldrig. En tom tolkning får därför inte
/// skriva över fältet.
struct HomeState: Equatable {

    enum Phase: Equatable {
        /// Inget pågår. Fältet går att skriva i.
        case idle
        /// Mikrofon och taligenkänning frågas om.
        case asking
        /// Mikrofonen är öppen.
        case recording
        /// Användaren är klar, taltjänsten har ännu inte svarat.
        case finishing
    }

    private(set) var phase = Phase.idle

    /// Texten i fältet. Användaren får skriva i den själv när inget pågår.
    var draft = ""

    /// Texten som stod i fältet när dikteringen startade, med ett avslutande
    /// mellanslag. Sätts om vid varje start, så att en text användaren
    /// rättat för hand är den som nästa diktering läggs efter.
    private var base = ""

    /// Sant när den här omgången har hört något. Behövs för att skilja "det
    /// står text i fältet" från "något hördes just nu": stod det text där
    /// sedan förra gången får skärmen inte påstå att dikteringen lyckades.
    private(set) var heardSomething = false

    // MARK: - Läsningar

    var trimmed: String { draft.trimmingCharacters(in: .whitespacesAndNewlines) }
    var hasText: Bool { !trimmed.isEmpty }
    var isRecording: Bool { phase == .recording }
    /// Mikrofonen är öppen bara under inspelning — aldrig medan frågan
    /// ställs, och aldrig medan taltjänsten slutför.
    var microphoneIsOpen: Bool { phase == .recording }

    /// Tala-knappen. Går att trycka på för att starta, och för att avsluta.
    var canTalk: Bool { phase == .idle || phase == .recording }
    /// Skicka och Rensa väntar tills dikteringen är över: medan mikrofonen är
    /// öppen är Klar nästa steg, och en delningsruta över en öppen mikrofon
    /// vore ingen tjänst mot användaren.
    var canSend: Bool { hasText && phase == .idle }
    var canClear: Bool { hasText && phase == .idle }
    /// Fältet går att skriva i när inget pågår, som på Android.
    var canEdit: Bool { phase == .idle }

    // MARK: - Övergångar

    mutating func ask() {
        guard phase == .idle else { return }
        phase = .asking
    }

    /// Mikrofonen är igång. Grunden läses av här, inte vid tolkningarna, så
    /// att texten läggs efter det som redan står i fältet.
    mutating func beginRecording() {
        base = hasText ? trimmed + " " : ""
        heardSomething = false
        phase = .recording
    }

    /// En tolkning från taltjänsten. Delresultaten ersätter varandra och
    /// slutresultatet ersätter dem — därav grunden plus tolkningen, aldrig
    /// en påbyggnad. En tom tolkning rör ingenting.
    mutating func transcribe(_ text: String) {
        let clean = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        draft = base + clean
        heardSomething = true
    }

    mutating func finishRecording() {
        guard phase == .recording else { return }
        phase = .finishing
    }

    /// Går till vila. Texten står kvar, vad som än hände på vägen.
    mutating func rest() {
        phase = .idle
    }

    mutating func clear() {
        draft = ""
        base = ""
        heardSomething = false
        phase = .idle
    }
}
