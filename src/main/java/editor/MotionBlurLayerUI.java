package editor;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MotionBlurLayerUI extends LayerUI<JScrollPane> {
    private int lastValue = -1;
    private int deltaY = 0;
    private long lastScrollTime = 0;

    public void updateScroll(int newValue) {
        if (lastValue != -1) {
            deltaY = newValue - lastValue;
        }
        lastValue = newValue;
        lastScrollTime = System.currentTimeMillis();
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (!(c instanceof JLayer<?> layer)) {
            super.paint(g, c);
            return;
        }

        long age = System.currentTimeMillis() - lastScrollTime;

        // Wenn nicht gescrollt wird oder die Bewegung minimal ist -> normal zeichnen
        if (age > 50 || Math.abs(deltaY) < 3) {
            super.paint(g, c);
            return;
        }

        // Snapshot der JScrollPane machen (wichtig, um Rekursion zu vermeiden!)
        Component view = layer.getView();
        BufferedImage img = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D imgG = img.createGraphics();
        view.paint(imgG);
        imgG.dispose();

        Graphics2D g2d = (Graphics2D) g.create();
        
        // 1. Das originale, scharfe Bild zeichnen
        g2d.drawImage(img, 0, 0, null);

        // 2. "Geisterbilder" für den Blur-Effekt drüberlegen
        int steps = Math.min(4, Math.abs(deltaY) / 5); // Anzahl der Blur-Stufen je nach Speed
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f)); // Transparenz der Echos
        
        for (int i = 1; i <= steps; i++) {
            int offset = (deltaY * i) / steps;
            // Verschiebung entgegengesetzt der Scroll-Richtung zeichnen
            g2d.drawImage(img, 0, -offset, null);
        }
        
        g2d.dispose();

        // Repaint erzwingen, damit der Effekt flüssig abbaut
        c.repaint();
    }
}