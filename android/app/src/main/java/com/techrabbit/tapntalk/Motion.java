package com.techrabbit.tapntalk;

import android.content.Context;
import android.provider.Settings;

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

    /** Falskt när telefonen har stängt av animationer. */
    static boolean wanted(Context context) {
        float scale = Settings.Global.getFloat(
                context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return scale != 0f;
    }
}
