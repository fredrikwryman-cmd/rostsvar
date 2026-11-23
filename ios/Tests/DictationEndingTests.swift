import XCTest

/// Testerna för buggen: texten dök upp medan användaren talade, och efteråt
/// stod det att inget tal uppfattades.
///
/// Regeln de mäter är enkel: har något hörts behålls det, vad som än händer
/// sedan. Bara när ingenting hörts får användaren höra att inget uppfattades.
final class DictationEndingTests: XCTestCase {

    func testFinalResultKeepsTheText() {
        let how = Dictation.ending(heard: "Jag är på väg", final: true, failed: nil)
        XCTAssertEqual(how, .done(text: "Jag är på väg"))
    }

    /// Kärnan i buggen. Taltjänsten avslutar med fel efter att ha hört rätt.
    func testErrorAfterSomethingWasHeardKeepsTheText() {
        let how = Dictation.ending(heard: "Jag hör av mig i morgon", final: false,
                                   failed: "Hörde inget. Tryck på Tala och prova igen.")
        switch how {
        case .salvaged(let text, let note):
            XCTAssertEqual(text, "Jag hör av mig i morgon")
            XCTAssertFalse(note.contains("Inget tal uppfattades"))
        default:
            XCTFail("Text som hörts får aldrig kastas. Fick \(how).")
        }
    }

    /// Svarar taltjänsten inte alls ska det som hörts ändå levereras.
    func testSilenceFromTheServiceKeepsTheText() {
        let how = Dictation.ending(heard: "Tre ägg och smör", final: false,
                                   failed: Notice.noAnswer)
        XCTAssertEqual(how, .salvaged(text: "Tre ägg och smör",
                                      note: Notice.salvaged))
    }

    func testNothingHeardReportsTheReason() {
        let how = Dictation.ending(heard: "", final: false,
                                   failed: Notice.noSpeech)
        XCTAssertEqual(how, .nothing(note: Notice.noSpeech))
    }

    /// Ett slutresultat utan text är inte ett resultat.
    func testBlankFinalResultReportsNothingHeard() {
        let how = Dictation.ending(heard: "   \n ", final: true, failed: nil)
        XCTAssertEqual(how, .nothing(note: Notice.noSpeech))
    }

    /// Avslutas dikteringen utan både fel och slutresultat är texten ändå klar.
    func testNoErrorAndNoFinalStillDelivers() {
        let how = Dictation.ending(heard: "Kommer strax", final: false, failed: nil)
        XCTAssertEqual(how, .done(text: "Kommer strax"))
    }
}
