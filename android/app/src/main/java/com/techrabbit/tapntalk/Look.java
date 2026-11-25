package com.techrabbit.tapntalk;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

/**
 * Appens utseende på ett ställe: svart bakgrund och grön accent, samma linje
 * på hemskärmen, i guiden och i tangentbordet. Grönt är kupolens färg i
 * appikonen.
 */
final class Look {

    // Samma värden som ios/Shared/Palette.swift, §2 i Tech Rabbit-underlaget.
    // De två plattformarna kan inte dela en fil, men de ska inte skilja sig i
    // värde. Förr hade Android ett eget grönt (0x3FE08A) och iOS-appen ett
    // annat (0x1CF038), och iOS-tangentbordet ett tredje.
    static final int BG = 0xFF0B0B0B;
    static final int INK = 0xFFF2F2F2;
    static final int DIM = 0xFFA8A8A8;
    static final int GREEN = 0xFF1CF038;
    static final int GREEN_DEEP = 0xFF12A426;
    /** Mörk text på grön knapp. */
    static final int DARK_INK = 0xFF0B0B0B;
    static final int FIELD = 0xFF1A1A1A;
    static final int BORDER = 0xFF707070;
    /** Kanten runt en sekundär knapp som går att trycka på. §6. */
    static final int DIM_DOT = 0xFF8A8A8A;
    /** Linjen runt en tangent. Tangentens yta når inte 3:1 mot botten. */
    static final int KEY_EDGE = 0xFF707070;
    /**
     * Tonen för innehåll som är avstängt eller inte nått än: guidens
     * förklaringsrad, platshållartexten i fälten, stegen som ligger framåt i
     * guiden, och Skicka och Tala när de inte går att trycka på.
     *
     * Var 0xFF3A3F42 fram till granskningen, och det gav uppmätt 1,75:1 mot
     * FIELD och 1,97:1 mot BG — kravet är 4,5:1. Det var inte dekor utan
     * riktig text, bland annat integritetsförklaringarna i guiden, som ingen
     * med nedsatt syn kunde läsa.
     *
     * Värdet är iOS-palettens faint, samma ton till samma uppgift: 5,04:1 mot
     * FIELD och 5,70:1 mot BG, och fortfarande tydligt under DIM (7,32 och
     * 8,28), så ordningen mellan tonerna är kvar.
     */
    static final int OFF = 0xFF8A8A8A;
    /** Varningens gula, används bara när något fattas. */
    static final int WARN = 0xFFFFC46B;
    /** Varningsrutans yta: det gula mörkt nedtonat. */
    static final int WARN_FILL = 0xFF1A1407;
    /** Genomskinligt. En knapp i vila som bara är en ram. */
    static final int CLEAR = 0x00000000;
    /**
     * Svart, helt täckande. Mörkläggningen bakom ett ark ligger här, och
     * styrkan sätts med vyns alfa i stället för i färgen: den ska kunna
     * tonas in och ut.
     */
    static final int BLACKOUT = 0xFF000000;
    /**
     * Svart på en tredjedel. Bubblans kant mot en bakgrund appen inte
     * känner: den ligger över andra appar, och en ljus app bakom skulle
     * annars sudda ut den gröna skivans kontur.
     */
    static final int SHADE = 0x55000000;

    // Rösttangentbordets egna ytor. Tangentbordet är mörkare än appen: det
    // ligger under innehållet i någon annans app och ska inte lysa.
    /** Tangentbordets botten. */
    static final int BOARD = 0xFF101315;
    /** Statusradens yta i tangentbordet. */
    static final int BOARD_BAR = 0xFF1B1F21;
    /** Tangentens yta, och samma yta nedtryckt. */
    static final int KEY = 0xFF1E2325;
    static final int KEY_DOWN = 0xFF2E3437;

    // ------------------------------------------------------------ måtten
    //
    // Måtten låg förut som tal i vyerna: dp(20) för sidmarginalen på fem
    // ställen i tre filer, dp(14) för kortets radie bland tjugotre andra
    // dp(14) som betydde luft. Det gick att ändra en skärm och tro att man
    // ändrat alla. Värdena står i dp och är de som redan användes; den här
    // listan flyttar dem, den ändrar dem inte.
    //
    // iOS har samma uppsättning i ios/Shared/Theme.swift. De två
    // plattformarna kan inte dela en fil, men ska inte skilja sig i värde.

