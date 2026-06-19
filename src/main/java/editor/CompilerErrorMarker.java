package editor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import ui.ConsolePanel;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompilerErrorMarker {

	private final JTabbedPane editorTabs;
	private final Map<Component, File> openFiles;
	private final ConsolePanel consolePanel;

	// Eigener Painter: roter Hintergrund, halbtransparent
	private static final Highlighter.HighlightPainter ERROR_PAINTER =
		new DefaultHighlighter.DefaultHighlightPainter(new Color(180, 30, 30, 60));

	public CompilerErrorMarker(JTabbedPane editorTabs, Map<Component, File> openFiles, ConsolePanel consolePanel) {
		this.editorTabs   = editorTabs;
		this.openFiles    = openFiles;
		this.consolePanel = consolePanel;
	}

	public void markCompilerErrors(String output) {
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
			"(.+\\.java):(\\d+): (?:error|Fehler): (.+)");
		java.util.regex.Matcher matcher = pattern.matcher(output);

		// Alle Treffer sammeln, dann einmal auf dem EDT anwenden
		record ErrorEntry(String fileName, int lineNum, String message) {}
		List<ErrorEntry> errors = new ArrayList<>();
		while (matcher.find()) {
			errors.add(new ErrorEntry(
				new File(matcher.group(1)).getName(),
				Integer.parseInt(matcher.group(2)) - 1, // 0-basiert
				matcher.group(3)
			));
		}
		if (errors.isEmpty()) return;

		SwingUtilities.invokeLater(() -> {
			for (int i = 0; i < editorTabs.getTabCount(); i++) {
				Component tab = editorTabs.getComponentAt(i);
				File tabFile  = openFiles.get(tab);
				if (!(tab instanceof RTextScrollPane)) continue;

				RSyntaxTextArea ta = (RSyntaxTextArea) ((RTextScrollPane) tab).getTextArea();
				Highlighter hl = ta.getHighlighter();

				for (ErrorEntry e : errors) {
					if (tabFile == null || !tabFile.getName().equals(e.fileName())) continue;

					try {
						// Offset-basiert: Swing verschiebt den Bereich automatisch
						// wenn Zeilen darüber eingefügt / gelöscht werden.
						int lineStart = ta.getLineStartOffset(e.lineNum());
						int lineEnd   = ta.getLineEndOffset(e.lineNum());
						hl.addHighlight(lineStart, lineEnd, ERROR_PAINTER);
					} catch (BadLocationException ignored) {
						// Zeile liegt außerhalb des Dokuments → ignorieren
					}

					consolePanel.log(
						"[FEHLER Zeile " + (e.lineNum() + 1) + "] " + e.message() + "\n",
						Color.RED);
				}
			}
		});
	}

	public void clearCompilerErrors() {
		SwingUtilities.invokeLater(() -> {
			for (int i = 0; i < editorTabs.getTabCount(); i++) {
				Component tab = editorTabs.getComponentAt(i);
				if (!(tab instanceof RTextScrollPane)) continue;

				RSyntaxTextArea ta = (RSyntaxTextArea) ((RTextScrollPane) tab).getTextArea();
				Highlighter hl = ta.getHighlighter();

				// Nur unsere eigenen Error-Highlights entfernen,
				// andere Highlights (z.B. Suchmarkierungen) bleiben erhalten.
				for (Highlighter.Highlight h : hl.getHighlights()) {
					if (h.getPainter() == ERROR_PAINTER) {
						hl.removeHighlight(h);
					}
				}
			}
		});
	}
}
