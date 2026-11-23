import Messages
import UIKit
import UniformTypeIdentifiers

final class MessagesController: MSMessagesAppViewController {
    private var state = DraftState()
    /// Vakt för taligenkänningen. Byts när dikteringen avbryts eller går om.
    private var generation = UUID()
    /// Egen vakt för skickningen. Skild från generation, eftersom en
    /// ihopfälld panel avbryter dikteringen men aldrig en pågående skickning.
    private var sendToken = UUID()
    private var note = "Tryck på Tala för att svara."

    /// Dikteringen är delad med tangentbordet. Rättningen av buggen där
    /// texten försvann när taltjänsten avslutade med fel ligger i Dictation,
    /// så att den gäller båda.
    private let ears = Dictation()

    private let status = UILabel()
    /// En rad som säger var huvudvägen går. Tillägget är genvägen i
    /// Meddelanden; tangentbordet gör samma sak i varje app.
    private let aside = UILabel()
    private let draft = UITextView()
    private let mic = UIButton(type: .system)
    private let stop = UIButton(type: .system)
    private let send = UIButton(type: .system)

    deinit { ears.cancel() }

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

        aside.font = .preferredFont(forTextStyle: .caption1)
        aside.adjustsFontForContentSizeCategory = true
        aside.numberOfLines = 0
        aside.textColor = .tertiaryLabel
        aside.text = "Tap&Talk-tangentbordet dikterar i alla appar. Det här är genvägen i Meddelanden."

        let row = UIStackView(arrangedSubviews: [mic, stop])
        row.distribution = .fillEqually
        row.spacing = 10

        let stack = UIStackView(arrangedSubviews: [status, draft, row, send, aside])
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
        // Räddades text förra gången panelen fälldes ihop är det första
        // användaren ska få veta: den finns kvar, och var.
        if let saved = rescued() { settle(saved); return }
        settle(state.hasDraft ? "Läs igenom och tryck på Skicka." : "Tryck på Tala för att svara.")
    }

    override func didBecomeActive(with conversation: MSConversation) {
        super.didBecomeActive(with: conversation)
        if presentationStyle == .compact { requestPresentationStyle(.expanded) }
    }

    override func willResignActive(with conversation: MSConversation) {
        super.willResignActive(with: conversation)
        // Utkastet nollas, men det som hörts kastas inte: det läggs i
        // urklipp, som bubblan gör på Android när texten inte kan skrivas i
        // ett fält. Appen ska aldrig tappa det som sagts.
        if let heard = state.resignActive() { keep(heard) }
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

    // MARK: - Urklippsräddningen

    /// Att det ligger räddad text i urklipp måste överleva att tillägget
    /// rivs mellan två öppningar, därför en flagga på disken och inte i
    /// minnet. Bara flaggan sparas — texten ligger i urklipp, ingen annanstans.
    private static let rescueFlag = "raddad_text_i_urklipp"

    /// Samma formulering som bubblan på Android använder vid urklippsräddning.
    private static let rescueNote = "Mikrofonen stängdes när panelen fälldes ihop. "
        + "Texten ligger i urklipp — håll in i fältet och välj Klistra in."

    /// Lägger den hörda texten i urklipp. Bara lokalt: texten ska inte hoppa
    /// vidare till användarens andra Apple-enheter.
    private func keep(_ text: String) {
        UIPasteboard.general.setItems(
            [[UTType.utf8PlainText.identifier: text]],
            options: [.localOnly: true])
        UserDefaults.standard.set(true, forKey: Self.rescueFlag)
    }

    /// Beskedet om räddad text, en gång. Nil när inget räddats.
    private func rescued() -> String? {
        guard UserDefaults.standard.bool(forKey: Self.rescueFlag) else { return nil }
        UserDefaults.standard.removeObject(forKey: Self.rescueFlag)
        return Self.rescueNote
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
        ears.authorize { [weak self] refused in
            guard let self, self.generation == id else { return }
            if let refused { self.reset(refused); return }
            self.start(id)
        }
    }

    private func start(_ id: UUID) {
        // Mikrofonen startas före beginRecording, eftersom beginRecording
        // rensar utkastet. Går mikrofonen inte igång ska den gamla texten
        // ligga kvar.
        let refused = ears.start(partial: { [weak self] text in
            guard let self, self.generation == id else { return }
            self.state.transcribe(text)
            self.render()
        }, ending: { [weak self] how in
            guard let self, self.generation == id else { return }
            switch how {
            case .done(let text):
                self.state.transcribe(text)
                self.reset("Läs igenom och tryck på Skicka.")
            case .salvaged(let text, let why):
                // Texten hördes. Då säger vi aldrig att inget uppfattades.
                self.state.transcribe(text)
                self.reset(why + " Läs igenom och tryck på Skicka.")
            case .nothing(let why):
                self.reset(self.state.hasDraft ? "Läs igenom och tryck på Skicka." : why)
            }
        })
        if let refused { reset(refused); return }
        state.beginRecording()
        note = "Lyssnar på svenska. Tryck på Klar när du talat."
        render()
    }

    /// Användaren är klar. Dikteringen avslutar ljudet själv och svarar
    /// genom sin ending-stängning; här väntar vi bara på svaret. Tidigare
    /// stängdes sessionen här, och det var det som fick taltjänsten att
    /// avsluta med fel efter att redan ha hört rätt.
    private func finish() {
        guard state.phase == .recording else { return }
        state.finishRecording()
        note = "Slutför texten …"
        render()
        ears.finish()
    }

    @objc private func abandon() {
        state.cancel()
        reset("Avbrutet. Inget skickades.")
    }

    /// Avslutar allt pågående arbete, stänger mikrofonen och går till vila.
    private func reset(_ message: String) {
        generation = UUID()
        ears.cancel()
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