    /** Sidmarginalen på hemskärmen och i guiden. */
    static final int SIDE = 20;
    /** Sidans luft upptill och nedtill. */
    static final int PAGE_TOP = 28;
    static final int PAGE_BOTTOM = 24;
    /** Kortets och fältets inre luft. */
    static final int CARD_PAD = 14;
    /** Kortets och fältets radie. */
    static final int RADIUS_CARD = 14;
    /** Pillerformen: halva knappens höjd. */
    static final int RADIUS_PILL = 26;
    /** Tangentens radie i rösttangentbordet. */
    static final int RADIUS_KEY = 7;
    /** Guidens knapp i vila: en ram utan fylld yta. */
    static final int RADIUS_GHOST = 10;
    /** Tala-knappens radie på hemskärmen. */
    static final int RADIUS_MIC = 16;
    /** Den lilla rutan: varningens ikonplatta, pillerknappen i huvudet. */
    static final int RADIUS_SMALL = 12;
    /** Bubblans radie: en cirkel, alltså halva sidan. */
    static final int RADIUS_ROUND = 28;

    static final int GAP_HAIR = 2;
    static final int GAP_TICK = 3;
    static final int GAP_TINY = 4;
    static final int GAP_XS = 6;
    static final int GAP_S = 8;
    static final int GAP_SM = 10;
    static final int GAP_M = 12;
    static final int GAP_ML = 14;
    static final int GAP_L = 16;
    static final int GAP_XL = 18;
    static final int GAP_XXL = 20;
    static final int GAP_WIDE = 22;
    static final int GAP_XHUGE = 24;
    static final int GAP_HUGE = 28;
    /** Den minsta knuffen: tangentens luft uppåt i tangentbordet. */
    static final int GAP_NUDGE = 5;

    /** Minsta träffyta. 48 dp är Androids golv, inte ett riktmärke. */
    static final int TOUCH = 48;
    /** Primär- och sekundärknapp. */
    static final int CONTROL_H = 52;
    /** Pillerknappen i huvudet. */
    static final int PILL_H = 46;
    static final int PILL_W = 92;
    /** Tala-knappen på hemskärmen. */
    static final int MIC_H = 84;
    /** Strömbrytarens minsta bredd. */
    static final int SWITCH_W = 56;
    /** Guidens höga rad. */
    static final int ROW_TALL = 56;
    /** Bubblans sida. */
    static final int BUBBLE_SIZE = 66;
    /** Textfältets minsta höjd. */
    static final int FIELD_MIN = 96;
    /** Snurran som ersätter lägesprickan. */
    static final int SPINNER = 18;

    /** Kanter: en hårfin linje, och den som bär en komponentgräns. */
    static final int HAIRLINE = 1;
    static final int EDGE = 2;

    /**
     * Avdelaren mellan listrader, som rgba(255,255,255,0.08). Beslut A i
     * ändringslistan: bara mellan rader, aldrig som komponentgräns — en
     * gräns ska klara 3:1 och det gör inte den här.
     */
    static final int DIVIDER = 0x14FFFFFF;

    // -------------------------------------------------------- typskalan
    //
    // Storlekarna i sp, rollerna är de som finns på skärmarna.

    /** Appnamnet i huvudet. */
    static final int TYPE_APP_NAME = 30;
    /** Skärmrubrik. */
    static final int TYPE_TITLE = 22;
    /** Raden över rubriken i guiden. */
    static final int TYPE_LEAD = 21;
    /** Rubrik i ett kort, och etiketten på Tala-knappen. */
    static final int TYPE_HEADING = 19;
    /** Utkastet i fältet. */
    static final int TYPE_DRAFT = 18;
    /** Knapptext. */
    static final int TYPE_BUTTON = 17;
    /** Brödtext. */
    static final int TYPE_BODY = 16;
    /** Sekundär rad. */
    static final int TYPE_SUBHEAD = 15;
    /** Lägesord och förklaringsrad. */
    static final int TYPE_CAPTION = 14;
    /** Teckenräknaren och etiketter. */
    static final int TYPE_SMALL = 13;
    /**
     * Funktionstangent med ett ord på: 123, ABC, Klar.
     *
     * Ett ord behöver mer bredd än en glyf och får därför mindre text, annars
     * står det "Mellansl…" på tangenten. Storleken låg förut som ett tal i
     * {@code VoiceKeys}.
     */
    static final int TYPE_TOOL_WORD = 14;
    /** Funktionstangent med en glyf på: ⇧, ⌫, ⏎. */
    static final int TYPE_TOOL_GLYPH = 18;

    private Look() { }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable box(int fill, int stroke, int radiusPx, int strokePx) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(fill);
        shape.setCornerRadius(radiusPx);
        if (stroke != 0) shape.setStroke(strokePx, stroke);
        return shape;
    }

    static StateListDrawable skin(int rest, int down, int radiusPx) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, box(down, 0, radiusPx, 0));
        states.addState(new int[]{}, box(rest, 0, radiusPx, 0));
        return states;
    }
}
