import Combine
import SwiftUI
import UIKit

/// Kom igång-guiden på iPhone: samma fem steg som Androids Guide, i samma
/// ordning och med samma formuleringar där plattformarna gör samma sak.
///
/// Skillnaden mot Android är vad appen får veta. Android kan läsa av varje
/// steg; iOS kan bara läsa av mikrofonen. Om tangentbordet är tillagt, har
/// fullständig åtkomst eller är framme just nu säger iOS ingenting till
/// appen. De stegen är därför märkta "kontrollera själv" och räknas aldrig
/// som klara av guiden — ett påstående appen inte kan backa upp hör inte hit.
///
/// Allt som går att läsa av läses om varje gång appen kommer fram igen
/// (`UIApplication.didBecomeActiveNotification` plus `onAppear`), eftersom
/// användaren kommer tillbaka från Inställningar utan att appen får något
/// resultat.
///
/// Utseendet är Tech Rabbit-designen, samma kort, färger och typsnitt som
/// startskärmen. Stegen, texterna och allt som läses av är oförändrade.
struct Onboarding: View {

    @Environment(\.dismiss) private var dismiss

    /// Mikrofonen och taligenkänningen, det enda guiden kan läsa av.
    @State private var micDone = false
    /// Sant när iOS inte frågar igen. Då är Inställningar enda vägen.
    @State private var micRefused = false
    /// Texten användaren dikterar i provfältet.
    @State private var typed = ""
    /// Språket som tangentbordet i fältet rapporterar, när det rapporterar något.
    @State private var inputLanguage: String?
    /// Dikteringen äger frågan om mikrofon och taligenkänning, så att appen
    /// och de två tilläggen säger samma sak. Hålls kvar mellan omritningar.
    @State private var ears = Dictation()

    var body: some View {
        TRSheet("Kom igång", close: { dismiss() }) {
            Text(micDone
                 ? "Mikrofonen är klar. De fyra tangentbordsstegen kan appen inte läsa av — gå igenom dem och kontrollera själv."
                 : "Fem steg. Mikrofonen frågar appen om; de fyra andra görs i Inställningar, och iOS berättar inte för appen när de är gjorda.")
                .trBody()

            step(1, "Tillåt mikrofonen", checked: micDone, selfCheck: false) {
                Text("Dikteringen behöver mikrofonen och taligenkänningen. Frågan ställs här i appen, eftersom ett tangentbord inte får fråga själv. Tap & Talk spelar inte in något; ljudet går till telefonens taltjänst, som gör om det till text.")
                    .trBody()
                if micDone {
                    done("Mikrofonen och taligenkänningen är tillåtna.")
                } else if micRefused {
                    TRButton(title: "Öppna inställningarna", filled: true) { openSettings() }
                    Text("iOS frågar inte igen när man svarat nej. Slå på Mikrofon och Taligenkänning under Inställningar → Tap & Talk.")
                        .trFootnote()
                } else {
                    TRButton(title: "Tillåt mikrofonen", filled: true) { askForMicrophone() }
                }
            }

            step(2, "Lägg till tangentbordet", checked: false, selfCheck: true) {
                Text("Tangentbordet är huvudvägen: med det dikterar du i varje app där du skriver. iOS kräver att du själv lägger till det.")
                    .trBody()
                SettingsPath("Inställningar → Allmänt → Tangentbord → Tangentbord → Lägg till nytt tangentbord → Tap & Talk")
                TRButton(title: "Öppna inställningarna") { openSettings() }
                Text("Knappen öppnar Tap & Talks egen sida i Inställningar. Därifrån finns Tangentbord ett tryck bort, och hela vägen ovan fungerar också.")
                    .trFootnote()
            }

            step(3, "Slå på Tillåt fullständig åtkomst", checked: false, selfCheck: true) {
                Text("Utan fullständig åtkomst får tangentbordet inte använda mikrofonen, och kan inte be om den själv — då går dikteringen inte att skicka vidare till fältet du skriver i.")
                    .trBody()
                SettingsPath("Inställningar → Allmänt → Tangentbord → Tangentbord → Tap & Talk → Tillåt fullständig åtkomst")
                TRButton(title: "Öppna inställningarna") { openSettings() }
                Text("iOS varnar att tangentbordet då kan se allt du skriver. Det är samma varning för alla tangentbord. Tap & Talk loggar ingenting, sparar ingenting och skickar ingenting vidare; det enda som lämnar telefonen är talet, till Apples taltjänst, och bara medan du håller mikrofonen igång.")
                    .trFootnote()
            }

            step(4, "Byt till Tap & Talk", checked: false, selfCheck: true) {
                Text("Nu finns tangentbordet, men telefonen använder fortfarande ditt vanliga. Tryck i ett textfält och håll in jordgloben 🌐 nere till vänster — står det en smiley 🙂 där i stället är det samma knapp — och välj Tap & Talk i listan.")
                    .trBody()
                Text("Du byter tillbaka med samma knapp, eller med ⌨-knappen i Tap & Talks egen rad.")
                    .trFootnote()
            }

            step(5, "Prova att diktera", checked: false, selfCheck: true) {
                Text("Tryck i fältet nedan, byt till Tap & Talk och tryck på Tala. Säg en mening. Texten ska hamna i fältet medan du talar.")
                    .trBody()
                ProofField(text: $typed, language: $inputLanguage)
                    .frame(minHeight: 96)
                receipt
            }
        }
        // Användaren kommer tillbaka från Inställningar utan att appen får
        // något svar. Därför läses läget om både vid öppning och varje gång
        // appen blir aktiv igen.
        .onAppear { reread() }
        .onReceive(NotificationCenter.default.publisher(
            for: UIApplication.didBecomeActiveNotification)) { _ in reread() }
    }

