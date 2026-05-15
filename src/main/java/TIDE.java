import com.formdev.flatlaf.FlatDarkLaf;
import ui.MainWindow;

import javax.swing.*;
import java.awt.*;

public class TIDE {

    public static void main(String[] args) {
    System.setProperty("sun.java2d.d3d", "false");
    System.setProperty("sun.java2d.noddraw", "true");

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

    SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
}
}