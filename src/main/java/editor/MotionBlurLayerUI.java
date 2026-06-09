package editor;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;

/**
 * Legacy-Stub – nicht mehr aktiv genutzt.
 * Das Motion-Blur-System läuft seit der Überarbeitung direkt in der
 * anonymen RTextScrollPane-Unterklasse innerhalb von EditorManager.
 */


@SuppressWarnings({"serial", "this-escape"})
 public class MotionBlurLayerUI extends LayerUI<JScrollPane> {
    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
    }
}