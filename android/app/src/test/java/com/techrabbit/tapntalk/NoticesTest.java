package com.techrabbit.tapntalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.speech.SpeechRecognizer;

import org.junit.Test;

/**
 * Beskeden om taltjänsten.
 *
 * Tre saker vaktas här, och alla tre har varit fel någon gång. Varje felkod
 * ska ge en mening; förr returnerade tabellen null vid de två vanligaste
 * felen och då stod tangentbordet helt tyst. Ingen mening får bära felkoden;
 * förr stod "Taltjänsten svarade med ett fel (7)" i rutan, vilket inte
 * hjälper någon som ska diktera. Och hördes något ska det levereras, aldrig
 * påstås att inget uppfattades.
 */
public class NoticesTest {

    /**
     * Alla felkoder SpeechRecognizer har, plus noll och ett okänt nummer.
     * En telefon kan svara med en kod som inte finns i den här listan, och
     * då ska default-grenen ta den.
     */
    private static final int[] CODES = {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_AUDIO,
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_CLIENT,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT,
            0,
            4711,
            -1,
    };

    private static final String[] AGAIN = {Notices.TALK, Notices.BUBBLE};

    // --------------------------------------------- varje kod ger ett besked

    @Test public void varje_felkod_ger_en_mening() {
        for (int code : CODES) {
            for (String again : AGAIN) {
                String said = Notices.failure(code, again);
                assertNotNull("kod " + code + " gav null", said);
                assertTrue("kod " + code + " gav en tom mening", said.length() > 0);
                assertTrue("kod " + code + " slutar inte som en mening",
                        said.endsWith(".") || said.endsWith("!"));
            }
        }
    }

    @Test public void varje_felkod_ger_en_mening_ocksa_efter_tystnad() {
        for (int code : CODES) {
            String said = Notices.afterFailure(code, false, Notices.TALK);
            assertNotNull("kod " + code + " gav null", said);
            assertTrue("kod " + code + " gav en tom mening", said.length() > 0);
        }
    }

    @Test public void varje_besked_sager_vad_man_gor_nu() {
        for (int code : CODES) {
            String said = Notices.failure(code, Notices.BUBBLE);
            // Antingen vägen vidare, eller en annan handling: hämta svenska,
            // tillåt mikrofonen, stänga andra appar.
            assertTrue("kod " + code + " säger inte vad användaren gör nu: " + said,
                    said.contains("Tryck") || said.contains("tryck")
                            || said.contains("Hämta") || said.contains("Tillåt")
                            || said.contains("Stäng"));
        }
    }

    // -------------------------------------------- ingen kod syns för användaren

    @Test public void inget_besked_innehaller_en_felkod() {
        for (int code : CODES) {
            for (String again : AGAIN) {
                nakedOfDigits(Notices.failure(code, again), code);
                nakedOfDigits(Notices.afterFailure(code, false, again), code);
                nakedOfDigits(Notices.afterFailure(code, true, again), code);
            }
        }
    }

    @Test public void inget_av_de_fasta_beskeden_innehaller_en_siffra() {
        nakedOfDigits(Notices.NO_SERVICE, 0);
        nakedOfDigits(Notices.OFFLINE, 0);
        nakedOfDigits(Notices.BUSY_ELSEWHERE, 0);
        nakedOfDigits(Notices.DONE_KEYBOARD, 0);
        nakedOfDigits(Notices.LISTENING, 0);
        nakedOfDigits(Notices.FINISHING, 0);
        nakedOfDigits(Notices.ABANDONED, 0);
        nakedOfDigits(Notices.afterSilence(true, Notices.TALK), 0);
        nakedOfDigits(Notices.afterSilence(false, Notices.TALK), 0);
    }

    /** En siffra i ett besked är nästan alltid en felkod som läckt ut. */
    private void nakedOfDigits(String said, int code) {
        assertFalse("beskedet bär felkoden " + code + ": " + said,
                said.contains(String.valueOf(code)));
        for (char sign : said.toCharArray()) {
            assertFalse("beskedet innehåller siffran " + sign + ": " + said,
                    sign >= '0' && sign <= '9');
        }
    }

    // --------------------------------------------- hörd text levereras alltid

    @Test public void hord_text_pastas_aldrig_vara_ohord() {
        for (int code : CODES) {
            String said = Notices.afterFailure(code, true, Notices.BUBBLE);
            assertTrue("kod " + code + " nämner inte att något hördes: " + said,
                    said.contains("hördes"));
            assertFalse("kod " + code + " påstår att inget hördes fast det gjorde det",
                    said.contains("Hörde inget"));
        }
    }

    @Test public void hord_text_pastas_aldrig_vara_ohord_vid_tystnad() {
        String said = Notices.afterSilence(true, Notices.TALK);
        assertTrue(said.contains("står kvar"));
        assertFalse(said.contains("Hörde inget"));
    }

    @Test public void ohord_text_pastas_aldrig_vara_hord() {
        String said = Notices.afterSilence(false, Notices.TALK);
        assertFalse("tyst omgång påstods ha hört något: " + said,
                said.contains("står kvar"));
        assertTrue(said.contains(Notices.TALK));
    }

    @Test public void hord_text_ger_samma_besked_for_varje_kod() {
        String first = Notices.afterFailure(CODES[0], true, Notices.TALK);
        for (int code : CODES) {
            assertEquals("beskedet när något hördes ska inte bero på felkoden",
                    first, Notices.afterFailure(code, true, Notices.TALK));
        }
    }

    // ----------------------------------------------------- de kända grenarna

    @Test public void natfelet_sager_att_svenska_gar_att_hamta() {
        for (int code : new int[]{SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT}) {
            assertTrue(Notices.failure(code, Notices.TALK).startsWith(Notices.OFFLINE));
        }
    }

    @Test public void tyst_omgang_sager_att_inget_hordes() {
        for (int code : new int[]{SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT}) {
            assertTrue(Notices.failure(code, Notices.TALK).startsWith("Hörde inget"));
        }
    }

    @Test public void bubblan_och_appen_far_var_sin_vag_vidare() {
        String onBubble = Notices.failure(SpeechRecognizer.ERROR_NO_MATCH, Notices.BUBBLE);
        String inApp = Notices.failure(SpeechRecognizer.ERROR_NO_MATCH, Notices.TALK);
        assertTrue(onBubble.contains(Notices.BUBBLE));
        assertTrue(inApp.contains(Notices.TALK));
        assertFalse(onBubble.equals(inApp));
    }
}
