import SwiftUI

@main
struct RostsvarApp: App {
    var body: some Scene {
        WindowGroup {
            Home()
        }
    }
}

/// Startskärmen. Tangentbordet är huvudvägen: med det dikterar man i varje
/// app där man skriver. iMessage-tillägget finns kvar som genväg i
/// Meddelanden, men pekas inte längre ut som förstaval.
private struct Home: View {

    /// Guiden visas en gång av sig själv, sedan när man själv öppnar den.
    /// Samma nyckel som Android använder, så att de två sidorna går att
    /// läsa som ett och samma upplägg.
    @AppStorage("guiden_visad") private var guideSeen = false
    @State private var showGuide = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Button {
                        showGuide = true
                    } label: {
                        Label(guideSeen ? "Öppna kom igång-guiden" : "Kom igång", systemImage: "list.number")
                    }
                    .accessibilityLabel("Öppna kom igång-guiden")
                } header: {
                    Text("Så kommer du igång")
                } footer: {
                    Text("Fem steg: mikrofonen, lägg till tangentbordet, Tillåt fullständig åtkomst, byt till Tap & Talk, och prova.")
                }

                Section("Tangentbordet") {
                    Text("Tap & Talk är ett tangentbord. Du dikterar svenska i varje app där du skriver — meddelanden, mejl, anteckningar, sökfält.")
                    Step(1, "Byt till Tap & Talk med jordgloben 🌐 nere till vänster.")
                    Step(2, "Tryck på Tala och säg din mening.")
                    Step(3, "Tryck på Klar. Texten står kvar i fältet, och du rättar den med tangenterna om du vill.")
                    Text("Tangentbordet skriver bara i det fält du står i. Ingenting av det du skriver loggas, sparas eller lämnar telefonen.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }

                Section("Genväg i Meddelanden") {
                    Text("Tap & Talk finns också som liten app inne i Meddelanden: tryck på plustecknet vid skrivfältet och välj Tap & Talk. Samma diktering, men bara i Meddelanden — tangentbordet är huvudvägen och fungerar överallt.")
                }

                Section("Om mikrofonen") {
                    Text("Mikrofonen är igång bara mellan Tala och Klar. Tap & Talk sparar ingen inspelning och ingen meddelandehistorik.")
                    Text("Tap & Talk väljer taligenkänning på enheten när den svenska igenkännaren stöder det. Annars kan Apples taltjänst använda nätet.")
                    Text("Hörs något men taltjänsten avslutar med fel behålls texten ändå. Det du sagt ska aldrig gå förlorat.")
                }

                Section("Version") {
                    LabeledContent("Tap & Talk", value: Self.version)
                }
            }
            .navigationTitle("Tap & Talk")
        }
        .sheet(isPresented: $showGuide) {
            Onboarding()
        }
        .onAppear {
            // Första starten går direkt till guiden, som på Android.
            guard !guideSeen else { return }
            guideSeen = true
            showGuide = true
        }
    }

    private static var version: String {
        let info = Bundle.main.infoDictionary
        let short = info?["CFBundleShortVersionString"] as? String ?? "0"
        let build = info?["CFBundleVersion"] as? String ?? "0"
        return "\(short) (\(build))"
    }
}

private struct Step: View {
    let number: Int
    let text: String

    init(_ number: Int, _ text: String) {
        self.number = number
        self.text = text
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text("\(number)")
                .font(.headline)
                .monospacedDigit()
                .foregroundStyle(.tint)
                .accessibilityHidden(true)
            Text(text)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Steg \(number). \(text)")
    }
}
