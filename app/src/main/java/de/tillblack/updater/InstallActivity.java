package de.tillblack.updater;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Wird von Tills Apps gestartet: lädt ein Update und übergibt es an Android zur Bestätigung.
 * Sicherheit: nur Downloads von github.com/TillBlack, nur Updates bereits installierter Apps,
 * nur mit derselben Signatur wie die installierte App. Danach schließt sich der Helfer sofort.
 *
 * Aufruf: Intent "de.tillblack.updater.INSTALL" mit Extras "url", "package", "name".
 */
public class InstallActivity extends Activity {

    private static WeakReference<InstallActivity> current = new WeakReference<>(null);

    private TextView status;
    private ProgressBar bar;
    private boolean confirmShown;
    private volatile boolean cancelled;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        T.load(this);
        Ui.colors(this);
        current = new WeakReference<>(this);
        String url = getIntent().getStringExtra("url");
        String pkg = getIntent().getStringExtra("package");
        String name = getIntent().getStringExtra("name");
        if (name == null || name.length() > 40) name = pkg;

        if (!allowedUrl(url) || pkg == null || !installed(pkg)) {
            done(T.refused());
            return;
        }
        if (!getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(this, T.allow(), Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) { }
            finish();
            return;
        }
        buildCard(T.updating(name));
        String app = name;
        new Thread(() -> run(url, pkg, app)).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Android-Bestätigung wurde geschlossen: die Installation läuft (oder wurde abgebrochen) – Helfer beenden
        if (confirmShown) finish();
    }

    @Override
    protected void onDestroy() {
        cancelled = true;
        super.onDestroy();
    }

    private void buildCard(String title) {
        FrameLayout root = new FrameLayout(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.round(this, Ui.card, Ui.line, 16));
        int p = Ui.dp(this, 20);
        card.setPadding(p, p, p, Ui.dp(this, 12));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(new Ui.Mark(this), new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36)));
        TextView t = Ui.text(this, title, 17, Ui.text, true);
        t.setPadding(Ui.dp(this, 12), 0, 0, 0);
        head.addView(t);
        card.addView(head);
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setIndeterminate(true);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.accent));
        bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Ui.accent));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, Ui.dp(this, 16), 0, 0);
        card.addView(bar, bp);
        status = Ui.text(this, "", 13, Ui.dim, false);
        status.setPadding(0, Ui.dp(this, 6), 0, 0);
        card.addView(status);
        TextView cancel = Ui.text(this, T.cancel(), 15, Ui.accent, true);
        cancel.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 6));
        cancel.setOnClickListener(v -> { cancelled = true; finish(); });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.gravity = Gravity.END;
        card.addView(cancel, cp);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 48), Ui.dp(this, 420));
        root.addView(card, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER));
        setContentView(root);
    }

    private void setStatus(String s, int percent) {
        runOnUiThread(() -> {
            if (status != null) status.setText(s);
            if (bar != null && percent >= 0) { bar.setIndeterminate(false); bar.setProgress(percent); }
        });
    }

    // ------------------------------------------------------------------ Prüfungen

    static boolean allowedUrl(String url) {
        if (url == null) return false;
        try {
            Uri u = Uri.parse(url);
            String path = u.getPath() == null ? "" : u.getPath();
            return "https".equals(u.getScheme()) && "github.com".equalsIgnoreCase(u.getHost())
                    && path.toLowerCase(Locale.ROOT).startsWith("/tillblack/")
                    && path.toLowerCase(Locale.ROOT).endsWith(".apk")
                    && !path.contains("..");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean installed(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Set<String> signers(PackageInfo pi) {
        Set<String> out = new HashSet<>();
        Signature[] sigs = null;
        if (Build.VERSION.SDK_INT >= 28 && pi.signingInfo != null) {
            sigs = pi.signingInfo.hasMultipleSigners() ? pi.signingInfo.getApkContentsSigners() : pi.signingInfo.getSigningCertificateHistory();
        } else if (pi.signatures != null) {
            sigs = pi.signatures;
        }
        if (sigs != null) for (Signature s : sigs) out.add(Arrays.toString(s.toByteArray()).hashCode() + ":" + s.toCharsString());
        return out;
    }

    /** Das heruntergeladene Paket muss dieselbe App mit derselben Signatur und höherer Version sein. */
    private boolean matches(File apk, String pkg) {
        PackageManager pm = getPackageManager();
        int flag = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo archive = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flag);
        if (archive == null || !pkg.equals(archive.packageName)) return false;
        if (archive.applicationInfo != null) {
            archive.applicationInfo.sourceDir = apk.getAbsolutePath();
            archive.applicationInfo.publicSourceDir = apk.getAbsolutePath();
        }
        try {
            PackageInfo inst = pm.getPackageInfo(pkg, flag);
            if (archive.getLongVersionCode() <= inst.getLongVersionCode()) return false;
            Set<String> a = signers(archive), i = signers(inst);
            if (a.isEmpty() || i.isEmpty()) return false;
            for (String s : a) if (i.contains(s)) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ Ablauf

    private void run(String url, String pkg, String name) {
        File apk = new File(getCacheDir(), "update.apk");
        try {
            // herunterladen
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "Updater");
            if (c.getResponseCode() / 100 != 2) throw new Exception("HTTP " + c.getResponseCode());
            long total = c.getContentLengthLong();
            try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(apk)) {
                byte[] buf = new byte[65536];
                long got = 0;
                int n, last = -1;
                while ((n = in.read(buf)) > 0) {
                    if (cancelled) return;
                    out.write(buf, 0, n);
                    got += n;
                    if (got > 300L * 1024 * 1024) throw new Exception("too large");
                    if (total > 0) {
                        int pct = (int) (got * 100 / total);
                        if (pct != last) { last = pct; setStatus(pct + " %", pct); }
                    }
                }
            }
            if (cancelled) return;
            setStatus(T.checking(), 100);
            if (!matches(apk, pkg)) { runOnUiThread(() -> done(T.refused())); return; }

            // an Android übergeben
            PackageInstaller pi = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(pkg);
            params.setSize(apk.length());
            if (Build.VERSION.SDK_INT >= 31) params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED);
            int id = pi.createSession(params);
            try (PackageInstaller.Session s = pi.openSession(id)) {
                try (InputStream in = new FileInputStream(apk); OutputStream out = s.openWrite("base.apk", 0, apk.length())) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    s.fsync(out);
                }
                int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                PendingIntent cb = PendingIntent.getBroadcast(this, id, new Intent(this, Result.class).setPackage(getPackageName()), flags);
                s.commit(cb.getIntentSender());
            }
            setStatus(T.confirm(), 100);
        } catch (Exception e) {
            runOnUiThread(() -> done(T.failed()));
        } finally {
            // Kopie wird nicht mehr gebraucht (die Sitzung hat ihre eigene)
            apk.delete();
        }
    }

    private void done(String message) {
        if (message != null) Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void showConfirm(Intent confirm) {
        confirmShown = true;
        try {
            startActivity(confirm);
        } catch (Exception e) {
            done(T.failed());
        }
    }

    /** Antwort von Android auf die Installation. */
    public static final class Result extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            int st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            InstallActivity a = current.get();
            if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent confirm = Build.VERSION.SDK_INT >= 33
                        ? i.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class)
                        : i.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm == null) return;
                if (a != null && !a.isFinishing()) a.showConfirm(confirm);
                else {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try { c.startActivity(confirm); } catch (Exception ignored) { }
                }
                return;
            }
            if (st != PackageInstaller.STATUS_SUCCESS && st != PackageInstaller.STATUS_FAILURE_ABORTED) {
                T.load(c);
                Toast.makeText(c, T.failed(), Toast.LENGTH_LONG).show();
            }
            if (a != null && !a.isFinishing()) a.finish();
        }
    }
}
