package editor;

import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import ui.ConsolePanel;
import ui.WordManagerDialog;
import config.TIDEProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

public class EditorManager {

    private final JFrame parent;
    private final JTabbedPane editorTabs;
    private final Map<Component, File> openFiles;
    private final ConsolePanel consolePanel;
    private final WordManagerDialog wordManagerDialog;

    public EditorManager(JFrame parent, JTabbedPane editorTabs, Map<Component, File> openFiles,
                         ConsolePanel consolePanel, WordManagerDialog wordManagerDialog) {
        this.parent            = parent;
        this.editorTabs        = editorTabs;
        this.openFiles         = openFiles;
        this.consolePanel      = consolePanel;
        this.wordManagerDialog = wordManagerDialog;
    }

    public void updateUIWithLocale(Locale neueLocale) {
    try {
        boolean isEnglish = neueLocale.getLanguage().equalsIgnoreCase("en");

        for (int i = 0; i < editorTabs.getTabCount(); i++) {
            Component tab = editorTabs.getComponentAt(i);
            
            if (tab instanceof JScrollPane) {
                Component view = ((JScrollPane) tab).getViewport().getView();
                
                if (view instanceof org.fife.ui.rsyntaxtextarea.RSyntaxTextArea) {
                    org.fife.ui.rsyntaxtextarea.RSyntaxTextArea textArea = 
                        (org.fife.ui.rsyntaxtextarea.RSyntaxTextArea) view;
                    
                    // 1. Locale setzen (wichtig, falls das Menü noch nie offen war)
                    textArea.setLocale(neueLocale);
                    
                    // 2. Bestehendes Menü live übersetzen (falls es schon im Speicher existiert)
                    JPopupMenu popup = textArea.getPopupMenu();
                    if (popup != null) {
                        for (Component comp : popup.getComponents()) {
                            if (comp instanceof JMenuItem) {
                                JMenuItem item = (JMenuItem) comp;
                                String text = item.getText();
                                
                                if (text != null) {
                                    // .contains() fängt auch deine manuellen oder modifizierten Einträge ab
                                    if (isEnglish) {
                                        if (text.contains("R\u00FCckg\u00E4ngig") || text.contains("Rückgängig")) item.setText("Undo");
                                        if (text.contains("Wiederherstellen")) item.setText("Redo");
                                        if (text.contains("Ausschneiden")) item.setText("Cut");
                                        if (text.contains("Kopieren")) item.setText("Copy");
                                        if (text.contains("Einf\u00FCgen") || text.contains("Einfügen")) item.setText("Paste");
                                        if (text.contains("L\u00F6schen") || text.contains("Löschen")) item.setText("Delete");
                                        if (text.contains("Alles ausw\u00E4hlen") || text.contains("Alles auswählen")) item.setText("Select All");
                                    } else {
                                        if (text.equalsIgnoreCase("Undo")) item.setText("R\u00FCckg\u00E4ngig");
                                        if (text.equalsIgnoreCase("Redo")) item.setText("Wiederherstellen");
                                        if (text.equalsIgnoreCase("Cut")) item.setText("Ausschneiden");
                                        if (text.equalsIgnoreCase("Copy")) item.setText("Kopieren");
                                        if (text.equalsIgnoreCase("Paste")) item.setText("Einf\u00FCgen");
                                        if (text.equalsIgnoreCase("Delete")) item.setText("L\u00F6schen");
                                        if (text.equalsIgnoreCase("Select All")) item.setText("Alles ausw\u00E4hlen");
                                    }
                                }
                            }
                        }
                    }

                    // 3. ActionMap aktualisieren für Shortcuts und interne Funktionen
                    ActionMap am = textArea.getActionMap();
                    if (am != null && am.allKeys() != null) {
                        for (Object key : am.allKeys()) {
                            if (key == null) continue;
                            String keyStr = key.toString().toLowerCase();
                            Action a = am.get(key);
                            if (a == null) continue;

                            if (keyStr.contains("undo")) {
                                a.putValue(Action.NAME, isEnglish ? "Undo" : "R\u00FCckg\u00E4ngig");
                            } else if (keyStr.contains("redo")) {
                                a.putValue(Action.NAME, isEnglish ? "Redo" : "Wiederherstellen");
                            }
                        }
                    }
                }
            }
        }
    } catch (Exception e) {
        System.err.println("Fehler beim Live-Sprachwechsel im Editor: " + e.getMessage());
    }
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

            RTextScrollPane sp = new RTextScrollPane(textArea);
            sp.setBorder(null);

            JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            tabHeader.setOpaque(false);
            tabHeader.add(new JLabel(file.getName()));

            JButton closeBtn = new JButton("×");
            closeBtn.setBorder(null);
            closeBtn.setContentAreaFilled(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.addActionListener(e -> { openFiles.remove(sp); editorTabs.remove(sp); });
            tabHeader.add(closeBtn);

            editorTabs.addTab(file.getName(), sp);
            editorTabs.setTabComponentAt(editorTabs.getTabCount() - 1, tabHeader);
            editorTabs.setSelectedComponent(sp);
            openFiles.put(sp, file);
            textArea.requestFocusInWindow();

            // --- AUTOCOMPLETE SETUP ---
            DefaultCompletionProvider provider = createCompletionProvider(textArea);
            AutoCompletion ac = new AutoCompletion(provider);
            ac.setAutoCompleteSingleChoices(false);
            ac.setAutoActivationEnabled(true);
            ac.setAutoActivationDelay(TIDEProperties.AUTOCOMPLETE_DELAY);
            ac.install(textArea);

            // --- LERN-FUNKTION ---
            Set<String> knownWords = new HashSet<>();
            String[] initialKeywords = {"public", "private", "static", "void", "class", "import",
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

            // --- WÖRTER VERWALTEN ---
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
        if (tab instanceof RTextScrollPane) {
            File file = openFiles.get(tab);
            RSyntaxTextArea ta = (RSyntaxTextArea) ((RTextScrollPane) tab).getTextArea();
            try {
                Files.writeString(file.toPath(), ta.getText());
                consolePanel.log("[SAVE] " + file.getName() + " gespeichert.\n", Color.GREEN);
            } catch (IOException e) {
                consolePanel.log("Fehler beim Speichern\n", Color.RED);
            }
        }
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
