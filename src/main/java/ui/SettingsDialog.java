package ui;

import config.LanguageManager;
import config.TIDEPreferences;
import config.TIDEProperties;
import editor.EditorManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Locale;

import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.HashMap;


public class SettingsDialog {

	private final JFrame parent;
	private final EditorManager editorManager;
	private final Runnable onLanguageChanged;

	public SettingsDialog(JFrame parent, EditorManager editorManager, Runnable onLanguageChanged) {
		this.parent            = parent;
		this.editorManager     = editorManager;
		this.onLanguageChanged = onLanguageChanged;
	}

	public void show() {
		JDialog dialog = new JDialog(parent, "Einstellungen / Settings", true);
		dialog.setSize(600, 700);
		dialog.setLocationRelativeTo(parent);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.getContentPane().setBackground(new Color(43, 45, 48));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(new Color(43, 45, 48));
		content.setBorder(new EmptyBorder(15, 20, 10, 20));

		// ── Sprache ──────────────────────────────────────────────
		JPanel langPanel = createSection("Sprache / Language");

		JComboBox<LanguageManager.Language> langBox =
		new JComboBox<>(LanguageManager.Language.values());
		langBox.setSelectedItem(LanguageManager.Language.valueOf(TIDEPreferences.getLanguage()));
		langBox.setMaximumSize(new Dimension(200, 28));
		langBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		langPanel.add(langBox);
		content.add(langPanel);
		content.add(Box.createVerticalStrut(12));


		// ── Theme ────────────────────────────────────────────────────
		JPanel themePanel = createSection("Theme", 90);

		String[] themeNames = java.util.Arrays.stream(config.Theme.ALL)
		.map(t -> t.name).toArray(String[]::new);
		JComboBox<String> themeBox = new JComboBox<>(themeNames);
		themeBox.setSelectedItem(TIDEPreferences.getTheme());
		themeBox.setMaximumSize(new Dimension(200, 28));
		themeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		themeBox.setBackground(new Color(55, 58, 62));
		themeBox.setForeground(Color.WHITE);

		JLabel themeHint = new JLabel(
			"<html><font color='#C8C8C8'>Änderung wirkt nach Neustart von TIDE.</font></html>");
		themeHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		themeHint.setAlignmentX(Component.LEFT_ALIGNMENT);

		themePanel.add(themeBox);
		themePanel.add(Box.createVerticalStrut(4));
		themePanel.add(themeHint);
		content.add(themePanel);
		content.add(Box.createVerticalStrut(12));

		// ── Schriftgröße ─────────────────────────────────────────
		JPanel fontPanel = createSection("Editor-Schriftgröße / Font Size");

		int currentSize = TIDEPreferences.getEditorFontSize();
		JLabel fontSizeLabel = new JLabel(currentSize + " pt");
		fontSizeLabel.setForeground(new Color(200, 200, 200));
		fontSizeLabel.setFont(new Font("Consolas", Font.PLAIN, 13));

		JSlider fontSlider = new JSlider(8, 28, currentSize);
		fontSlider.setBackground(new Color(43, 45, 48));
		fontSlider.setForeground(new Color(200, 200, 200));
		fontSlider.setMajorTickSpacing(4);
		fontSlider.setMinorTickSpacing(1);
		fontSlider.setPaintTicks(true);
		fontSlider.setPaintLabels(true);
		fontSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
		fontSlider.setAlignmentX(Component.LEFT_ALIGNMENT);

		fontSlider.addChangeListener(e -> fontSizeLabel.setText(fontSlider.getValue() + " pt"));

		JPanel fontRow = new JPanel(new BorderLayout(8, 0));
		fontRow.setBackground(new Color(43, 45, 48));
		fontRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
		fontRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		fontRow.add(fontSlider, BorderLayout.CENTER);
		fontRow.add(fontSizeLabel, BorderLayout.EAST);

		fontPanel.add(fontRow);
		content.add(fontPanel);
		content.add(Box.createVerticalStrut(12));

		// -- Auto stop --------------------------------------------
		JPanel auStPanel = createSection("Stop when to many resources are used");

		JCheckBox auStBox = new JCheckBox(
			"Auto Stop",
			TIDEPreferences.getAuSt());

		auStPanel.add(auStBox);
		content.add(auStPanel);
		content.add(Box.createVerticalStrut(12));

		// ── Autocomplete ─────────────────────────────────────────
		JPanel acPanel = createSection("Autocomplete");

		int currentDelay = TIDEPreferences.getAutocompleteDelay();
		JLabel acLabel = new JLabel("Verzögerung: " + currentDelay + " ms");
		acLabel.setForeground(new Color(200, 200, 200));
		acLabel.setFont(new Font("Consolas", Font.PLAIN, 13));

		JSlider acSlider = new JSlider(0, 1000, currentDelay);
		acSlider.setBackground(new Color(43, 45, 48));
		acSlider.setForeground(new Color(200, 200, 200));
		acSlider.setMajorTickSpacing(250);
		acSlider.setPaintTicks(true);
		acSlider.setPaintLabels(true);
		acSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
		acSlider.setAlignmentX(Component.LEFT_ALIGNMENT);

		acSlider.addChangeListener(e -> acLabel.setText("Verzögerung: " + acSlider.getValue() + " ms"));

		JPanel acRow = new JPanel(new BorderLayout(8, 0));
		acRow.setBackground(new Color(43, 45, 48));
		acRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
		acRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		acRow.add(acSlider, BorderLayout.CENTER);
		acRow.add(acLabel, BorderLayout.EAST);

		acPanel.add(acRow);
		content.add(acPanel);
		content.add(Box.createVerticalStrut(12));

		// ── Visuell ───────────────────────────────────────────────
		JPanel visualPanel = createSection("Visuell / Visual", 210);

		JCheckBox motionBlurBox = new JCheckBox(
			"Scroll-Effekte aktiviert (Motion Blur & Stretch)",
			TIDEPreferences.getMotionBlurEnabled());
		motionBlurBox.setBackground(new Color(43, 45, 48));
		motionBlurBox.setForeground(new Color(200, 200, 200));
		motionBlurBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		motionBlurBox.setToolTipText("Motion Blur und Stretch-Effekt beim Scrollen ein-/ausschalten");
		visualPanel.add(motionBlurBox);

		// Scroll FPS Dropdown
		JPanel fpsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
		fpsPanel.setBackground(new Color(43, 45, 48));

		JLabel lblFps = new JLabel("Scroll Animations-FPS: ");
		lblFps.setForeground(new Color(220, 220, 220));
		lblFps.setFont(new Font("Segoe UI", Font.PLAIN, 14));

		Integer[] fpsOptions = { 60, 90, 144, 240 };
		JComboBox<Integer> cbFps = new JComboBox<>(fpsOptions);
		cbFps.setSelectedItem(TIDEPreferences.getScrollFPS());
		cbFps.setBackground(new Color(60, 63, 65));
		cbFps.setForeground(Color.WHITE);

		fpsPanel.add(lblFps);
		fpsPanel.add(cbFps);
		visualPanel.add(fpsPanel);

		// Scroll-Geschwindigkeit Slider
		JPanel speedPanel = new JPanel(new BorderLayout(10, 0));
		speedPanel.setBackground(new Color(43, 45, 48));
		speedPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
		speedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel lblSpeedDesc = new JLabel("Scroll-Geschwindigkeit: ");
		lblSpeedDesc.setForeground(new Color(220, 220, 220));
		lblSpeedDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));

		int currentSpeed = TIDEPreferences.getScrollSpeed();
		JLabel speedValueLabel = new JLabel(currentSpeed + "%");
		speedValueLabel.setForeground(new Color(200, 200, 200));
		speedValueLabel.setFont(new Font("Consolas", Font.PLAIN, 13));
		speedValueLabel.setPreferredSize(new Dimension(45, 20));

		JSlider speedSlider = new JSlider(10, 250, currentSpeed);
		speedSlider.setBackground(new Color(43, 45, 48));
		speedSlider.setForeground(new Color(200, 200, 200));
		speedSlider.setMajorTickSpacing(40);
		speedSlider.setPaintTicks(true);
		speedSlider.addChangeListener(e -> speedValueLabel.setText(speedSlider.getValue() + "%"));

		JPanel speedControls = new JPanel(new BorderLayout(5, 0));
		speedControls.setBackground(new Color(43, 45, 48));
		speedControls.add(speedSlider, BorderLayout.CENTER);
		speedControls.add(speedValueLabel, BorderLayout.EAST);

		speedPanel.add(lblSpeedDesc, BorderLayout.WEST);
		speedPanel.add(speedControls, BorderLayout.CENTER);

		visualPanel.add(Box.createVerticalStrut(8));
		visualPanel.add(speedPanel);

		JLabel restartHint = new JLabel(
			"<html><font color='#C8C8C8'>Hinweis: Nach Änderung der Scroll-FPS wird ein Neustart empfohlen.</font></html>");
		restartHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		restartHint.setAlignmentX(Component.LEFT_ALIGNMENT);
		visualPanel.add(Box.createVerticalStrut(4));
		visualPanel.add(restartHint);

		content.add(visualPanel);
		content.add(Box.createVerticalStrut(12));

		// ── Rendering / Hardwarebeschleunigung ────────────────────
		JPanel hwPanel = createSection("Rendering / Hardwarebeschleunigung", 90);

		JPanel hwRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
		hwRow.setBackground(new Color(43, 45, 48));

		JLabel lblHw = new JLabel("Hardwarebeschleunigung: ");
		lblHw.setForeground(new Color(220, 220, 220));
		lblHw.setFont(new Font("Segoe UI", Font.PLAIN, 14));

		// Reihenfolge muss mit hwOptionKeys uebereinstimmen
		String[] hwOptions   = { "Automatisch", "Immer AN", "Immer AUS" };
		String[] hwOptionKeys = { "auto", "on", "off" };

		JComboBox<String> hwBox = new JComboBox<>(hwOptions);
		String currentHw = TIDEPreferences.getHwAccelMode();
		for (int i = 0; i < hwOptionKeys.length; i++) {
			if (hwOptionKeys[i].equals(currentHw)) {
				hwBox.setSelectedIndex(i);
				break;
			}
		}
		hwBox.setBackground(new Color(60, 63, 65));
		hwBox.setForeground(Color.WHITE);
		hwBox.setMaximumSize(new Dimension(160, 28));

		hwRow.add(lblHw);
		hwRow.add(hwBox);
		hwPanel.add(hwRow);

		JLabel hwHint = new JLabel(
			"<html><font color='#C8C8C8'>Automatisch: AN bei installierter App, AUS beim JAR-Start.<br>" +
			"Änderung wirkt nach Neustart von TIDE.</font></html>");
		hwHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		hwHint.setAlignmentX(Component.LEFT_ALIGNMENT);
		hwPanel.add(Box.createVerticalStrut(4));
		hwPanel.add(hwHint);

		content.add(hwPanel);
		content.add(Box.createVerticalStrut(12));

		// ── Konsole ───────────────────────────────────────────────
		JPanel consolePanel = createSection("Konsole / Console");

		JCheckBox autoScrollBox = new JCheckBox(
			"Auto-Scroll aktiviert",
			TIDEPreferences.getConsoleAutoScroll());
		autoScrollBox.setBackground(new Color(43, 45, 48));
		autoScrollBox.setForeground(new Color(200, 200, 200));
		autoScrollBox.setAlignmentX(Component.LEFT_ALIGNMENT);

		consolePanel.add(autoScrollBox);
		content.add(consolePanel);
		content.add(Box.createVerticalStrut(12));

		// ── Hotkeys ───────────────────────────────────────────────
		JPanel hotkeyPanel = createSection("Hotkeys", 230);

		Object[][] hotkeyDefs = {
			{ "save",    "Speichern",      KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK },
			{ "stop",    "Stop",     KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK },
			{ "search",  "Lokale Suche",   KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK },
			{ "gsearch", "Globale Suche",  KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK },
			{ "run",     "Ausführen",      KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK },
			{ "debug",   "Debuggen",       KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK },
		};

		Map<String, Integer> pendingHotkeys   = new HashMap<>();
		Map<String, Integer> pendingModifiers = new HashMap<>();
		JPanel tablePanel = new JPanel(new GridLayout(hotkeyDefs.length, 2, 10, 6));
		tablePanel.setBackground(new Color(43, 45, 48));
		tablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		for (Object[] def : hotkeyDefs) {
			String action     = (String)  def[0];
			String label      = (String)  def[1];
			int    defaultKey = (Integer) def[2];
			int    defaultMod = (Integer) def[3];
			int    savedKey   = TIDEPreferences.getHotkey(action, defaultKey);
			int    savedMod   = TIDEPreferences.getHotkeyModifier(action, defaultMod);

			JLabel descLabel = new JLabel(label);
			descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
			descLabel.setForeground(new Color(200, 200, 200));

			String initModText = "";
			if ((savedMod & InputEvent.CTRL_DOWN_MASK)  != 0) initModText += "Strg+";
			if ((savedMod & InputEvent.SHIFT_DOWN_MASK) != 0) initModText += "Shift+";
			if ((savedMod & InputEvent.ALT_DOWN_MASK)   != 0) initModText += "Alt+";

			JTextField keyField = new JTextField(initModText + KeyEvent.getKeyText(savedKey));
			keyField.setFont(new Font("Consolas", Font.BOLD, 12));
			keyField.setForeground(new Color(255, 200, 80));
			keyField.setBackground(new Color(55, 58, 62));
			keyField.setCaretColor(new Color(255, 200, 80));
			keyField.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(new Color(80, 80, 80)),
					BorderFactory.createEmptyBorder(2, 6, 2, 6)));
			keyField.setEditable(false);
			keyField.setMaximumSize(new Dimension(80, 26));

			keyField.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						int code = e.getKeyCode();
						if (code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_SHIFT
							|| code == KeyEvent.VK_ALT || code == KeyEvent.VK_META) return;

						int mod = 0;
						if (e.isControlDown()) mod |= InputEvent.CTRL_DOWN_MASK;
						if (e.isShiftDown())   mod |= InputEvent.SHIFT_DOWN_MASK;
						if (e.isAltDown())     mod |= InputEvent.ALT_DOWN_MASK;

						String modText = "";
						if (e.isControlDown()) modText += "Strg+";
						if (e.isShiftDown())   modText += "Shift+";
						if (e.isAltDown())     modText += "Alt+";

						keyField.setText(modText + KeyEvent.getKeyText(code));
						keyField.setForeground(new Color(80, 200, 120));

						pendingHotkeys.put(action, code);
						pendingModifiers.put(action, mod);
					}
				});

			keyField.addFocusListener(new java.awt.event.FocusAdapter() {
					@Override public void focusGained(java.awt.event.FocusEvent e) {
						keyField.setBorder(BorderFactory.createCompoundBorder(
								BorderFactory.createLineBorder(new Color(80, 200, 120)),
								BorderFactory.createEmptyBorder(2, 6, 2, 6)));
					}
					@Override public void focusLost(java.awt.event.FocusEvent e) {
						keyField.setBorder(BorderFactory.createCompoundBorder(
								BorderFactory.createLineBorder(new Color(80, 80, 80)),
								BorderFactory.createEmptyBorder(2, 6, 2, 6)));
					}
				});

			tablePanel.add(descLabel);
			tablePanel.add(keyField);
		}

		hotkeyPanel.add(tablePanel);
		content.add(hotkeyPanel);
		content.add(Box.createVerticalStrut(12));

		// ── Buttons ───────────────────────────────────────────────
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
		btnPanel.setBackground(new Color(43, 45, 48));

		JButton btnApply  = new JButton("Übernehmen");
		JButton btnCancel = new JButton("Abbrechen");

		btnApply.setForeground(new Color(80, 200, 120));
		btnCancel.setForeground(new Color(180, 180, 180));

		btnCancel.addActionListener(e -> dialog.dispose());

		btnApply.addActionListener(e -> {
				// Sprache
				LanguageManager.Language selectedLang =
				(LanguageManager.Language) langBox.getSelectedItem();
				LanguageManager.set(selectedLang);
				TIDEPreferences.saveLanguage(selectedLang.name());
				Locale locale = selectedLang.name().equals("DE") ? Locale.GERMAN : Locale.ENGLISH;
				Locale.setDefault(locale);
				JComponent.setDefaultLocale(locale);
				if (editorManager != null) editorManager.updateUIWithLocale(locale);

				// Schriftgröße
				int newSize = fontSlider.getValue();
				TIDEPreferences.saveEditorFontSize(newSize);
				if (editorManager != null) editorManager.applyFontSizeToAllEditors(newSize);

				// Theme speichern
				TIDEPreferences.saveTheme((String) themeBox.getSelectedItem());

				// Autocomplete-Delay
				TIDEPreferences.saveAutocompleteDelay(acSlider.getValue());

				TIDEPreferences.saveAuSt(auStBox.isSelected());

				// Motion Blur & Scroll
				TIDEPreferences.saveMotionBlurEnabled(motionBlurBox.isSelected());
				TIDEPreferences.saveScrollSpeed(speedSlider.getValue());
				TIDEPreferences.saveScrollFPS((int) cbFps.getSelectedItem());

				// Hardwarebeschleunigung
				TIDEPreferences.saveHwAccelMode(hwOptionKeys[hwBox.getSelectedIndex()]);

				// Auto-Scroll
				TIDEPreferences.saveConsoleAutoScroll(autoScrollBox.isSelected());

				// Hotkeys
				for (Map.Entry<String, Integer> entry : pendingHotkeys.entrySet()) {
					TIDEPreferences.saveHotkey(entry.getKey(), entry.getValue());
				}
				for (Map.Entry<String, Integer> entry : pendingModifiers.entrySet()) {
					TIDEPreferences.saveHotkeyModifier(entry.getKey(), entry.getValue());
				}

				if (onLanguageChanged != null) onLanguageChanged.run();

				dialog.dispose();
			});

		btnPanel.add(btnCancel);
		btnPanel.add(btnApply);

		JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setBorder(null);
		scrollPane.getViewport().setBackground(new Color(43, 45, 48));

		int baseIncrement = 16;
		double multiplier = TIDEPreferences.getScrollSpeed() / 100.0;
		scrollPane.getVerticalScrollBar().setUnitIncrement((int)(baseIncrement * multiplier));

		dialog.add(scrollPane, BorderLayout.CENTER);
		dialog.add(btnPanel,   BorderLayout.SOUTH);
		dialog.setVisible(true);
	}

	private JPanel createSection(String title, int height) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(new Color(43, 45, 48));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

		TitledBorder border = BorderFactory.createTitledBorder(
			BorderFactory.createLineBorder(new Color(80, 80, 80), 1), title);
		border.setTitleColor(new Color(180, 180, 180));
		border.setTitleFont(new Font("Segoe UI", Font.PLAIN, 12));
		panel.setBorder(BorderFactory.createCompoundBorder(
				border, new EmptyBorder(6, 8, 8, 8)));
		return panel;
	}

	private JPanel createSection(String title) {
		return createSection(title, 120);
	}
}