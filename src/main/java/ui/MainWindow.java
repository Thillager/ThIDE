package ui;

import editor.CompilerErrorMarker;
import editor.EditorManager;
import git.GitManager;
import org.fife.ui.rtextarea.RTextScrollPane;
import runner.ProjectRunner;
import runner.DebugRunner;
import update.UpdateManager;
import config.TIDEProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import config.LanguageManager;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import config.TIDEPreferences;

public class MainWindow extends JFrame {

    private static final String MODE_JAVA   = ProjectRunner.MODE_JAVA;
    private static final String MODE_PYTHON = ProjectRunner.MODE_PYTHON;
    private static final String MODE_C      = ProjectRunner.MODE_C;
    private static final String MODE_CPP    = ProjectRunner.MODE_CPP;
    private static final String MODE_BATCH  = ProjectRunner.MODE_BATCH;

    // ---- State ----
    private File currentProjectFolder;
    private final Map<Component, File> openFiles = new HashMap<>();

    // ---- UI-Komponenten ----
    private JTabbedPane editorTabs;
    private JComboBox<String> modeSelector;
    private JComboBox<String> debugModeSelector;
    private JTextField mainClassInput;
    private JButton btnTBuild;
    private JButton btnOpen;
    private JButton btnSave;
    private JButton btnTerminate;
    private JButton btnClear;
    private JButton btnAbout;
    private JButton btnDebugStart;
    private JButton btnDebugRestart;
    private JLabel modeLabel;
    private JLabel mainClassLabel;
    private JLabel debugModeLabel;
    private SearchPanel searchPanel;

    // ---- Subsysteme ----
    private ConsolePanel consolePanel;
    private FileTreePanel fileTreePanel;
    private EditorManager editorManager;
    private CompilerErrorMarker errorMarker;
    private ProjectRunner projectRunner;
    private DebugRunner debugRunner;
    private GitManager gitManager;
    private UpdateManager updateManager;
    private AboutDialog aboutDialog;
    private WordManagerDialog wordManagerDialog;

    private JSplitPane horizontalSplit;
    private JSplitPane verticalSplit;

