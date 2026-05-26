package editor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import ui.ConsolePanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Map;

public class CompilerErrorMarker {

	private final JTabbedPane editorTabs;
	private final Map<Component, File> openFiles;
	private final ConsolePanel consolePanel;

	public CompilerErrorMarker(JTabbedPane editorTabs, Map<Component, File> openFiles, ConsolePanel consolePanel) {
		this.editorTabs   = editorTabs;
		this.openFiles    = openFiles;
		this.consolePanel = consolePanel;
	}

	public void markCompilerErrors(String output) {
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
			"(.+\\.java):(\\d+): (?:error|Fehler): (.+)");
		java.util.regex.Matcher matcher = pattern.matcher(output);

		while (matcher.find()) {
			String fileName = new File(matcher.group(1)).getName();
			int    lineNum  = Integer.parseInt(matcher.group(2)) - 1;
			String message  = matcher.group(3);

			for (int i = 0; i < editorTabs.getTabCount(); i++) {
				Component tab = editorTabs.getComponentAt(i);
				File tabFile  = openFiles.get(tab);
				if (tabFile != null && tabFile.getName().equals(fileName)
					&& tab instanceof RTextScrollPane) {
					RSyntaxTextArea ta = (RSyntaxTextArea)
					((RTextScrollPane) tab).getTextArea();
					SwingUtilities.invokeLater(() -> {
							try {
								ta.addLineHighlight(lineNum, new Color(180, 30, 30, 60));
							} catch (Exception ignored) {}
						});
					consolePanel.log("[FEHLER Zeile " + (lineNum + 1) + "] " + message + "\n", Color.RED);
				}
			}
		}
	}

	public void clearCompilerErrors() {
		for (int i = 0; i < editorTabs.getTabCount(); i++) {
			Component tab = editorTabs.getComponentAt(i);
			if (tab instanceof RTextScrollPane) {
				RSyntaxTextArea ta = (RSyntaxTextArea) ((RTextScrollPane) tab).getTextArea();
				ta.removeAllLineHighlights();
			}
		}
	}
}
