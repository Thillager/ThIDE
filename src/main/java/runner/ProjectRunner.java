package runner;

import editor.CompilerErrorMarker;
import editor.EditorManager;
import ui.ConsolePanel;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ProjectRunner {

    public static final String MODE_JAVA   = "Java";
    public static final String MODE_PYTHON = "Python";
    public static final String MODE_C      = "C";
    public static final String MODE_CPP    = "C++";
    public static final String MODE_BATCH  = "Batch";

    private final ConsolePanel consolePanel;
    private final EditorManager editorManager;
    private final CompilerErrorMarker errorMarker;
    private File currentProjectFolder;

    public ProjectRunner(ConsolePanel consolePanel, EditorManager editorManager,
                         CompilerErrorMarker errorMarker) {
        this.consolePanel  = consolePanel;
        this.editorManager = editorManager;
        this.errorMarker   = errorMarker;
    }

    public void setCurrentProjectFolder(File folder) {
        this.currentProjectFolder = folder;
    }

    public void runProject(String mode, String mainClass) {
        if (currentProjectFolder == null) {
            consolePanel.log("[WARNUNG] Bitte öffne zuerst einen Projektordner.\n", Color.ORANGE);
            return;
        }

        String mc = mainClass;
        if (mc.endsWith(".java")) mc = mc.substring(0, mc.length() - 5);
        editorManager.saveCurrentFile();

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        switch (mode) {
            case MODE_JAVA:
                if (mc.isEmpty()) { consolePanel.log("[FEHLER] Bitte gib die Main-Klasse ein.\n", Color.RED); return; }
                File outFolder = new File(currentProjectFolder, "out");
                if (!outFolder.exists()) outFolder.mkdirs();
                consolePanel.log("[JAVA] Kompiliere nach /out...\n", Color.CYAN);
                File sourceFile = findSourceFile(currentProjectFolder, mc);
                if (sourceFile == null) { consolePanel.log("[FEHLER] Datei nicht gefunden.\n", Color.RED); return; }
                String separator = System.getProperty("path.separator");
                String classpath  = "\"out\"" + separator + "\"libs/*\"";
                String compileCmd = "javac -encoding UTF-8 -cp " + classpath + " -d out \"" + sourceFile.getAbsolutePath() + "\"";
                String runCmd     = "java -cp " + classpath + " " + mc;
                executeCommand(compileCmd + " && " + runCmd, false);
                break;

            case MODE_C:
            case MODE_CPP:
                File activeC = editorManager.getActiveFile();
                if (activeC != null) {
                    String compiler = mode.equals(MODE_C) ? "gcc" : "g++";
                    String exeName  = isWindows ? "program.exe" : "./program";
                    consolePanel.log("[" + mode.toUpperCase() + "] Kompiliere " + activeC.getName() + "...\n", Color.CYAN);
                    executeCommand(compiler + " \"" + activeC.getAbsolutePath() + "\" -o program && " + exeName, false);
                } else { consolePanel.log("[FEHLER] Keine C/C++ Datei offen.\n", Color.RED); }
                break;

            case MODE_BATCH:
                File activeBat = editorManager.getActiveFile();
                if (activeBat != null) {
                    consolePanel.log("[BATCH] Starte " + activeBat.getName() + "...\n", Color.CYAN);
                    executeCommand("\"" + activeBat.getAbsolutePath() + "\"", false);
                } else { consolePanel.log("[FEHLER] Keine Batch-Datei offen.\n", Color.RED); }
                break;
        }
    }

    public void handleTBuild() {
        if (currentProjectFolder == null) {
            consolePanel.log("[FEHLER] Bitte öffne zuerst einen Projektordner.\n", Color.RED);
            return;
        }
        File tbuildJar = new File(currentProjectFolder, "TBuild.jar");
        if (tbuildJar.exists()) {
            executeCommand("java -jar TBuild.jar", true);
        } else {
            consolePanel.log("[INFO] TBuild.jar nicht gefunden. Lade herunter...\n", Color.YELLOW);
            new Thread(() -> {
                try {
                    URL url = new URL("https://github.com/Thillager/Tbuild/releases/latest/download/TBuild.jar");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setInstanceFollowRedirects(true);
                    try (InputStream in = connection.getInputStream()) {
                        Files.copy(in, tbuildJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    consolePanel.log("[ERFOLG] TBuild erfolgreich heruntergeladen!\n", Color.GREEN);
                    executeCommand("java -jar TBuild.jar", true);
                    if (onRefreshFileTree != null)
                        SwingUtilities.invokeLater(onRefreshFileTree);
                } catch (Exception ex) {
                    consolePanel.log("[FEHLER] Download fehlgeschlagen: " + ex.getMessage() + "\n", Color.RED);
                }
            }).start();
        }
    }

    public void executeCommand(String command, boolean isTBuild) {
        consolePanel.log("> " + command + "\n", Color.GRAY);
        if (!isTBuild) {
            SwingUtilities.invokeLater(errorMarker::clearCompilerErrors);
        }
        new Thread(() -> {
            try {
                ProcessBuilder pb;
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    pb = new ProcessBuilder("cmd.exe", "/c", command);
                } else {
                    pb = new ProcessBuilder("bash", "-c", command);
                }
                if (currentProjectFolder != null) pb.directory(currentProjectFolder);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
                String line;
                StringBuilder fullOutput = new StringBuilder();
                while ((line = r.readLine()) != null) {
                    fullOutput.append(line).append("\n");
                    consolePanel.log(line + "\n", isTBuild ? Color.CYAN : Color.WHITE);
                }
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    consolePanel.log("[PROZESS BEENDET MIT CODE " + exitCode + "]\n", Color.RED);
                    if (!isTBuild) {
                        errorMarker.markCompilerErrors(fullOutput.toString());
                    }
                }
            } catch (Exception e) {
                consolePanel.log("[TERMINAL FEHLER] " + e.getMessage() + "\n", Color.RED);
            }
        }).start();
    }

    // ======= Callbacks =======

    private Runnable onRefreshFileTree;

    public void setOnRefreshFileTree(Runnable r) {
        this.onRefreshFileTree = r;
    }

    // ======= Hilfsmethoden =======

    public File findSourceFile(File dir, String fqcn) {
        String relativePath = fqcn.replace(".", File.separator) + ".java";
        File direct = new File(dir, relativePath);
        if (direct.exists()) return direct;
        File src = new File(dir, "src");
        if (src.exists() && src.isDirectory()) {
            File inSrc = new File(src, relativePath);
            if (inSrc.exists()) return inSrc;
        }
        if (!fqcn.contains(".")) return searchFileRecursively(dir, fqcn + ".java");
        return null;
    }

    private File searchFileRecursively(File dir, String fileName) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                if (f.getName().equals(".git") || f.getName().equals("out")) continue;
                File found = searchFileRecursively(f, fileName);
                if (found != null) return found;
            } else if (f.getName().equals(fileName)) return f;
        }
        return null;
    }
}
