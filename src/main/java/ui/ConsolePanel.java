package ui;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

import config.TIDEProperties;

public class ConsolePanel extends JPanel {

    private JTextPane consolePane;
    private JTextField terminalInput;

    public ConsolePanel() {
        super(new BorderLayout());

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

        add(consoleScroll, BorderLayout.CENTER);
        add(terminalInput, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(0, TIDEProperties.CONSOLE_HEIGHT));
    }

    public JTextPane getConsolePane() {
        return consolePane;
    }

    public JTextField getTerminalInput() {
        return terminalInput;
    }

    public void log(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = consolePane.getStyledDocument();
		  if (doc.getLength() > TIDEProperties.CONSOLE_MAX_CHARS) {
		  try {
		  doc.remove(0, TIDEProperties.CONSOLE_TRIM_CHARS); // erste 5000 Zeichen löschen
		  } catch (Exception ignored) {}
		  }
            Style style = consolePane.addStyle("style", null);
            StyleConstants.setForeground(style, color);
            try {
                doc.insertString(doc.getLength(), msg, style);
                consolePane.setCaretPosition(doc.getLength());
            } catch (Exception ignored) {}
        });
    }

    public void clear() {
        consolePane.setText("");
    }
}