    // MARK: - Kvittot

    /// Kvittot säger det som går att mäta: att texten kom in, och vilket
    /// språk tangentbordet rapporterar. Vilket tangentbord som skrev säger
    /// iOS inte, så det sista ledet får användaren själv se — och guiden
    /// säger exakt vad man ska titta efter.
    @ViewBuilder private var receipt: some View {
        if typed.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            Text("Inget i fältet än.")
                .trFootnote()
        } else {
            done("Texten kom in i fältet.")
            if let inputLanguage {
                Text("Tangentbordet som skrev rapporterar språket \(inputLanguage).")
                    .trFootnote()
            }
            Text("iOS berättar inte för appen vilket tangentbord som skrev. Ser du den svarta raden med den gröna Tala-knappen under fältet kom texten från Tap & Talk. Ser du Apples vanliga tangentbord kom den från ett annat — håll in 🌐 och välj Tap & Talk.")
                .trFootnote()
        }
    }

    /// En rad som bekräftar något appen faktiskt har läst av.
    private func done(_ text: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(TR.green)
                .accessibilityHidden(true)
            Text(text)
                .trFont(15, .bold, relativeTo: .subheadline, lineHeight: 21)
                .foregroundStyle(TR.green)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(text)
    }

    // MARK: - Stegen

    /// Ett steg: siffra, märke, rubrik och innehåll, i ett kort av samma
    /// form som textfältet på startskärmen. Märket är grönt bara när appen
    /// själv har läst av att steget är gjort.
    @ViewBuilder private func step<Content: View>(
        _ number: Int,
        _ title: String,
        checked: Bool,
        selfCheck: Bool,
        @ViewBuilder content: () -> Content
    ) -> some View {
        TRCard {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Image(systemName: checked ? "checkmark.circle.fill" : "\(number).circle")
                    .foregroundStyle(checked ? TR.green : TR.faint)
                    .accessibilityHidden(true)
                Text(title)
                    .trHeading()
                Spacer(minLength: 8)
                if selfCheck {
                    TRBadge("kontrollera själv")
                }
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel(checked
                                ? "Steg \(number), \(title). Klart."
                                : (selfCheck
                                   ? "Steg \(number), \(title). Kontrollera själv."
                                   : "Steg \(number), \(title)."))
            content()
        }
    }

    // MARK: - Läget

    private func reread() {
        micDone = Dictation.permitted
        micRefused = !micDone && Dictation.permissionRefused
    }

    /// Frågar om mikrofon och taligenkänning. Dictation äger frågan, så
    /// appen, iMessage-tillägget och tangentbordet säger samma sak.
    private func askForMicrophone() {
        ears.authorize { _ in reread() }
    }

    private func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }
}

/// En väg genom Inställningar, satt i monospace så att leden går att följa
/// med ögat och inte blandas med den löpande texten. Heter inte Path, för
/// att inte skugga SwiftUI:s egen Path.
///
/// Monospace är systemets: Archivo har ingen fast bredd, och vägen ska gå
/// att läsa led för led.
private struct SettingsPath: View {
    let text: String

    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.system(.footnote, design: .monospaced))
            .foregroundStyle(TR.text)
            .textSelection(.enabled)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(TR.card, in: RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14)
                .strokeBorder(TR.border, lineWidth: 1))
    }
}

/// Provfältet. Ett vanligt UITextView, av ett enda skäl: SwiftUI:s TextField
/// säger inget om vilket tangentbord som skriver, men en UITextView kan
/// läsa `textInputMode` och rapportera språket det uppger.
private struct ProofField: UIViewRepresentable {
    @Binding var text: String
    @Binding var language: String?

    func makeUIView(context: Context) -> UITextView {
        let field = UITextView()
        // Archivo i UIKit-ändan också, skalat mot Dynamic Type på samma
        // sätt som TRFont gör i SwiftUI. Saknas filen faller den tillbaka
        // på systemtypsnittet.
        let archivo = UIFont(name: "Archivo-Regular", size: 17)
            ?? .preferredFont(forTextStyle: .body)
        field.font = UIFontMetrics(forTextStyle: .body).scaledFont(for: archivo)
        field.adjustsFontForContentSizeCategory = true
        field.backgroundColor = UIColor(TR.card)
        field.textColor = UIColor(TR.text)
        field.tintColor = UIColor(TR.green)
        field.keyboardAppearance = .dark
        field.layer.cornerRadius = 22
        field.layer.borderWidth = 1.5
        field.layer.borderColor = UIColor(TR.border).cgColor
        field.textContainerInset = UIEdgeInsets(top: 14, left: 12, bottom: 14, right: 12)
        field.delegate = context.coordinator
        field.accessibilityLabel = "Provfält"
        field.accessibilityHint = "Diktera en mening med Tap & Talk-tangentbordet."
        return field
    }

    func updateUIView(_ field: UITextView, context: Context) {
        // Bindningarna förnyas vid varje omritning, så hjälparen får den
        // färska kopian i stället för den den föddes med.
        context.coordinator.owner = self
        // Texten sätts bara när den skiljer sig: annars flyttas markören
        // varje gång SwiftUI ritar om.
        if field.text != text { field.text = text }
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UITextViewDelegate {
        var owner: ProofField

        init(_ owner: ProofField) { self.owner = owner }

        func textViewDidChange(_ field: UITextView) {
            owner.text = field.text
            owner.language = field.textInputMode?.primaryLanguage
        }
    }
}
