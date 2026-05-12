import com.formdev.flatlaf.FlatDarkLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.parser.*;
import org.fife.ui.autocomplete.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
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
// JGit
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.revwalk.RevCommit;

public class TIDE extends JFrame {

    // Aktuelle Version der App - bei jedem Release erhoehen
    private static final String APP_VERSION = "2.2.0";
    private static final String GITHUB_REPO = "Thillager/TIDE";

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

    private static final String MODE_JAVA  = "Java";
    private static final String MODE_PYTHON = "Python";
    private static final String MODE_C     = "C";
    private static final String MODE_CPP   = "C++";
    private static final String MODE_BATCH = "Batch";

    private File clipboard = null;

    private JPanel searchPanel;
    private JTextField searchField;
    private JCheckBox matchCaseCB;

    

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
        setTitle("TIDE v" + APP_VERSION + " - Leichte IDE");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }



    

    private void initUI() {
        // --- Search Panel initialisieren (MUSS vor editorContainer!) ---
        initSearchPanel();
        editorTabs = new JTabbedPane();
        editorTabs.setBorder(null);

        // --- Toolbar ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(new EmptyBorder(8, 10, 8, 10));
        toolBar.setBackground(new Color(43, 45, 48));

        JButton btnOpen  = new JButton("Ordner öffnen");
        JButton btnSave  = new JButton("Speichern");

        modeSelector = new JComboBox<>(new String[]{MODE_JAVA, MODE_PYTHON, MODE_C, MODE_CPP, MODE_BATCH});
        modeSelector.setMaximumSize(new Dimension(100, 30));

        mainClassLabel = new JLabel(" Main-Class: ");
        mainClassInput = new JTextField("Main", 15);
        mainClassInput.setMaximumSize(new Dimension(200, 30));

        JButton btnRun   = new JButton("▶");
        btnTBuild        = new JButton("T-Build");
        JButton btnClear = new JButton("Konsole leeren");
        JButton btnAbout = new JButton("Über");

        btnRun.setForeground(new Color(80, 200, 120));
        btnRun.setFont(btnRun.getFont().deriveFont(Font.BOLD));
        btnTBuild.setForeground(new Color(100, 150, 255));
        btnAbout.setForeground(new Color(180, 180, 180));

        // ---- Git-Dropdown ----
        JMenuBar gitMenuBar = new JMenuBar();
        gitMenuBar.setOpaque(false);
        gitMenuBar.setBorder(null);
        JMenu gitMenu = new JMenu("Git ▾");
        gitMenu.setForeground(new Color(255, 200, 80));
        gitMenu.setFont(gitMenu.getFont().deriveFont(Font.BOLD));

        JMenuItem gitCommit = new JMenuItem("Commit");
        JMenuItem gitPush   = new JMenuItem("Push");
        JMenuItem gitPull   = new JMenuItem("Pull");

        gitMenu.add(gitCommit);
        gitMenu.add(gitPush);
        gitMenu.add(gitPull);

        gitCommit.addActionListener(e -> gitCommit());
        gitPush.addActionListener(e   -> gitPush());
        gitPull.addActionListener(e   -> gitPull());

        gitMenuBar.add(gitMenu);

        // --- Editor Container ---
        JPanel editorContainer = new JPanel(new BorderLayout());
        editorContainer.add(searchPanel, BorderLayout.NORTH);
        editorContainer.add(editorTabs,  BorderLayout.CENTER);

        // Toolbar befuellen
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
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(gitMenuBar);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(btnClear);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnAbout);

        add(toolBar, BorderLayout.NORTH);

        // --- Dateibaum (Links) ---
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Kein Projekt geöffnet");
        treeModel = new DefaultTreeModel(root);
        fileTree  = new JTree(treeModel);
        fileTree.setBackground(new Color(30, 31, 34));
        fileTree.setBorder(new EmptyBorder(10, 10, 10, 10));

        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
                    if (node != null && node.getUserObject() instanceof FileNode) {
                        File selectedFile = ((FileNode) node.getUserObject()).getFile();
                        if (selectedFile.isFile()) openFileInEditor(selectedFile);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent me) { handleTreePopup(me); }

            @Override
            public void mouseReleased(MouseEvent me) { handleTreePopup(me); }

            private void handleTreePopup(MouseEvent me) {
                if (me.isPopupTrigger()) showFileTreePopup(me);
            }
        });

        JScrollPane treeScroll = new JScrollPane(fileTree);
        treeScroll.setPreferredSize(new Dimension(250, 0));
        treeScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));

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
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        bottomPanel.add(consoleScroll,  BorderLayout.CENTER);
        bottomPanel.add(terminalInput, BorderLayout.SOUTH);
        bottomPanel.setPreferredSize(new Dimension(0, 250));

        // --- Layout Splitting ---
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorContainer, bottomPanel);
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
        btnAbout.addActionListener(e -> showAboutDialog());

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
            if (e.getID() == KeyEvent.KEY_PRESSED && e.isControlDown()) {
                if (e.getKeyCode() == KeyEvent.VK_S) { saveCurrentFile(); return true; }
                if (e.getKeyCode() == KeyEvent.VK_F) {
                    searchPanel.setVisible(!searchPanel.isVisible());
                    if (searchPanel.isVisible()) searchField.requestFocusInWindow();
                    revalidate();
                    return true;
                }
            }
            return false;
        });

        updateDynamicUI();
    }







    // ================== ÜBER-DIALOG & UPDATE-SYSTEM ==================

    private void showAboutDialog() {
        JDialog dialog = new JDialog(this, "Über TIDE", true);
        dialog.setSize(420, 280);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // Info-Panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new EmptyBorder(20, 30, 10, 30));
        infoPanel.setBackground(new Color(43, 45, 48));

        JLabel titleLabel = new JLabel("TIDE - Leichte Java IDE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel versionLabel = new JLabel("Version " + APP_VERSION);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        versionLabel.setForeground(new Color(180, 180, 180));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel repoLabel = new JLabel("github.com/" + GITHUB_REPO);
        repoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        repoLabel.setForeground(new Color(100, 150, 255));
        repoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("<html><center>Einfache, leichte IDE fuer Anfaenger.<br></center></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(new Color(160, 160, 160));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(versionLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(repoLabel);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(descLabel);

        // Button-Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPanel.setBackground(new Color(43, 45, 48));

        JButton btnUpdate = new JButton("Nach Updates suchen");
        JButton btnClose  = new JButton("Schließen");

        btnUpdate.setForeground(new Color(80, 200, 120));
        btnUpdate.addActionListener(e -> {
            dialog.dispose();
            checkForUpdates();
        });
        btnClose.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnUpdate);
        btnPanel.add(btnClose);

        dialog.add(infoPanel,  BorderLayout.CENTER);
        dialog.add(btnPanel,   BorderLayout.SOUTH);
        dialog.getContentPane().setBackground(new Color(43, 45, 48));
        dialog.setVisible(true);
    }







    private void markCompilerErrors(String output) {
    // Suche nach "Dateiname.
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
    "(.+\\.java):(\\d+): (?:error|Fehler): (.+)");
    java.util.regex.Matcher matcher = pattern.matcher(output);

    while (matcher.find()) {
        String fileName = new File(matcher.group(1)).getName();
 // Dateinamen nehmen
        int    lineNum  = Integer.parseInt(matcher.group(2)) - 1; // Zeile nehmen
        String message  = matcher.group(3);
 // Nachricht nehmen

        // Tab suchen der zur Datei gehoert
        for (int i = 0; i < editorTabs.getTabCount(); i++) {
            Component tab = editorTabs.getComponentAt(i);
            File tabFile  = openFiles.get(tab);
            if (tabFile != null && tabFile.getName().equals(fileName)
                    && tab instanceof RTextScrollPane) {
                RSyntaxTextArea ta = (RSyntaxTextArea)
                    ((RTextScrollPane) tab).getTextArea();
                SwingUtilities.invokeLater(() -> {
                    try {
                        int start = ta.getLineStartOffset(lineNum);
                        int end   = ta.getLineEndOffset(lineNum);
                        // Roten Highlight setzen
                        ta.addLineHighlight(lineNum,
                            new Color(180, 30, 30, 60));
                    } catch (Exception ignored) {}
                });
                log("[FEHLER Zeile " + (lineNum + 1) + "] " + message + "\n",
                    Color.RED);
            }
        }
    }
}

