import Foundation

/// Tillståndet i Tap & Talk, skilt från vyn så att det går att mäta med tester.
///
/// Tre regler bär hela funktionen och testas i RostsvarTests:
/// mikrofonen är öppen bara under inspelning, Avbryt rensar utkastet utan att
/// något skickas, och ett byte av konversation rensar utkastet.
struct DraftState: Equatable {
    enum Phase: Equatable {
        case idle        // vilar, utkastet får finnas kvar
        case asking      // frågar om mikrofon och taligenkänning
        case recording   // mikrofonen är öppen
        case finishing   // mikrofonen stängd, texten slutförs
        case sending     // överlämnas till Meddelanden
    }

    private(set) var phase: Phase = .idle
    private(set) var draft: String = ""
    private(set) var conversation: String?

    /// Sant bara när mikrofonen ska vara öppen. Allt annat ska ha stängt den.
    var microphoneIsOpen: Bool { phase == .recording }

    var trimmedDraft: String {
        draft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var hasDraft: Bool { !trimmedDraft.isEmpty }

    /// Skicka går bara att trycka på när inget annat pågår och det finns text.
    var canSend: Bool { phase == .idle && hasDraft }

    var canTalk: Bool { phase == .idle || phase == .recording }

    /// Avbryt går att trycka på så snart det finns något att avbryta. Aldrig
    /// medan ett meddelande skickas: då är det för sent att ångra sig.
    var canCancel: Bool { phase != .sending && (phase != .idle || hasDraft) }

    // MARK: - Konversationen

    /// Tap & Talk blir aktivt i en konversation. Byts konversationen rensas
    /// utkastet, så att text aldrig följer med till fel mottagare.
    mutating func becomeActive(conversation identifier: String) {
        if conversation != identifier {
            conversation = identifier
            draft = ""
        }
        leaveUnlessSending()
    }

    /// Tap & Talk lämnas. Utkastet rensas.
    mutating func resignActive() {
        draft = ""
        leaveUnlessSending()
    }

    /// En pågående skickning får bara avslutas av finishSending. Att fälla
    /// ihop panelen, lämna Tap & Talk eller komma tillbaka ska aldrig göra ett
    /// utkast som redan lämnats till Meddelanden skickbart en gång till.
    private mutating func leaveUnlessSending() {
        guard phase != .sending else { return }
        phase = .idle
    }

    // MARK: - Diktering

    mutating func ask() {
        guard phase == .idle else { return }
        phase = .asking
    }

    mutating func beginRecording() {
        draft = ""
        phase = .recording
    }

    mutating func transcribe(_ text: String) {
        guard phase == .recording || phase == .finishing else { return }
        draft = text
    }

    mutating func finishRecording() {
        guard phase == .recording else { return }
        phase = .finishing
    }

    /// Allt pågående arbete avslutas. Utkastet lämnas orört.
    mutating func rest() {
        leaveUnlessSending()
    }

    /// Avbryt. Utkastet rensas och ingenting skickas. Går inte att använda
    /// medan ett meddelande skickas.
    mutating func cancel() {
        guard phase != .sending else { return }
        draft = ""
        phase = .idle
    }

    // MARK: - Skicka

    mutating func beginSending() {
        guard canSend else { return }
        phase = .sending
    }

    /// Enda vägen ut ur .sending. Vid bekräftat fel behålls utkastet så att
    /// det går att skicka om.
    mutating func finishSending(failed: Bool) {
        guard phase == .sending else { return }
        if !failed { draft = "" }
        phase = .idle
    }
}
