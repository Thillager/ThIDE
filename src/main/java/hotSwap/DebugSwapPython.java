package runner;

import java.io.File;

/**
 * Python-spezifische Debug-Swap-Logik.
 * Verwaltet Execution für Debug-Modus in Python.
 */
public class DebugSwapPython {
    
    private final ProjectRunner projectRunner;
    private volatile Process lastDebugProcess;
    
    public DebugSwapPython(ProjectRunner projectRunner) {
        this.projectRunner = projectRunner;
    }
    
    /**
     * Startet ein Python-Programm im Debug-Modus.
     * Nutzt pdb (Python Debugger) für interaktives Debugging.
     */
    public void runDebugPython(File pythonFile) throws Exception {
        if (!pythonFile.exists() || !pythonFile.getName().endsWith(".py")) {
            throw new Exception("Keine gültige Python-Datei!");
        }
        
        // Python Debugger Befehl (pdb)
        String pythonCmd = System.getProperty("os.name").toLowerCase().contains("win")
                ? "python"
                : "python3";
        
        // -m pdb = Python Debugger Modul
        String debugCmd = pythonCmd + " -m pdb \"" + pythonFile.getAbsolutePath() + "\"";
        
        // Führe aus
        projectRunner.executeCommand(debugCmd, false);
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
}
