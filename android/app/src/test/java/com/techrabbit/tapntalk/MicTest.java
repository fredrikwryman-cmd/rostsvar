package com.techrabbit.tapntalk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

/**
 * Spärren som bestämmer vilken av appens tre ytor som har mikrofonen.
 *
 * Felet den här klassen vaktar kom fram i buggjakten: hemskärmen tog
 * spärren, frågade sedan om taltjänsten alls fanns, och lämnade den aldrig
 * tillbaka när svaret var nej. Mikrofonen var då låst för bubblan och
 * rösttangentbordet i sjuttio sekunder utan att någon lyssnade.
 *
 * Mic har ingen klocka som går att skruva på, så tiden flyttas framåt genom
 * att fältet since skrivs bakåt med reflektion. Det är privat men ligger i
 * samma paket som testet, och alternativet vore att lägga en bakdörr i
 * koden som används på telefonen.
 */
public class MicTest {

    /** Står för bubblan, tangentbordet och hemskärmen i testet. */
    private final Object bubble = new Object();
    private final Object keyboard = new Object();
    private final Object home = new Object();

    @Before public void clear() throws Exception {
        set("holder", null);
        set("since", 0L);
    }

    // ------------------------------------------------------------ grundfallet

    @Test public void den_forsta_far_mikrofonen() {
        assertTrue(Mic.take(bubble));
        assertFalse("tangentbordet ska inte få den av bubblan", Mic.take(keyboard));
        assertTrue(Mic.heldByOther(keyboard));
        assertFalse(Mic.heldByOther(bubble));
    }

    @Test public void den_som_lamnar_tillbaka_slapper_den_till_nasta() {
        assertTrue(Mic.take(bubble));
        Mic.release(bubble);
        assertTrue(Mic.take(keyboard));
    }

    @Test public void ett_andra_tag_av_samma_yta_gar_igenom() {
        assertTrue(Mic.take(bubble));
        assertTrue("samma yta ska kunna ta om sin egen spärr", Mic.take(bubble));
    }

    @Test public void den_som_inte_har_den_kan_inte_lamna_tillbaka_den() {
        assertTrue(Mic.take(bubble));
        Mic.release(keyboard);
        assertFalse("tangentbordet lämnade tillbaka bubblans spärr", Mic.take(home));
    }

    @Test public void release_utan_tag_gar_igenom() {
        Mic.release(bubble);
        assertTrue(Mic.take(keyboard));
    }

    // ------------------------------------------- nekad start lämnar inget kvar

    /**
     * Vägen som gick fel: spärren tas, taltjänsten visar sig saknas, och
     * ytan går hem. Lämnas spärren inte tillbaka i den grenen är mikrofonen
     * låst för de andra två.
     */
    @Test public void nekad_start_lamnar_inte_sparren_tagen() {
        assertTrue(Mic.take(home));
        // Det hemskärmen gör i dag när isRecognitionAvailable svarar nej,
        // och det bubblan och tangentbordet gör när startListening kastar.
        Mic.release(home);
        assertFalse(Mic.heldByOther(bubble));
        assertTrue("bubblan kom inte åt mikrofonen efter en nekad start",
                Mic.take(bubble));
    }

    @Test public void ett_undantag_i_startlaget_lamnar_inte_sparren_tagen() {
        assertTrue(Mic.take(keyboard));
        try {
            throw new IllegalStateException("taltjänsten nekade starten");
        } catch (RuntimeException refused) {
            Mic.release(keyboard);
        }
        assertTrue(Mic.take(home));
    }

    // -------------------------------------------------- sjuttiosekundersgardet

    @Test public void en_gammal_sparr_gar_att_ta_over() throws Exception {
        assertTrue(Mic.take(bubble));
        age(71_000L);
        assertFalse("en övergiven spärr ska inte räknas som hållen",
                Mic.heldByOther(keyboard));
        assertTrue("tangentbordet fick inte ta över en övergiven spärr",
                Mic.take(keyboard));
    }

    @Test public void en_fardig_sparr_gar_inte_att_ta_over_i_fortid() throws Exception {
        assertTrue(Mic.take(bubble));
        age(69_000L);
        assertTrue("en pågående diktering får inte bli av med mikrofonen",
                Mic.heldByOther(keyboard));
        assertFalse(Mic.take(keyboard));
    }

    /**
     * Kärnan i gardet: en yta som dödas utan att lämna tillbaka spärren får
     * inte låsa mikrofonen för resten av processens liv. Ingen ägare, hur
     * gammal spärren än är, kan hindra nästa yta när tiden gått.
     */
    @Test public void gardet_kan_inte_lamna_sparren_tagen_for_alltid() throws Exception {
        assertTrue(Mic.take(bubble));
        // Ytan försvann utan release. Spärren står kvar med bubblan som ägare.
        age(10L * 70_000L);
        assertTrue(Mic.take(keyboard));
        assertFalse(Mic.heldByOther(keyboard));
    }

    @Test public void overtagandet_nollar_aldern() throws Exception {
        assertTrue(Mic.take(bubble));
        age(71_000L);
        assertTrue(Mic.take(keyboard));
        // Den nya ägaren ska ha hela sin tid, inte den gamlas återstod.
        assertTrue(Mic.heldByOther(bubble));
        assertFalse(Mic.take(bubble));
    }

    @Test public void den_gamla_agaren_kan_inte_stjala_tillbaka_sparren() throws Exception {
        assertTrue(Mic.take(bubble));
        age(71_000L);
        assertTrue(Mic.take(keyboard));
        // Bubblans sena release får inte öppna tangentbordets spärr.
        Mic.release(bubble);
        assertFalse("bubblans release tog tangentbordets spärr", Mic.take(home));
    }

    // ------------------------------------------------------------------ hjälp

    /** Flyttar spärrens starttid bakåt, alltså tiden framåt. */
    private void age(long millis) throws Exception {
        Field since = Mic.class.getDeclaredField("since");
        since.setAccessible(true);
        since.setLong(null, since.getLong(null) - millis);
    }

    private void set(String name, Object value) throws Exception {
        Field field = Mic.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
