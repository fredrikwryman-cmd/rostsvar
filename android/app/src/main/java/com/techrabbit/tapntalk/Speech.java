package com.techrabbit.tapntalk;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

/**
 * Valet av taltjänst, på ett ställe för alla tre ytorna.
 *
 * iOS har sedan början bett om tolkning på enheten när telefonen klarar det
 * ({@code requiresOnDeviceRecognition}). Android gjorde inte det: bubblan,
 * hemskärmen och tangentbordet tog den vanliga taltjänsten, och på de flesta
 * telefoner är det Googles, som kan skicka ljudet till sina servrar. Nu
 * frågar alla tre efter enhetstolkning först.
 *
 * Enhetstolkning finns från Android 13 och bara när telefonen har hämtat
 * svenska. Saknas den, eller visar den sig inte kunna svenska först när
 * dikteringen börjat, går omgången över till den vanliga taltjänsten. Ingen
 * ska bli utan diktering för att vi ville slippa molnet.
 *
 * Klassen har två halvor. De statiska metoderna rör Android och går inte att
 * köra utan telefon. Tillståndet nedanför är vanlig Java, och det är där
 * reglerna bor: vilken tjänst som valdes, om bytet redan är gjort, och om ett
 * fel alls är värt ett byte. Den halvan mäts av {@code SpeechTest}.
 */
final class Speech {

    /** Första versionen med enhetstolkning i SpeechRecognizer. */
    static final int ON_DEVICE_FROM = 33;

    // --------------------------------------------------------------- reglerna

    /**
     * Sant när enhetstolkning ska väljas före den vanliga taltjänsten.
     *
     * @param sdk    telefonens Android-version.
     * @param ready  svaret från {@code isOnDeviceRecognitionAvailable}.
     */
    static boolean onDeviceFirst(int sdk, boolean ready) {
        return sdk >= ON_DEVICE_FROM && ready;
    }

    /**
     * Sant när någon form av diktering alls är möjlig.
     *
     * Enhetstolkningen räknas med: en telefon kan ha den utan att ha någon
     * vanlig {@code RecognitionService} installerad, och då ska Tala-knappen
     * gå att trycka på ändå.
     */
    static boolean anyService(int sdk, boolean ready, boolean service) {
        return service || onDeviceFirst(sdk, ready);
    }

    /**
     * Sant när felet betyder att enhetstolkningen inte går att använda för
     * svenska, och den vanliga taltjänsten alltså är värd ett försök.
     *
     * De tre första koderna kom till i Android 13 tillsammans med
     * enhetstolkningen och säger precis det: språket stöds inte, språket är
     * inte hämtat, stödet gick inte att kontrollera. ERROR_CLIENT är med
     * eftersom en enhetstolkning utan tjänst bakom sig svarar så.
     *
     * Nät-, ljud-, server- och tystnadsfel är inte med. De säger ingenting
     * om att tjänsten är fel vald, och ett byte skulle bara kosta
     * användaren en omgång till.
     */
    static boolean cloudWorthATry(int code) {
        return code == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                || code == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                || code == SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT
                || code == SpeechRecognizer.ERROR_CLIENT;
    }

    // -------------------------------------------------------------- tillstånd

    /** Sant när den tjänst som är igång är enhetstolkningen. */
    private boolean onDevice;

    /** Sant när omgången redan bytt tjänst. Bytet görs högst en gång. */
    private boolean swapped;

    /** Skrivs när en tjänst skapats, med vilken av de två det blev. */
    void chose(boolean onDevice) {
        this.onDevice = onDevice;
    }

    boolean onDevice() {
        return onDevice;
    }

    boolean swapped() {
        return swapped;
    }

    /**
     * Frågan ytan ställer i {@code onError}: ska omgången göras om på den
     * vanliga taltjänsten?
     *
     * Svaret är ja bara när allt stämmer: det är enhetstolkningen som
     * klagar, bytet är inte gjort än, felet är ett av dem som ett byte
     * hjälper mot, och ingenting har hörts. Det sista är regeln som väger
     * tyngst: hörs något levereras det, och då är en ny omgång fel svar —
     * den skulle kasta texten och be användaren tala igen.
     *
     * Svarar den ja räknas bytet som gjort i samma andetag, så att ett
     * andra fel aldrig ger ett andra byte.
     */
    boolean swapToCloud(int code, boolean anythingHeard) {
        if (!onDevice || swapped || anythingHeard) return false;
        if (!cloudWorthATry(code)) return false;
        swapped = true;
        onDevice = false;
        return true;
    }

    /** Omgången är över. Nästa diktering får byta en gång den också. */
    void roundOver() {
        swapped = false;
    }

    // ------------------------------------------------------------ mot Android

    /** Svaret från Android om enhetstolkning finns, eller falskt före 33. */
    private static boolean deviceReady(Context context) {
        if (Build.VERSION.SDK_INT < ON_DEVICE_FROM) return false;
        try {
            return SpeechRecognizer.isOnDeviceRecognitionAvailable(context);
        } catch (RuntimeException unknown) {
            // Frågan finns men svarar inte på alla telefoner. Vet vi inte,
            // påstår vi ingenting: den vanliga taltjänsten får duga.
            return false;
        }
    }

    /** Sant när någon taltjänst finns att diktera med. */
    static boolean available(Context context) {
        boolean service;
        try {
            service = SpeechRecognizer.isRecognitionAvailable(context);
        } catch (RuntimeException unknown) {
            service = false;
        }
        return anyService(Build.VERSION.SDK_INT, deviceReady(context), service);
    }

    /**
     * Öppnar den tjänst som ska användas, enhetstolkning först.
     *
     * Kastar enhetstolkningen vid skapandet tas den vanliga i stället, utan
     * att användaren märker något. Går inte heller den kastar metoden
     * vidare, och den som ringde får ta hand om det som vilken nekad start
     * som helst.
     */
    static SpeechRecognizer open(Context context, Speech picker) {
        if (onDeviceFirst(Build.VERSION.SDK_INT, deviceReady(context))) {
            try {
                SpeechRecognizer ears = SpeechRecognizer.createOnDeviceSpeechRecognizer(context);
                picker.chose(true);
                return ears;
            } catch (RuntimeException missing) {
                // Svaret var ja, men tjänsten gick inte att skapa.
            }
        }
        return cloud(context, picker);
    }

    /** Öppnar den vanliga taltjänsten, den omgången byter till. */
    static SpeechRecognizer cloud(Context context, Speech picker) {
        SpeechRecognizer ears = SpeechRecognizer.createSpeechRecognizer(context);
        picker.chose(false);
        return ears;
    }

    /** Frågan till taltjänsten. Samma på alla tre ytorna, därför här. */
    static Intent request(String callingPackage) {
        Intent ask = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE");
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "sv-SE");
        ask.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        ask.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        ask.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, callingPackage);
        return ask;
    }
}
