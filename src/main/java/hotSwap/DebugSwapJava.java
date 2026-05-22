package runner;

import config.LanguageManager;
import editor.CompilerErrorMarker;
import editor.EditorManager;
import ui.ConsolePanel;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * DebugSwapJava implementiert Debug für Java-Programme mit Restart-Fähigkeit
 */
public class DebugSwapJava implements DebugRunner.DebugStrategy {

    private final ConsolePanel consolePanel;
    private final EditorManager editorManager;
    private final CompilerErrorMarker errorMarker;
    private final File currentProjectFolder;

    public DebugSwapJava(ConsolePanel consolePanel, EditorManager editorManager,
                         CompilerErrorMarker errorMarker, File currentProjectFolder) {
        this.consolePanel = consolePanel;
        this.editorManager = editorManager;
        this.errorMarker = errorMarker;
        this.currentProjectFolder = currentProjectFolder;
    }

    @Override
    public Process execute(String mainClass) throws InterruptedException {
        String mc = mainClass;
        if (mc.endsWith(".java")) mc = mc.substring(0, mc.length() - 5);

        File outFolder = new File(currentProjectFolder, "out");
        if (!outFolder.exists()) outFolder.mkdirs();

        consolePanel.log("[DEBUG JAVA] " + LanguageManager.t("compiling") + "...\n", Color.CYAN);

        // Hauptdatei finden
        File mainFile = findSourceFile(currentProjectFolder, mc);
        if (mainFile == null) {
            consolePanel.log("[DEBUG] " + LanguageManager.t("file_not_found") + ": " + mc + "\n", Color.RED);
            return null;
        }

        File sourceRoot = calculateSourceRoot(mainFile, mc);
        List<File> javaFiles = new ArrayList<>();
        collectAllJavaFiles(currentProjectFolder, javaFiles);

        if (javaFiles.isEmpty()) {
            consolePanel.log("[DEBUG] " + LanguageManager.t("file_not_found") + "\n", Color.RED);
            return null;
        }

        // Argument-Datei erstellen
        File sourcesListFile = new File(currentProjectFolder, ".sources.txt");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(sourcesListFile), StandardCharsets.UTF_8))) {
            for (File f : javaFiles) {
                writer.println(f.getAbsolutePath());
            }
        } catch (IOException e) {
            consolePanel.log("[DEBUG] Error creating source list: " + e.getMessage() + "\n", Color.RED);
            return null;
        }

        String separator = System.getProperty("path.separator");
        String classpath = "out" + separator + "libs/*";

        // Compilation mit Debug-Info (-g Flag)
        String compileCmd = "javac -encoding UTF-8 -g -cp \"" + classpath + "\" " +
                "-d out " +
                "-sourcepath \"" + sourceRoot.getAbsolutePath() + "\" " +
                "\"@" + sourcesListFile.getName() + "\"";

        // Zufälligen Port wählen (5005-5050 Range)
        int debugPort = 5005 + (int)(Math.random() * 45);
        
        // JVM mit JDWP für Debug - OHNE suspend (läuft direkt los)
        String debugCmd = "java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" 
                + debugPort + " -cp \"out" + separator + "libs/*\" " + mc;

        return executeDebugCommand(compileCmd + " && " + debugCmd, debugPort);
    }

    /**
     * Führt den Debug-Befehl aus und gibt den Process zurück
     */
    private Process executeDebugCommand(String command, int debugPort) {
        consolePanel.log("> " + command + "\n", Color.GRAY);
        consolePanel.log("[DEBUG] Debugger läuft auf Port " + debugPort + "\n", Color.YELLOW);
        consolePanel.log("[DEBUG] Verbinde mit IDE (z.B. IntelliJ/Eclipse) zur Breakpoint-Verwaltung\n", Color.CYAN);

        try {
            ProcessBuilder pb;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("bash", "-c", command);
            }
            pb.directory(currentProjectFolder);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            // Output lesen in separatem Thread
            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        consolePanel.log(line + "\n", Color.WHITE);
                    }
                } catch (IOException e) {
                    consolePanel.log("[DEBUG] IO Error: " + e.getMessage() + "\n", Color.RED);
                }
            }).start();

            return p;

        } catch (IOException e) {
            consolePanel.log("[DEBUG] Fehler: " + e.getMessage() + "\n", Color.RED);
            return null;
        }
    }

    private File calculateSourceRoot(File sourceFile, String fqcn) {
        int dotCount = 0;
        for (char c : fqcn.toCharArray()) if (c == '.') dotCount++;

        File root = sourceFile.getParentFile();
        for (int i = 0; i < dotCount; i++) {
            if (root != null && root.getParentFile() != null) {
                root = root.getParentFile();
            }
        }
        return root;
    }

    private void collectAllJavaFiles(File dir, List<File> list) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (name.equals(".git") || name.equals("out") || name.equals("libs") || name.equals("target")) continue;
                collectAllJavaFiles(f, list);
            } else if (f.getName().endsWith(".java")) {
                list.add(f);
            }
        }
    }

    private File findSourceFile(File dir, String fqcn) {
        String relativePath = fqcn.replace(".", File.separator) + ".java";
        File direct = new File(dir, relativePath);
        if (direct.exists()) return direct;

        File srcMain = new File(dir, "src" + File.separator + "main" + File.separator + "java");
        if (srcMain.exists()) {
            File f = new File(srcMain, relativePath);
            if (f.exists()) return f;
        }

        if (!fqcn.contains(".")) {
            return searchFileRecursively(dir, fqcn + ".java");
        }
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