package se.rostsvar.mobile;

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
import android.speech.RecognizerIntent;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tap &amp; Talks rösttangentbord: diktera svenska rakt ned i fältet man står
 * i, och vanliga tangenter för att rätta det som blev fel.
 *
 * Det som skrivs eller dikteras loggas aldrig och lämnar aldrig tangentbordet
 * annat än till fältet användaren själv står i. Tjänsten når inget nät; det
 * gör bara telefonens egen taltjänst.
 */
public class VoiceKeys extends InputMethodService {

    /** Samma svarta yta och gröna accent som appens skärm. */
    private static final int BOARD = 0xFF000000;
    private static final int KEY = 0xFF1B1F21;
    private static final int KEY_DOWN = 0xFF2E3437;
    private static final int TOOL = 0xFF101315;
    private static final int TOOL_DOWN = 0xFF1E2325;
    private static final int INK = 0xFFF2F4F3;
    private static final int DIM = 0xFF9AA3A0;
    private static final int GREEN = 0xFF3FE08A;
    private static final int GREEN_DEEP = 0xFF16A35A;
    private static final int DARK_INK = 0xFF05140C;

    /** Svensk layout. Tre rader bokstäver, med å ä ö på sina vanliga platser. */
    private static final String[] LETTERS = {"qwertyuiopå", "asdfghjklöä", "zxcvbnm"};
    /** Samma platser, men siffror och skiljetecken. */
    private static final String[] SYMBOLS = {"1234567890+", "@#%&-_()/:;", "*\"'!?€£"};

    private final Handler hand = new Handler(Looper.getMainLooper());
    /** Tangenterna som byter min, i samma ordning som raderna ovan. */
    private final List<Button> glyphs = new ArrayList<Button>();

    private LinearLayout board;
    private Button micKey;
    private Button shiftKey;
    private Button layerKey;

    private boolean shift;
    private boolean shiftLock;
    private long shiftTapped;
    private boolean symbols;

    private SpeechRecognizer speech;
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

    // ------------------------------------------------------------- tangenterna

    @Override public View onCreateInputView() {
        board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setBackgroundColor(BOARD);
        board.setPadding(dp(3), dp(6), dp(3), dp(8));

        glyphs.clear();
        for (int i = 0; i < LETTERS.length; i++) {
            LinearLayout line = row();
            if (i == 2) line.addView(shiftKey = tool("⇧", 1.5f, new Runnable() {
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
        tools.addView(layerKey = tool("123", 1.4f, new Runnable() {
            @Override public void run() { symbols = !symbols; relabel(); }
        }));
        tools.addView(tool("⌨", 1.2f, new Runnable() {
            @Override public void run() { back(); }
        }));
        tools.addView(tool(",", 1f, new Runnable() {
            @Override public void run() { type(","); }
        }));
        tools.addView(tool("mellanslag", 3.6f, new Runnable() {
            @Override public void run() { type(" "); }
        }));
        tools.addView(tool(".", 1f, new Runnable() {
            @Override public void run() { type("."); }
        }));
        tools.addView(tool("⏎", 1.4f, new Runnable() {
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
        LinearLayout.LayoutParams wide = new LinearLayout.LayoutParams(0, dp(52), 1f);
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

    /** En funktionstangent: mörkare, dämpad text, egen bredd. */
    private Button tool(String label, float weight, final Runnable tap) {
        Button key = new Button(this);
        key.setAllCaps(false);
        key.setText(label);
        key.setTextSize(TypedValue.COMPLEX_UNIT_SP, label.length() > 2 ? 14 : 18);
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
        key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        key.setTextColor(DIM);
        key.setPadding(0, 0, 0, 0);
        key.setBackground(skin(TOOL, TOOL_DOWN));
        key.setLayoutParams(weight(weight, 46));
        final Runnable[] again = new Runnable[1];
        again[0] = new Runnable() {
            @Override public void run() { erase(); hand.postDelayed(again[0], 55); }
        };
        key.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        erase();
                        hand.postDelayed(again[0], 400);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        hand.removeCallbacks(again[0]);
                        return true;
                    default:
                        return true;
                }
            }
        });
        return key;
    }

    private LinearLayout.LayoutParams weight(float weight, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(height), weight);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    private StateListDrawable skin(int rest, int down) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, box(down));
        states.addState(new int[]{}, box(rest));
        return states;
    }

    private GradientDrawable box(int fill) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(fill);
        shape.setCornerRadius(dp(7));
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
            shiftKey.setTextColor(shift || shiftLock ? GREEN : DIM);
        }
        if (layerKey != null) layerKey.setText(symbols ? "abc" : "123");
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
            say("Diktering är avstängd i det här fältet.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            say("Tillåt mikrofonen i Tap & Talk först.");
            Intent open = new Intent(this, Home.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(open);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say("Telefonen har ingen taltjänst som kan tolka tal.");
            return;
        }
        startListening();
    }

    private void startListening() {
        if (listening) return;
        // Ett svar från förra dikteringen kan vara på väg. Släpp det först,
        // annars skrivs den gamla meningen in mitt i den nya.
        if (accepting) abandon();
        if (speech == null) {
            speech = SpeechRecognizer.createSpeechRecognizer(this);
            speech.setRecognitionListener(new Ears());
        }
        pending = "";
        accepting = true;
        Intent ask = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE");
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "sv-SE");
        ask.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        ask.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        ask.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        listening = true;
        paintMic();
        speech.startListening(ask);
    }

