import UIKit

/// Tap&Talk som tangentbord: samma diktering, men i varje app där man
/// skriver, inte bara i Meddelanden.
///
/// Svart bakgrund och grön accent, som appen. Ingenting av det som skrivs
/// loggas, sparas eller lämnar telefonen. Det enda som går ut är talet, till
/// Apples taltjänst, och bara medan mikrofonknappen är igång.
final class KeyboardController: UIInputViewController {

    // Samma palett som appen och Android-sidan, ur Shared/Palette.swift.
    // Stod förut som egna literaler här, med ett grönt som varken var appens
    // eller Androids — tre gröna i en app som har en accent.
    private let bg = Palette.ui(Palette.bg)
    private let ink = Palette.ui(Palette.text)
    private let dim = Palette.ui(Palette.muted)
    private let green = Palette.ui(Palette.green)
    private let greenDeep = Palette.ui(Palette.greenDeep)
    private let darkInk = Palette.ui(Palette.onGreen)
    private let keyFill = Palette.ui(Palette.keyFill)
    private let keyEdge = Palette.ui(Palette.keyEdge)

    /// Svensk layout. Å, Ä och Ö sitter där de sitter på ett svenskt
    /// tangentbord, sist på de två övre raderna.
    private static let letters = ["qwertyuiopå", "asdfghjklöä", "zxcvbnm"]
    private static let pageOne = ["1234567890", "-/:;()&@\"", ".,?!'"]
    private static let pageTwo = ["+=%#*[]{}", "_|~<>€£¥", "•§…"]

    private enum Shift { case off, on, locked }

    private var shift = Shift.off
    private var digits = false
    private var extraPage = false
    private var listening = false
    /// Vakt för dikteringen. Byts vid varje start och vid varje avbrott, så
    /// att en omgång användaren lämnat inte kan starta mikrofonen eller
    /// skriva något efteråt. Appen och iMessage-tillägget har samma vakt.
    private var run = UUID()
    /// Det dikteringen själv har skrivit i fältet, och alltså äger. Behövs
    /// för att kunna byta ut en tolkning mot nästa utan att röra användarens
    /// egen text.
    private var mine = ""

    private let ears = Dictation()

    private let status = UILabel()
    private let rows = UIStackView()
    private let mic = UIButton(type: .system)

    deinit { ears.cancel() }

    // MARK: - Vy

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = bg

        status.font = .systemFont(ofSize: 12)
        status.textColor = dim
        status.numberOfLines = 2
        status.textAlignment = .center
        status.adjustsFontSizeToFitWidth = true
        status.minimumScaleFactor = 0.8

        rows.axis = .vertical
        rows.spacing = 6
        rows.distribution = .fillEqually

