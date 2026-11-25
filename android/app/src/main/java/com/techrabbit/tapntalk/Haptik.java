package com.techrabbit.tapntalk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

/**
 * Haptiken på ett ställe: hela kartan ur rev-ux avsnitt 7, en spärr på
 * 80 ms, och en av-knapp.
 *
 * Androidsidan hade ingen haptik alls. Tala-knappen, Rensa, Skicka, bubblans
 * lägesbyten och rösttangentbordets tangenter gav ingenting i handen, medan
 * iOS har vibrerat sedan Tech Rabbit-underlaget §8. Två appar med samma namn
 * som kändes olika.
 *
 * Kartan är händelser, inte styrkor, precis som i {@code Haptics.swift}. Den
 * som lägger till en knapp ska behöva säga vad knappen gör, inte välja
 * styrka.
 *
 * Vägen är {@code performHapticFeedback} på vyn. Den följer telefonens egen
 * inställning för beröringsvibration, kräver ingen behörighet, och ger varje
 * tillverkare chansen att använda sin egen motor. Två händelser har ingen
 * konstant som betyder rätt sak — varningen och upplösningen — och de går
 * genom {@link VibrationEffect} i stället.
 *
 * Haptiken ligger kvar vid reducerad rörelse. Den är inte rörelse på skärmen,
 * och för den som slagit på inställningen är den ofta hela bekräftelsen.
 */
final class Haptik {

    /** Tryck på primär- eller snabbhandling: Tala, Rensa, Skicka, Guide. */
    static final int PRESS = 1;
    /** Tangent, segment, steg i en väljare, karusellsnäpp. */
    static final int KEY = 2;
    /** Ett objekt låser i läge: bubblan snäpper till kanten. */
    static final int SNAP = 3;
    /** En uppgift lyckades, eller en stor handling landade. */
    static final int SUCCESS = 4;
    /** Fel, ogiltigt, över gräns. */
    static final int FAILURE = 5;
    /** Före en oåterkallelig handling. */
    static final int WARNING = 6;
    /** Tillståndsbyte utan navigering: bubblans tre lägen. */
    static final int STATE_CHANGE = 7;
    /** Något löses upp och tas bort. */
    static final int REMOVE = 8;
    /** Känsligt innehåll döljs eller visas. */
    static final int PRIVACY = 9;
    /** Texten lämnas vidare. */
    static final int SEND = 10;
    /** Mikrofonen öppnas. Tyngre än när den stängs. */
    static final int RECORD_START = 11;
    /** Mikrofonen stängs. */
    static final int RECORD_STOP = 12;

    /**
     * Nyckeln av-knappen skriver. Samma namn som iOS använder, och samma
     * håll: lagrat är AV, så att påslaget är standard utan att något
     * behöver skrivas vid första starten.
     */
    static final String KEY_OFF = "haptik_av";

    /**
     * Två haptiker närmare än så här känns som en enda smet, och i värsta
     * fall som att telefonen skakar. Tiden står i rev-ux avsnitt 7.
     */
    static final long GAP = 80L;

    /**
     * Spärren är process-vid och statisk av samma skäl som {@link Mic}:
     * hemskärmen, bubblan och rösttangentbordet lever i samma process och
     * kan vibrera i varandras fönster.
     */
    private static long last;

    private Haptik() { }

    /** Sant när haptiken är påslagen. Standard är på. */
    static synchronized boolean on(Context context) {
        if (context == null) return false;
        return !prefs(context).getBoolean(KEY_OFF, false);
    }

    /** Slår på eller av haptiken för alla appens ytor. */
    static synchronized void set(Context context, boolean wanted) {
        if (context == null) return;
        prefs(context).edit().putBoolean(KEY_OFF, !wanted).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(Guide.SETTINGS, Context.MODE_PRIVATE);
    }

