package config;

import java.util.prefs.Preferences;

public class TIDEPreferences {
private static final Preferences prefs = Preferences.userNodeForPackage(TIDEPreferences.class);

// Speichern
public static void saveLastFolder(String path)   { prefs.put("lastFolder", path); }
public static void saveLanguage(String lang)     { prefs.put("language", lang); }
public static void saveMode(String mode)         { prefs.put("mode", mode); }
public static void saveWindowWidth(int w)        { prefs.putInt("windowWidth", w); }
public static void saveWindowHeight(int h)       { prefs.putInt("windowHeight", h); }
public static void saveDividerH(int pos)         { prefs.putInt("dividerH", pos); }
public static void saveDividerV(int pos)         { prefs.putInt("dividerV", pos); }

// Laden (mit Standardwert falls noch nichts gespeichert)
public static String getLastFolder()   { return prefs.get("lastFolder", null); }
public static String getLanguage()     { return prefs.get("language", "DE"); }
public static String getMode()         { return prefs.get("mode", "Java"); }
public static int getWindowWidth()     { return prefs.getInt("windowWidth", 1600); }
public static int getWindowHeight()    { return prefs.getInt("windowHeight", 900); }
public static int getDividerH()        { return prefs.getInt("dividerH", -1); }
public static int getDividerV()        { return prefs.getInt("dividerV", -1); }

public static void saveDividerHProportion(double value) {
    prefs.putDouble("dividerHProportion", value);
}

public static void saveDividerVProportion(double value) {
    prefs.putDouble("dividerVProportion", value);
}

public static double getDividerHProportion() {
    return prefs.getDouble("dividerHProportion", 0.20); // Dateibaum 20%
}

public static double getDividerVProportion() {
    return prefs.getDouble("dividerVProportion", 0.70); // Editor 70%, Terminal 30%
}


}