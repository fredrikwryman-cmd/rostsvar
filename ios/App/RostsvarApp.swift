import SwiftUI

@main
struct RostsvarApp: App {
    var body: some Scene {
        WindowGroup {
            NavigationStack {
                List {
                    Section("Så gör du") {
                        Step(1, "Öppna en konversation i Meddelanden.")
                        Step(2, "Tryck på plustecknet vid skrivfältet och välj Röstsvar bland iMessage-apparna.")
                        Step(3, "Tryck på Tala och diktera på svenska.")
                        Step(4, "Tryck på Klar, läs igenom utkastet och tryck på Skicka.")
                    }
                    Section("Avbryta") {
                        Text("Avbryt stänger mikrofonen och rensar utkastet. Inget skickas förrän du trycker på Skicka.")
                        Text("Utkastet rensas också när du lämnar Röstsvar eller byter konversation.")
                    }
                    Section("Om mikrofonen") {
                        Text("Mikrofonen är igång bara mellan Tala och Klar. Röstsvar sparar ingen inspelning och ingen meddelandehistorik.")
                        Text("Röstsvar väljer taligenkänning på enheten när den svenska igenkännaren stöder det. Annars kan Apples taltjänst använda nätet.")
                    }
                    Section("Version") {
                        LabeledContent("Röstsvar", value: Self.version)
                    }
                }
                .navigationTitle("Röstsvar")
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
