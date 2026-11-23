package de.tillblack.updater;

import android.content.Context;

/** Texte auf Englisch und Deutsch. */
final class T {
    static boolean de;

    static void load(Context c) {
        String saved = c.getSharedPreferences("updater", Context.MODE_PRIVATE).getString("lang", null);
        de = saved != null ? saved.equals("de") : java.util.Locale.getDefault().getLanguage().equals("de");
    }

    static void set(Context c, boolean german) {
        de = german;
        c.getSharedPreferences("updater", Context.MODE_PRIVATE).edit().putString("lang", german ? "de" : "en").apply();
    }

    static String s(String en, String deText) { return de ? deText : en; }

    static String subtitle() { return s("Keeps Till's apps up to date", "Hält Tills Apps aktuell"); }
    static String whatTitle() { return s("What is this?", "Was ist das?"); }
    static String what() { return s(
            "This small helper installs updates for apps made by TillBlack, for example Nachrichten. When one of these apps finds a new version and you tap \"Update\", it hands the download to this helper. Android then asks you to confirm, and the helper closes again.",
            "Dieser kleine Helfer installiert Updates für Apps von TillBlack, zum Beispiel Nachrichten. Wenn eine dieser Apps eine neue Version findet und du auf \"Aktualisieren\" tippst, gibt sie den Download an diesen Helfer weiter. Android fragt dich dann zur Bestätigung, und der Helfer schließt sich wieder."); }
    static String safeTitle() { return s("What it does and doesn't do", "Was er tut und was nicht"); }
    static String[] safe() {
        return de ? new String[]{
                "Lädt Updates nur von github.com/TillBlack.",
                "Aktualisiert nur Apps, die schon installiert sind und mit demselben Schlüssel signiert wurden. Andere oder neue Apps lehnt er ab.",
                "Installiert nie etwas ohne deine Bestätigung.",
                "Läuft nicht im Hintergrund, sammelt keine Daten und hat keine Werbung.",
                "Du musst hier nichts tun. Du kannst dieses Fenster einfach schließen."
        } : new String[]{
                "Downloads updates only from github.com/TillBlack.",
                "Only updates apps that are already installed and signed with the same key. Other or new apps are refused.",
                "Never installs anything without your confirmation.",
                "Does not run in the background, collects no data and has no ads.",
                "There is nothing to do here. You can simply close this window."
        };
    }
    static String github() { return s("Till on GitHub", "Till auf GitHub"); }
    static String version(String v) { return s("Version ", "Version ") + v; }

    // Installation
    static String updating(String app) { return s("Updating " + app + "…", app + " wird aktualisiert…"); }
    static String checking() { return s("Checking the download…", "Download wird geprüft…"); }
    static String confirm() { return s("Please confirm the installation.", "Bitte bestätige die Installation."); }
    static String allow() { return s("Please allow \"Install unknown apps\" for Updater once. Then tap Update again in the app.",
            "Bitte erlaube einmalig \"Unbekannte Apps installieren\" für Updater. Tippe danach in der App nochmal auf Aktualisieren."); }
    static String refused() { return s("This update was refused: it does not come from TillBlack or does not match the installed app.",
            "Dieses Update wurde abgelehnt: Es stammt nicht von TillBlack oder passt nicht zur installierten App."); }
    static String failed() { return s("The update could not be installed. Please try again later.",
            "Das Update konnte nicht installiert werden. Bitte versuch es später nochmal."); }
    static String cancel() { return s("Cancel", "Abbrechen"); }
}
