import Combine
import SwiftUI
import UIKit

/// Dikteringsskärmen, appens startskärm: samma uppbyggnad som Androids
/// `Home.java`. Tryck på Tala, tala svenska, läs igenom texten, rätta den för
/// hand om du vill, och välj själv var den skickas.
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

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            header
            Text(note)
                .font(.subheadline)
                .foregroundStyle(Look.dim)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.updatesFrequently)
            field
            micButton
            HStack(spacing: 10) {
                clearButton
                sendButton
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .padding(.bottom, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Look.bg.ignoresSafeArea())
        .preferredColorScheme(.dark)
        .sheet(isPresented: $showGuide) { Onboarding() }
        .sheet(isPresented: $showAbout) { About() }
        .sheet(isPresented: $showShare) { ShareSheet(text: state.trimmed) }
        .onAppear { arrive() }
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
            }
    }

    // MARK: - Delarna

    private var header: some View {
        HStack(spacing: 10) {
            Text("Tap & Talk")
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(Look.ink)
            Spacer(minLength: 8)
            quietButton("Guide", hint: "Öppna kom igång-guiden") { showGuide = true }
            quietButton("Om", hint: "Om appen och tangentbordet") { showAbout = true }
        }
    }

    private var field: some View {
        ZStack(alignment: .topLeading) {
            TextEditor(text: $state.draft)
                .scrollContentBackground(.hidden)
                .font(.system(size: 19))
                .foregroundStyle(Look.ink)
                .tint(Look.green)
                .padding(10)
                // Fältet låses medan mikrofonen är på, men släcks inte:
                // disabled hade gjort den dikterade texten grå medan den
                // skrevs, och det är just då den ska vara läsbar.
                .allowsHitTesting(state.canEdit)
                .accessibilityLabel("Din text")
            if state.draft.isEmpty {
                Text("Texten du dikterar hamnar här. Du kan rätta den för hand.")
                    .font(.system(size: 19))
                    .foregroundStyle(Look.off)
                    .padding(.horizontal, 15)
                    .padding(.vertical, 18)
                    .allowsHitTesting(false)
            }
        }
        .background(Look.field, in: RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(Look.border, lineWidth: 1))
        .frame(maxHeight: .infinity)
    }

    private var micButton: some View {
        Button {
            micTapped()
        } label: {
            Text(micTitle)
                .font(.system(size: 22, weight: .semibold))
                .frame(maxWidth: .infinity)
                .frame(height: 84)
                .background(micFill, in: RoundedRectangle(cornerRadius: 16))
                .foregroundStyle(state.canTalk ? Look.darkInk : Look.off)
        }
        .buttonStyle(.plain)
        .disabled(!state.canTalk)
        .accessibilityLabel(micLabel)
    }

    private var sendButton: some View {
        Button {
            showShare = true
        } label: {
            Text("Skicka")
                .font(.system(size: 19, weight: .medium))
                .frame(maxWidth: .infinity)
                .frame(height: 58)
                .overlay(RoundedRectangle(cornerRadius: 14)
                    .strokeBorder(state.canSend ? Look.green : Look.border, lineWidth: 1))
                .foregroundStyle(state.canSend ? Look.green : Look.off)
        }
        .buttonStyle(.plain)
        .disabled(!state.canSend)
        .accessibilityLabel("Skicka texten vidare")
    }

    private var clearButton: some View {
        Button {
            state.clear()
            note = "Fältet är tomt. Tryck på Tala och diktera igen."
        } label: {
            Text("Rensa")
                .font(.system(size: 19, weight: .medium))
                .frame(maxWidth: .infinity)
                .frame(height: 58)
                .overlay(RoundedRectangle(cornerRadius: 14)
                    .strokeBorder(Look.border, lineWidth: 1))
                .foregroundStyle(state.canClear ? Look.dim : Look.off)
        }
        .buttonStyle(.plain)
        .disabled(!state.canClear)
        .accessibilityLabel("Rensa fältet")
    }

    private func quietButton(_ title: String, hint: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 15))
                .padding(.horizontal, 14)
                .frame(height: 46)
                .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Look.border, lineWidth: 1))
                .foregroundStyle(Look.dim)
        }
        .buttonStyle(.plain)
        // Etiketten börjar med den text som syns, och säger sedan vad
        // knappen gör.
        .accessibilityLabel("\(title). \(hint)")
    }

    // MARK: - Knapparnas ord, hämtade ur läget

    private var micTitle: String {
        if state.isRecording { return "Klar" }
        if micBlocked { return "Tillåt mikrofon" }
        return state.hasText ? "Tala om" : "Tala"
    }

    private var micLabel: String {
        if state.isRecording { return "Klar, stäng mikrofonen" }
        if micBlocked { return "Öppna inställningarna och tillåt mikrofonen" }
        return "Tala, starta mikrofonen"
    }

    private var micFill: Color {
        guard state.canTalk else { return Look.field }
        return state.isRecording ? Look.greenDeep : Look.green
    }

    // MARK: - Dikteringen

    private func micTapped() {
        if state.isRecording { finish(); return }
        guard state.phase == .idle else { return }
        if micBlocked && !Dictation.permitted { openSettings(); return }
        guard ears.isAvailable else {
            note = "Svensk taligenkänning är inte tillgänglig just nu."
            return
        }

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
            micBlocked = Dictation.permissionRefused
            state.rest()
            note = micBlocked
                ? "Mikrofonen är avstängd för Tap & Talk. Tryck på Tillåt mikrofon för att öppna inställningarna."
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
                settle(state.heardSomething
                       ? "Läs igenom texten, rätta den om du vill, och tryck på Skicka."
                       : why)
            }
        })
        if let refused {
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
        state.finishRecording()
        note = "Tolkar det du sa …"
        ears.finish()
    }

    // MARK: - Appens gång

    private func arrive() {
        if Dictation.permitted { micBlocked = false }
        // Första starten går direkt till guiden, som på Android.
        guard !guideSeen else { return }
        guideSeen = true
        showGuide = true
    }

    private func leave() {
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
