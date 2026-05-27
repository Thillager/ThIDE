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

public class DebugRunner {

	private final ConsolePanel consolePanel;
	private final EditorManager editorManager;
	private final CompilerErrorMarker errorMarker;
	private final ProjectRunner projectRunner;

	private volatile Process debugProcess;
	private DebugSwapJava javaStrategy; 
	private String lastMainClass;
	private String lastDetectedMode;
	private File currentProjectFolder;
	private JButton btnHotSwap;
	private JButton btnTerminate;

	public DebugRunner(ConsolePanel consolePanel, EditorManager editorManager,
		CompilerErrorMarker errorMarker, ProjectRunner projectRunner) {
		this.consolePanel = consolePanel;
		this.editorManager = editorManager;
		this.errorMarker = errorMarker;
		this.projectRunner = projectRunner;
	}

	

	// Setter
	public void setTerminateButton(JButton btnTerminate) {this.btnTerminate = btnTerminate;}
	public void setHotSwapButton(JButton btn) { this.btnHotSwap = btn; }
	public void setCurrentProjectFolder(File folder) { this.currentProjectFolder = folder; }

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
			consolePanel.log("[DEBUG] Sprache '" + detectedMode + "' unterstützt kein Debug – starte normal.\n", Color.ORANGE);
			projectRunner.runProject(detectedMode, mainClass);
		}
}


public void hotSwap() {
	if (javaStrategy != null && javaStrategy.getActiveDebugPort() != -1) {
		javaStrategy.hotSwap();
	} else if (ProjectRunner.MODE_PYTHON.equals(lastDetectedMode)) {
		consolePanel.log("[HOTSWAP] Python ist eine Interpreter-Sprache – kein HotSwap möglich.\n"
			+ "[HOTSWAP] Nutze den ⟲ Restart-Button um neu zu starten.\n", Color.ORANGE);
	} else {
		consolePanel.log("[HOTSWAP] Kein laufender Debug-Prozess.\n", Color.ORANGE);
	}
}

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
	setHotSwapButtonVisible(false);


	new Thread(() -> {
			try {
				debugProcess = strategy.execute(mainClass);
				if (debugProcess != null) {
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
	return ProjectRunner.MODE_JAVA;
}

private void setHotSwapButtonVisible(boolean visible) {
	if (btnHotSwap != null)
	SwingUtilities.invokeLater(() -> btnHotSwap.setVisible(visible));
	SwingUtilities.invokeLater(() -> btnTerminate.setVisible(visible));
}

public interface DebugStrategy {
	Process execute(String mainClass) throws InterruptedException;
}
}