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
		JDialog dialog = new JDialog(parent, "Einstellunge / Settings", true);
		dialog.setSize(480, 420);
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

		fontSlider.addChangeListener(e -> {
			fontSizeLabel.setText(fontSlider.getValue() + " pt");
		});

		JPanel fontRow = new JPanel(new BorderLayout(8, 0));
		fontRow.setBackground(new Color(43, 45, 48));
		fontRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
		fontRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		fontRow.add(fontSlider, BorderLayout.CENTER);
		fontRow.add(fontSizeLabel, BorderLayout.EAST);

		fontPanel.add(fontRow);
		content.add(fontPanel);
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

		acSlider.addChangeListener(e -> {
			acLabel.setText("Verzögerung: " + acSlider.getValue() + " ms");
		});

		JPanel acRow = new JPanel(new BorderLayout(8, 0));
		acRow.setBackground(new Color(43, 45, 48));
		acRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
		acRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		acRow.add(acSlider, BorderLayout.CENTER);
		acRow.add(acLabel, BorderLayout.EAST);

		acPanel.add(acRow);
		content.add(acPanel);
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

			// Autocomplete-Delay
			TIDEPreferences.saveAutocompleteDelay(acSlider.getValue());

			// Auto-Scroll
			TIDEPreferences.saveConsoleAutoScroll(autoScrollBox.isSelected());

			if (onLanguageChanged != null) onLanguageChanged.run();

			dialog.dispose();
		});

		btnPanel.add(btnCancel);
		btnPanel.add(btnApply);

		JScrollPane scrollPane = new JScrollPane(content);
		scrollPane.setBorder(null);
		scrollPane.getViewport().setBackground(new Color(43, 45, 48));

		dialog.add(scrollPane,  BorderLayout.CENTER);
		dialog.add(btnPanel,    BorderLayout.SOUTH);
		dialog.setVisible(true);
	}

	private JPanel createSection(String title) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(new Color(43, 45, 48));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

		TitledBorder border = BorderFactory.createTitledBorder(
			BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
			title);
		border.setTitleColor(new Color(180, 180, 180));
		border.setTitleFont(new Font("Segoe UI", Font.PLAIN, 12));
		panel.setBorder(BorderFactory.createCompoundBorder(
			border,
			new EmptyBorder(6, 8, 8, 8)));
		return panel;
	}
}