    /** Klar-knappen: mikrofonen stängs, men sista svaret tas emot. */
    private void stopListening() {
        if (!listening) return;
        listening = false;
        paintMic();
        if (speech != null) speech.stopListening();
    }

    /**
     * Användaren tog över med tangenterna. Taltjänsten avbryts och svaren
     * slutar tas emot, och det som redan hörts blir riktig text på plats.
     */
    private void abandon() {
        if (!accepting && !listening && pending.length() == 0) return;
        accepting = false;
        listening = false;
        if (speech != null) speech.cancel();
        settle(false);
        paintMic();
    }

    /** Stänger mikrofonen helt och river taltjänsten. */
    private void closeMicrophone() {
        accepting = false;
        listening = false;
        settle(false);
        if (speech != null) {
            speech.cancel();
            speech.destroy();
            speech = null;
        }
        paintMic();
    }

    private void paintMic() {
        if (micKey == null) return;
        boolean on = listening;
        micKey.setText(on ? "Klar" : (dictation ? "Tala" : "Diktering av"));
        micKey.setContentDescription(on
                ? "Klar, stäng mikrofonen"
                : "Tala, diktera in i fältet");
        micKey.setBackground(skin(dictation ? (on ? GREEN_DEEP : GREEN) : TOOL,
                dictation ? GREEN_DEEP : TOOL_DOWN));
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
            String heard = first(results);
            InputConnection field = getCurrentInputConnection();
            if (field != null && heard != null && heard.length() > 0) {
                pending = "";
                field.commitText(heard + " ", 1);
            } else {
                // Inget sista svar: det som hörts under vägen står kvar.
                settle(true);
            }
            paintMic();
        }
        @Override public void onError(int code) {
            if (!accepting) return;
            accepting = false;
            listening = false;
            // Det som redan hörts behålls, så ett avbrott aldrig äter text.
            settle(true);
            paintMic();
            String word = reason(code);
            if (word != null) say(word);
        }
        @Override public void onEvent(int type, Bundle params) { }
    }

    private String first(Bundle results) {
        if (results == null) return null;
        ArrayList<String> words = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return words == null || words.isEmpty() ? null : words.get(0);
    }

    /** Null betyder tyst: inget att säga till användaren om. */
    private String reason(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return null;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Tillåt mikrofonen i Tap & Talk först.";
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                return "Taltjänsten har inte svenska installerad.";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Taltjänsten nådde inte nätet.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Taltjänsten var upptagen. Prova igen.";
            default:
                return "Taltjänsten svarade med ett fel (" + code + ").";
        }
    }

    private void say(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    // ------------------------------------------------------------ tjänstens liv

    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        dictation = ordinaryText(info);
        shift = false;
        shiftLock = false;
        symbols = false;
        accepting = false;
        pending = "";
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
