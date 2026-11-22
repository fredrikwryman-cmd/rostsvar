package se.rostsvar.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * Kom igång-guiden: tre steg som gör rösttangentbordet användbart, och ett
 * fält där man provar att det fungerar.
 *
 * Guiden kan inte slå på något åt användaren — Android kräver att man gör det
 * själv i systemets egna rutor. Det guiden gör är att förklara varför, öppna
 * rätt ruta med ett tryck, och själv läsa av när steget är gjort.
 */
public class Guide extends Activity {

    static final String SETTINGS = "tapandtalk";
    static final String SEEN = "guiden_visad";

    private static final int MIC_PERMISSION = 1;
    private static final int TEST_ID = 0x7EAD0002;

    /** Stegen i ordning. Steg 3 är provet, som inte kan bockas av i förväg. */
    private static final int STEP_MIC = 0;
    private static final int STEP_ENABLE = 1;
    private static final int STEP_CHOOSE = 2;
    private static final int STEP_TEST = 3;

    private TextView counter;
    private final TextView[] marks = new TextView[3];
    private final TextView[] labels = new TextView[3];
    private TextView heading;
    private TextView body;
    private Button action;
    private TextView aside;
    private LinearLayout proof;
    private EditText test;
    private TextView receipt;
    private Button close;

    private int step = STEP_MIC;
    private boolean micBlocked;
    /** Tangentbordet öppnas en gång när provet visas, sedan får användaren råda. */
    private boolean opened;

    // ----------------------------------------------------------------- läget

