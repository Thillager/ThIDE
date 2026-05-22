package runner;

import config.LanguageManager;
import editor.CompilerErrorMarker;
import editor.EditorManager;
import ui.ConsolePanel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * DebugRunner verwaltet den Debug-Modus mit Restart-Funktionalität.
 * Verwendet das Strategy-Pattern mit SwapJava und SwapPython.
 */
public class DebugRunner {

    private final ConsolePanel consolePanel;
    private final EditorManager editorManager;
    private final CompilerErrorMarker errorMarker;
    private final ProjectRunner projectRunner;
    
    private volatile Process debugProcess;
    private DebugStrategy currentStrategy;
    private String lastMode;
    private String lastMainClass;
    private File currentProjectFolder;
    private JButton btnDebugRestart;

    public DebugRunner(ConsolePanel consolePanel, EditorManager editorManager,
                       CompilerErrorMarker errorMarker, ProjectRunner projectRunner) {
        this.consolePanel = consolePanel;
        this.editorManager = editorManager;
        this.errorMarker = errorMarker;
        this.projectRunner = projectRunner;
    }

    public void setDebugRestartButton(JButton btnDebugRestart) {
        this.btnDebugRestart = btnDebugRestart;
    }

    public void setCurrentProjectFolder(File folder) {
        this.currentProjectFolder = folder;
    }

    /**
     * Startet den Debug-Modus mit der entsprechenden Strategie
     */
    public void startDebug(String mode, String mainClass) {
        if (currentProjectFolder == null) {
            consolePanel.log(LanguageManager.t("no_project") + "\n", Color.ORANGE);
            return;
        }

        lastMode = mode;
        lastMainClass = mainClass;

        // Strategie je nach Modus auswählen
        switch (mode) {
            case ProjectRunner.MODE_JAVA:
                currentStrategy = new SwapJava(consolePanel, editorManager, errorMarker, currentProjectFolder);
                break;
            case ProjectRunner.MODE_PYTHON:
                currentStrategy = new SwapPython(consolePanel, editorManager, errorMarker, currentProjectFolder);
                break;
            default:
                consolePanel.log("[DEBUG] Modus nicht unterstützt: " + mode + "\n", Color.RED);
                return;
        }

        executeDebug(mainClass);
    }

    /**
     * Startet den Debug-Prozess neu (wird vom Debug-Restart-Button aufgerufen)
     */
    public void restartDebug() {
        if (lastMode == null || lastMainClass == null) {
            consolePanel.log("[DEBUG] Kein Debug-Prozess zum Neustarten vorhanden.\n", Color.ORANGE);
            return;
        }

        // Alten Prozess beenden
        stopDebugProcess();

        consolePanel.log("\n[DEBUG] Neustarten...\n", Color.CYAN);
        
        // Neu starten
        executeDebug(lastMainClass);
    }

    /**
     * Führt den Debug-Prozess aus (delegiert an die Strategie)
     */
    private void executeDebug(String mainClass) {
        if (currentStrategy == null) {
            consolePanel.log("[DEBUG] Keine Debug-Strategie gesetzt.\n", Color.RED);
            return;
        }

        editorManager.saveCurrentFile();

        // Debug-Restart-Button anzeigen
        if (btnDebugRestart != null) {
            SwingUtilities.invokeLater(() -> btnDebugRestart.setVisible(true));
        }

        new Thread(() -> {
            try {
                debugProcess = currentStrategy.execute(mainClass);
                if (debugProcess != null) {
                    debugProcess.waitFor();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                debugProcess = null;
                if (btnDebugRestart != null) {
                    SwingUtilities.invokeLater(() -> btnDebugRestart.setVisible(false));
                }
            }
        }).start();
    }

    /**
     * Beendet den Debug-Prozess
     */
    public void stopDebugProcess() {
        if (debugProcess != null && debugProcess.isAlive()) {
            debugProcess.descendants().forEach(ProcessHandle::destroy);
            debugProcess.destroyForcibly();
            debugProcess = null;
            consolePanel.log("[DEBUG] Prozess beendet.\n", Color.ORANGE);
        }
    }

    /**
     * Strategy-Interface für verschiedene Debug-Modi
     */
    public interface DebugStrategy {
        Process execute(String mainClass) throws InterruptedException;
    }
}