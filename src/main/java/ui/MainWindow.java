package ui;

import editor.CompilerErrorMarker;
import editor.EditorManager;
import git.GitManager;
import runner.ProjectRunner;
import runner.DebugRunner;
import update.UpdateManager;
import config.TIDEProperties;
import config.LanguageManager;
import config.TIDEPreferences;
import ui.SettingsDialog;

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
import java.util.Locale;
import javax.swing.border.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;




@SuppressWarnings({"serial", "this-escape"})
public class MainWindow extends JFrame {

	private static final String MODE_JAVA   = ProjectRunner.MODE_JAVA;
	private static final String MODE_PYTHON = ProjectRunner.MODE_PYTHON;
	private static final String MODE_C      = ProjectRunner.MODE_C;
	private static final String MODE_CPP    = ProjectRunner.MODE_CPP;
	private static final String MODE_BATCH  = ProjectRunner.MODE_BATCH;

	private static final String RUN_MODE_STANDARD = "Standard";
	private static final String RUN_MODE_DEBUG     = "Debug";

	// ---- State ----
	private File currentProjectFolder;
	private final Map<Component, File> openFiles = new HashMap<>();

	// ---- UI-Komponenten ----
	private JTabbedPane editorTabs;
	private JComboBox<String> modeSelector;
	private JComboBox<String> runModeSelector;
	private JTextField mainClassInput;
	private JButton btnTBuild;
	private JButton btnOpen;
	private JButton btnSave;
	private JButton btnTerminate;
	private JButton btnClear;
	private JButton btnAbout;
	private JButton btnRun;
	private JButton btnFormat;
	private JButton btnHotSwap;
	private JLabel modeLabel;
	private JLabel mainClassLabel;
	private SearchPanel searchPanel;
	private GlobalSearchPanel globalSearchPanel; // ← HIER

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
	private SettingsDialog settingsDialog;

	private JSplitPane horizontalSplit;
	private JSplitPane verticalSplit;
	private JSplitPane editorOutlineSplit; 
	private OutlinePanel outlinePanel;


	private Timer smoothScrollTimer;
	private double targetScrollY = 0;


	public static int rawDeltaY = 0;


	public static double currentVelocity = 0.0;
	public static int scrollDir = 0;
	public static float dynIntensity = 0.0f;

	public MainWindow() {

		LanguageManager.Language lang =
		LanguageManager.Language.valueOf(TIDEPreferences.getLanguage());

		Locale locale =
		lang == LanguageManager.Language.DE
		? Locale.GERMAN
		: Locale.ENGLISH;

		Locale.setDefault(locale);
		JComponent.setDefaultLocale(locale);

		setTitle("TIDE v" + TIDEProperties.APP_VERSION);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		initSubsystems();
		initUI();

		setSize(TIDEPreferences.getWindowWidth(), TIDEPreferences.getWindowHeight());
		setLocationRelativeTo(null);
		setExtendedState(JFrame.MAXIMIZED_BOTH);

		SwingUtilities.invokeLater(() -> {
				horizontalSplit.setDividerLocation(TIDEPreferences.getDividerHProportion());
				verticalSplit.setDividerLocation(TIDEPreferences.getDividerVProportion());
				editorOutlineSplit.setDividerLocation(
					editorOutlineSplit.getWidth() - TIDEPreferences.getOutlineWidth()
				);
			});
	}

