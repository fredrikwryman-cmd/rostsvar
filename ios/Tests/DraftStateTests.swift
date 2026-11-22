import XCTest

final class DraftStateTests: XCTestCase {

    private func dictated(_ text: String = "Hej, jag hör av mig i morgon.") -> DraftState {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        state.ask()
        state.beginRecording()
        state.transcribe(text)
        state.finishRecording()
        state.rest()
        return state
    }

    // MARK: - Mikrofonen stängs efter diktering

    func testMikrofonenÄrStängdFrånBörjan() {
        XCTAssertFalse(DraftState().microphoneIsOpen)
    }

    func testMikrofonenÄrÖppenBaraUnderInspelning() {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        XCTAssertFalse(state.microphoneIsOpen)
        state.ask()
        XCTAssertFalse(state.microphoneIsOpen, "Mikrofonen får inte öppnas medan frågan ställs")
        state.beginRecording()
        XCTAssertTrue(state.microphoneIsOpen)
        state.finishRecording()
        XCTAssertFalse(state.microphoneIsOpen, "Klar ska stänga mikrofonen direkt")
        state.rest()
        XCTAssertFalse(state.microphoneIsOpen)
    }

    func testMikrofonenÄrStängdIVarjeLägeUtomInspelning() {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        state.ask()
        state.beginRecording()
        state.transcribe("Något")

        var avbruten = state
        avbruten.cancel()
        XCTAssertFalse(avbruten.microphoneIsOpen, "Avbryt ska stänga mikrofonen")

        var lämnad = state
        lämnad.resignActive()
        XCTAssertFalse(lämnad.microphoneIsOpen, "Att lämna Röstsvar ska stänga mikrofonen")

        var bytt = state
        bytt.becomeActive(conversation: "B")
        XCTAssertFalse(bytt.microphoneIsOpen, "Ett konversationsbyte ska stänga mikrofonen")
    }

    // MARK: - Avbryt utan att något skickas

    func testAvbrytUnderInspelningRensarUtkastetOchSkickarInget() {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        state.ask()
        state.beginRecording()
        state.transcribe("Text som inte ska skickas")
        XCTAssertTrue(state.canCancel)

        state.cancel()

        XCTAssertEqual(state.draft, "")
        XCTAssertFalse(state.hasDraft)
        XCTAssertEqual(state.phase, .idle)
        XCTAssertFalse(state.canSend, "Efter Avbryt ska Skicka inte gå att trycka på")
        XCTAssertFalse(state.microphoneIsOpen)
    }

    func testAvbrytEfterDikteringRensarFärdigtUtkast() {
        var state = dictated()
        XCTAssertTrue(state.canSend)

        state.cancel()

        XCTAssertEqual(state.draft, "")
        XCTAssertFalse(state.canSend)
    }

    func testEttFärdigtUtkastSkickasInteAvSigSjälvt() {
        let state = dictated()
        XCTAssertEqual(state.phase, .idle)
        XCTAssertTrue(state.canSend, "Skicka ska erbjudas, men bara som ett val")
        XCTAssertTrue(state.hasDraft, "Utkastet ligger kvar tills användaren väljer")
    }

    func testAvbrytGårAttTryckaPåIVarjeLägeDärNågotPågår() {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        XCTAssertFalse(state.canCancel, "Inget att avbryta när inget pågår och inget utkast finns")
        state.ask()
        XCTAssertTrue(state.canCancel)
        state.beginRecording()
        XCTAssertTrue(state.canCancel)
        state.transcribe("Något")
        state.finishRecording()
        XCTAssertTrue(state.canCancel)
        state.rest()
        XCTAssertTrue(state.canCancel, "Ett färdigt utkast ska gå att kasta")
    }

    // MARK: - Utkastet rensas vid byte av konversation

    func testBytePåKonversationRensarUtkastet() {
        var state = dictated("Hemlig text till A")
        XCTAssertTrue(state.hasDraft)

        state.becomeActive(conversation: "B")

        XCTAssertEqual(state.draft, "", "Utkastet får aldrig följa med till en annan konversation")
        XCTAssertEqual(state.conversation, "B")
        XCTAssertFalse(state.canSend)
    }

    func testSammaKonversationBehållerUtkastetVidÅterkomst() {
        var state = dictated("Kvar i samma chatt")
        state.becomeActive(conversation: "A")
        XCTAssertEqual(state.draft, "Kvar i samma chatt")
    }

    func testAttLämnaRöstsvarRensarUtkastet() {
        var state = dictated()
        state.resignActive()
        XCTAssertEqual(state.draft, "")
        XCTAssertEqual(state.phase, .idle)
    }

    func testUtkastetRensasVidBytePåKonversationÄvenUnderInspelning() {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        state.ask()
        state.beginRecording()
        state.transcribe("Halvfärdig mening")

        state.becomeActive(conversation: "B")

        XCTAssertEqual(state.draft, "")
        XCTAssertEqual(state.phase, .idle)
    }

    // MARK: - Skicka

    func testSkickaGårInteAttTryckaPåUtanText() {
        var state = DraftState()
        state.becomeActive(conversation: "A")
        XCTAssertFalse(state.canSend)
        state.ask()
        state.beginRecording()
        state.transcribe("   \n\t  ")
        state.finishRecording()
        state.rest()
        XCTAssertFalse(state.canSend, "Bara blanksteg är inget utkast")
    }

    func testSkickaGårInteAttTryckaPåMedanNågotPågår() {
        var state = dictated()
        state.beginSending()
        XCTAssertEqual(state.phase, .sending)
        XCTAssertFalse(state.canSend, "Ett andra tryck får inte ge ett andra meddelande")
    }

    func testLyckadSändningRensarUtkastet() {
        var state = dictated()
        state.beginSending()
        state.finishSending(failed: false)
        XCTAssertEqual(state.draft, "")
        XCTAssertEqual(state.phase, .idle)
        XCTAssertFalse(state.canSend)
    }

    func testMisslyckadSändningBehållerUtkastet() {
        var state = dictated("Text som inte kom fram")
        state.beginSending()
        state.finishSending(failed: true)
        XCTAssertEqual(state.draft, "Text som inte kom fram")
        XCTAssertTrue(state.canSend, "Utkastet ska gå att skicka om")
    }

    // MARK: - Tala om

    func testTalaOmErsätterUtkastet() {
        var state = dictated("Första försöket")
        state.ask()
        state.beginRecording()
        XCTAssertEqual(state.draft, "", "Ny diktering börjar från tomt")
        state.transcribe("Andra försöket")
        state.finishRecording()
        state.rest()
        XCTAssertEqual(state.draft, "Andra försöket")
    }
}
