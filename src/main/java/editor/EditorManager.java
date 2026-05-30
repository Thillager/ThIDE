package editor;

import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import ui.ConsolePanel;
import ui.WordManagerDialog;
import ui.MainWindow;
import config.TIDEPreferences;
import config.TIDEProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import ui.OutlinePanel;

public class EditorManager {

	private final JFrame parent;
	private final JTabbedPane editorTabs;
	private final Map<Component, File> openFiles;
	private final ConsolePanel consolePanel;
	private final WordManagerDialog wordManagerDialog;
	private OutlinePanel outlinePanel;

	public EditorManager(JFrame parent, JTabbedPane editorTabs, Map<Component, File> openFiles,
		ConsolePanel consolePanel, WordManagerDialog wordManagerDialog) {
		this.parent            = parent;
		this.editorTabs        = editorTabs;
		this.openFiles         = openFiles;
		this.consolePanel      = consolePanel;
		this.wordManagerDialog = wordManagerDialog;
	}


	public void applyFontSizeToAllEditors(int size) {
		for (int i = 0; i < editorTabs.getTabCount(); i++) {
			Component tab = editorTabs.getComponentAt(i);
			if (tab instanceof RTextScrollPane sp) {
				Component view = sp.getViewport().getView();
				if (view instanceof RSyntaxTextArea textArea) {
					textArea.setFont(new Font(TIDEProperties.EDITOR_FONT, Font.PLAIN, size));
				}
			}
		}
	}

	public void updateUIWithLocale(Locale neueLocale) {
		Locale.setDefault(neueLocale);

		for (int i = 0; i < editorTabs.getTabCount(); i++) {
			Component tab = editorTabs.getComponentAt(i);

			if (tab instanceof JScrollPane scrollPane) {
				Component view = scrollPane.getViewport().getView();

				if (view instanceof RSyntaxTextArea textArea) {
					textArea.setLocale(neueLocale);
					translatePopupMenuRecursive(textArea.getPopupMenu(), neueLocale);
					SwingUtilities.updateComponentTreeUI(textArea);
				}
			}
		}
	}


	public void setOutlinePanel(OutlinePanel outlinePanel) {
		this.outlinePanel = outlinePanel;
	}

	private void installPopupLocalizationHook(RSyntaxTextArea textArea) {
		JPopupMenu popup = textArea.getPopupMenu();

		if (popup != null) {
			popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
					@Override
					public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
						SwingUtilities.invokeLater(() ->
							translatePopupMenuRecursive(popup, Locale.getDefault())
						);
					}

					@Override
					public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}

