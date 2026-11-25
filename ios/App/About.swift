import SwiftUI

/// Om appen: var tangentbordet kommer in, vad genvägen i Meddelanden är,
/// och vad som händer med mikrofonen. Nås från Om-knappen på startskärmen.
///
/// Utseendet är Tech Rabbit-designen, samma kort, färger och typsnitt som
/// startskärmen. Texterna är oförändrade.
struct About: View {

    @Environment(\.dismiss) private var dismiss

    /// Av-knappen för haptiken. Nyckeln är `Haptics.key`, samma namn som på
    /// Android. `@AppStorage` lagrar av och inte på, så att standardläget
    /// blir påslaget utan att något behöver skrivas vid första starten.
    @AppStorage(Haptics.key) private var buzzOff = false

    var body: some View {
        TRSheet("Om Tap&Talk", close: { dismiss() }) {
            TRCard {
                Text("Tangentbordet").trHeading()
                Text("Tap&Talk är också ett tangentbord. Då dikterar du i varje app där du skriver — meddelanden, mejl, anteckningar, sökfält — utan att gå via den här skärmen.")
                    .trBody()
                Step(1, "Byt till Tap&Talk med jordgloben 🌐 nere till vänster.")
                Step(2, "Tryck på Tala och säg din mening.")
                Step(3, "Tryck på Klar. Texten står kvar i fältet, och du rättar den med tangenterna om du vill.")
                Text("Tangentbordet skriver bara i det fält du står i. Ingenting av det du skriver loggas, sparas eller lämnar telefonen.")
                    .trFootnote()
            }

            TRCard {
                Text("Genväg i Meddelanden").trHeading()
                Text("Tap&Talk finns också som liten app inne i Meddelanden: tryck på plustecknet vid skrivfältet och välj Tap&Talk. Samma diktering, men bara i Meddelanden — tangentbordet är huvudvägen och fungerar överallt.")
                    .trBody()
            }

            TRCard {
                Text("Om mikrofonen").trHeading()
                Text("Mikrofonen är igång bara mellan Tala och Klar. Tap&Talk sparar ingen inspelning och ingen meddelandehistorik.")
                    .trBody()
                Text("Tap&Talk väljer taligenkänning på enheten när den svenska igenkännaren stöder det. Annars kan Apples taltjänst använda nätet.")
                    .trBody()
                Text("Hörs något men taltjänsten avslutar med fel behålls texten ändå. Det du sagt ska aldrig gå förlorat.")
                    .trBody()
            }

            TRCard {
                Text("Så skickas texten").trHeading()
                Text("Appen skickar aldrig något själv. Skicka lämnar texten till iOS egna delningsruta, där du väljer Messenger, Signal, SMS eller något annat.")
                    .trBody()
            }

            TRCard {
                Text("Vibration").trHeading()
                Toggle(isOn: Binding(get: { !buzzOff },
                                     set: { on in
                                        buzzOff = !on
                                        // Bekräftelsen på att haptiken är på
                                        // är själva vibrationen. Slås den av
                                        // finns inget att känna.
                                        if on { Haptics.fire(.press) }
                                     })) {
                    Text("Vibrera vid tryck och besked")
                        .trFont(TypeScale.bodyLine)
                        .foregroundStyle(TR.text)
                }
                .tint(TR.green)
                Text("Appen vibrerar lätt när mikrofonen öppnas och stängs, och när något går fel. Tangentbordets tangenter följer telefonens egen inställning för tangentljud.")
                    .trFootnote()
            }

            TRCard {
                Text("Version").trHeading()
                HStack(spacing: Metric.gapM) {
                    Text("Tap&Talk")
                        .trFont(TypeScale.bodyLine)
                        .foregroundStyle(TR.muted)
                    Spacer(minLength: Metric.gapS)
                    Text(Self.version)
                        .trFont(TypeScale.bodyStrongLine)
                        .foregroundStyle(TR.text)
                        .monospacedDigit()
                }
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Tap&Talk, version \(Self.version)")
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

/// Ett numrerat steg, läst som en enda rad av VoiceOver. Siffran är grön,
/// som det aktiva är i Tech Rabbit-designen.
struct Step: View {
    let number: Int
    let text: String

    init(_ number: Int, _ text: String) {
        self.number = number
        self.text = text
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: Metric.gapM) {
            Text("\(number)")
                .trFont(TypeScale.stepNumber)
                .monospacedDigit()
                .foregroundStyle(TR.green)
                .accessibilityHidden(true)
            Text(text)
                .trFont(TypeScale.body)
                .foregroundStyle(TR.text)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Steg \(number). \(text)")
    }
}