	private void initSubsystems() {
		consolePanel      = new ConsolePanel();
		editorTabs        = new JTabbedPane();
		wordManagerDialog = new WordManagerDialog(this, consolePanel);
		editorManager     = new EditorManager(this, editorTabs, openFiles, consolePanel, wordManagerDialog);
		errorMarker       = new CompilerErrorMarker(editorTabs, openFiles, consolePanel);
		projectRunner     = new ProjectRunner(consolePanel, editorManager, errorMarker);
		debugRunner       = new DebugRunner(consolePanel, editorManager, errorMarker, projectRunner);
		gitManager        = new GitManager(this, consolePanel);
		updateManager     = new UpdateManager(this, consolePanel, TIDEProperties.APP_VERSION, TIDEProperties.GITHUB_REPO);
		aboutDialog       = new AboutDialog(this, TIDEProperties.APP_VERSION, TIDEProperties.GITHUB_REPO, updateManager);
		fileTreePanel     = new FileTreePanel(this, consolePanel, file -> editorManager.openFileInEditor(file));

		gitManager.setOnRefreshFileTree(() -> { fileTreePanel.updateFileTree(currentProjectFolder); revalidate(); repaint(); });
		gitManager.setOnSaveCurrentFile(() -> editorManager.saveCurrentFile());
		projectRunner.setOnRefreshFileTree(() -> fileTreePanel.updateFileTree(currentProjectFolder));
		outlinePanel = new OutlinePanel();
		editorManager.setOutlinePanel(outlinePanel);

		settingsDialog    = new SettingsDialog(this, editorManager, () -> {
				// Das sorgt dafür, dass sich die Texte im Hauptfenster direkt anpassen
				btnOpen.setText(LanguageManager.t("open"));
				btnSave.setText(LanguageManager.t("save"));
				btnClear.setText(LanguageManager.t("clear"));
				btnAbout.setText(LanguageManager.t("about"));
				modeLabel.setText(LanguageManager.t("mode"));
				mainClassLabel.setText(LanguageManager.t("main"));
				btnFormat.setText(LanguageManager.t("format"));
				revalidate();
				repaint();
			});
	}

