package se.rostsvar.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

public class Home extends Activity {
    private LinearLayout page;
    private TextView status;
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(48), dp(22), dp(52));
        scroll.addView(page);
        setContentView(scroll);
        label("Tap & Talk", 32);
        label("Öppna en chatt och tryck på den flytande knappen Tala. Läs texten och skicka med chattens egen knapp.", 18);
        status = label("", 16);
        button("Tillåt visning över appar", () -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        button("Tillåt mikrofon", () -> requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1));
        button("Starta flytande knapp", () -> {
            if (ready()) startForegroundService(new Intent(this, Bubble.class));
            else Toast.makeText(this, "Aktivera båda behörigheterna först.", Toast.LENGTH_LONG).show();
        });
        button("Stäng av knappen", () -> stopService(new Intent(this, Bubble.class)));
        label("Infoga direkt i chatten", 23);
        label("Aktivera och välj Tap & Talk som tangentbord för direkt infogning. Med ditt vanliga tangentbord kan du välja Kopiera och sedan klistra in. Talet behandlas av telefonens taltjänst som kan använda internet.", 16);
        button("Aktivera tangentbord", () -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        button("Välj tangentbord", () -> getSystemService(InputMethodManager.class).showInputMethodPicker());
    }
    private boolean ready() {
        return Settings.canDrawOverlays(this) && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    @Override public void onResume() { super.onResume(); refresh(); }
    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] result) {
        super.onRequestPermissionsResult(code, perms, result);
        refresh();
    }
    private void refresh() {
        status.setText("Visa över appar · " + (Settings.canDrawOverlays(this) ? "tillåtet" : "behövs") +
            "\nMikrofon · " + (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ? "tillåten" : "behövs"));
    }
    private TextView label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size); view.setPadding(0, dp(8), 0, dp(8));
        page.addView(view); return view;
    }
    private void button(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text); button.setAllCaps(false);
        button.setOnClickListener(v -> action.run()); page.addView(button);
    }
}
