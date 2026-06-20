package ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import config.TIDEPreferences;
import config.Theme;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings({"serial", "this-escape"})
public class SearchPanel extends JPanel {

	private JTextField searchField;
	private JCheckBox matchCaseCB;
	private JTabbedPane editorTabs;
	private ConsolePanel consolePanel;
	private final Theme currentTheme;

	public SearchPanel(JTabbedPane editorTabs, ConsolePanel consolePanel) {
		super(new FlowLayout(FlowLayout.LEFT));
		this.editorTabs   = editorTabs;
		this.consolePanel = consolePanel;
		this.currentTheme      = Theme.byName(TIDEPreferences.getTheme());
		this.setBackground(currentTheme.backgroundLight);
		
		setVisible(false);

		searchField = new JTextField(20);
		JButton btnNext  = new JButton("Abwärts");
		JButton btnPrev  = new JButton("Aufwärts");
		matchCaseCB      = new JCheckBox("Groß/Klein");
		JButton btnClose = new JButton("x");

		searchField.addActionListener(e -> search(true));
		btnNext.addActionListener(e  -> search(true));
		btnPrev.addActionListener(e  -> search(false));
		btnClose.addActionListener(e -> setVisible(false));

		searchField.setBackground(currentTheme.background);

		add(new JLabel("Suchen:"));
		add(searchField);
		add(btnNext);
		add(btnPrev);
		add(matchCaseCB);
		add(btnClose);
	}

	public JTextField getSearchField() {
		return searchField;
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
		if (!result.wasFound()) consolePanel.log("[INFO] Text nicht gefunden.\n", Color.ORANGE);
	}
}
