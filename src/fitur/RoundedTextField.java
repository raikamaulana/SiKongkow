/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fitur;

/**
 *
 * @author Asus
 */
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JTextField;

public class RoundedTextField extends JTextField {

    private Shape shape;
    private int cornerRadius = 15; // Radius lengkungan

    public RoundedTextField() {
        setOpaque(false); // Buat transparan
        // Atur padding (atas, kiri, bawah, kanan)
        setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Gambar latar belakang bulat
        g2.setColor(getBackground());
        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));

        // Panggil super.paintComponent(g) untuk menggambar teks
        super.paintComponent(g);
        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Gambar border bulat
        g2.setColor(getForeground()); // Warna border bisa diubah, misal Color.GRAY
        g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius));
        g2.dispose();
    }

    // Ini untuk memastikan area klik sesuai dengan bentuk bulat
    @Override
    public boolean contains(int x, int y) {
        if (shape == null || !shape.getBounds().equals(getBounds())) {
            shape = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }
        return shape.contains(x, y);
    }
}