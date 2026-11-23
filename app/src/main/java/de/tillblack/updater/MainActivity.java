package de.tillblack.updater;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Infoseite: erklärt, wofür der Helfer da ist. Keine Befehle, nur ein Link zu GitHub. */
public class MainActivity extends Activity {

    private FrameLayout root;
    private ScrollView scroll;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        T.load(this);
        Ui.colors(this);
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | (Ui.light() ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0));
        }
        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.bg);
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Ui.bg));
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 30 && w.getInsetsController() != null) {
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            w.getInsetsController().setSystemBarsAppearance(Ui.light() ? mask : 0, mask);
        }
        root.setOnApplyWindowInsetsListener((v, in) -> {
            int top, bottom, left, right;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = i.top; bottom = i.bottom; left = i.left; right = i.right;
            } else {
                top = in.getSystemWindowInsetTop(); bottom = in.getSystemWindowInsetBottom();
                left = in.getSystemWindowInsetLeft(); right = in.getSystemWindowInsetRight();
            }
            if (scroll != null) scroll.setPadding(left + dp(24), top + dp(32), right + dp(24), bottom + dp(24));
            return in;
        });
        build();
    }

    private int dp(float v) { return Ui.dp(this, v); }

    private void build() {
        root.removeAllViews();
        scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(box);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(new Ui.Mark(this), new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(16), 0, 0, 0);
        titles.addView(Ui.text(this, "Updater", 28, Ui.text, true));
        titles.addView(Ui.text(this, T.subtitle(), 15, Ui.dim, false));
        head.addView(titles);
        box.addView(head);

        box.addView(section(T.whatTitle()));
        TextView what = Ui.text(this, T.what(), 15, Ui.text, false);
        what.setLineSpacing(0, 1.25f);
        box.addView(card(what));

        box.addView(section(T.safeTitle()));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (String s : T.safe()) {
            LinearLayout r = new LinearLayout(this);
            r.setPadding(0, dp(5), 0, dp(5));
            View dot = new View(this);
            dot.setBackground(Ui.round(this, Ui.accent, 0, 3));
            LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(dp(6), dp(6));
            dp1.setMargins(0, dp(8), dp(12), 0);
            r.addView(dot, dp1);
            TextView t = Ui.text(this, s, 15, Ui.text, false);
            t.setLineSpacing(0, 1.2f);
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
            list.addView(r);
        }
        box.addView(card(list));

        TextView gh = Ui.text(this, T.github(), 16, Ui.accent, true);
        gh.setGravity(Gravity.CENTER);
        gh.setPadding(dp(16), dp(14), dp(16), dp(14));
        gh.setBackground(Ui.round(this, 0, Ui.line, 12));
        gh.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TillBlack"))); } catch (Exception ignored) { }
        });
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
        gp.setMargins(0, dp(24), 0, 0);
        box.addView(gh, gp);

        // Sprache und Version
        LinearLayout foot = new LinearLayout(this);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        foot.setPadding(0, dp(18), 0, 0);
        String ver = "";
        try { ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) { }
        foot.addView(Ui.text(this, T.version(ver), 13, Ui.dim, false), new LinearLayout.LayoutParams(0, -2, 1f));
        foot.addView(langChip("English", !T.de, false));
        foot.addView(langChip("Deutsch", T.de, true));
        box.addView(foot);
    }

    private TextView langChip(String label, boolean on, boolean german) {
        TextView c = Ui.text(this, label, 13, on ? 0xFFFFFFFF : Ui.text, on);
        c.setPadding(dp(12), dp(6), dp(12), dp(6));
        c.setBackground(Ui.round(this, on ? Ui.accent : Ui.card, on ? 0 : Ui.line, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(8), 0, 0, 0);
        c.setLayoutParams(lp);
        c.setOnClickListener(v -> { T.set(this, german); build(); root.requestApplyInsets(); });
        return c;
    }

    private TextView section(String s) {
        TextView t = Ui.text(this, s, 13, Ui.dim, true);
        t.setPadding(dp(4), dp(24), 0, dp(8));
        return t;
    }

    private View card(View content) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(Ui.round(this, Ui.card, Ui.line, 14));
        f.setPadding(dp(16), dp(14), dp(16), dp(14));
        f.addView(content);
        return f;
    }
}
