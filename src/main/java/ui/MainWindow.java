package ui;

import editor.CompilerErrorMarker;
import editor.EditorManager;
import git.GitManager;
import org.fife.ui.rtextarea.RTextScrollPane;
import runner.ProjectRunner;
import update.UpdateManager;

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

public class MainWindow extends JFrame {

    public static final String APP_VERSION = "2.5.0";
    public static final String GITHUB_REPO = "Thillager/TIDE";

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
    private JTextField mainClassInput;
    private JButton btnTBuild;
    private JButton btnOpen;
    private JButton btnSave;
    private JButton btnClear;
    private JButton btnAbout;
    private JLabel modeLabel;
    private JLabel mainClassLabel;
    private SearchPanel searchPanel;

    // ---- Subsysteme ----
    private ConsolePanel consolePanel;
    private FileTreePanel fileTreePanel;
    private EditorManager editorManager;
    private CompilerErrorMarker errorMarker;
    private ProjectRunner projectRunner;
    private GitManager gitManager;
    private UpdateManager updateManager;
    private AboutDialog aboutDialog;
    private WordManagerDialog wordManagerDialog;

    public MainWindow() {
        setTitle("TIDE v" + APP_VERSION);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initSubsystems();
        initUI();
    }

    private void initSubsystems() {
        consolePanel     = new ConsolePanel();
        editorTabs       = new JTabbedPane();
        wordManagerDialog = new WordManagerDialog(this, consolePanel);
        editorManager    = new EditorManager(this, editorTabs, openFiles, consolePanel, wordManagerDialog);
        errorMarker      = new CompilerErrorMarker(editorTabs, openFiles, consolePanel);
        projectRunner    = new ProjectRunner(consolePanel, editorManager, errorMarker);
        gitManager       = new GitManager(this, consolePanel);
        updateManager    = new UpdateManager(this, consolePanel, APP_VERSION, GITHUB_REPO);
        aboutDialog      = new AboutDialog(this, APP_VERSION, GITHUB_REPO, updateManager);

        fileTreePanel = new FileTreePanel(this, consolePanel, file -> editorManager.openFileInEditor(file));

        // Callbacks
        gitManager.setOnRefreshFileTree(() -> fileTreePanel.updateFileTree(currentProjectFolder));
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

        // Toolbar befuellen
        toolBar.add(btnOpen);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnSave);
        modeLabel = new JLabel("Modus/Mode: ");
        toolBar.addSeparator(new Dimension(20, 30));
        toolBar.add(modeLabel);
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

        // Language selector
        JComboBox<LanguageManager.Language> langSel = new JComboBox<>(LanguageManager.Language.values());
        langSel.setPreferredSize(new Dimension(95, 28));
        langSel.setMaximumSize(new Dimension(95, 28));
        langSel.setToolTipText("Language / Sprache");
        langSel.addActionListener(e -> {
            LanguageManager.set((LanguageManager.Language) langSel.getSelectedItem());
            // Update alle UI-Texte
            btnOpen.setText(LanguageManager.t("open"));
            btnSave.setText(LanguageManager.t("save"));
            btnClear.setText(LanguageManager.t("clear"));
            btnAbout.setText(LanguageManager.t("about"));
            modeLabel.setText(LanguageManager.t("mode"));
            mainClassLabel.setText(LanguageManager.t("main"));
        });
        toolBar.add(langSel);
        toolBar.add(Box.createHorizontalStrut(10));

        toolBar.add(btnClear);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(btnAbout);

        add(toolBar, BorderLayout.NORTH);

        // FileTree MouseListener für Kontextmenü (mit Projektordner-Referenz)
        fileTreePanel.getViewport().getView().addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent me) {
                if (me.isPopupTrigger()) fileTreePanel.showFileTreePopup(me, currentProjectFolder, null);
            }
            @Override public void mouseReleased(MouseEvent me) {
                if (me.isPopupTrigger()) fileTreePanel.showFileTreePopup(me, currentProjectFolder, null);
            }
        });

        // Layout
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorContainer, consolePanel);
        verticalSplit.setResizeWeight(0.7);
        verticalSplit.setDividerSize(4);
        verticalSplit.setBorder(null);

        JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileTreePanel, verticalSplit);
        horizontalSplit.setDividerSize(4);
        horizontalSplit.setBorder(null);
        add(horizontalSplit, BorderLayout.CENTER);

        // Event Listeners
        btnOpen.addActionListener(e  -> openFolderDialog());
        btnSave.addActionListener(e  -> editorManager.saveCurrentFile());
        btnRun.addActionListener(e   -> projectRunner.runProject(
                (String) modeSelector.getSelectedItem(), mainClassInput.getText().trim()));
        btnClear.addActionListener(e -> consolePanel.clear());
        btnTBuild.addActionListener(e -> projectRunner.handleTBuild());
        btnAbout.addActionListener(e  -> aboutDialog.show());

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