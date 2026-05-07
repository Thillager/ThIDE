import com.formdev.flatlaf.FlatDarkLaf;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

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

public class TIDE extends JFrame {

    // Aktuelle Version der App - bei jedem Release erhoehen
    private static final String APP_VERSION = "1.7.0";
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

        JButton btnOpen  = new JButton("📁 Ordner öffnen");
        JButton btnSave  = new JButton("💾 Speichern");

        modeSelector = new JComboBox<>(new String[]{MODE_JAVA, MODE_PYTHON, MODE_C, MODE_CPP, MODE_BATCH});
        modeSelector.setMaximumSize(new Dimension(100, 30));

        mainClassLabel = new JLabel(" Main-Class: ");
        mainClassInput = new JTextField("Main", 15);
        mainClassInput.setMaximumSize(new Dimension(200, 30));

        JButton btnRun   = new JButton("▶ Run");
        btnTBuild        = new JButton("🛠 T-Build");
        JButton btnClear = new JButton("🗑 Clear Console");
        JButton btnAbout = new JButton("ℹ Über");

        btnRun.setForeground(new Color(80, 200, 120));
        btnRun.setFont(btnRun.getFont().deriveFont(Font.BOLD));
        btnTBuild.setForeground(new Color(100, 150, 255));
        btnAbout.setForeground(new Color(180, 180, 180));

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

        JLabel descLabel = new JLabel("<html><center>Einfache IDE fuer Anfaenger.<br>Kein Maven, kein Gradle - einfach bauen.</center></html>");
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

        JButton btnUpdate = new JButton("🔄 Nach Updates suchen");
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

    /**
     * Laedt den Installer herunter und startet ihn ueber ein externes Skript.
     * Das Skript wartet bis die App beendet ist, installiert dann und startet neu.
     */
    private void downloadAndInstallUpdate(String version, String releaseJson) {
        new Thread(() -> {
            try {
                String os = System.getProperty("os.name", "").toLowerCase();
                boolean isWindows = os.contains("win");
                boolean isLinux   = !isWindows && !os.contains("mac");

                // Richtigen Dateinamen fuer dieses OS bestimmen
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
     * Erstellt eine .bat-Datei die wartet bis TIDE beendet ist,
     * dann den MSI-Installer ausfuehrt und TIDE neu startet.
     */
    private void launchWindowsUpdate(File msiFile) throws IOException {
        File batFile = new File(System.getProperty("java.io.tmpdir"), "tide_update.bat");

        // Installationspfad der laufenden App ermitteln
        String appExe = ProcessHandle.current().info().command().orElse(null);

        try (PrintWriter pw = new PrintWriter(batFile)) {
            pw.println("@echo off");
            pw.println("echo Warte auf TIDE-Beendigung...");
            pw.println("timeout /t 3 /nobreak > nul");
            pw.println("echo Installiere Update...");
            // Anführungszeichen korrekt escaped, kein Leerzeichen-Problem
            pw.println("msiexec /i \"" + msiFile.getAbsolutePath() + "\" /qn");
            pw.println("if %errorlevel% neq 0 (");
            pw.println("  echo Installation fehlgeschlagen, versuche mit Adminrechten...");
            pw.println("  PowerShell -Command \"Start-Process msiexec -ArgumentList '/i \\\""
                    + msiFile.getAbsolutePath().replace("\\", "\\\\")
                    + "\\\" /qn' -Verb RunAs -Wait\"");
            pw.println(")");
            // App neu starten falls Pfad bekannt
            if (appExe != null && new File(appExe).exists() && appExe.endsWith(".exe")) {
                pw.println("echo Starte TIDE neu...");
                pw.println("start \"\" \"" + appExe + "\"");
            }
            pw.println("del \"%~f0\"");
        }

        log("[INFO] Starte Windows-Update-Skript...\n", Color.CYAN);
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(TIDE.this,
                    "Das Update wird jetzt installiert.\n" +
                            "TIDE wird sich gleich beenden und automatisch neu starten.",
                    "Update wird installiert",
                    JOptionPane.INFORMATION_MESSAGE);
            try {
                // Korrekte Übergabe: cmd /c mit dem Pfad in Anführungszeichen
                new ProcessBuilder("cmd.exe", "/c", "\"" + batFile.getAbsolutePath() + "\"")
                        .start();
            } catch (IOException e) {
                log("[FEHLER] Konnte Update-Skript nicht starten.\n", Color.RED);
                return;
            }
            System.exit(0);
        });
    }

    /**
     * Erstellt ein .sh-Skript das pkexec (grafischer Passwort-Dialog) nutzt
     * um dpkg mit Root-Rechten auszufuehren.
     */
    private void launchLinuxUpdate(File debFile) throws IOException {
        File shFile = new File(System.getProperty("java.io.tmpdir"), "tide_update.sh");

        try (PrintWriter pw = new PrintWriter(shFile)) {
            pw.println("#!/bin/bash");
            pw.println("sleep 2");
            pw.println("# pkexec zeigt grafischen Passwort-Dialog fuer Root-Rechte");
            pw.println("pkexec dpkg -i " + debFile.getAbsolutePath());
            pw.println("# App neu starten falls im PATH vorhanden");
            pw.println("which tide && tide &");
            pw.println("rm -- \"$0\""); // Skript selbst loeschen
        }
        // Skript ausfuehrbar machen
        shFile.setExecutable(true);

        log("[INFO] Starte Linux-Update-Skript...\n", Color.CYAN);
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(TIDE.this,
                    "Das Update wird jetzt installiert.\n" +
                            "Du wirst nach deinem Passwort gefragt.\n" +
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
                while ((line = r.readLine()) != null) log(line + "\n", isTBuild ? Color.CYAN : Color.WHITE);
                int exitCode = p.waitFor();
                if (exitCode != 0) log("[PROZESS BEENDET MIT CODE " + exitCode + "]\n", Color.RED);
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
            closeBtn.addActionListener(e -> { openFiles.remove(sp); editorTabs.remove(sp); });
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
}