	private void initUI() {
		searchPanel = new SearchPanel(editorTabs, consolePanel);
		editorTabs.setBorder(null);

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setBorder(new EmptyBorder(8, 10, 8, 10));
		toolBar.setBackground(new Color(43, 45, 48));

		// ---- Buttons ----
		btnOpen  = new JButton(LanguageManager.t("open"));
		btnSave  = new JButton(LanguageManager.t("save"));

		btnTerminate = new JButton("X");
		btnTerminate.setForeground(new Color(230, 75, 75));
		btnTerminate.setFont(btnTerminate.getFont().deriveFont(Font.BOLD, 14f));
		btnTerminate.setVisible(false);

		modeSelector = new JComboBox<>(new String[]{MODE_JAVA, MODE_PYTHON, MODE_C, MODE_CPP, MODE_BATCH});
		modeSelector.setPreferredSize(new Dimension(90, 28));
		modeSelector.setMaximumSize(new Dimension(90, 28));

		mainClassLabel = new JLabel(" Main-Class: ");
		mainClassInput = new JTextField("Main", 10);
		mainClassInput.setPreferredSize(new Dimension(130, 28));
		mainClassInput.setMaximumSize(new Dimension(130, 28));

		// Run-Modus Dropdown: Standard / Debug
		runModeSelector = new JComboBox<>(new String[]{RUN_MODE_STANDARD, RUN_MODE_DEBUG});
		runModeSelector.setPreferredSize(new Dimension(100, 28));
		runModeSelector.setMaximumSize(new Dimension(100, 28));
		runModeSelector.setToolTipText("Standard = normal ausführen | Debug = mit Debugger starten");

		// Run-Button
		btnRun = new JButton("▶");
		btnRun.setForeground(new Color(80, 200, 120));
		btnRun.setFont(btnRun.getFont().deriveFont(Font.BOLD));

		// Formatier Knopf	
		btnFormat = new JButton(LanguageManager.t("format"));
		btnFormat.setForeground(new Color(255, 200, 80));

		// HotSwap-Button – nur sichtbar während Java-Debug läuft
		btnHotSwap = new JButton("HotSwap");
		btnHotSwap.setForeground(new Color(255, 220, 50));
		btnHotSwap.setFont(btnHotSwap.getFont().deriveFont(Font.BOLD));
		btnHotSwap.setToolTipText("Klassen im laufenden Prozess ersetzen (kein Neustart)");
		btnHotSwap.setVisible(false);

		btnTBuild = new JButton("T-Build");
		btnClear  = new JButton(LanguageManager.t("clear"));
		btnAbout  = new JButton(LanguageManager.t("about"));

		btnTBuild.setForeground(new Color(100, 150, 255));
		btnAbout.setForeground(new Color(180, 180, 180));

		JButton btnSettings = new JButton("⚙");
		btnSettings.addActionListener(e -> settingsDialog.show());


		// Outlines der Buttons
		Border outline = new LineBorder(Color.DARK_GRAY, 2); 
		Border padding = BorderFactory.createEmptyBorder(5, 10, 5, 10); 
		Border compoundBorder = BorderFactory.createCompoundBorder(outline, padding);

		Color dezentHintergrund = new Color(55, 58, 62); 
		Color hoverHintergrund   = new Color(75, 78, 82); 

		JButton[] borderButtons = {btnFormat, btnOpen, btnSave, btnTBuild, btnAbout, btnSettings, btnHotSwap, btnClear};
		for (JButton btn : borderButtons) {
			btn.setBorder(compoundBorder);

			// Hintergrundfarbe zuweisen
			btn.setBackground(dezentHintergrund);
			btn.setContentAreaFilled(true);
			btn.setOpaque(true);

			// Hover-Effekt hinzufügen
			btn.addMouseListener(new java.awt.event.MouseAdapter() {
					@Override
					public void mouseEntered(java.awt.event.MouseEvent e) {
						btn.setBackground(hoverHintergrund);
					}

					@Override
					public void mouseExited(java.awt.event.MouseEvent e) {
						btn.setBackground(dezentHintergrund);
					}
				});
		}



		// Git-Dropdown
		JMenuBar gitMenuBar = new JMenuBar();
		gitMenuBar.setOpaque(false);
		gitMenuBar.setBorder(null);
		JMenu gitMenu = new JMenu("Git ▾");
		gitMenu.setForeground(new Color(255, 200, 80));
		gitMenu.setFont(gitMenu.getFont().deriveFont(Font.BOLD));
		gitMenu.setBackground(new Color(43, 45, 48));
		JMenuItem gitCommit = new JMenuItem("Commit");
		JMenuItem gitPush   = new JMenuItem("Push");
		JMenuItem gitPull   = new JMenuItem("Pull");
		gitMenu.add(gitCommit); gitMenu.add(gitPush); gitMenu.add(gitPull);
		gitCommit.addActionListener(e -> gitManager.gitCommit());
		gitPush.addActionListener(e   -> gitManager.gitPush());
		gitPull.addActionListener(e   -> gitManager.gitPull());
		gitMenuBar.add(gitMenu);

		// Language selector
		JComboBox<LanguageManager.Language> langSel = new JComboBox<>(LanguageManager.Language.values());
		langSel.setPreferredSize(new Dimension(95, 28));
		langSel.setMaximumSize(new Dimension(95, 28));
		langSel.setToolTipText("Language / Sprache");
		langSel.addActionListener(e -> {
				LanguageManager.Language selected = (LanguageManager.Language) langSel.getSelectedItem();
				LanguageManager.set(selected);
				TIDEPreferences.saveLanguage(selected.name());

				Locale neueLocale = selected.name().equalsIgnoreCase("DE") ? Locale.GERMAN : Locale.ENGLISH;
				Locale.setDefault(neueLocale);
				JComponent.setDefaultLocale(neueLocale); 

				// Bestehende Editoren updaten
				if (editorManager != null) {
					editorManager.updateUIWithLocale(neueLocale);
				}

				// Texte der Hauptkomponenten updaten
				btnOpen.setText(LanguageManager.t("open"));
				btnSave.setText(LanguageManager.t("save"));
				btnClear.setText(LanguageManager.t("clear"));
				btnAbout.setText(LanguageManager.t("about"));
				modeLabel.setText(LanguageManager.t("mode"));
				mainClassLabel.setText(LanguageManager.t("main"));
				btnFormat.setText(LanguageManager.t("format"));


				revalidate();
				repaint();
			});

		// ---- Toolbar befüllen ----
		toolBar.add(btnOpen);
		toolBar.add(Box.createHorizontalStrut(5));
		toolBar.add(btnSave);
		toolBar.addSeparator(new Dimension(20, 30));
		modeLabel = new JLabel(LanguageManager.t("mode"));
		toolBar.add(modeLabel);
		toolBar.add(modeSelector);
		toolBar.add(Box.createHorizontalStrut(5));
		toolBar.add(mainClassLabel);
		toolBar.add(mainClassInput);
		toolBar.addSeparator(new Dimension(20, 30));

		// Run-Modus Dropdown + ein Run-Button
		toolBar.add(runModeSelector);
		toolBar.add(Box.createHorizontalStrut(5));
		toolBar.add(btnRun);
		toolBar.add(Box.createHorizontalStrut(5));
		toolBar.add(btnTerminate);
		toolBar.add(Box.createHorizontalStrut(5));
		toolBar.add(btnHotSwap);

		toolBar.add(Box.createHorizontalGlue());

		toolBar.add(btnFormat);
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
		toolBar.add(Box.createHorizontalStrut(5));
		toolBar.add(btnSettings);

		add(toolBar, BorderLayout.NORTH);

		// FileTree MouseListener
		fileTreePanel.getViewport().getView().addMouseListener(new MouseAdapter() {
				@Override public void mousePressed(MouseEvent me) {
					if (me.isPopupTrigger()) fileTreePanel.showFileTreePopup(me, currentProjectFolder, null);
				}
				@Override public void mouseReleased(MouseEvent me) {
					if (me.isPopupTrigger()) fileTreePanel.showFileTreePopup(me, currentProjectFolder, null);
				}
			});

		// Layout
		JPanel editorContainer = new JPanel(new BorderLayout());
		globalSearchPanel = new GlobalSearchPanel(editorManager, editorTabs);
		editorContainer.add(globalSearchPanel, BorderLayout.SOUTH);
		editorContainer.add(searchPanel, BorderLayout.NORTH);
		editorContainer.add(editorTabs,  BorderLayout.CENTER);

		verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorContainer, consolePanel);
		verticalSplit.setResizeWeight(0.7);
		verticalSplit.setDividerSize(4);
		verticalSplit.setBorder(null);
		verticalSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
				if (verticalSplit.getHeight() > 0)
				TIDEPreferences.saveDividerVProportion(
					verticalSplit.getDividerLocation() / (double) verticalSplit.getHeight());
			});

		// ← NEU: Editor+Console | Outline
		editorOutlineSplit = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT, verticalSplit, outlinePanel);
		editorOutlineSplit.setResizeWeight(1.0); 
		editorOutlineSplit.setDividerSize(4);
		editorOutlineSplit.setBorder(null);
		editorOutlineSplit.setDividerLocation(0.8);  // 80% Editor, 20% Outline

		editorOutlineSplit.setOpaque(false);
		editorOutlineSplit.setBackground(new Color(0, 0, 0, 0));

		horizontalSplit = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT, fileTreePanel, editorOutlineSplit); 
		horizontalSplit.setDividerSize(4);
		horizontalSplit.setBorder(null);
		horizontalSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
				if (horizontalSplit.getWidth() > 0)
				TIDEPreferences.saveDividerHProportion(
					horizontalSplit.getDividerLocation() / (double) horizontalSplit.getWidth());
			});

		editorOutlineSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
				int total = editorOutlineSplit.getWidth();
				int divider = editorOutlineSplit.getDividerLocation();
				if (total > 0 && divider > 0) {
					TIDEPreferences.saveOutlineWidth(total - divider);
				}
			});

		add(horizontalSplit, BorderLayout.CENTER);

		// ---- Event Listeners ----
		btnOpen.addActionListener(e  -> openFolderDialog());
		btnSave.addActionListener(e  -> editorManager.saveCurrentFile());
		btnClear.addActionListener(e -> consolePanel.clear());
		btnTBuild.addActionListener(e -> projectRunner.handleTBuild());
		btnFormat.addActionListener(e -> editorManager.formatCurrentFile());
		btnAbout.addActionListener(e  -> aboutDialog.show());

		btnTerminate.addActionListener(e -> {
				projectRunner.stopRunningProcess();
				debugRunner.stopDebugProcess();
			});

		// Ein Run-Button – Verhalten hängt vom Dropdown ab
		btnRun.addActionListener(e -> {
				String mode      = (String) modeSelector.getSelectedItem();
				String mainClass = mainClassInput.getText().trim();
				String runMode   = (String) runModeSelector.getSelectedItem();

				if (RUN_MODE_DEBUG.equals(runMode)) {
					debugRunner.startDebug(mainClass);
				} else {
					projectRunner.runProject(mode, mainClass);
				}
			});

		// HotSwap-Button: Klassen ersetzen ohne Neustart
		btnHotSwap.addActionListener(e -> debugRunner.hotSwap());

		modeSelector.addActionListener(e -> updateDynamicUI());

		consolePanel.getTerminalInput().addKeyListener(new KeyAdapter() {
				@Override public void keyPressed(KeyEvent e) {
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
				if (e.getID() != KeyEvent.KEY_PRESSED) return false;

				// aktuellen Modifier-Status zusammenbauen
				int mod = 0;
				if (e.isControlDown()) mod |= InputEvent.CTRL_DOWN_MASK;
				if (e.isShiftDown())   mod |= InputEvent.SHIFT_DOWN_MASK;
				if (e.isAltDown())     mod |= InputEvent.ALT_DOWN_MASK;

				int key = e.getKeyCode();

				// Speichern
				if (key == TIDEPreferences.getHotkey("save", KeyEvent.VK_S)
					&& mod == TIDEPreferences.getHotkeyModifier("save", InputEvent.CTRL_DOWN_MASK)) {
					editorManager.saveCurrentFile();
					return true;
				}

				// Lokale Suche
				if (key == TIDEPreferences.getHotkey("search", KeyEvent.VK_F)
					&& mod == TIDEPreferences.getHotkeyModifier("search", InputEvent.CTRL_DOWN_MASK)) {
					searchPanel.setVisible(!searchPanel.isVisible());
					if (searchPanel.isVisible()) searchPanel.getSearchField().requestFocusInWindow();
					revalidate();
					return true;
				}

				// Globale Suche
				if (key == TIDEPreferences.getHotkey("gsearch", KeyEvent.VK_F)
					&& mod == TIDEPreferences.getHotkeyModifier("gsearch",
						InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) {
					globalSearchPanel.setVisible(!globalSearchPanel.isVisible());
					if (globalSearchPanel.isVisible()) globalSearchPanel.getSearchField().requestFocusInWindow();
					revalidate();
					return true;
				}

				// Ausführen
				if (key == TIDEPreferences.getHotkey("run", KeyEvent.VK_R)
					&& mod == TIDEPreferences.getHotkeyModifier("run", InputEvent.CTRL_DOWN_MASK)) {
					runModeSelector.setSelectedItem(RUN_MODE_STANDARD);
					projectRunner.runProject((String) modeSelector.getSelectedItem(),
						mainClassInput.getText().trim());
					return true;
				}

				// Debuggen
				if (key == TIDEPreferences.getHotkey("debug", KeyEvent.VK_R)
					&& mod == TIDEPreferences.getHotkeyModifier("debug",
						InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) {
					runModeSelector.setSelectedItem(RUN_MODE_DEBUG);
					debugRunner.startDebug(mainClassInput.getText().trim());
					return true;
				}

				return false;
			});





		addWindowListener(new WindowAdapter() {
				@Override public void windowClosing(WindowEvent e) {
					TIDEPreferences.saveWindowWidth(getWidth());
					TIDEPreferences.saveWindowHeight(getHeight());
					if (currentProjectFolder != null)
					TIDEPreferences.saveLastFolder(currentProjectFolder.getAbsolutePath());
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
			globalSearchPanel.setSubsystems(currentProjectFolder);
		}

		modeSelector.setSelectedItem(TIDEPreferences.getMode());

		projectRunner.setTerminateButton(btnTerminate);
		debugRunner.setTerminateButton(btnTerminate);
		debugRunner.setHotSwapButton(btnHotSwap);

		updateDynamicUI();

		// Outline bei Tab-Wechsel aktualisieren
		editorTabs.addChangeListener(e -> {
				Component tab = editorTabs.getSelectedComponent();
				if (tab instanceof org.fife.ui.rtextarea.RTextScrollPane sp) {
					Component view = sp.getViewport().getView();
					if (view instanceof org.fife.ui.rsyntaxtextarea.RSyntaxTextArea textArea) {
						File f = openFiles.get(tab);
						outlinePanel.refresh(textArea, f != null ? f.getName() : null);
					}
				} else {
					outlinePanel.refresh(null, null);
				}
			});

		// Alle JScrollPanes im Fenster smooth machen
		for (Component c : getAllComponents(this)) {
			if (c instanceof JScrollPane sp) {
				sp.getVerticalScrollBar().setUnitIncrement(16);
				sp.getHorizontalScrollBar().setUnitIncrement(16);
			}
		}

		for (Component c : getAllComponents(this)) {
			if (c instanceof JScrollPane sp) {
				enableSmoothScrolling(sp);
			}
		}
	}

	private java.util.List<Component> getAllComponents(Container container) {
		java.util.List<Component> list = new java.util.ArrayList<>();
		for (Component c : container.getComponents()) {
			list.add(c);
			if (c instanceof Container) list.addAll(getAllComponents((Container) c));
		}
		return list;
	}

	public void enableSmoothScrolling(JScrollPane scrollPane) {
		JScrollBar bar = scrollPane.getVerticalScrollBar();
		Timer timer = new Timer(16, null);

		timer.addActionListener(e -> {
			// ── 1. KNACKIGER STOPP ──────────────────────────────────────────
			if (Math.abs(currentVelocity) < 1.5) {
				currentVelocity = 0.0;
				
				// Beim echten Stopp fadet der Effekt nun organisch aus, statt gelöscht zu werden
				dynIntensity *= 0.70f;
				if (dynIntensity < 0.01f) {
					dynIntensity = 0.0f;
					timer.stop();
				}
				scrollPane.repaint();
				return;
			}

			double current = bar.getValue();
			double nextPos = current + currentVelocity;

			int max = bar.getMaximum() - bar.getVisibleAmount();
			if (nextPos < 0) {
				nextPos = 0;
				currentVelocity = 0.0; 
			} else if (nextPos > max) {
				nextPos = max;
				currentVelocity = 0.0;
			}

			bar.setValue((int) Math.round(nextPos));

			// Knackige Reibung für das Scrollen (35% Verlust pro Frame)
			currentVelocity *= 0.65;

			if (currentVelocity > 0.1) scrollDir = 1;
			else if (currentVelocity < -0.1) scrollDir = -1;

			// ── 2. ORGANISCHER EFFEKT-SPEICHER ──────────────────────────────
			// Wir berechnen die Wunsch-Intensität basierend auf dem aktuellen Tempo
			float targetIntensity = (float) Math.min(Math.abs(currentVelocity) / 45.0, 1.0);
			
			// Anstatt dynIntensity hart zu setzen, gleitet sie jetzt langsam zum Ziel!
			// Wenn targetIntensity höher ist, lädt sie sich auf. 
			// Wenn targetIntensity in deinen Dreh-Pausen kurz absinkt, hält dynIntensity die Stellung!
			if (targetIntensity > dynIntensity) {
				dynIntensity += (targetIntensity - dynIntensity) * 0.20f; // Sanfter Aufbau (Trägheit)
			} else {
				dynIntensity += (targetIntensity - dynIntensity) * 0.10f; // Noch langsameres Abklingen bei Pausen!
			}

			scrollPane.repaint();
		});

		scrollPane.addMouseWheelListener(e -> {
			e.consume(); 
			int rotation = e.getWheelRotation();
			int increment = bar.getUnitIncrement() > 0 ? bar.getUnitIncrement() : 16;

			double push = rotation * increment * 4.5;
			currentVelocity += push;

			double maxSpeed = 80.0;
			if (currentVelocity > maxSpeed)  currentVelocity = maxSpeed;
			if (currentVelocity < -maxSpeed) currentVelocity = -maxSpeed;

			if (!timer.isRunning()) timer.start();
		});

		bar.setUnitIncrement(16);
	}

	private void openFolderDialog() {
		JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			currentProjectFolder = chooser.getSelectedFile();
			fileTreePanel.updateFileTree(currentProjectFolder);
			consolePanel.log("[INFO] Ordner geöffnet: " + currentProjectFolder.getName() + "\n", Color.CYAN);
			loadTXml(currentProjectFolder);
			projectRunner.setCurrentProjectFolder(currentProjectFolder);
			debugRunner.setCurrentProjectFolder(currentProjectFolder);
			gitManager.setCurrentProjectFolder(currentProjectFolder);
			gitManager.checkGitStatusOnOpen();
			globalSearchPanel.setSubsystems(currentProjectFolder);
		}
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


	private void loadTXml(File folder) {
		File txml = new File(folder, "T.xml");
		if (!txml.exists()) {
			consolePanel.log("[WARNUNG] Keine T.xml im Projektordner gefunden.\n", Color.ORANGE);
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
			setTitle("TIDE v" + TIDEProperties.APP_VERSION + " - " + titleAppName
				+ (titleVersion.isEmpty() ? "" : " v" + titleVersion));
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