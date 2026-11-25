import UIKit

/// Haptiken på ett ställe: hela kartan ur rev-ux avsnitt 7, en spärr på
/// 80 ms, och en av-knapp.
///
/// Låg förut som fyra rader i botten av `HomeScreen.swift`, under namnet
/// `Buzz`, och bara appen hade dem. Tangentbordet och iMessage-tillägget
/// vibrerade inte alls, och ingen av de tre gick att stänga av.
///
/// Kartan är händelser, inte styrkor. Den som lägger till en knapp ska
/// behöva säga vad knappen gör — `.press`, `.success`, `.stateChange` — och
/// aldrig välja mellan `light` och `medium`. Då hamnar två knappar med samma
/// uppgift i samma styrka av sig själva.
///
/// Haptiken ligger kvar vid reducerad rörelse. Den är inte rörelse på
/// skärmen, och för den som slagit på inställningen är den ofta hela
/// bekräftelsen.
enum Haptics {

    /// Händelserna i kartan. Styrkan står i `feedback`, en gång.
    enum Event {
        /// Tryck på primär- eller snabbhandling: Tala, Rensa, Skicka,
        /// Guide, Om.
        case press
        /// Tangent, segment, steg i en väljare, karusellsnäpp.
        case key
        /// Ett ark snäpper upp eller ned, ett objekt låser i läge.
        case snap
        /// En uppgift lyckades, eller en stor handling landade.
        case success
        /// Fel, ogiltigt, över gräns.
        case failure
        /// Före en oåterkallelig handling.
        case warning
        /// Tillståndsbyte utan navigering: bubblans lägen, fältets lägen.
        case stateChange
        /// Något löses upp och tas bort.
        case remove
        /// Känsligt innehåll döljs eller visas med en gest.
        case privacy
        /// Texten lämnas vidare.
        case send
        /// Mikrofonen öppnas. Tyngre än när den stängs.
        case recordStart
        /// Mikrofonen stängs.
        case recordStop
    }

    // MARK: - Av-knappen

    /// Nyckeln av-knappen skriver. Samma namn på Android.
    static let key = "haptik_av"

    /// Sant när haptiken är påslagen. Standard är på.
    ///
    /// Varje mål har sin egen `UserDefaults`: appen, tangentbordet och
    /// iMessage-tillägget delar ingen behållare, eftersom appen inte har
    /// någon appgrupp. Av-knappen i Om gäller därför appens egna ytor.
    /// Tangentbordets tangenter följer systemets eget ljud- och
    /// vibrationsval för tangentbord, som användaren redan styr i
    /// Inställningar.
    static var enabled: Bool {
        get { !UserDefaults.standard.bool(forKey: key) }
        set { UserDefaults.standard.set(!newValue, forKey: key) }
    }

    // MARK: - Spärren

    /// Två haptiker närmare än så här känns som en enda smet, och i värsta
    /// fall som att telefonen skakar. Tiden står i rev-ux avsnitt 7.
    static let gap: TimeInterval = 0.08

    private static var last: TimeInterval = 0

    /// Vibrerar för en händelse, om haptiken är på och spärren släppt.
    ///
    /// Alltid i par med något synligt: en bock, ett byte av färg, ett ord
    /// som ändras. En vibration utan något på skärmen säger ingenting till
    /// den som inte höll telefonen i handen just då.
    static func fire(_ event: Event) {
        guard enabled else { return }
        let now = Date.timeIntervalSinceReferenceDate
        guard now - last >= gap else { return }
        last = now
        feedback(event)
    }

    /// Kartan. Ett ställe, och det enda stället styrkorna står.
    private static func feedback(_ event: Event) {
        switch event {
        case .key:
            UISelectionFeedbackGenerator().selectionChanged()
        case .success:
            UINotificationFeedbackGenerator().notificationOccurred(.success)
        case .failure:
            UINotificationFeedbackGenerator().notificationOccurred(.error)
        case .warning:
            UINotificationFeedbackGenerator().notificationOccurred(.warning)
        case .stateChange, .recordStart:
            // Mikrofonen öppnas tyngre än den stängs. Det är vänt mot Tech
            // Rabbit-underlaget §8 och mot appens fyra första rader, och det
            // är avsiktligt: ändringslistans rad om Tala-knappen och rev-ux
            // avsnitt 7 säger start = medium, stopp = light. Att öppna
            // mikrofonen är det som behöver kännas.
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        case .snap, .privacy:
            UIImpactFeedbackGenerator(style: .soft).impactOccurred()
        case .remove:
            UIImpactFeedbackGenerator(style: .rigid).impactOccurred()
        case .press, .send, .recordStop:
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        }
    }
}
