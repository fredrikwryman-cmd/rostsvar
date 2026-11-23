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