    public MainWindow() {
        setTitle("TIDE v" + TIDEProperties.APP_VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initSubsystems();
        initUI();

        // 1. Größe setzen (falls NICHT maximiert)
        setSize(
            TIDEPreferences.getWindowWidth(),
            TIDEPreferences.getWindowHeight()
        );

        // 2. Position zentrieren
        setLocationRelativeTo(null);

        // 3. Maximieren (entscheidend: NACH setSize!)
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // 4. Divider erst NACH Layout setzen
        SwingUtilities.invokeLater(() -> {
            horizontalSplit.setDividerLocation(
                TIDEPreferences.getDividerHProportion()
            );

            verticalSplit.setDividerLocation(
                TIDEPreferences.getDividerVProportion()
            );
        });
    }

    private void initSubsystems() {
        consolePanel     = new ConsolePanel();
        editorTabs       = new JTabbedPane();
        wordManagerDialog = new WordManagerDialog(this, consolePanel);
        editorManager    = new EditorManager(this, editorTabs, openFiles, consolePanel, wordManagerDialog);
        errorMarker      = new CompilerErrorMarker(editorTabs, openFiles, consolePanel);
        projectRunner    = new ProjectRunner(consolePanel, editorManager, errorMarker);
        debugRunner      = new DebugRunner(consolePanel, editorManager, errorMarker, projectRunner);
        gitManager       = new GitManager(this, consolePanel);
        updateManager    = new UpdateManager(this, consolePanel, TIDEProperties.APP_VERSION, TIDEProperties.GITHUB_REPO);
        aboutDialog      = new AboutDialog(this, TIDEProperties.APP_VERSION, TIDEProperties.GITHUB_REPO, updateManager);

        fileTreePanel = new FileTreePanel(this, consolePanel, file -> editorManager.openFileInEditor(file));

        // Callbacks
        gitManager.setOnRefreshFileTree(() -> {
            fileTreePanel.updateFileTree(currentProjectFolder);
            revalidate();
            repaint();
        });
        gitManager.setOnSaveCurrentFile(() -> editorManager.saveCurrentFile());
        projectRunner.setOnRefreshFileTree(() -> fileTreePanel.updateFileTree(currentProjectFolder));
    }

    private void initUI() {
        // Search Panel (muss vor editorContainer erstellt sein)
        searchPanel = new SearchPanel(editorTabs, consolePanel);
        editorTabs.setBorder(null);

        // --- Toolbar ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(new EmptyBorder(8, 10, 8, 10));
        toolBar.setBackground(new Color(43, 45, 48));

        btnOpen  = new JButton(LanguageManager.t("open"));
        btnSave  = new JButton(LanguageManager.t("save"));

        btnTerminate = new JButton("X");
        btnTerminate.setForeground(new Color(230, 75, 75)); 
        btnTerminate.setFont(btnTerminate.getFont().deriveFont(Font.BOLD, 14f)); 

        modeSelector = new JComboBox<>(new String[]{MODE_JAVA, MODE_PYTHON, MODE_C, MODE_CPP, MODE_BATCH});
        modeSelector.setPreferredSize(new Dimension(90, 28));
        modeSelector.setMaximumSize(new Dimension(90, 28));

        mainClassLabel = new JLabel(" Main-Class: ");
        mainClassInput = new JTextField("Main", 10);
        mainClassInput.setPreferredSize(new Dimension(130, 28));
        mainClassInput.setMaximumSize(new Dimension(130, 28));

        JButton btnRun = new JButton("▶");
        btnTBuild      = new JButton("T-Build");
        btnClear       = new JButton(LanguageManager.t("clear"));
        btnAbout       = new JButton(LanguageManager.t("about"));

        btnRun.setForeground(new Color(80, 200, 120));
        btnRun.setFont(btnRun.getFont().deriveFont(Font.BOLD));
        btnTBuild.setForeground(new Color(100, 150, 255));
        btnAbout.setForeground(new Color(180, 180, 180));

        // ===== DEBUG COMPONENTS =====
        debugModeLabel = new JLabel(" Debug Mode: ");
        debugModeSelector = new JComboBox<>(new String[]{MODE_JAVA, MODE_PYTHON});
        debugModeSelector.setPreferredSize(new Dimension(100, 28));
        debugModeSelector.setMaximumSize(new Dimension(100, 28));

        btnDebugStart = new JButton("🐛 Debug");
        btnDebugStart.setForeground(new Color(255, 100, 100));
        btnDebugStart.setFont(btnDebugStart.getFont().deriveFont(Font.BOLD));

        btnDebugRestart = new JButton("⟲ Restart");
        btnDebugRestart.setForeground(new Color(255, 150, 100));
        btnDebugRestart.setFont(btnDebugRestart.getFont().deriveFont(Font.BOLD));
        btnDebugRestart.setVisible(false);

        // Git-Dropdown
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

        gitCommit.addActionListener(e -> gitManager.gitCommit());
        gitPush.addActionListener(e   -> gitManager.gitPush());
        gitPull.addActionListener(e   -> gitManager.gitPull());
        gitMenuBar.add(gitMenu);

        // Editor Container
        JPanel editorContainer = new JPanel(new BorderLayout());
        editorContainer.add(searchPanel, BorderLayout.NORTH);
        editorContainer.add(editorTabs,  BorderLayout.CENTER);

        // Language selector
        JComboBox<LanguageManager.Language> langSel = new JComboBox<>(LanguageManager.Language.values());
        langSel.setPreferredSize(new Dimension(95, 28));
        langSel.setMaximumSize(new Dimension(95, 28));
        langSel.setToolTipText("Language / Sprache");
        langSel.addActionListener(e -> {
            LanguageManager.Language selected = (LanguageManager.Language) langSel.getSelectedItem();
            LanguageManager.set(selected);
            TIDEPreferences.saveLanguage(selected.name());

            // UI-Texte aktualisieren
            btnOpen.setText(LanguageManager.t("open"));
            btnSave.setText(LanguageManager.t("save"));
            btnClear.setText(LanguageManager.t("clear"));
            btnAbout.setText(LanguageManager.t("about"));
            modeLabel.setText(LanguageManager.t("mode"));
            mainClassLabel.setText(LanguageManager.t("main"));
        });

        // --- Toolbar befüllen ---
        
        // 1. LINKER BEREICH
        toolBar.add(btnOpen);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnSave);
        modeLabel = new JLabel(LanguageManager.t("mode"));
        toolBar.addSeparator(new Dimension(20, 30));
        toolBar.add(modeLabel);
        toolBar.add(modeSelector);
        toolBar.add(mainClassLabel);
        toolBar.add(mainClassInput);
        toolBar.addSeparator(new Dimension(20, 30));
        toolBar.add(btnRun);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnTerminate);
        
