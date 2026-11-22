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

    // MARK: - Regression: skickningen går inte att ångra eller göra om

    /// sendText har anropats. Då är det för sent att avbryta.
    func testAvbrytÄrInaktiveratMedanMeddelandetSkickas() {
        var state = dictated()
        XCTAssertTrue(state.canCancel)

        state.beginSending()

        XCTAssertFalse(state.canCancel, "Avbryt får inte gå att trycka på efter sendText")
        XCTAssertFalse(state.canTalk, "Tala får inte gå att trycka på medan det skickas")
        XCTAssertFalse(state.canSend)
    }

    func testAvbrytGörIngentingMedanMeddelandetSkickas() {
        var state = dictated("Redan överlämnad text")
        state.beginSending()

        state.cancel()

        XCTAssertEqual(state.phase, .sending, "Avbryt får inte lämna skickningen")
        XCTAssertEqual(state.draft, "Redan överlämnad text")
    }

    /// Panelen fälls ihop och öppnas igen medan skickningen pågår. Utkastet
    /// får inte bli skickbart en andra gång.
    func testIhopfälldOchÖppnadPanelUnderSändningGerInteEttAndraMeddelande() {
        var state = dictated("Skickas just nu")
        state.beginSending()

        state.rest()                              // panelen fälls ihop
        XCTAssertEqual(state.phase, .sending)
        XCTAssertFalse(state.canSend, "Ett ihopfällt läge får inte frigöra utkastet")

        state.becomeActive(conversation: "A")     // panelen öppnas igen
        XCTAssertEqual(state.phase, .sending)
        XCTAssertFalse(state.canSend, "Utkastet får inte gå att skicka om innan svaret kommit")
        XCTAssertFalse(state.canCancel)

        state.finishSending(failed: false)         // svaret kommer till slut
        XCTAssertEqual(state.phase, .idle)
        XCTAssertEqual(state.draft, "", "Ett lyckat skick lämnar inget kvar att skicka om")
        XCTAssertFalse(state.canSend)
    }

    func testAttLämnaOchKommaTillbakaUnderSändningGerInteEttAndraMeddelande() {
        var state = dictated("Skickas just nu")
        state.beginSending()

        state.resignActive()
        XCTAssertEqual(state.phase, .sending, "Skickningen äger läget tills svaret kommit")
        XCTAssertFalse(state.canSend)

        state.becomeActive(conversation: "A")
        XCTAssertFalse(state.canSend)
    }

    func testBytePåKonversationUnderSändningRensarUtkastet() {
        var state = dictated("Till A")
        state.beginSending()

        state.becomeActive(conversation: "B")

        XCTAssertEqual(state.draft, "", "Texten får inte ligga kvar i en annan konversation")
        XCTAssertFalse(state.canSend)
    }

    /// Bekräftat fel efter en ihopfälld panel: utkastet ska finnas kvar.
    func testBekräftatFelBehållerUtkastetÄvenEfterIhopfälldPanel() {
        var state = dictated("Kom aldrig fram")
        state.beginSending()
        state.rest()
        state.becomeActive(conversation: "A")

        state.finishSending(failed: true)

        XCTAssertEqual(state.phase, .idle)
        XCTAssertEqual(state.draft, "Kom aldrig fram")
        XCTAssertTrue(state.canSend, "Efter ett bekräftat fel ska det gå att skicka om")
        XCTAssertTrue(state.canCancel, "Och att kasta utkastet")
    }

    func testEttSvarSomKommerTvåGångerGerInteTvåMeddelanden() {
        var state = dictated()
        state.beginSending()
        state.finishSending(failed: false)
        XCTAssertEqual(state.phase, .idle)

        state.finishSending(failed: true)

        XCTAssertEqual(state.draft, "", "Ett sent svar får inte återuppliva utkastet")
        XCTAssertFalse(state.canSend)
    }

    func testSkickningenKanInteStartasTvåGånger() {
        var state = dictated()
        state.beginSending()
        let efterFörsta = state

        state.beginSending()

        XCTAssertEqual(state, efterFörsta, "Ett andra beginSending får inte ändra något")
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
