import AVFoundation
import Speech

/// Svensk diktering på ett ställe, delad av iMessage-tillägget och
/// tangentbordet. Finns den bara en gång kan den bara vara fel en gång.
///
/// Två regler bär den, och båda kommer av en bugg som gick att se på en
/// telefon: texten dök upp medan användaren talade, och efteråt stod det att
/// inget tal uppfattades.
///
/// 1. Ljudsessionen får inte rivas förrän taltjänsten har svarat. Stängs
///    sessionen i samma andetag som mikrofonen hinner taltjänsten inte
///    slutföra, och svarar med fel eller med ett tomt slutresultat.
/// 2. Det som en gång hörts kastas aldrig. Varje delresultat sparas, och ett
///    tomt eller uteblivet slutresultat får inte skriva över det.
final class Dictation {

    /// Hur en diktering slutade.
    enum Ending: Equatable {
        /// Taltjänsten blev klar. Texten är slutlig.
        case done(text: String)
        /// Taltjänsten slutade med fel, men det här hann höras och behålls.
        case salvaged(text: String, note: String)
        /// Ingenting hördes. Skälet går att visa för användaren.
        case nothing(note: String)
    }

    /// Så länge mikrofonen får vara öppen utan att användaren gör något.
    private let cap: TimeInterval = 55

