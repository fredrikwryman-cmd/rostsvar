import Messages
import UIKit
import UniformTypeIdentifiers

final class MessagesController: MSMessagesAppViewController, UITextViewDelegate {
    private var state = DraftState()
    /// Vakt för taligenkänningen. Byts när dikteringen avbryts eller går om.
    private var generation = UUID()
    /// Egen vakt för skickningen. Skild från generation, eftersom en
    /// ihopfälld panel avbryter dikteringen men aldrig en pågående skickning.
    private var sendToken = UUID()
    private var note = "Tryck på Tala för att svara."
    /// Det senast upplästa beskedet. render() kallas vid varje tecken, och
    /// samma mening får inte läsas upp om och om igen.
    private var announced = ""

    /// Dikteringen är delad med tangentbordet. Rättningen av buggen där
    /// texten försvann när taltjänsten avslutade med fel ligger i Dictation,
    /// så att den gäller båda.
    private let ears = Dictation()

    private let status = UILabel()
    /// Syns medan taltjänsten slutför och medan meddelandet skickas.
    private let spin = UIActivityIndicatorView(style: .medium)
    /// En rad som säger var huvudvägen går. Tillägget är genvägen i
    /// Meddelanden; tangentbordet gör samma sak i varje app.
    private let aside = UILabel()
    /// Märket överst i kortet: vad det är man läser. rev-ux 5.3.
    private let badge = UILabel()
    private let draft = UITextView()
    /// Tiden nederst i kortet, dämpad, högerställd.
    private let stamp = UILabel()
    /// När utkastet kom. Nollas när fältet töms, sätts när det får text.
    private var written: Date?
    private let mic = UIButton(type: .system)
    private let stop = UIButton(type: .system)
    private let send = UIButton(type: .system)

    deinit { ears.cancel() }

    // MARK: - Vy

