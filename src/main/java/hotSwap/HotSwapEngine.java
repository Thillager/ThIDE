package hotSwap;

import ui.ConsolePanel;

import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class HotSwapEngine {

	private final ConsolePanel consolePanel;
	private final File projectFolder;
	private final File outFolder;

	public HotSwapEngine(ConsolePanel consolePanel, File projectFolder) {
		this.consolePanel = consolePanel;
		this.projectFolder = projectFolder;
		this.outFolder = new File(projectFolder, "out");
	}

	public boolean hotSwap(File sourceRoot, List<File> javaFiles, int debugPort) {
		consolePanel.log("[HOTSWAP] Kompiliere...\n", Color.CYAN);

		if (!recompile(sourceRoot, javaFiles)) {
			consolePanel.log("[HOTSWAP] Kompilierung fehlgeschlagen.\n", Color.RED);
			return false;
		}

		consolePanel.log("[HOTSWAP] Verbinde mit Prozess auf Port " + debugPort + "...\n", Color.CYAN);
		return runWorker(debugPort);
	}

	// -------------------------------------------------------------------------
	// Schritt 1: Kompilieren
	// -------------------------------------------------------------------------

	private boolean recompile(File sourceRoot, List<File> javaFiles) {
		if (javaFiles.isEmpty()) return false;

		File sourcesFile = new File(projectFolder, ".sources.txt");
		try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(sourcesFile), java.nio.charset.StandardCharsets.UTF_8))) {
			for (File f : javaFiles) pw.println(f.getAbsolutePath());
		} catch (IOException e) {
			consolePanel.log("[HOTSWAP] .sources.txt Fehler: " + e.getMessage() + "\n", Color.RED);
			return false;
		}

		String sep = System.getProperty("path.separator");
		String cmd = "javac -encoding UTF-8 -g -cp \"out" + sep + "libs/*\" "
		+ "-d out "
		+ "-sourcepath \"" + sourceRoot.getAbsolutePath() + "\" "
		+ "\"@" + sourcesFile.getName() + "\"";
		try {
			ProcessBuilder pb = isWindows()
			? new ProcessBuilder("cmd.exe", "/c", cmd)
			: new ProcessBuilder("bash", "-c", cmd);
			pb.directory(projectFolder);
			pb.redirectErrorStream(true);
			Process p = pb.start();
			try (BufferedReader r = new BufferedReader(
					new InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
				String line;
				while ((line = r.readLine()) != null)
				consolePanel.log(line + "\n", Color.YELLOW);
			}
			return p.waitFor() == 0;
		} catch (Exception e) {
			consolePanel.log("[HOTSWAP] javac Fehler: " + e.getMessage() + "\n", Color.RED);
			return false;
		}
	}

	// -------------------------------------------------------------------------
	// Schritt 2: HotSwapWorker als eigener Prozess mit --add-opens starten
	// -------------------------------------------------------------------------

	private boolean runWorker(int debugPort) {
		String sep = System.getProperty("path.separator");
		String tideClasspath = getTideClasspath();
		String workerCp = outFolder.getAbsolutePath()
		+ sep + new File(projectFolder, "libs/*").getAbsolutePath()
		+ (tideClasspath.isEmpty() ? "" : sep + tideClasspath);

		String javaExe = ProcessHandle.current().info().command().orElse("java");

		List<String> cmd = new ArrayList<>(Arrays.asList(
				javaExe,
				"--add-opens", "jdk.jdi/com.sun.tools.jdi=ALL-UNNAMED",
				"-cp", workerCp,
				"hotSwap.HotSwapWorker",
				outFolder.getAbsolutePath(),
				String.valueOf(debugPort)
			));

		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.directory(projectFolder);
			pb.redirectErrorStream(true);
			Process p = pb.start();

			boolean success = false;
			try (BufferedReader r = new BufferedReader(
					new InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
				String line;
				while ((line = r.readLine()) != null) {
					if (line.startsWith("OK ")) {
						consolePanel.log("[HOTSWAP] ✓ " + line.substring(3)
							+ " Klasse(n) live ersetzt – kein Neustart nötig.\n", Color.GREEN);
						success = true;
					} else if (line.startsWith("ERR ")) {
						String msg = line.substring(4);
						if (msg.contains("UnsupportedOperation")) {
							consolePanel.log("[HOTSWAP] Nicht möglich: Klassenstruktur wurde geändert "
								+ "(neue Methode/Feld hinzugefügt).\n"
								+ "[HOTSWAP] Bitte Prozess neu starten.\n", Color.ORANGE);
						} else {
							consolePanel.log("[HOTSWAP] Fehler: " + msg + "\n", Color.RED);
						}
					} else if (line.startsWith("WARN ")) {
						consolePanel.log("[HOTSWAP] " + line.substring(5) + "\n", Color.ORANGE);
					} else if (!line.isBlank()) {
						consolePanel.log("[HOTSWAP] " + line + "\n", Color.GRAY);
					}
				}
			}
			p.waitFor();
			return success;
		} catch (Exception e) {
			consolePanel.log("[HOTSWAP] Worker-Fehler: " + e.getMessage() + "\n", Color.RED);
			return false;
		}
	}

	private String getTideClasspath() {
		String cp = System.getProperty("java.class.path", "");
		if (!cp.isEmpty()) return cp;
		File jar = new File(projectFolder, "TBuild.jar");
		return jar.exists() ? jar.getAbsolutePath() : "";
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}
}
