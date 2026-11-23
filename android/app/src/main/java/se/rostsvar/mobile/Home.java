package se.rostsvar.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Tap &amp; Talk, hela appen på en skärm: diktera svenska, rätta texten för
 * hand och skicka den vidare med Androids delningsruta.
 *
 * Appen skickar aldrig något själv och når inget nät på egen hand. Det enda
 * som lämnar appen är texten du själv väljer att dela, och det tal som
 * telefonens egen taltjänst tolkar (den tjänsten kan använda nätet).
 */
public class Home extends Activity {

    /** Svart bakgrund och grön accent. Färgerna står i Look. */
    private static final int BG = Look.BG;
    private static final int INK = Look.INK;
    private static final int DIM = Look.DIM;
    private static final int GREEN = Look.GREEN;
    private static final int GREEN_DEEP = Look.GREEN_DEEP;
    private static final int FIELD = Look.FIELD;
    private static final int BORDER = Look.BORDER;
    private static final int OFF = Look.OFF;
    private static final int WARN = Look.WARN;

    private static final int MIC_PERMISSION = 1;
    private static final int NOTE_PERMISSION = 2;
    /** Fast id, så att texten överlever en vridning av telefonen. */
    private static final int DRAFT_ID = 0x7EAD0001;

    private TextView status;
    private LinearLayout warning;
    private TextView warnText;
    private Switch bubble;
    private TextView bubbleNote;
    private EditText draft;
    private Button mic;
    private Button send;

    private SpeechRecognizer speech;
    private boolean listening;
    /**
     * Sant från Tala till sista svaret, också efter att mikrofonen stängts.
     * Startas en ny omgång innan den förra svarat skriver den gamla
     * tolkningen in sig mitt i den nya, och taltjänsten svarar dessutom med
     * upptaget. Flaggan håller den andra omgången tillbaka.
     */
    private boolean working;
    private final android.os.Handler hand =
            new android.os.Handler(android.os.Looper.getMainLooper());
    /** Hur länge ett uteblivet svar väntas ut innan knappen släpps fri. */
    private static final long NO_ANSWER = 8000L;
    private final Runnable giveUp = new Runnable() {
        @Override public void run() {
            if (!working) return;
            working = false;
            listening = false;
            Mic.release(Home.this);
            // Texten står redan i fältet: delresultaten skrevs dit medan
            // användaren talade. Det enda som fattas är ett besked.
            note = "Taltjänsten svarade inte. Det som hördes står kvar — tryck på Tala om för att fortsätta.";
            render();
        }
    };
    /** Texten som redan stod i fältet när dikteringen startade. */
    private String base = "";
    /** Sant när mikrofonen är avslagen för appen i systeminställningarna. */
    private boolean micBlocked;
    /** Vad användaren bad strömbrytaren om, senast. */
    private boolean wantBubble;
    private String note = "Tryck på Tala och diktera. Du läser texten och väljer själv var den skickas.";

