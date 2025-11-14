/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fitur;

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

public class RoundedBottomPanel extends JPanel {

    // Anda bisa ubah nilai ini untuk radius lengkungan yang berbeda
    private int cornerRadius = 25;

    public RoundedBottomPanel() {
        // PENTING: Kita harus mengatur ini ke 'false'
        // agar panel tidak melukis latar belakang kotaknya sendiri.
        setOpaque(false);
    }
    
    // Anda bisa tambahkan setter jika ingin mengubah radius dari luar
    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint(); // Gambar ulang panel dengan radius baru
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Kita perlu Graphics2D untuk antialiasing (memperhalus pinggiran)
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Tentukan Path (jalur) bentuk kustom kita
        Path2D.Double path = new Path2D.Double();
        
        // 1. Mulai dari kiri atas (sudut tajam)
        path.moveTo(0, 0);
        // 2. Garis lurus ke kanan atas (sudut tajam)
        path.lineTo(w, 0);
        // 3. Garis lurus ke bawah (sebelum lengkungan kanan bawah)
        path.lineTo(w, h - cornerRadius);
        // 4. Lengkungan kanan bawah
        path.quadTo(w, h, w - cornerRadius, h);
        // 5. Garis lurus ke kiri (sebelum lengkungan kiri bawah)
        path.lineTo(cornerRadius, h);
        // 6. Lengkungan kiri bawah
        path.quadTo(0, h, 0, h - cornerRadius);
        // 7. Tutup jalur kembali ke titik awal (kiri atas)
        path.closePath();

        // Set warna cat ke warna background panel
        // PENTING: Ambil background dari 'this' (panel kustom)
        g2d.setColor(getBackground());
        
        // Isi (fill) bentuk yang sudah kita buat
        g2d.fill(path);

        // Hapus resource g2d
        g2d.dispose();
        
        // PENTING: Panggil super.paintComponent(g) SETELAHNYA
        // Ini agar semua komponen anak (seperti JButton, JLabel)
        // dilukis DI ATAS latar belakang kustom kita.
        super.paintComponent(g);
    }
}
