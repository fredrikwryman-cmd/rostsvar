package com.techrabbit.tapntalk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.speech.SpeechRecognizer;

import org.junit.Before;
import org.junit.Test;

/**
 * Valet av taltjänst och bytet i drift.
 *
 * Android gjorde förr alltid samma sak: den vanliga taltjänsten, som på de
 * flesta telefoner är Googles och kan skicka ljudet vidare. iOS hade sedan
 * början bett om tolkning på enheten när telefonen klarade det. Nu gör
 * Android likadant, och det som mäts här är de två reglerna som följer av
 * det: vilken tjänst som ska väljas, och när ett fel är värt att göra om
 * omgången på den andra.
 *
 * Själva skapandet av tjänsterna rör Android och går inte att köra här.
 * Reglerna gör det, och det är i dem felen bor.
 */
public class SpeechTest {

    private Speech picker;

    @Before public void fresh() {
        picker = new Speech();
    }

    // --------------------------------------------------------------- valet

    @Test public void enhetstolkning_valjs_nar_den_finns() {
        assertTrue(Speech.onDeviceFirst(33, true));
        assertTrue(Speech.onDeviceFirst(35, true));
    }

    @Test public void vanlig_tjanst_valjs_nar_enhetstolkning_saknas() {
        assertFalse("svaret nej ska inte ge enhetstolkning",
                Speech.onDeviceFirst(35, false));
    }

    @Test public void enhetstolkning_valjs_aldrig_fore_android_13() {
        // Appen går att installera från 30. Frågan finns inte där, och ett
        // anrop skulle kasta i stället för att svara.
        for (int sdk = 30; sdk < Speech.ON_DEVICE_FROM; sdk++) {
            assertFalse("sdk " + sdk + " har ingen enhetstolkning",
                    Speech.onDeviceFirst(sdk, true));
        }
    }

    /**
     * Ingen ska bli utan diktering. En telefon kan ha enhetstolkning utan
     * någon vanlig RecognitionService installerad, och då måste Tala-knappen
     * gå att trycka på ändå.
     */
    @Test public void nagon_tjanst_racker_for_att_fa_diktera() {
        assertTrue("vanlig tjänst finns", Speech.anyService(30, false, true));
        assertTrue("bara enhetstolkning", Speech.anyService(35, true, false));
        assertTrue("båda finns", Speech.anyService(35, true, true));
        assertFalse("ingen av dem finns", Speech.anyService(35, false, false));
        assertFalse("enhetstolkning på för gammal telefon",
                Speech.anyService(32, true, false));
    }

    // ----------------------------------------------------------- fallbacket

    @Test public void enhetstolkningens_sprakfel_ger_ett_byte() {
        for (int code : SWAP_WORTHY) {
            Speech one = new Speech();
            one.chose(true);
            assertTrue("kod " + code + " borde ge ett byte",
                    one.swapToCloud(code, false));
            assertFalse("efter bytet körs den vanliga tjänsten", one.onDevice());
        }
    }

    @Test public void vanliga_fel_ger_inget_byte() {
        // De här säger ingenting om att tjänsten är fel vald. Ett byte skulle
        // bara kosta användaren en omgång till.
        for (int code : new int[]{
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                SpeechRecognizer.ERROR_AUDIO,
                SpeechRecognizer.ERROR_SERVER,
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                SpeechRecognizer.ERROR_TOO_MANY_REQUESTS,
                0, 4711, -1}) {
            Speech one = new Speech();
            one.chose(true);
            assertFalse("kod " + code + " ska inte ge ett byte",
                    one.swapToCloud(code, false));
        }
    }

    @Test public void den_vanliga_tjansten_byter_inte_till_sig_sjalv() {
        picker.chose(false);
        assertFalse("ett byte från den vanliga tjänsten går ingenstans",
                picker.swapToCloud(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, false));
    }

    /** Kärnan i steg 2: bytet sker högst en gång per omgång. */
    @Test public void bytet_sker_hogst_en_gang() {
        picker.chose(true);
        assertTrue(picker.swapToCloud(SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT, false));
        assertTrue("bytet ska vara bokfört", picker.swapped());
        // Samma fel igen, och ett annat av dem som annars ger byte.
        assertFalse("ett andra byte i samma omgång",
                picker.swapToCloud(SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT, false));
        assertFalse("ett andra byte i samma omgång",
                picker.swapToCloud(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, false));
    }

