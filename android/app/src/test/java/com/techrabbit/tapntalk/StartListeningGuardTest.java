package com.techrabbit.tapntalk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Att en nekad start nollar läget på alla tre ytorna.
 *
 * Bubblan, rösttangentbordet och hemskärmen är Service, InputMethodService
 * och Activity. Ingen av dem går att starta på en vanlig JVM, och appen har
 * varken Robolectric eller instrumenterade tester. Flaggorna working och
 * accepting går alltså inte att läsa i ett enhetstest.
 *
 * Det som går att läsa är källan. Felet i buggjakten var att
 * startListening låg bar: nekade taltjänsten starten kastade den, flaggorna
 * stod kvar som sanna och mikrofonspärren låg tagen i sjuttio sekunder
 * medan ytan sa att den lyssnade. Testet vaktar att varje anrop ligger i en
 * try, och att grenen som fångar nollar läget och lämnar tillbaka spärren.
 *
 * Byggs appen om till ett riktigt testbart lager får det här testet gå.
 * Tills dess är det enda som fångar återfallet.
 */
public class StartListeningGuardTest {

    /**
     * Ytorna som startar taligenkänning: filen, det som nollar läget, det
     * som lämnar tillbaka mikrofonspärren i den nekade starten, och det som
     * lämnar tillbaka den i felgrenen. Hemskärmen gör allt genom
     * closeRound(), de andra två rad för rad.
     */
    private static final String[][] SURFACES = {
            {"Bubble.java", "working = false", "Mic.release(this)", "close()"},
            {"VoiceKeys.java", "accepting = false", "Mic.release(this)",
                    "Mic.release(VoiceKeys.this)"},
            {"Home.java", "closeRound()", "closeRound()", "closeRound()"},
    };

    @Test public void varje_start_ligger_i_en_try_som_nollar_laget() throws IOException {
        for (String[] surface : SURFACES) {
            String name = surface[0];
            String resets = surface[1];
            String releases = surface[2];
            String source = flat(read(name));

            int calls = count(source, "speech.startListening(");
            assertTrue(name + " startar ingen taligenkänning längre, så testet "
                    + "pekar på fel fil", calls > 0);

            // Ytorna har två startar nu: den vanliga, och omförsöket på den
            // vanliga taltjänsten när enhetstolkningen visat sig oduglig.
            // Båda ska ligga i en try, men de ser olika ut i källan, så det
            // som räknas är om det finns en try som fortfarande är öppen.
            for (int at = source.indexOf("speech.startListening("); at >= 0;
                    at = source.indexOf("speech.startListening(", at + 1)) {
                if (!insideOpenTry(source, at)) {
                    fail(name + " har ett anrop till startListening utanför en "
                            + "try. Nekar taltjänsten starten står ytan kvar som "
                            + "lyssnande med mikrofonen tagen.");
                }
                String after = source.substring(at, Math.min(source.length(), at + 500));
                assertTrue(name + ": en start utan catch för en nekad start",
                        after.contains("} catch (RuntimeException"));
            }

            int at = source.indexOf("try { speech.startListening(");
            assertTrue(name + ": den vanliga starten ligger inte i en egen try",
                    at > 0);
            String caught = source.substring(at, Math.min(source.length(), at + 700));
            assertTrue(name + ": grenen som fångar en nekad start saknar catch",
                    caught.contains("} catch (RuntimeException"));
            assertTrue(name + ": en nekad start lämnar inte tillbaka mikrofonspärren "
                            + "genom " + releases, caught.contains(releases));
            assertTrue(name + ": en nekad start nollar inte läget genom " + resets,
                    caught.contains(resets));
            assertTrue(name + ": en nekad start river inte taltjänsten",
                    caught.contains("speech = null"));
            assertTrue(name + ": en nekad start säger inget till användaren",
                    caught.contains("Notices."));
        }
    }

    /** Hemskärmens closeRound() är det som lämnar tillbaka spärren. */
    @Test public void hemskarmens_closeRound_lamnar_tillbaka_sparren() throws IOException {
        String source = flat(read("Home.java"));
        int at = source.indexOf("private void closeRound() {");
        assertTrue("hemskärmen har ingen closeRound() längre", at > 0);
        String body = source.substring(at, Math.min(source.length(), at + 300));
        assertTrue("closeRound() lämnar inte tillbaka mikrofonspärren",
                body.contains("Mic.release(this)"));
        assertTrue("closeRound() nollar inte working", body.contains("working = false"));
        assertTrue("closeRound() nollar inte listening", body.contains("listening = false"));
    }

    /**
     * Hemskärmen tar spärren innan den frågar om taltjänsten finns. Svaret
     * nej var vägen som lämnade mikrofonen låst utan att någon lyssnade.
     */
    @Test public void hemskarmen_lamnar_tillbaka_sparren_nar_taltjansten_saknas()
            throws IOException {
        String source = flat(read("Home.java"));
        // Frågan ställs på flera ställen i filen, bland annat i onCreate och
        // onResume. Det är den inne i startListening som gäller, alltså den
        // som ligger efter att spärren tagits.
        int at = source.indexOf("private void startListening() {");
        assertTrue("hemskärmen har ingen startListening() längre", at > 0);
        String method = source.substring(at, Math.min(source.length(), at + 1400));
        int asks = method.indexOf("if (!Speech.available(this))");
        assertTrue("startListening() frågar inte längre om taltjänsten finns", asks > 0);
        assertTrue("spärren tas inte i startListening()", method.contains("Mic.take(this)"));
        String branch = method.substring(asks, Math.min(method.length(), asks + 500));
        assertTrue("grenen där taltjänsten saknas lämnar inte tillbaka spärren",
                branch.contains("Mic.release(this)"));
    }

