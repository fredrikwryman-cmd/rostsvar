package se.rostsvar.mobile;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

public class VoiceKeys extends InputMethodService {
    // Main-thread only. A session ends when the input view closes or the editor restarts.
    private static Object session;
    private static VoiceKeys active;
    static Object currentSession() { return session; }

    static boolean insert(Object originalSession, String text) {
        if (originalSession == null || originalSession != session || active == null) return false;
        InputConnection field = active.getCurrentInputConnection();
        return field != null && field.commitText(text, 1);
    }

    @Override public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting); clearSession();
    }
    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        boolean ordinaryText = (info.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT
            && variation != InputType.TYPE_TEXT_VARIATION_PASSWORD
            && variation != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            && variation != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
        active = this;
        session = ordinaryText ? new Object() : null;
    }
    private void clearSession() {
        if (active == this) { session = null; active = null; }
    }
    @Override public void onFinishInputView(boolean finishing) { clearSession(); super.onFinishInputView(finishing); }
    @Override public void onFinishInput() { clearSession(); super.onFinishInput(); }
    @Override public void onDestroy() { clearSession(); super.onDestroy(); }
    @Override public View onCreateInputView() {
        LinearLayout row = new LinearLayout(this);
        Button talk = new Button(this);
        talk.setText("Tala"); talk.setOnClickListener(v -> {
            if (!Bubble.fromKeyboard()) android.widget.Toast.makeText(this, "Starta den flytande knappen i Tap & Talk.", 1).show();
        });
        row.addView(talk);
        Button switcher = new Button(this);
        switcher.setText("Byt tangentbord");
        switcher.setOnClickListener(v -> getSystemService(InputMethodManager.class).showInputMethodPicker());
        row.addView(switcher); return row;
    }
}
