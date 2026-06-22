import com.formdev.flatlaf.FlatDarkLaf;

import ui.MainWindow;
import update.UpdateManager;
import config.TIDEPreferences;
import config.Theme;

import javax.swing.*;
import java.awt.*;

public class TIDE {

    public static void main(String[] args) {

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
        UIManager.put("Component.arc",                8);
        UIManager.put("Button.arc",                   8);
        UIManager.put("TextComponent.arc",            8);
        UIManager.put("ScrollBar.thumbArc",           8);
        UIManager.put("TabbedPane.selectedBackground", currentTheme.backgroundLight);
        UIManager.put("TabbedPane.showTabSeparators",  true);

        UIManager.put("Button.background", currentTheme.backgroundLight);
        UIManager.put("Button.hoverBackground", currentTheme.backgroundHover);

        // ── 3. GUI START ───────────────────────────────────────────────────
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}