					@Override
					public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
				});
		}
	}

	private void translatePopupMenuRecursive(JPopupMenu popup, Locale locale) {
		if (popup == null) return;

		boolean english = locale.getLanguage().equalsIgnoreCase("en");

		for (Component c : popup.getComponents()) {
			if (c instanceof JMenu menu) {
				String txt = menu.getText();
				if (txt != null && !txt.isEmpty()) {
					String translated = translateMenuItem(txt, english);
					menu.setText(translated);
				}
				translatePopupMenuRecursive(menu.getPopupMenu(), locale);
			} else if (c instanceof JMenuItem item) {
				String txt = item.getText();
				if (txt != null && !txt.isEmpty()) {
					String translated = translateMenuItem(txt, english);
					item.setText(translated);
				}
			}
		}

		popup.revalidate();
		popup.repaint();
	}

	private String translateMenuItem(String text, boolean toEnglish) {
		String normalized = normalizeToEnglish(text);

		if (toEnglish) {
			return normalized;
		} else {
			return translateToGerman(normalized);
		}
	}

	private String normalizeToEnglish(String text) {
		if (text == null || text.isEmpty()) return text;

		if (text.contains("Rückgängig") && text.contains("nicht")) return "Can't Undo";
		if (text.contains("Rückgängig")) return "Undo";

		if (text.contains("Wiederherstellen") || text.contains("Wiederholen")) return "Redo";
		if (text.contains("nicht möglich") || text.contains("Kann nicht") && text.contains("wiederholen")) return "Can't Redo";
		if (text.contains("Kann nicht wiederherstellen")) return "Can't Redo";

		if (text.contains("Ausschneiden")) return "Cut";
		if (text.contains("Kopieren")) return "Copy";
		if (text.contains("Einfügen")) return "Paste";
		if (text.contains("Löschen")) return "Delete";
		if (text.contains("Alles") && text.contains("auswählen")) return "Select All";
		if (text.contains("markieren")) return "Select All";

		if (text.contains("Falten") && !text.contains("Falz")) return "Folding";
		if (text.contains("Entfalten")) return "Unfolding";

		if (text.contains("Falz") && text.contains("umschalten")) return "Toggle Current Fold";
		if (text.contains("Kommentare") && text.contains("einklappen")) return "Collapse All Comments";
		if (text.contains("Falze") && text.contains("einklappen")) return "Collapse All Folds";
		if (text.contains("Falze") && text.contains("ausklappen")) return "Expand All Folds";

		return text;
	}


	private String translateToGerman(String englishText) {
		return switch (englishText) {
			case "Undo" -> "Rückgängig";
			case "Can't Undo" -> "Kann nicht rückgängig";
			case "Redo" -> "Wiederherstellen";
			case "Can't Redo" -> "Kann nicht wiederherstellen";
			case "Cut" -> "Ausschneiden";
			case "Copy" -> "Kopieren";
			case "Paste" -> "Einfügen";
			case "Delete" -> "Löschen";
			case "Select All" -> "Alles markieren";
			case "Folding" -> "Falten";
			case "Unfolding" -> "Entfalten";

			case "Toggle Current Fold" -> "Aktuellen Falz umschalten";
			case "Collapse All Comments" -> "Alle Kommentare einklappen";
			case "Collapse All Folds" -> "Alle Falze einklappen";
			case "Expand All Folds" -> "Alle Falze ausklappen";

			default -> englishText;
		};
	}


	public void openFileInEditor(File file) {
		for (int i = 0; i < editorTabs.getTabCount(); i++) {
			if (file.equals(openFiles.get(editorTabs.getComponentAt(i)))) {
				editorTabs.setSelectedIndex(i);
				return;
			}
		}
		try {
			String content = Files.readString(file.toPath());
			RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);

			textArea.setLocale(Locale.getDefault());

			SwingUtilities.invokeLater(() -> {
					installPopupLocalizationHook(textArea);
				});

			textArea.setText(content);

			String fileName = file.getName().toLowerCase();
			if      (fileName.endsWith(".java"))                        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			else if (fileName.endsWith(".py"))                          textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
			else if (fileName.endsWith(".c") || fileName.endsWith(".h"))       textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
			else if (fileName.endsWith(".cpp") || fileName.endsWith(".hpp"))   textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
			else if (fileName.endsWith(".xml"))                         textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
			else if (fileName.endsWith(".bat") || fileName.endsWith(".cmd"))   textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
			else textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);

			textArea.setCodeFoldingEnabled(true);
			textArea.setFont(new Font (TIDEProperties.EDITOR_FONT, Font.PLAIN, TIDEProperties.EDITOR_FONT_SIZE));

			try {
				Theme theme = Theme.load(getClass().getResourceAsStream(
						"/org/fife/ui/rsyntaxtextarea/themes/monokai.xml"));
				theme.apply(textArea);
			} catch (IOException ioe) {
				textArea.setBackground(new Color(30, 31, 34));
			}
			textArea.setCaretColor(Color.WHITE);


			// ── Geteilte State-Holder für SmoothScroll ↔ Blur-Effekt ────────────
			float[] sharedDynIntensity = { 0.0f };
			int[]   sharedScrollDir    = { 0 };

			// ─────────────────────────────────────────────────────────────────────
			// MOTION-BLUR SCROLL-PANE
			// ─────────────────────────────────────────────────────────────────────
			RTextScrollPane sp = new RTextScrollPane(textArea) {

				// ── State ────────────────────────────────────────────────────────
				private int   lastScrollValue = -1;
				private VolatileImage volatileBuffer = null;

				{
					getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

					getVerticalScrollBar().addAdjustmentListener(e -> {
							int newVal = e.getValue();
							if (lastScrollValue != -1) {
								int delta = newVal - lastScrollValue;
								if (delta != 0) sharedScrollDir[0] = delta > 0 ? 1 : -1;
							}
							lastScrollValue = newVal;
						});
				}

				@Override
				public void paint(Graphics g) {
					// ── Motion Blur komplett deaktiviert? ────────────────────────
					if (!TIDEPreferences.getMotionBlurEnabled()) {
						super.paint(g);
						return;
					}

					float dynIntensity = sharedDynIntensity[0];

					// ── Früh-Ausstieg: Wenn das System steht, sofort normal zeichnen ──
					if (dynIntensity < 0.01f) {
						super.paint(g);
						return;
					}

					int w = getWidth();
					int h = getHeight();

					// ── VolatileImage (VRAM) frisch halten ───────────────────────
					GraphicsConfiguration gc = getGraphicsConfiguration();
					if (volatileBuffer == null
						|| volatileBuffer.getWidth()  != w
						|| volatileBuffer.getHeight() != h
						|| volatileBuffer.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
						if (volatileBuffer != null) volatileBuffer.flush();
						volatileBuffer = gc.createCompatibleVolatileImage(w, h, Transparency.OPAQUE);
					}

					do {
						if (volatileBuffer.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
							volatileBuffer.flush();
							volatileBuffer = gc.createCompatibleVolatileImage(w, h, Transparency.OPAQUE);
						}
						Graphics2D gBuf = volatileBuffer.createGraphics();
						gBuf.setRenderingHint(RenderingHints.KEY_RENDERING,
							RenderingHints.VALUE_RENDER_SPEED);
						super.paint(gBuf);
						gBuf.dispose();
					} while (volatileBuffer.contentsLost());

					// ── Auf Haupt-Graphics rendern ────────────────────────────────
					Graphics2D g2d = (Graphics2D) g.create();

					g2d.setRenderingHint(
						RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
					);



					// ── 1. Basis-Bild (Wird bei langem/schnellem Scrollen dezent transparenter) ──
					// ── 2. Basis-Bild ────────────────────────────────────────────
					float baseAlpha = 1.0f - (0.10f * dynIntensity);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, baseAlpha));
					g2d.drawImage(volatileBuffer, 0, 0, null);

					// ── 3. ORGANISCHER MOTION-BLUR TRAIL ────────────────────────
					float blurAlpha = 0.75f * dynIntensity;
					if (blurAlpha > 0.005f) {
						int trailDir = -sharedScrollDir[0]; // Entgegen der echten Scrollrichtung

						float maxTrail = 40.0f * dynIntensity;

						// Schicht 1: Nah und dicker
						g2d.setComposite(AlphaComposite.getInstance(
								AlphaComposite.SRC_OVER, blurAlpha * 0.40f));
						g2d.drawImage(volatileBuffer, 0, Math.round(maxTrail * 0.30f * trailDir), null);

						// Schicht 2: Der dynamische Ausläufer
						g2d.setComposite(AlphaComposite.getInstance(
								AlphaComposite.SRC_OVER, blurAlpha * 0.20f));
						g2d.drawImage(volatileBuffer, 0, Math.round(maxTrail * 0.80f * trailDir), null);
					}

					g2d.dispose();
				}
			};


			sp.getVerticalScrollBar().setUnitIncrement(0);
			javax.swing.Timer smoothScrollTimer = null;
			if (parent instanceof MainWindow mw) {
				smoothScrollTimer = mw.enableSmoothScrolling(sp, sharedDynIntensity, sharedScrollDir);
			}
			final javax.swing.Timer timerRef = smoothScrollTimer;
			sp.getVerticalScrollBar().setUnitIncrement(
				textArea.getFontMetrics(textArea.getFont()).getHeight()
			);
			sp.setBorder(null);

			JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
			tabHeader.setOpaque(false);
			tabHeader.add(new JLabel(file.getName()));

			JButton closeBtn = new JButton("×");
			closeBtn.setBorder(null);
			closeBtn.setContentAreaFilled(false);
			closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
			closeBtn.addActionListener(e -> {
					if (timerRef != null) timerRef.stop(); // Memory Leak Fix
					openFiles.remove(sp);
					editorTabs.remove(sp);
				});
			tabHeader.add(closeBtn);

			editorTabs.addTab(file.getName(), sp);
			editorTabs.setTabComponentAt(editorTabs.getTabCount() - 1, tabHeader);
			editorTabs.setSelectedComponent(sp);
			openFiles.put(sp, file);
			textArea.requestFocusInWindow();

			if (outlinePanel != null) {
				outlinePanel.refresh(textArea, file.getName());
			}

			DefaultCompletionProvider provider = createCompletionProvider(textArea);
			AutoCompletion ac = new AutoCompletion(provider);
			ac.setAutoCompleteSingleChoices(false);
			ac.setAutoActivationEnabled(true);
			ac.setAutoActivationDelay(TIDEProperties.AUTOCOMPLETE_DELAY);
			ac.install(textArea);

			Set<String> knownWords = new HashSet<>();
			String[] initialKeywords = {"public", "pri	vate", "static", "void", "class", "import",
				"String", "int", "boolean", "new", "return"};
			knownWords.addAll(Arrays.asList(initialKeywords));
			String existingContent = textArea.getText();
			if (existingContent != null) {
				for (String t : existingContent.split("[^\\w]+")) {
					if (t.length() > TIDEProperties.AUTOCOMPLETE_MIN_LEN) knownWords.add(t);
				}
			}

			textArea.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent e) {
						char c = e.getKeyChar();
						if (!Character.isLetterOrDigit(c) && c != KeyEvent.CHAR_UNDEFINED) {
							try {
								int caret = textArea.getCaretPosition() - 1;
								if (caret < 1) return;
								String text  = textArea.getText(0, caret);
								int start    = caret - 1;
								while (start >= 0 && Character.isLetterOrDigit(text.charAt(start))) {
									start--;
								}
								start++;
								if (start < caret) {
									String word = text.substring(start, caret);
									if (word.length() > TIDEProperties.AUTOCOMPLETE_MIN_LEN && knownWords.add(word)) {
										provider.addCompletion(new BasicCompletion(provider, word));
									}
								}
							} catch (Exception ignored) {}
						}
					}
				});

			JButton manageWordsBtn = new JButton("Wörter");
			manageWordsBtn.setFont(manageWordsBtn.getFont().deriveFont(10f));
			manageWordsBtn.setForeground(new Color(180, 180, 255));
			manageWordsBtn.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
			manageWordsBtn.setContentAreaFilled(false);
			manageWordsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
			manageWordsBtn.setToolTipText("Gelernte Wörter verwalten / löschen");
			manageWordsBtn.addActionListener(ev -> wordManagerDialog.show(provider, knownWords));
			tabHeader.add(manageWordsBtn);

		} catch (Exception e) {
			consolePanel.log("Öffnen fehlgeschlagen\n", Color.RED);
		}
	}


	public void saveCurrentFile() {
		Component tab = editorTabs.getSelectedComponent();
		if (tab instanceof RTextScrollPane sp) {
			File file = openFiles.get(sp);
			RSyntaxTextArea ta = (RSyntaxTextArea) sp.getTextArea();
			try {
				Files.writeString(file.toPath(), ta.getText());
				consolePanel.log("[SAVE] " + file.getName() + " gespeichert.\n", Color.GREEN);
			} catch (IOException e) {
				consolePanel.log("Fehler beim Speichern\n", Color.RED);
			}
		}
	}

	public void saveAllFiles() {
		int saved = 0;
		int errors = 0;
		for (int i = 0; i < editorTabs.getTabCount(); i++) {
			Component tab = editorTabs.getComponentAt(i);
			if (!(tab instanceof RTextScrollPane sp)) continue;
			File file = openFiles.get(sp);
			if (file == null) continue;
			RSyntaxTextArea ta = (RSyntaxTextArea) sp.getTextArea();
			try {
				Files.writeString(file.toPath(), ta.getText());
				saved++;
			} catch (IOException e) {
				consolePanel.log("[SAVE] Fehler beim Speichern von " + file.getName() + "\n", Color.RED);
				errors++;
			}
		}
		if (errors == 0) {
			consolePanel.log("[SAVE] " + saved + " Datei(en) gespeichert.\n", Color.GREEN);
		} else {
			consolePanel.log("[SAVE] " + saved + " gespeichert, " + errors + " Fehler.\n", Color.ORANGE);
		}
	}

	public void formatCurrentFile() {
		Component tab = editorTabs.getSelectedComponent();
		if (!(tab instanceof RTextScrollPane sp)) return; 
		File file = openFiles.get(sp);

		RSyntaxTextArea ta = (RSyntaxTextArea) sp.getTextArea();
		String[] lines     = ta.getText().split("\n", -1);
		int      tabSize   = ta.getTabSize();
		boolean  useTabs   = !ta.getTabsEmulated();
		String   tabUnit   = useTabs ? "\t" : " ".repeat(tabSize);

		StringBuilder result = new StringBuilder();
		int     indent       = 0;

		for (int i = 0; i < lines.length; i++) {
			String trimmed = lines[i].stripLeading();

			if (trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]")) {
	indent = Math.max(0, indent - 1);
}

if (!trimmed.isEmpty()) {
	result.append(tabUnit.repeat(indent)).append(trimmed);
}
if (i < lines.length - 1) result.append("\n");

long opens  = trimmed.chars().filter(c -> c == '{' || c == '(' || c == '[').count();
			long closes = trimmed.chars().filter(c -> c == '}' || c == ')' || c == ']').count();

if (trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]")) {
closes = Math.max(0, closes - 1);
}

