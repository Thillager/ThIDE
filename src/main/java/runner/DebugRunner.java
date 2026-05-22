package runner;

import config.LanguageManager;
import editor.CompilerErrorMarker;
import editor.EditorManager;
import hotSwap.DebugSwapJava;
import hotSwap.DebugSwapPython;
import ui.ConsolePanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * DebugRunner – verwaltet Debug-Prozesse und echten HotSwap für Java.
 *
 * Spracherkennung erfolgt automatisch anhand der aktiven Datei:
 *   .java → DebugSwapJava (mit echtem JDI-HotSwap)
 *   .py   → DebugSwapPython (pdb)
 *   sonst → normaler Run über ProjectRunner
 */
public class DebugRunner {

    private final ConsolePanel consolePanel;
    private final EditorManager editorManager;
    private final CompilerErrorMarker errorMarker;
    private final ProjectRunner projectRunner;

    private volatile Process debugProcess;
    private DebugSwapJava javaStrategy; // gehalten für hotSwap()-Zugriff
    private String lastMainClass;
    private String lastDetectedMode;
    private File currentProjectFolder;
    private JButton btnHotSwap;

    public DebugRunner(ConsolePanel consolePanel, EditorManager editorManager,
                       CompilerErrorMarker errorMarker, ProjectRunner projectRunner) {
        this.consolePanel = consolePanel;
        this.editorManager = editorManager;
        this.errorMarker = errorMarker;
        this.projectRunner = projectRunner;
    }

    public void setHotSwapButton(JButton btn) { this.btnHotSwap = btn; }
    public void setCurrentProjectFolder(File folder) { this.currentProjectFolder = folder; }

    /**
     * Startet Debug. Sprache wird automatisch aus der aktiven Datei erkannt.
     * Ist die Sprache nicht Java oder Python, wird normal gestartet.
     *
     * @param mainClass Main-Class für Java (ignoriert bei Python/anderen)
     */
    public void startDebug(String mainClass) {
        if (currentProjectFolder == null) {
            consolePanel.log(LanguageManager.t("no_project") + "\n", Color.ORANGE);
            return;
        }

        File activeFile = editorManager.getActiveFile();
        String detectedMode = detectMode(activeFile, mainClass);
        lastMainClass = mainClass;
        lastDetectedMode = detectedMode;

        switch (detectedMode) {
            case ProjectRunner.MODE_JAVA:
                javaStrategy = new DebugSwapJava(consolePanel, editorManager, errorMarker, currentProjectFolder);
                executeDebug(javaStrategy, mainClass);
                break;

            case ProjectRunner.MODE_PYTHON:
                javaStrategy = null;
                executeDebug(new DebugSwapPython(consolePanel, editorManager, errorMarker, currentProjectFolder), mainClass);
                break;

            default:
                // Andere Sprachen (C, C++, Batch) → normaler Run
                consolePanel.log("[DEBUG] Sprache '" + detectedMode + "' unterstützt kein Debug – starte normal.\n", Color.ORANGE);
                projectRunner.runProject(detectedMode, mainClass);
        }
    }

    /**
     * Echter HotSwap für Java: Klassen werden per JDI im laufenden Prozess
     * ersetzt, kein Neustart.
     * Für Python: nicht möglich, daher Hinweis in der Konsole.
     */
    public void hotSwap() {
        if (javaStrategy != null && javaStrategy.getActiveDebugPort() != -1) {
            // Echter JDI-HotSwap
            javaStrategy.hotSwap();
        } else if (ProjectRunner.MODE_PYTHON.equals(lastDetectedMode)) {
            consolePanel.log("[HOTSWAP] Python ist eine Interpreter-Sprache – kein HotSwap möglich.\n"
                           + "[HOTSWAP] Nutze den ⟲ Restart-Button um neu zu starten.\n", Color.ORANGE);
        } else {
            consolePanel.log("[HOTSWAP] Kein laufender Debug-Prozess.\n", Color.ORANGE);
        }
    }

    /** Stoppt den laufenden Debug-Prozess. */
    public void stopDebugProcess() {
        if (debugProcess != null && debugProcess.isAlive()) {
            debugProcess.descendants().forEach(ProcessHandle::destroy);
            debugProcess.destroyForcibly();
            debugProcess = null;
            consolePanel.log("[DEBUG] Prozess beendet.\n", Color.ORANGE);
        }
        javaStrategy = null;
        setHotSwapButtonVisible(false);
    }

    // ---- intern ----

    private void executeDebug(DebugStrategy strategy, String mainClass) {
        editorManager.saveCurrentFile();
        setHotSwapButtonVisible(false); // erst sichtbar wenn Prozess läuft

        new Thread(() -> {
            try {
                debugProcess = strategy.execute(mainClass);
                if (debugProcess != null) {
                    // HotSwap-Button nur für Java einblenden (Port gesetzt)
                    if (javaStrategy != null) setHotSwapButtonVisible(true);
                    debugProcess.waitFor();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                debugProcess = null;
                javaStrategy = null;
                setHotSwapButtonVisible(false);
            }
        }).start();
    }

    private String detectMode(File activeFile, String mainClass) {
        if (activeFile != null) {
            String name = activeFile.getName().toLowerCase();
            if (name.endsWith(".py"))  return ProjectRunner.MODE_PYTHON;
            if (name.endsWith(".c"))   return ProjectRunner.MODE_C;
            if (name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".cxx"))
                                       return ProjectRunner.MODE_CPP;
            if (name.endsWith(".bat") || name.endsWith(".cmd"))
                                       return ProjectRunner.MODE_BATCH;
        }
        // Default: Java (auch wenn aktive Datei .java ist oder nichts offen)
        return ProjectRunner.MODE_JAVA;
    }

    private void setHotSwapButtonVisible(boolean visible) {
        if (btnHotSwap != null)
            SwingUtilities.invokeLater(() -> btnHotSwap.setVisible(visible));
    }

    public interface DebugStrategy {
        Process execute(String mainClass) throws InterruptedException;
    }
}