    @Test public void nasta_omgang_far_byta_den_ocksa() {
        picker.chose(true);
        assertTrue(picker.swapToCloud(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, false));
        picker.roundOver();
        assertFalse("bokföringen ska vara nollad", picker.swapped());
        picker.chose(true);
        assertTrue("en ny omgång får sitt eget byte",
                picker.swapToCloud(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, false));
    }

    // ------------------------------------------------ hörd text väger tyngst

    /**
     * Regeln som inte får brytas: hörs något levereras det. Ett byte startar
     * en ny omgång och skulle kasta texten, så när något hörts säger valet
     * nej hur bytesvärt felet än är.
     */
    @Test public void hord_text_stoppar_bytet() {
        for (int code : SWAP_WORTHY) {
            Speech one = new Speech();
            one.chose(true);
            assertFalse("kod " + code + " kastade text som redan hörts",
                    one.swapToCloud(code, true));
            assertFalse("ett stoppat byte ska inte bokföras", one.swapped());
        }
    }

    @Test public void stoppat_byte_lamnar_tjansten_orord() {
        picker.chose(true);
        picker.swapToCloud(SpeechRecognizer.ERROR_CLIENT, true);
        assertTrue("tjänsten byttes fast texten skulle levereras",
                picker.onDevice());
    }

    /**
     * Och när texten är levererad och omgången stängd får nästa omgång byta
     * fritt. Annars hade en enda hörd mening låst bytet för resten av
     * processens liv.
     */
    @Test public void stoppat_byte_blockerar_inte_nasta_omgang() {
        picker.chose(true);
        assertFalse(picker.swapToCloud(SpeechRecognizer.ERROR_CLIENT, true));
        picker.roundOver();
        picker.chose(true);
        assertTrue(picker.swapToCloud(SpeechRecognizer.ERROR_CLIENT, false));
    }

    // ------------------------------------------------- spärren släpps alltid

    /**
     * Mikrofonspärren släpps av ytan, inte av Speech, och vilken väg som
     * gäller beror på svaret här. Säger valet nej går onError vidare till
     * close(), som släpper. Säger det ja håller ytan spärren över bytet och
     * släpper när den omgången stängs.
     *
     * Det som mäts här är att svaret alltid kommer, för varje kod en telefon
     * kan hitta på och i varje läge: ett kast härifrån hade lämnat spärren
     * tagen i sjuttio sekunder med en yta som sa att den lyssnade.
     */
    @Test public void svaret_kommer_alltid_utan_att_kasta() {
        for (int sdk : new int[]{30, 32, 33, 35}) {
            for (boolean device : new boolean[]{true, false}) {
                for (boolean anythingHeard : new boolean[]{true, false}) {
                    for (int code = -3; code <= 16; code++) {
                        Speech one = new Speech();
                        one.chose(device);
                        boolean swap = one.swapToCloud(code, anythingHeard);
                        // Ett ja betyder att ytan behåller spärren och gör om
                        // omgången, och det får bara ske från enhetstolkningen
                        // när inget hörts.
                        if (swap) {
                            assertTrue(device);
                            assertFalse(anythingHeard);
                            assertTrue(Speech.cloudWorthATry(code));
                        }
                        Speech.onDeviceFirst(sdk, device);
                        Speech.anyService(sdk, device, anythingHeard);
                    }
                }
            }
        }
    }

    /**
     * Bytet får bara ske när en tjänst är vald, alltså när omgången är öppen
     * och ytan håller spärren. En ny Speech har inte valt någonting.
     */
    @Test public void ett_oanvant_val_byter_ingenting() {
        assertFalse(picker.onDevice());
        assertFalse(picker.swapped());
        assertFalse("ett byte utan vald tjänst",
                picker.swapToCloud(SpeechRecognizer.ERROR_CLIENT, false));
    }

    /** Felen som betyder att enhetstolkningen inte duger för svenska. */
    private static final int[] SWAP_WORTHY = {
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT,
            SpeechRecognizer.ERROR_CLIENT,
    };
}
