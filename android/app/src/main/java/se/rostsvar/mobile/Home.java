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

    /** Svart bakgrund och grön accent, samma visuella linje som iOS-appen. */
    private static final int BG = 0xFF000000;
    private static final int INK = 0xFFF2F4F3;
    private static final int DIM = 0xFF9AA3A0;
    private static final int GREEN = 0xFF3FE08A;
    private static final int GREEN_DEEP = 0xFF16A35A;
    private static final int FIELD = 0xFF111315;
    private static final int BORDER = 0xFF2A2F31;
    private static final int OFF = 0xFF3A3F42;

    private static final int MIC_PERMISSION = 1;
    /** Fast id, så att texten överlever en vridning av telefonen. */
    private static final int DRAFT_ID = 0x7EAD0001;

    private TextView status;
    private EditText draft;
    private Button mic;
    private Button send;

    private SpeechRecognizer speech;
    private boolean listening;
    /** Texten som redan stod i fältet när dikteringen startade. */
    private String base = "";
    /** Sant när mikrofonen är avslagen för appen i systeminställningarna. */
    private boolean micBlocked;
    private String note = "Tryck på Tala och diktera. Du läser texten och väljer själv var den skickas.";

    // ---------------------------------------------------------------- skärmen

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        page.setPadding(dp(20), dp(28), dp(20), dp(24));
        page.setFitsSystemWindows(true);

        TextView title = new TextView(this);
        title.setText("Tap & Talk");
        title.setTextColor(INK);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        page.addView(title, row(0, 0));

        status = new TextView(this);
        status.setTextColor(DIM);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        status.setLineSpacing(0, 1.3f);
        page.addView(status, row(dp(8), 0));

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
    }

    /** Mikrofonen stängs när skärmen lämnas. Inget spelas in och inget sparas. */
    @Override protected void onStop() {
        super.onStop();
        if (listening) {
            listening = false;
            note = "Mikrofonen stängdes när du lämnade appen. Tryck på Tala för att fortsätta.";
        }
        if (speech != null) {
            speech.cancel();
            speech.destroy();
            speech = null;
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (granted()) micBlocked = false;
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
            }
        }
        @Override public void onPartialResults(Bundle partial) { heard(partial, false); }
        @Override public void onResults(Bundle results) {
            heard(results, true);
            listening = false;
            note = "Läs igenom texten, rätta den om du vill, och tryck på Skicka.";
            render();
        }
        @Override public void onError(int code) {
            listening = false;
            note = reason(code);
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
