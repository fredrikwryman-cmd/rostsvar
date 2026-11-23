package se.rostsvar.mobile;

/**
 * Vem av appens tre ytor som har mikrofonen just nu.
 *
 * Bubblan, rösttangentbordet och hemskärmen ligger i samma process men vet
 * inget om varandra. Startar två av dem taligenkänningen samtidigt svarar
 * Android med ERROR_RECOGNIZER_BUSY på den andra — och användaren får ett fel
 * i stället för ett besked om vad som faktiskt pågår. Spärren här gör att den
 * som är sist ute får veta det i förväg, och att den som talar får tala
 * färdigt.
 *
 * Spärren går att ta över när den är gammal. En yta som dödas av systemet
 * hinner inte alltid lämna tillbaka den, och då får den inte låsa mikrofonen
 * för resten av processens liv. Gränsen ligger över dikteringens egen längsta
 * omgång, så en pågående diktering kan aldrig bli av med mikrofonen.
 */
final class Mic {

    /** Äldre än så här är ett innehav övergivet. */
    private static final long STALE = 70_000L;

    private static Object holder;
    private static long since;

    private Mic() { }

    /** Tar mikrofonen, eller svarar falskt när någon annan har den. */
    static synchronized boolean take(Object who) {
        long now = System.currentTimeMillis();
        if (holder != null && holder != who && now - since < STALE) return false;
        holder = who;
        since = now;
        return true;
    }

    /** Lämnar tillbaka mikrofonen. Tål att kallas utan att ha den. */
    static synchronized void release(Object who) {
        if (holder == who) holder = null;
    }

    /** Sant när någon annan än den som frågar har mikrofonen. */
    static synchronized boolean heldByOther(Object who) {
        return holder != null && holder != who
                && System.currentTimeMillis() - since < STALE;
    }
}