    /**
     * Varje yta skapar sin taltjänst genom Speech, och ingen annanstans.
     *
     * Valet mellan enhetstolkning och den vanliga taltjänsten ska bo på ett
     * ställe. Står ett createSpeechRecognizer kvar i en yta kör just den ytan
     * molnet oavsett vad telefonen klarar.
     */
    @Test public void ingen_yta_skapar_taltjansten_sjalv() throws IOException {
        for (String[] surface : SURFACES) {
            String name = surface[0];
            String source = flat(read(name));
            assertTrue(name + " skapar ingen taltjänst genom Speech",
                    source.contains("Speech.open(this, picker)"));
            assertFalse(name + " skapar taltjänsten själv i stället för genom "
                            + "Speech, och kör då molnet vad telefonen än klarar",
                    source.contains("SpeechRecognizer.createSpeechRecognizer"));
            assertFalse(name + " frågar om taltjänsten utan att räkna med "
                            + "enhetstolkningen",
                    source.contains("SpeechRecognizer.isRecognitionAvailable"));
        }
    }

    /**
     * Felgrenen frågar om bytet, och gör det innan omgången stängs.
     *
     * Stängdes omgången först vore spärren redan släppt när omförsöket
     * startade, och en annan yta kunde ta mikrofonen mellan de två försöken.
     */
    @Test public void felgrenen_fragar_om_bytet_fore_den_stanger() throws IOException {
        for (String[] surface : SURFACES) {
            String name = surface[0];
            String source = flat(read(name));
            int at = source.indexOf("public void onError(int code) {");
            assertTrue(name + " har ingen onError längre", at > 0);
            String body = source.substring(at, Math.min(source.length(), at + 900));
            int asks = body.indexOf("picker.swapToCloud(");
            assertTrue(name + ": felgrenen frågar inte om bytet till den "
                    + "vanliga taltjänsten", asks > 0);
            assertTrue(name + ": bytet görs inte av om det inte går",
                    body.contains("&& retry()) return;"));
            int releases = body.indexOf(surface[3]);
            assertTrue(name + ": felgrenen släpper inte spärren", releases > 0);
            assertTrue(name + ": omgången stängs innan bytet hunnit frågas, "
                    + "och då är spärren fri mitt i dikteringen", asks < releases);
        }
    }

    /**
     * Omförsöket håller spärren. Släpptes den där kunde bubblan eller
     * tangentbordet ta mikrofonen mellan de två försöken, och användaren
     * skulle få veta att mikrofonen används någon annanstans mitt i sin egen
     * diktering.
     */
    @Test public void omforsoket_slapper_aldrig_sparren() throws IOException {
        for (String[] surface : SURFACES) {
            String name = surface[0];
            String source = flat(read(name));
            int at = source.indexOf("private boolean retry() {");
            assertTrue(name + " har inget omförsök", at > 0);
            String body = source.substring(at, Math.min(source.length(), at + 900));
            int ends = body.indexOf("private void drop()");
            if (ends > 0) body = body.substring(0, ends);
            assertFalse(name + ": omförsöket släpper spärren",
                    body.contains("Mic.release"));
            assertFalse(name + ": omförsöket stänger omgången",
                    body.contains("closeRound()"));
            assertTrue(name + ": omförsöket tar inte den vanliga taltjänsten",
                    body.contains("Speech.cloud(this, picker)"));
            assertTrue(name + ": omförsöket startar ingen ny lyssning",
                    body.contains("speech.startListening("));
        }
    }

    // ------------------------------------------------------------------ hjälp

    /**
     * Sant när tecknet på plats {@code at} ligger inne i en try som ännu
     * inte stängts. Enklaste måttet som tål att try-blocket innehåller mer
     * än själva anropet.
     */
    private static boolean insideOpenTry(String source, int at) {
        String before = source.substring(0, at);
        int opened = before.lastIndexOf("try {");
        if (opened < 0) return false;
        return before.indexOf("}", opened) < 0;
    }

    /** Allt mellanrum blir ett blanksteg, så indraget inte spelar någon roll. */
    private static String flat(String source) {
        return source.replaceAll("\\s+", " ");
    }

    private static int count(String source, String part) {
        int found = 0;
        int at = source.indexOf(part);
        while (at >= 0) {
            found++;
            at = source.indexOf(part, at + 1);
        }
        return found;
    }

    /**
     * Källfilen, letad uppåt från den mapp testet körs i. Gradle startar
     * enhetstesterna i modulmappen, men den som kör dem för hand kan stå
     * någon annanstans.
     */
    private static String read(String name) throws IOException {
        String inside = "src/main/java/com/techrabbit/tapntalk/" + name;
        Path at = Paths.get("").toAbsolutePath();
        for (int up = 0; up < 6 && at != null; up++, at = at.getParent()) {
            Path file = at.resolve(inside);
            if (Files.isRegularFile(file)) {
                return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            }
        }
        throw new IOException("hittar inte " + inside + " från "
                + Paths.get("").toAbsolutePath());
    }
}