    override func viewDidLoad() {
        super.viewDidLoad()
        // Tech Rabbit-designen är ritad bara för mörkt läge, §1. Panelen
        // följde förut systemets läge och blev vit i ljust.
        overrideUserInterfaceStyle = .dark
        view.backgroundColor = Palette.ui(Palette.bg)

        status.font = .preferredFont(forTextStyle: .subheadline)
        status.adjustsFontForContentSizeCategory = true
        status.numberOfLines = 0
        status.textColor = Palette.ui(Palette.muted)
        status.accessibilityTraits.insert(.updatesFrequently)

        spin.color = Palette.ui(Palette.muted)
        spin.hidesWhenStopped = true

        // Märket, texten, tiden: innehållskortet ur rev-ux 5.3. Förut var
        // utkastet ett fält med kant, utan att säga vad det var man läste.
        // Formen är kortets nu, och fältet inuti är bara texten. Funktionen
        // är densamma: utkastet går att rätta och skickas med Skicka.
        badge.font = TypeScale.badge.uiFont()
        badge.adjustsFontForContentSizeCategory = true
        badge.textColor = Palette.ui(Palette.faint)
        badge.text = Self.mark
        // Fältet bär redan samma ord till skärmläsaren. Två gånger är en
        // gång för mycket.
        badge.isAccessibilityElement = false

        draft.font = TypeScale.draft.uiFont()
        draft.adjustsFontForContentSizeCategory = true
        draft.isEditable = true
        draft.isSelectable = true
        draft.delegate = self
        draft.inputAccessoryView = dismissBar()
        // Ytan, kanten och radien ligger på kortet. Fältet är texten.
        draft.backgroundColor = .clear
        draft.textColor = Palette.ui(Palette.text)
        draft.tintColor = Palette.ui(Palette.green)
        draft.keyboardAppearance = .dark
        draft.textContainerInset = .zero
        draft.textContainer.lineFragmentPadding = 0
        draft.accessibilityLabel = Self.mark
        draft.accessibilityHint = "Går att rätta för hand när dikteringen är över."
        draft.heightAnchor.constraint(greaterThanOrEqualToConstant: Metric.draftMinHeight).isActive = true

        stamp.font = TypeScale.counter.uiFont()
        stamp.adjustsFontForContentSizeCategory = true
        stamp.textColor = Palette.ui(Palette.faint)
        stamp.textAlignment = .right

        paint(mic, prominent: false)
        paint(stop, prominent: false)
        paint(send, prominent: true)
        mic.addTarget(self, action: #selector(talk), for: .touchUpInside)
        stop.setTitle("Rensa", for: .normal)
        stop.accessibilityLabel = "Rensa utkastet"
        stop.addTarget(self, action: #selector(abandon), for: .touchUpInside)
        send.setTitle("Skicka", for: .normal)
        send.accessibilityLabel = "Skicka utkastet i konversationen"
        send.addTarget(self, action: #selector(sendDraft), for: .touchUpInside)

        aside.font = .preferredFont(forTextStyle: .caption1)
        aside.adjustsFontForContentSizeCategory = true
        aside.numberOfLines = 0
        aside.textColor = Palette.ui(Palette.faint)
        aside.text = "Tap&Talk-tangentbordet dikterar i alla appar. Det här är genvägen i Meddelanden."

        let row = UIStackView(arrangedSubviews: [mic, stop])
        row.distribution = .fillEqually
        row.spacing = Metric.gapSM

        let head = UIStackView(arrangedSubviews: [spin, status])
        head.axis = .horizontal
        head.spacing = Metric.gapXS
        head.alignment = .center

        // Kortet: märket, texten, tiden. Måtten, radien och färgerna ur
        // temat, samma som kortet på startskärmen.
        let card = UIStackView(arrangedSubviews: [badge, draft, stamp])
        card.axis = .vertical
        card.spacing = Metric.gapS
        card.isLayoutMarginsRelativeArrangement = true
        card.directionalLayoutMargins = NSDirectionalEdgeInsets(
            top: Metric.cardPad, leading: Metric.cardPad,
            bottom: Metric.fieldBottom, trailing: Metric.cardPad)
        card.backgroundColor = Palette.ui(Palette.card)
        card.layer.cornerRadius = Metric.cardRadius
        card.layer.borderWidth = Metric.edge
        card.layer.borderColor = Palette.ui(Palette.border).cgColor

        let stack = UIStackView(arrangedSubviews: [head, card, row, send, aside])
        stack.axis = .vertical
        stack.spacing = Metric.gapSM
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)

        let area = view.safeAreaLayoutGuide
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: area.topAnchor,
                                       constant: Metric.gapM),
            stack.bottomAnchor.constraint(equalTo: area.bottomAnchor,
                                          constant: -Metric.gapM),
            stack.leadingAnchor.constraint(equalTo: area.leadingAnchor,
                                           constant: Metric.gapL),
            stack.trailingAnchor.constraint(equalTo: area.trailingAnchor,
                                            constant: -Metric.gapL)
        ])
        render()
    }

    /// Systemtangentbordet täcker hela panelen medan man rättar texten, och
    /// en meddelandepanel har ingen plats för en verktygsrad. Raden ovanför
    /// tangentbordet är den enda vägen ut som alltid finns. Ordet är inte
    /// "Klar": det betyder stäng mikrofonen här.
    private func dismissBar() -> UIToolbar {
        let bar = UIToolbar()
        bar.sizeToFit()
        let hide = UIBarButtonItem(title: "Dölj tangentbordet", style: .done,
                                  target: self, action: #selector(stopEditing))
        bar.items = [UIBarButtonItem(systemItem: .flexibleSpace), hide]
        return bar
    }

    @objc private func stopEditing() {
        draft.resignFirstResponder()
    }

    /// Användarens egen rättning går in i tillståndet, så att Skicka och
    /// Rensa följer det som faktiskt står i fältet.
    func textViewDidChange(_ field: UITextView) {
        state.edit(field.text)
        render()
    }

    /// Samma knappform som Rensa och Skicka på startskärmen: piller, grön
    /// när den är vägen vidare, annars kortets yta med kant.
    private func paint(_ button: UIButton, prominent: Bool) {
        button.titleLabel?.font = .preferredFont(forTextStyle: .title3)
        button.titleLabel?.adjustsFontForContentSizeCategory = true
        button.titleLabel?.adjustsFontSizeToFitWidth = true
        button.titleLabel?.minimumScaleFactor = 0.8
        button.backgroundColor = Palette.ui(prominent ? Palette.green : Palette.card)
        button.setTitleColor(Palette.ui(prominent ? Palette.onGreen : Palette.text),
                             for: .normal)
        button.setTitleColor(Palette.ui(Palette.disabled), for: .disabled)
        button.layer.borderWidth = Metric.hairline
        button.layer.borderColor =
            Palette.ui(prominent ? Palette.green : Palette.dimDot).cgColor
        let high: CGFloat = Metric.controlHeight
        button.heightAnchor.constraint(greaterThanOrEqualToConstant: high).isActive = true
        // Pillerform: radien är halva höjden, som Capsule i appen.
        button.layer.cornerRadius = high / 2
    }

    private func render() {
        status.text = note
        if note != announced {
            announced = note
            UIAccessibility.post(notification: .announcement, argument: note)
        }
        // Texten sätts bara när den skiljer sig, annars flyttas markören
        // varje gång panelen ritas om medan användaren skriver.
        if draft.text != state.draft { draft.text = state.draft }
        draft.isEditable = state.canEdit
        // Tiden i kortets fot: när utkastet kom. Den sätts en gång per
        // utkast och nollas när fältet töms, så att den inte tickar vidare
        // medan man rättar en bokstav.
        if state.hasDraft {
            if written == nil { written = Date() }
        } else {
            written = nil
        }
        if let written {
            let time = Self.clock.string(from: written)
            stamp.text = time
            stamp.accessibilityLabel = "Dikterat klockan " + time
            stamp.isHidden = false
        } else {
            stamp.text = ""
            stamp.isHidden = true
        }
        // Väntan ska synas: taltjänstens sista tolkning och överlämningen
        // till Meddelanden tar båda sekunder.
        let waiting = state.phase == .finishing || state.phase == .sending
        if waiting { spin.startAnimating() } else { spin.stopAnimating() }
        let talking = state.phase == .recording
        mic.setTitle(talking ? "Klar" : (state.hasDraft ? "Tala om" : "Tala"), for: .normal)
        mic.accessibilityLabel = talking ? "Klar, stäng mikrofonen" : "Tala, starta mikrofonen"
        mic.isEnabled = state.canTalk
        stop.isEnabled = state.canCancel
        send.isEnabled = state.canSend && activeConversation != nil
    }

    /// Märket i kortet. Samma ord som skärmläsaren får om fältet.
    private static let mark = "Ditt utkast"

    /// Klockan i kortets fot. Systemets eget format, så att tolv eller
    /// tjugofyra timmar följer telefonen och inte oss.
    private static let clock: DateFormatter = {
        let shown = DateFormatter()
        shown.dateStyle = .none
        shown.timeStyle = .short
        return shown
    }()

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
        draft.resignFirstResponder()
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
        reset("Fältet är tomt. Inget skickades.")
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
