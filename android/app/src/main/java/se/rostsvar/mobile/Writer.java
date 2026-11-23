package se.rostsvar.mobile;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Tjänsten som skriver den dikterade texten i det fält användaren står i.
 *
 * Det här är enda vägen för en app att skriva i ett annat program på Android.
 * Tjänsten gör därför exakt en sak: den frågar systemet vilket textfält som
 * har skrivfokus och lägger texten efter den som redan står där.
 *
 * Den lyssnar inte på händelser, läser inga andra fält, loggar ingenting,
 * sparar ingenting och når inget nät. Samma mening står i beskrivningen som
 * Android visar i tillgänglighetsinställningarna.
 */
public class Writer extends AccessibilityService {

    /** Den anslutna tjänsten. Android håller bara en instans i livet. */
    private static Writer live;

    // ------------------------------------------------------------------ läget

    /** Sant när tjänsten står som påslagen i Androids inställningar. */
    static boolean enabled(Context context) {
        AccessibilityManager helper = context.getSystemService(AccessibilityManager.class);
        if (helper == null) return false;
        List<AccessibilityServiceInfo> list =
                helper.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        if (list == null) return false;
        String mine = context.getPackageName() + "/";
        for (AccessibilityServiceInfo info : list) {
            String id = info.getId();
            if (id != null && id.startsWith(mine)) return true;
        }
        return false;
    }

    /** Sant när tjänsten verkligen är uppkopplad och kan skriva just nu. */
    static boolean ready() {
        return live != null;
    }

    /**
     * Vilket program som äger det fält som har skrivfokus, eller null när
     * inget fält har det och när tjänsten inte är påslagen.
     *
     * Bubblan frågar om det här innan den börjar lyssna och en gång till
     * innan den skriver. Har användaren bytt app under vägen ska texten inte
     * skrivas in i ett fält hon inte talade till.
     */
    static String focusedPackage() {
        Writer now = live;
        if (now == null) return null;
        try {
            AccessibilityNodeInfo node = now.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (node == null) return null;
            CharSequence name = node.getPackageName();
            return name == null ? null : name.toString();
        } catch (RuntimeException broken) {
            // Fönstret kan ha rivits mellan frågan och svaret. Vet vi inte
            // vilket program det är, påstår vi inget.
            return null;
        }
    }

    // ------------------------------------------------------------- livscykeln

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        live = this;
    }

    @Override public boolean onUnbind(Intent intent) {
        live = null;
        return super.onUnbind(intent);
    }

    @Override public void onDestroy() {
        live = null;
        super.onDestroy();
    }

    /**
     * Vi prenumererar inte på något innehåll. Metoden måste finnas, men
     * lämnas tom med flit: tjänsten läser bara när bubblan ber om det.
     */
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override public void onInterrupt() { }

    // ------------------------------------------------------------- skrivandet

    /**
     * Lägger texten i det fokuserade fältet.
     *
     * @return sant när texten kom in, falskt när inget skrivbart fält hade
     *         fokus. Falskt betyder att den som ringde måste rädda texten
     *         på annat sätt, och det gör {@link Bubble} med urklippet.
     */
    static boolean write(String text) {
        Writer now = live;
        if (now == null || text == null || text.length() == 0) return false;
        try {
            return now.put(text);
        } catch (RuntimeException broken) {
            // Fältet kan ha försvunnit mellan frågan och skrivningen. Då är
            // det ett misslyckande som alla andra, och texten räddas av den
            // som ringde.
            return false;
        }
    }

    private boolean put(String text) {
        AccessibilityNodeInfo node = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (node == null) return false;
        // Noder behöver inte lämnas tillbaka längre; recycle() är en tom
        // metod från Android 13 och bortvald här med flit.
        if (!node.isEditable()) return false;

        String before = "";
        CharSequence had = node.getText();
        // Ett tomt fält kan svara med sin egen ledtext. Den ska inte med.
        if (had != null && !node.isShowingHintText()) before = had.toString();

        String joined = join(before, text);

        Bundle what = new Bundle();
        what.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, joined);
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, what)) return false;

        // Markören till slutet, så att nästa diktering hamnar efter och inte
        // mitt i det som just skrevs.
        Bundle where = new Bundle();
        where.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, joined.length());
        where.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, joined.length());
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, where);
        return true;
    }

    /** Ett mellanslag mellan det gamla och det nya, men aldrig två. */
    private static String join(String before, String text) {
        if (before.length() == 0) return text;
        char last = before.charAt(before.length() - 1);
        if (last == ' ' || last == '\n' || last == '\t') return before + text;
        return before + " " + text;
    }
}