private void clearCompilerErrors() {
    for (int i = 0; i < editorTabs.getTabCount(); i++) {
        Component tab = editorTabs.getComponentAt(i);
        if (tab instanceof RTextScrollPane) {
            RSyntaxTextArea ta = (RSyntaxTextArea) ((RTextScrollPane) tab).getTextArea();
            ta.removeAllLineHighlights();
        }
    }
}





//==================== UPDATES===========================




    /**
     * Prueft auf GitHub ob eine neuere Version verfuegbar ist.
     * Vergleicht APP_VERSION mit dem neuesten Release-Tag.
     */
    private void checkForUpdates() {
        log("[INFO] Suche nach Updates...\n", Color.CYAN);
        new Thread(() -> {
            try {
                // GitHub API: neueste Release-Infos holen
                String apiUrl = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "TIDE-App");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                if (conn.getResponseCode() != 200) {
                    log("[FEHLER] Konnte GitHub nicht erreichen (HTTP " + conn.getResponseCode() + ").\n", Color.RED);
                    return;
                }

                // JSON manuell parsen (kein JSON-Parser als Dependency noetig)
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                String json = sb.toString();

                // tag_name extrahieren (z.B. "v1.2.0")
                String latestVersion = extractJsonValue(json, "tag_name");
                if (latestVersion == null) {
                    log("[FEHLER] Konnte Version nicht aus GitHub-Antwort lesen.\n", Color.RED);
                    return;
                }
                // "v" am Anfang entfernen falls vorhanden
                latestVersion = latestVersion.replaceAll("^v", "");

                String finalLatestVersion = latestVersion;
                SwingUtilities.invokeLater(() -> {
                    if (isNewerVersion(finalLatestVersion, APP_VERSION)) {
                        int result = JOptionPane.showConfirmDialog(
                                TIDE.this,
                                "Neue Version verfuegbar: " + finalLatestVersion + "\n" +
                                        "Aktuelle Version: " + APP_VERSION + "\n\n" +
                                        "Jetzt updaten?",
                                "Update verfuegbar",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.INFORMATION_MESSAGE);
                        if (result == JOptionPane.YES_OPTION) {
                            downloadAndInstallUpdate(finalLatestVersion, json);
                        }
                    } else {
                        JOptionPane.showMessageDialog(
                                TIDE.this,
                                "Du hast bereits die aktuellste Version (" + APP_VERSION + ").",
                                "Kein Update noetig",
                                JOptionPane.INFORMATION_MESSAGE);
                        log("[INFO] Keine Updates verfuegbar. Version " + APP_VERSION + " ist aktuell.\n", Color.GREEN);
                    }
                });

            } catch (Exception e) {
                log("[FEHLER] Update-Pruefung fehlgeschlagen: " + e.getMessage() + "\n", Color.RED);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(TIDE.this,
                                "Update-Pruefung fehlgeschlagen:\n" + e.getMessage(),
                                "Fehler", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }


    // ================== NEU: JAR-Erkennung ==================

    /**
     * Erkennt ob die App als .jar laeuft (nicht als jpackage-Programm).
     */
    private boolean isRunningAsJar() {
        try {
            java.net.URI uri = TIDE.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            return uri.getPath().endsWith(".jar");
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Gibt den Pfad zur laufenden JAR-Datei zurueck, oder null wenn keine JAR.
     */
    private File getRunningJarFile() {
        try {
            java.net.URI uri = TIDE.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            File f = new File(uri);
            if (f.getName().endsWith(".jar")) return f;
        } catch (Exception ignored) {}
        return null;
    }


    /**
     * Laedt den Installer herunter und startet ihn ueber ein externes Skript.
     * Das Skript wartet bis die App beendet ist, installiert dann und startet neu.
     * Erkennt automatisch ob die App als .jar laeuft und ersetzt sich in diesem Fall selbst.
     */
    private void downloadAndInstallUpdate(String version, String releaseJson) {
        new Thread(() -> {
            try {
                String os = System.getProperty("os.name", "").toLowerCase();
                boolean isWindows = os.contains("win");
                boolean isLinux   = !isWindows && !os.contains("mac");
                boolean runAsJar  = isRunningAsJar();

                // --- JAR-Selbstaustausch (plattformuebergreifend) ---
                if (runAsJar) {
                    String jarAssetName = findAssetName(releaseJson, ".jar");
                    if (jarAssetName == null) {
                        log("[FEHLER] Kein .jar-Asset im Release gefunden.\n", Color.RED);
                        return;
                    }
                    File currentJar = getRunningJarFile();
                    if (currentJar == null) {
                        log("[FEHLER] Konnte den Pfad der laufenden JAR nicht bestimmen.\n", Color.RED);
                        return;
                    }
                    String downloadUrl = "https://github.com/" + GITHUB_REPO
                            + "/releases/download/v" + version + "/" + jarAssetName;
                    log("[INFO] Lade neue JAR herunter: " + jarAssetName + "...\n", Color.CYAN);
                    SwingUtilities.invokeLater(() ->
                            log("[INFO] Download laeuft, bitte warten...\n", Color.YELLOW));

                    File tempJar = new File(System.getProperty("java.io.tmpdir"), jarAssetName);
                    HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(60000);
                    if (conn.getResponseCode() != 200) {
                        log("[FEHLER] Download fehlgeschlagen (HTTP " + conn.getResponseCode() + ").\n", Color.RED);
                        return;
                    }
                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    log("[ERFOLG] Neue JAR heruntergeladen: " + tempJar.getAbsolutePath() + "\n", Color.GREEN);
                    launchJarSelfReplace(tempJar, currentJar);
                    return;
                }

                // --- Nativer Installer (jpackage) ---
                String installerName;
                if (isWindows) {
                    installerName = findAssetName(releaseJson, ".msi");
                } else if (isLinux) {
                    installerName = findAssetName(releaseJson, ".deb");
                } else {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(TIDE.this,
                                    "Auto-Update wird auf macOS noch nicht unterstuetzt.\n" +
                                            "Bitte manuell von github.com/" + GITHUB_REPO + " herunterladen.",
                                    "Nicht unterstuetzt", JOptionPane.WARNING_MESSAGE));
                    return;
                }

                if (installerName == null) {
                    log("[FEHLER] Kein passender Installer fuer dieses OS gefunden.\n", Color.RED);
                    return;
                }

                // Download-URL bauen
                String downloadUrl = "https://github.com/" + GITHUB_REPO + "/releases/download/v" + version + "/" + installerName;
                File tempDir  = new File(System.getProperty("java.io.tmpdir"));
                File installer = new File(tempDir, installerName);

                log("[INFO] Lade Installer herunter: " + installerName + "...\n", Color.CYAN);
                SwingUtilities.invokeLater(() ->
                        log("[INFO] Download laeuft, bitte warten...\n", Color.YELLOW));

                // Installer herunterladen
                HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);

                if (conn.getResponseCode() != 200) {
                    log("[FEHLER] Download fehlgeschlagen (HTTP " + conn.getResponseCode() + ").\n", Color.RED);
                    return;
                }

                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, installer.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                log("[ERFOLG] Installer heruntergeladen: " + installer.getAbsolutePath() + "\n", Color.GREEN);

                // Update-Skript erstellen und starten
                if (isWindows) {
                    launchWindowsUpdate(installer);
                } else {
                    launchLinuxUpdate(installer);
                }

            } catch (Exception e) {
                log("[FEHLER] Update fehlgeschlagen: " + e.getMessage() + "\n", Color.RED);
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(TIDE.this,
                                "Update fehlgeschlagen:\n" + e.getMessage(),
                                "Fehler", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }


    /**
     * Ersetzt die laufende JAR durch die neue und startet neu.
     * Funktioniert auf Linux und Windows.
     */
    private void launchJarSelfReplace(File newJar, File currentJar) throws IOException {
        String os         = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("win");

        // Java-Executable ermitteln
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator
                + (isWindows ? "java.exe" : "java");

        if (isWindows) {
            // .bat: warten bis TIDE beendet, dann ersetzen & neu starten
            File bat = new File(System.getProperty("java.io.tmpdir"), "tide_jar_update.bat");
            try (PrintWriter pw = new PrintWriter(bat)) {
                pw.println("@echo off");
                pw.println("echo Warte auf TIDE-Beendigung...");
                pw.println("timeout /t 3 /nobreak > nul");
                pw.println("echo Ersetze JAR...");
                pw.println("copy /Y \"" + newJar.getAbsolutePath() + "\" \"" + currentJar.getAbsolutePath() + "\"");
                pw.println("if %errorlevel% neq 0 (");
                pw.println("  echo JAR-Austausch fehlgeschlagen!");
                pw.println("  pause");
                pw.println("  exit /b 1");
                pw.println(")");
                pw.println("echo Starte neue Version...");
                pw.println("start \"\" \"" + java + "\" -jar \"" + currentJar.getAbsolutePath() + "\"");
                pw.println("del \"%~f0\"");
            }
            log("[INFO] Starte JAR-Update (Windows)...\n", Color.CYAN);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(TIDE.this,
                        "Die JAR wird jetzt aktualisiert.\nTIDE startet automatisch neu.",
                        "JAR-Update", JOptionPane.INFORMATION_MESSAGE);
                try {
                    new ProcessBuilder("cmd.exe", "/c", bat.getAbsolutePath()).start();
                } catch (IOException e) {
                    log("[FEHLER] Konnte JAR-Update-Skript nicht starten: " + e.getMessage() + "\n", Color.RED);
                    return;
                }
                System.exit(0);
            });
        } else {
            // .sh: warten bis TIDE beendet, dann ersetzen & neu starten
            File sh = new File(System.getProperty("java.io.tmpdir"), "tide_jar_update.sh");
            try (PrintWriter pw = new PrintWriter(sh)) {
                pw.println("#!/bin/bash");
                pw.println("sleep 2");
                pw.println("cp -f \"" + newJar.getAbsolutePath() + "\" \"" + currentJar.getAbsolutePath() + "\"");
                pw.println("chmod +x \"" + currentJar.getAbsolutePath() + "\"");
                pw.println("\"" + java + "\" -jar \"" + currentJar.getAbsolutePath() + "\" &");
                pw.println("rm -- \"$0\"");
            }
            sh.setExecutable(true);
            log("[INFO] Starte JAR-Update (Linux)...\n", Color.CYAN);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(TIDE.this,
                        "Die JAR wird jetzt aktualisiert.\nTIDE startet automatisch neu.",
                        "JAR-Update", JOptionPane.INFORMATION_MESSAGE);
                try {
                    new ProcessBuilder("bash", sh.getAbsolutePath()).start();
                } catch (IOException e) {
                    log("[FEHLER] Konnte JAR-Update-Skript nicht starten: " + e.getMessage() + "\n", Color.RED);
                    return;
                }
                System.exit(0);
            });
        }
    }


    /**
     * Erstellt eine .bat-Datei die wartet bis TIDE beendet ist,
     * dann den MSI-Installer ausfuehrt und TIDE neu startet.
     */
    private void launchWindowsUpdate(File msiFile) throws IOException {
    File batFile = new File(System.getProperty("java.io.tmpdir"), "tide_update.bat");

    try (PrintWriter pw = new PrintWriter(batFile)) {
        pw.println("@echo off");
        pw.println("echo Warte auf TIDE-Beendigung...");
        pw.println("timeout /t 5 /nobreak > nul");
        pw.println("echo Installiere Update...");
        pw.println("msiexec /i \"" + msiFile.getAbsolutePath() + "\" /qn /norestart");
        pw.println("if %errorlevel% neq 0 (");
        pw.println("  echo Installation fehlgeschlagen!");
        pw.println("  pause");
        pw.println("  exit /b 1");
        pw.println(")");
        pw.println("echo Update erfolgreich installiert.");
        pw.println("del \"%~f0\"");
    }

    log("[INFO] Starte Windows-Update-Skript...\n", Color.CYAN);
    SwingUtilities.invokeLater(() -> {
        JOptionPane.showMessageDialog(TIDE.this,
                "Das Update wird jetzt installiert.\n" +
                "TIDE wird sich gleich beenden.",
                "Update wird installiert",
                JOptionPane.INFORMATION_MESSAGE);
        try {
            new ProcessBuilder("cmd.exe", "/c", batFile.getAbsolutePath()).start();
        } catch (IOException e) {
            log("[FEHLER] Konnte Update-Skript nicht starten: " + e.getMessage() + "\n", Color.RED);
            return;
        }
        System.exit(0);
    });
}

    /**
     * Erstellt ein .sh-Skript fuer das Linux-.deb-Update.
     * Nutzt sudo direkt wenn bereits Root-Rechte vorhanden sind (z.B. "sudo tide"),
     * faellt sonst auf pkexec (grafischer Passwort-Dialog) zurueck.
     */
    private void launchLinuxUpdate(File debFile) throws IOException {
        // Pruefen ob sudo ohne Passwort verfuegbar ist (z.B. weil bereits als sudo gestartet)
        boolean hasSudoNopass = false;
        try {
            Process p = new ProcessBuilder("sudo", "-n", "true")
                    .redirectErrorStream(true).start();
            hasSudoNopass = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
                    && p.exitValue() == 0;
        } catch (Exception ignored) {}

        File shFile = new File(System.getProperty("java.io.tmpdir"), "tide_update.sh");

        try (PrintWriter pw = new PrintWriter(shFile)) {
            pw.println("#!/bin/bash");
            pw.println("sleep 2");
            if (hasSudoNopass) {
                // Bereits mit sudo-Rechten gestartet – kein Passwort-Dialog noetig
                pw.println("sudo dpkg -i \"" + debFile.getAbsolutePath() + "\"");
            } else {
                // pkexec zeigt grafischen Passwort-Dialog fuer Root-Rechte (Fallback)
                pw.println("if command -v pkexec &>/dev/null; then");
                pw.println("    pkexec dpkg -i \"" + debFile.getAbsolutePath() + "\"");
                pw.println("else");
                pw.println("    # Letzter Fallback: Terminal mit sudo");
                pw.println("    x-terminal-emulator -e bash -c \"sudo dpkg -i '" + debFile.getAbsolutePath() + "' && echo Fertig; read\"");
                pw.println("fi");
            }
            // App neu starten falls im PATH vorhanden
            pw.println("which tide && tide &");
            pw.println("rm -- \"$0\""); // Skript selbst loeschen
        }
        // Skript ausfuehrbar machen
        shFile.setExecutable(true);

        String sudoHint = hasSudoNopass
                ? "Kein Passwort nötig (sudo-Rechte erkannt)."
                : "Du wirst nach deinem Passwort gefragt.";

        log("[INFO] Starte Linux-Update-Skript"
                + (hasSudoNopass ? " (sudo, kein Passwort noetig)" : " (pkexec)") + "...\n", Color.CYAN);

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(TIDE.this,
                    "Das Update wird jetzt installiert.\n" +
                            sudoHint + "\n" +
                            "TIDE wird sich gleich beenden.",
                    "Update wird installiert",
                    JOptionPane.INFORMATION_MESSAGE);
            try {
                new ProcessBuilder("bash", shFile.getAbsolutePath()).start();
            } catch (IOException e) {
                log("[FEHLER] Konnte Update-Skript nicht starten.\n", Color.RED);
                return;
            }
            System.exit(0);
        });
    }

    /**
     * Einfacher JSON-Wert-Extraktor fuer "key":"value" Paare.
     * Kein vollstaendiger JSON-Parser - reicht fuer GitHub API.
     */
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    /**
     * Sucht in den Release-Assets nach einer Datei mit der gegebenen Endung.
     */
    private String findAssetName(String releaseJson, String extension) {
        // Suche nach "name":"...extension" im JSON
        int searchFrom = 0;
        while (true) {
            int idx = releaseJson.indexOf("\"name\":\"", searchFrom);
            if (idx == -1) return null;
            int start = idx + 8;
            int end = releaseJson.indexOf("\"", start);
            if (end == -1) return null;
            String name = releaseJson.substring(start, end);
            if (name.endsWith(extension)) return name;
            searchFrom = end;
        }
    }

    /**
     * Vergleicht zwei Versions-Strings im Format "x.y.z".
     * Gibt true zurueck wenn newVersion neuer als currentVersion ist.
     */
    private boolean isNewerVersion(String newVersion, String currentVersion) {
        try {
            String[] newParts     = newVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");
            int length = Math.max(newParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                int newPart     = i < newParts.length     ? Integer.parseInt(newParts[i].trim())     : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i].trim()) : 0;
                if (newPart > currentPart) return true;
                if (newPart < currentPart) return false;
            }
            return false; // gleiche Version
        } catch (NumberFormatException e) {
            return !newVersion.equals(currentVersion);
        }
    }







    

    // ================== FILE TREE POPUP & HELFERMETHODEN ==================

    private void showFileTreePopup(MouseEvent me) {
        JPopupMenu popup = new JPopupMenu();
        TreePath path = fileTree.getPathForLocation(me.getX(), me.getY());

        JMenuItem neuDatei    = new JMenuItem("Neue Datei");
        JMenuItem neuOrdner   = new JMenuItem("Neuer Ordner");
        JMenuItem umbenennen  = new JMenuItem("Umbenennen");
        JMenuItem delete      = new JMenuItem("Löschen");
        JMenuItem explorer    = new JMenuItem("In Explorer öffnen");
        JMenuItem copy        = new JMenuItem("Kopieren");
        JMenuItem cut         = new JMenuItem("Ausschneiden");
        JMenuItem paste       = new JMenuItem("Einfügen");
        JMenuItem aktualisieren = new JMenuItem("Aktualisieren");
        aktualisieren.addActionListener(e -> updateFileTree(currentProjectFolder));

        if (path != null) {
            fileTree.setSelectionPath(path);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            File clickedFile = null;
            if (node.getUserObject() instanceof FileNode) {
                clickedFile = ((FileNode) node.getUserObject()).getFile();
            }
            final File finalClickedFile = clickedFile;

            neuDatei.addActionListener(e   -> createNewFile(finalClickedFile));
            neuOrdner.addActionListener(e  -> createNewFolder(finalClickedFile));
            copy.addActionListener(e       -> copyFile(finalClickedFile));
            cut.addActionListener(e        -> cutFile(finalClickedFile));
            paste.addActionListener(e      -> pasteFile(finalClickedFile));
            delete.addActionListener(e     -> deleteFile(finalClickedFile));
            umbenennen.addActionListener(e -> renameFile(finalClickedFile));
            explorer.addActionListener(e   -> openExplorer(finalClickedFile));

            popup.add(neuDatei);
            popup.add(neuOrdner);
            popup.addSeparator();
            popup.add(umbenennen);
            popup.add(delete);
            popup.addSeparator();
            popup.add(explorer);
            popup.addSeparator();
            popup.add(copy);
            popup.add(cut);
            popup.add(paste);
            popup.addSeparator();
            popup.add(aktualisieren);
        } else {
            neuDatei.addActionListener(e  -> createNewFileInRoot());
            neuOrdner.addActionListener(e -> createNewFolderInRoot());
            popup.add(neuDatei);
            popup.add(neuOrdner);
            popup.addSeparator();
            popup.add(aktualisieren);
        }

        popup.show(fileTree, me.getX(), me.getY());
    }





    private void createNewFile(File target) {
        if (target == null || currentProjectFolder == null) return;
        File zielOrdner = target.isDirectory() ? target : target.getParentFile();
        String name = (String) JOptionPane.showInputDialog(
                TIDE.this, "Wie soll die neue Datei heißen?",
                "Neue Datei", JOptionPane.PLAIN_MESSAGE, null, null, "");
        if (name != null && !name.trim().isEmpty()) {
            try {
                new File(zielOrdner, name.trim()).createNewFile();
                updateFileTree(currentProjectFolder);
            } catch (IOException ex) {
                log("[FEHLER] Datei konnte nicht erstellt werden.\n", Color.RED);
            }
        }
    }



    

    private void createNewFolder(File target) {
        if (target == null || currentProjectFolder == null) return;
        File zielOrdner = target.isDirectory() ? target : target.getParentFile();
        String name = (String) JOptionPane.showInputDialog(
                TIDE.this, "Wie soll der neue Ordner heißen?",
                "Neuer Ordner", JOptionPane.PLAIN_MESSAGE, null, null, "");
        if (name != null && !name.trim().isEmpty()) {
            new File(zielOrdner, name.trim()).mkdirs();
            updateFileTree(currentProjectFolder);
        }
    }




    

    private void copyFile(File file) {
        if (file != null) {
            clipboard = file;
            log("[INFO] Kopiert: " + file.getName() + "\n", Color.LIGHT_GRAY);
        }
    }





    

    private void cutFile(File file) {
        if (file != null) {
            clipboard = file;
            log("[INFO] Ausgeschnitten: " + file.getName() + "\n", Color.LIGHT_GRAY);
        }
    }




    

    private void pasteFile(File target) {
        if (clipboard == null || currentProjectFolder == null) return;
        File zielOrdner = target.isDirectory() ? target : target.getParentFile();
        try {
            Files.copy(clipboard.toPath(),
                    new File(zielOrdner, clipboard.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            updateFileTree(currentProjectFolder);
            log("[INFO] Eingefügt: " + clipboard.getName() + "\n", Color.GREEN);
        } catch (IOException ex) {
            log("[FEHLER] Einfügen fehlgeschlagen.\n", Color.RED);
        }
    }




    

    private void deleteFile(File file) {
        if (file == null) return;
        int confirm = JOptionPane.showConfirmDialog(
                TIDE.this, "'" + file.getName() + "' wirklich löschen?",
                "Löschen bestätigen", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Files.walk(file.toPath())
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(java.nio.file.Path::toFile)
                        .forEach(File::delete);
                updateFileTree(currentProjectFolder);
                log("[INFO] Gelöscht: " + file.getName() + "\n", Color.LIGHT_GRAY);
            } catch (IOException ex) {
                log("[FEHLER] Löschen fehlgeschlagen.\n", Color.RED);
            }
        }
    }




    

    private void renameFile(File file) {
        if (file == null) return;
        String name = (String) JOptionPane.showInputDialog(
                TIDE.this, "Neuer Name:", "Umbenennen",
                JOptionPane.PLAIN_MESSAGE, null, null, file.getName());
        if (name != null && !name.trim().isEmpty()) {
            file.renameTo(new File(file.getParentFile(), name.trim()));
            updateFileTree(currentProjectFolder);
        }
    }





    

    private void openExplorer(File file) {
        try {
            File ordner = file.isDirectory() ? file : file.getParentFile();
            Desktop.getDesktop().open(ordner);
        } catch (IOException ex) {
            log("[FEHLER] Explorer konnte nicht geöffnet werden.\n", Color.RED);
        }
    }



    

    private void createNewFileInRoot()   { createNewFile(currentProjectFolder);   }
    private void createNewFolderInRoot() { createNewFolder(currentProjectFolder); }

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

    private void loadTXml(File folder) {
        File txml = new File(folder, "T.xml");
        if (!txml.exists()) {
            log("[WARNUNG] Keine T.xml im Projektordner gefunden.\n", Color.ORANGE);
            setTitle("TIDE v" + APP_VERSION + " - " + folder.getName());
            return;
        }
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(txml);
            String mainClass = getXmlTag(doc, "mainClass");
            String appName   = getXmlTag(doc, "appName");
            String version   = getXmlTag(doc, "version");
            if (mainClass != null && !mainClass.isEmpty()) mainClassInput.setText(mainClass);
            String titleAppName = (appName != null && !appName.isEmpty()) ? appName : folder.getName();
            String titleVersion = (version != null && !version.isEmpty()) ? version : "";
            if (!titleVersion.isEmpty()) {
                setTitle("TIDE v" + APP_VERSION + " - " + titleAppName + " v" + titleVersion);
            } else {
                setTitle("TIDE v" + APP_VERSION + " - " + titleAppName);
            }
            log("[INFO] T.xml geladen — Main-Class: " + mainClass + " | App: " + titleAppName + "\n", Color.GREEN);
        } catch (Exception e) {
            log("[FEHLER] T.xml konnte nicht gelesen werden: " + e.getMessage() + "\n", Color.RED);
        }
    }

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
        if (mainClass.endsWith(".java")) mainClass = mainClass.substring(0, mainClass.length() - 5);
        saveCurrentFile();

        String mode = (String) modeSelector.getSelectedItem();
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        switch (mode) {
            case MODE_JAVA:
                if (mainClass.isEmpty()) { log("[FEHLER] Bitte gib die Main-Klasse ein.\n", Color.RED); return; }
                File outFolder = new File(currentProjectFolder, "out");
                if (!outFolder.exists()) outFolder.mkdirs();
                log("[JAVA] Kompiliere nach /out...\n", Color.CYAN);
                File sourceFile = findSourceFile(currentProjectFolder, mainClass);
                if (sourceFile == null) { log("[FEHLER] Datei nicht gefunden.\n", Color.RED); return; }
                String separator = System.getProperty("path.separator");
                String classpath = "\"out\"" + separator + "\"libs/*\"";
                String compileCmd = "javac -encoding UTF-8 -cp " + classpath + " -d out \"" + sourceFile.getAbsolutePath() + "\"";
                String runCmd     = "java -cp " + classpath + " " + mainClass;
                executeCommand(compileCmd + " && " + runCmd, false);
                break;
            case MODE_C:
            case MODE_CPP:
                File activeC = getActiveFile();
                if (activeC != null) {
                    String compiler = mode.equals(MODE_C) ? "gcc" : "g++";
                    String exeName  = isWindows ? "program.exe" : "./program";
                    log("[" + mode.toUpperCase() + "] Kompiliere " + activeC.getName() + "...\n", Color.CYAN);
                    executeCommand(compiler + " \"" + activeC.getAbsolutePath() + "\" -o program && " + exeName, false);
                } else { log("[FEHLER] Keine C/C++ Datei offen.\n", Color.RED); }
                break;
            case MODE_BATCH:
                File activeBat = getActiveFile();
                if (activeBat != null) {
                    log("[BATCH] Starte " + activeBat.getName() + "...\n", Color.CYAN);
                    executeCommand("\"" + activeBat.getAbsolutePath() + "\"", false);
                } else { log("[FEHLER] Keine Batch-Datei offen.\n", Color.RED); }
                break;
        }

        SwingUtilities.invokeLater(() -> {
            Component tab = editorTabs.getSelectedComponent();
            if (tab instanceof RTextScrollPane) ((RTextScrollPane) tab).getTextArea().requestFocusInWindow();
        });
    }

    private File findSourceFile(File dir, String fqcn) {
        String relativePath = fqcn.replace(".", File.separator) + ".java";
        File direct = new File(dir, relativePath);
        if (direct.exists()) return direct;
        File src = new File(dir, "src");
        if (src.exists() && src.isDirectory()) {
            File inSrc = new File(src, relativePath);
            if (inSrc.exists()) return inSrc;
        }
        if (!fqcn.contains(".")) return searchFileRecursively(dir, fqcn + ".java");
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







    private void handleTBuild() {
        if (currentProjectFolder == null) {
            log("[FEHLER] Bitte öffne zuerst einen Projektordner.\n", Color.RED);
            return;
        }
        File tbuildJar = new File(currentProjectFolder, "TBuild.jar");
        if (tbuildJar.exists()) {
            executeCommand("java -jar TBuild.jar", true);
        } else {
            log("[INFO] TBuild.jar nicht gefunden. Lade herunter...\n", Color.YELLOW);
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
    if (!isTBuild) {
        SwingUtilities.invokeLater(this::clearCompilerErrors); // <-- neu
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
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            String line;
            StringBuilder fullOutput = new StringBuilder();
            while ((line = r.readLine()) != null) {
                fullOutput.append(line).append("\n");
                log(line + "\n", isTBuild ? Color.CYAN : Color.WHITE);
            }
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                log("[PROZESS BEENDET MIT CODE " + exitCode + "]\n", Color.RED);
                if (!isTBuild) {
                    markCompilerErrors(fullOutput.toString());
                }
            }
        } catch (Exception e) {
            log("[TERMINAL FEHLER] " + e.getMessage() + "\n", Color.RED);
        }
    }).start();
}







    private void openFolderDialog() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentProjectFolder = chooser.getSelectedFile();
            updateFileTree(currentProjectFolder);
            log("[INFO] Ordner geöffnet: " + currentProjectFolder.getName() + "\n", Color.CYAN);
            loadTXml(currentProjectFolder);
            // Git-Status beim Öffnen prüfen
            checkGitStatusOnOpen();
        }
    }





    

    private void checkGitStatusOnOpen() {
        new Thread(() -> {
            // Credentials-Status
            String[] creds = loadGitCredentials();
            if (creds != null && creds[1] != null && !creds[1].isEmpty()) {
                log("[GIT]  Angemeldet als: " + creds[0] + "\n", new Color(255, 200, 80));
            } else if (creds != null && creds[0] != null) {
                log("[GIT]  Git-Nutzer: " + creds[0] + " (kein Token – Push/Pull nicht möglich ohne TBuild-Anmeldung)\n", Color.ORANGE);
            } else {
                log("[GIT]  Nicht angemeldet. TBuild → Git → Anmelden.\n", Color.ORANGE);
            }
            // Ist es ein Git-Repo?
            try (Git git = openRepo()) {
                String branch = git.getRepository().getBranch();
                Status status = git.status().call();
                log("[GIT]  Branch: " + branch, new Color(255, 200, 80));
                if (!status.isClean()) {
                    int changed = status.getModified().size() + status.getAdded().size()
                            + status.getUntracked().size() + status.getRemoved().size();
                    log("  (" + changed + " Änderungen)\n", Color.ORANGE);
                } else {
                    log("  (sauber)\n", new Color(80, 200, 120));
                }
            } catch (Exception ignored) {
                // Kein Git-Repo – kein Problem, kein Log
            }
        }).start();
    }






    

    private void updateFileTree(File rootFolder) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new FileNode(rootFolder));
        buildTree(rootFolder, rootNode);
        treeModel.setRoot(rootNode);
    }




    

    private void buildTree(File folder, DefaultMutableTreeNode node) {
        File[] files = folder.listFiles();
        if (files == null) return;
        Arrays.sort(files, (a, b) -> a.isDirectory() == b.isDirectory()
                ? a.getName().compareToIgnoreCase(b.getName())
                : a.isDirectory() ? -1 : 1);
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
            if      (fileName.endsWith(".java"))                 textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            else if (fileName.endsWith(".py"))                   textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
            else if (fileName.endsWith(".c") || fileName.endsWith(".h"))   textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
            else if (fileName.endsWith(".cpp") || fileName.endsWith(".hpp")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
            else if (fileName.endsWith(".xml"))                  textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
            else if (fileName.endsWith(".bat") || fileName.endsWith(".cmd")) textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH);
            else textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);

            textArea.setCodeFoldingEnabled(true);
            textArea.setFont(new Font("Consolas", Font.PLAIN, 15));
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

            // WICHTIG: Verhindert das sofortige Einfügen, wenn es nur einen Treffer gibt!
            ac.setAutoCompleteSingleChoices(false);

            ac.setAutoActivationEnabled(true);
            ac.setAutoActivationDelay(100);
            ac.install(textArea);

            // --- LERN-FUNKTION (Ressourcenschonend) ---
            // knownWords mit Standard-Keywords UND allen Wörtern aus der Datei vorbelegen
            Set<String> knownWords = new HashSet<>();
            String[] initialKeywords = {"public", "private", "static", "void", "class", "import", "String", "int", "boolean", "new", "return"};
            knownWords.addAll(Arrays.asList(initialKeywords));
            // Datei-Tokens vorbelegen (damit beim Lernen keine Duplikate entstehen)
            String existingContent = textArea.getText();
            if (existingContent != null) {
                for (String t : existingContent.split("[^\\w]+")) {
                    if (t.length() > 2) knownWords.add(t);
                }
            }

            // Der KeyListener reagiert nur, wenn eine Taste losgelassen wird.
            textArea.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    char c = e.getKeyChar();

                    // Wir checken das Wort nur, wenn der Nutzer ein Trennzeichen tippt
                    // (z.B. Leerzeichen, Enter, Klammer, Punkt)
                    if (!Character.isLetterOrDigit(c) && c != KeyEvent.CHAR_UNDEFINED) {
                        try {
                            int caret = textArea.getCaretPosition() - 1; // Position vor dem Trennzeichen
                            if (caret < 1) return;

                            String text = textArea.getText(0, caret);
                            int start = caret - 1;

                            // Rückwärts gehen, bis wir den Anfang des Wortes finden
                            while (start >= 0 && Character.isLetterOrDigit(text.charAt(start))) {
                                start--;
                            }
                            start++; // Start-Index des fertigen Wortes

                            if (start < caret) {
                                String word = text.substring(start, caret);
                                // Wir lernen nur Wörter ab 3 Buchstaben (spart massig RAM)
                                if (word.length() > 2 && knownWords.add(word)) {
                                    provider.addCompletion(new BasicCompletion(provider, word));
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            });

            // --- WÖRTER VERWALTEN (Löschen) ---
            JButton manageWordsBtn = new JButton("Wörter");
            manageWordsBtn.setFont(manageWordsBtn.getFont().deriveFont(10f));
            manageWordsBtn.setForeground(new Color(180, 180, 255));
            manageWordsBtn.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
            manageWordsBtn.setContentAreaFilled(false);
            manageWordsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            manageWordsBtn.setToolTipText("Gelernte Wörter verwalten / löschen");
            manageWordsBtn.addActionListener(ev -> showWordManagerDialog(provider, knownWords));
            tabHeader.add(manageWordsBtn);
        } catch (Exception e) { log("Öffnen fehlgeschlagen\n", Color.RED); }
    }

