package com.techrabbit.tapntalk;

import android.speech.SpeechRecognizer;
import android.util.Log;

/**
 * Beskeden om taltjänsten, på ett ställe.
 *
 * Förr hade hemskärmen, bubblan och rösttangentbordet var sin tabell, och de
 * sa olika saker om samma fel: hemskärmen berättade att svenska går att hämta
 * för att slippa nätet, tangentbordet sa bara att nätet inte gick att nå, och
 * bubblan hade ingen gren för nätet alls och föll till ett felnummer. Finns
 * tabellen bara en gång kan den bara vara fel en gång.
 *
 * Varje mening säger tre saker: vad som hände, vad det betyder, och vad
 * användaren gör nu. Det sista ledet skiljer sig mellan ytorna — man trycker
 * på Tala i appen och i tangentbordet, men på bubblan när det är bubblan som
 * lyssnar — därför skickas det in.
 *
 * Konstanterna i SpeechRecognizer är static final int och bakas in av
 * kompilatorn, men sedan punkt 17 loggas felkoden här, och android.util.Log
 * är ett riktigt beroende. Ska tabellen mätas på en ren JVM behöver Log
 * skärmas av först.
 */
final class Notices {

    /** Vad man gör för att prova igen, på appens och tangentbordets ytor. */
    static final String TALK = "Tryck på Tala";
    /** Samma sak, men bubblan är det man trycker på. */
    static final String BUBBLE = "Tryck på bubblan";

    static final String NO_SERVICE =
            "Telefonen har ingen taltjänst som kan tolka tal. Hämta en rösttjänst "
            + "med svenska i telefonens inställningar.";

    /**
     * Samma mening om nätet på alla ytor, och på iOS i Notice.offline.
     * Nätet är inte hela sanningen: svensk diktering på enheten finns på de
     * flesta telefoner, den är bara inte hämtad. Därför står vägen dit med.
     */
    static final String OFFLINE =
            "Taltjänsten nådde inte nätet, så talet kunde inte tolkas. Många "
            + "telefoner klarar svenska utan nät när språket är hämtat i "
            + "telefonens röstinställningar.";

    static final String BUSY_ELSEWHERE =
            "Mikrofonen används av Tap&Talk någon annanstans. Avsluta dikteringen "
            + "där först.";

    /** Dikteringen gick igenom. Samma ord som iOS-tangentbordet använder. */
    static final String DONE_KEYBOARD = "Klart. Rätta med tangenterna om du vill.";

    static final String LISTENING = "Lyssnar. Tala svenska och tryck på Klar.";

    static final String FINISHING = "Slutför texten …";

    static final String ABANDONED = "Dikteringen avbröts. Texten står kvar.";

    private Notices() { }

    /**
     * Beskedet när taltjänsten avslutade med ett fel.
     *
     * Avslutsregeln bor här: hördes något levereras det, och då får användaren
     * aldrig höra att inget uppfattades. Det var precis den lögnen som
     * felsökningspasset handlade om.
     *
     * @param code  felkoden från SpeechRecognizer
     * @param heard sant när den här omgången hann höra något som står kvar
     * @param again TALK eller BUBBLE
     */
    static String afterFailure(int code, boolean heard, String again) {
        if (heard) {
            // Koden loggas även här: texten räddades, men felet hände.
            Log.w(TAG, "taltjansten svarade med fel " + code + ", text raddad");
            return "Taltjänsten avslutade med ett fel, men det här hördes. "
                    + "Läs igenom texten.";
        }
        return failure(code, again);
    }

    /**
     * Beskedet när taltjänsten inte svarade alls inom tidsgränsen. Hördes
     * något står det kvar, och det är det användaren behöver veta.
     */
    static String afterSilence(boolean heard, String again) {
        if (heard) {
            return "Taltjänsten svarade inte, men det som hördes står kvar. "
                    + "Läs igenom texten.";
        }
        return "Taltjänsten svarade inte. " + again + " och prova igen.";
    }

    /** Taggen felkoderna loggas under. `adb logcat -s TapnTalk` visar dem. */
    private static final String TAG = "TapnTalk";

    /**
     * Felet som en mening, utan hänsyn till om något hördes.
     *
     * Koden loggas men visas inte. Förr stod den i rutan — "Taltjänsten
     * svarade med ett fel (7)" — vilket inte hjälper någon som ska diktera,
     * och den som felsöker hittar den i logcat i stället.
     */
    static String failure(int code, String again) {
        Log.w(TAG, "taltjansten svarade med fel " + code);
        return sentence(code, again);
    }

    private static String sentence(int code, String again) {
        switch (code) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Hörde inget. " + again + " och prova igen.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Taltjänsten fick inte använda mikrofonen. Tillåt mikrofonen "
                        + "i Tap&Talk och prova igen.";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return OFFLINE + " " + again + " och prova igen.";
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                return "Taltjänsten har inte svenska installerad, så talet kunde inte "
                        + "tolkas. Hämta svenska i telefonens röstinställningar.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Taltjänsten var upptagen med något annat. " + again
                        + " om ett ögonblick.";
            case SpeechRecognizer.ERROR_AUDIO:
                return "Mikrofonen gick inte att läsa. Stäng andra appar som spelar "
                        + "in, och " + again.toLowerCase(java.util.Locale.ROOT)
                        + " igen.";
            default:
                return "Taltjänsten kunde inte tolka talet. " + again
                        + " och prova igen.";
        }
    }
}
