package ui;

import editor.EditorManager;
import config.Theme;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"serial", "this-escape"})
public class GlobalSearchPanel extends JPanel {

    private JTextField searchField;
    private JCheckBox matchCaseCB;
    private DefaultListModel<SearchMatch> listModel;
    private JList<SearchMatch> resultList;
    
    private final EditorManager editorManager;
    private final JTabbedPane editorTabs;
    private File currentProjectFolder;

    public GlobalSearchPanel(EditorManager editorManager, JTabbedPane editorTabs) { 
        super(new BorderLayout());
        this.editorManager = editorManager;
        this.editorTabs = editorTabs;

        Theme t = MainWindow.THEME;

        setBackground(t.backgroundLight);
        setBorder(new EmptyBorder(5, 10, 5, 10));
        setVisible(false);

        // ---- Top-Leiste (Eingabe) ----
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        
        searchField = new JTextField(25);
        searchField.setBackground(t.background);
        searchField.setForeground(t.foreground);
        searchField.setCaretColor(t.foreground);

        JButton btnSearch = new JButton("Projekt durchsuchen");
        matchCaseCB = new JCheckBox("Groß/Klein beachten");
        matchCaseCB.setForeground(t.foreground);
        matchCaseCB.setOpaque(false);
        JButton btnClose = new JButton("x");

        JLabel label = new JLabel("Globale Suche:");
        label.setForeground(t.foreground);

        topPanel.add(label);
        topPanel.add(searchField);
        topPanel.add(btnSearch);
        topPanel.add(matchCaseCB);
        topPanel.add(btnClose);

        // ---- Ergebnisliste ----
        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setBackground(t.background);
        resultList.setForeground(t.foregroundDim);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane listScrollPane = new JScrollPane(resultList);
        listScrollPane.setPreferredSize(new Dimension(0, 120));
        listScrollPane.setBackground(t.background);
        listScrollPane.getViewport().setBackground(t.background);
        listScrollPane.setBorder(BorderFactory.createLineBorder(t.border));

        add(topPanel, BorderLayout.NORTH);
        add(listScrollPane, BorderLayout.CENTER);

        // ---- Event-Listener ----
        searchField.addActionListener(e -> executeGlobalSearch());
        btnSearch.addActionListener(e -> executeGlobalSearch());
        btnClose.addActionListener(e -> setVisible(false));

        // Doppelklick auf ein Ergebnis öffnet die Datei an der Zeile
        resultList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    SearchMatch selected = resultList.getSelectedValue();
                    if (selected != null) {
                        openMatchInEditor(selected);
                    }
                }
            }
        });
    }

    public void setSubsystems(File projectFolder) {
        this.currentProjectFolder = projectFolder;
    }

    public JTextField getSearchField() {
        return searchField;
    }

    private void executeGlobalSearch() {
        listModel.clear();
        String query = searchField.getText().trim();
        if (query.isEmpty() || currentProjectFolder == null) return;

        List<SearchMatch> matches = new ArrayList<>();
        searchDirectory(currentProjectFolder, query, matchCaseCB.isSelected(), matches);

        if (matches.isEmpty()) {
            listModel.addElement(new SearchMatch(null, 0, "Keine Treffer gefunden."));
        } else {
            for (SearchMatch m : matches) {
                listModel.addElement(m);
            }
        }
    }

    private void searchDirectory(File dir, String query, boolean matchCase, List<SearchMatch> matches) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                // Überspringe typische Build- und Versionsverwaltungs-Ordner
                if (!f.getName().equals(".git") && !f.getName().equals("bin") && !f.getName().equals("out")) {
                    searchDirectory(f, query, matchCase, matches);
                }
            } else {
                // Nur Textdateien/Code-Dateien durchsuchen
                String name = f.getName().toLowerCase();
                if (name.endsWith(".java") || name.endsWith(".py") || name.endsWith(".c") || 
                    name.endsWith(".cpp") || name.endsWith(".h") || name.endsWith(".txt") || name.endsWith(".xml")) {
                    searchFile(f, query, matchCase, matches);
                }
            }
        }
    }

    private void searchFile(File file, String query, boolean matchCase, List<SearchMatch> matches) {
        String finalQuery = matchCase ? query : query.toLowerCase();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 1;
            while ((line = br.readLine()) != null) {
                String compLine = matchCase ? line : line.toLowerCase();
                if (compLine.contains(finalQuery)) {
                    matches.add(new SearchMatch(file, lineNum, line.trim()));
                }
                lineNum++;
            }
        } catch (Exception e) {
            // Ignorieren oder loggen
        }
    }

    private void openMatchInEditor(SearchMatch match) {
        if (match.file == null) return;
        // Datei im Editor öffnen
        editorManager.openFileInEditor(match.file);
        
        // Per invokeLater warten, bis der Editor den Tab gewechselt/geladen hat
        SwingUtilities.invokeLater(() -> {
            Component selectedTab = editorTabs.getSelectedComponent(); 
            if (selectedTab instanceof org.fife.ui.rtextarea.RTextScrollPane sp) {
                if (sp.getViewport().getView() instanceof org.fife.ui.rsyntaxtextarea.RSyntaxTextArea rsta) {
                    try {
                        // Zeilennummer in RSyntaxTextArea ist 0-basiert
                        int lineOffset = rsta.getLineStartOffset(match.lineNumber - 1);
                        rsta.setCaretPosition(lineOffset);
                        rsta.requestFocusInWindow();
                    } catch (Exception e) {
                        // Falls die Zeile außerhalb des Textbereichs liegt
                    }
                }
            }
        });
    }

    // Datentyp für ein Suchergebnis
    private static class SearchMatch {
        File file;
        int lineNumber;
        String lineText;

        SearchMatch(File file, int lineNumber, String lineText) {
            this.file = file;
            this.lineNumber = lineNumber;
            this.lineText = lineText;
        }

        @Override
        public String toString() {
            if (file == null) return lineText;
            return "[" + file.getName() + ":" + lineNumber + "] " + lineText;
        }
    }
}