    /**
     * Vibrerar för en händelse, om haptiken är på och spärren släppt.
     *
     * Alltid i par med något synligt: ett ord som byts, en färg som ändras,
     * en bock som växer fram. En vibration utan något på skärmen säger
     * ingenting till den som inte höll telefonen i handen just då.
     *
     * @param view  vyn händelsen hör till. Null går bra: då tas
     *              vibratorvägen, som inte behöver något fönster.
     */
    static synchronized void fire(View view, Context context, int event) {
        Context where = context != null ? context
                : view != null ? view.getContext() : null;
        if (!on(where)) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - last < GAP) return;
        last = now;
        if (!feedback(view, event)) buzz(where, event);
    }

    /** Kortformen för en vy som vet sitt eget sammanhang. */
    static void fire(View view, int event) {
        fire(view, null, event);
    }

    /**
     * Kartan, väg ett: telefonens egen beröringsvibration.
     *
     * @return falskt när vyn inte kunde vibrera, eller när händelsen inte
     *         har någon konstant som betyder rätt sak.
     */
    private static boolean feedback(View view, int event) {
        if (view == null) return false;
        int constant;
        switch (event) {
            case KEY:
                constant = HapticFeedbackConstants.KEYBOARD_TAP;
                break;
            case SNAP:
            case PRIVACY:
                constant = HapticFeedbackConstants.CLOCK_TICK;
                break;
            case SUCCESS:
                constant = HapticFeedbackConstants.CONFIRM;
                break;
            case FAILURE:
                constant = HapticFeedbackConstants.REJECT;
                break;
            case STATE_CHANGE:
            case RECORD_START:
                // Mikrofonen öppnas tyngre än den stängs: det är att öppna
                // den som behöver kännas. rev-ux avsnitt 7, och
                // ändringslistans rad om Tala-knappen.
                constant = HapticFeedbackConstants.LONG_PRESS;
                break;
            case PRESS:
            case SEND:
            case RECORD_STOP:
                constant = HapticFeedbackConstants.VIRTUAL_KEY;
                break;
            default:
                // WARNING och REMOVE har ingen konstant som betyder rätt
                // sak. De går genom vibratorn i stället.
                return false;
        }
        return view.performHapticFeedback(constant,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
    }

    /**
     * Kartan, väg två: vibratorn direkt.
     *
     * Används för varningen, som ska kännas som två stötar och inte en, för
     * upplösningen, som ska kännas hård, och för varje händelse som uppstår
     * utan en vy — skrivtjänsten har inget fönster.
     *
     * Saknar telefonen vibrator, eller är behörigheten borttagen, händer
     * ingenting. Haptik är en bekräftelse och aldrig den enda, så det är
     * inget att berätta om.
     */
    private static void buzz(Context context, int event) {
        if (context == null) return;
        Vibrator motor = motor(context);
        if (motor == null || !motor.hasVibrator()) return;
        int effect;
        switch (event) {
            case WARNING:
                effect = VibrationEffect.EFFECT_DOUBLE_CLICK;
                break;
            case REMOVE:
            case RECORD_START:
            case STATE_CHANGE:
                effect = VibrationEffect.EFFECT_HEAVY_CLICK;
                break;
            case FAILURE:
                effect = VibrationEffect.EFFECT_DOUBLE_CLICK;
                break;
            case SNAP:
            case PRIVACY:
            case KEY:
                effect = VibrationEffect.EFFECT_TICK;
                break;
            default:
                effect = VibrationEffect.EFFECT_CLICK;
                break;
        }
        try {
            motor.vibrate(VibrationEffect.createPredefined(effect));
        } catch (RuntimeException refused) {
            // Behörigheten kan vara borttagen för hand i en ROM. Då är det
            // tyst, och inget annat.
        }
    }

    /** Sant när telefonen har en vibrator att vibrera med. */
    static boolean possible(Context context) {
        if (context == null) return false;
        Vibrator motor = motor(context);
        return motor != null && motor.hasVibrator();
    }

    /**
     * Telefonens vibrator.
     *
     * Från Android 12 kan en telefon ha flera motorer, och
     * {@code VIBRATOR_SERVICE} är ersatt av {@link VibratorManager}. Den
     * gamla vägen står kvar under: appen går att installera från API 30.
     */
    private static Vibrator motor(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager all = (VibratorManager)
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return all == null ? null : all.getDefaultVibrator();
        }
        return legacy(context);
    }

    /**
     * Vägen under Android 12. {@code VIBRATOR_SERVICE} är ersatt sedan API
     * 31, men appen går att installera från 30, så anropet måste finnas
     * kvar. Det står i en egen metod för att dämpningen ska gälla just den
     * här raden och inte dölja nästa varning någon annanstans i filen.
     */
    @SuppressWarnings("deprecation")
    private static Vibrator legacy(Context context) {
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