    static boolean micOk(Context context) {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Sant när Tap & Talk står i Androids lista över påslagna tangentbord. */
    static boolean enabledOk(Context context) {
        InputMethodManager boards = context.getSystemService(InputMethodManager.class);
        if (boards == null) return false;
        List<InputMethodInfo> list = boards.getEnabledInputMethodList();
        if (list == null) return false;
        for (InputMethodInfo info : list) {
            if (context.getPackageName().equals(info.getPackageName())) return true;
        }
        return false;
    }

    /** Sant när Tap & Talk är det tangentbord som faktiskt används. */
    static boolean chosenOk(Context context) {
        String current = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return current != null && current.startsWith(context.getPackageName() + "/");
    }

    static boolean allDone(Context context) {
        return micOk(context) && enabledOk(context) && chosenOk(context);
    }

    static boolean done(Context context, int which) {
        switch (which) {
            case STEP_MIC: return micOk(context);
            case STEP_ENABLE: return enabledOk(context);
            case STEP_CHOOSE: return chosenOk(context);
            default: return false;
        }
    }

    // ---------------------------------------------------------------- skärmen

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getSharedPreferences(SETTINGS, MODE_PRIVATE).edit().putBoolean(SEEN, true).apply();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Look.BG);
        scroll.setFillViewport(true);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(28), dp(20), dp(24));
        page.setFitsSystemWindows(true);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("Kom igång");
        title.setTextColor(Look.INK);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        page.addView(title, line(0, 0));

        counter = new TextView(this);
        counter.setTextColor(Look.DIM);
        counter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        page.addView(counter, line(dp(4), dp(18)));

        String[] names = {
                "Tillåt mikrofonen",
                "Slå på tangentbordet",
                "Byt till Tap & Talk"
        };
        for (int i = 0; i < 3; i++) {
            LinearLayout rad = new LinearLayout(this);
            rad.setOrientation(LinearLayout.HORIZONTAL);
            rad.setGravity(Gravity.CENTER_VERTICAL);

            marks[i] = new TextView(this);
            marks[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            marks[i].setWidth(dp(28));
            rad.addView(marks[i]);

            labels[i] = new TextView(this);
            labels[i].setText(names[i]);
            labels[i].setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            rad.addView(labels[i]);

            page.addView(rad, line(dp(5), 0));
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Look.box(Look.FIELD, Look.BORDER, dp(16), dp(1)));
        card.setPadding(dp(18), dp(18), dp(18), dp(18));

        heading = new TextView(this);
        heading.setTextColor(Look.INK);
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        card.addView(heading, line(0, 0));

        body = new TextView(this);
        body.setTextColor(Look.DIM);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        body.setLineSpacing(0, 1.35f);
        card.addView(body, line(dp(10), dp(16)));

        action = new Button(this);
        action.setAllCaps(false);
        action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        action.setTextColor(Look.DARK_INK);
        action.setBackground(Look.skin(Look.GREEN, Look.GREEN_DEEP, dp(14)));
        action.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { act(); }
        });
        LinearLayout.LayoutParams tall = line(0, 0);
        tall.height = dp(56);
        card.addView(action, tall);

        aside = new TextView(this);
        aside.setTextColor(Look.OFF);
        aside.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        aside.setLineSpacing(0, 1.3f);
        card.addView(aside, line(dp(12), 0));

        page.addView(card, line(dp(20), 0));

        proof = new LinearLayout(this);
        proof.setOrientation(LinearLayout.VERTICAL);
        proof.setVisibility(View.GONE);

        test = new EditText(this);
        test.setId(TEST_ID);
        test.setBackground(Look.box(Look.FIELD, Look.BORDER, dp(14), dp(1)));
        test.setTextColor(Look.INK);
        test.setHintTextColor(Look.OFF);
        test.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        test.setHint("Tryck här och tryck sedan på Tala");
        test.setGravity(Gravity.TOP | Gravity.START);
        test.setPadding(dp(14), dp(14), dp(14), dp(14));
        test.setMinHeight(dp(96));
        test.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        test.setContentDescription("Provfält");
        test.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { showReceipt(); }
        });
        proof.addView(test, line(dp(16), 0));

        receipt = new TextView(this);
        receipt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        receipt.setLineSpacing(0, 1.3f);
        proof.addView(receipt, line(dp(12), 0));

        page.addView(proof, line(0, 0));

        close = new Button(this);
        close.setAllCaps(false);
        close.setText("Stäng guiden");
        close.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        close.setTextColor(Look.DIM);
        close.setBackground(Look.box(0x00000000, Look.BORDER, dp(14), dp(1)));
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        LinearLayout.LayoutParams last = line(dp(22), 0);
        last.height = dp(52);
        page.addView(close, last);

        setContentView(scroll);
        step = firstUndone();
        render();
    }

    private LinearLayout.LayoutParams line(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = top;
        params.bottomMargin = bottom;
        return params;
    }

    private int dp(int value) { return Look.dp(this, value); }

    private int firstUndone() {
        for (int i = STEP_MIC; i <= STEP_CHOOSE; i++) {
            if (!done(this, i)) return i;
        }
        return STEP_TEST;
    }

    /**
     * Läget läses om varje gång appen kommer fram igen, så ett steg som
     * gjordes i systemets inställningar bockas av utan att användaren
     * behöver trycka vidare.
     */
    @Override protected void onResume() {
        super.onResume();
        if (micOk(this)) micBlocked = false;
        // Guiden går framåt av sig själv, men aldrig bakåt: har man nått
        // provet står man kvar där även om något stängs av under tiden.
        int next = firstUndone();
        if (next > step) step = next;
        render();
    }

    private void render() {
        for (int i = 0; i < 3; i++) {
            boolean klar = done(this, i);
            marks[i].setText(klar ? "✓" : (i == step ? "›" : "○"));
            marks[i].setTextColor(klar ? Look.GREEN : (i == step ? Look.INK : Look.OFF));
            labels[i].setTextColor(klar ? Look.INK : (i == step ? Look.INK : Look.OFF));
        }

        boolean testing = step == STEP_TEST;
        counter.setText(testing ? "Allt är påslaget. Prova att diktera."
                : "Steg " + (step + 1) + " av 3");
        proof.setVisibility(testing ? View.VISIBLE : View.GONE);

        switch (step) {
            case STEP_MIC:
                heading.setText("Tillåt mikrofonen");
                body.setText("Dikteringen behöver mikrofonen. Frågan ställs här i appen, "
                        + "eftersom ett tangentbord inte får fråga själv. Tap & Talk spelar "
                        + "inte in något; ljudet går till telefonens taltjänst, som gör om "
                        + "det till text.");
                action.setText(micBlocked ? "Öppna inställningarna" : "Tillåt mikrofonen");
                aside.setText(micBlocked
                        ? "Android frågar inte igen när man svarat nej två gånger. Slå på "
                        + "Mikrofon under Behörigheter."
                        : "");
                break;
            case STEP_ENABLE:
                heading.setText("Slå på tangentbordet");
                body.setText("Tap & Talk är ett tangentbord. Android kräver att du själv slår "
                        + "på det i listan över tangentbord. Knappen nedan öppnar den listan: "
                        + "leta upp Tap & Talk · rösttangentbord och slå på det.");
                action.setText("Öppna listan över tangentbord");
                aside.setText("Android varnar att ett tangentbord kan se allt du skriver. Det "
                        + "är samma varning för alla tangentbord. Tap & Talk loggar ingenting "
                        + "och skickar ingenting vidare.");
                break;
            case STEP_CHOOSE:
                heading.setText("Byt till Tap & Talk");
                body.setText("Nu finns tangentbordet, men telefonen använder fortfarande ditt "
                        + "vanliga. Knappen nedan öppnar väljaren, där du trycker på "
                        + "Tap & Talk · rösttangentbord.");
                action.setText("Öppna tangentbordsväljaren");
                aside.setText("Du byter tillbaka när du vill, med ⌨-knappen i tangentbordet "
                        + "eller samma väljare.");
                break;
            default:
                heading.setText("Prova att diktera");
                body.setText("Tryck i fältet nedan så kommer Tap & Talk upp. Tryck på den "
                        + "breda gröna Tala-knappen och säg en mening. Texten ska skrivas i "
                        + "fältet medan du talar.");
                action.setText("Klart");
                aside.setText("Samma sak fungerar nu i Messenger, Signal, Meddelanden och "
                        + "alla andra appar där du skriver.");
                break;
        }
        showReceipt();
    }

    /** Kvittot: fältet fick text, och det var vårt tangentbord som skrev. */
    private void showReceipt() {
        if (receipt == null || test == null) return;
        boolean any = test.getText().toString().trim().length() > 0;
        if (!any) {
            receipt.setText("");
            return;
        }
        if (chosenOk(this)) {
            receipt.setTextColor(Look.GREEN);
            receipt.setText("✓ Det fungerar. Tap & Talk skriver i fältet.");
        } else {
            receipt.setTextColor(Look.WARN);
            receipt.setText("Texten kom in, men från ett annat tangentbord. Byt till "
                    + "Tap & Talk för att prova dikteringen.");
        }
    }

    // ---------------------------------------------------------------- knappen

    private void act() {
        switch (step) {
            case STEP_MIC:
                if (micBlocked) {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName())));
                } else {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
                }
                break;
            case STEP_ENABLE:
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
                break;
            case STEP_CHOOSE:
                InputMethodManager boards = getSystemService(InputMethodManager.class);
                if (boards != null) boards.showInputMethodPicker();
                break;
            default:
                finish();
                break;
        }
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] result) {
        super.onRequestPermissionsResult(code, perms, result);
        if (code != MIC_PERMISSION) return;
        if (micOk(this)) {
            micBlocked = false;
            step = firstUndone();
        } else {
            micBlocked = !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO);
        }
        render();
    }

    /** Väljaren ger inget svar tillbaka, så läget läses om när vi får fokus. */
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (!focus) return;
        int next = firstUndone();
        if (next > step) step = next;
        render();
        // Tangentbordet fälls upp en gång när provet nås, sedan råder
        // användaren. Det görs här, när fönstret verkligen har fokus.
        if (step == STEP_TEST && !opened) {
            opened = true;
            test.requestFocus();
            InputMethodManager boards = getSystemService(InputMethodManager.class);
            if (boards != null) boards.showSoftInput(test, 0);
        }
    }
}
