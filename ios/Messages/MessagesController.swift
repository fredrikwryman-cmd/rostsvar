import AVFoundation
import Messages
import Speech
import UIKit

final class MessagesController: MSMessagesAppViewController {
    private var state = DraftState()
    /// Vakt för taligenkänningen. Byts när dikteringen avbryts eller går om.
    private var generation = UUID()
    /// Egen vakt för skickningen. Skild från generation, eftersom en
    /// ihopfälld panel avbryter dikteringen men aldrig en pågående skickning.
    private var sendToken = UUID()
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

    private func render() {
        status.text = note
        if draft.text != state.draft { draft.text = state.draft }
        let talking = state.phase == .recording
        mic.setTitle(talking ? "Klar" : (state.hasDraft ? "Tala om" : "Tala"), for: .normal)
        mic.accessibilityLabel = talking ? "Klar, stäng mikrofonen" : "Tala, starta mikrofonen"
        mic.isEnabled = state.canTalk
        stop.isEnabled = state.canCancel
        send.isEnabled = state.canSend && activeConversation != nil
    }

    // MARK: - Meddelanden

    private func fingerprint(_ conversation: MSConversation) -> String {
        let others = conversation.remoteParticipantIdentifiers.map(\.uuidString).sorted()
        return ([conversation.localParticipantIdentifier.uuidString] + others).joined(separator: "|")
    }

    override func willBecomeActive(with conversation: MSConversation) {
        super.willBecomeActive(with: conversation)
        state.becomeActive(conversation: fingerprint(conversation))
        settle(state.hasDraft ? "Läs igenom och tryck på Skicka." : "Tryck på Tala för att svara.")
    }

    override func didBecomeActive(with conversation: MSConversation) {
        super.didBecomeActive(with: conversation)
        if presentationStyle == .compact { requestPresentationStyle(.expanded) }
    }

    override func willResignActive(with conversation: MSConversation) {
        super.willResignActive(with: conversation)
        state.resignActive()
        settle("Tryck på Tala för att svara.")
    }

    override func willTransition(to presentationStyle: MSMessagesAppPresentationStyle) {
        super.willTransition(to: presentationStyle)
        guard presentationStyle == .compact else { return }
        if state.phase != .idle {
            settle("Mikrofonen stängdes. Tryck på Tala igen.")
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if state.phase != .idle { settle("Mikrofonen stängdes.") }
    }

    /// Går till vila med ett nytt besked, utom medan ett meddelande skickas.
    /// Då äger skickningen både texten och beskedet tills svaret kommit.
    private func settle(_ message: String) {
        guard state.phase != .sending else { render(); return }
        reset(message)
    }

    // MARK: - Diktering

    @objc private func talk() {
        if state.phase == .recording { finish(); return }
        guard state.phase == .idle else { return }
        let id = UUID()
        generation = id
        state.ask()
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
            state.beginRecording()
            note = "Lyssnar på svenska. Tryck på Klar när du talat."
            render()
            task = recognizer.recognitionTask(with: buffer) { result, error in
                DispatchQueue.main.async {
                    guard self.generation == id else { return }
                    if let result {
                        self.state.transcribe(result.bestTranscription.formattedString)
                        self.render()
                    }
                    if result?.isFinal == true || error != nil {
                        self.reset(self.state.hasDraft ? "Läs igenom och tryck på Skicka." : "Inget tal uppfattades. Försök igen.")
                    }
                }
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 55) {
                if self.generation == id, self.state.phase == .recording { self.finish() }
            }
        } catch {
            reset("Mikrofonen kunde inte starta. Försök igen.")
        }
    }

    private func finish() {
        guard state.phase == .recording else { return }
        state.finishRecording()
        closeMicrophone()
        request?.endAudio()
        note = "Slutför texten …"
        render()
        let id = generation
        DispatchQueue.main.asyncAfter(deadline: .now() + 4) {
            guard self.generation == id else { return }
            self.reset(self.state.hasDraft ? "Läs igenom och tryck på Skicka." : "Inget tal uppfattades. Försök igen.")
        }
    }

    @objc private func abandon() {
        state.cancel()
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
        state.rest()
        note = message
        render()
    }

    // MARK: - Skicka

    @objc private func sendDraft() {
        let text = state.trimmedDraft
        guard state.canSend,
              presentationContext == .messages,
              let conversation = activeConversation else { return }
        let token = UUID()
        sendToken = token
        state.beginSending()
        note = "Skickar …"
        render()
        conversation.sendText(text) { error in
            DispatchQueue.main.async {
                guard self.sendToken == token else { return }
                self.state.finishSending(failed: error != nil)
                self.note = error == nil ? "Skickat." : "Kunde inte skicka. Ditt utkast finns kvar."
                self.render()
            }
        }
    }
}
