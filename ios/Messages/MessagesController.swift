import AVFoundation
import Messages
import Speech
import UIKit

final class MessagesController: MSMessagesAppViewController {
    private enum Phase { case idle, asking, recording, finishing, sending }

    private var phase = Phase.idle
    private var generation = UUID()
    private var conversationKey: String?
    private var note = "Tryck på Tala för att svara."

    private let engine = AVAudioEngine()
    private let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "sv-SE"))
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private var hasTap = false
    private var audioActive = false

    private let status = UILabel()
    private let draft = UITextView()
    private let mic = UIButton(type: .system)
    private let stop = UIButton(type: .system)
    private let send = UIButton(type: .system)

    deinit { closeMicrophone() }

    // MARK: - Vy

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        status.font = .preferredFont(forTextStyle: .subheadline)
        status.adjustsFontForContentSizeCategory = true
        status.numberOfLines = 0
        status.textColor = .secondaryLabel
        status.accessibilityTraits.insert(.updatesFrequently)

        draft.font = .preferredFont(forTextStyle: .body)
        draft.adjustsFontForContentSizeCategory = true
        draft.isEditable = false
        draft.isSelectable = true
        draft.backgroundColor = .secondarySystemBackground
        draft.textColor = .label
        draft.layer.cornerRadius = 12
        draft.textContainerInset = UIEdgeInsets(top: 12, left: 12, bottom: 12, right: 12)
        draft.accessibilityLabel = "Ditt utkast"
        draft.heightAnchor.constraint(greaterThanOrEqualToConstant: 120).isActive = true

        paint(mic, prominent: false)
        paint(stop, prominent: false)
        paint(send, prominent: true)
        mic.addTarget(self, action: #selector(talk), for: .touchUpInside)
        stop.setTitle("Avbryt", for: .normal)
        stop.accessibilityLabel = "Avbryt och rensa utkastet"
        stop.addTarget(self, action: #selector(abandon), for: .touchUpInside)
        send.setTitle("Skicka", for: .normal)
        send.accessibilityLabel = "Skicka utkastet i konversationen"
        send.addTarget(self, action: #selector(sendDraft), for: .touchUpInside)

        let row = UIStackView(arrangedSubviews: [mic, stop])
        row.distribution = .fillEqually
        row.spacing = 10

        let stack = UIStackView(arrangedSubviews: [status, draft, row, send])
        stack.axis = .vertical
        stack.spacing = 10
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)

        let area = view.safeAreaLayoutGuide
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: area.topAnchor, constant: 12),
            stack.bottomAnchor.constraint(equalTo: area.bottomAnchor, constant: -12),
            stack.leadingAnchor.constraint(equalTo: area.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: area.trailingAnchor, constant: -16)
        ])
        render()
    }

    private func paint(_ button: UIButton, prominent: Bool) {
        button.titleLabel?.font = .preferredFont(forTextStyle: .title3)
        button.titleLabel?.adjustsFontForContentSizeCategory = true
        button.titleLabel?.adjustsFontSizeToFitWidth = true
        button.titleLabel?.minimumScaleFactor = 0.8
        button.layer.cornerRadius = 12
        button.backgroundColor = prominent ? .systemBlue : .secondarySystemBackground
        button.setTitleColor(prominent ? .white : .label, for: .normal)
        button.setTitleColor(prominent ? UIColor(white: 1, alpha: 0.55) : .tertiaryLabel, for: .disabled)
        button.heightAnchor.constraint(greaterThanOrEqualToConstant: 50).isActive = true
    }

    private var draftIsEmpty: Bool {
        draft.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private func render() {
        status.text = note
        mic.setTitle(phase == .recording ? "Klar" : (draftIsEmpty ? "Tala" : "Tala om"), for: .normal)
        mic.accessibilityLabel = phase == .recording ? "Klar, stäng mikrofonen" : "Tala, starta mikrofonen"
        mic.isEnabled = phase == .idle || phase == .recording
        stop.isEnabled = phase == .asking || phase == .recording || phase == .finishing || !draftIsEmpty
        send.isEnabled = phase == .idle && !draftIsEmpty && activeConversation != nil
    }

    // MARK: - Meddelanden

    private func fingerprint(_ conversation: MSConversation) -> String {
        let others = conversation.remoteParticipantIdentifiers.map(\.uuidString).sorted()
        return ([conversation.localParticipantIdentifier.uuidString] + others).joined(separator: "|")
    }

    override func willBecomeActive(with conversation: MSConversation) {
        super.willBecomeActive(with: conversation)
        let current = fingerprint(conversation)
        if conversationKey != current {
            conversationKey = current
            draft.text = ""
        }
        reset(draftIsEmpty ? "Tryck på Tala för att svara." : "Läs igenom och tryck på Skicka.")
    }

    override func didBecomeActive(with conversation: MSConversation) {
        super.didBecomeActive(with: conversation)
        if presentationStyle == .compact { requestPresentationStyle(.expanded) }
    }

    override func willResignActive(with conversation: MSConversation) {
        super.willResignActive(with: conversation)
        draft.text = ""
        reset("Tryck på Tala för att svara.")
    }

    override func willTransition(to presentationStyle: MSMessagesAppPresentationStyle) {
        super.willTransition(to: presentationStyle)
        guard presentationStyle == .compact else { return }
        if phase == .asking || phase == .recording || phase == .finishing {
            reset("Mikrofonen stängdes. Tryck på Tala igen.")
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if phase != .idle { reset("Mikrofonen stängdes.") }
    }

    // MARK: - Diktering

    @objc private func talk() {
        if phase == .recording { finish(); return }
        guard phase == .idle else { return }
        let id = UUID()
        generation = id
        phase = .asking
        note = "Kontrollerar mikrofon och taligenkänning …"
        render()
        SFSpeechRecognizer.requestAuthorization { auth in
            DispatchQueue.main.async {
                guard self.generation == id else { return }
                guard auth == .authorized else { self.reset("Tillåt taligenkänning i Inställningar."); return }
                self.askMicrophone { granted in
                    guard self.generation == id else { return }
                    guard granted else { self.reset("Tillåt mikrofon i Inställningar."); return }
                    self.start(id)
                }
            }
        }
    }

    private func askMicrophone(_ done: @escaping (Bool) -> Void) {
        let hand: (Bool) -> Void = { granted in DispatchQueue.main.async { done(granted) } }
        if #available(iOS 17.0, *) {
            AVAudioApplication.requestRecordPermission(completionHandler: hand)
        } else {
            AVAudioSession.sharedInstance().requestRecordPermission(hand)
        }
    }

    private func start(_ id: UUID) {
        guard let recognizer, recognizer.isAvailable else {
            reset("Svensk taligenkänning är inte tillgänglig just nu.")
            return
        }
        do {
            let audio = AVAudioSession.sharedInstance()
            try audio.setCategory(.record, mode: .measurement, options: [])
            try audio.setActive(true, options: [])
            audioActive = true
            let input = engine.inputNode
            let format = input.outputFormat(forBus: 0)
            guard format.sampleRate > 0, format.channelCount > 0 else {
                reset("Ingen mikrofon tillgänglig.")
                return
            }
            let buffer = SFSpeechAudioBufferRecognitionRequest()
            buffer.shouldReportPartialResults = true
            buffer.requiresOnDeviceRecognition = recognizer.supportsOnDeviceRecognition
            buffer.taskHint = .dictation
            buffer.addsPunctuation = true
            request = buffer
            input.installTap(onBus: 0, bufferSize: 1024, format: format) { sample, _ in buffer.append(sample) }
            hasTap = true
            engine.prepare()
            try engine.start()
            draft.text = ""
            phase = .recording
            note = "Lyssnar på svenska. Tryck på Klar när du talat."
            render()
            task = recognizer.recognitionTask(with: buffer) { result, error in
                DispatchQueue.main.async {
                    guard self.generation == id else { return }
                    if let result { self.draft.text = result.bestTranscription.formattedString }
                    if result?.isFinal == true || error != nil {
                        self.reset(self.draftIsEmpty ? "Inget tal uppfattades. Försök igen." : "Läs igenom och tryck på Skicka.")
                    }
                }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 55) {
                if self.generation == id, self.phase == .recording { self.finish() }
            }
        } catch {
            reset("Mikrofonen kunde inte starta. Försök igen.")
        }
    }

    private func finish() {
        guard phase == .recording else { return }
        phase = .finishing
        closeMicrophone()
        request?.endAudio()
        note = "Slutför texten …"
        render()
        let id = generation
        DispatchQueue.main.asyncAfter(deadline: .now() + 4) {
            guard self.generation == id else { return }
            self.reset(self.draftIsEmpty ? "Inget tal uppfattades. Försök igen." : "Läs igenom och tryck på Skicka.")
        }
    }

    @objc private func abandon() {
        draft.text = ""
        reset("Avbrutet. Inget skickades.")
    }

    /// Stänger mikrofonen och ljudsessionen. Tål att kallas flera gånger.
    private func closeMicrophone() {
        if engine.isRunning { engine.stop() }
        if hasTap {
            engine.inputNode.removeTap(onBus: 0)
            hasTap = false
        }
        engine.reset()
        if audioActive {
            try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
            audioActive = false
        }
    }

    /// Avslutar allt pågående arbete, stänger mikrofonen och går till vila.
    private func reset(_ message: String) {
        generation = UUID()
        closeMicrophone()
        task?.cancel()
        task = nil
        request = nil
        phase = .idle
        note = message
        render()
    }

    // MARK: - Skicka

    @objc private func sendDraft() {
        let text = draft.text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard phase == .idle, !text.isEmpty,
              presentationContext == .messages,
              let conversation = activeConversation else { return }
        let id = UUID()
        generation = id
        phase = .sending
        note = "Skickar …"
        render()
        conversation.sendText(text) { error in
            DispatchQueue.main.async {
                guard self.generation == id else { return }
                self.phase = .idle
                if error == nil { self.draft.text = "" }
                self.note = error == nil ? "Skickat." : "Kunde inte skicka. Ditt utkast finns kvar."
                self.render()
            }
        }
    }
}
