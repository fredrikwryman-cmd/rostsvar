package com.techrabbit.tapntalk;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tap&amp;Talks rösttangentbord: diktera svenska rakt ned i fältet man står
 * i, och vanliga tangenter för att rätta det som blev fel.
 *
 * Det som skrivs eller dikteras loggas aldrig och lämnar aldrig tangentbordet
 * annat än till fältet användaren själv står i. Tjänsten når inget nät; det
 * gör bara telefonens egen taltjänst.
 */
public class VoiceKeys extends InputMethodService {

    /** Samma svarta yta och gröna accent som appens skärm. Se Look. */
    private static final int BOARD = Look.BG;
    private static final int KEY = 0xFF1B1F21;
    private static final int KEY_DOWN = 0xFF2E3437;
    private static final int TOOL = 0xFF101315;
    private static final int TOOL_DOWN = 0xFF1E2325;
    private static final int INK = Look.INK;
    private static final int DIM = Look.DIM;
    private static final int GREEN = Look.GREEN;
    private static final int GREEN_DEEP = Look.GREEN_DEEP;
    private static final int DARK_INK = Look.DARK_INK;
    private static final int KEY_EDGE = Look.KEY_EDGE;

    /** Svensk layout. Tre rader bokstäver, med å ä ö på sina vanliga platser. */
    private static final String[] LETTERS = {"qwertyuiopå", "asdfghjklöä", "zxcvbnm"};
    /** Samma platser, men siffror och skiljetecken. */
    private static final String[] SYMBOLS = {"1234567890+", "@#%&-_()/:;", "*\"'!?€£"};

    private final Handler hand = new Handler(Looper.getMainLooper());
    /** Tangenterna som byter min, i samma ordning som raderna ovan. */
    private final List<Button> glyphs = new ArrayList<Button>();

    private LinearLayout board;
    /**
     * Statusraden. iOS-tangentbordet har haft en sedan början; här fanns
     * ingen, och allt gick genom en toast som dessutom tego vid de två
     * vanligaste felen. Tryck på Tala, säg inget, tryck Klar: ingenting
     * hände, och användaren fick aldrig veta varför.
     */
    private TextView says;
    /** Syns bara medan taltjänsten slutför. */
    private ProgressBar spin;
    private Button micKey;
    private Button shiftKey;
    private Button layerKey;

    private boolean shift;
    private boolean shiftLock;
    private long shiftTapped;
    private boolean symbols;

    private SpeechRecognizer speech;

    /** Vilken taltjänst omgången kör på, och om bytet redan är gjort. */
    private final Speech picker = new Speech();
    private boolean listening;
    /**
     * Sant mellan Tala och sista svaret. Rör användaren tangenterna under
     * tiden avbryts dikteringen och flaggan släcks, så att ett svar som
     * kommer efteråt inte skriver in texten en gång till.
     */
    private boolean accepting;
    /** Sista tolkningen som visats som preliminär text, så inget tappas. */
    private String pending = "";
    /** Falskt i lösenordsfält och liknande, där diktering inte hör hemma. */
    private boolean dictation = true;

    /** Hur länge ett uteblivet svar från taltjänsten väntas ut. */
    private static final long NO_ANSWER = 8000L;
    /**
     * Taltjänsten svarade inte. Den preliminära texten blir riktig text, så
     * att inget som sagts kan kastas av nästa programbyte.
     */
    private final Runnable giveUp = new Runnable() {
        @Override public void run() {
            if (!accepting && pending.length() == 0) return;
            accepting = false;
            listening = false;
            // Läses före settle: settle tömmer pending.
            boolean heard = pending.length() > 0;
            settle(true);
            paintMic();
            Mic.release(VoiceKeys.this);
            say(Notices.afterSilence(heard, Notices.TALK));
        }
    };

    // ------------------------------------------------------------- tangenterna