/**
 * Öffnet einen Dialog, in dem alle gelernten Wörter angezeigt
 * und einzeln oder mehrfach gelöscht werden können.
 */
private void showWordManagerDialog(DefaultCompletionProvider provider, Set<String> knownWords) {
    JDialog dialog = new JDialog(this, "Wörter verwalten", true);
    dialog.setSize(400, 480);
    dialog.setLocationRelativeTo(this);
    dialog.setLayout(new BorderLayout(8, 8));
    dialog.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

    // Sortierte Liste der Wörter
    java.util.List<String> sortedWords = new ArrayList<>(knownWords);
    java.util.Collections.sort(sortedWords, String.CASE_INSENSITIVE_ORDER);
    DefaultListModel<String> listModel = new DefaultListModel<>();
    for (String w : sortedWords) listModel.addElement(w);

    JList<String> wordList = new JList<>(listModel);
    wordList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    wordList.setBackground(new Color(30, 31, 34));
    wordList.setForeground(Color.WHITE);
    wordList.setFont(new Font("Consolas", Font.PLAIN, 13));
    JScrollPane scroll = new JScrollPane(wordList);
    scroll.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(Color.DARK_GRAY), "Gelernte Wörter (" + listModel.size() + ")",
        javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
        null, Color.LIGHT_GRAY));

    // Suchfeld zum Filtern
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

    // Buttons
    JButton btnDelete  = new JButton("Ausgewählte löschen");
    JButton btnClose   = new JButton("Schließen");
    btnDelete.setForeground(new Color(255, 100, 100));
    btnDelete.addActionListener(e -> {
        java.util.List<String> selected = wordList.getSelectedValuesList();
        if (selected.isEmpty()) return;
        for (String word : selected) {
            knownWords.remove(word);
            sortedWords.remove(word);
            listModel.removeElement(word);
        }
        // Provider neu aufbauen (SimpleCompletion kennt keine remove-API)
        provider.clear();
        for (String w : knownWords) {
            provider.addCompletion(new BasicCompletion(provider, w));
        }
        // Titel aktualisieren
        scroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY), "Gelernte Wörter (" + listModel.size() + ")",
            javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
            null, Color.LIGHT_GRAY));
        log("[INFO] " + selected.size() + " Wort/Wörter aus Autocomplete entfernt.\n", Color.LIGHT_GRAY);
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

