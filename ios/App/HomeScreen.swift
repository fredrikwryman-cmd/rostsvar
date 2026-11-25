import Combine
import SwiftUI
import UIKit

/// Dikteringsskärmen, appens startskärm: samma uppbyggnad som Androids
/// `Home.java`. Tryck på Tala, tala svenska, läs igenom texten, rätta den för
/// hand om du vill, och välj själv var den skickas.
///
/// Utseendet är Tech Rabbit-designen v1. Underlaget ligger i
/// `design/techrabbit-taptalk-v1/`, ritningarna i `TechRabbit.swift`, och
/// paragraftecknen nedan pekar på avsnitt i underlagets direktiv.
///
/// Taligenkänningen är den delade `Dictation`, samma som tangentbordet och
/// iMessage-tillägget använder. Det betyder att avslutsregeln i
/// `Dictation.ending` gäller också här: har något hörts levereras det, och
/// ett tomt slutresultat får aldrig skriva över det.
///
/// Appen skickar aldrig något själv och når inget nät på egen hand. Det enda
/// som lämnar appen är texten du själv väljer att dela, och talet, som
/// telefonens egen taltjänst tolkar.
struct HomeScreen: View {

    @State private var state = HomeState()
    @State private var note = "Tryck på Tala och diktera. Du läser texten och väljer själv var den skickas."
    /// Sant när mikrofonen är avslagen för appen i Inställningar. Då frågar
    /// iOS inte igen, och Inställningar är enda vägen vidare.
    @State private var micBlocked = false
    /// Vakt för taligenkänningen. Byts när dikteringen avbryts eller går om.
    @State private var generation = UUID()
    @State private var ears = Dictation()

    /// Sant när appen lämnades med mikrofonen öppen. Beskedet om det sparas
    /// till dikteringen svarat, så att det inte skrivs över av svaret.
    @State private var leftWhileListening = false

    @State private var showGuide = false
    @State private var showAbout = false
    @State private var showShare = false

    /// Guiden visas en gång av sig själv, sedan när man själv öppnar den.
    /// Samma nyckelnamn som Android använder.
    @AppStorage("guiden_visad") private var guideSeen = false

    /// Sant medan användaren rättar texten för hand. Behövs för att kunna
    /// fälla ned systemtangentbordet igen: utan en väg ut låg det kvar över
    /// Tala, Rensa och Skicka, och skärmen var en återvändsgränd.
    @FocusState private var editing: Bool

    /// Reducera rörelse i Inställningar. §7: inga pulserande ringar, ingen
    /// andande glöd, ingen blinkande markör, inga tonade lägesbyten.
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// Lägesbytet tonas. Svaret kommer från `Move`, som är den enda
    /// kontrollen för reducerad rörelse: vid påslagen inställning blir
    /// tonandet en crossfade på 160 ms i stället för att försvinna, för ett
    /// byte utan animation hoppar, och ett hopp är också rörelse.
    private var fade: Animation { Move.change(reduced: reduceMotion) }

