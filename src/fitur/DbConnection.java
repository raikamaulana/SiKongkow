/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package fitur;

/**
 *
 * @author Asus
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {
    
    // 1. Definisikan detail koneksi Anda
    //    Pastikan sama dengan pengaturan Laragon Anda
    
    // Ganti "db_kafe" jika nama database Anda berbeda
    private static final String DB_URL = "jdbc:mysql://localhost:3306/kongkow_db";
    
    // User default MySQL di Laragon adalah "root"
    private static final String DB_USER = "root";
    
    // Password default MySQL di Laragon adalah string kosong ""
    private static final String DB_PASS = "";
    
    // Variabel untuk menyimpan koneksi
    private static Connection connection;
    
    /**
     * Metode (fungsi) inilah yang akan dipanggil oleh file lain.
     * Fungsi ini akan memberikan koneksi ke database.
     * * @return Koneksi (Connection) ke database 'db_kafe'
     * @throws SQLException jika koneksi gagal
     */
    public static Connection getConnection() throws SQLException {
        // Pengecekan:
        // 1. Apakah koneksi belum ada (null)? 
        // 2. ATAU Apakah koneksi sudah ditutup?
        // Jika ya, buat koneksi baru.
        if (connection == null || connection.isClosed()) {
            try {
                // Baris ini tidak wajib di driver modern, tapi bagus untuk ada
                // Class.forName("com.mysql.cj.jdbc.Driver");
                
                // Buat koneksi baru menggunakan detail yang sudah kita definisikan
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                
                System.out.println("Koneksi ke database BERHASIL dibuat.");
                
            } catch (SQLException e) {
                // Tangani jika terjadi error
                System.err.println("Koneksi ke database GAGAL! Error: " + e.getMessage());
                // Melempar error lagi agar pemanggil tahu bahwa koneksi gagal
                throw e; 
            }
        }
        
        // Kembalikan koneksi yang sudah ada (atau yang baru dibuat)
        return connection;
    }
    
    /**
     * Fungsi main ini hanya untuk UJI COBA
     * Anda bisa klik kanan file ini -> Run File
     * untuk mengetes apakah koneksi berhasil.
     */
    public static void main(String[] args) {
        try {
            // Coba panggil fungsi getConnection()
            Connection conn = DbConnection.getConnection();
            
            // Jika 'conn' tidak null dan tidak ditutup, berarti berhasil
            if (conn != null && !conn.isClosed()) {
                System.out.println("TES BERHASIL: Koneksi sukses didapatkan!");
            } else {
                System.out.println("TES GAGAL: Koneksi tidak didapatkan.");
            }
            
            // Tutup koneksi setelah selesai tes
            conn.close();
            
        } catch (SQLException e) {
            // Cetak error jika tes gagal
            System.err.println("Tes koneksi GAGAL! " + e.getMessage());
        }
    }
}
