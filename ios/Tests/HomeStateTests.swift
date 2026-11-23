import XCTest

/// Testerna för dikteringsskärmens egen logik. De rör varken mikrofonen
/// eller taltjänsten: de mäter reglerna, så att de går att visa utan telefon.
///
/// Den bärande regeln är densamma som i `Dictation.ending`: det som en gång
/// hörts kastas aldrig.
final class HomeStateTests: XCTestCase {

    /// Ett vanligt förlopp: fråga, mikrofon på, en tolkning, klar, vila.
    private func dictated(_ texts: [String], into start: String = "") -> HomeState {
        var state = HomeState()
        state.draft = start
        state.ask()
        state.beginRecording()
        for text in texts { state.transcribe(text) }
        state.finishRecording()
        state.rest()
        return state
    }

    // MARK: - Tolkningarna

    /// Delresultaten ersätter varandra. De får aldrig staplas på varandra.
    func testPartialResultsReplaceEachOther() {
        let state = dictated(["Jag", "Jag hör", "Jag hör av mig"])
        XCTAssertEqual(state.draft, "Jag hör av mig")
    }

    /// Det som dikteras läggs efter texten som redan stod i fältet, med ett
    /// mellanslag emellan. Samma regel som Androids `base`.
    func testDictationIsAppendedAfterExistingText() {
        let state = dictated(["och jag tar med kaffe"], into: "Jag hör av mig i morgon.")
        XCTAssertEqual(state.draft, "Jag hör av mig i morgon. och jag tar med kaffe")
    }

    /// Kärnan i buggen som rättades i dikteringen, mätt en gång till här:
    /// en tom tolkning får inte radera det som redan hörts.
    func testBlankTranscriptionNeverWipesTheText() {
        var state = HomeState()
        state.ask()
        state.beginRecording()
        state.transcribe("Tre ägg och smör")
        state.transcribe("")
        state.transcribe("   \n ")
        state.finishRecording()
        state.rest()
        XCTAssertEqual(state.draft, "Tre ägg och smör")
        XCTAssertTrue(state.hasText)
    }

    /// Texten står kvar när dikteringen går till vila, vad som än hände på
    /// vägen — också när taltjänsten avslutade utan slutresultat.
    func testTextSurvivesAnEndingWithoutAFinalResult() {
        var state = HomeState()
        state.ask()
        state.beginRecording()
        state.transcribe("Kommer strax")
        state.rest()
        XCTAssertEqual(state.draft, "Kommer strax")
        XCTAssertEqual(state.phase, .idle)
    }

    /// En text användaren rättat för hand är den som nästa diktering läggs
    /// efter, inte den som taltjänsten senast levererade.
    func testTextEditedByHandBecomesTheGroundForTheNextDictation() {
        var state = dictated(["Hej då"])
        state.draft = "Hej!"
        let again = dictated(["Vi ses i morgon"], into: state.draft)
        XCTAssertEqual(again.draft, "Hej! Vi ses i morgon")
    }

    /// Två omgångar i rad bygger på varandra, utan dubbla mellanslag.
    func testTwoRoundsInARowAppend() {
        let first = dictated(["Jag hör av mig"])
        let second = dictated(["i morgon"], into: first.draft)
        XCTAssertEqual(second.draft, "Jag hör av mig i morgon")
    }

    /// Skillnaden mellan "det står text i fältet" och "något hördes just
    /// nu". Stod texten kvar sedan förra gången får skärmen inte påstå att
    /// den här omgången lyckades.
    func testTextLeftFromBeforeIsNotSomethingHeardNow() {
        var state = HomeState()
        state.draft = "Jag hör av mig i morgon."
        state.ask()
        state.beginRecording()
        state.finishRecording()
        state.rest()
        XCTAssertTrue(state.hasText)
        XCTAssertFalse(state.heardSomething, "Ingen tolkning kom, alltså hördes ingenting")
    }

    func testSomethingHeardIsRememberedForTheRound() {
        var state = HomeState()
        state.ask()
        state.beginRecording()
        state.transcribe("Jag hör av mig")
        XCTAssertTrue(state.heardSomething)
        state.finishRecording()
        state.rest()
        XCTAssertTrue(state.heardSomething)
        // Nästa omgång börjar om från noll.
        state.ask()
        state.beginRecording()
        XCTAssertFalse(state.heardSomething)
    }

    func testBlankTranscriptionIsNotSomethingHeard() {
        var state = HomeState()
        state.ask()
        state.beginRecording()
        state.transcribe("   ")
        XCTAssertFalse(state.heardSomething)
    }

    // MARK: - Mikrofonen

    func testMicrophoneIsClosedFromTheStart() {
        XCTAssertFalse(HomeState().microphoneIsOpen)
    }

    /// Mikrofonen är öppen bara under inspelning: inte medan frågan ställs,
    /// och inte medan taltjänsten slutför texten.
    func testMicrophoneIsOpenOnlyWhileRecording() {
        var state = HomeState()
        state.ask()
        XCTAssertFalse(state.microphoneIsOpen, "Mikrofonen får inte öppnas medan frågan ställs")
        state.beginRecording()
        XCTAssertTrue(state.microphoneIsOpen)
        state.finishRecording()
        XCTAssertFalse(state.microphoneIsOpen, "Mikrofonen ska vara stängd medan texten slutförs")
        state.rest()
        XCTAssertFalse(state.microphoneIsOpen)
    }

    /// Tala-knappen går att trycka på både för att starta och för att
    /// avsluta, men inte medan frågan eller slutförandet pågår.
    func testTalkEndsAsWellAsStarts() {
        var state = HomeState()
        XCTAssertTrue(state.canTalk)
        state.ask()
        XCTAssertFalse(state.canTalk)
        state.beginRecording()
        XCTAssertTrue(state.canTalk)
        state.finishRecording()
        XCTAssertFalse(state.canTalk)
    }

    // MARK: - Knapparna

    /// Skicka och Rensa väntar tills mikrofonen är stängd. Medan den är
    /// öppen är Klar nästa steg.
    func testSendAndClearWaitForTheMicrophone() {
        var state = HomeState()
        state.draft = "Jag hör av mig"
        XCTAssertTrue(state.canSend)
        XCTAssertTrue(state.canClear)
        state.ask()
        state.beginRecording()
        XCTAssertFalse(state.canSend)
        XCTAssertFalse(state.canClear)
        XCTAssertFalse(state.canEdit, "Fältet ska inte gå att skriva i medan mikrofonen är på")
    }

    /// Ett fält med bara blanktecken är inget att skicka.
    func testWhitespaceIsNothingToSend() {
        var state = HomeState()
        state.draft = "   \n  "
        XCTAssertFalse(state.hasText)
        XCTAssertFalse(state.canSend)
        XCTAssertEqual(state.trimmed, "")
    }

    func testClearEmptiesEverything() {
        var state = dictated(["Jag hör av mig"])
        state.clear()
        XCTAssertEqual(state.draft, "")
        XCTAssertFalse(state.hasText)
        XCTAssertEqual(state.phase, .idle)
    }

    /// Rensa får inte lämna kvar en grund som nästa diktering byggs på.
    func testClearAlsoForgetsTheGround() {
        var state = dictated(["Jag hör av mig"])
        state.clear()
        state.ask()
        state.beginRecording()
        state.transcribe("Nytt meddelande")
        XCTAssertEqual(state.draft, "Nytt meddelande")
    }
}
