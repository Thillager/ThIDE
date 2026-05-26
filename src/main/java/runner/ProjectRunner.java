package runner;

import config.LanguageManager;
import editor.CompilerErrorMarker;
import editor.EditorManager;
import ui.ConsolePanel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

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

	private JButton btnTerminate;

	private volatile Process runningProcess;

	public ProjectRunner(ConsolePanel consolePanel, EditorManager editorManager,
		CompilerErrorMarker errorMarker) {
		this.consolePanel  = consolePanel;
		this.editorManager = editorManager;
		this.errorMarker   = errorMarker;
	}

	// 2. Einen einfachen Setter hinzufügen
	public void setTerminateButton(JButton btnTerminate) {
		this.btnTerminate = btnTerminate;
	}

	public void setCurrentProjectFolder(File folder) {
		this.currentProjectFolder = folder;
	}

	public void stopRunningProcess() {
		if (runningProcess != null && runningProcess.isAlive()) {
			runningProcess.descendants().forEach(ProcessHandle::destroy);
			runningProcess.destroyForcibly();
			runningProcess = null;
			consolePanel.log("[INFO] Prozess beendet.\n", Color.ORANGE);

			// Button direkt ausblenden
			if (btnTerminate != null) {
				SwingUtilities.invokeLater(() -> btnTerminate.setVisible(false));
			}
		} else {
			consolePanel.log("[INFO] Kein laufender Prozess.\n", Color.GRAY);
		}
	}

	private void startResourceMonitor() {
		new Thread(() -> {
				com.sun.management.OperatingSystemMXBean os =
				(com.sun.management.OperatingSystemMXBean)
				java.lang.management.ManagementFactory.getOperatingSystemMXBean();

				while (runningProcess != null && runningProcess.isAlive()) {
					try {
						Thread.sleep(1000);

						double cpuLoad = os.getCpuLoad() * 100;
						double ramLoad = (double)(os.getTotalMemorySize() - os.getFreeMemorySize())
						/ os.getTotalMemorySize() * 100;

						if (cpuLoad >= 95 || ramLoad >= 95) {
							consolePanel.log("[WARNUNG] Ressourcen kritisch (CPU: "
									+ (int)cpuLoad + "% RAM: " + (int)ramLoad
									+ "%) — Prozess wird beendet.\n", Color.ORANGE);
							stopRunningProcess();
							return;
						}
					} catch (InterruptedException e) {
						return;
					}
				}
			}, "TideResourceMonitor").start();
	}

	public void runProject(String mode, String mainClass) {
		if (currentProjectFolder == null) {
			consolePanel.log(LanguageManager.t("no_project") + "\n", Color.ORANGE);
			return;
		}

		String mc = mainClass;
		if (mc.endsWith(".java")) mc = mc.substring(0, mc.length() - 5);
		editorManager.saveAllFiles();

		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

		switch (mode) {
			case MODE_JAVA:
			if (mc.isEmpty()) { 
				consolePanel.log(LanguageManager.t("no_main") + "\n", Color.RED); 
				return; 
			}

			File outFolder = new File(currentProjectFolder, "out");
			if (!outFolder.exists()) outFolder.mkdirs();

			consolePanel.log(LanguageManager.t("compiling") + "\n", Color.CYAN);

			// 1. Hauptdatei finden
			File mainFile = findSourceFile(currentProjectFolder, mc);
			if (mainFile == null) {
				consolePanel.log(LanguageManager.t("file_not_found") + ": " + mc + "\n", Color.RED);
				return;
			}

			// 2. Source-Root dynamisch berechnen
			// Wenn mc "TIDE" ist (0 Punkte), ist die Root der Ordner von TIDE.java
			// Wenn mc "com.test.Main" ist (2 Punkte), ist die Root 2 Ebenen über Main.java
			File sourceRoot = calculateSourceRoot(mainFile, mc);

			// 3. Alle Java-Dateien im gesamten Projekt sammeln
			List<File> javaFiles = new ArrayList<>();
			collectAllJavaFiles(currentProjectFolder, javaFiles);

			if (javaFiles.isEmpty()) {
				consolePanel.log(LanguageManager.t("file_not_found") + "\n", Color.RED);
				return;
			}

			// 4. Argument-Datei erstellen (@sources.txt)
			// Verhindert Probleme mit Leerzeichen und zu langen Befehlszeilen unter Windows
			File sourcesListFile = new File(currentProjectFolder, ".sources.txt");
			try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
						new FileOutputStream(sourcesListFile), StandardCharsets.UTF_8))) {
				for (File f : javaFiles) {
					writer.println(f.getAbsolutePath());
				}
			} catch (IOException e) {
				consolePanel.log("Error creating source list: " + e.getMessage() + "\n", Color.RED);
				return;
			}

			String separator = System.getProperty("path.separator");
			String classpath = "out" + separator + "libs/*";

			// -nowarn unterdrückt die verbleibenden Standard-Hinweise des Compilers
			String compileCmd = "javac -encoding UTF-8 -nowarn -Xlint:none -cp \"" + classpath + "\" " +
			"-d out " +
			"-sourcepath \"" + sourceRoot.getAbsolutePath() + "\" " +
			"\"@" + sourcesListFile.getName() + "\"";

			String runCmd = "java --enable-native-access=ALL-UNNAMED -cp \"out" + separator + "libs/*\" " + mc;

			executeCommand(compileCmd + " && " + runCmd, false);
			break;

			case MODE_BATCH:
			File activeBat = editorManager.getActiveFile();
			if (activeBat == null ||
				!(activeBat.getName().endsWith(".bat") ||
					activeBat.getName().endsWith(".cmd"))) {
				consolePanel.log(LanguageManager.t("no_batch_file") + "\n", Color.RED);
				return;
			}
			consolePanel.log("[BATCH] " + activeBat.getName() + "...\n", Color.CYAN);
			if (isWindows) {
				executeCommand("cmd /c \"" + activeBat.getAbsolutePath() + "\"", false);
			} else {
				consolePanel.log("[BATCH] Batch-Dateien laufen nur unter Windows.\n", Color.RED);
			}
			break;

			case MODE_C:
			case MODE_CPP:
			File activeC = editorManager.getActiveFile();
			if (activeC != null) {
				String fileName = activeC.getName();

				// Prüfen, ob die richtige Dateiendung verwendet wird
				if (mode.equals(MODE_C) && !fileName.endsWith(".c")) {
					consolePanel.log(LanguageManager.t("no_c_file") + "\n", Color.RED);
					return;
				}

				if (mode.equals(MODE_CPP)
					&& !(fileName.endsWith(".cpp")
						|| fileName.endsWith(".cc")
						|| fileName.endsWith(".cxx"))) {
					consolePanel.log(LanguageManager.t("no_cpp_file") + "\n", Color.RED);
					return;
				}

				String compiler = mode.equals(MODE_C) ? "gcc" : "g++";
				String outputName = System.getProperty("os.name").toLowerCase().contains("win")
				? "program.exe"
				: "program";

				// Unter Unix muss das Programm mit ./ gestartet werden
				String runCommand = System.getProperty("os.name").toLowerCase().contains("win")
				? outputName
				: "./" + outputName;

				consolePanel.log(
					"[" + mode.toUpperCase() + "] "
					+ LanguageManager.t("compiling")
					+ " " + fileName + "...\n",
					Color.CYAN
				);

				executeCommand(
					compiler
					+ " \"" + activeC.getAbsolutePath() + "\""
					+ " -o " + outputName
					+ " && " + runCommand,
					false
				);
			} else {
				if (mode.equals(MODE_C)) {
					consolePanel.log(LanguageManager.t("no_c_file") + "\n", Color.RED);
				} else {
					consolePanel.log(LanguageManager.t("no_cpp_file") + "\n", Color.RED);
				}
			}
			break;



			case MODE_PYTHON:
			File activePy = editorManager.getActiveFile();
			if (activePy != null) {
				// Prüfen, ob es sich um eine Python-Datei handelt
				if (!activePy.getName().endsWith(".py")) {
					consolePanel.log(LanguageManager.t("no_python_file") + "\n", Color.RED);
					return;
				}

				// Python ausführen
				consolePanel.log("[PYTHON] " + activePy.getName() + "...\n", Color.CYAN);

				// Unter Windows und Linux/macOS funktioniert "python" oft,
				// auf manchen Systemen ist nur "python3" vorhanden.
				String pythonCmd = System.getProperty("os.name").toLowerCase().contains("win")
				? "python"
				: "python3";

				executeCommand(
					pythonCmd + " \"" + activePy.getAbsolutePath() + "\"",
					false
				);
			} else {
				consolePanel.log(LanguageManager.t("no_python_file") + "\n", Color.RED);
			}
			break;
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

	public void executeCommand(String command, boolean isTBuild) {
		consolePanel.log("> " + command + "\n", Color.GRAY);
		if (!isTBuild) SwingUtilities.invokeLater(errorMarker::clearCompilerErrors);

		// HIER: Zeige den Button sofort, wenn der Befehl abgeschickt wird
		if (btnTerminate != null) {
			SwingUtilities.invokeLater(() -> btnTerminate.setVisible(true));
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
					runningProcess = p;
					startResourceMonitor(); 

					BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
					String line;
					StringBuilder fullOutput = new StringBuilder();
					while ((line = r.readLine()) != null) {
						if (line.startsWith("Hinweis:") || line.startsWith("Note:")) {
							if (line.contains("veraltete API") || line.contains("deprecated API") ||
								line.contains("-Xlint:deprecation")) {
								continue; 
							}
						}

						fullOutput.append(line).append("\n");
						consolePanel.log(line + "\n", isTBuild ? Color.CYAN : Color.WHITE);
					}

					int exitCode = p.waitFor();
					if (exitCode != 0) {
						consolePanel.log("[ERROR CODE " + exitCode + "]\n", Color.RED);
						if (!isTBuild) errorMarker.markCompilerErrors(fullOutput.toString());
					}
				} catch (Exception e) {
					consolePanel.log("[TERMINAL ERROR] " + e.getMessage() + "\n", Color.RED);
				} finally {
					runningProcess = null;
					if (btnTerminate != null) {
						SwingUtilities.invokeLater(() -> btnTerminate.setVisible(false));
					}
				}
			}).start();
	}

	public File findSourceFile(File dir, String fqcn) {
		String relativePath = fqcn.replace(".", File.separator) + ".java";
		File direct = new File(dir, relativePath);
		if (direct.exists()) return direct;

		// Suche in src/main/java (Standard)
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

	public void handleTBuild() {
		if (currentProjectFolder == null) return;
		File tbuildJar = new File(currentProjectFolder, "TBuild.jar");
		if (tbuildJar.exists()) {
			executeCommand("java -jar TBuild.jar", true);
		} else {
			new Thread(() -> {
					try {
						URL url = java.net.URI.create("https://github.com/Thillager/Tbuild/releases/latest/download/TBuild.jar").toURL();
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						connection.setInstanceFollowRedirects(true);
						try (InputStream in = connection.getInputStream()) {
							Files.copy(in, tbuildJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
						executeCommand("java -jar TBuild.jar", true);
					} catch (Exception e) {
						System.err.println("Fehler beim Herunterladen oder Ausführen von TBuild: " + e.getMessage());
						e.printStackTrace();
					}
				}).start();
		}
	}

	private Runnable onRefreshFileTree;
	public void setOnRefreshFileTree(Runnable r) { this.onRefreshFileTree = r; }
}
