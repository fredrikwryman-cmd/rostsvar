package com.techrabbit.tapntalk;

import android.view.MotionEvent;
import android.view.View;

/**
 * Trycket, rev-ux 4.2 A12: allt tryckbart skalas till 0,97 medan fingret
 * ligger kvar, och något händer inom 100 ms.
 *
 * Android har en nedtryckt-färg i {@code Look.skin}, men ingen rörelse. En
 * knapp som bara byter nyans känns långsammare än den är, särskilt den stora
 * Tala-knappen, där hela ytan är densamma före och efter trycket.
 *
 * Lyssnaren svarar alltid falskt. Trycket går vidare som förut, knappen får
 * sitt klick, och TalkBack går sin egen väg in utan att röra det här.
 * Skalningen är dekor och får aldrig stå i vägen för handlingen.
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
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View touched, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        down(touched, true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        down(touched, false);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private static void down(View view, boolean pressed) {
        long time = Motion.swap(view.getContext(), TAP);
        if (Motion.paused(view.getContext())) {
            view.animate().cancel();
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.animate().alpha(pressed ? DIM : 1f).setDuration(time).start();
            return;
        }
        float to = pressed ? SCALE : 1f;
        view.animate().cancel();
        view.setAlpha(1f);
        view.animate().scaleX(to).scaleY(to).setDuration(time).start();
    }
}
