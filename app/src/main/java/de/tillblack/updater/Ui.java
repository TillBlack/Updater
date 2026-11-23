package de.tillblack.updater;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

/** Farben (folgen dem Hell/Dunkel des Systems) und kleine Bausteine. */
final class Ui {
    static int bg, card, line, text, dim, accent;

    static void colors(Context c) {
        boolean night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (night) {
            bg = 0xFF0F1112; card = 0xFF1A1D1F; line = 0xFF2D3235; text = 0xFFEBEDEE; dim = 0xFF8B9296; accent = 0xFF5B9BD5;
        } else {
            bg = 0xFFF2F2F0; card = 0xFFFFFFFF; line = 0xFFDADBDE; text = 0xFF16171A; dim = 0xFF62676F; accent = 0xFF2F6FB0;
        }
    }

    static boolean light() { return bg == 0xFFF2F2F0; }

    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    static GradientDrawable round(Context c, int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) g.setStroke(Math.max(1, dp(c, 1)), stroke);
        return g;
    }

    /** Das Zeichen der App: Kreis mit Pfeil nach unten in eine Schale. */
    static final class Mark extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        Mark(Context c) { super(c); }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) / 2f, cx = w / 2, cy = h / 2;
            p.setStyle(Paint.Style.FILL);
            p.setColor(accent);
            c.drawCircle(cx, cy, r, p);
            p.setColor(0xFFFFFFFF);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setStrokeWidth(r * 0.12f);
            c.drawLine(cx, cy - r * 0.48f, cx, cy + r * 0.1f, p);
            path.reset();
            path.moveTo(cx - r * 0.24f, cy - r * 0.12f);
            path.lineTo(cx, cy + r * 0.12f);
            path.lineTo(cx + r * 0.24f, cy - r * 0.12f);
            c.drawPath(path, p);
            path.reset();
            path.moveTo(cx - r * 0.45f, cy + r * 0.12f);
            path.lineTo(cx - r * 0.45f, cy + r * 0.42f);
            path.lineTo(cx + r * 0.45f, cy + r * 0.42f);
            path.lineTo(cx + r * 0.45f, cy + r * 0.12f);
            c.drawPath(path, p);
        }
    }
}
