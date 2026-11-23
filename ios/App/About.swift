import SwiftUI

/// Om appen: var tangentbordet kommer in, vad genvägen i Meddelanden är,
/// och vad som händer med mikrofonen. Nås från Om-knappen på startskärmen.
struct About: View {

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section("Tangentbordet") {
                    Text("Tap & Talk är också ett tangentbord. Då dikterar du i varje app där du skriver — meddelanden, mejl, anteckningar, sökfält — utan att gå via den här skärmen.")
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

                Section("Så skickas texten") {
                    Text("Appen skickar aldrig något själv. Skicka lämnar texten till iOS egna delningsruta, där du väljer Messenger, Signal, SMS eller något annat.")
                }

                Section("Version") {
                    LabeledContent("Tap & Talk", value: Self.version)
                }
            }
            .navigationTitle("Om Tap & Talk")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Klar") { dismiss() }
                }
            }
        }
    }

    private static var version: String {
        let info = Bundle.main.infoDictionary
        let short = info?["CFBundleShortVersionString"] as? String ?? "0"
        let build = info?["CFBundleVersion"] as? String ?? "0"
        return "\(short) (\(build))"
    }
}

/// Ett numrerat steg, läst som en enda rad av VoiceOver.
struct Step: View {
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
