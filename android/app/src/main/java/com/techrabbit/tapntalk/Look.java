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

    static final int BG = 0xFF000000;
    static final int INK = 0xFFF2F4F3;
    static final int DIM = 0xFF9AA3A0;
    static final int GREEN = 0xFF3FE08A;
    static final int GREEN_DEEP = 0xFF16A35A;
    /** Mörk text på grön knapp. */
    static final int DARK_INK = 0xFF05140C;
    static final int FIELD = 0xFF111315;
    static final int BORDER = 0xFF2A2F31;
    /**
     * Tonen för innehåll som är avstängt eller inte nått än: guidens
     * förklaringsrad, platshållartexten i fälten, stegen som ligger framåt i
     * guiden, och Skicka och Tala när de inte går att trycka på.
     *
     * Var 0xFF3A3F42 fram till granskningen, och det gav uppmätt 1,75:1 mot
     * FIELD och 1,97:1 mot BG — kravet är 4,5:1. Det var inte dekor utan
     * riktig text, bland annat integritetsförklaringarna i guiden, som ingen
     * med nedsatt syn kunde läsa. 0xFF7D8481 ger 4,87:1 mot FIELD och 5,49:1
     * mot BG, och ligger fortfarande tydligt under DIM (7,20 och 8,12), så
     * ordningen mellan tonerna är kvar.
     */
    static final int OFF = 0xFF7D8481;
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