        // ===== DEBUG TOOLBAR SECTION =====
        toolBar.addSeparator(new Dimension(20, 30));
        toolBar.add(debugModeLabel);
        toolBar.add(debugModeSelector);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnDebugStart);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnDebugRestart);

        // 2. DER KLICKPUNKT: Alles ab hier rutscht nach RECHTS
        toolBar.add(Box.createHorizontalGlue());

        // 3. RECHTER BEREICH
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnTBuild);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(gitMenuBar);
        toolBar.add(Box.createHorizontalStrut(15));
        toolBar.add(langSel);
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(btnClear);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnAbout);

        add(toolBar, BorderLayout.NORTH);

        // FileTree MouseListener für Kontextmenü
        fileTreePanel.getViewport().getView().addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent me) {
                if (me.isPopupTrigger()) fileTreePanel.showFileTreePopup(me, currentProjectFolder, null);
            }
            @Override public void mouseReleased(MouseEvent me) {
                if (me.isPopupTrigger()) fileTreePanel.showFileTreePopup(me, currentProjectFolder, null);
            }
        });

        // Layout Splits
        verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorContainer, consolePanel);
        verticalSplit.setResizeWeight(0.7);
        verticalSplit.setDividerSize(4);
        verticalSplit.setBorder(null);

        verticalSplit.addPropertyChangeListener(
            JSplitPane.DIVIDER_LOCATION_PROPERTY,
            e -> {
                if (verticalSplit.getHeight() > 0) {
                    TIDEPreferences.saveDividerVProportion(
                        verticalSplit.getDividerLocation() / (double) verticalSplit.getHeight()
                    );
                }
            }
        );

        horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileTreePanel, verticalSplit);
        horizontalSplit.setDividerSize(4);
        horizontalSplit.setBorder(null);

        horizontalSplit.addPropertyChangeListener(
            JSplitPane.DIVIDER_LOCATION_PROPERTY,
            e -> {
                if (horizontalSplit.getWidth() > 0) {
                    TIDEPreferences.saveDividerHProportion(
                        horizontalSplit.getDividerLocation() / (double) horizontalSplit.getWidth()
                    );
                }
            }
        );

        add(horizontalSplit, BorderLayout.CENTER);

        // Event Listeners
        btnOpen.addActionListener(e  -> openFolderDialog());
        btnSave.addActionListener(e  -> editorManager.saveCurrentFile());
        btnTerminate.addActionListener(e -> projectRunner.stopRunningProcess());
        btnRun.addActionListener(e   -> projectRunner.runProject(
                (String) modeSelector.getSelectedItem(), mainClassInput.getText().trim()));
        btnClear.addActionListener(e -> consolePanel.clear());
        btnTBuild.addActionListener(e -> projectRunner.handleTBuild());
        btnAbout.addActionListener(e  -> aboutDialog.show());

        // ===== DEBUG EVENT LISTENERS =====
        btnDebugStart.addActionListener(e -> {
            String debugMode = (String) debugModeSelector.getSelectedItem();
            String mainClass = mainClassInput.getText().trim();
            if (mainClass.isEmpty() && debugMode.equals(MODE_JAVA)) {
                consolePanel.log("[DEBUG] Main-Class ist erforderlich für Java Debug.\n", Color.RED);
                return;
            }
            debugRunner.startDebug(debugMode, mainClass);
        });

        btnDebugRestart.addActionListener(e -> debugRunner.restartDebug());

        modeSelector.addActionListener(e -> updateDynamicUI());

        consolePanel.getTerminalInput().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String cmd = consolePanel.getTerminalInput().getText().trim();
                    if (!cmd.isEmpty()) {
                        projectRunner.executeCommand(cmd, false);
                        consolePanel.getTerminalInput().setText("");
                    }
                }
            }
        });

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.isControlDown()) {
                if (e.getKeyCode() == KeyEvent.VK_S) { editorManager.saveCurrentFile(); return true; }
                if (e.getKeyCode() == KeyEvent.VK_F) {
                    searchPanel.setVisible(!searchPanel.isVisible());
                    if (searchPanel.isVisible()) searchPanel.getSearchField().requestFocusInWindow();
                    revalidate();
                    return true;
                }
            }
            return false;
        });

        // Fenster-Einstellungen beim Schließen speichern
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                TIDEPreferences.saveWindowWidth(getWidth());
                TIDEPreferences.saveWindowHeight(getHeight());

                if (currentProjectFolder != null) {
                    TIDEPreferences.saveLastFolder(currentProjectFolder.getAbsolutePath());
                }
            }
        });

        // Sprache laden
        LanguageManager.set(LanguageManager.Language.valueOf(TIDEPreferences.getLanguage()));
        langSel.setSelectedItem(LanguageManager.Language.valueOf(TIDEPreferences.getLanguage()));

        // Letzten Ordner öffnen
        String lastFolder = TIDEPreferences.getLastFolder();
        if (lastFolder != null) {
            currentProjectFolder = new File(lastFolder);
            fileTreePanel.updateFileTree(currentProjectFolder);
            loadTXml(currentProjectFolder);
            projectRunner.setCurrentProjectFolder(currentProjectFolder);
            debugRunner.setCurrentProjectFolder(currentProjectFolder);
            gitManager.setCurrentProjectFolder(currentProjectFolder);
        }

        // Modus und Main-Class wiederherstellen
        modeSelector.setSelectedItem(TIDEPreferences.getMode());

        // --- Verknüpfung & Initialisierung für die Buttons ---
        projectRunner.setTerminateButton(btnTerminate);
        btnTerminate.setVisible(false);

        debugRunner.setDebugRestartButton(btnDebugRestart);

        updateDynamicUI();
    }


    private void openFolderDialog() {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentProjectFolder = chooser.getSelectedFile();
            fileTreePanel.updateFileTree(currentProjectFolder);
            consolePanel.log("[INFO] Ordner geöffnet/[Info] Folder opened: " + currentProjectFolder.getName() + "\n", Color.CYAN);
            loadTXml(currentProjectFolder);
            projectRunner.setCurrentProjectFolder(currentProjectFolder);
            debugRunner.setCurrentProjectFolder(currentProjectFolder);
            gitManager.setCurrentProjectFolder(currentProjectFolder);
            gitManager.checkGitStatusOnOpen();
        }
    }

    private void updateDynamicUI() {
        String mode   = (String) modeSelector.getSelectedItem();
        boolean isJava = MODE_JAVA.equals(mode);
        btnTBuild.setVisible(isJava);
        mainClassLabel.setVisible(isJava);
        mainClassInput.setVisible(isJava);
        revalidate();
        repaint();
    }

    // ======= T.XML =======

    private void loadTXml(File folder) {
        File txml = new File(folder, "T.xml");
        if (!txml.exists()) {
            consolePanel.log("[WARNUNG] Keine T.xml im Projektordner gefunden.\n" + "[Attention] No T.xml found.\n", Color.ORANGE);
            setTitle("TIDE v" + TIDEProperties.APP_VERSION + " - " + folder.getName());
            return;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document doc = dbf.newDocumentBuilder().parse(txml);
            String mainClass = getXmlTag(doc, "mainClass");
            String appName   = getXmlTag(doc, "appName");
            String version   = getXmlTag(doc, "version");
            if (mainClass != null && !mainClass.isEmpty()) mainClassInput.setText(mainClass);
            String titleAppName = (appName != null && !appName.isEmpty()) ? appName : folder.getName();
            String titleVersion = (version != null && !version.isEmpty()) ? version : "";
            if (!titleVersion.isEmpty()) {
                setTitle("TIDE v" + TIDEProperties.APP_VERSION + " - " + titleAppName + " v" + titleVersion);
            } else {
                setTitle("TIDE v" + TIDEProperties.APP_VERSION + " - " + titleAppName);
            }
            consolePanel.log("[INFO] T.xml geladen — Main-Class: " + mainClass + " | App: " + titleAppName + "\n", Color.GREEN);
        } catch (Exception e) {
            consolePanel.log("[FEHLER] T.xml konnte nicht gelesen werden: " + e.getMessage() + "\n", Color.RED);
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
}