indent = Math.max(0, indent + (int)(opens - closes));
}

int caretPos = ta.getCaretPosition();
ta.setText(result.toString());
ta.setCaretPosition(Math.min(caretPos, ta.getDocument().getLength()));
consolePanel.log("[FORMAT] Datei formatiert.\n", Color.GREEN);
}

public File getActiveFile() {
	Component tab = editorTabs.getSelectedComponent();
	if (tab != null) return openFiles.get(tab);
	return null;
}

private DefaultCompletionProvider createCompletionProvider(RSyntaxTextArea textArea) {
	DefaultCompletionProvider provider = new DefaultCompletionProvider();
	Set<String> seen = new HashSet<>();

	String[] keywords = {"public", "private", "static", "void", "class", "import",
		"String", "int", "boolean", "new", "return"};
	for (String kw : keywords) {
		if (seen.add(kw)) {
			provider.addCompletion(new BasicCompletion(provider, kw));
		}
	}

	String content = textArea.getText();
	if (content != null && !content.isEmpty()) {
		String[] tokens = content.split("[^\\w]+");
		for (String token : tokens) {
			if (token.length() > TIDEProperties.AUTOCOMPLETE_MIN_LEN && seen.add(token)) {
				provider.addCompletion(new BasicCompletion(provider, token));
			}
		}
	}

	return provider;
}
}