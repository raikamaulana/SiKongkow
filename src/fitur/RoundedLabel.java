/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fitur;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class RoundedLabel extends JLabel {

    private int radius = 25;

    public RoundedLabel() {
        setOpaque(false);
    }

    public void setCornerRadius(int radius) {
        this.radius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Buat background melengkung
        Shape round = new RoundRectangle2D.Double(0, 0, w, h, radius, radius);
        g2.setClip(round);

        // Gambar icon jika ada
        Icon icon = getIcon();
        if (icon != null) {
            int x = (w - icon.getIconWidth()) / 2;
            int y = (h - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, x, y);
        }

        super.paintComponent(g);
        g2.dispose();
    }
}

