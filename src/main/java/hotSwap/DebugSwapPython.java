package runner;

import config.LanguageManager;
import editor.CompilerErrorMarker;
import editor.EditorManager;
import ui.ConsolePanel;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * DebugSwapPython implementiert Debug für Python-Programme mit Restart-Fähigkeit
 */
public class DebugSwapPython implements DebugRunner.DebugStrategy {

    private final ConsolePanel consolePanel;
    private final EditorManager editorManager;
    private final CompilerErrorMarker errorMarker;
    private final File currentProjectFolder;
    private static final int DEBUG_PORT = 5678;

    public DebugSwapPython(ConsolePanel consolePanel, EditorManager editorManager,
                           CompilerErrorMarker errorMarker, File currentProjectFolder) {
        this.consolePanel = consolePanel;
        this.editorManager = editorManager;
        this.errorMarker = errorMarker;
        this.currentProjectFolder = currentProjectFolder;
    }

    @Override
    public Process execute(String mainClass) throws InterruptedException {
        File activePy = editorManager.getActiveFile();

        if (activePy == null) {
            consolePanel.log("[DEBUG PYTHON] " + LanguageManager.t("no_python_file") + "\n", Color.RED);
            return null;
        }

        if (!activePy.getName().endsWith(".py")) {
            consolePanel.log("[DEBUG PYTHON] " + LanguageManager.t("no_python_file") + "\n", Color.RED);
            return null;
        }

        consolePanel.log("[DEBUG PYTHON] Starte Debugger für " + activePy.getName() + "...\n", Color.CYAN);

        String pythonCmd = System.getProperty("os.name").toLowerCase().contains("win")
                ? "python"
                : "python3";

        // Python mit debugpy Debugger (pdb als Fallback)
        String debugCmd = pythonCmd + " -m pdb \"" + activePy.getAbsolutePath() + "\"";

        return executeDebugCommand(debugCmd);
    }

    /**
     * Führt den Debug-Befehl aus und gibt den Process zurück
     */
    private Process executeDebugCommand(String command) {
        consolePanel.log("> " + command + "\n", Color.GRAY);
        consolePanel.log("[DEBUG PYTHON] Python Debugger gestartet. Gib Befehle ein (h für Hilfe).\n", Color.YELLOW);

        try {
            ProcessBuilder pb;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("bash", "-c", command);
            }
            pb.directory(currentProjectFolder);
            pb.inheritIO(); // Wichtig: IO direkt durchleiten für interaktives Debugging

            Process p = pb.start();

            // Optional: Output in Console Panel spiegeln
            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        consolePanel.log(line + "\n", Color.WHITE);
                    }
                } catch (IOException e) {
                    // Erwartet bei inheritIO
                }
            }).start();

            return p;

        } catch (IOException e) {
            consolePanel.log("[DEBUG PYTHON] Fehler: " + e.getMessage() + "\n", Color.RED);
            return null;
        }
    }
}