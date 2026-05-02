import com.formdev.flatlaf.FlatDarkLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class TIDE extends JFrame {

    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private JTabbedPane editorTabs;
    private JTextPane consolePane;
    private JTextField terminalInput;
    private File currentProjectFolder;
    private Map<Component, File> openFiles = new HashMap<>();

    private JComboBox<String> modeSelector;
    private JTextField mainClassInput;
    private JButton btnTBuild;
    private JLabel mainClassLabel;

    private static final String MODE_JAVA = "Java";
    private static final String MODE_PYTHON = "Python";
    private static final String MODE_C = "C";
    private static final String MODE_CPP = "C++";
    private static final String MODE_BATCH = "Batch";

    public static void main(String[] args) {
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("TabbedPane.selectedBackground", new Color(60, 63, 65));
        UIManager.put("TabbedPane.showTabSeparators", true);

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Konnte FlatLaf nicht laden.");
        }
        SwingUtilities.invokeLater(() -> new TIDE().setVisible(true));
    }

    public TIDE() {
        setTitle("T-IDE - Leichte IDE");
        setSize(1300, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        // --- Toolbar ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(new EmptyBorder(8, 10, 8, 10));
        toolBar.setBackground(new Color(43, 45, 48));

        JButton btnOpen = new JButton("📁 Ordner öffnen");
        JButton btnSave = new JButton("💾 Speichern");

        modeSelector = new JComboBox<>(new String[]{MODE_JAVA, MODE_PYTHON, MODE_C, MODE_CPP, MODE_BATCH});
        modeSelector.setMaximumSize(new Dimension(100, 30));

        mainClassLabel = new JLabel(" Main-Class: ");
        mainClassInput = new JTextField("Main", 15);
        mainClassInput.setMaximumSize(new Dimension(200, 30));

        JButton btnRun = new JButton("▶ Run");
        btnTBuild = new JButton("🛠 T-Build");
        JButton btnClear = new JButton("🗑 Clear Console");

        btnRun.setForeground(new Color(80, 200, 120));
        btnRun.setFont(btnRun.getFont().deriveFont(Font.BOLD));
        btnTBuild.setForeground(new Color(100, 150, 255));

        toolBar.add(btnOpen);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnSave);
        toolBar.addSeparator(new Dimension(20, 30));

        toolBar.add(new JLabel("Modus: "));
        toolBar.add(modeSelector);
        toolBar.add(mainClassLabel);
        toolBar.add(mainClassInput);

        toolBar.addSeparator(new Dimension(20, 30));
        toolBar.add(btnRun);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnTBuild);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(btnClear);

        add(toolBar, BorderLayout.NORTH);

        // --- Dateibaum (Links) ---
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Kein Projekt geöffnet");
        treeModel = new DefaultTreeModel(root);
        fileTree = new JTree(treeModel);
        fileTree.setBackground(new Color(30, 31, 34));
        fileTree.setBorder(new EmptyBorder(10, 10, 10, 10));

        fileTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
                    if (node != null && node.getUserObject() instanceof FileNode) {
                        File selectedFile = ((FileNode) node.getUserObject()).getFile();
                        if (selectedFile.isFile()) openFileInEditor(selectedFile);
                    }
                }
            }
        });

        JScrollPane treeScroll = new JScrollPane(fileTree);
        treeScroll.setPreferredSize(new Dimension(250, 0));
        treeScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));

        // --- Editor Tabs ---
        editorTabs = new JTabbedPane();
        editorTabs.setBorder(null);

        // --- TERMINAL & KONSOLE (Unten) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());

        consolePane = new JTextPane();
        consolePane.setBackground(new Color(25, 25, 25));
        consolePane.setFont(new Font("Consolas", Font.PLAIN, 14));
        consolePane.setEditable(false);
        JScrollPane consoleScroll = new JScrollPane(consolePane);
        consoleScroll.setBorder(null);

        terminalInput = new JTextField();
        terminalInput.setBackground(new Color(35, 35, 35));
        terminalInput.setForeground(Color.WHITE);
        terminalInput.setCaretColor(Color.GREEN);
        terminalInput.setFont(new Font("Consolas", Font.PLAIN, 14));
        terminalInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        bottomPanel.add(consoleScroll, BorderLayout.CENTER);
        bottomPanel.add(terminalInput, BorderLayout.SOUTH);
        bottomPanel.setPreferredSize(new Dimension(0, 250));

        // --- Layout Splitting ---
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorTabs, bottomPanel);
        verticalSplit.setResizeWeight(0.7);
        verticalSplit.setDividerSize(4);
        verticalSplit.setBorder(null);

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, verticalSplit);
        horizontalSplit.setDividerSize(4);
        horizontalSplit.setBorder(null);
        add(horizontalSplit, BorderLayout.CENTER);

        // --- Event Listeners ---
        btnOpen.addActionListener(e -> openFolderDialog());
        btnSave.addActionListener(e -> saveCurrentFile());
        btnRun.addActionListener(e -> runProject());
        btnClear.addActionListener(e -> consolePane.setText(""));
        btnTBuild.addActionListener(e -> handleTBuild());

        modeSelector.addActionListener(e -> updateDynamicUI());

        terminalInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String cmd = terminalInput.getText().trim();
                    if (!cmd.isEmpty()) {
                        executeCommand(cmd, false);
                        terminalInput.setText("");
                    }
                }
            }
        });

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
                saveCurrentFile();
                return true;
            }
            return false;
        });

        updateDynamicUI();
    }

    private void updateDynamicUI() {
        String mode = (String) modeSelector.getSelectedItem();
        boolean isJava = MODE_JAVA.equals(mode);

        btnTBuild.setVisible(isJava);
        mainClassLabel.setVisible(isJava);
        mainClassInput.setVisible(isJava);

        revalidate();
        repaint();
    }

    // ================== T.XML LESEN ==================

    /**
     * Liest die T.xml aus dem Projektordner und befüllt automatisch:
     * - mainClassInput  (mainClass)
     * - Fenstertitel    (appName + version)
     *
     * Gibt eine Warnung in der Konsole aus, wenn keine T.xml vorhanden ist.
     */
    private void loadTXml(File folder) {
        File txml = new File(folder, "T.xml");

        if (!txml.exists()) {
            log("[WARNUNG] Keine T.xml im Projektordner gefunden. Bitte Main-Klasse manuell eingeben.\n", Color.ORANGE);
            setTitle("T-IDE - " + folder.getName());
            return;
        }

        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(txml);

            String mainClass = getXmlTag(doc, "mainClass");
            String appName   = getXmlTag(doc, "appName");
            String version   = getXmlTag(doc, "version");

            // Main-Class ins Textfeld schreiben
            if (mainClass != null && !mainClass.isEmpty()) {
                mainClassInput.setText(mainClass);
            }

            // Fenstertitel aktualisieren
            String titleAppName = (appName != null && !appName.isEmpty()) ? appName : folder.getName();
            String titleVersion = (version != null && !version.isEmpty()) ? version : "";
            if (!titleVersion.isEmpty()) {
                setTitle("T-IDE - " + titleAppName + " v" + titleVersion);
            } else {
                setTitle("T-IDE - " + titleAppName);
            }

            log("[INFO] T.xml geladen — Main-Class: " + mainClass
                    + " | App: " + titleAppName
                    + " | Version: " + titleVersion + "\n", Color.GREEN);

        } catch (Exception e) {
            log("[FEHLER] T.xml konnte nicht gelesen werden: " + e.getMessage() + "\n", Color.RED);
        }
    }

    /** Hilfsmethode: Liest den Textinhalt eines XML-Tags (erstes Vorkommen). */
    private String getXmlTag(Document doc, String tagName) {
        NodeList nl = doc.getElementsByTagName(tagName);
        if (nl.getLength() > 0) {
            String val = nl.item(0).getTextContent();
            return val != null ? val.trim() : null;
        }
        return null;
    }

    // ================== RUN ==================

    private void runProject() {
        if (currentProjectFolder == null) {
            log("[WARNUNG] Bitte öffne zuerst einen Projektordner.\n", Color.ORANGE);
            return;
        }

        String mainClass = mainClassInput.getText().trim();

        // ".java" am Ende entfernen falls aus Versehen eingetippt
        if (mainClass.endsWith(".java")) {
            mainClass = mainClass.substring(0, mainClass.length() - 5);
        }

        saveCurrentFile();

        String mode = (String) modeSelector.getSelectedItem();
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        switch (mode) {
            case MODE_JAVA:
                if (mainClass.isEmpty()) {
                    log("[FEHLER] Bitte gib die Main-Klasse ein.\n", Color.RED);
                    return;
                }

                // 1. Sicherstellen, dass der /out Ordner existiert
                File outFolder = new File(currentProjectFolder, "out");
                if (!outFolder.exists()) {
                    outFolder.mkdirs();
                }

                log("[JAVA] Kompiliere nach /out...\n", Color.CYAN);

                File sourceFile = findSourceFile(currentProjectFolder, mainClass);
                if (sourceFile == null) {
                    log("[FEHLER] Datei nicht gefunden.\n", Color.RED);
                    return;
                }

                String separator = System.getProperty("path.separator");

                // 2. Classpath anpassen:
                // Wir fügen "out" hinzu, damit Java dort nach deinen kompilierten Dateien sucht.
                // "libs/*" bleibt für deine Bibliotheken.
                String classpath = "\"out\"" + separator + "\"libs/*\"";

                // -d out sagt dem Compiler: Schreib die .class Files in den out-Ordner
                String compileCmd = "javac -encoding UTF-8 -cp " + classpath + " -d out \"" + sourceFile.getAbsolutePath() + "\"";

                // java -cp ... lädt die Klassen aus out und den libs
                String runCmd = "java -cp " + classpath + " " + mainClass;

                executeCommand(compileCmd + " && " + runCmd, false);
                break;

            case MODE_C:
            case MODE_CPP:
                File activeC = getActiveFile();
                if (activeC != null) {
                    String compiler = mode.equals(MODE_C) ? "gcc" : "g++";
                    String exeName = isWindows ? "program.exe" : "./program";
                    log("[" + mode.toUpperCase() + "] Kompiliere " + activeC.getName() + "...\n", Color.CYAN);
                    executeCommand(compiler + " \"" + activeC.getAbsolutePath() + "\" -o program && " + exeName, false);
                } else {
                    log("[FEHLER] Keine C/C++ Datei offen.\n", Color.RED);
                }
                break;

            case MODE_BATCH:
                File activeBat = getActiveFile();
                if (activeBat != null) {
                    log("[BATCH] Starte " + activeBat.getName() + "...\n", Color.CYAN);
                    executeCommand("\"" + activeBat.getAbsolutePath() + "\"", false);
                } else {
                    log("[FEHLER] Keine Batch-Datei (.bat) offen.\n", Color.RED);
                }
                break;
        }

        // Fokus zurück in den Editor
        SwingUtilities.invokeLater(() -> {
            Component tab = editorTabs.getSelectedComponent();
            if (tab instanceof RTextScrollPane) {
                ((RTextScrollPane) tab).getTextArea().requestFocusInWindow();
            }
        });
    }

    /**
     * Sucht die Java-Datei passend zur angegebenen Klasse (mit oder ohne Packages)
     */
    private File findSourceFile(File dir, String fqcn) {
        String relativePath = fqcn.replace(".", File.separator) + ".java";

        // 1. Suche direkt im Root-Ordner
        File direct = new File(dir, relativePath);
        if (direct.exists()) return direct;

        // 2. Suche in einem "src" Ordner (Standard in vielen IDEs)
        File src = new File(dir, "src");
        if (src.exists() && src.isDirectory()) {
            File inSrc = new File(src, relativePath);
            if (inSrc.exists()) return inSrc;
        }

        // 3. Fallback: Rekursiv im Ordner suchen, falls die Klasse ohne Packages aufgerufen wird
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
            } else if (f.getName().equals(fileName)) {
                return f;
            }
        }
        return null;
    }

    private void handleTBuild() {
        if (currentProjectFolder == null) {
            log("[FEHLER] Bitte öffne zuerst einen Projektordner für TBuild.\n", Color.RED);
            return;
        }

        File tbuildJar = new File(currentProjectFolder, "TBuild.jar");
        if (tbuildJar.exists()) {
            executeCommand("java -jar TBuild.jar", true);
        } else {
            log("[INFO] TBuild.jar nicht gefunden. Lade neueste Version herunter...\n", Color.YELLOW);
            new Thread(() -> {
                try {
                    URL url = new URL("https://github.com/Thillager/Tbuild/releases/latest/download/TBuild.jar");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setInstanceFollowRedirects(true);

                    try (InputStream in = connection.getInputStream()) {
                        Files.copy(in, tbuildJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    log("[ERFOLG] TBuild erfolgreich heruntergeladen!\n", Color.GREEN);
                    executeCommand("java -jar TBuild.jar", true);

                    SwingUtilities.invokeLater(() -> updateFileTree(currentProjectFolder));

                } catch (Exception ex) {
                    log("[FEHLER] Download fehlgeschlagen: " + ex.getMessage() + "\n", Color.RED);
                }
            }).start();
        }
    }

    private void executeCommand(String command, boolean isTBuild) {
        log("> " + command + "\n", Color.GRAY);
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

                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
                String line;
                while ((line = r.readLine()) != null) {
                    log(line + "\n", isTBuild ? Color.CYAN : Color.WHITE);
                }

                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    log("[PROZESS BEENDET MIT CODE " + exitCode + "]\n", Color.RED);
                }
            } catch (Exception e) {
                log("[TERMINAL FEHLER] " + e.getMessage() + "\n", Color.RED);
            }
        }).start();
    }

    private void openFolderDialog() {
        String userHome = System.getProperty("user.home");
        JFileChooser chooser = new JFileChooser(userHome);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentProjectFolder = chooser.getSelectedFile();
            updateFileTree(currentProjectFolder);
            log("[INFO] Ordner geöffnet: " + currentProjectFolder.getName() + "\n", Color.CYAN);

            // T.xml auslesen und UI befüllen
            loadTXml(currentProjectFolder);
        }
    }

    private void updateFileTree(File rootFolder) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new FileNode(rootFolder));
        buildTree(rootFolder, rootNode);
        treeModel.setRoot(rootNode);
    }

    private void buildTree(File folder, DefaultMutableTreeNode node) {
        File[] files = folder.listFiles();
        if (files == null) return;
        Arrays.sort(files, (a, b) -> a.isDirectory() == b.isDirectory() ? a.getName().compareToIgnoreCase(b.getName()) : a.isDirectory() ? -1 : 1);
        for (File file : files) {
            if (file.getName().startsWith(".") || file.getName().equals("out")) continue;
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
            node.add(childNode);
            if (file.isDirectory()) buildTree(file, childNode);
        }
    }

    private void openFileInEditor(File file) {
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
            if (fileName.endsWith(".java")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            else if (fileName.endsWith(".py")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
            else if (fileName.endsWith(".c") || fileName.endsWith(".h")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
            else if (fileName.endsWith(".cpp") || fileName.endsWith(".hpp")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
            else if (fileName.endsWith(".xml")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            else if (fileName.endsWith(".bat") || fileName.endsWith(".cmd")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
            else textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);

            textArea.setCodeFoldingEnabled(true);
            textArea.setFont(new Font("Consolas", Font.PLAIN, 15));
            textArea.setBackground(new Color(30, 31, 34));
            textArea.setForeground(Color.WHITE);
            textArea.setCaretColor(Color.WHITE);
            textArea.setCurrentLineHighlightColor(new Color(45, 45, 45));

            RTextScrollPane sp = new RTextScrollPane(textArea);
            sp.setBorder(null);

            JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            tabHeader.setOpaque(false);
            tabHeader.add(new JLabel(file.getName()));
            JButton closeBtn = new JButton("×");
            closeBtn.setBorder(null);
            closeBtn.setContentAreaFilled(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.addActionListener(e -> {
                openFiles.remove(sp);
                editorTabs.remove(sp);
            });
            tabHeader.add(closeBtn);

            editorTabs.addTab(file.getName(), sp);
            editorTabs.setTabComponentAt(editorTabs.getTabCount() - 1, tabHeader);
            editorTabs.setSelectedComponent(sp);
            openFiles.put(sp, file);

            textArea.requestFocusInWindow();

        } catch (Exception e) { log("Öffnen fehlgeschlagen\n", Color.RED); }
    }

    private void saveCurrentFile() {
        Component tab = editorTabs.getSelectedComponent();
        if (tab instanceof RTextScrollPane) {
            File file = openFiles.get(tab);
            RSyntaxTextArea ta = (RSyntaxTextArea) ((RTextScrollPane) tab).getTextArea();
            try {
                Files.writeString(file.toPath(), ta.getText());
                log("[SAVE] " + file.getName() + " gespeichert.\n", Color.GREEN);
            } catch (IOException e) { log("Fehler beim Speichern\n", Color.RED); }
        }
    }

    private File getActiveFile() {
        Component tab = editorTabs.getSelectedComponent();
        if (tab != null) return openFiles.get(tab);
        return null;
    }

    private void log(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = consolePane.getStyledDocument();
            Style style = consolePane.addStyle("style", null);
            StyleConstants.setForeground(style, color);
            try {
                doc.insertString(doc.getLength(), msg, style);
                consolePane.setCaretPosition(doc.getLength());
            } catch (Exception ignored) {}
        });
    }

    private static class FileNode {
        private File file;
        public FileNode(File file) { this.file = file; }
        public File getFile() { return file; }
        @Override public String toString() { return file.getName(); }
    }
}