    /// Appens läge. Skärmen döljs när den inte är i förgrunden: utkastet är
    /// transkriptet, och transkriptet är känsligt enligt app-profilen.
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        ZStack {
            TRBackdrop()
            GeometryReader { geo in
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        header.padding(.top, Metric.gapXS)
                        ingress.padding(.top, Metric.gapML)
                        field.padding(.top, Metric.gapL)
                        micBlock.padding(.top, Metric.gapXXL)
                        actions.padding(.top, Metric.gapL)
                            .padding(.bottom, Metric.gapSM)
                    }
                    .padding(.horizontal, Metric.side)
                    // Vid vanliga textstorlekar är minimihöjden hela skärmen.
                    // Då står sidan still och ser ut precis som förut: fältet
                    // tar det som blir över, foten står kvar längst ner. Ställer
                    // användaren upp textstorleken växer stapeln förbi skärmen,
                    // och först då börjar sidan rulla i stället för att klippa.
                    .frame(minHeight: geo.size.height)
                }
                .scrollDismissesKeyboard(.interactively)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(TR.bg.ignoresSafeArea())
        // Fotografiet till app switchern tas under övergången, alltså redan
        // vid inactive. Ett ställe, för hela skärmen.
        .trShield(scenePhase != .active)
        // §1: skärmen är ritad bara för mörkt läge.
        .preferredColorScheme(.dark)
        .sheet(isPresented: $showGuide) { Onboarding() }
        .sheet(isPresented: $showAbout) { About() }
        .sheet(isPresented: $showShare) { ShareSheet(text: state.trimmed) }
        // Systemtangentbordet täcker Tala, Rensa och Skicka medan man rättar
        // texten. Raden ovanför det är den enda plats iOS ger en väg ut som
        // alltid finns, också för den som inte vet att man kan svepa.
        // Ordet är inte "Klar": det betyder stäng mikrofonen i den här appen.
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Dölj tangentbordet") { editing = false }
                    .trFont(TypeScale.button)
                    .foregroundStyle(TR.green)
            }
        }
        .onAppear { arrive() }
        // §9: läget annonseras när det byts, inte vid varje tecken.
        //
        // Det är note som annonseras och inte chip-ordet. Chip säger "Klar";
        // note säger varför — att mikrofonen är avstängd i Inställningar, att
        // taltjänsten inte nådde nätet, att texten står kvar. En seende läser
        // båda, och den som lyssnar fick förut bara det ena ordet. Två
        // annonseringar för samma händelse vore värre än en, så det blev den
        // som bär innehållet. Lägesordet står kvar som läsbar text i foten.
        .onChange(of: note) { now in
            UIAccessibility.post(notification: .announcement, argument: now)
        }
        // Mikrofonen stängs när appen lämnas, som på Android. Skillnaden är
        // att vi avslutar i stället för att avbryta: det som hörts levereras
        // och står kvar i fältet.
        .onReceive(NotificationCenter.default.publisher(
            for: UIApplication.didEnterBackgroundNotification)) { _ in leave() }
        // Användaren kan ha tillåtit mikrofonen i Inställningar medan appen
        // låg undan, och då får knappen inte stå kvar och säga nej.
        .onReceive(NotificationCenter.default.publisher(
            for: UIApplication.didBecomeActiveNotification)) { _ in
                if Dictation.permitted { micBlocked = false }
                checkService()
            }
    }

    // MARK: - Huvudet, §4.1

    private var header: some View {
        HStack(spacing: Metric.gapM) {
            mark
            VStack(alignment: .leading, spacing: 0) {
                Text("Tap&Talk")
                    .trFont(TypeScale.appName)
                    .foregroundStyle(TR.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Text("TECH RABBIT")
                    .trFont(TypeScale.wordmark)
                    .foregroundStyle(TR.green)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .combine)
            pill("Guide", hint: "Öppna kom igång-guiden") { showGuide = true }
            pill("Om", hint: "Om appen och tangentbordet") { showAbout = true }
        }
        .frame(height: Metric.headerHeight)
    }

    /// Ikonringen med Tech Rabbits kaninsymbol.
    private var mark: some View {
        ZStack {
            Circle().fill(TR.card)
            Circle().strokeBorder(TR.green, lineWidth: Metric.edge)
            TRIcon.rabbit(Metric.markIcon, color: TR.text)
        }
        .frame(width: Metric.markSize, height: Metric.markSize)
        .accessibilityHidden(true)
    }

    /// Pillerknapp i huvudet. Den syns som 38 hög, men träffytan är 44, som
    /// den minsta en knapp får vara.
    private func pill(_ title: String, hint: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .trFont(TypeScale.pill)
                .foregroundStyle(TR.text)
                .padding(.horizontal, Metric.gapML)
                .frame(height: Metric.pillHeight)
                .background(TR.card, in: Capsule())
                .overlay(Capsule().strokeBorder(TR.border, lineWidth: Metric.hairline))
        }
        .buttonStyle(.plain)
        .frame(minHeight: Metric.touch)
        .contentShape(Rectangle())
        // Etiketten börjar med den text som syns, och säger sedan vad
        // knappen gör.
        .accessibilityLabel("\(title). \(hint)")
    }

    // MARK: - Ingressen, §4.2

    /// Underlaget har en fast ingress. Appen använder samma rad till att
    /// svara användaren — om mikrofonen nekades, om taltjänsten tystnade,
    /// om texten är klar — och den upplysningen får inte försvinna. Raden
    /// börjar därför med underlagets text och byts när något händer.
    private var ingress: some View {
        Text(note)
            .trFont(TypeScale.body)
            .foregroundStyle(TR.muted)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityAddTraits(.updatesFrequently)
    }

    // MARK: - Textfältet, §4.3

    private var field: some View {
        VStack(alignment: .leading, spacing: Metric.gapS) {
            page
            footer
        }
        .padding(EdgeInsets(top: Metric.fieldTop, leading: Metric.cardPad,
                            bottom: Metric.fieldBottom, trailing: Metric.cardPad))
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(TR.fieldFill,
                    in: RoundedRectangle(cornerRadius: Metric.cardRadius))
        .overlay(RoundedRectangle(cornerRadius: Metric.cardRadius)
            .strokeBorder(state.isRecording ? TR.green : TR.border,
                          lineWidth: Metric.edge))
        .frame(minHeight: Metric.fieldMinHeight)
        .animation(fade, value: state.isRecording)
    }

    /// Fältets innehåll. Går det att skriva i är det en riktig redigerare;
    /// medan mikrofonen är på är det en läsyta med grön markör, precis som
    /// underlaget visar. Texten är densamma, bara ytan byts.
    ///
    /// Båda ytorna tar den höjd som blir över, så att foten står kvar
    /// längst ner i fältet och inte flyttar sig när texten växer.
    @ViewBuilder private var page: some View {
        if state.canEdit {
            editor.frame(maxHeight: .infinity, alignment: .topLeading)
        } else {
            reading.frame(maxHeight: .infinity, alignment: .topLeading)
        }
    }

    private var editor: some View {
        ZStack(alignment: .topLeading) {
            TextEditor(text: $state.draft)
                .scrollContentBackground(.hidden)
                .trFont(TypeScale.draft)
                .foregroundStyle(TR.text)
                .tint(TR.green)
                .padding(.horizontal, Metric.editorInsetX)
                .padding(.vertical, Metric.editorInsetY)
                .focused($editing)
                .accessibilityLabel("Din text")
            if state.draft.isEmpty {
                placeholder
            }
        }
    }

    /// Läsytan under diktering. Den rullar med till sista raden, så att den
    /// text som just hörts syns även när utkastet blivit långt.
    private var reading: some View {
        ScrollViewReader { scroll in
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if state.draft.isEmpty && !state.isRecording {
                        placeholder
                    } else {
                        written
                    }
                    Color.clear.frame(height: 1).id(Self.lastLine)
                }
                .frame(maxWidth: .infinity, alignment: .topLeading)
            }
            .scrollIndicators(.hidden)
            .onChange(of: state.draft) { _ in
                withAnimation(fade) { scroll.scrollTo(Self.lastLine, anchor: .bottom) }
            }
        }
    }

    private static let lastLine = "sista-raden"

    /// Texten med den gröna markören sist. Markören blinkar var 450:e
    /// millisekund medan mikrofonen är på, och står still när rörelse ska
    /// vara reducerad.
    @ViewBuilder private var written: some View {
        if state.isRecording && !reduceMotion {
            TimelineView(.periodic(from: Date(), by: 0.45)) { timeline in
                let tick = Int(timeline.date.timeIntervalSinceReferenceDate / 0.45)
                line(caret: tick % 2 == 0)
            }
        } else {
            line(caret: state.isRecording)
        }
    }

    private func line(caret: Bool) -> some View {
        (Text(state.draft).foregroundColor(TR.text)
            + Text(verbatim: caret ? "▏" : "\u{200B}").foregroundColor(TR.green))
            .trFont(TypeScale.draft)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .accessibilityLabel("Din text")
            .accessibilityValue(state.draft)
    }

    private var placeholder: some View {
        Text("Texten du dikterar hamnar här. Du kan rätta den för hand.")
            .trFont(TypeScale.draft)
            .foregroundStyle(TR.faint)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }

    /// Fältets fot: läget till vänster, antalet tecken till höger. §4.3.
    private var footer: some View {
        HStack(spacing: 0) {
            HStack(spacing: Metric.gapXS) {
                // Medan taltjänsten slutför byts pricken mot något som rör
                // sig. Väntan är fem sekunder på iOS och åtta på Android, och
                // en stillastående prick säger inte att något pågår.
                if chip == .interpreting {
                    ProgressView()
                        .scaleEffect(0.6)
                        .frame(width: Metric.spinner, height: Metric.spinner)
                        .tint(TR.muted)
                        .accessibilityHidden(true)
                } else {
                    Circle()
                        .fill(chip.dot)
                        .frame(width: Metric.dot, height: Metric.dot)
                        .accessibilityHidden(true)
                }
                Text(chip.word)
                    .trFont(TypeScale.chip)
                    .foregroundStyle(chip.ink)
            }
            Spacer(minLength: Metric.gapS)
            Text("\(state.draft.count) tecken")
                .trFont(TypeScale.counter)
                .foregroundStyle(TR.faint)
        }
        .animation(fade, value: chip)
    }

    /// De lägen underlaget ritar, §6, plus ett som appen har och underlaget
    /// inte modellerar: taltjänsten har fått frågan men inte svarat ännu.
    /// Att då säga "Klar" vore att svara åt den.
    private enum Chip: Equatable {
        case ready, listening, interpreting, done

        var word: String {
            switch self {
            case .ready: return "Redo"
            case .listening: return "Lyssnar…"
            case .interpreting: return "Tolkar…"
            case .done: return "Klar"
            }
        }

        var dot: Color {
            switch self {
            case .ready: return TR.dimDot
            case .listening: return TR.green
            case .interpreting, .done: return TR.muted
            }
        }

        var ink: Color {
            switch self {
            case .listening: return TR.green
            case .interpreting: return TR.muted
            case .ready, .done: return TR.faint
            }
        }
    }

    private var chip: Chip {
        if state.isRecording { return .listening }
        if state.phase == .finishing { return .interpreting }
        return state.hasText ? .done : .ready
    }

    // MARK: - Tala-knappen, §4.4

    private var micBlock: some View {
        VStack(spacing: Metric.gapSM) {
            ZStack {
                TRRings(listening: state.isRecording, still: reduceMotion)
                Button { micTapped() } label: { face }
                    .buttonStyle(.plain)
                    .disabled(!state.canTalk)
                    .accessibilityLabel(micWord)
                    .accessibilityHint(state.isRecording
                                       ? "Avslutar diktering"
                                       : "Startar diktering")
            }
            .frame(width: Metric.micRing, height: Metric.micRing)
            Text(micWord)
                .trFont(TypeScale.heading)
                .foregroundStyle(TR.text)
                .accessibilityHidden(true)
        }
        .frame(maxWidth: .infinity)
        .animation(fade, value: state.isRecording)
    }

    /// Knappens yta med glöden. I vila andas glöden, under inspelning står
    /// den still på rött. §7.
    @ViewBuilder private var face: some View {
        if reduceMotion {
            disc(glow: TRGlow.opacity(listening: state.isRecording, still: true, at: 0))
        } else {
            TimelineView(.animation) { timeline in
                disc(glow: TRGlow.opacity(
                    listening: state.isRecording,
                    still: false,
                    at: timeline.date.timeIntervalSinceReferenceDate))
            }
        }
    }

    private func disc(glow: Double) -> some View {
        Circle()
            .fill(micFill)
            .frame(width: Metric.micDisc, height: Metric.micDisc)
            .shadow(color: micFill.opacity(glow), radius: TRGlow.radius)
            .overlay(glyph)
    }

    @ViewBuilder private var glyph: some View {
        if state.isRecording {
            RoundedRectangle(cornerRadius: Metric.keyRadius)
                .fill(Color.white)
                .frame(width: Metric.micStop, height: Metric.micStop)
        } else {
            TRIcon.mic(Metric.micIcon, color: TR.onGreen)
        }
    }

    private var micFill: Color {
        let base = state.isRecording ? TR.red : TR.green
        // Medan frågan ställs eller taltjänsten slutför går knappen inte att
        // trycka på. Den dämpas i stället för att bytas ut, så att det syns
        // att det är samma knapp som strax är tillbaka.
        return state.canTalk ? base : base.opacity(0.45)
    }

    /// Underlagets §6 har två ord på knappen och säger "Tala" också när det
    /// redan står text i fältet. Här står det "Tala om" i stället, samma ord
    /// som Android använder: det säger att nästa diktering läggs efter
    /// texten som redan finns, inte i stället för den. En medveten avvikelse
    /// från underlaget, till förmån för samma ordval på båda plattformarna.
    ///
    /// Ordet för att stänga mikrofonen är "Klar" och inte "Stoppa". Det var
    /// "Stoppa" här och "Klar" på de fem andra ytorna — tangentbordet,
    /// iMessage-tillägget, Androids hemskärm, bubblan och rösttangentbordet
    /// — och Om-skärmen instruerade "Tryck på Klar" om en knapp intill som
    /// sa Stoppa.
    ///
    /// Det fjärde ordet hör till ett läge underlaget inte ritar: mikrofonen
    /// är avslagen i Inställningar, och knappen leder dit i stället för att
    /// fråga om igen.
    private var micWord: String {
        if state.isRecording { return "Klar" }
        if micBlocked { return "Tillåt mikrofon" }
        return state.hasText ? "Tala om" : "Tala"
    }

    // MARK: - Rensa och Skicka, §4.5

    private var actions: some View {
        HStack(spacing: Metric.gapSM) {
            clearButton
            sendButton
        }
    }

    private var clearButton: some View {
        Button {
            state.clear()
            note = "Fältet är tomt. Tryck på Tala och diktera igen."
        } label: {
            let ink = state.canClear ? TR.text : TR.disabled
            HStack(spacing: Metric.gapS) {
                TRIcon.trash(Metric.actionIcon, color: ink)
                Text("Rensa").trFont(TypeScale.button).foregroundStyle(ink)
            }
            .frame(maxWidth: .infinity)
            .frame(height: Metric.controlHeight)
            .background(TR.card, in: Capsule())
            .overlay(Capsule().strokeBorder(
                state.canClear ? TR.dimDot : TR.border, lineWidth: Metric.hairline))
        }
        .buttonStyle(.plain)
        .disabled(!state.canClear)
        .animation(fade, value: state.canClear)
        .accessibilityLabel("Rensa fältet")
    }

    private var sendButton: some View {
        Button {
            Haptics.fire(.success)
            showShare = true
        } label: {
            let ink = state.canSend ? TR.onGreen : TR.disabled
            HStack(spacing: Metric.gapS) {
                TRIcon.plane(Metric.actionIcon, color: ink)
                Text("Skicka").trFont(TypeScale.button).foregroundStyle(ink)
            }
            .frame(maxWidth: .infinity)
            .frame(height: Metric.controlHeight)
            .background(state.canSend ? TR.green : TR.card, in: Capsule())
            .overlay(Capsule().strokeBorder(
                state.canSend ? TR.green : TR.border, lineWidth: Metric.hairline))
        }
        .buttonStyle(.plain)
        .disabled(!state.canSend)
        .animation(fade, value: state.canSend)
        .accessibilityLabel("Skicka texten vidare")
    }

    // MARK: - Dikteringen

    private func micTapped() {
        if state.isRecording { finish(); return }
        guard state.phase == .idle else { return }
        if micBlocked && !Dictation.permitted {
            Haptics.fire(.warning)
            openSettings()
            return
        }
        guard ears.isAvailable else {
            Haptics.fire(.warning)
            note = Notice.unavailable
            return
        }

        Haptics.fire(.recordStart)
        let id = UUID()
        generation = id
        state.ask()
        note = "Kontrollerar mikrofon och taligenkänning …"
        ears.authorize { refused in
            guard generation == id else { return }
            guard let refused else {
                micBlocked = false
                start(id)
                return
            }
            // Svarade användaren nej frågar iOS inte igen. Då är det
            // Inställningar som gäller, och knappen säger det.
            Haptics.fire(.warning)
            micBlocked = Dictation.permissionRefused
            state.rest()
            note = micBlocked
                ? "Mikrofonen är avstängd för Tap&Talk. Tryck på Tillåt mikrofon för att öppna inställningarna."
                : refused
        }
    }

    private func start(_ id: UUID) {
        // Mikrofonen startas före beginRecording: går den inte igång ska
        // skärmen stå kvar i vila med texten orörd.
        let refused = ears.start(partial: { text in
            guard generation == id else { return }
            state.transcribe(text)
        }, ending: { how in
            guard generation == id else { return }
            state.rest()
            switch how {
            case .done(let text):
                state.transcribe(text)
                settle("Läs igenom texten, rätta den om du vill, och tryck på Skicka.")
            case .salvaged(let text, let why):
                // Texten hördes. Då säger vi aldrig att inget uppfattades.
                state.transcribe(text)
                settle(why + " Läs igenom texten och tryck på Skicka.")
            case .nothing(let why):
                // Hördes något den här omgången lyckades den, hur den än
                // slutade. Stod det bara text kvar sedan förra gången är
                // skälet det användaren behöver höra.
                if state.heardSomething {
                    settle("Läs igenom texten, rätta den om du vill, och tryck på Skicka.")
                } else {
                    Haptics.fire(.warning)
                    settle(why)
                }
            }
        })
        if let refused {
            Haptics.fire(.warning)
            state.rest()
            note = refused
            return
        }
        state.beginRecording()
        note = "Mikrofonen är på. Tala svenska och tryck på Klar när du är färdig."
    }

    /// Svarets besked, med raden om att appen lämnades först när det hände.
    /// Dikteringens eget svar får aldrig radera den upplysningen.
    private func settle(_ message: String) {
        guard leftWhileListening else { note = message; return }
        leftWhileListening = false
        note = "Mikrofonen stängdes när du lämnade appen. " + message
    }

    /// Användaren är klar. Dikteringen avslutar ljudet själv och svarar genom
    /// sin ending-stängning; här väntar vi bara på svaret.
    private func finish() {
        guard state.isRecording else { return }
        Haptics.fire(.recordStop)
        state.finishRecording()
        note = "Tolkar det du sa …"
        ears.finish()
    }

    // MARK: - Appens gång

    /// Säger till i förväg när svensk taligenkänning inte går att nå, i
    /// stället för att låta användaren trycka på Tala och få ett fel.
    /// Läget kan ha bytts medan appen låg undan: nätet kom eller försvann.
    private func checkService() {
        guard state.phase == .idle else { return }
        if !ears.isAvailable {
            note = Notice.unavailable
        } else if note == Notice.unavailable {
            note = "Tryck på Tala och diktera. Du läser texten och väljer själv var den skickas."
        }
    }

    private func arrive() {
        if Dictation.permitted { micBlocked = false }
        checkService()
        // Första starten går direkt till guiden, som på Android.
        guard !guideSeen else { return }
        guideSeen = true
        showGuide = true
    }

    private func leave() {
        editing = false
        guard state.isRecording else { return }
        state.finishRecording()
        leftWhileListening = true
        note = "Mikrofonen stängdes när du lämnade appen. Texten står kvar."
        ears.finish()
    }

    private func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }
}

/// iOS egen delningsruta. Appen skickar inget själv: texten lämnas här, och
/// användaren väljer Messenger, Signal, SMS eller något annat.
private struct ShareSheet: UIViewControllerRepresentable {
    let text: String

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [text], applicationActivities: nil)
    }

    func updateUIViewController(_ sheet: UIActivityViewController, context: Context) { }
}