    private let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "sv-SE"))
    private let engine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?

    private var hasTap = false
    private var audioActive = false
    /// Byts vid varje start, så att en sen återkoppling från en gammal
    /// diktering inte kan röra en ny.
    private var run = UUID()
    /// Allt som hörts under den här dikteringen. Skrivs aldrig över med tomt.
    private var heard = ""
    private var partial: ((String) -> Void)?
    private var ending: ((Ending) -> Void)?
    private var settled = false

    private(set) var isListening = false

    deinit { releaseEverything() }

    var isAvailable: Bool { recognizer?.isAvailable == true }

    // MARK: - Tillstånd

    /// Frågar om taligenkänning och mikrofon. Svarar med nil när allt är
    /// tillåtet, annars med en mening att visa.
    func authorize(_ done: @escaping (String?) -> Void) {
        SFSpeechRecognizer.requestAuthorization { auth in
            DispatchQueue.main.async {
                guard auth == .authorized else {
                    done("Tillåt taligenkänning i Inställningar.")
                    return
                }
                let hand: (Bool) -> Void = { granted in
                    DispatchQueue.main.async {
                        done(granted ? nil : "Tillåt mikrofon i Inställningar.")
                    }
                }
                if #available(iOS 17.0, *) {
                    AVAudioApplication.requestRecordPermission(completionHandler: hand)
                } else {
                    AVAudioSession.sharedInstance().requestRecordPermission(hand)
                }
            }
        }
    }

    // MARK: - Dikteringen

    /// Startar dikteringen. `partial` ropas för varje tolkning som kommer in,
    /// `ending` exakt en gång när dikteringen är över.
    ///
    /// - Returns: nil när mikrofonen är igång, annars skälet att den inte är det.
    @discardableResult
    func start(partial: @escaping (String) -> Void,
               ending: @escaping (Ending) -> Void) -> String? {
        releaseEverything()

        guard let recognizer, recognizer.isAvailable else {
            return "Svensk taligenkänning är inte tillgänglig just nu."
        }

        let id = UUID()
        run = id
        heard = ""
        settled = false
        self.partial = partial
        self.ending = ending

        do {
            let audio = AVAudioSession.sharedInstance()
            try audio.setCategory(.record, mode: .measurement, options: [])
            try audio.setActive(true, options: [])
            audioActive = true

            let input = engine.inputNode
            let format = input.outputFormat(forBus: 0)
            guard format.sampleRate > 0, format.channelCount > 0 else {
                releaseEverything()
                return "Ingen mikrofon tillgänglig."
            }

            let buffer = SFSpeechAudioBufferRecognitionRequest()
            buffer.shouldReportPartialResults = true
            buffer.requiresOnDeviceRecognition = recognizer.supportsOnDeviceRecognition
            buffer.taskHint = .dictation
            buffer.addsPunctuation = true
            request = buffer

            input.installTap(onBus: 0, bufferSize: 1024, format: format) { sample, _ in
                buffer.append(sample)
            }
            hasTap = true
            engine.prepare()
            try engine.start()
            isListening = true

            task = recognizer.recognitionTask(with: buffer) { [weak self] result, error in
                DispatchQueue.main.async {
                    self?.handle(id, result, error)
                }
            }

            DispatchQueue.main.asyncAfter(deadline: .now() + cap) { [weak self] in
                guard let self, self.run == id, self.isListening else { return }
                self.finish()
            }
            return nil
        } catch {
            releaseEverything()
            return "Mikrofonen kunde inte starta. Försök igen."
        }
    }

    /// Användaren är klar. Ljudet avslutas, men sessionen lämnas igång tills
    /// taltjänsten svarat — annars hinner den inte slutföra texten.
    func finish() {
        guard !settled, isListening || task != nil else { return }
        isListening = false
        stopInput()
        // Måste komma efter stopInput och före varje nedrivning: det är det
        // här som säger till taltjänsten att ljudet är slut och att den ska
        // leverera sitt slutresultat.
        request?.endAudio()

        // Svarar taltjänsten inte alls får dikteringen inte hänga sig. Det
        // som hörts levereras då ändå.
        let id = run
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) { [weak self] in
            guard let self, self.run == id, !self.settled else { return }
            self.settle(Dictation.ending(heard: self.heard, final: false,
                                         failed: "Taltjänsten svarade inte. Försök igen."))
        }
    }

    /// Avbryter. Ingen återkoppling ges, och texten är den som ringde ansvarig för.
    func cancel() {
        settled = true
        run = UUID()
        partial = nil
        ending = nil
        heard = ""
        releaseEverything()
    }

    // MARK: - Svaren

    private func handle(_ id: UUID, _ result: SFSpeechRecognitionResult?, _ error: Error?) {
        guard run == id, !settled else { return }

        if let result {
            let text = result.bestTranscription.formattedString
            // Ett tomt slutresultat får aldrig radera det som redan hörts.
            // Det var precis det som gjorde att texten försvann och ersattes
            // av "Inget tal uppfattades".
            if !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                heard = text
                partial?(text)
            }
            if result.isFinal {
                settle(Dictation.ending(heard: heard, final: true, failed: nil))
                return
            }
        }

        guard let error else { return }
        settle(Dictation.ending(heard: heard, final: false, failed: Dictation.reason(error)))
    }

    /// Regeln som buggen handlade om, skild från ljudet så att den går att
    /// mäta med tester: har något hörts behålls det, vad som än hände sedan.
    /// Bara när ingenting hörts får användaren höra att inget uppfattades.
    static func ending(heard: String, final: Bool, failed: String?) -> Ending {
        let text = heard.trimmingCharacters(in: .whitespacesAndNewlines)
        guard text.isEmpty else {
            if final || failed == nil { return .done(text: heard) }
            return .salvaged(text: heard,
                             note: "Taltjänsten blev inte klar, men det här hördes.")
        }
        return .nothing(note: failed ?? "Inget tal uppfattades. Försök igen.")
    }

    private func settle(_ how: Ending) {
        guard !settled else { return }
        settled = true
        isListening = false
        let tell = ending
        partial = nil
        ending = nil
        releaseEverything()
        tell?(how)
    }

    /// Felen från taltjänsten är inte skrivna för någon att läsa. Det här är.
    private static func reason(_ error: Error) -> String {
        let problem = error as NSError
        // 1110 betyder "no speech detected" och är det vanligaste av dem alla.
        if problem.code == 1110 || problem.code == 203 {
            return "Hörde inget. Tryck på Tala och prova igen."
        }
        if problem.domain == NSURLErrorDomain {
            return "Taltjänsten nådde inte nätet. Prova igen, eller hämta svenska för offline-diktering."
        }
        return "Taltjänsten kunde inte tolka talet just nu. Försök igen."
    }

    // MARK: - Mikrofonen

    /// Stänger ljudet in, men lämnar sessionen igång.
    private func stopInput() {
        if engine.isRunning { engine.stop() }
        if hasTap {
            engine.inputNode.removeTap(onBus: 0)
            hasTap = false
        }
    }

    /// Stänger allt, inklusive sessionen. Tål att kallas flera gånger.
    private func releaseEverything() {
        stopInput()
        engine.reset()
        task?.cancel()
        task = nil
        request = nil
        isListening = false
        if audioActive {
            try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
            audioActive = false
        }
    }
}
