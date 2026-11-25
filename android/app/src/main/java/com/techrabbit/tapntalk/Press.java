package com.techrabbit.tapntalk;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.view.View;

/**
 * Trycket, rev-ux 4.2 A12: allt tryckbart skalas till 0,97 medan fingret
 * ligger kvar, och något händer inom 100 ms.
 *
 * Android har en nedtryckt-färg i {@code Look.skin}, men ingen rörelse. En
 * knapp som bara byter nyans känns långsammare än den är, särskilt den stora
 * Tala-knappen, där hela ytan är densamma före och efter trycket.
 *
 * Rörelsen hänger på vyns nedtryckta tillstånd genom en
 * {@link StateListAnimator}, inte på en egen {@code OnTouchListener}.
 * Skillnaden är inte kosmetisk: en lyssnare som fångar varje
 * {@code ACTION_DOWN} och svarar falskt kommer i vägen för
 * tillgänglighetstjänsterna, och lint säger det rakt ut
 * (ClickableViewAccessibility). Tillståndet är dessutom sannare — det följer
 * knappens egen {@code pressed}, så ett tryck som glider ut ur knappen eller
 * avbryts av en scroll släpper skalan av sig själv, utan att vi behöver
 * räkna händelser.
 *
 * Med reducerad rörelse tonas knappen i stället för att krympa: tiden
 * kommer från {@link Motion}, och inställningen gäller rörelse, inte
 * återkoppling.
 */
final class Press {

    /** Samma tal som `Move.pressScale` på iOS. */
    static final float SCALE = 0.97f;
    /** Samma tal som `Move.pressDim`. */
    static final float DIM = 0.8f;
    /** Trycket ska svara inom 100 ms. 90 är rev-ux rörelsetoken `tap`. */
    static final long TAP = 90L;

    private Press() { }

    /** Lägger trycket på en vy. */
    static void on(final View view) {
        if (view == null) return;
        long time = Motion.swap(view.getContext(), TAP);
        boolean still = Motion.paused(view.getContext());

        // Vyn kan ha legat nedtryckt eller tonad från en tidigare
        // inställning. Utgångsläget skrivs om varje gång trycket läggs på.
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setAlpha(1f);

        StateListAnimator press = new StateListAnimator();
        press.addState(new int[]{android.R.attr.state_pressed},
                still ? dim(DIM, time) : size(SCALE, time));
        press.addState(new int[]{},
                still ? dim(1f, time) : size(1f, time));
        view.setStateListAnimator(press);
    }

    /** Skalan. Målet sätts av tillståndet, därför är animatorns mål null. */
    private static AnimatorSet size(float to, long time) {
        AnimatorSet both = new AnimatorSet();
        both.playTogether(ObjectAnimator.ofFloat((View) null, View.SCALE_X, to),
                ObjectAnimator.ofFloat((View) null, View.SCALE_Y, to));
        both.setDuration(time);
        return both;
    }

    /** Tonen, för den som bett om mindre rörelse. */
    private static AnimatorSet dim(float to, long time) {
        AnimatorSet one = new AnimatorSet();
        one.playTogether(ObjectAnimator.ofFloat((View) null, View.ALPHA, to));
        one.setDuration(time);
        return one;
    }
}