        mic.backgroundColor = green
        mic.setTitleColor(darkInk, for: .normal)
        mic.titleLabel?.font = .systemFont(ofSize: 20, weight: .semibold)
        // Primärknappen är ett piller i appen och i iMessage-tillägget. Här
        // är den en tangent bland tangenter, men radien följer höjden på samma
        // sätt, så formen känns igen.
        mic.layer.cornerRadius = 12
        mic.addTarget(self, action: #selector(micTapped), for: .touchUpInside)

        let page = UIStackView(arrangedSubviews: [status, rows, mic])
        page.axis = .vertical
        page.spacing = 6
        page.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(page)

        NSLayoutConstraint.activate([
            page.topAnchor.constraint(equalTo: view.topAnchor, constant: 6),
            page.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -6),
            page.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 4),
            page.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -4),
            mic.heightAnchor.constraint(equalToConstant: 50)
        ])

        // Ett tangentbord måste själv säga hur högt det är.
        let tall = view.heightAnchor.constraint(equalToConstant: 296)
        tall.priority = UILayoutPriority(999)
        tall.isActive = true

        build()
        render()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        stopDictation()
    }

    // MARK: - Tangenterna

    private func key(_ title: String, action: Selector, accent: Bool = false,
                     label: String? = nil) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.backgroundColor = keyFill
        button.setTitleColor(accent ? green : ink, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: accent ? 16 : 19)
        button.titleLabel?.adjustsFontSizeToFitWidth = true
        button.titleLabel?.minimumScaleFactor = 0.6
        button.layer.cornerRadius = 7
        button.layer.borderWidth = 1
        button.layer.borderColor = keyEdge.cgColor
        button.addTarget(self, action: action, for: .touchUpInside)
        button.accessibilityLabel = label ?? title
        return button
    }

    private func line(_ characters: String, upper: Bool) -> UIStackView {
        let row = UIStackView()
        row.axis = .horizontal
        row.spacing = 5
        row.distribution = .fillEqually
        for letter in characters {
            let shown = upper ? String(letter).uppercased() : String(letter)
            row.addArrangedSubview(key(shown, action: #selector(letterTapped)))
        }
        return row
    }

    /// Bygger om tangenterna. Görs vid skift och vid växling av tecken.
    private func build() {
        rows.arrangedSubviews.forEach {
            rows.removeArrangedSubview($0)
            $0.removeFromSuperview()
        }

        let source = digits ? (extraPage ? Self.pageTwo : Self.pageOne) : Self.letters
        let upper = !digits && shift != .off

        rows.addArrangedSubview(line(source[0], upper: upper))
        rows.addArrangedSubview(line(source[1], upper: upper))

        // Tredje raden: skift, bokstäverna, backsteg. Fasta andelar av
        // bredden, så raden ser lika ut oavsett hur många tecken den bär.
        let third = UIStackView()
        third.axis = .horizontal
        third.spacing = 5
        third.distribution = .fill

        let shiftTitle = digits ? (extraPage ? "123" : "#+=") : (shift == .locked ? "⇪" : "⇧")
        let left = key(shiftTitle, action: #selector(shiftTapped), accent: true,
                       label: digits ? "Fler tecken" : (shift == .locked ? "Skift låst" : "Skift"))
        let erase = key("⌫", action: #selector(eraseTapped), accent: true, label: "Backsteg")
        let middle = line(source[2], upper: upper)

        third.addArrangedSubview(left)
        third.addArrangedSubview(middle)
        third.addArrangedSubview(erase)
        rows.addArrangedSubview(third)
        NSLayoutConstraint.activate([
            left.widthAnchor.constraint(equalTo: third.widthAnchor, multiplier: 0.13),
            erase.widthAnchor.constraint(equalTo: left.widthAnchor)
        ])

        let bottom = UIStackView()
        bottom.axis = .horizontal
        bottom.spacing = 5
        bottom.distribution = .fill

        let swap = key(digits ? "ABC" : "123", action: #selector(digitsTapped), accent: true,
                       label: digits ? "Bokstäver" : "Siffror och tecken")
        let back = key("⌨", action: #selector(backTapped), accent: true,
                       label: "Tillbaka till föregående tangentbord")
        let space = key("mellanslag", action: #selector(spaceTapped), label: "Mellanslag")
        let enter = key("retur", action: #selector(returnTapped), accent: true, label: "Retur")

        bottom.addArrangedSubview(swap)
        bottom.addArrangedSubview(back)
        bottom.addArrangedSubview(space)
        bottom.addArrangedSubview(enter)
        rows.addArrangedSubview(bottom)
        NSLayoutConstraint.activate([
            swap.widthAnchor.constraint(equalTo: bottom.widthAnchor, multiplier: 0.15),
            back.widthAnchor.constraint(equalTo: bottom.widthAnchor, multiplier: 0.13),
            enter.widthAnchor.constraint(equalTo: bottom.widthAnchor, multiplier: 0.19)
        ])
    }

    private func render() {
        if listening {
            mic.backgroundColor = greenDeep
            mic.setTitleColor(darkInk, for: .normal)
            mic.setTitle("Klar", for: .normal)
            mic.accessibilityLabel = "Klar, stäng mikrofonen"
        } else {
            mic.backgroundColor = hasFullAccess ? green : keyFill
            mic.setTitleColor(hasFullAccess ? darkInk : dim, for: .normal)
            mic.setTitle(hasFullAccess ? "Tala" : "Mikrofonen är låst", for: .normal)
            mic.accessibilityLabel = hasFullAccess
                ? "Tala, starta dikteringen"
                : "Mikrofonen är låst. Slå på Tillåt fullständig åtkomst."
        }
    }

    private func tell(_ text: String) {
        status.text = text
    }

    // MARK: - Trycken

    @objc private func letterTapped(_ sender: UIButton) {
        guard let text = sender.title(for: .normal) else { return }
        stopDictation()
        textDocumentProxy.insertText(text)
        // Ett enkelt skift gäller ett tecken. Låst skift ligger kvar.
        if shift == .on {
            shift = .off
            build()
        }
    }

    @objc private func spaceTapped() {
        stopDictation()
        textDocumentProxy.insertText(" ")
    }

    @objc private func returnTapped() {
        stopDictation()
        textDocumentProxy.insertText("\n")
    }

    @objc private func eraseTapped() {
        stopDictation()
        textDocumentProxy.deleteBackward()
    }

    @objc private func shiftTapped() {
        if digits {
            extraPage.toggle()
        } else {
            switch shift {
            case .off: shift = .on
            case .on: shift = .locked
            case .locked: shift = .off
            }
        }
        build()
    }

    @objc private func digitsTapped() {
        digits.toggle()
        extraPage = false
        shift = .off
        build()
    }

    /// Tillbaka till det tangentbord användaren hade innan.
    @objc private func backTapped() {
        stopDictation()
        advanceToNextInputMode()
    }

    // MARK: - Dikteringen

    @objc private func micTapped() {
        if listening {
            // Klar: ljudet avslutas och svaret inväntas. Att det pågår måste
            // synas, annars ser tangentbordet dött ut medan taltjänsten
            // skriver sin sista tolkning.
            tell("Slutför texten …")
            ears.finish()
            return
        }
        // Ett tangentbord får inte röra mikrofonen utan fullständig åtkomst,
        // och får heller inte fråga om den själv. Då säger vi det i stället.
        guard hasFullAccess else {
            tell("Dikteringen behöver Tillåt fullständig åtkomst: Inställningar → Allmänt → "
                 + "Tangentbord → Tangentbord → Tap&Talk. Appen visar vägen.")
            return
        }
        guard ears.isAvailable else {
            tell("Svensk taligenkänning är inte tillgänglig just nu.")
            return
        }
        let id = UUID()
        run = id
        mine = ""
        tell("Kontrollerar mikrofon och taligenkänning …")
        ears.authorize { [weak self] refused in
            // Hann användaren trycka på en tangent medan frågan ställdes är
            // omgången över. Då får mikrofonen inte starta efteråt.
            guard let self, self.run == id else { return }
            if let refused { self.tell(refused + " Öppna Tap&Talk-appen."); return }
            self.begin(id)
        }
    }

    private func begin(_ id: UUID) {
        let refused = ears.start(partial: { [weak self] text in
            guard let self, self.run == id else { return }
            self.write(text)
        }, ending: { [weak self] how in
            guard let self, self.run == id else { return }
            self.listening = false
            switch how {
            case .done(let text):
                self.write(text)
                self.tell("Klart. Rätta med tangenterna om du vill.")
            case .salvaged(let text, let why):
                // Texten hördes, alltså står den kvar. Vi säger aldrig att
                // inget uppfattades när något gjorde det.
                self.write(text)
                self.tell(why)
            case .nothing(let why):
                self.tell(why)
            }
            self.mine = ""
            self.render()
        })
        if let refused { tell(refused); return }
        listening = true
        tell("Lyssnar. Tala svenska och tryck på Klar.")
        render()
    }

    private func stopDictation() {
        // Vakten byts alltid, också när mikrofonen inte hunnit starta: en
        // fråga om tillstånd kan vara ute, och svaret får inte dra igång en
        // diktering efter att användaren tagit över med tangenterna.
        run = UUID()
        guard listening else { return }
        listening = false
        ears.cancel()
        mine = ""
        tell("Dikteringen avbröts. Texten står kvar.")
        render()
    }

    /// Skriver tolkningen i fältet, löpande medan man talar.
    ///
    /// Ett textDocumentProxy går bara att skriva i och radera bakåt i, så en
    /// ny tolkning kan inte ersätta den gamla rakt av. Det dikteringen själv
    /// skrivit sparas i `mine`: den gemensamma början behålls, resten raderas
    /// tecken för tecken och det nya läggs till. Användarens egen text rörs
    /// aldrig, eftersom den ligger före `mine`.
    private func write(_ text: String) {
        // Först: äger vi fortfarande texten före markören? Har användaren
        // flyttat markören eller markerat något gör vi inte det, och då
        // raderas ingenting — annars hade backsteget ätit användarens egen
        // text. Reglerna står i ProxyWriting och mäts av ProxyWritingTests.
        mine = ProxyWriting.owned(mine: mine,
                                  before: textDocumentProxy.documentContextBeforeInput,
                                  selected: textDocumentProxy.selectedText)
        guard text != mine else { return }
        let step = ProxyWriting.plan(text: text, mine: mine)
        for _ in 0..<step.erase { textDocumentProxy.deleteBackward() }
        if !step.insert.isEmpty { textDocumentProxy.insertText(step.insert) }
        mine = text
    }
}
