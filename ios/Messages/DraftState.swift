import Foundation

/// Tillståndet i Röstsvar, skilt från vyn så att det går att mäta med tester.
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

    /// Avbryt går att trycka på så snart det finns något att avbryta.
    var canCancel: Bool { phase != .idle || hasDraft }

    // MARK: - Konversationen

    /// Röstsvar blir aktivt i en konversation. Byts konversationen rensas
    /// utkastet, så att text aldrig följer med till fel mottagare.
    mutating func becomeActive(conversation identifier: String) {
        if conversation != identifier {
            conversation = identifier
            draft = ""
        }
        phase = .idle
    }

    /// Röstsvar lämnas. Utkastet rensas.
    mutating func resignActive() {
        draft = ""
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
        phase = .idle
    }

    /// Avbryt. Utkastet rensas och ingenting skickas.
    mutating func cancel() {
        draft = ""
        phase = .idle
    }

    // MARK: - Skicka

    mutating func beginSending() {
        guard canSend else { return }
        phase = .sending
    }

    mutating func finishSending(failed: Bool) {
        if !failed { draft = "" }
        phase = .idle
    }
}