    // ---------------------------------------------------------------- skärmen

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        page.setPadding(dp(20), dp(28), dp(20), dp(24));
        page.setFitsSystemWindows(true);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Tap & Talk");
        title.setTextColor(INK);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        head.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button guide = new Button(this);
        guide.setAllCaps(false);
        guide.setText("Guide");
        guide.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        guide.setTextColor(DIM);
        guide.setBackground(box(0x00000000, BORDER, dp(12)));
        guide.setContentDescription("Öppna kom igång-guiden");
        guide.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openGuide(); }
        });
        head.addView(guide, new LinearLayout.LayoutParams(dp(92), dp(46)));
        page.addView(head, row(0, 0));

        status = new TextView(this);
        status.setTextColor(DIM);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        status.setLineSpacing(0, 1.3f);
        page.addView(status, row(dp(8), 0));

        // Strömbrytaren för bubblan. Den ligger högst upp eftersom den är
        // appens egentliga funktion: knappen som följer med överallt.
        LinearLayout power = new LinearLayout(this);
        power.setOrientation(LinearLayout.HORIZONTAL);
        power.setGravity(Gravity.CENTER_VERTICAL);
        power.setBackground(box(FIELD, BORDER, dp(14)));
        power.setPadding(dp(14), dp(12), dp(10), dp(12));

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);

        TextView powerTitle = new TextView(this);
        powerTitle.setText("Flytande knapp");
        powerTitle.setTextColor(INK);
        powerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        words.addView(powerTitle, row(0, 0));

        bubbleNote = new TextView(this);
        bubbleNote.setTextColor(DIM);
        bubbleNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        bubbleNote.setLineSpacing(0, 1.25f);
        words.addView(bubbleNote, row(dp(3), 0));

        power.addView(words, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        bubble = new Switch(this);
        bubble.setContentDescription("Starta eller stoppa den flytande knappen");
        bubble.setMinimumWidth(dp(56));
        bubble.setMinimumHeight(dp(48));
        bubble.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bubbleToggled(); }
        });
        power.addView(bubble, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));

        page.addView(power, row(dp(14), 0));

        // Varningen står kvar så länge något steg i guiden fattas, eftersom
        // appen då bara kan diktera här inne och inte i chattarna.
        warning = new LinearLayout(this);
        warning.setOrientation(LinearLayout.VERTICAL);
        warning.setBackground(box(0xFF1A1407, WARN, dp(14)));
        warning.setPadding(dp(14), dp(14), dp(14), dp(14));

        warnText = new TextView(this);
        warnText.setTextColor(WARN);
        warnText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        warnText.setLineSpacing(0, 1.3f);
        warning.addView(warnText, row(0, 0));

        Button fix = new Button(this);
        fix.setAllCaps(false);
        fix.setText("Öppna guiden");
        fix.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        fix.setTextColor(0xFF1A1407);
        fix.setBackground(box(WARN, 0, dp(12)));
        fix.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openGuide(); }
        });
        LinearLayout.LayoutParams fixRow = row(dp(12), 0);
        fixRow.height = dp(48);
        warning.addView(fix, fixRow);

        page.addView(warning, row(dp(14), 0));

        draft = new EditText(this);
        draft.setId(DRAFT_ID);
        draft.setBackground(box(FIELD, BORDER, dp(14)));
        draft.setTextColor(INK);
        draft.setHintTextColor(OFF);
        draft.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        draft.setHint("Texten du dikterar hamnar här. Du kan rätta den för hand.");
        draft.setGravity(Gravity.TOP | Gravity.START);
        draft.setPadding(dp(14), dp(14), dp(14), dp(14));
        draft.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        draft.setContentDescription("Din text");
        draft.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { render(); }
        });
        LinearLayout.LayoutParams field = row(dp(16), dp(16));
        field.height = 0;
        field.weight = 1;
        page.addView(draft, field);

        mic = new Button(this);
        mic.setAllCaps(false);
        mic.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        mic.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { micTapped(); }
        });
        LinearLayout.LayoutParams micRow = row(0, dp(12));
        micRow.height = dp(84);
        page.addView(mic, micRow);

        send = new Button(this);
        send.setAllCaps(false);
        send.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        send.setText("Skicka");
        send.setContentDescription("Skicka texten vidare");
        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { share(); }
        });
        LinearLayout.LayoutParams sendRow = row(0, 0);
        sendRow.height = dp(58);
        page.addView(send, sendRow);

        setContentView(page);

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            note = "Telefonen har ingen taltjänst som kan tolka tal. Installera eller aktivera en rösttjänst med svenska.";
        }
        render();

        // Första starten går direkt till guiden. Den visas en gång av sig
        // själv; sedan når man den med Guide-knappen.
        boolean seen = getSharedPreferences(Guide.SETTINGS, MODE_PRIVATE)
                .getBoolean(Guide.SEEN, false);
        if (!seen) openGuide();
    }

    private void openGuide() {
        startActivity(new Intent(this, Guide.class));
    }

    // ------------------------------------------------------------- bubblan

    /**
     * Strömbrytaren. Switch:en har redan vänt sig själv när vi kommer hit,
     * så dess läge är vad användaren bad om.
     */
    private void bubbleToggled() {
        if (!bubble.isChecked()) {
            Bubble.stop(this);
            wantBubble = false;
            note = "Bubblan är stängd. Slå på den igen när du vill ha den framme.";
            render();
            return;
        }
        if (!granted()) {
            wantBubble = false;
            note = "Bubblan behöver mikrofonen. Välj Tillåt i rutan som visas.";
            render();
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
            return;
        }
        if (!Bubble.mayShow(this)) {
            wantBubble = false;
            note = "Bubblan får inte visas över andra appar än. Guiden visar var det slås på.";
            render();
            openGuide();
            return;
        }
        // Notisen är Androids krav på en förgrundstjänst. Nekas den körs
        // bubblan ändå, men då syns den inte i notisfältet.
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTE_PERMISSION);
        }
        Bubble.start(this);
        wantBubble = true;
        note = Writer.enabled(this)
                ? "Bubblan ligger framme. Lämna appen, tryck på den och tala."
                : "Bubblan ligger framme. Slå på tillgängligheten i guiden för att texten "
                + "ska hamna direkt i fältet.";
        render();
    }

    private LinearLayout.LayoutParams row(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = top;
        params.bottomMargin = bottom;
        return params;
    }

    private GradientDrawable box(int fill, int stroke, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Allt som syns härleds ur läget, så att knapparna aldrig ljuger. */
    private void render() {
        boolean hasText = draft.getText().toString().trim().length() > 0;
        boolean canTalk = SpeechRecognizer.isRecognitionAvailable(this);

        status.setText(note);

        // Varningen namnger det som faktiskt fattas, i stället för att säga
        // samma sak vad det än är.
        String gap = Guide.missing(this);
        warning.setVisibility(gap == null ? View.GONE : View.VISIBLE);
        if (gap != null) {
            warnText.setText(gap + " Du kan diktera här i appen, men ännu inte "
                    + "direkt där du skriver.");
        }

        // Strömbrytaren visar vad användaren bad om, inte vad tjänsten hunnit
        // göra: en tjänst startar och stoppar inte i samma andetag som ett
        // tryck, och knappen ska inte hoppa tillbaka under fingret.
        boolean mayStart = granted() && Bubble.mayShow(this);
        bubble.setChecked(wantBubble);
        bubble.setEnabled(mayStart || wantBubble);
        if (wantBubble) {
            bubbleNote.setText(Writer.enabled(this)
                    ? "Bubblan ligger framme. Tryck på den, tala, och texten hamnar där du skriver."
                    : "Bubblan ligger framme, men får inte skriva i andra appar än. Texten "
                    + "hamnar i urklipp tills du slår på tillgängligheten i guiden.");
        } else {
            bubbleNote.setText(mayStart
                    ? "Lägg en knapp över alla appar. Ett tryck, du talar, texten hamnar i fältet."
                    : "Behöver mikrofon och Visa över andra appar. Öppna guiden.");
        }

        mic.setText(listening ? "Klar" : (micBlocked ? "Tillåt mikrofon" : (hasText ? "Tala om" : "Tala")));
        mic.setContentDescription(listening
                ? "Klar, stäng mikrofonen"
                : (micBlocked ? "Öppna inställningarna och tillåt mikrofonen" : "Tala, starta mikrofonen"));
        mic.setEnabled(canTalk);
        mic.setBackground(box(canTalk ? (listening ? GREEN_DEEP : GREEN) : FIELD, canTalk ? 0 : BORDER, dp(16)));
        mic.setTextColor(canTalk ? 0xFF05140C : OFF);

        send.setEnabled(hasText);
        send.setBackground(box(0x00000000, hasText ? GREEN : BORDER, dp(14)));
        send.setTextColor(hasText ? GREEN : OFF);

        draft.setFocusable(!listening);
        draft.setFocusableInTouchMode(!listening);
        draft.setCursorVisible(!listening);
    }

    // ------------------------------------------------------- tillstånd och tal

    private void micTapped() {
        if (listening) { stopListening(); return; }
        if (working) {
            note = "Texten tolkas. Vänta ett ögonblick.";
            render();
            return;
        }
        if (micBlocked && !granted()) { openSettings(); return; }
        if (!granted()) {
            note = "Mikrofonen behövs för att diktera. Välj Tillåt i rutan som visas.";
            render();
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
            return;
        }
        startListening();
    }

    private boolean granted() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void openSettings() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName())));
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] result) {
        super.onRequestPermissionsResult(code, perms, result);
        if (code == NOTE_PERMISSION) return;
        if (code != MIC_PERMISSION) return;
        if (granted()) {
            micBlocked = false;
            startListening();
            return;
        }
        // Visas rutan inte längre är mikrofonen avslagen för appen, och då är
        // systeminställningarna enda vägen vidare.
        micBlocked = !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO);
        note = micBlocked
                ? "Mikrofonen är avstängd för Tap & Talk. Tryck på Tillåt mikrofon för att öppna inställningarna."
                : "Utan mikrofon kan appen inte diktera. Tryck på Tala igen och välj Tillåt.";
        render();
    }

    private void startListening() {
        if (listening) return;
        // Bubblan eller rösttangentbordet kan hålla mikrofonen. Hellre ett
        // besked om var den är än ett upptaget-fel från taltjänsten.
        if (!Mic.take(this)) {
            note = "Mikrofonen används av Tap & Talk någon annanstans. Avsluta dikteringen där först.";
            render();
            return;
        }
        if (speech == null) {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) { render(); return; }
            speech = SpeechRecognizer.createSpeechRecognizer(this);
            speech.setRecognitionListener(new Ears());
        }
        base = draft.getText().toString().trim();
        if (base.length() > 0) base = base + " ";

        Intent ask = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE");
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "sv-SE");
        ask.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        ask.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        ask.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        listening = true;
        working = true;
        note = "Mikrofonen är på. Tala svenska och tryck på Klar när du är färdig.";
        render();
        speech.startListening(ask);
    }

    private void stopListening() {
        if (!listening) return;
        listening = false;
        note = "Tolkar det du sa …";
        render();
        if (speech != null) speech.stopListening();
        hand.removeCallbacks(giveUp);
        hand.postDelayed(giveUp, NO_ANSWER);
    }

    /** Omgången är över: tidsgränsen avbokad, mikrofonen lämnad tillbaka. */
    private void closeRound() {
        working = false;
        listening = false;
        hand.removeCallbacks(giveUp);
        Mic.release(this);
    }

    /** Mikrofonen stängs när skärmen lämnas. Inget spelas in och inget sparas. */
    @Override protected void onStop() {
        super.onStop();
        if (listening) {
            note = "Mikrofonen stängdes när du lämnade appen. Tryck på Tala för att fortsätta.";
        }
        // Omgången är över vad som än pågick, och mikrofonen ska inte ligga
        // tagen för de andra ytorna när skärmen inte finns.
        closeRound();
        if (speech != null) {
            speech.cancel();
            speech.destroy();
            speech = null;
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (granted()) micBlocked = false;
        // Bubblan kan ha stängts från sin egen notis medan appen låg undan.
        wantBubble = Bubble.running();
        render();
    }

    private void heard(Bundle results, boolean done) {
        if (results == null) return;
        java.util.ArrayList<String> words = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (words == null || words.isEmpty()) return;
        String text = base + words.get(0);
        draft.setText(text);
        draft.setSelection(text.length());
        if (done) base = text.trim().length() > 0 ? text.trim() + " " : "";
    }

    /** Lyssnaren fyller fältet löpande och lämnar sista tolkningen som text. */
    private final class Ears implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) {
            note = "Tala nu. Tryck på Klar när du är färdig.";
            render();
        }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rms) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onEndOfSpeech() {
            if (listening) {
                listening = false;
                note = "Tolkar det du sa …";
                render();
                // Talet är slut, tolkningen inte. Omgången hålls öppen tills
                // svaret kommer, eller tills tiden går ut.
                hand.removeCallbacks(giveUp);
                hand.postDelayed(giveUp, NO_ANSWER);
            }
        }
        @Override public void onPartialResults(Bundle partial) {
            if (!working) return;
            heard(partial, false);
        }
        @Override public void onResults(Bundle results) {
            if (!working) return;
            heard(results, true);
            closeRound();
            note = "Läs igenom texten, rätta den om du vill, och tryck på Skicka.";
            render();
        }
        @Override public void onError(int code) {
            if (!working) return;
            closeRound();
            // Hann något höras står det kvar i fältet, och då är det inte
            // sant att inget uppfattades.
            boolean something = draft.getText().toString().trim().length() > base.trim().length();
            note = something
                    ? "Taltjänsten avslutade med ett fel, men det här hördes. Läs igenom texten."
                    : reason(code);
            render();
        }
        @Override public void onEvent(int type, Bundle params) { }
    }

    private String reason(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_NO_MATCH:
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Hörde inget. Tryck på Tala och prova igen.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                micBlocked = !granted();
                return "Taltjänsten fick inte använda mikrofonen. Tillåt mikrofon och prova igen.";
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Taltjänsten nådde inte nätet. Många telefoner klarar svenska utan nät när språket är hämtat.";
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                return "Taltjänsten har inte svenska installerad. Hämta svenska i telefonens röstinställningar.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Taltjänsten var upptagen. Tryck på Tala igen.";
            case SpeechRecognizer.ERROR_AUDIO:
                return "Mikrofonen gick inte att läsa. Stäng andra appar som spelar in och prova igen.";
            default:
                return "Taltjänsten svarade med ett fel (" + code + "). Tryck på Tala för att prova igen.";
        }
    }

    // ------------------------------------------------------------- delningen

    /** Skicka-knappen lämnar texten till Androids delningsruta, inget annat. */
    private void share() {
        String text = draft.getText().toString().trim();
        if (text.length() == 0) return;
        Intent out = new Intent(Intent.ACTION_SEND);
        out.setType("text/plain");
        out.putExtra(Intent.EXTRA_TEXT, text);
        out.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivity(Intent.createChooser(out, "Skicka texten"));
    }
}