    @Override public View onCreateInputView() {
        board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setBackgroundColor(BOARD);
        board.setPadding(dp(3), dp(6), dp(3), dp(8));

        LinearLayout said = new LinearLayout(this);
        said.setOrientation(LinearLayout.HORIZONTAL);
        said.setGravity(Gravity.CENTER);

        spin = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        spin.setIndeterminate(true);
        spin.setVisibility(View.GONE);
        LinearLayout.LayoutParams spinSlot =
                new LinearLayout.LayoutParams(dp(16), dp(16));
        spinSlot.rightMargin = dp(6);
        said.addView(spin, spinSlot);

        says = new TextView(this);
        says.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        says.setTextColor(DIM);
        says.setGravity(Gravity.CENTER);
        says.setMaxLines(2);
        says.setEllipsize(android.text.TextUtils.TruncateAt.END);
        says.setPadding(dp(10), dp(2), dp(10), dp(4));
        // Punkt 13: beskedet ska läsas upp när det byts, inte bara stå där.
        says.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        said.addView(says, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        board.addView(said, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        glyphs.clear();
        for (int i = 0; i < LETTERS.length; i++) {
            LinearLayout line = row();
            if (i == 2) line.addView(shiftKey = tool("⇧", "Skift", 1.5f, new Runnable() {
                @Override public void run() { shiftTapped(); }
            }));
            String keys = LETTERS[i];
            for (int k = 0; k < keys.length(); k++) {
                Button key = glyph(keys.charAt(k));
                glyphs.add(key);
                line.addView(key);
            }
            if (i == 2) line.addView(backspace(1.5f));
            board.addView(line);
        }

        LinearLayout tools = row();
        tools.addView(layerKey = tool("123", "Siffror och tecken", 1.4f, new Runnable() {
            @Override public void run() { symbols = !symbols; relabel(); }
        }));
        tools.addView(tool("⌨", "Tillbaka till föregående tangentbord", 1.2f, new Runnable() {
            @Override public void run() { back(); }
        }));
        tools.addView(tool(",", "Komma", 1f, new Runnable() {
            @Override public void run() { type(","); }
        }));
        tools.addView(tool("mellanslag", "Mellanslag", 3.6f, new Runnable() {
            @Override public void run() { type(" "); }
        }));
        tools.addView(tool(".", "Punkt", 1f, new Runnable() {
            @Override public void run() { type("."); }
        }));
        tools.addView(tool("⏎", "Retur", 1.4f, new Runnable() {
            @Override public void run() { enter(); }
        }));
        board.addView(tools);

        LinearLayout talkRow = row();
        micKey = new Button(this);
        micKey.setAllCaps(false);
        micKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        micKey.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { micTapped(); }
        });
        LinearLayout.LayoutParams wide = new LinearLayout.LayoutParams(0, tall(52), 1f);
        wide.setMargins(dp(3), dp(5), dp(3), dp(2));
        talkRow.addView(micKey, wide);
        board.addView(talkRow);

        relabel();
        paintMic();
        return board;
    }

    private LinearLayout row() {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return line;
    }

