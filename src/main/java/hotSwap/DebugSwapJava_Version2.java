package runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

/**
 * Java-spezifische Debug-Swap-Logik.
 * Verwaltet Compilation und Execution für Debug-Modus im Java.
 */
public class DebugSwapJava {
    
    private final ProjectRunner projectRunner;
    private volatile Process lastDebugProcess;
    
    public DebugSwapJava(ProjectRunner projectRunner) {
        this.projectRunner = projectRunner;
    }
    
    /**
     * Kompiliert und startet ein Java-Programm im Debug-Modus.
     */
    public void runDebugJava(File projectFolder, File mainFile, String fqcn, String classpath) 
            throws Exception {
        
        // 1. Alle Java-Dateien sammeln
        List<File> javaFiles = new ArrayList<>();
        collectAllJavaFiles(projectFolder, javaFiles);
        
        if (javaFiles.isEmpty()) {
            throw new Exception("Keine Java-Dateien gefunden!");
        }
        
        // 2. Sources-Datei erstellen
        File sourcesListFile = new File(projectFolder, ".debug_sources.txt");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new java.io.FileOutputStream(sourcesListFile), StandardCharsets.UTF_8))) {
            for (File f : javaFiles) {
                writer.println(f.getAbsolutePath());
            }
        }
        
        // 3. Source-Root berechnen
        File sourceRoot = calculateSourceRoot(mainFile, fqcn);
        
        // 4. Kompile-Befehl (mit -g für Debug-Infos)
        String separator = System.getProperty("path.separator");
        String compileCmd = "javac -encoding UTF-8 -g " +  // -g = Debug-Infos
                           "-cp \"" + classpath + "\" " +
                           "-d out " +
                           "-sourcepath \"" + sourceRoot.getAbsolutePath() + "\" " +
                           "\"@" + sourcesListFile.getName() + "\"";
        
        // 5. Run-Befehl (mit aktivem Debugging)
        String runCmd = "java -cp \"out" + separator + "libs/*\" " + fqcn;
        
        // 6. Führe aus
        String fullCmd = compileCmd + " && " + runCmd;
        projectRunner.executeCommand(fullCmd, false);
    }
    
    /**
     * Stoppt den aktuellen Debug-Prozess.
     */
    public void stopDebugProcess() {
        if (lastDebugProcess != null && lastDebugProcess.isAlive()) {
            lastDebugProcess.descendants().forEach(ProcessHandle::destroy);
            lastDebugProcess.destroyForcibly();
            lastDebugProcess = null;
        }
    }
    
    private void collectAllJavaFiles(File dir, List<File> list) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (name.equals(".git") || name.equals("out") || 
                    name.equals("libs") || name.equals("target")) continue;
                collectAllJavaFiles(f, list);
            } else if (f.getName().endsWith(".java")) {
                list.add(f);
            }
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
}