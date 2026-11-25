package com.techrabbit.tapntalk;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
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
    /**
     * Fönstret bubblan ligger i: knappen, och lägesbytets yta ovanpå den.
     *
     * Ytan behöver ett eget lager att växa i, och lagret måste ligga i samma
     * fönster som knappen. Knappen är kvar som den var.
     */
    private FrameLayout shell;
    private TextView knob;
    /** Lägesbytets yta. Växer över knappen, och är borta däremellan. */
    private View wash;
    /** Kastet efter ett släpp. Avbryts om bubblan tas igen. */
    private ValueAnimator flight;
    /**
     * Var fingret låg vid senaste tryck, i knappens egna koordinater.
     *
     * Negativt betyder att bytet inte kom av ett tryck: TalkBack trycker
     * utan finger, och taltjänsten byter läge av sig själv. Då växer ytan
     * från mitten. Läses en gång och nollas sedan.
     */
    private float tapX = -1f;
    private float tapY = -1f;

    /**
     * Hur långt farten räcker efter ett släpp, i sekunder av oförminskad
     * fart. En snärt på 3000 punkter i sekunden bär alltså ungefär 360
     * punkter vidare innan friktionen ätit upp den.
     */
    private static final float GLIDE = 0.12f;
    /** Längsta tid ett kast får ta. Längre än så känns det som sirap. */
    private static final long FLIGHT_MAX = 520L;

    private SpeechRecognizer speech;

    /** Vilken taltjänst omgången kör på, och om bytet redan är gjort. */
    private final Speech picker = new Speech();
    private boolean listening;
    /** Hur många punkter som visas just nu medan taltjänsten slutför. */
    private int dots;
    private static final String[] BEATS = {".", "..", "..."};
    /**
     * Punkterna i bubblan medan tolkningen pågår. Bubblan har ingen
     * statusrad, så rörelsen är det enda som kan säga att något händer under
     * de åtta sekunder NO_ANSWER tillåter. Står stilla på "…" när telefonen
     * har stängt av animationer.
     */
    /** Punkternas takt medan taltjänsten tolkar. */
    private static final long BEAT = 400L;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!working || listening) return;
            dots = dots % 3 + 1;
            paint();
            long step = Motion.pulse(Bubble.this, BEAT);
            if (step > 0L) hand.postDelayed(tick, step);
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
            shift();
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
        if (shell == null) show();
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent order) {
        return null;
    }

    @Override public void onDestroy() {
        alive = false;
        quiet();
        halt();
        if (shell != null && windows != null) {
            try {
                windows.removeView(shell);
            } catch (IllegalArgumentException gone) {
                // Vyn kan redan vara borta om fönstret rivits. Inget att göra.
            }
        }
        shell = null;
        knob = null;
        wash = null;
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

        Notification note = sheet(RESTING);

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

    /** Notisens text i vila. */
    private static final String RESTING = "Tryck på bubblan och tala.";

    /**
     * Notisen, med en given text.
     *
     * Bubblan har ingen statusrad, och en toast är borta efter tre och en
     * halv sekund utan väg tillbaka. Beskeden är längre än det — "Du bytte
     * app medan texten tolkades. Den ligger i urklipp — håll in i fältet och
     * välj Klistra in." — så de skrivs också hit, där de ligger kvar och går
     * att läsa om. BigTextStyle, eftersom en rad inte räcker.
     */
    private Notification sheet(String text) {
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, Home.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent off = PendingIntent.getService(this, 1,
                new Intent(this, Bubble.class).setAction(STOP), PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Tap&Talk")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setOngoing(true)
                .setShowWhen(false)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                        "Stäng", off).build())
                .build();
    }

    // --------------------------------------------------------------- bubblan

    private int dp(int value) {
        return Look.dp(this, value);
    }

    private void show() {
        int size = dp(Look.BUBBLE_SIZE);

        knob = new TextView(this);
        knob.setText("Tala");
        knob.setAllCaps(false);
        knob.setGravity(Gravity.CENTER);
        knob.setTextSize(TypedValue.COMPLEX_UNIT_SP, Look.TYPE_BODY);
        paint();

        // Ytan som växer vid lägesbyte. Ligger över knappen, tar aldrig emot
        // ett tryck, och syns bara under de millisekunder bytet tar.
        wash = new View(this);
        wash.setVisibility(View.GONE);
        wash.setClickable(false);
        wash.setFocusable(false);
        wash.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        shell = new FrameLayout(this);
        // Knappen andas med rösten och växer utöver sina egna mått. Utan den
        // här raden klipper lagret bort andningen.
        shell.setClipChildren(false);
        shell.addView(knob, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        shell.addView(wash, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

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
        Rect room = room();
        where.x = saved.getInt(AT_X, room.right - size);
        where.y = saved.getInt(AT_Y, room.centerY() - size / 2);
        // Det sparade läget kan komma från en annan skärm, eller från innan
        // telefonen vändes. Det hålls inom ytan i stället för att hamna
        // utanför kanten eller under ett systemfält.
        hold(size);

        // Dragningen ligger i touch-lyssnaren, men trycket går genom en
        // riktig klicklyssnare. TalkBack trycker aldrig med fingrar: det
        // skickar ACTION_CLICK, som blir performClick(), och det når bara en
        // OnClickListener. Utan den här raden är bubblan — appens huvudväg på
        // Android — helt stängd för skärmläsare.
        knob.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { tapped(); }
        });
        touch(knob, new Drag(size));
        try {
            windows.addView(shell, where);
        } catch (RuntimeException refused) {
            // Händer om rätten dras tillbaka i samma stund. Bubblan kan inte
            // visas, och då ska tjänsten inte ligga kvar och låtsas.
            shell = null;
            wash = null;
            knob = null;
            say("Bubblan kunde inte visas. Kontrollera Visa över andra appar.");
            stopSelf();
        }
    }

    /**
     * Sätter en touch-lyssnare på en vy som redan har en klicklyssnare.
     *
     * Lint räknar en touch-lyssnare utan performClick som otillgänglig
     * (ClickableViewAccessibility). Regeln är rätt i allmänhet och fel här:
     * klickvägen finns, den ligger i en egen OnClickListener, och det är den
     * TalkBack går igenom. Undantaget står på ett ställe med skälet, i
     * stället för utspritt över anropen.
     */
    @SuppressLint("ClickableViewAccessibility")
    private static void touch(View view, View.OnTouchListener listener) {
        view.setOnTouchListener(listener);
    }

    /** Grönt i vila, djupare grönt medan mikrofonen är på. */
    private void paint() {
        paint(true);
    }

    /**
     * Samma målning, men ordet kan bytas rakt av.
     *
     * @param tone sant när ett nytt ord ska tonas in. Falskt när bytet redan
     *             bärs av något annat — lägesbytets yta ligger över knappen
     *             just då, och två toningar i rad är en för mycket.
     */
    private void paint(boolean tone) {
        if (knob == null) return;
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(listening ? Look.GREEN_DEEP : Look.GREEN);
        circle.setStroke(dp(Look.EDGE), listening ? Look.GREEN : Look.SHADE);
        knob.setBackground(circle);
        knob.setTextColor(Look.DARK_INK);
        // Tre lägen, inte två: vila, lyssnar, och tolkar. Utan det sista ser
        // bubblan ledig ut medan taltjänsten fortfarande arbetar.
        // Beskrivningen sätts här tillsammans med texten, inte en gång i
        // show(). Förr sa den "tryck för att diktera" också medan mikrofonen
        // var på och medan tolkningen pågick.
        final String word;
        final String said;
        if (listening) {
            word = "Klar";
            said = "Klar. Mikrofonen är på, tryck för att stänga den.";
        } else if (working) {
            // Inte String.repeat: bygget kompileras mot Java 8. Står pulsen
            // still blir punkterna tre och orörliga.
            word = Motion.pulse(Bubble.this, BEAT) > 0L
                    ? BEATS[Math.max(1, dots) - 1] : "…";
            said = "Tap&Talk tolkar det du sa. Vänta ett ögonblick.";
        } else {
            word = "Tala";
            said = "Tala. Tap&Talk, tryck för att diktera.";
        }
        knob.setContentDescription(said);
        if (!tone || word.equals(knob.getText().toString())) {
            knob.setText(word);
        } else {
            // Lägesbytet tonar i stället för att hoppa. Tiden kommer från
            // Motion: det vanliga lägesbytet vid vanlig rörelse, crossfaden
            // på 160 ms när telefonen har stängt av animationer. Ett hopp är
            // också rörelse.
            final TextView face = knob;
            Motion.crossfade(face, Motion.swap(Bubble.this, Motion.STATE),
                    new Runnable() {
                        @Override public void run() { face.setText(word); }
                    });
        }
        if (!listening) {
            knob.setScaleX(1f);
            knob.setScaleY(1f);
        }
    }

    /**
     * Flyttar bubblans fönster till det läge som står i {@code where}.
     *
     * Dras Visa över andra appar tillbaka medan tjänsten lever river
     * systemet fönstret, men tjänsten lever vidare och {@code knob} står
     * kvar. Nästa flytt kastar då IllegalArgumentException — en krasch mitt
     * i en dragning. Hellre lägger bubblan sig ned och säger varför.
     *
     * @return falskt när fönstret är borta. Då är tjänsten på väg ned och
     *         den som ringde ska inte fortsätta.
     */
    private boolean place() {
        if (shell == null || windows == null) return false;
        try {
            windows.updateViewLayout(shell, where);
            return true;
        } catch (RuntimeException gone) {
            shell = null;
            wash = null;
            knob = null;
            say("Bubblan kan inte visas längre. Kontrollera Visa över andra appar.");
            stopSelf();
            return false;
        }
    }

    /**
     * Ytan bubblan får ligga i, i fönstrets egna koordinater.
     *
     * Bubblans fönster sätter inte FLAG_LAYOUT_IN_SCREEN, så 0,0 är hörnet
     * innanför systemfälten — samma hörn som förut, så ett sparat läge
     * betyder fortfarande samma sak. Det som ändras är hur stor ytan räknas:
     * måtten kom förut ur DisplayMetrics, som inte vet något om statusrad,
     * navigeringsfält eller kamerahål, och bubblan kunde hamna under dem.
     *
     * WindowMetrics finns från Android 11, och appen kräver Android 11. Vill
     * telefonen ändå inte svara får de gamla måtten gälla: en bubbla på ett
     * ungefär är bättre än ingen bubbla.
     */
    private Rect room() {
        Rect fallback = new Rect(0, 0,
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);
        if (windows == null) return fallback;
        try {
            WindowMetrics metrics = windows.getCurrentWindowMetrics();
            Rect bounds = metrics.getBounds();
            Insets bars = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            int wide = bounds.width() - bars.left - bars.right;
            int high = bounds.height() - bars.top - bars.bottom;
            if (wide <= 0 || high <= 0) return fallback;
            return new Rect(0, 0, wide, high);
        } catch (RuntimeException refused) {
            return fallback;
        }
    }

    /** Håller bubblan inom ytan. */
    private void hold(int size) {
        Rect room = room();
        where.x = Math.max(room.left, Math.min(where.x, room.right - size));
        where.y = Math.max(room.top, Math.min(where.y, room.bottom - size));
    }

    /**
     * Lägesbytet, rev-ux 4.2 A8.
     *
     * Bubblan bytte förut färg på plats. Nu växer den nya ytan över knappen
     * från den punkt där fingret låg — eller från mitten, när bytet inte kom
     * av ett tryck — och under den har knappen redan bytt färg och ord när
     * ytan tonar bort. Ingen navigering, inget nytt läge: det är samma knapp
     * som säger något annat.
     *
     * Vid reducerad rörelse görs bytet med den crossfade som redan finns i
     * {@link Motion}, och ingenting växer.
     */
    private void shift() {
        float atX = tapX;
        float atY = tapY;
        tapX = -1f;
        tapY = -1f;
        if (knob == null) return;
        // Ett lägesbyte ska kännas: medium. rev-ux avsnitt 7.
        Haptik.fire(knob, this, Haptik.STATE_CHANGE);
        if (wash == null || Motion.paused(this)) {
            paint();
            return;
        }
        int size = dp(Look.BUBBLE_SIZE);
        float x = atX < 0f ? size / 2f : atX;
        float y = atY < 0f ? size / 2f : atY;
        GradientDrawable over = new GradientDrawable();
        over.setShape(GradientDrawable.OVAL);
        over.setColor(listening ? Look.GREEN_DEEP : Look.GREEN);
        final View face = wash;
        face.setBackground(over);
        face.setAlpha(1f);
        face.setVisibility(View.VISIBLE);
        // Så långt det är från fingret till den bortaste kanten av knappen.
        float far = (float) Math.hypot(Math.max(x, size - x), Math.max(y, size - y));
        Animator grow = ViewAnimationUtils.createCircularReveal(
                face, Math.round(x), Math.round(y), 0f, far);
        grow.setDuration(Motion.swap(this, Motion.STATE));
        grow.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator done) {
                // Knappen byter under ytan. Ordet kommer fram när ytan tonar
                // bort, så att ingenting hoppar.
                paint(false);
                face.animate().alpha(0f).setDuration(Motion.CROSSFADE)
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                face.setVisibility(View.GONE);
                                face.setAlpha(1f);
                            }
                        }).start();
            }
        });
        grow.start();
    }

    /**
     * Kastet efter ett släpp, rev-ux 4.2 A13.
     *
     * Bubblan stannade förut i samma stund fingret lämnade den, och hoppade
     * till kanten. Nu fortsätter den med farten den fick, bromsas som något
     * med vikt, och låser vid närmaste kant.
     *
     * Vid reducerad rörelse flyttas den direkt till kanten: ingen fysik,
     * ingen glidning. Haptiken ligger kvar.
     */
    private void fling(final int size, float vx, float vy) {
        halt();
        Rect room = room();
        // Så långt farten hade räckt innan friktionen åt upp den.
        int toX = Math.round(where.x + vx * GLIDE);
        int toY = Math.round(where.y + vy * GLIDE);
        // Närmaste kant räknas på var bubblan hade hamnat, inte på var fingret
        // släppte: kastar man den åt höger ska den åt höger.
        final int endX = toX + size / 2 < room.centerX() ? room.left : room.right - size;
        final int endY = Math.max(room.top, Math.min(toY, room.bottom - size));
        if (Motion.paused(this)) {
            where.x = endX;
            where.y = endY;
            land();
            return;
        }
        final int startX = where.x;
        final int startY = where.y;
        double far = Math.hypot(endX - startX, endY - startY);
        double speed = Math.hypot(vx, vy);
        // En avtagande kurva börjar i dubbla medelfarten. Ska den börja i
        // fingrets fart blir tiden alltså två gånger vägen delat med farten.
        long time = speed < 1.0 ? Motion.STATE : Math.round(2000.0 * far / speed);
        time = Math.max(Motion.CROSSFADE, Math.min(time, FLIGHT_MAX));
        ValueAnimator throwing = ValueAnimator.ofFloat(0f, 1f);
        throwing.setDuration(time);
        throwing.setInterpolator(new DecelerateInterpolator());
        throwing.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator step) {
                float part = step.getAnimatedFraction();
                where.x = Math.round(startX + (endX - startX) * part);
                where.y = Math.round(startY + (endY - startY) * part);
                if (!place()) step.cancel();
            }
        });
        throwing.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator done) {
                flight = null;
                where.x = endX;
                where.y = endY;
                land();
            }
        });
        flight = throwing;
        throwing.start();
    }

    /** Bubblan är framme: den låser, det känns, och läget sparas. */
    private void land() {
        if (!place()) return;
        // Ett objekt som snäpper i läge ska kännas: soft. rev-ux avsnitt 7.
        Haptik.fire(knob, this, Haptik.SNAP);
        // Det sparade läget är kanten den snäppte till, inte där fingret
        // släppte. Då ligger den kvar på samma kant nästa gång.
        getSharedPreferences(Guide.SETTINGS, MODE_PRIVATE).edit()
                .putInt(AT_X, where.x).putInt(AT_Y, where.y).apply();
    }

    /**
     * Avbryter ett kast som är på väg.
     *
     * Lyssnarna tas bort först: ett avbrutet kast ska inte landa, vibrera och
     * spara ett läge som användaren just tagit tillbaka med fingret.
     */
    private void halt() {
        ValueAnimator old = flight;
        flight = null;
        if (old == null) return;
        old.removeAllListeners();
        old.cancel();
    }

    /**
     * Drar man bubblan flyttar den sig, och kastar man den fortsätter den.
     *
     * Farten mäts med telefonens egen {@link VelocityTracker}, samma väg
     * systemet självt mäter ett svep. Släppet lämnas till {@link #fling},
     * som står för fysiken och för kanten.
     */
    private final class Drag implements View.OnTouchListener {
        private final int size;
        private float fromX, fromY;
        private int wasX, wasY;
        private boolean moved;
        private long down;
        /** Farten i fingret, för kastet efter släpp. */
        private VelocityTracker speedo;

        Drag(int size) {
            this.size = size;
        }

        @Override public boolean onTouch(View view, MotionEvent touch) {
            switch (touch.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Tas bubblan mitt i ett kast är det fingret som gäller.
                    halt();
                    fromX = touch.getRawX();
                    fromY = touch.getRawY();
                    wasX = where.x;
                    wasY = where.y;
                    moved = false;
                    down = System.currentTimeMillis();
                    // Var fingret låg, för lägesbytets yta.
                    tapX = touch.getX();
                    tapY = touch.getY();
                    track(touch);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    track(touch);
                    int dx = Math.round(touch.getRawX() - fromX);
                    int dy = Math.round(touch.getRawY() - fromY);
                    if (Math.abs(dx) > dp(Look.GAP_S) || Math.abs(dy) > dp(Look.GAP_S)) moved = true;
                    if (!moved) return true;
                    where.x = wasX + dx;
                    where.y = wasY + dy;
                    hold(size);
                    place();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    track(touch);
                    float vx = 0f;
                    float vy = 0f;
                    if (speedo != null) {
                        speedo.computeCurrentVelocity(1000);
                        vx = speedo.getXVelocity();
                        vy = speedo.getYVelocity();
                        speedo.recycle();
                        speedo = null;
                    }
                    boolean quick = System.currentTimeMillis() - down < 500;
                    if (!moved && quick && touch.getAction() == MotionEvent.ACTION_UP) {
                        // performClick i stället för tapped(): fingret och
                        // TalkBack ska gå exakt samma väg in.
                        view.performClick();
                        return true;
                    }
                    // Dragningen var ingen tryckning. Då ska nästa lägesbyte
                    // inte växa fram ur en punkt fingret lämnade för länge sedan.
                    tapX = -1f;
                    tapY = -1f;
                    fling(size, vx, vy);
                    return true;
                default:
                    return false;
            }
        }

        private void track(MotionEvent touch) {
            if (speedo == null) speedo = VelocityTracker.obtain();
            speedo.addMovement(touch);
        }
    }

    // ------------------------------------------------------------------ talet

    private void tapped() {
        if (listening) {
            if (speech != null) speech.stopListening();
            listening = false;
            shift();
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
        if (!Speech.available(this)) {
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
            try {
                speech = Speech.open(this, picker);
            } catch (RuntimeException refused) {
                // Ingen av de två tjänsterna gick att skapa. Spärren togs
                // två rader upp och måste tillbaka.
                speech = null;
                Mic.release(this);
                say(Notices.failure(SpeechRecognizer.ERROR_CLIENT, Notices.BUBBLE));
                return;
            }
            speech.setRecognitionListener(new Ears());
        }
        heard = "";
        // Vilket program fältet tillhör, så att texten inte hamnar i en app
        // användaren bytte till medan tolkningen pågick.
        target = Writer.focusedPackage();

        Intent ask = Speech.request(getPackageName());

        listening = true;
        working = true;
        shift();
        try {
            speech.startListening(ask);
        } catch (RuntimeException refused) {
            // Taltjänsten kan neka i samma stund den ska börja. Utan den här
            // grenen stod bubblan kvar och tolkade i evighet, med mikrofonen
            // tagen från de andra ytorna i sjuttio sekunder.
            listening = false;
            working = false;
            paint();
            Mic.release(this);
            speech.destroy();
            speech = null;
            say(Notices.failure(SpeechRecognizer.ERROR_CLIENT, Notices.BUBBLE));
        }
    }

    /**
     * Gör om omgången på den vanliga taltjänsten.
     *
     * Mikrofonspärren hålls kvar hela vägen: släpps den här kan en annan yta
     * ta den mellan de två försöken, och användaren skulle få "mikrofonen
     * används någon annanstans" mitt i sin egen diktering.
     *
     * @return sant när det nya försöket startade. Falskt betyder att
     *         {@code onError} ska gå vidare som om inget byte fanns, och då
     *         är det {@code close()} där som släpper spärren.
     */
    private boolean retry() {
        hand.removeCallbacks(giveUp);
        drop();
        try {
            speech = Speech.cloud(this, picker);
            speech.setRecognitionListener(new Ears());
            listening = true;
            working = true;
            shift();
            beat(true);
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

    /** Startar eller stoppar punkterna. */
    private void beat(boolean on) {
        hand.removeCallbacks(tick);
        dots = 3;
        long step = Motion.pulse(this, BEAT);
        if (on && step > 0L) hand.postDelayed(tick, step);
    }

    private void quiet() {
        listening = false;
        working = false;
        picker.roundOver();
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

    /**
     * Beskedet syns två gånger: som toast nu, och i notisen efteråt.
     *
     * Toasten ger beskedet med en gång, notisen ger en väg tillbaka till det.
     * Den som inte hann läsa drar ned notisfältet, och texten står kvar.
     */
    private void say(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        if (!alive) return;
        NotificationManager shade = getSystemService(NotificationManager.class);
        if (shade != null) shade.notify(NOTE_ID, sheet(text));
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
            //
            // Har telefonen stängt av animationer står den stilla. iOS har
            // stillat ringarna, glöden och markören mot
            // accessibilityReduceMotion sedan början; Android gjorde
            // ingenting, och bubblan andades vad användaren än hade ställt
            // in. Att mikrofonen är på syns ändå: knappen säger Klar och är
            // djupare grön.
            if (Motion.paused(Bubble.this)) {
                knob.setScaleX(1f);
                knob.setScaleY(1f);
                return;
            }
            float grow = 1f + Math.max(0f, Math.min(rms, 10f)) / 45f;
            knob.setScaleX(grow);
            knob.setScaleY(grow);
        }

        @Override public void onBufferReceived(byte[] buffer) { }

        @Override public void onEndOfSpeech() {
            listening = false;
            shift();
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
            // Enhetstolkningen kan vara oduglig för svenska på just den
            // här telefonen. Då görs omgången om på den vanliga
            // taltjänsten, en gång, utan att släppa mikrofonen och utan
            // att kasta något som redan hörts.
            if (picker.swapToCloud(code, heard.length() > 0) && retry()) return;
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
            picker.roundOver();
            hand.removeCallbacks(giveUp);
            beat(false);
            Mic.release(Bubble.this);
            shift();
        }
    }
}
