package ui;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class WordManagerDialog {

    private final JFrame parent;
    private final ConsolePanel consolePanel;

    public WordManagerDialog(JFrame parent, ConsolePanel consolePanel) {
        this.parent       = parent;
        this.consolePanel = consolePanel;
    }

    /**
     * Öffnet einen Dialog, in dem alle gelernten Wörter angezeigt
     * und einzeln oder mehrfach gelöscht werden können.
     */
    public void show(DefaultCompletionProvider provider, Set<String> knownWords) {
        JDialog dialog = new JDialog(parent, "Wörter verwalten", true);
        dialog.setSize(400, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        java.util.List<String> sortedWords = new ArrayList<>(knownWords);
        Collections.sort(sortedWords, String.CASE_INSENSITIVE_ORDER);
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String w : sortedWords) listModel.addElement(w);

        JList<String> wordList = new JList<>(listModel);
        wordList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        wordList.setBackground(new Color(30, 31, 34));
        wordList.setForeground(Color.WHITE);
        wordList.setFont(new Font("Consolas", Font.PLAIN, 13));

        JScrollPane scroll = new JScrollPane(wordList);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                "Gelernte Wörter (" + listModel.size() + ")",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                null, Color.LIGHT_GRAY));

        JTextField filterField = new JTextField();
        filterField.setBackground(new Color(45, 47, 49));
        filterField.setForeground(Color.WHITE);
        filterField.setCaretColor(Color.WHITE);
        filterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        filterField.setToolTipText("Filtern...");

        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filter() {
                String q = filterField.getText().toLowerCase();
                listModel.clear();
                for (String w : sortedWords) {
                    if (q.isEmpty() || w.toLowerCase().contains(q)) listModel.addElement(w);
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        JPanel topPanel = new JPanel(new BorderLayout(4, 4));
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("Suchen: "), BorderLayout.WEST);
        topPanel.add(filterField, BorderLayout.CENTER);

        JButton btnDelete = new JButton("Ausgewählte löschen");
        JButton btnClose  = new JButton("Schließen");
        btnDelete.setForeground(new Color(255, 100, 100));
        btnDelete.addActionListener(e -> {
            java.util.List<String> selected = wordList.getSelectedValuesList();
            if (selected.isEmpty()) return;
            for (String word : selected) {
                knownWords.remove(word);
                sortedWords.remove(word);
                listModel.removeElement(word);
            }
            provider.clear();
            for (String w : knownWords) {
                provider.addCompletion(new BasicCompletion(provider, w));
            }
            scroll.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY),
                    "Gelernte Wörter (" + listModel.size() + ")",
                    javax.swing.border.TitledBorder.LEFT,
                    javax.swing.border.TitledBorder.TOP,
                    null, Color.LIGHT_GRAY));
            consolePanel.log("[INFO] " + selected.size() + " Wort/Wörter aus Autocomplete entfernt.\n", Color.LIGHT_GRAY);
        });
        btnClose.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClose);

        dialog.getContentPane().setBackground(new Color(43, 45, 48));
        dialog.add(topPanel, BorderLayout.NORTH);
        dialog.add(scroll,   BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
