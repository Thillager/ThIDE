import com.formdev.flatlaf.FlatDarkLaf;

import ui.MainWindow;
import update.UpdateManager;
import config.TIDEPreferences;
import config.Theme;

import javax.swing.*;
import java.awt.*;

public class TIDE {

	public static void main(String[] args) {

		try {
			Toolkit.getDefaultToolkit().getDesktopProperty("awt.appID");
			System.setProperty("sun.awt.warmup", "false");
			// Setze hier einen eindeutigen Namen für deine App (z.B. "Thillager.ThIDE.4.7")
			Class<?> shellClass = Class.forName("com.sun.jna.platform.win32.Shell32");
			// Alternativ über jni/jna falls im Projekt, ansonsten reicht oft der obige jpackage-Wrapper
		} catch (Exception e) {
			// Ignorieren auf Linux/Mac
		}

		String os = System.getProperty("os.name", "").toLowerCase();

		// ── 1. HARDWARE-BESCHLEUNIGUNG ──────────────────────────────────────
		// Moegliche Werte: "auto" | "on" | "off"
		// "auto" erkennt automatisch ob JAR- oder jpackage-Start

		UpdateManager envCheck = new UpdateManager(null, null, "", "");
		String hwMode = TIDEPreferences.getHwAccelMode();

		boolean enableAccel;
		switch (hwMode) {
			case "on"  -> {
				enableAccel = true;
			}
			case "off" -> {
				enableAccel = false;
			}
			default    -> {
				// auto: JAR-Start = kein HW-Accel, jpackage-Start = HW-Accel an
				enableAccel = !envCheck.isRunningAsJar();
				System.out.println("[TIDE] Hardwarebeschleunigung: AUTO → "
					+ (enableAccel ? "AN (jpackage erkannt)" : "AUS (JAR-Start erkannt)"));
			}
		}

		if (!enableAccel) {
			// Alle potenziell fehlerhaften Grafik-Pipelines deaktivieren
			System.setProperty("sun.java2d.d3d",        "false");
			System.setProperty("sun.java2d.opengl",     "false");
			System.setProperty("sun.java2d.xrender",    "false");
			System.setProperty("swing.bufferPerWindow", "false");
		} else {
			if (os.contains("win")) {
				// Windows: Direct3D ist schneller als OpenGL
				System.setProperty("sun.java2d.d3d",     "true");
				System.setProperty("sun.java2d.noddraw", "false");
			} else {
				// Linux / macOS: OpenGL-Pipeline
				System.setProperty("sun.java2d.opengl",  "true");
			}
			// VolatileImage immer im VRAM halten (wichtig fuer Blur-Effekt)
			System.setProperty("sun.java2d.accthreshold", "0");
		}

		String savedTheme = config.TIDEPreferences.getTheme();
		config.Theme currentTheme = config.Theme.byName(savedTheme);

		// ── 2. FlatLaf UI-Tweaks ───────────────────────────────────────────
		// ── 2. FlatLaf UI-Tweaks & Globales Theme-Skinning ───────────────────────

		// --- Eckenabrundungen (wie gehabt) ---
		UIManager.put("Component.arc",           8);
		UIManager.put("Button.arc",              8);
		UIManager.put("TextComponent.arc",       8);
		UIManager.put("ScrollBar.thumbArc",      8);

		// --- Titelleiste ---
		/* JFrame.setDefaultLookAndFeelDecorated(true);
		UIManager.put("TitlePane.background", new Color(60, 63, 65));
		UIManager.put("TitlePane.inactiveBackground", new Color(75, 78, 80));
		UIManager.put("TitlePane.foreground", Color.WHITE);
		*/

		// --- Registerkarten (Tabs) ---
		UIManager.put("TabbedPane.selectedBackground", currentTheme.backgroundLight);
		UIManager.put("TabbedPane.background",         currentTheme.background);
		UIManager.put("TabbedPane.foreground",         currentTheme.foreground);
		UIManager.put("TabbedPane.showTabSeparators",  true);

		// --- Dropdowns & Menüs (Das lästige Blau entfernen) ---
		UIManager.put("ComboBox.selectionBackground",   currentTheme.backgroundHover);
		UIManager.put("ComboBox.selectionForeground",   currentTheme.foreground);
		UIManager.put("ComboBox.background",            currentTheme.backgroundLight);
		UIManager.put("ComboBox.foreground",            currentTheme.foreground);

		UIManager.put("MenuItem.selectionBackground",   currentTheme.backgroundHover);
		UIManager.put("MenuItem.selectionForeground",   currentTheme.foreground);
		UIManager.put("MenuItem.background",            currentTheme.backgroundLight);
		UIManager.put("MenuItem.foreground",            currentTheme.foreground);
		UIManager.put("PopupMenu.background",           currentTheme.backgroundLight);

		// --- Dateibaum (Links) & Listen ---
		UIManager.put("Tree.selectionBackground",       currentTheme.backgroundHover);
		UIManager.put("Tree.selectionForeground",       currentTheme.foreground);
		UIManager.put("Tree.background",                currentTheme.background);
		UIManager.put("Tree.foreground",                currentTheme.foreground);

		UIManager.put("List.selectionBackground",       currentTheme.backgroundHover);
		UIManager.put("List.selectionForeground",       currentTheme.foreground);

		// --- Buttons allgemein ---
		UIManager.put("Button.background",             currentTheme.backgroundLight);
		UIManager.put("Button.foreground",             currentTheme.foreground);
		UIManager.put("Button.hoverBackground",        currentTheme.backgroundHover);
		UIManager.put("Button.focusedBackground",      currentTheme.backgroundLight);

		UIManager.put("MenuBar.hoverBackground",           currentTheme.backgroundHover);
		UIManager.put("MenuBar.selectionBackground",       currentTheme.backgroundHover);
		UIManager.put("MenuBar.selectionForeground",       currentTheme.foreground);

		// --- Fokus-Rahmen & Trennlinien (Sehr wichtig für den Look) ---
		UIManager.put("Component.focusColor",           currentTheme.accent);
		UIManager.put("Separator.foreground",           currentTheme.border);

		// --- Scrollbalken (ScrollBars) ---
		UIManager.put("ScrollBar.thumbColor",           currentTheme.backgroundHover);
		UIManager.put("ScrollBar.track",                currentTheme.background);

		UIManager.put("Button.default.background",         currentTheme.backgroundLight);
		UIManager.put("Button.default.foreground",         currentTheme.foreground);
		UIManager.put("Button.default.hoverBackground",    currentTheme.backgroundHover);
		UIManager.put("Button.default.focusedBackground",  currentTheme.backgroundLight);
		UIManager.put("Button.default.focusColor",         currentTheme.accent);

		// ── 3. GUI START ───────────────────────────────────────────────────
		SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
	}
}