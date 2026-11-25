package com.techrabbit.tapntalk;

import android.content.Context;
import android.provider.Settings;
import android.view.View;

/**
 * Om telefonen vill ha rörelse eller inte.
 *
 * iOS läser `accessibilityReduceMotion` och stillar både ringarna, glöden och
 * den blinkande markören. Android gjorde ingenting: bubblan andades med
 * rösten vad användaren än hade ställt in.
 *
 * Android har ingen egen flagga för "reducera rörelse". Det närmaste är
 * animationsskalan, som Utvecklaralternativ och flera tillverkares
 * tillgänglighetsinställningar sätter till noll just för att stänga av
 * rörelse. Noll betyder stilla.
 */
final class Motion {

    private Motion() { }

    /**
     * Crossfadens längd, 160 ms. Samma tal som `Move.swap` på iOS.
     *
     * Med reducerad rörelse ersätts förflyttning, zoom och puls med en
     * toning på den här tiden. Inte med ingenting: ett byte utan animation
     * hoppar, och ett hopp är också rörelse.
     */
    static final long CROSSFADE = 160L;

    /** Lägesbytets vanliga längd, 200 ms. */
    static final long STATE = 200L;

    /**
     * Hur länge ett lägesbyte får ta på den här telefonen.
     *
     * Den enda kontrollen, inte en per vy: vyerna frågar här och får ett
     * svar, i stället för att var och en läsa inställningen och gissa vad
     * den betyder.
     */
    static long swap(Context context, long normal) {
        return wanted(context) ? normal : CROSSFADE;
    }

    /** Hur länge en förflyttning får ta, eller en toning i dess ställe. */
    static long travel(Context context, long normal) {
        return swap(context, normal);
    }

    /**
     * Sant när en puls, en andning eller en vandrande markör ska stå still.
     * Det som står still ska stå still, inte försvinna: formen är en del av
     * skärmen.
     */
    static boolean paused(Context context) {
        return !wanted(context);
    }

    /**
     * Byter innehåll i en vy med en crossfade i stället för att låta det
     * hoppa. Vid reducerad rörelse är det här hela animationen som finns.
     *
     * @param change körs mitt i toningen, när vyn är genomskinlig.
     */
    static void crossfade(final View view, final Runnable change) {
        if (view == null) {
            if (change != null) change.run();
            return;
        }
        long half = CROSSFADE / 2;
        view.animate().cancel();
        view.setAlpha(1f);
        view.animate().alpha(0f).setDuration(half).withEndAction(new Runnable() {
            @Override public void run() {
                if (change != null) change.run();
                view.animate().alpha(1f).setDuration(half).start();
            }
        }).start();
    }

    /** Falskt när telefonen har stängt av animationer. */
    static boolean wanted(Context context) {
        float scale = Settings.Global.getFloat(
                context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return scale != 0f;
    }
}