private DefaultCompletionProvider createCompletionProvider(RSyntaxTextArea textArea) {
    DefaultCompletionProvider provider = new DefaultCompletionProvider();

    // Set verhindert, dass ein Wort doppelt hinzugefügt wird
    Set<String> seen = new HashSet<>();

    // Standard-Keywords
    String[] keywords = {"public", "private", "static", "void", "class", "import", "String", "int", "boolean", "new", "return"};
    for (String kw : keywords) {
        if (seen.add(kw)) {
            provider.addCompletion(new BasicCompletion(provider, kw));
        }
    }

    // Alle Wörter aus dem aktuellen Dateiinhalt scannen (einmalig je Wort)
    String content = textArea.getText();
    if (content != null && !content.isEmpty()) {
        String[] tokens = content.split("[^\\w]+");
        for (String token : tokens) {
            if (token.length() > 2 && seen.add(token)) {
                provider.addCompletion(new BasicCompletion(provider, token));
            }
        }
    }

    return provider;
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


    

    private void search(boolean forward) {
        Component selectedTab = editorTabs.getSelectedComponent();
        if (!(selectedTab instanceof RTextScrollPane)) return;
        RSyntaxTextArea textArea = (RSyntaxTextArea) ((RTextScrollPane) selectedTab).getTextArea();
        String text = searchField.getText();
        if (text.isEmpty()) return;
        SearchContext context = new SearchContext();
        context.setSearchFor(text);
        context.setMatchCase(matchCaseCB.isSelected());
        context.setSearchForward(forward);
        context.setWholeWord(false);
        SearchResult result = SearchEngine.find(textArea, context);
        if (!result.wasFound()) log("[INFO] Text nicht gefunden.\n", Color.ORANGE);
    }



    

    private void initSearchPanel() {
        searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(new Color(45, 47, 49));
        searchPanel.setVisible(false);
        searchField = new JTextField(20);
        JButton btnNext  = new JButton("Abwärts");
        JButton btnPrev  = new JButton("Aufwärts");
        matchCaseCB      = new JCheckBox("Groß/Klein");
        JButton btnClose = new JButton("x");
        searchField.addActionListener(e -> search(true));
        btnNext.addActionListener(e  -> search(true));
        btnPrev.addActionListener(e  -> search(false));
        btnClose.addActionListener(e -> searchPanel.setVisible(false));
        searchPanel.add(new JLabel("Suchen:"));
        searchPanel.add(searchField);
        searchPanel.add(btnNext);
        searchPanel.add(btnPrev);
        searchPanel.add(matchCaseCB);
        searchPanel.add(btnClose);
    }










    // ================== GIT INTEGRATION ==================

    /** Laedt Credentials aus ~/.git-credentials (shared mit TBuild und nativem Git) */
    private String[] loadGitCredentials() {
        // 1. ~/.git-credentials (git credential store)
        File credFile = new File(System.getProperty("user.home"), ".git-credentials");
        if (credFile.exists()) {
            try {
                java.util.List<String> lines = Files.readAllLines(credFile.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.contains("github.com") && line.startsWith("https://")) {
                        String part = line.substring("https://".length());
                        int atIdx = part.lastIndexOf('@');
                        if (atIdx > 0) {
                            String userPass = part.substring(0, atIdx);
                            int colonIdx = userPass.indexOf(':');
                            if (colonIdx > 0) {
                                return new String[]{
                                    userPass.substring(0, colonIdx),
                                    userPass.substring(colonIdx + 1)
                                };
                            }
                        }
                    }
                }
            } catch (IOException ignored) {}
        }

        // 2. git credential fill - explizit HOME-Verzeichnis setzen damit
        //    Windows Credential Manager auch ohne Git-Repo-Kontext funktioniert
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "credential", "fill");
            pb.redirectErrorStream(false);
            pb.environment().put("HOME", System.getProperty("user.home"));
            // Arbeitsverzeichnis auf Home setzen - kein Git-Repo noetig
            pb.directory(new File(System.getProperty("user.home")));
            Process p = pb.start();
            // Input in separatem Thread schreiben damit kein Deadlock entsteht
            new Thread(() -> {
                try {
p.getOutputStream().write("protocol=https\nhost=github.com\n\n".getBytes());


                    p.getOutputStream().flush();
                    p.getOutputStream().close();
                } catch (Exception ignored2) {}
            }).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            String user = null, pass = null;
            for (String l : out.split("[\r\n]+")) {
                if (l.startsWith("username=")) user = l.substring(9).trim();
                if (l.startsWith("password=")) pass = l.substring(9).trim();
            }
            if (user != null && pass != null && !user.isEmpty() && !pass.isEmpty()) {
                return new String[]{user, pass};
            }
        } catch (Exception ignored) {}

        // 3. Windows Credential Manager direkt via cmdkey auslesen
        //    (Fallback falls git nicht im PATH ist)
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                Process p = new ProcessBuilder("cmdkey", "/list:git:https://github.com")
                        .redirectErrorStream(true)
                        .directory(new File(System.getProperty("user.home")))
                        .start();
                String out = new String(p.getInputStream().readAllBytes()).trim();
                p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
                // Benutzername aus cmdkey-Ausgabe extrahieren
                for (String l : out.split("[\r\n]+")) {
                    l = l.trim();
                    if (l.startsWith("Benutzername:") || l.startsWith("User name:")) {
                        String user = l.substring(l.indexOf(':') + 1).trim();
                        if (!user.isEmpty()) {
                            // Token koennen wir nicht aus cmdkey lesen,
                            // aber git credential fill sollte dann funktionieren
                            // - nochmal versuchen mit bekanntem User
                            return new String[]{user, ""};
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 4. Nur Username aus git config (fuer Anzeige)
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "config", "--global", "user.name");
            pb.redirectErrorStream(true);
            pb.directory(new File(System.getProperty("user.home")));
            Process p = pb.start();
            String name = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            if (!name.isEmpty()) return new String[]{name, null};
        } catch (Exception ignored) {}

        return null;
    }

    /** Oeffnet das Git-Repo im aktuellen Projektordner */
    private Git openRepo() throws IOException {
        if (currentProjectFolder == null) throw new IOException("Kein Projektordner geöffnet.");
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.findGitDir(currentProjectFolder).readEnvironment().build();
        return new Git(repo);
    }

    /** Gibt CredentialsProvider zurueck oder null */
    private UsernamePasswordCredentialsProvider getCredentials() {
        String[] c = loadGitCredentials();
        if (c == null || c[1] == null || c[1].isEmpty()) return null;
        return new UsernamePasswordCredentialsProvider(c[0], c[1]);
    }




    

    private void gitCommit() {
        if (currentProjectFolder == null) {
            log("[GIT] Bitte zuerst einen Projektordner öffnen.\n", Color.ORANGE);
            return;
        }
        // Alle offenen Dateien speichern
        saveCurrentFile();

        // Anmeldestatus anzeigen
        String[] creds = loadGitCredentials();
        String userHint = creds != null ? " (als " + creds[0] + ")" : " (nicht angemeldet)";

        String message = (String) JOptionPane.showInputDialog(this,
                "Commit-Nachricht:" + userHint,
                "Git Commit", JOptionPane.PLAIN_MESSAGE, null, null,
                "Update");
        if (message == null || message.trim().isEmpty()) return;

        new Thread(() -> {
            log("[GIT] Committe Änderungen...\n", new Color(255, 200, 80));
            try (Git git = openRepo()) {
                // Alle Änderungen stagen
                Status status = git.status().call();
                if (status.isClean()) {
                    log("[GIT] Nichts zu committen – alles sauber.\n", Color.LIGHT_GRAY);
                    return;
                }
                git.add().addFilepattern(".").call();
                // Auch gelöschte Dateien stagen
                for (String missing : status.getMissing()) {
                    git.rm().addFilepattern(missing).call();
                }
                RevCommit commit = git.commit().setMessage(message.trim()).call();
                log("[GIT] ✓ Commit: " + commit.getShortMessage()
                        + " [" + commit.abbreviate(7).name() + "]\n", new Color(80, 200, 120));
            } catch (Exception e) {
                log("[GIT FEHLER] Commit fehlgeschlagen: " + e.getMessage() + "\n", Color.RED);
                if (e.getMessage() != null && e.getMessage().contains("git dir not found")) {
                    log("[GIT] Tipp: Repo noch nicht initialisiert. TBuild → Git → Lokales Repo erstellen.\n", Color.ORANGE);
                }
            }
        }).start();
    }







    

    private void gitPush() {
        if (currentProjectFolder == null) {
            log("[GIT] Bitte zuerst einen Projektordner öffnen.\n", Color.ORANGE);
            return;
        }
        UsernamePasswordCredentialsProvider cp = getCredentials();
        if (cp == null) {
            String[] creds = loadGitCredentials();
            if (creds != null && creds[0] != null) {
                log("[GIT] Git-Nutzer erkannt (" + creds[0] + "), aber kein Token gefunden.\n", Color.ORANGE);
                log("[GIT] Bitte in TBuild → Git → Anmelden einen PAT eintragen.\n", Color.ORANGE);
            } else {
                log("[GIT] Nicht angemeldet. Bitte in TBuild → Git → Anmelden.\n", Color.ORANGE);
            }
            return;
        }
        String[] creds = loadGitCredentials();
        new Thread(() -> {
            log("[GIT] Pushe zu Remote...\n", new Color(255, 200, 80));
            try (Git git = openRepo()) {
                git.push()
                        .setCredentialsProvider(cp)
                        .setRemote("origin")
                        .call();
                log("[GIT] ✓ Push erfolgreich" + (creds != null ? " (als " + creds[0] + ")" : "") + ".\n",
                        new Color(80, 200, 120));
            } catch (Exception e) {
                log("[GIT FEHLER] Push fehlgeschlagen: " + e.getMessage() + "\n", Color.RED);
                if (e.getMessage() != null && e.getMessage().contains("no remote")) {
                    log("[GIT] Tipp: Kein Remote gesetzt. TBuild → Git → Remote hinzufügen.\n", Color.ORANGE);
                }
            }
        }).start();
    }
    

    private void gitPull() {
        if (currentProjectFolder == null) {
            log("[GIT] Bitte zuerst einen Projektordner öffnen.\n", Color.ORANGE);
            return;
        }
        UsernamePasswordCredentialsProvider cp = getCredentials();
        new Thread(() -> {
            log("[GIT] Pulle vom Remote...\n", new Color(255, 200, 80));
            try (Git git = openRepo()) {
                org.eclipse.jgit.api.PullCommand pullCmd = git.pull();
                if (cp != null) pullCmd.setCredentialsProvider(cp);
                PullResult result = pullCmd.call();
                if (result.isSuccessful()) {
                    String mergeMsg = result.getMergeResult() != null
                            ? result.getMergeResult().getMergeStatus().toString()
                            : "OK";
                    log("[GIT] Pull erfolgreich: " + mergeMsg + "\n", new Color(80, 200, 120));
                    // Dateibaum aktualisieren
                    SwingUtilities.invokeLater(() -> updateFileTree(currentProjectFolder));
                } else {
                    log("[GIT FEHLER] Pull nicht erfolgreich.\n", Color.RED);
                }
            } catch (Exception e) {
                log("[GIT FEHLER] Pull fehlgeschlagen: " + e.getMessage() + "\n", Color.RED);
            }
        }).start();
    }

}