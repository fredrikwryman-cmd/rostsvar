import XCTest

/// Testerna för vilket besked användaren får i vilket läge.
///
/// Punkt 3 och 14 ur granskningen handlade båda om det. Punkt 3: Androids
/// rösttangentbord svarade `null` vid de två vanligaste felen, så ett tryck
/// på Tala kunde sluta i fullständig tystnad. Punkt 14: de fem ytorna sa
/// olika saker om samma fel, och bubblan sa ett felnummer när nätet inte gick
/// att nå.
///
/// Texterna ligger i `Notice` just för att gå att mäta här, utan telefon.
/// Reglerna som mäts:
///
/// 1. Varje fel ger ett besked. Inget läge är tyst.
/// 2. Inget besked visar en felkod.
/// 3. Varje besked säger vad användaren gör nu.
/// 4. Hördes något levereras det, vad felet än var — och då får beskedet
///    aldrig påstå att inget uppfattades.
///
/// Androids `Notices` bär samma fyra regler i samma ordning. Repot har ingen
/// JUnit-uppsättning, så den sidan är inte mätt här.
final class NoticeTests: XCTestCase {

    /// Alla beskeden som kan hamna framför användaren.
    private let everything: [String] = [
        Notice.offline,
        Notice.unavailable,
        Notice.noMicrophone,
        Notice.micWontStart,
        Notice.noSpeech,
        Notice.unclear,
        Notice.noAnswer,
    ]

    // MARK: - Punkt 14: nätet

    /// Själva punkt 14. Förr sa iOS "Prova igen, eller hämta svenska för
    /// offline-diktering" utan att säga var, och Androids bubbla sa
    /// "ett fel (2)".
    func testNetworkFailureNamesTheProblemAndTheWayOut() {
        let dropped = NSError(domain: NSURLErrorDomain,
                              code: NSURLErrorNotConnectedToInternet)
        XCTAssertEqual(Notice.speechFailure(dropped), Notice.offline)

        let said = Notice.offline
        XCTAssertTrue(said.contains("nätet"), "Felet ska namnges. Fick: \(said)")
        XCTAssertTrue(said.contains("Inställningar"),
                      "Vägen till offline-svenska ska stå med. Fick: \(said)")
        XCTAssertTrue(said.contains("Tala"),
                      "Nästa steg ska stå med. Fick: \(said)")
    }

    func testTimeoutOnTheNetworkIsAlsoTreatedAsOffline() {
        let slow = NSError(domain: NSURLErrorDomain, code: NSURLErrorTimedOut)
        XCTAssertEqual(Notice.speechFailure(slow), Notice.offline)
    }

    // MARK: - Punkt 3: inget läge är tyst

    /// 1110 är "no speech detected" och är det vanligaste felet av alla.
    func testTheTwoCommonestCodesGiveTheSameSpokenAnswer() {
        for code in [1110, 203] {
            let quiet = NSError(domain: "kAFAssistantErrorDomain", code: code)
            XCTAssertEqual(Notice.speechFailure(quiet), Notice.noSpeech,
                           "Kod \(code) ska ge beskedet om att inget hördes.")
        }
    }

    /// Ett fel ingen känner igen får inte bli tystnad, och inte heller en kod.
    func testAnUnknownFailureStillGivesAMessage() {
        let odd = NSError(domain: "något.annat", code: 4711)
        let said = Notice.speechFailure(odd)
        XCTAssertEqual(said, Notice.unclear)
        XCTAssertFalse(said.isEmpty)
        XCTAssertFalse(said.contains("4711"), "Felkoden hör i loggen, inte i rutan.")
    }

    func testNothingHeardAlwaysCarriesAReason() {
        for said in everything {
            let how = Dictation.ending(heard: "", final: false, failed: said)
            XCTAssertEqual(how, .nothing(note: said))
        }
        // Och utan något angivet skäl finns ändå ett besked.
        let bare = Dictation.ending(heard: "", final: false, failed: nil)
        XCTAssertEqual(bare, .nothing(note: Notice.noSpeech))
    }

    // MARK: - Beskedens form

    func testNoMessageShowsAnErrorCode() {
        let digits = CharacterSet.decimalDigits
        for said in everything {
            XCTAssertNil(said.rangeOfCharacter(from: digits),
                         "Inget besked ska bära en siffra. Fick: \(said)")
        }
    }

    /// Ett fel som bara säger vad som hände lämnar användaren kvar där den
    /// står. Varje besked ska också säga vad man gör nu.
    func testEveryMessageSaysWhatToDoNext() {
        let steps = ["Tryck", "prova", "Lägg till", "Stäng"]
        for said in everything {
            XCTAssertTrue(steps.contains(where: said.contains),
                          "Inget nästa steg i: \(said)")
        }
    }

    func testEveryMessageIsAWholeSentence() {
        for said in everything {
            XCTAssertTrue(said.count > 20, "För kort att förklara något: \(said)")
            XCTAssertTrue(said.hasSuffix("."), "Saknar punkt: \(said)")
        }
    }

    // MARK: - Avslutsregeln, mot varje besked

    /// Regeln som felsökningspasset handlade om, mätt mot hela tabellen: vad
    /// felet än var, hördes något levereras det, och beskedet får aldrig
    /// påstå att inget uppfattades.
    func testHeardTextSurvivesEveryFailure() {
        for said in everything {
            let how = Dictation.ending(heard: "Jag kommer vid fyra", final: false,
                                       failed: said)
            switch how {
            case .salvaged(let text, let note):
                XCTAssertEqual(text, "Jag kommer vid fyra")
                XCTAssertEqual(note, Notice.salvaged)
                XCTAssertFalse(note.contains("Hörde inget"),
                               "Hördes något får det inte sägas att inget hördes.")
            default:
                XCTFail("Text som hörts får aldrig kastas. \(said) gav \(how).")
            }
        }
    }

    /// Samma regel när taltjänsten tystnar helt i stället för att fela.
    func testHeardTextSurvivesSilenceFromTheService() {
        let how = Dictation.ending(heard: "Tre ägg och smör", final: false,
                                   failed: Notice.noAnswer)
        XCTAssertEqual(how, .salvaged(text: "Tre ägg och smör", note: Notice.salvaged))
    }
}
