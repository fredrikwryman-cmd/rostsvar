package se.rostsvar.mobile;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.speech.*;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

public class Bubble extends Service {
    private static Bubble running;
    private WindowManager windows;
    private WindowManager.LayoutParams layout;
    private LinearLayout panel;
    private SpeechRecognizer speech;
    private Object editor;
    private Button handle;
    private boolean attached;
    private final Handler timer = new Handler(Looper.getMainLooper());
    private final Runnable timeout = () -> problem("Tiden gick ut. Försök igen.");
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    static boolean fromKeyboard() {
        if (running == null || !running.attached) return false;
        if (running.panel.getChildCount() == 1) running.listen();
        return true;
    }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onCreate() {
        super.onCreate();
        windows = getSystemService(WindowManager.class);
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel("bubble", "Flytande knapp", NotificationManager.IMPORTANCE_LOW));
        PendingIntent home = PendingIntent.getActivity(this, 0, new Intent(this, Home.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, Bubble.class).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE);
        Notification note = new Notification.Builder(this, "bubble").setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Tap & Talk är aktivt").setContentText("Tryck på Tala i chatten för att diktera.")
            .setContentIntent(home).setOngoing(true).addAction(new Notification.Action.Builder(null, "Stäng av", stop).build()).build();
        startForeground(1, note);
        running = this;
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if ((intent != null && "STOP".equals(intent.getAction())) || !Settings.canDrawOverlays(this)) {
            stopSelf(); return START_NOT_STICKY;
        }
        if (!attached) {
            panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(dp(4), dp(4), dp(4), dp(4));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(30, 45, 62)); bg.setCornerRadius(dp(18));
            panel.setBackground(bg);
            layout = new WindowManager.LayoutParams(-2, -2, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_SECURE, PixelFormat.TRANSLUCENT);
            layout.gravity = Gravity.TOP | Gravity.LEFT;
            layout.x = dp(12); layout.y = dp(110);
            compact();
            try { windows.addView(panel, layout); attached = true; }
            catch (RuntimeException e) { stopSelf(); }
        }
        return START_NOT_STICKY;
    }
    private Button action(String label, Runnable fn) {
        Button b = new Button(this); b.setAllCaps(false); b.setText(label);
        b.setOnClickListener(v -> fn.run()); panel.addView(b); return b;
    }
    private void compact() {
        releaseSpeech();
        editor = null;
        panel.removeAllViews();
        handle = action("Tala", this::listen);
        handle.setContentDescription("Tala in svar. Dra för att flytta.");
        handle.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY; int startX, startY; boolean moved;
            public boolean onTouch(View view, MotionEvent event) {
                float x = event.getRawX(), y = event.getRawY();
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX=x; downY=y; startX=layout.x; startY=layout.y; moved=false; return true;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.hypot(x-downX, y-downY) > ViewConfiguration.get(Bubble.this).getScaledTouchSlop()) moved=true;
                        if (moved) {
                            android.graphics.Rect bounds = windows.getCurrentWindowMetrics().getBounds();
                            layout.x=Math.max(0, Math.min(bounds.width()-panel.getWidth(), startX+Math.round(x-downX)));
                            layout.y=Math.max(0, Math.min(bounds.height()-panel.getHeight(), startY+Math.round(y-downY)));
                            windows.updateViewLayout(panel, layout);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) view.performClick();
                        return true;
                    default: return true;
                }
            }
        });
    }
    private TextView caption(String value) {
        TextView t = new TextView(this);
        t.setTextSize(16); t.setTextColor(Color.WHITE); t.setPadding(dp(8),dp(6),dp(8),dp(6));
        t.setMaxWidth(dp(270)); t.setText(value);
        return t;
    }
    private void listen() {
        releaseSpeech();
        editor = VoiceKeys.currentSession();
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            problem("Ingen taltjänst hittades. Aktivera taligenkänning på telefonen."); return;
        }
        panel.removeAllViews();
        panel.addView(caption("Lyssnar på svenska …"));
        Button done = action("Klar", () -> { if (speech != null) speech.stopListening(); });
        done.setEnabled(false);
        action("Avbryt", this::compact);
        try {
            final SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speech = recognizer;
            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) { if (speech == recognizer) done.setEnabled(true); }
                public void onBeginningOfSpeech() {}
                public void onRmsChanged(float volume) {}
                public void onBufferReceived(byte[] bytes) {}
                public void onEndOfSpeech() { if (speech == recognizer) done.setEnabled(false); }
                public void onPartialResults(Bundle results) {}
                public void onEvent(int code, Bundle b) {}
                public void onError(int code) {
                    if (speech != recognizer) return;
                    String message;
                    switch (code) {
                        case SpeechRecognizer.ERROR_NO_MATCH:
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message="Inget tal uppfattades."; break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message="Mikrofonen behöver tillåtas i inställningarna."; break;
                        case SpeechRecognizer.ERROR_NETWORK:
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: message="Taltjänsten saknar anslutning."; break;
                        default: message="Taltjänsten kunde inte slutföra dikteringen (" + code + ").";
                    }
                    problem(message);
                }
                public void onResults(Bundle b) {
                    if (speech != recognizer) return;
                    ArrayList<String> words = b == null ? null : b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (words == null || words.isEmpty() || words.get(0) == null || words.get(0).trim().isEmpty()) {
                        problem("Inget tal uppfattades."); return;
                    }
                    review(words.get(0).trim());
                }
            });
            Intent req = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            req.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            req.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "sv-SE");
            req.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            recognizer.startListening(req);
            timer.postDelayed(timeout, 60000);
        } catch (RuntimeException e) { problem("Taltjänsten kunde inte starta."); }
    }
    private void review(String text) {
        releaseSpeech();
        panel.removeAllViews();
        ScrollView scroll = new ScrollView(this); scroll.addView(caption(text));
        panel.addView(scroll, -2, dp(84));
        if (editor != null && editor == VoiceKeys.currentSession()) {
            action("Infoga i chatten", () -> {
                if (VoiceKeys.insert(editor, text)) compact();
                else { editor = null; review(text); Toast.makeText(this, "Skrivfältet har ändrats. Kopiera vid behov.", 1).show(); }
            });
        }
        action("Kopiera", () -> {
            ClipData clip = ClipData.newPlainText("Tap & Talk", text);
            if (Build.VERSION.SDK_INT >= 33) {
                PersistableBundle extras = new PersistableBundle();
                extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
                clip.getDescription().setExtras(extras);
            }
            getSystemService(ClipboardManager.class).setPrimaryClip(clip);
            compact(); Toast.makeText(this, "Klistra in i chatten och tryck på Skicka.", 1).show();
        });
        action("Kasta", this::compact);
    }
    private void problem(String text) {
        releaseSpeech();
        panel.removeAllViews(); panel.addView(caption(text));
        action("Försök igen", this::listen); action("Stäng", this::compact);
    }
    private void releaseSpeech() {
        timer.removeCallbacks(timeout);
        SpeechRecognizer old = speech; speech = null;
        if (old != null) { old.cancel(); old.destroy(); }
    }
    @Override public void onDestroy() {
        running = null; releaseSpeech();
        if (attached) { windows.removeView(panel); attached=false; }
        stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
}