    /** En bokstavstangent. Den skriver det som står på den, så skift och
     *  sifferläget behöver bara byta text. */
    private Button glyph(char base) {
        final Button key = new Button(this);
        key.setAllCaps(false);
        key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        key.setTextColor(INK);
        key.setPadding(0, 0, 0, 0);
        key.setBackground(skin(KEY, KEY_DOWN));
        key.setTag(String.valueOf(base));
        key.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                type(key.getText().toString());
                if (shift && !shiftLock) { shift = false; relabel(); }
            }
        });
        key.setLayoutParams(weight(1f, 46));
        return key;
    }

    /**
     * En funktionstangent: mörkare, dämpad text, egen bredd.
     *
     * @param face   det som står på tangenten, ofta en glyf
     * @param spoken det TalkBack läser upp. En glyf som ⇧ eller ⏎ säger
     *               ingenting till den som inte ser den, så varje
     *               funktionstangent bär sitt ord. Samma ord som
     *               iOS-tangentbordet använder.
     */
    private Button tool(String face, String spoken, float weight, final Runnable tap) {
        Button key = new Button(this);
        key.setAllCaps(false);
        key.setText(face);
        key.setContentDescription(spoken);
        key.setTextSize(TypedValue.COMPLEX_UNIT_SP, face.length() > 2 ? 14 : 18);
        key.setTextColor(DIM);
        key.setPadding(0, 0, 0, 0);
        key.setBackground(skin(TOOL, TOOL_DOWN));
        key.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { tap.run(); }
        });
        key.setLayoutParams(weight(weight, 46));
        return key;
    }

    /** Backsteget raderar ett tecken, och fortsätter medan fingret ligger kvar. */
    private Button backspace(float weight) {
        Button key = new Button(this);
        key.setAllCaps(false);
        key.setText("⌫");
        key.setContentDescription("Backsteg");
        key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        key.setTextColor(DIM);
        key.setPadding(0, 0, 0, 0);
        key.setBackground(skin(TOOL, TOOL_DOWN));
        key.setLayoutParams(weight(weight, 46));
        final Runnable[] again = new Runnable[1];
        // Sant så snart repetitionen har hunnit radera minst ett tecken.
        final boolean[] repeating = new boolean[1];
        again[0] = new Runnable() {
            @Override public void run() {
                repeating[0] = true;
                erase();
                hand.postDelayed(again[0], 55);
            }
        };
        // Ett tecken ligger i klicklyssnaren, repetitionen i touch-lyssnaren.
        // TalkBack skickar ACTION_CLICK och rör aldrig touch-lyssnaren, så utan
        // den här gick raderingen inte att nå med skärmläsare — knappen
        // annonserades men gjorde ingenting.
        key.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { erase(); }
        });
        key.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        repeating[0] = false;
                        // Första tecknet går genom klicklyssnaren, som ACTION_UP
                        // utlöser. Här startas bara repetitionen.
                        hand.postDelayed(again[0], 400);
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        hand.removeCallbacks(again[0]);
                        // Hann repetitionen igång har den redan raderat. Då ska
                        // släppet inte radera ett tecken till.
                        if (!repeating[0]) v.performClick();
                        repeating[0] = false;
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        hand.removeCallbacks(again[0]);
                        repeating[0] = false;
                        return true;
                    default:
                        return true;
                }
            }
        });
        return key;
    }

    private LinearLayout.LayoutParams weight(float weight, int height) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, tall(height), weight);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    /**
     * Tangenthöjd som följer systemets textstorlek.
     *
     * Höjden var fast i dp medan etiketten står i sp, så vid stor
     * systemtext växte bokstaven ur tangenten. Taket på 1,5 finns för att
     * tangentbordet inte ska ta halva skärmen; iOS-sidan har samma tak.
     */
    private int tall(int height) {
        float scale = getResources().getConfiguration().fontScale;
        float k = Math.min(Math.max(scale, 1f), 1.5f);
        return Math.round(dp(height) * k);
    }

    private StateListDrawable skin(int rest, int down) {
        return skin(rest, down, KEY_EDGE);
    }

    /**
     * Tangentens yta i två lägen, med en linje runt.
     *
     * Linjen är inte dekor. Tangentens yta har 1,27:1 mot tangentbordets
     * botten, och utan en kant går tangenterna inte att skilja från den eller
     * från varandra. 0xFF707070 ger 3,13:1 mot ytan. Grön tangent får ingen
     * linje: där bär ytan gränsen själv med 12,72:1.
     */
    private StateListDrawable skin(int rest, int down, int stroke) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, box(down, stroke));
        states.addState(new int[]{}, box(rest, stroke));
        return states;
    }

    private GradientDrawable box(int fill, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(fill);
        shape.setCornerRadius(dp(7));
        if (stroke != 0) shape.setStroke(Math.max(1, dp(1)), stroke);
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Tangenternas text följer skift och sifferläge. */
    private void relabel() {
        int index = 0;
        for (int i = 0; i < LETTERS.length; i++) {
            String letters = LETTERS[i];
            String signs = SYMBOLS[i];
            for (int k = 0; k < letters.length(); k++, index++) {
                if (index >= glyphs.size()) return;
                String face;
                if (symbols) {
                    face = k < signs.length() ? String.valueOf(signs.charAt(k)) : "";
                } else {
                    String low = String.valueOf(letters.charAt(k));
                    face = (shift || shiftLock) ? low.toUpperCase(new Locale("sv", "SE")) : low;
                }
                Button key = glyphs.get(index);
                key.setText(face);
                key.setEnabled(face.length() > 0);
                key.setTextColor(face.length() > 0 ? INK : TOOL);
            }
        }
        if (shiftKey != null) {
            shiftKey.setText(shiftLock ? "⇪" : "⇧");
            shiftKey.setContentDescription(shiftLock ? "Skift låst" : "Skift");
            shiftKey.setTextColor(shift || shiftLock ? GREEN : DIM);
        }
        if (layerKey != null) {
            layerKey.setText(symbols ? "abc" : "123");
            layerKey.setContentDescription(symbols ? "Bokstäver" : "Siffror och tecken");
        }
    }

    private void shiftTapped() {
        long now = System.currentTimeMillis();
        if (now - shiftTapped < 400) {
            shiftLock = !shiftLock;
            shift = false;
        } else {
            if (shiftLock) { shiftLock = false; shift = false; }
            else shift = !shift;
        }
        shiftTapped = now;
        relabel();
    }

    // ------------------------------------------------------------- inmatningen

    /** Tangenttryck skrivs rakt in i fältet. Inget mellanlagras, inget loggas. */
    private void type(String text) {
        InputConnection field = getCurrentInputConnection();
        if (field == null) return;
        abandon();
        field.commitText(text, 1);
    }

    private void erase() {
        InputConnection field = getCurrentInputConnection();
        if (field == null) return;
        // Preliminär text blir riktig text först, så backsteget alltid tar
        // bort ett tecken och aldrig hela meningen på en gång.
        abandon();
        if (field.getSelectedText(0) != null) field.commitText("", 1);
        else field.deleteSurroundingText(1, 0);
    }

    private void enter() {
        InputConnection field = getCurrentInputConnection();
        if (field == null) return;
        abandon();
        EditorInfo info = getCurrentInputEditorInfo();
        int action = info == null ? EditorInfo.IME_ACTION_NONE
                : (info.imeOptions & EditorInfo.IME_MASK_ACTION);
        boolean blocked = info != null && (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        if (!blocked && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            field.performEditorAction(action);
        } else {
            field.commitText("\n", 1);
        }
    }

    /** Tillbaka till tangentbordet man kom ifrån. */
    private void back() {
        abandon();
        if (!switchToPreviousInputMethod()) {
            android.view.inputmethod.InputMethodManager pick =
                    getSystemService(android.view.inputmethod.InputMethodManager.class);
            if (pick != null) pick.showInputMethodPicker();
        }
    }

    // ------------------------------------------------------------- dikteringen

    private void micTapped() {
        if (listening) { stopListening(); return; }
        if (!dictation) {
            say("Diktering är avstängd i det här fältet. Rätta med tangenterna, "
                    + "eller byt till ett vanligt textfält för att diktera.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            say("Ett tangentbord får inte fråga om mikrofonen själv. Appen "
                    + "öppnas nu: tillåt mikrofonen där och kom tillbaka hit.");
            Intent open = new Intent(this, Home.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(open);
            return;
        }
        if (!Speech.available(this)) {
            say(Notices.NO_SERVICE);
            return;
        }
        startListening();
    }

    private void startListening() {
        if (listening) return;
        // Ett svar från förra dikteringen kan vara på väg. Släpp det först,
        // annars skrivs den gamla meningen in mitt i den nya.
        if (accepting) abandon();
        // Bubblan eller hemskärmen kan hålla mikrofonen. Hellre ett besked
        // om det än ett fel från taltjänsten.
        if (!Mic.take(this)) {
            say(Notices.BUSY_ELSEWHERE);
            return;
        }
        if (speech == null) {
            try {
                speech = Speech.open(this, picker);
            } catch (RuntimeException refused) {
                // Ingen av de två tjänsterna gick att skapa. Spärren togs
                // två rader upp och måste tillbaka.
                speech = null;
                Mic.release(this);
                say(Notices.failure(SpeechRecognizer.ERROR_CLIENT, Notices.TALK));
                return;
            }
            speech.setRecognitionListener(new Ears());
        }
        pending = "";
        accepting = true;
        Intent ask = Speech.request(getPackageName());
        listening = true;
        say(Notices.LISTENING);
        paintMic();
        try {
            speech.startListening(ask);
        } catch (RuntimeException refused) {
            // Nekar taltjänsten starten stod tangentbordet kvar och sa
            // "Lyssnar" utan att lyssna, med mikrofonen tagen.
            listening = false;
            accepting = false;
            Mic.release(this);
            speech.destroy();
            speech = null;
            paintMic();
            say(Notices.failure(SpeechRecognizer.ERROR_CLIENT, Notices.TALK));
        }
    }

    /**
     * Gör om omgången på den vanliga taltjänsten.
     *
     * Mikrofonspärren hålls kvar hela vägen: släpps den här kan bubblan eller
     * appen ta den mellan de två försöken, och användaren skulle få veta att
     * mikrofonen används någon annanstans mitt i sin egen diktering.
     *
     * @return sant när det nya försöket startade. Falskt betyder att
     *         {@code onError} går vidare som om inget byte fanns, och då är
     *         det grenen där som släpper spärren och lämnar ett besked.
     */
    private boolean retry() {
        hand.removeCallbacks(giveUp);
        drop();
        try {
            speech = Speech.cloud(this, picker);
            speech.setRecognitionListener(new Ears());
            accepting = true;
            listening = true;
            say(Notices.LISTENING);
            paintMic();
            speech.startListening(Speech.request(getPackageName()));
            return true;
        } catch (RuntimeException refused) {
            drop();
            return false;
        }
    }

    /** River taltjänsten utan att bry sig om att den redan kan vara riven. */
    private void drop() {
        if (speech == null) return;
        try {
            speech.destroy();
        } catch (RuntimeException gone) {
            // Redan riven. Inget att göra, inget att säga.
        }
        speech = null;
    }

    /** Klar-knappen: mikrofonen stängs, men sista svaret tas emot. */
    private void stopListening() {
        if (!listening) return;
        listening = false;
        say(Notices.FINISHING);
        paintMic();
        if (speech != null) speech.stopListening();
        // Svarar taltjänsten inte alls står den preliminära texten kvar som
        // just preliminär, och ett programbyte kan kasta den. Tidsgränsen
        // gör den till riktig text i stället.
        waitForAnswer();
    }

    /**
     * Håller omgången öppen en stund efter att mikrofonen stängts, och
     * räddar texten om inget svar kommer.
     */
    private void waitForAnswer() {
        hand.removeCallbacks(giveUp);
        hand.postDelayed(giveUp, NO_ANSWER);
    }

    /**
     * Användaren tog över med tangenterna. Taltjänsten avbryts och svaren
     * slutar tas emot, och det som redan hörts blir riktig text på plats.
     */
    private void abandon() {
        if (!accepting && !listening && pending.length() == 0) return;
        accepting = false;
        listening = false;
        picker.roundOver();
        hand.removeCallbacks(giveUp);
        Mic.release(this);
        if (speech != null) speech.cancel();
        settle(false);
        say(Notices.ABANDONED);
        paintMic();
    }

    /** Stänger mikrofonen helt och river taltjänsten. */
    private void closeMicrophone() {
        accepting = false;
        listening = false;
        picker.roundOver();
        hand.removeCallbacks(giveUp);
        Mic.release(this);
        settle(false);
        if (speech != null) {
            speech.cancel();
            speech.destroy();
            speech = null;
        }
        paintMic();
    }

    private void paintMic() {
        if (spin != null) {
            spin.setVisibility(accepting && !listening ? View.VISIBLE : View.GONE);
        }
        if (micKey == null) return;
        boolean on = listening;
        micKey.setText(on ? "Klar" : (dictation ? "Tala" : "Diktering av"));
        // Stod "Tala, diktera in i fältet" också när tangenten sa "Diktering
        // av" i ett lösenordsfält, alltså en etikett som inte gick att följa.
        if (on) {
            micKey.setContentDescription("Klar, stäng mikrofonen");
        } else if (dictation) {
            micKey.setContentDescription("Tala, diktera in i fältet");
        } else {
            micKey.setContentDescription(
                    "Diktering av. Det går inte att diktera i det här fältet.");
        }
        micKey.setBackground(skin(dictation ? (on ? GREEN_DEEP : GREEN) : TOOL,
                dictation ? GREEN_DEEP : TOOL_DOWN,
                dictation ? 0 : KEY_EDGE));
        micKey.setTextColor(dictation ? DARK_INK : DIM);
    }

    /** Preliminär text blir riktig text, en gång. */
    private void settle(boolean space) {
        InputConnection field = getCurrentInputConnection();
        if (field == null) { pending = ""; return; }
        if (pending.length() > 0) {
            String text = pending;
            pending = "";
            field.commitText(space ? text + " " : text, 1);
        } else {
            field.finishComposingText();
        }
    }

    /**
     * Texten skrivs i fältet medan man talar: varje delresultat läggs som
     * preliminär text, som ersätts av nästa. Den sista tolkningen görs om
     * till vanlig text med ett mellanslag efter.
     */
    private final class Ears implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) { }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rms) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onEndOfSpeech() {
            if (!accepting) return;
            listening = false;
            paintMic();
            // Talet är slut, tolkningen inte. Omgången hålls öppen tills
            // svaret kommer, eller tills tiden går ut.
            waitForAnswer();
        }
        @Override public void onPartialResults(Bundle partial) {
            if (!accepting) return;
            String heard = first(partial);
            if (heard == null) return;
            InputConnection field = getCurrentInputConnection();
            if (field == null) return;
            pending = heard;
            field.setComposingText(heard, 1);
        }
        @Override public void onResults(Bundle results) {
            if (!accepting) return;
            accepting = false;
            listening = false;
            picker.roundOver();
            hand.removeCallbacks(giveUp);
            Mic.release(VoiceKeys.this);
            String heard = first(results);
            InputConnection field = getCurrentInputConnection();
            boolean something = heard != null && heard.length() > 0;
            if (field != null && something) {
                pending = "";
                field.commitText(heard + " ", 1);
            } else {
                // Inget sista svar: det som hörts under vägen står kvar.
                something = pending.length() > 0;
                settle(true);
            }
            say(something ? Notices.DONE_KEYBOARD
                    : Notices.failure(SpeechRecognizer.ERROR_NO_MATCH, Notices.TALK));
            paintMic();
        }
        @Override public void onError(int code) {
            if (!accepting) return;
            // Enhetstolkningen kan vara oduglig för svenska på just den
            // här telefonen. Då görs omgången om på den vanliga
            // taltjänsten, en gång, utan att släppa mikrofonen och utan
            // att kasta den preliminära texten.
            if (picker.swapToCloud(code, pending.length() > 0) && retry()) return;
            accepting = false;
            listening = false;
            picker.roundOver();
            hand.removeCallbacks(giveUp);
            Mic.release(VoiceKeys.this);
            // Det som redan hörts behålls, så ett avbrott aldrig äter text.
            // Flaggan läses före settle, som tömmer pending.
            boolean heard = pending.length() > 0;
            settle(true);
            paintMic();
            // Alltid ett besked. Förr returnerade reason() null vid de två
            // vanligaste felen, och då stod tangentbordet helt tyst.
            say(Notices.afterFailure(code, heard, Notices.TALK));
        }
        @Override public void onEvent(int type, Bundle params) { }
    }

    private String first(Bundle results) {
        if (results == null) return null;
        ArrayList<String> words = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return words == null || words.isEmpty() ? null : words.get(0);
    }

    /**
     * Beskedet hamnar i statusraden ovanför tangenterna, inte i en toast.
     * En toast hinner inte läsas, går inte att läsa om, och tangentbordet
     * ligger dessutom ofta över den plats där den visas.
     */
    private void say(String text) {
        if (says == null) return;
        // Bara vid ändring: statusraden sätts om vid varje läge, och en live
        // region läser upp varje skrivning.
        if (!text.contentEquals(says.getText())) says.setText(text);
    }

    // ------------------------------------------------------------ tjänstens liv

    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        dictation = ordinaryText(info);
        shift = false;
        shiftLock = false;
        symbols = false;
        // Fältet startas om. Förr nollades flaggorna bara, och då kunde
        // mikrofonen ligga kvar öppen med en preliminär text som ingen
        // längre tog emot — alltså tappad text och en mikrofon som inte
        // gick att stänga. Nu stängs dikteringen på riktigt, och det som
        // hörts blir riktig text först.
        closeMicrophone();
        say("");
        relabel();
        paintMic();
    }

    /** Lösenordsfält och liknande ska varken dikteras i eller tolkas av en
     *  taltjänst som kan nå nätet. */
    private boolean ordinaryText(EditorInfo info) {
        if (info == null) return false;
        int klass = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        if (klass == InputType.TYPE_CLASS_NUMBER || klass == InputType.TYPE_CLASS_PHONE) return false;
        if (klass != InputType.TYPE_CLASS_TEXT) return false;
        return variation != InputType.TYPE_TEXT_VARIATION_PASSWORD
                && variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                && variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
    }

    @Override public void onFinishInputView(boolean finishing) {
        closeMicrophone();
        super.onFinishInputView(finishing);
    }

    @Override public void onFinishInput() {
        closeMicrophone();
        super.onFinishInput();
    }

    @Override public void onDestroy() {
        hand.removeCallbacksAndMessages(null);
        closeMicrophone();
        super.onDestroy();
    }

    /** Fältet man skriver i ska synas, även liggande. */
    @Override public boolean onEvaluateFullscreenMode() {
        return false;
    }
}
