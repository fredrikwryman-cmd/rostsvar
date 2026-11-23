package com.techrabbit.tapntalk;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Den flytande knappen: ligger över alla appar, lyssnar när man trycker och
 * lägger texten i det fält man står i.
 *
 * Tjänsten körs i förgrunden, som Android kräver av något som håller
 * mikrofonen igång utanför sin egen skärm. Notisen är avsiktligt tyst och
 * lägst möjliga vikt — den finns för att Android kräver den och för att man
 * ska kunna stänga bubblan utan att leta upp appen.
 *
 * Inget spelas in och ingenting sparas. Det enda som lämnar telefonen är
 * talet, till telefonens egen taltjänst.
 */
public class Bubble extends Service {

    static final String START = "com.techrabbit.tapntalk.START_BUBBLA";
    static final String STOP = "com.techrabbit.tapntalk.STOPPA_BUBBLA";

    private static final String CHANNEL = "bubbla";
    private static final int NOTE_ID = 11;
    /** Var bubblan låg sist. Sparas så att den ligger kvar där man lade den. */
    private static final String AT_X = "bubbla_x";
    private static final String AT_Y = "bubbla_y";

    private static boolean alive;

    private WindowManager windows;
    private WindowManager.LayoutParams where;
    private TextView knob;

    private SpeechRecognizer speech;
    private boolean listening;
    /** Hur många punkter som visas just nu medan taltjänsten slutför. */
    private int dots;
    /**
     * Punkterna i bubblan medan tolkningen pågår. Bubblan har ingen
     * statusrad, så rörelsen är det enda som kan säga att något händer under
     * de åtta sekunder NO_ANSWER tillåter. Står stilla på "…" när telefonen
     * har stängt av animationer.
     */
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!working || listening) return;
            dots = dots % 3 + 1;
            paint();
            hand.postDelayed(tick, 400);
        }
    };
    /**
     * Sant från Tala till sista svaret, också efter att mikrofonen stängts.
     * Taltjänsten skriver sin sista tolkning efter att talet tagit slut, och
     * startas en ny omgång under tiden levereras den gamla meningen mitt i
     * den nya. Flaggan håller den andra omgången tillbaka till svaret kommit.
     */
    private boolean working;
    /** Sista tolkningen. Finns den kvar vid ett fel levereras den ändå. */
    private String heard = "";
    /** Programmet som ägde fältet när dikteringen startade. */
    private String target;

    private final Handler hand = new Handler(Looper.getMainLooper());
    /**
     * Svarar taltjänsten inte alls får texten inte bli inlåst. Gränsen är
     * satt över det en vanlig sluttolkning tar.
     */
    private static final long NO_ANSWER = 8000L;
    private final Runnable giveUp = new Runnable() {
        @Override public void run() {
            if (!working) return;
            working = false;
            listening = false;
            beat(false);
            paint();
            // Det som hunnit höras levereras. Hördes inget sägs det.
            if (heard.length() > 0) {
                String text = heard;
                heard = "";
                deliver(text);
            } else {
                say(Notices.afterSilence(false, Notices.BUBBLE));
            }
            Mic.release(Bubble.this);
        }
    };

    /** Sant mellan start och stopp. Hemskärmens strömbrytare läser den. */
    static boolean running() {
        return alive;
    }

    /** Sant när Android låter appen rita över andra appar. */
    static boolean mayShow(Context context) {
        return Settings.canDrawOverlays(context);
    }

    static void start(Context context) {
        Intent order = new Intent(context, Bubble.class).setAction(START);
        context.startForegroundService(order);
    }

    static void stop(Context context) {
        context.startService(new Intent(context, Bubble.class).setAction(STOP));
    }

    // ------------------------------------------------------------- livscykeln

    @Override public void onCreate() {
        super.onCreate();
        windows = getSystemService(WindowManager.class);
    }

    @Override public int onStartCommand(Intent order, int flags, int id) {
        if (order != null && STOP.equals(order.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Bubblan är meningslös utan rätten att ligga över andra appar, och
        // en förgrundstjänst av typen mikrofon får inte startas utan
        // mikrofontillstånd. Hellre ett tydligt nej än en krasch.
        if (!mayShow(this)) {
            say("Tap&Talk får inte visas över andra appar. Slå på det i guiden.");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            say("Mikrofonen behövs för att diktera. Tillåt den i appen.");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!raise()) return START_NOT_STICKY;
        if (knob == null) show();
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent order) {
        return null;
    }

    @Override public void onDestroy() {
        alive = false;
        quiet();
        if (knob != null && windows != null) {
            try {
                windows.removeView(knob);
            } catch (IllegalArgumentException gone) {
                // Vyn kan redan vara borta om fönstret rivits. Inget att göra.
            }
            knob = null;
        }
        super.onDestroy();
    }

    // -------------------------------------------------------------- notisen

    /**
     * Tyst notis med lägsta vikt, plus en knapp som stänger bubblan.
     *
     * @return falskt när Android nekade förgrundstjänsten. Då har tjänsten
     *         lagt sig ned och den som ringde ska inte fortsätta.
     */
    private boolean raise() {
        NotificationManager shade = getSystemService(NotificationManager.class);
        if (shade != null && shade.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "Bubblan", NotificationManager.IMPORTANCE_MIN);
            channel.setDescription("Visas så länge den flytande knappen ligger framme.");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            shade.createNotificationChannel(channel);
        }

        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, Home.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent off = PendingIntent.getService(this, 1,
                new Intent(this, Bubble.class).setAction(STOP), PendingIntent.FLAG_IMMUTABLE);

        Notification note = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Tap&Talk")
                .setContentText("Tryck på bubblan och tala.")
                .setContentIntent(open)
                .setOngoing(true)
                .setShowWhen(false)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                        "Stäng", off).build())
                .build();

        // Android kan neka en förgrundstjänst som startas om från bakgrunden
        // efter att systemet dödat den. Ett nej får inte bli en kraschslinga:
        // då lägger sig tjänsten ned, och strömbrytaren i appen startar den
        // igen när användaren vill.
        try {
            startForeground(NOTE_ID, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } catch (RuntimeException refused) {
            alive = false;
            stopSelf();
            return false;
        }
        alive = true;
        return true;
    }

    // --------------------------------------------------------------- bubblan

    private int dp(int value) {
        return Look.dp(this, value);
    }

    private void show() {
        int size = dp(66);

        knob = new TextView(this);
        knob.setText("Tala");
        knob.setAllCaps(false);
        knob.setGravity(Gravity.CENTER);
        knob.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        knob.setContentDescription("Tap&Talk, tryck för att diktera");
        paint();

        where = new WindowManager.LayoutParams(
                size, size,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // Får aldrig ta skrivfokus. Tar bubblan fokus tappar fältet
                // under den sin markör, och då finns inget att skriva i.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        where.gravity = Gravity.TOP | Gravity.START;

        SharedPreferences saved = getSharedPreferences(Guide.SETTINGS, MODE_PRIVATE);
        int wide = getResources().getDisplayMetrics().widthPixels;
        int high = getResources().getDisplayMetrics().heightPixels;
        where.x = saved.getInt(AT_X, wide - size);
        where.y = saved.getInt(AT_Y, high / 2);

        // Dragningen ligger i touch-lyssnaren, men trycket går genom en
        // riktig klicklyssnare. TalkBack trycker aldrig med fingrar: det
        // skickar ACTION_CLICK, som blir performClick(), och det når bara en
        // OnClickListener. Utan den här raden är bubblan — appens huvudväg på
        // Android — helt stängd för skärmläsare.
        knob.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { tapped(); }
        });
        knob.setOnTouchListener(new Drag(size));
        try {
            windows.addView(knob, where);
        } catch (RuntimeException refused) {
            // Händer om rätten dras tillbaka i samma stund. Bubblan kan inte
            // visas, och då ska tjänsten inte ligga kvar och låtsas.
            knob = null;
            say("Bubblan kunde inte visas. Kontrollera Visa över andra appar.");
            stopSelf();
        }
    }

    /** Grönt i vila, djupare grönt medan mikrofonen är på. */
    private void paint() {
        if (knob == null) return;
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(listening ? Look.GREEN_DEEP : Look.GREEN);
        circle.setStroke(dp(2), listening ? Look.GREEN : 0x55000000);
        knob.setBackground(circle);
        knob.setTextColor(Look.DARK_INK);
        // Tre lägen, inte två: vila, lyssnar, och tolkar. Utan det sista ser
        // bubblan ledig ut medan taltjänsten fortfarande arbetar.
        if (listening) {
            knob.setText("Klar");
        } else if (working) {
            knob.setText(Motion.wanted(Bubble.this) ? ".".repeat(Math.max(1, dots)) : "…");
        } else {
            knob.setText("Tala");
        }
        if (!listening) {
            knob.setScaleX(1f);
            knob.setScaleY(1f);
        }
    }

    /** Drar man bubblan flyttar den sig och fastnar vid närmaste kant. */
    private final class Drag implements View.OnTouchListener {
        private final int size;
        private float fromX, fromY;
        private int wasX, wasY;
        private boolean moved;
        private long down;

        Drag(int size) {
            this.size = size;
        }

        @Override public boolean onTouch(View view, MotionEvent touch) {
            switch (touch.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    fromX = touch.getRawX();
                    fromY = touch.getRawY();
                    wasX = where.x;
                    wasY = where.y;
                    moved = false;
                    down = System.currentTimeMillis();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = Math.round(touch.getRawX() - fromX);
                    int dy = Math.round(touch.getRawY() - fromY);
                    if (Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8)) moved = true;
                    if (!moved) return true;
                    where.x = wasX + dx;
                    where.y = wasY + dy;
                    hold();
                    windows.updateViewLayout(knob, where);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    boolean quick = System.currentTimeMillis() - down < 500;
                    if (!moved && quick && touch.getAction() == MotionEvent.ACTION_UP) {
                        // performClick i stället för tapped(): fingret och
                        // TalkBack ska gå exakt samma väg in.
                        view.performClick();
                        return true;
                    }
                    snap();
                    return true;
                default:
                    return false;
            }
        }

        /** Håller bubblan inom skärmen medan den dras. */
        private void hold() {
            int wide = getResources().getDisplayMetrics().widthPixels;
            int high = getResources().getDisplayMetrics().heightPixels;
            where.x = Math.max(0, Math.min(where.x, wide - size));
            where.y = Math.max(0, Math.min(where.y, high - size));
        }

        private void snap() {
            hold();
            int wide = getResources().getDisplayMetrics().widthPixels;
            where.x = where.x + size / 2 < wide / 2 ? 0 : wide - size;
            windows.updateViewLayout(knob, where);
            getSharedPreferences(Guide.SETTINGS, MODE_PRIVATE).edit()
                    .putInt(AT_X, where.x).putInt(AT_Y, where.y).apply();
        }
    }

    // ------------------------------------------------------------------ talet

    private void tapped() {
        if (listening) {
            if (speech != null) speech.stopListening();
            listening = false;
            paint();
            // Mikrofonen är stängd, men svaret är inte inne. Håll omgången
            // öppen, och ge upp om taltjänsten tiger.
            hand.removeCallbacks(giveUp);
            hand.postDelayed(giveUp, NO_ANSWER);
            beat(true);
            return;
        }
        if (working) {
            say("Texten tolkas. Vänta ett ögonblick.");
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say(Notices.NO_SERVICE);
            return;
        }
        // Mikrofonen kan ha dragits tillbaka i inställningarna sedan bubblan
        // startade. Då är ett besked bättre än ett fel från taltjänsten.
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            say("Mikrofonen är inte tillåten längre. Slå på den i Tap&Talk.");
            return;
        }
        // En diktering i rösttangentbordet eller i appen har mikrofonen.
        // Hellre ett besked om det än ett fel från taltjänsten.
        if (!Mic.take(this)) {
            say(Notices.BUSY_ELSEWHERE);
            return;
        }
        if (speech == null) {
            speech = SpeechRecognizer.createSpeechRecognizer(this);
            speech.setRecognitionListener(new Ears());
        }
        heard = "";
        // Vilket program fältet tillhör, så att texten inte hamnar i en app
        // användaren bytte till medan tolkningen pågick.
        target = Writer.focusedPackage();

        Intent ask = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE");
        ask.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "sv-SE");
        ask.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        ask.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        ask.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        listening = true;
        working = true;
        paint();
        speech.startListening(ask);
    }

    /** Startar eller stoppar punkterna. */
    private void beat(boolean on) {
        hand.removeCallbacks(tick);
        dots = 3;
        if (on && Motion.wanted(this)) hand.postDelayed(tick, 400);
    }

    private void quiet() {
        listening = false;
        working = false;
        hand.removeCallbacks(giveUp);
        beat(false);
        Mic.release(this);
        if (speech != null) {
            speech.cancel();
            speech.destroy();
            speech = null;
        }
    }

    /**
     * Levererar texten. Går den inte in i något fält hamnar den i urklipp,
     * så att det som sagts aldrig är förlorat.
     */
    private void deliver(String text) {
        if (text == null) return;
        String clean = text.trim();
        if (clean.length() == 0) return;

        // Bytte användaren app medan texten tolkades hör den inte hemma i
        // det fält som råkar ha fokus nu. Då räddas den i urklipp i stället.
        String now = Writer.focusedPackage();
        if (target != null && now != null && !now.equals(target)) {
            toClipboard(clean, "Du bytte app medan texten tolkades. Den ligger i urklipp — "
                    + "håll in i fältet och välj Klistra in.");
            return;
        }

        if (Writer.write(clean)) return;

        toClipboard(clean, Writer.ready()
                ? "Hittade inget textfält. Texten ligger i urklipp — håll in i fältet och välj Klistra in."
                : "Tillgänglighetstjänsten är inte påslagen. Texten ligger i urklipp — klistra in den.");
    }

    /** Sista utposten: texten hamnar i urklipp, och användaren får veta det. */
    private void toClipboard(String clean, String message) {
        ClipboardManager clip = getSystemService(ClipboardManager.class);
        if (clip != null) {
            clip.setPrimaryClip(ClipData.newPlainText("Tap&Talk", clean));
            say(message);
        } else {
            // Inget urklipp att lägga den i. Då sägs texten rakt ut, så att
            // den åtminstone går att läsa och skriva av.
            say("Texten gick inte att leverera: " + clean);
        }
    }

    private void say(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private String first(Bundle box) {
        if (box == null) return null;
        ArrayList<String> words = box.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (words == null || words.isEmpty()) return null;
        return words.get(0);
    }

    /**
     * Lyssnaren skriver inte löpande i fältet. Varje skrivning i en annan app
     * sätter om hela fältets text, och gjordes det för varje preliminär
     * tolkning skulle användarens markör hoppa och texten blinka. Partialen
     * sparas i stället, syns som rörelse i bubblan och levereras en gång.
     */
    private final class Ears implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) { }
        @Override public void onBeginningOfSpeech() { }

        @Override public void onRmsChanged(float rms) {
            if (knob == null || !listening) return;
            // Bubblan andas med rösten, så det syns att mikrofonen är på.
            float grow = 1f + Math.max(0f, Math.min(rms, 10f)) / 45f;
            knob.setScaleX(grow);
            knob.setScaleY(grow);
        }

        @Override public void onBufferReceived(byte[] buffer) { }

        @Override public void onEndOfSpeech() {
            listening = false;
            paint();
            // Talet är slut, men tolkningen är inte klar. Omgången står
            // öppen tills svaret kommer, eller tills tiden går ut.
            hand.removeCallbacks(giveUp);
            hand.postDelayed(giveUp, NO_ANSWER);
            beat(true);
        }

        @Override public void onPartialResults(Bundle partial) {
            if (!working) return;
            String text = first(partial);
            if (text != null) heard = text;
        }

        @Override public void onResults(Bundle results) {
            if (!working) return;
            close();
            String text = first(results);
            deliver(text != null && text.length() > 0 ? text : heard);
            heard = "";
        }

        @Override public void onError(int code) {
            if (!working) return;
            close();
            // Hann något sägas innan felet ska det levereras ändå.
            if (heard.length() > 0) {
                deliver(heard);
                heard = "";
                return;
            }
            // Hela tabellen ligger i Notices. Förr hade bubblan ingen gren
            // för nätet, så utan nät fick användaren "ett fel (2)" i stället
            // för att få veta att svenska går att hämta för offline-bruk.
            say(Notices.afterFailure(code, false, Notices.BUBBLE));
        }

        @Override public void onEvent(int type, Bundle params) { }

        /**
         * Omgången är över: mikrofonen stängd, tidsgränsen avbokad och
         * mikrofonen lämnad tillbaka till de andra ytorna.
         */
        private void close() {
            working = false;
            listening = false;
            hand.removeCallbacks(giveUp);
            beat(false);
            Mic.release(Bubble.this);
            paint();
        }
    }
}
