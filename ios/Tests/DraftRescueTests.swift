import XCTest

/// Testerna för urklippsräddningen i iMessage-tillägget.
///
/// Utkastet nollas när panelen fälls ihop — ingen text ska ligga kvar i
/// någons meddelandepanel. Men det som hörts får inte kastas för det. Samma
/// regel som bubblan på Android: kan texten inte stå kvar där den hör hemma,
/// hamnar den i urklipp i stället.
final class DraftRescueTests: XCTestCase {

    /// Ett vanligt förlopp: aktiv i en konversation, en diktering, klar.
    private func dictated(_ text: String) -> DraftState {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        state.ask()
        state.beginRecording()
        state.transcribe(text)
        state.finishRecording()
        state.rest()
        return state
    }

    // MARK: - Det som hörts räddas

    func testHeardTextIsRescuedWhenThePanelCloses() {
        var state = dictated("Jag hör av mig i morgon")
        XCTAssertEqual(state.resignActive(), "Jag hör av mig i morgon")
    }

    /// Räddningen nollar utkastet. Texten ska inte ligga kvar i panelen.
    func testTheRescueStillClearsTheDraft() {
        var state = dictated("Jag hör av mig i morgon")
        _ = state.resignActive()
        XCTAssertEqual(state.draft, "")
        XCTAssertFalse(state.hasDraft)
        XCTAssertFalse(state.canSend)
    }

    /// Texten som räddas är putsad, så att inga blanktecken följer med in i
    /// urklippet.
    func testTheRescuedTextIsTrimmed() {
        var state = dictated("  Jag hör av mig  ")
        XCTAssertEqual(state.resignActive(), "Jag hör av mig")
    }

    /// Hörs något mitt i en inspelning räddas det också — ett ihopfällt
    /// fönster är just det avbrott regeln finns för.
    func testTextHeardDuringRecordingIsRescued() {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        state.ask()
        state.beginRecording()
        state.transcribe("Tre ägg och smör")
        XCTAssertEqual(state.resignActive(), "Tre ägg och smör")
    }

    // MARK: - När ingenting ska räddas

    func testAnEmptyDraftRescuesNothing() {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        XCTAssertNil(state.resignActive(), "Tomt utkast ska inte lägga något i urklipp")
    }

    /// Bara blanktecken är ingen text.
    func testWhitespaceOnlyRescuesNothing() {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        state.ask()
        state.beginRecording()
        state.transcribe("   \n ")
        XCTAssertNil(state.resignActive())
    }

    /// Under en pågående skickning är texten redan överlämnad till
    /// Meddelanden. Då vore urklippet bara en kopia av det som är på väg.
    func testNothingIsRescuedWhileSending() {
        var state = dictated("Jag hör av mig")
        state.beginSending()
        XCTAssertNil(state.resignActive())
        XCTAssertEqual(state.phase, .sending, "Skickningen får inte avbrytas av att panelen fälls ihop")
    }

    /// Räddningen sker en gång. Andra gången finns ingen text kvar att rädda.
    func testTheRescueHappensOnlyOnce() {
        var state = dictated("Jag hör av mig")
        XCTAssertNotNil(state.resignActive())
        XCTAssertNil(state.resignActive())
    }

    /// Avbryt är användarens eget beslut att kasta texten. Den räddas inte,
    /// och ska inte hamna i urklipp bakom ryggen på den som ångrade sig.
    func testCancelThrowsTheTextAwayOnPurpose() {
        var state = dictated("Jag hör av mig")
        state.cancel()
        XCTAssertNil(state.textToRescue)
        XCTAssertNil(state.resignActive())
    }

    // MARK: - Att läsa är inte att ändra

    func testReadingTheTextToRescueChangesNothing() {
        let state = dictated("Jag hör av mig")
        XCTAssertEqual(state.textToRescue, "Jag hör av mig")
        XCTAssertEqual(state.draft, "Jag hör av mig")
        XCTAssertTrue(state.hasDraft)
    }

    func testTextToRescueIsNilWithoutADraft() {
        XCTAssertNil(DraftState().textToRescue)
    }
}
