import XCTest

/// Testerna för hur tangentbordet skriver i ett annat programs fält.
///
/// Den farliga delen är backsteget: tangentbordet raderar tecken för att byta
/// ut en tolkning mot nästa, och gör det bakåt från markören. Ligger inte vår
/// egen text där längre raderar det användarens. Reglerna nedan är gränsen
/// för när tangentbordet får radera alls.
final class ProxyWritingTests: XCTestCase {

    // MARK: - Vad vi äger

    /// Normalfallet: vår text ligger närmast före markören, och då får den
    /// bytas ut.
    func testOurOwnTextBeforeTheCaretIsOurs() {
        let owned = ProxyWriting.owned(mine: "Jag hör",
                                       before: "Hej. Jag hör",
                                       selected: nil)
        XCTAssertEqual(owned, "Jag hör")
    }

    /// Kärnan: användaren flyttar markören mitt i en diktering. Då ligger
    /// vår text inte före markören längre, och ingenting får raderas.
    func testMovingTheCaretMakesUsOwnNothing() {
        let owned = ProxyWriting.owned(mine: "Jag hör",
                                       before: "Hej. ",
                                       selected: nil)
        XCTAssertEqual(owned, "", "Flyttad markör: tangentbordet får inte radera användarens text")
    }

    /// Markerad text: ett backsteg raderar markeringen, inte våra tecken.
    func testASelectionMakesUsOwnNothing() {
        let owned = ProxyWriting.owned(mine: "Jag hör",
                                       before: "Hej. Jag hör",
                                       selected: "Hej")
        XCTAssertEqual(owned, "", "Markerad text: backsteget skulle äta markeringen")
    }

    /// Lämnar programmet ingen text ifrån sig får vår egen bokföring gälla.
    /// Annars skulle dikteringen dubblera sig i varje fält som håller sitt
    /// innehåll för sig själv.
    func testWithoutContextOurOwnBookkeepingStands() {
        let owned = ProxyWriting.owned(mine: "Jag hör", before: nil, selected: nil)
        XCTAssertEqual(owned, "Jag hör")
    }

    /// Har vi inte skrivit något äger vi inget, vad fältet än säger.
    func testNothingWrittenIsNothingOwned() {
        XCTAssertEqual(ProxyWriting.owned(mine: "", before: "Hej", selected: nil), "")
        XCTAssertEqual(ProxyWriting.owned(mine: "", before: nil, selected: "Hej"), "")
    }

    /// Ett tomt markeringsfält är ingen markering.
    func testAnEmptySelectionIsNoSelection() {
        let owned = ProxyWriting.owned(mine: "Jag", before: "Jag", selected: "")
        XCTAssertEqual(owned, "Jag")
    }

    // MARK: - Vägen till nästa tolkning

    /// En tolkning som bara växer raderar ingenting.
    func testAGrowingTranscriptionErasesNothing() {
        let step = ProxyWriting.plan(text: "Jag hör av mig", mine: "Jag hör")
        XCTAssertEqual(step.erase, 0)
        XCTAssertEqual(step.insert, " av mig")
    }

    /// Ändras slutet raderas bara slutet.
    func testOnlyTheChangedTailIsErased() {
        let step = ProxyWriting.plan(text: "Jag hör av mig", mine: "Jag hörde")
        XCTAssertEqual(step.erase, 2)
        XCTAssertEqual(step.insert, " av mig")
    }

    /// Ändras allt raderas allt vi skrivit, och bara det.
    func testAWhollyNewTranscriptionErasesOnlyOurs() {
        let step = ProxyWriting.plan(text: "Nej", mine: "Jag hör")
        XCTAssertEqual(step.erase, 7)
        XCTAssertEqual(step.insert, "Nej")
    }

    /// Äger vi ingenting raderas ingenting, och hela tolkningen läggs till.
    /// Texten kan då stå dubbelt — men användarens egen text är orörd, och
    /// det är det viktigare av de två.
    func testOwningNothingMeansInsertingWithoutErasing() {
        let step = ProxyWriting.plan(text: "Jag hör av mig", mine: "")
        XCTAssertEqual(step.erase, 0)
        XCTAssertEqual(step.insert, "Jag hör av mig")
    }

    /// Samma tolkning igen ska inte röra fältet alls.
    func testTheSameTranscriptionChangesNothing() {
        let step = ProxyWriting.plan(text: "Jag hör", mine: "Jag hör")
        XCTAssertEqual(step.erase, 0)
        XCTAssertEqual(step.insert, "")
    }

    /// Å, ä och ö är ett tecken var, inte två. Annars hade backsteget
    /// raderat halva bokstäver.
    func testSwedishLettersCountAsOneCharacter() {
        let step = ProxyWriting.plan(text: "Jag köper", mine: "Jag kör")
        XCTAssertEqual(step.erase, 1, "Bara r:et ska bort, inte ö:et")
        XCTAssertEqual(step.insert, "per")
    }
}
