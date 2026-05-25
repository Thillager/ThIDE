package hotSwap;

import config.LanguageManager;
import editor.CompilerErrorMarker;
import editor.EditorManager;
import runner.DebugRunner;
import ui.ConsolePanel;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class DebugSwapPython implements DebugRunner.DebugStrategy {

    private final ConsolePanel consolePanel;
    private final EditorManager editorManager;
    private final CompilerErrorMarker errorMarker;
    private final File currentProjectFolder;

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
        if (activePy == null || !activePy.getName().endsWith(".py")) {
            consolePanel.log("[DEBUG PYTHON] " + LanguageManager.t("no_python_file") + "\n", Color.RED);
            return null;
        }

        consolePanel.log("[DEBUG PYTHON] Starte pdb für " + activePy.getName() + "...\n", Color.CYAN);

        String pythonCmd = isWindows() ? "python" : "python3";
        String cmd = pythonCmd + " -m pdb \"" + activePy.getAbsolutePath() + "\"";
        consolePanel.log("> " + cmd + "\n", Color.GRAY);
        consolePanel.log("[DEBUG PYTHON] Befehle: n(ext), s(tep), c(ontinue), l(ist), q(uit), h(elp)\n", Color.YELLOW);

        try {
            ProcessBuilder pb = isWindows()
                    ? new ProcessBuilder("cmd.exe", "/c", cmd)
                    : new ProcessBuilder("bash", "-c", cmd);
            pb.directory(currentProjectFolder);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null)
                        consolePanel.log(line + "\n", Color.WHITE);
                } catch (IOException e) {
                    consolePanel.log("[DEBUG PYTHON] IO Error: " + e.getMessage() + "\n", Color.RED);
                }
            }).start();

            return p;
        } catch (IOException e) {
            consolePanel.log("[DEBUG PYTHON] Fehler: " + e.getMessage() + "\n", Color.RED);
            return null;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
