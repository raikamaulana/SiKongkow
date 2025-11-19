package fitur;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Asus
 */
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JPanel;
public class RoundedTopPanel extends JPanel {
     private int cornerRadius = 45;

    public RoundedTopPanel() {
        setOpaque(false);
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Path2D.Double path = new Path2D.Double();

        // MULAI DARI BAGIAN KIRI BAWAH (sudut tajam)
        path.moveTo(0, h);

        // Garis ke kanan bawah (sudut tajam)
        path.lineTo(w, h);

        // Garis naik sebelum sudut kanan atas
        path.lineTo(w, cornerRadius);

        // Lengkungan kanan atas
        path.quadTo(w, 0, w - cornerRadius, 0);

        // Garis mendatar ke kiri sebelum lengkungan kiri atas
        path.lineTo(cornerRadius, 0);

        // Lengkungan kiri atas
        path.quadTo(0, 0, 0, cornerRadius);

        // Tutup path
        path.closePath();

        // Isi warna background
        g2d.setColor(getBackground());
        g2d.fill(path);

        g2d.dispose();
        super.paintComponent(g);
    }
}
