package com.techrabbit.tapntalk;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Tap&amp;Talk, hela appen på en skärm: diktera svenska, rätta texten för
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
    private static final int DIM_DOT = Look.DIM_DOT;
    private static final int DARK_INK = Look.DARK_INK;
    private static final int WARN = Look.WARN;

    private static final int MIC_PERMISSION = 1;
    private static final int NOTE_PERMISSION = 2;
    /** Fast id, så att texten överlever en vridning av telefonen. */
    private static final int DRAFT_ID = 0x7EAD0001;

    private TextView status;
    /** Syns bara medan taltjänsten slutför. NO_ANSWER är åtta sekunder. */
    private ProgressBar spin;
    private LinearLayout warning;
    private TextView warnText;
    private Switch bubble;
    private TextView bubbleNote;
    /** Av-knappen för haptiken. Samma nyckel som iOS: `haptik_av`. */
    private Switch buzz;
    private EditText draft;
    /**
     * Raden under fältet i tomt läge, rev-ux 2.2: den förklarar vad man får
     * av att trycka på Tala. Försvinner så snart det finns text — då är det
     * texten man ska läsa, inte en förklaring av varför den finns.
     */
    private TextView empty;
    /** Sidan som sjunker när delningsarket ligger över den. */
    private LinearLayout page;
    /** Mörkläggningen bakom arket. Tar aldrig emot ett tryck. */
    private View shade;
    private Button mic;
    /**
     * Rensa. Fanns bara på iOS: här måste man markera allt och radera för
     * hand. Samma ord, samma plats och samma ordning som där — Rensa till
     * vänster, Skicka till höger.
     */
    private Button clear;
    private Button send;

    private SpeechRecognizer speech;

    /** Vilken taltjänst omgången kör på, och om bytet redan är gjort. */
    private final Speech picker = new Speech();
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
            // Texten står redan i fältet: delresultaten skrevs dit medan
            // användaren talade. Hördes inget får det inte påstås att något
            // står kvar, och hördes något får det inte påstås att inget gjorde.
            problem(Notices.afterSilence(heardThisRound(), Notices.TALK));
            render();
        }
    };
    /** Texten som redan stod i fältet när dikteringen startade. */
    private String base = "";
    /** Sant när mikrofonen är avslagen för appen i systeminställningarna. */
    private boolean micBlocked;
    /**
     * Sant när beskedet i statusraden är ett fel och inte en upplysning.
     *
     * Texten är densamma antingen vägen — den kommer ur {@link Notices} och
     * säger redan vad som hände, vad det betyder och vad man gör nu. Det som
     * skiljer är ytan: ett fel får en egen ruta i stället för att ligga som
     * dämpad grå text bland allt annat. rev-ux 2.2.
     */
    private boolean trouble;
    /** Vad användaren bad strömbrytaren om, senast. */
    private boolean wantBubble;
    private String note = "Tryck på Tala och diktera. Du läser texten och väljer själv var den skickas.";

    // ---------------------------------------------------------------- skärmen

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);

        // Utkastet i fältet är transkriptet, och transkriptet är känsligt
        // enligt app-profilen. FLAG_SECURE gör att Android inte lägger en
        // bild av skärmen i app switchern — och inte heller låter någon
        // annan app spela in den. Texten en användare dikterat är ofta ett
        // meddelande till en människa, och den ska inte ligga kvar som ett
        // fotografi efter att appen lämnats.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        page.setPadding(dp(Look.SIDE), dp(Look.PAGE_TOP),
                dp(Look.SIDE), dp(Look.PAGE_BOTTOM));
        page.setFitsSystemWindows(true);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Tap&Talk");
        title.setTextColor(INK);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_APP_NAME);
        head.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button guide = new Button(this);
        guide.setAllCaps(false);
        guide.setText("Guide");
        guide.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_SUBHEAD);
        guide.setTextColor(DIM);
        guide.setBackground(box(Look.CLEAR, BORDER, dp(Look.RADIUS_SMALL)));
        guide.setContentDescription("Öppna kom igång-guiden");
        guide.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openGuide(); }
        });
        head.addView(guide, new LinearLayout.LayoutParams(dp(Look.PILL_W), dp(Look.PILL_H)));
        page.addView(head, row(0, 0));

        LinearLayout said = new LinearLayout(this);
        said.setOrientation(LinearLayout.HORIZONTAL);

        spin = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        spin.setIndeterminate(true);
        spin.setVisibility(View.GONE);
        LinearLayout.LayoutParams spinSlot =
                new LinearLayout.LayoutParams(dp(Look.GAP_XL), dp(Look.GAP_XL));
        spinSlot.rightMargin = dp(Look.GAP_S);
        spinSlot.topMargin = dp(Look.GAP_TICK);
        said.addView(spin, spinSlot);

        status = new TextView(this);
        status.setTextColor(DIM);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_SUBHEAD);
        status.setLineSpacing(0, 1.3f);
        // Raden bär hela förklaringen — varför mikrofonen inte går igång, vad
        // taltjänsten svarade, att texten står kvar. En seende läser den av
        // sig själv; utan live region fick den som lyssnar aldrig veta något.
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        said.addView(status, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        page.addView(said, row(dp(Look.GAP_S), 0));

        // Strömbrytaren för bubblan. Den ligger högst upp eftersom den är
        // appens egentliga funktion: knappen som följer med överallt.
        LinearLayout power = new LinearLayout(this);
        power.setOrientation(LinearLayout.HORIZONTAL);
        power.setGravity(Gravity.CENTER_VERTICAL);
        power.setBackground(box(FIELD, BORDER, dp(Look.RADIUS_CARD)));
        power.setPadding(dp(Look.GAP_ML), dp(Look.GAP_M), dp(Look.GAP_SM), dp(Look.GAP_M));

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);

        TextView powerTitle = new TextView(this);
        powerTitle.setText("Flytande knapp");
        powerTitle.setTextColor(INK);
        powerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_BUTTON);
        words.addView(powerTitle, row(0, 0));

        bubbleNote = new TextView(this);
        bubbleNote.setTextColor(DIM);
        bubbleNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_CAPTION);
        bubbleNote.setLineSpacing(0, 1.25f);
        bubbleNote.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        words.addView(bubbleNote, row(dp(Look.GAP_TICK), 0));

        power.addView(words, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        bubble = new Switch(this);
        // Etiketten börjar med den text som syns intill, "Flytande knapp",
        // och säger sedan vad strömbrytaren gör.
        bubble.setContentDescription("Flytande knapp. Starta eller stäng den.");
        bubble.setMinimumWidth(dp(Look.ROW_TALL));
        bubble.setMinimumHeight(dp(Look.TOUCH));
        bubble.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bubbleToggled(); }
        });
        power.addView(bubble, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(Look.TOUCH)));

        page.addView(power, row(dp(Look.GAP_ML), 0));

        // Av-knappen för haptiken, samma form som bubblans strömbrytare.
        // Den ligger här och inte i guiden, för guiden är en genomgång man
        // gör en gång; det här är en inställning man ändrar när som helst.
        LinearLayout feel = new LinearLayout(this);
        feel.setOrientation(LinearLayout.HORIZONTAL);
        feel.setGravity(Gravity.CENTER_VERTICAL);
        feel.setBackground(box(FIELD, BORDER, dp(Look.RADIUS_CARD)));
        feel.setPadding(dp(Look.GAP_ML), dp(Look.GAP_M),
                dp(Look.GAP_SM), dp(Look.GAP_M));

        LinearLayout feelWords = new LinearLayout(this);
        feelWords.setOrientation(LinearLayout.VERTICAL);

        TextView feelTitle = new TextView(this);
        feelTitle.setText("Vibration");
        feelTitle.setTextColor(INK);
        feelTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_HEADING);
        feelWords.addView(feelTitle, row(0, 0));

        TextView feelNote = new TextView(this);
        feelNote.setText("Telefonen vibrerar lätt när mikrofonen öppnas och "
                + "stängs, och när något går fel.");
        feelNote.setTextColor(DIM);
        feelNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_CAPTION);
        feelNote.setLineSpacing(0, 1.25f);
        feelWords.addView(feelNote, row(dp(Look.GAP_TICK), 0));

        feel.addView(feelWords, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        buzz = new Switch(this);
        buzz.setContentDescription("Vibration. Slå på eller av.");
        buzz.setChecked(Haptik.on(this));
        buzz.setMinimumWidth(dp(Look.SWITCH_W));
        buzz.setMinimumHeight(dp(Look.TOUCH));
        buzz.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { buzzToggled(); }
        });
        feel.addView(buzz, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(Look.TOUCH)));

        page.addView(feel, row(dp(Look.GAP_SM), 0));

        // Varningen står kvar så länge något steg i guiden fattas, eftersom
        // appen då bara kan diktera här inne och inte i chattarna.
        warning = new LinearLayout(this);
        warning.setOrientation(LinearLayout.VERTICAL);
        warning.setBackground(box(Look.WARN_FILL, WARN, dp(Look.RADIUS_CARD)));
        warning.setPadding(dp(Look.GAP_ML), dp(Look.GAP_ML), dp(Look.GAP_ML), dp(Look.GAP_ML));

        warnText = new TextView(this);
        warnText.setTextColor(WARN);
        warnText.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_SUBHEAD);
        warnText.setLineSpacing(0, 1.3f);
        warnText.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        warning.addView(warnText, row(0, 0));

        Button fix = new Button(this);
        fix.setAllCaps(false);
        fix.setText("Öppna guiden");
        fix.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_BODY);
        fix.setTextColor(Look.WARN_FILL);
        fix.setBackground(box(WARN, 0, dp(Look.RADIUS_SMALL)));
        fix.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openGuide(); }
        });
        LinearLayout.LayoutParams fixRow = row(dp(Look.GAP_M), 0);
        fixRow.height = dp(Look.TOUCH);
        warning.addView(fix, fixRow);

        page.addView(warning, row(dp(Look.GAP_ML), 0));

        draft = new EditText(this);
        draft.setId(DRAFT_ID);
        draft.setBackground(box(FIELD, BORDER, dp(Look.RADIUS_CARD)));
        draft.setTextColor(INK);
        draft.setHintTextColor(OFF);
        draft.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_HEADING);
        draft.setHint("Texten du dikterar hamnar här. Du kan rätta den för hand.");
        draft.setGravity(Gravity.TOP | Gravity.START);
        draft.setPadding(dp(Look.GAP_ML), dp(Look.GAP_ML), dp(Look.GAP_ML), dp(Look.GAP_ML));
        draft.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        draft.setContentDescription("Din text");
        draft.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { render(); }
        });
        LinearLayout.LayoutParams field = row(dp(Look.GAP_L), dp(Look.GAP_S));
        field.height = 0;
        field.weight = 1;
        page.addView(draft, field);

        empty = new TextView(this);
        empty.setText(EMPTY_VALUE);
        empty.setTextColor(OFF);
        empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_CAPTION);
        empty.setLineSpacing(0, 1.25f);
        page.addView(empty, row(0, dp(Look.GAP_S)));

        mic = new Button(this);
        mic.setAllCaps(false);
        mic.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_TITLE);
        mic.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { micTapped(); }
        });
        Press.on(mic);
        LinearLayout.LayoutParams micRow = row(0, dp(Look.GAP_M));
        micRow.height = dp(Look.MIC_H);
        page.addView(mic, micRow);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        clear = new Button(this);
        clear.setAllCaps(false);
        clear.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_HEADING);
        clear.setText("Rensa");
        clear.setContentDescription("Rensa fältet");
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { wipe(); }
        });
        Press.on(clear);
        LinearLayout.LayoutParams clearSlot =
                new LinearLayout.LayoutParams(0, dp(Look.CONTROL_H), 1f);
        clearSlot.rightMargin = dp(Look.GAP_SM);
        actions.addView(clear, clearSlot);

        send = new Button(this);
        send.setAllCaps(false);
        send.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_HEADING);
        send.setText("Skicka");
        send.setContentDescription("Skicka texten vidare");
        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { share(); }
        });
        Press.on(send);
        actions.addView(send, new LinearLayout.LayoutParams(0,
                dp(Look.CONTROL_H), 1f));

        page.addView(actions, row(0, 0));

        // Roten finns för delningsarket: sidan ska kunna sjunka och mörkna
        // bakom det, och en mörkläggning behöver ett lager att ligga i.
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        shade = new View(this);
        shade.setBackgroundColor(Look.BLACKOUT);
        shade.setAlpha(0f);
        shade.setClickable(false);
        shade.setFocusable(false);
        shade.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(shade, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);

        if (!Speech.available(this)) {
            problem(Notices.NO_SERVICE);
        }
        render();

        // Första starten går direkt till guiden. Den visas en gång av sig
        // själv; sedan når man den med Guide-knappen.
        boolean seen = getSharedPreferences(Guide.SETTINGS, MODE_PRIVATE)
                .getBoolean(Guide.SEEN, false);
        if (!seen) openGuide();
    }

    /**
     * Av-knappen slogs om. Bekräftelsen på att haptiken är på är själva
     * vibrationen; är den av finns inget att känna, och då står det i
     * stället i strömbrytarens läge.
     */
    private void buzzToggled() {
        Haptik.set(this, buzz.isChecked());
        if (buzz.isChecked()) Haptik.fire(buzz, Haptik.PRESS);
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
        Haptik.fire(bubble, Haptik.STATE_CHANGE);
        if (!bubble.isChecked()) {
            Bubble.stop(this);
            wantBubble = false;
            say("Bubblan är stängd. Slå på den igen när du vill ha den framme.");
            render();
            return;
        }
        if (!granted()) {
            wantBubble = false;
            say("Bubblan behöver mikrofonen. Välj Tillåt i rutan som visas.");
            render();
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
            return;
        }
        if (!Bubble.mayShow(this)) {
            wantBubble = false;
            say("Bubblan får inte visas över andra appar än. Guiden visar var det slås på.");
            render();
            openGuide();
            return;
        }
        // Notisen är Androids krav på en förgrundstjänst. Nekas den körs
        // bubblan ändå, men då syns den inte i notisfältet.
        // Rättigheten finns först i Android 13. På 11 och 12 är den ett okänt
        // namn: ingen ruta visas, svaret är alltid nej, och appen ber om en
        // rättighet som inte finns. Där visas notisen utan att frågas om.
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTE_PERMISSION);
        }
        Bubble.start(this);
        wantBubble = true;
        say(Writer.enabled(this)
                ? "Bubblan ligger framme. Lämna appen, tryck på den och tala."
                : "Bubblan ligger framme. Slå på tillgängligheten i guiden för att texten "
                + "ska hamna direkt i fältet.");
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
        if (stroke != 0) shape.setStroke(dp(Look.HAIRLINE), stroke);
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * Skriver bara när texten verkligen ändrats.
     *
     * render() kallas vid varje tecken, varje tryck och varje återkomst, och
     * setText säger till skärmläsaren att innehållet bytts även när det inte
     * gjort det. Med live region hade det blivit en upplasning per omritning.
     */
    private void write(TextView view, String text) {
        if (!text.contentEquals(view.getText())) view.setText(text);
    }

    /**
     * Beskedet i statusraden.
     *
     * Raden bar förut allt på samma yta: att mikrofonen är på, att texten är
     * klar, och att taltjänsten inte nådde nätet. Nu skiljs upplysning och
     * fel, och det är ytan som skiljer dem. Texten är densamma som förut.
     */
    private void say(String text) {
        note = text;
        trouble = false;
    }

    /** Samma rad, men något gick fel. rev-ux 2.2. */
    private void problem(String text) {
        note = text;
        trouble = true;
    }

    /** Allt som syns härleds ur läget, så att knapparna aldrig ljuger. */
    private void render() {
        boolean hasText = draft.getText().toString().trim().length() > 0;
        boolean canTalk = Speech.available(this);

        write(status, note);
        spin.setVisibility(working && !listening ? View.VISIBLE : View.GONE);

        // Felytan. Samma mening som förut, på en yta som syns: gul text i en
        // gul ram mot en nästan svart platta. Uppmätt 11,9:1.
        status.setTextColor(trouble ? WARN : DIM);
        status.setBackground(trouble
                ? box(Look.WARN_FILL, WARN, dp(Look.RADIUS_SMALL))
                : null);
        int pad = trouble ? dp(Look.GAP_SM) : 0;
        status.setPadding(pad, pad, pad, pad);

        // Tomt läge, rev-ux 2.2. Raden förklarar värdet och går så fort det
        // finns något att läsa i stället.
        empty.setVisibility(hasText ? View.GONE : View.VISIBLE);

        // Varningen namnger det som faktiskt fattas, i stället för att säga
        // samma sak vad det än är.
        String gap = Guide.missing(this);
        warning.setVisibility(gap == null ? View.GONE : View.VISIBLE);
        if (gap != null) {
            write(warnText, gap + " Du kan diktera här i appen, men ännu inte "
                    + "direkt där du skriver.");
        }

        // Strömbrytaren visar vad användaren bad om, inte vad tjänsten hunnit
        // göra: en tjänst startar och stoppar inte i samma andetag som ett
        // tryck, och knappen ska inte hoppa tillbaka under fingret.
        boolean mayStart = granted() && Bubble.mayShow(this);
        bubble.setChecked(wantBubble);
        bubble.setEnabled(mayStart || wantBubble);
        if (wantBubble) {
            write(bubbleNote, Writer.enabled(this)
                    ? "Bubblan ligger framme. Tryck på den, tala, och texten hamnar där du skriver."
                    : "Bubblan ligger framme, men får inte skriva i andra appar än. Texten "
                    + "hamnar i urklipp tills du slår på tillgängligheten i guiden.");
        } else {
            write(bubbleNote, mayStart
                    ? "Lägg en knapp över alla appar. Ett tryck, du talar, texten hamnar i fältet."
                    : "Behöver mikrofon och Visa över andra appar. Öppna guiden.");
        }

        mic.setText(listening ? "Klar" : (micBlocked ? "Tillåt mikrofon" : (hasText ? "Tala om" : "Tala")));
        mic.setContentDescription(listening
                ? "Klar, stäng mikrofonen"
                : (micBlocked ? "Öppna inställningarna och tillåt mikrofonen" : "Tala, starta mikrofonen"));
        mic.setEnabled(canTalk);
        mic.setBackground(box(canTalk ? (listening ? GREEN_DEEP : GREEN) : FIELD, canTalk ? 0 : BORDER, dp(Look.RADIUS_MIC)));
        mic.setTextColor(canTalk ? DARK_INK : OFF);

        // Samma regel som HomeState.canClear och canSend på iOS: båda väntar
        // tills dikteringen är över. En delningsruta över en öppen mikrofon
        // vore ingen tjänst mot användaren, och ett rensat fält mitt i en
        // tolkning skulle fyllas igen av svaret.
        boolean resting = !listening && !working;
        // Samma roller som på iOS: Skicka är skärmens enda gröna
        // primärknapp, fylld med mörk text. Rensa är sekundär — kortets yta,
        // grå kant, aldrig grön. Formen är piller på båda plattformarna
        // sedan punkt 10; den var rektangulär med radie 14 här.
        boolean ready = hasText && resting;
        clear.setEnabled(ready);
        clear.setBackground(box(FIELD, ready ? DIM_DOT : BORDER,
                dp(Look.RADIUS_PILL)));
        clear.setTextColor(ready ? INK : OFF);

        send.setEnabled(ready);
        send.setBackground(box(ready ? GREEN : FIELD, ready ? 0 : BORDER,
                dp(Look.RADIUS_PILL)));
        send.setTextColor(ready ? DARK_INK : OFF);

        draft.setFocusable(!listening);
        draft.setFocusableInTouchMode(!listening);
        draft.setCursorVisible(!listening);
    }

    // ------------------------------------------------------- tillstånd och tal

    private void micTapped() {
        if (listening) {
            Haptik.fire(mic, Haptik.RECORD_STOP);
            stopListening();
            return;
        }
        if (working) {
            say("Texten tolkas. Vänta ett ögonblick.");
            render();
            return;
        }
        if (micBlocked && !granted()) {
            Haptik.fire(mic, Haptik.WARNING);
            openSettings();
            return;
        }
        if (!granted()) {
            say("Mikrofonen behövs för att diktera. Välj Tillåt i rutan som visas.");
            render();
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION);
            return;
        }
        Haptik.fire(mic, Haptik.RECORD_START);
        startListening();
    }

    private boolean granted() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void openSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (RuntimeException missing) {
            // Tillverkarnas skal har inte alla rutor. Guiden gör samma sak i
            // open(): hellre ett besked om var inställningen ligger.
            problem("Telefonen öppnade inte inställningarna. Slå på mikrofonen för "
                    + "Tap&Talk under Appar i telefonens inställningar.");
            render();
        }
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
        problem(micBlocked
                ? "Mikrofonen är avstängd för Tap&Talk. Tryck på Tillåt mikrofon för att öppna inställningarna."
                : "Utan mikrofon kan appen inte diktera. Tryck på Tala igen och välj Tillåt.");
        render();
    }

    private void startListening() {
        if (listening) return;
        // Bubblan eller rösttangentbordet kan hålla mikrofonen. Hellre ett
        // besked om var den är än ett upptaget-fel från taltjänsten.
        if (!Mic.take(this)) {
            problem(Notices.BUSY_ELSEWHERE);
            render();
            return;
        }
        if (speech == null) {
            if (!Speech.available(this)) {
                // Spärren togs två rader upp. Lämnas den inte tillbaka är
                // mikrofonen låst för bubblan och tangentbordet i sjuttio
                // sekunder, utan att någon lyssnar.
                Mic.release(this);
                problem(Notices.NO_SERVICE);
                render();
                return;
            }
            try {
                speech = Speech.open(this, picker);
            } catch (RuntimeException refused) {
                // Varken enhetstolkningen eller den vanliga tjänsten gick
                // att skapa. Spärren är tagen och måste tillbaka.
                speech = null;
                Mic.release(this);
                problem(Notices.failure(SpeechRecognizer.ERROR_CLIENT, Notices.TALK));
                render();
                return;
            }
            speech.setRecognitionListener(new Ears());
        }
        base = draft.getText().toString().trim();
        if (base.length() > 0) base = base + " ";

        Intent ask = Speech.request(getPackageName());

        listening = true;
        working = true;
        say("Mikrofonen är på. Tala svenska och tryck på Klar när du är färdig.");
        render();
        try {
            speech.startListening(ask);
        } catch (RuntimeException refused) {
            // Nekas starten låg appen kvar på "Mikrofonen är på" utan
            // mikrofon, och spärren var tagen tills den blev gammal.
            closeRound();
            speech.destroy();
            speech = null;
            problem(Notices.failure(SpeechRecognizer.ERROR_CLIENT, Notices.TALK));
            render();
        }
    }

    /**
     * Gör om omgången på den vanliga taltjänsten.
     *
     * Mikrofonspärren hålls kvar hela vägen: släpps den här kan bubblan eller
     * tangentbordet ta den mellan de två försöken, och användaren skulle få
     * veta att mikrofonen används någon annanstans mitt i sin egen diktering.
     *
     * @return sant när det nya försöket startade. Falskt betyder att
     *         {@code onError} går vidare som om inget byte fanns, och då är
     *         det {@code closeRound()} där som släpper spärren.
     */
    private boolean retry() {
        hand.removeCallbacks(giveUp);
        drop();
        try {
            speech = Speech.cloud(this, picker);
            speech.setRecognitionListener(new Ears());
            listening = true;
            working = true;
            say("Mikrofonen är på. Tala svenska och tryck på Klar när du är färdig.");
            render();
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

    private void stopListening() {
        if (!listening) return;
        listening = false;
        say("Tolkar det du sa …");
        render();
        if (speech != null) speech.stopListening();
        hand.removeCallbacks(giveUp);
        hand.postDelayed(giveUp, NO_ANSWER);
    }

    /** Omgången är över: tidsgränsen avbokad, mikrofonen lämnad tillbaka. */
    private void closeRound() {
        working = false;
        listening = false;
        picker.roundOver();
        hand.removeCallbacks(giveUp);
        Mic.release(this);
    }

    /** Mikrofonen stängs när skärmen lämnas. Inget spelas in och inget sparas. */
    @Override protected void onStop() {
        super.onStop();
        if (listening) {
            say("Mikrofonen stängdes när du lämnade appen. Tryck på Tala för att fortsätta.");
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
        // Delningsarket kan ha stängts, eller delningen gjorts klar. Sidan
        // ska vara sig själv igen när den syns.
        recede(false);
        if (granted()) micBlocked = false;
        // Taltjänsten kan ha kommit eller försvunnit medan appen låg undan —
        // nätet, eller en rösttjänst som avinstallerats. Hellre ett besked i
        // förväg än ett fel efter att användaren talat färdigt.
        if (!Speech.available(this)) {
            problem(Notices.NO_SERVICE);
        } else if (Notices.NO_SERVICE.equals(note)) {
            say("Tryck på Tala och diktera. Du läser texten och väljer själv var den skickas.");
        }
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
            say("Tala nu. Tryck på Klar när du är färdig.");
            render();
        }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rms) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onEndOfSpeech() {
            if (listening) {
                listening = false;
                say("Tolkar det du sa …");
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
            say("Läs igenom texten, rätta den om du vill, och tryck på Skicka.");
            render();
        }
        @Override public void onError(int code) {
            if (!working) return;
            // Enhetstolkningen kan vara oduglig för svenska på just den
            // här telefonen. Då görs omgången om på den vanliga
            // taltjänsten, en gång, utan att släppa mikrofonen och utan
            // att kasta det som redan står i fältet.
            if (picker.swapToCloud(code, heardThisRound()) && retry()) return;
            closeRound();
            // Hann något höras står det kvar i fältet, och då är det inte
            // sant att inget uppfattades. Regeln bor i Notices.afterFailure.
            if (code == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) micBlocked = !granted();
            problem(Notices.afterFailure(code, heardThisRound(), Notices.TALK));
            render();
        }
        @Override public void onEvent(int type, Bundle params) { }
    }

    /**
     * Sant när den här omgången hann höra något som står kvar i fältet.
     * Grunden är texten som redan stod där när dikteringen startade, så allt
     * som är längre än grunden kom från den här omgången.
     */
    private boolean heardThisRound() {
        return draft.getText().toString().trim().length() > base.trim().length();
    }

    /**
     * Rensar fältet. Grunden nollas med texten: annars hade nästa diktering
     * lagts efter en text som inte finns kvar.
     */
    private void wipe() {
        if (listening || working) return;
        Haptik.fire(clear, Haptik.PRESS);
        draft.setText("");
        base = "";
        say("Fältet är tomt. Tryck på Tala och diktera igen.");
        render();
    }

    // ------------------------------------------------------------- delningen

    /** Skicka-knappen lämnar texten till Androids delningsruta, inget annat. */
    private void share() {
        String text = draft.getText().toString().trim();
        if (text.length() == 0) return;
        Haptik.fire(send, Haptik.SUCCESS);
        Intent out = new Intent(Intent.ACTION_SEND);
        out.setType("text/plain");
        out.putExtra(Intent.EXTRA_TEXT, text);
        out.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        recede(true);
        startActivity(Intent.createChooser(out, "Skicka texten"));
    }

    /**
     * Sidan bakom delningsarket, rev-ux 4.1.
     *
     * Delningsrutan är systemets eget ark och ligger över appen. Utan det här
     * låg hemskärmen kvar i full storlek och full styrka bakom den, som om
     * ingenting hänt. Måtten är samma som på iOS: 0,94 och 0,45.
     *
     * Vid reducerad rörelse krymper sidan inte. Då är mörkläggningen kvar och
     * ingenting flyttar sig.
     */
    private void recede(boolean behind) {
        if (page == null || shade == null) return;
        long time = Motion.travel(this, Motion.STATE);
        boolean still = Motion.paused(this);
        float to = behind && !still ? Motion.RECEDE : 1f;
        page.animate().cancel();
        page.animate().scaleX(to).scaleY(to).setDuration(time).start();
        shade.animate().cancel();
        shade.animate().alpha(behind ? Motion.RECEDE_DIM : 0f)
                .setDuration(time).start();
    }

    /** Raden under fältet när det är tomt. */
    private static final String EMPTY_VALUE =
            "Inget skrivet än. Texten stannar i telefonen tills du själv "
            + "väljer var den skickas.";
}
