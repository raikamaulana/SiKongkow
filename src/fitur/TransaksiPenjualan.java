/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package fitur;
import java.awt.BorderLayout; // Pastikan import ini ada
/**
 *
 * @author Asus
 */
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Transaksi Penjualan Final
 * Fitur: 
 * 1. Tampilan 2 Kolom Stabil
 * 2. Klik Label Kategori
 * 3. Potong Stok Otomatis (Resep)
 */
public class TransaksiPenjualan extends javax.swing.JFrame {
    // Data User & Transaksi
    private int userId;
    private String namaKaryawan;
    
    private ArrayList<CartItem> keranjang = new ArrayList<>();
    private int currentPelangganId = 0; // 0 = Tamu
    private double discountPercentage = 0.0;
    private double totalBelanja = 0;
       
    private DecimalFormat kursIndonesia = new DecimalFormat("Rp #,###");
// --- CONSTRUCTOR ---
    
    // 1. Constructor Default (Yang dipanggil saat Anda Run File ini langsung)
    public TransaksiPenjualan() {
        initComponents();
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // --- PERBAIKAN DISINI ---
        // Kita set ID manual (misal 1) supaya tidak error constraint saat testing
        // Pastikan di tabel 'users' ada ID 1. Kalau tidak ada, ganti angka ini.
        this.userId = 1; 
        this.namaKaryawan = "Admin Test";
        
        initCustomLayouts(); 
        initCategoryListeners(); 
        loadMenu("", ""); 
    }

    // 2. Constructor Utama (Yang dipanggil dari Dashboard setelah Login)
    public TransaksiPenjualan(int userId, String namaKaryawan) {
        this.userId = userId; // Ini mengambil ID asli dari login
        this.namaKaryawan = namaKaryawan;

        initComponents();
        this.setExtendedState(JFrame.MAXIMIZED_BOTH); 
        initCustomLayouts();
        initCategoryListeners(); 
        loadMenu("", ""); 
    }
    
        /**
     * Mengatur Layout Manager untuk Panel Kontainer agar rapi
     * Dipanggil setelah initComponents()
     */
    private void initCustomLayouts() {
       // 1. PERBAIKAN TAMPILAN PRODUK AGAR TIDAK MELAR
        // Masalah: GridLayout memaksa komponen memenuhi seluruh tinggi ScrollPane.
        // Solusi: Bungkus panelProdukContainer di dalam panel lain (BorderLayout.NORTH).
        
        if (panelProdukContainer != null && scrollProduk != null) {
            // Atur layout grid 2 kolom dengan gap
            panelProdukContainer.setLayout(new GridLayout(0, 2, 15, 15)); 
            panelProdukContainer.setBackground(Color.WHITE);
            
            // Buat Wrapper Panel
            JPanel wrapperPanel = new JPanel(new BorderLayout());
            wrapperPanel.setBackground(Color.WHITE);
            
            // Masukkan container produk ke bagian ATAS (NORTH) wrapper
            // Ini mencegah grid ditarik ke bawah memenuhi layar
            wrapperPanel.add(panelProdukContainer, BorderLayout.NORTH);
            
            // Set viewport scroll ke wrapper, bukan langsung ke container
            scrollProduk.setViewportView(wrapperPanel);
            
            // Percepat scroll mouse
            scrollProduk.getVerticalScrollBar().setUnitIncrement(16);
        }
        
        // Layout Keranjang (List ke Bawah)
        if (panelCartContainer != null) {
            panelCartContainer.setLayout(new BoxLayout(panelCartContainer, BoxLayout.Y_AXIS));
        }
        
        // Live Search
        if (txtSearch != null) {
            txtSearch.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { loadMenu(txtSearch.getText(), ""); }
                public void removeUpdate(DocumentEvent e) { loadMenu(txtSearch.getText(), ""); }
                public void changedUpdate(DocumentEvent e) { loadMenu(txtSearch.getText(), ""); }
            });
        }
    }

     /**
     * Menambahkan fungsi KLIK pada JLabel Kategori
     * Pastikan nama variabel di Desain NetBeans Anda sesuai dengan ini:
     * lblHeavyMeal, lblSnack, lblBakaran, lblMinuman
     */
    private void initCategoryListeners() {
        // Helper function untuk menambahkan listener
        addClickEffect(lblHeavyMeal, "Makanan Berat");
        addClickEffect(lblSnacks, "Cemilan");
        addClickEffect(lblBakaran, "Bakaran");
        addClickEffect(lblMinuman, "Minuman");
        
        // Tombol "Semua" atau Reset (jika ada labelnya, misal lblSemua)
        // addClickEffect(lblSemua, ""); 
    }

    private void addClickEffect(JLabel label, String kategori) {
        if (label == null) return; // Lewati jika label belum dibuat di desain

        label.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Ubah kursor jadi tangan
        
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Saat diklik, load menu sesuai kategori
                loadMenu("", kategori);
                
                // Opsional: Beri efek visual (misal ganti warna teks sebentar)
                resetAllLabelStyles();
                label.setForeground(new Color(113, 90, 90)); // Highlight aktif
            }
        });
    }
    
    private void resetAllLabelStyles() {
        // Reset warna label ke default (Hitam/Abu)
        Color defaultColor = Color.WHITE; 
        if(lblHeavyMeal != null) lblHeavyMeal.setForeground(defaultColor);
        if(lblSnacks != null) lblSnacks.setForeground(defaultColor);
        if(lblBakaran != null) lblBakaran.setForeground(defaultColor);
        if(lblMinuman != null) lblMinuman.setForeground(defaultColor);
    }
    
    // ========================================================================
    // BAGIAN LOGIKA DATABASE (SESUAI TABEL ANDA)
    // ========================================================================

    /**
     * 1. Mengambil Menu dari Database (Tabel: menu)
     */
    private void loadMenu(String keyword, String kategori) {
        if (panelProdukContainer == null) return;
        
        panelProdukContainer.removeAll(); 

        // Query ke tabel 'menu'
        String sql = "SELECT * FROM menu WHERE nama_menu LIKE ? AND status = 'Tersedia'";
        
        // Filter Kategori (Sesuai ENUM di database Anda: 'Makanan Berat', 'Cemilan', dll)
        if (!kategori.isEmpty()) {
            sql += " AND kategori = ?"; 
        }

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + keyword + "%");
            if (!kategori.isEmpty()) {
                stmt.setString(2, kategori);
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("menu_id");
                String nama = rs.getString("nama_menu");
                double harga = rs.getDouble("harga");
                String img = rs.getString("gambar_url");

                // Tampilkan Kartu
                panelProdukContainer.add(createProductCard(id, nama, harga, img));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        panelProdukContainer.revalidate();
        panelProdukContainer.repaint();
    }

    // Membuat Kartu Produk (UI)
    private JPanel createProductCard(int id, String name, double price, String imgUrl) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        card.setPreferredSize(new Dimension(160, 200));

        // Gambar
        JLabel lblImg = new JLabel(name, SwingConstants.CENTER);
        lblImg.setBackground(new Color(240, 240, 240));
        lblImg.setOpaque(true);
        
        // Info
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.setBackground(Color.WHITE);
        JLabel lblName = new JLabel(" " + name);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel lblPrice = new JLabel(" " + kursIndonesia.format(price));
        lblPrice.setForeground(new Color(113, 90, 90));
        infoPanel.add(lblName);
        infoPanel.add(lblPrice);

        // Tombol Tambah
        JButton btnAdd = new JButton("+");
        btnAdd.setBackground(new Color(113, 90, 90));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.addActionListener(e -> addToCart(id, name, price));

        card.add(lblImg, BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.NORTH);
        card.add(btnAdd, BorderLayout.SOUTH);

        return card;
    }

    /**
     * 2. Logika Keranjang Belanja
     */
    private void addToCart(int id, String name, double price) {
        boolean exists = false;
        for (CartItem item : keranjang) {
            if (item.menuId == id) {
                item.qty++;
                exists = true;
                break;
            }
        }
        if (!exists) {
            keranjang.add(new CartItem(id, name, price, 1));
        }
        updateCartUI();
    }

    private void updateCartUI() {
        if (panelCartContainer == null) return;
        
        panelCartContainer.removeAll();
        double subtotal = 0;

        for (CartItem item : keranjang) {
            JPanel itemPanel = new JPanel(new BorderLayout());
            itemPanel.setBackground(Color.WHITE);
            itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            itemPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

            JLabel lblName = new JLabel(" " + item.menuName);
            
            JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            qtyPanel.setBackground(Color.WHITE);
            JButton btnMin = new JButton("-");
            JButton btnPlus = new JButton("+");
            JLabel lblQty = new JLabel(String.valueOf(item.qty));
            
            btnMin.setPreferredSize(new Dimension(40, 25));
            btnPlus.setPreferredSize(new Dimension(40, 25));
            
            btnMin.addActionListener(e -> updateItemQty(item, -1));
            btnPlus.addActionListener(e -> updateItemQty(item, 1));

            qtyPanel.add(btnMin);
            qtyPanel.add(lblQty);
            qtyPanel.add(btnPlus);

            itemPanel.add(lblName, BorderLayout.CENTER);
            itemPanel.add(qtyPanel, BorderLayout.EAST);

            panelCartContainer.add(itemPanel);
            subtotal += (item.price * item.qty);
        }

        // Hitung Diskon & Total
        double discountAmount = subtotal * (discountPercentage / 100);
        this.totalBelanja = subtotal - discountAmount;

        if (lblSubtotalVal != null) lblSubtotalVal.setText(kursIndonesia.format(subtotal));
        if (lblDiscountVal != null) lblDiscountVal.setText(kursIndonesia.format(discountAmount));
        if (lblTotalVal != null) lblTotalVal.setText(kursIndonesia.format(this.totalBelanja));

        panelCartContainer.revalidate();
        panelCartContainer.repaint();
    }

    private void updateItemQty(CartItem item, int delta) {
        item.qty += delta;
        if (item.qty <= 0) keranjang.remove(item);
        updateCartUI();
    }

    /**
     * 3. Cek Member (Tabel: pelanggan)
     */
    private void cekMember() {
        String phone = txtMemberPhone.getText();
        if (phone.isEmpty()) { resetMember(); return; }

        // Query ke tabel 'pelanggan'
        String sql = "SELECT pelanggan_id, nama, visit_count FROM pelanggan WHERE no_hp = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                currentPelangganId = rs.getInt("pelanggan_id");
                String pNama = rs.getString("nama");
                int visit = rs.getInt("visit_count");
                
                // Logika Diskon
                if (visit > 10) discountPercentage = 15;
                else if (visit > 5) discountPercentage = 10;
                else discountPercentage = 0;
                
                if (lblMemberStatus != null) 
                    lblMemberStatus.setText("Member: " + pNama + " (" + discountPercentage + "%)");
            } else {
                if (lblMemberStatus != null) lblMemberStatus.setText("Tidak Ditemukan");
                resetMember();
                
                int opt = JOptionPane.showConfirmDialog(this, "Nomor belum terdaftar. Buat Member Baru?", "Member", JOptionPane.YES_NO_OPTION);
                if(opt == JOptionPane.YES_OPTION) {
                    String namaBaru = JOptionPane.showInputDialog("Masukkan Nama:");
                    if(namaBaru != null && !namaBaru.isEmpty()) {
                        tambahMemberBaru(phone, namaBaru);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        updateCartUI();
    }
    
    private void resetMember() {
        currentPelangganId = 0;
        discountPercentage = 0;
        if (lblMemberStatus != null) lblMemberStatus.setText("-");
        updateCartUI();
    }

    private void tambahMemberBaru(String phone, String nama) {
        String sql = "INSERT INTO pelanggan (nama, no_hp, visit_count) VALUES (?, ?, 1)";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nama);
            stmt.setString(2, phone);
            stmt.executeUpdate();
            cekMember(); 
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal daftar: " + e.getMessage());
        }
    }

 /**
     * PROSES PEMBAYARAN LENGKAP + POTONG STOK
     */
    private void prosesPembayaran() {
        if (keranjang.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keranjang Kosong!");
            return;
        }

        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false); // Transaction Start

            // 1. Simpan Header Pesanan
            String sqlPesanan = "INSERT INTO pesanan (karyawan_id, pelanggan_id, subtotal, diskon, total_harga, tipe_pembayaran, status_pesanan, waktu_pesanan) VALUES (?, ?, ?, ?, ?, ?, 'Selesai', NOW())";
            PreparedStatement ps = conn.prepareStatement(sqlPesanan, Statement.RETURN_GENERATED_KEYS);
            
            // Safety check untuk userId (ID 0 ditolak database)
            int safeUserId = (userId == 0) ? 1 : userId;
            
            ps.setInt(1, safeUserId);
            if (currentPelangganId == 0) ps.setNull(2, java.sql.Types.INTEGER);
            else ps.setInt(2, currentPelangganId);
            
            double sub = 0;
            for(CartItem c : keranjang) sub += (c.price * c.qty);
            double disc = sub * (discountPercentage/100);
            double tot = sub - disc;
            
            ps.setDouble(3, sub);
            ps.setDouble(4, disc);
            ps.setDouble(5, tot);
            ps.setString(6, comboPayment.getSelectedItem().toString());
            
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            int pesananId = 0;
            if (rs.next()) pesananId = rs.getInt(1);

            // 2. Simpan Detail & Update Stok
            String sqlDetail = "INSERT INTO detail_pesanan (pesanan_id, menu_id, jumlah, harga_saat_pesan, subtotal, catatan) VALUES (?, ?, ?, ?, ?, ?)";
            String sqlGetResep = "SELECT bahan_baku_id, jumlah_dibutuhkan FROM resep_menu WHERE menu_id = ?";
            String sqlUpdateStok = "UPDATE bahan_baku SET stok_saat_ini = stok_saat_ini - ? WHERE bahan_baku_id = ?";
            
            PreparedStatement psDet = conn.prepareStatement(sqlDetail);
            PreparedStatement psResep = conn.prepareStatement(sqlGetResep);
            PreparedStatement psStok = conn.prepareStatement(sqlUpdateStok);
            
            for (CartItem item : keranjang) {
                // A. Simpan Detail
                psDet.setInt(1, pesananId);
                psDet.setInt(2, item.menuId);
                psDet.setInt(3, item.qty);
                psDet.setDouble(4, item.price);
                psDet.setDouble(5, item.price * item.qty);
                psDet.setString(6, "-"); 
                psDet.addBatch();
                
                // B. Kurangi Stok Bahan Baku (Looping Resep)
                psResep.setInt(1, item.menuId);
                ResultSet rsResep = psResep.executeQuery();
                while(rsResep.next()) {
                    int idBahan = rsResep.getInt("bahan_baku_id");
                    double butuh = rsResep.getDouble("jumlah_dibutuhkan");
                    double totalKurang = butuh * item.qty;
                    
                    psStok.setDouble(1, totalKurang);
                    psStok.setInt(2, idBahan);
                    psStok.executeUpdate();
                }
            }
            psDet.executeBatch();

            // 3. Update Member
            if (currentPelangganId != 0) {
                conn.prepareStatement("UPDATE pelanggan SET visit_count = visit_count + 1 WHERE pelanggan_id = " + currentPelangganId).executeUpdate();
            }

            conn.commit(); 
            JOptionPane.showMessageDialog(this, "Pembayaran Berhasil!\nStok bahan baku telah diperbarui.");
            
            keranjang.clear();
            if(txtMemberPhone != null) txtMemberPhone.setText("");
            resetMember();
            updateCartUI();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal: " + e.getMessage());
        }
    }
    
   // --- TOMBOL AKSI (Sambungkan ke Event Handler Anda) ---
    private void aksiFilterMakanan() { loadMenu("", "Makanan Berat"); }
    private void aksiFilterMinuman() { loadMenu("", "Minuman"); }
    private void aksiFilterCemilan() { loadMenu("", "Cemilan"); }
    private void aksiFilterBakaran() { loadMenu("", "Bakaran"); }
    private void aksiCekMember() { cekMember(); }
    private void aksiBayar() { prosesPembayaran(); }
    
    private void kembaliKeDashboard() {
        new DashboardKaryawan(userId, namaKaryawan).setVisible(true);
        this.dispose();
    }

    class CartItem {
        int menuId; String menuName; double price; int qty;
        public CartItem(int id, String name, double p, int q) { 
            menuId=id; menuName=name; price=p; qty=q; 
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        exitBT = new javax.swing.JButton();
        jPanel6 = new fitur.RoundedPanel(30) ;
        transaksiLB = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jPanel5 = new fitur.RoundedTopPanel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        lblSubtotalVal = new javax.swing.JLabel();
        lblDiscountVal = new javax.swing.JLabel();
        comboPayment = new javax.swing.JComboBox<>();
        jLabel57 = new javax.swing.JLabel();
        lblTotalVal = new javax.swing.JLabel();
        submitBTN = new javax.swing.JButton();
        txtMemberPhone = new javax.swing.JTextField();
        jLabel61 = new javax.swing.JLabel();
        lblMemberStatus = new javax.swing.JLabel();
        jPanel15 = new fitur.RoundedPanel(30) ;
        transaksiLB6 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        panelCartContainer = new javax.swing.JPanel();
        jPanel8 = new fitur.RoundedPanel(30) ;
        lblHeavyMeal = new javax.swing.JLabel();
        lblSnack = new fitur.RoundedPanel(30) ;
        lblSnacks = new javax.swing.JLabel();
        jPanel10 = new fitur.RoundedPanel(30) ;
        lblBakaran = new javax.swing.JLabel();
        jPanel11 = new fitur.RoundedPanel(30) ;
        lblMinuman = new javax.swing.JLabel();
        txtSearch = new fitur.RoundedTextField();
        scrollProduk = new javax.swing.JScrollPane();
        panelProdukContainer = new javax.swing.JPanel();
        jPanel17 = new fitur.RoundedPanel(30) ;
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jButton8 = new javax.swing.JButton();
        jLabel23 = new javax.swing.JLabel();
        jPanel18 = new fitur.RoundedPanel(30) ;
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jButton9 = new javax.swing.JButton();
        jLabel26 = new javax.swing.JLabel();
        jPanel19 = new fitur.RoundedPanel(30) ;
        jButton10 = new javax.swing.JButton();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        jPanel20 = new fitur.RoundedPanel(30) ;
        jLabel30 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jLabel32 = new javax.swing.JLabel();
        jPanel21 = new fitur.RoundedPanel(30) ;
        jLabel33 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        jButton11 = new javax.swing.JButton();
        jLabel35 = new javax.swing.JLabel();
        jPanel22 = new fitur.RoundedPanel(30) ;
        jLabel36 = new javax.swing.JLabel();
        jLabel37 = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        jLabel38 = new javax.swing.JLabel();
        jPanel23 = new fitur.RoundedPanel(30) ;
        jButton12 = new javax.swing.JButton();
        jLabel39 = new javax.swing.JLabel();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jPanel24 = new fitur.RoundedPanel(30) ;
        jLabel42 = new javax.swing.JLabel();
        jLabel43 = new javax.swing.JLabel();
        jButton13 = new javax.swing.JButton();
        jLabel44 = new javax.swing.JLabel();
        jPanel25 = new fitur.RoundedPanel(30) ;
        jLabel45 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        jButton14 = new javax.swing.JButton();
        jLabel47 = new javax.swing.JLabel();
        jPanel26 = new fitur.RoundedPanel(30) ;
        jButton15 = new javax.swing.JButton();
        jLabel48 = new javax.swing.JLabel();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jPanel27 = new fitur.RoundedPanel(30) ;
        jLabel51 = new javax.swing.JLabel();
        jLabel52 = new javax.swing.JLabel();
        jButton16 = new javax.swing.JButton();
        jLabel53 = new javax.swing.JLabel();
        jPanel28 = new fitur.RoundedPanel(30) ;
        jLabel54 = new javax.swing.JLabel();
        jLabel55 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jLabel56 = new javax.swing.JLabel();

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel3.setBackground(new java.awt.Color(211, 218, 217));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/kasir.png"))); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addContainerGap(23, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(113, 90, 90));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Hai, Karyawan!");

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/user_17740782.png"))); // NOI18N

        exitBT.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/log-out.png"))); // NOI18N
        exitBT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitBTActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(exitBT, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap(18, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(15, 15, 15))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(exitBT, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBackground(new java.awt.Color(68, 68, 78));

        transaksiLB.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        transaksiLB.setForeground(new java.awt.Color(255, 255, 255));
        transaksiLB.setText("Transaksi");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(transaksiLB, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(transaksiLB, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        jPanel7.setBackground(new java.awt.Color(255, 255, 255));
        jPanel7.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));

        jPanel5.setBackground(new java.awt.Color(217, 217, 217));

        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel16.setText("Rincian Harga Pesanan");

        jLabel17.setText("Subtotal");

        jLabel18.setText("Harga Diskon ");

        lblSubtotalVal.setText("0");

        lblDiscountVal.setText("0");

        comboPayment.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Tunai", "Non-Tunai" }));
        comboPayment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboPaymentActionPerformed(evt);
            }
        });

        jLabel57.setText("Total");

        lblTotalVal.setText("0");

        submitBTN.setText("Selesaikan Pembayaran");
        submitBTN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitBTNActionPerformed(evt);
            }
        });

        txtMemberPhone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtMemberPhoneActionPerformed(evt);
            }
        });

        jLabel61.setText("No HP");

        lblMemberStatus.setText("VIP");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGap(44, 44, 44)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel57)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblTotalVal)
                        .addGap(68, 68, 68))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(comboPayment, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel18)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblDiscountVal))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel17)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblSubtotalVal))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jLabel61)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(txtMemberPhone, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblMemberStatus)
                        .addGap(18, 18, 18))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(submitBTN)
                        .addGap(59, 59, 59))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addGap(51, 51, 51))))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(lblSubtotalVal))
                .addGap(4, 4, 4)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel61)
                    .addComponent(txtMemberPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMemberStatus))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(lblDiscountVal))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(comboPayment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTotalVal)
                    .addComponent(jLabel57))
                .addGap(13, 13, 13)
                .addComponent(submitBTN)
                .addContainerGap())
        );

        jPanel15.setBackground(new java.awt.Color(68, 68, 78));

        transaksiLB6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        transaksiLB6.setForeground(new java.awt.Color(255, 255, 255));
        transaksiLB6.setText("Detail Pesanan");

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(transaksiLB6)
                .addContainerGap(13, Short.MAX_VALUE))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(transaksiLB6, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        javax.swing.GroupLayout panelCartContainerLayout = new javax.swing.GroupLayout(panelCartContainer);
        panelCartContainer.setLayout(panelCartContainerLayout);
        panelCartContainerLayout.setHorizontalGroup(
            panelCartContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 277, Short.MAX_VALUE)
        );
        panelCartContainerLayout.setVerticalGroup(
            panelCartContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 244, Short.MAX_VALUE)
        );

        jScrollPane2.setViewportView(panelCartContainer);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(56, 56, 56))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel8.setBackground(new java.awt.Color(68, 68, 78));

        lblHeavyMeal.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblHeavyMeal.setForeground(new java.awt.Color(255, 255, 255));
        lblHeavyMeal.setText("Heavy Meal");
        lblHeavyMeal.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblHeavyMeal.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblHeavyMealMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                .addContainerGap(18, Short.MAX_VALUE)
                .addComponent(lblHeavyMeal)
                .addGap(16, 16, 16))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblHeavyMeal, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE)
        );

        lblSnack.setBackground(new java.awt.Color(68, 68, 78));

        lblSnacks.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblSnacks.setForeground(new java.awt.Color(255, 255, 255));
        lblSnacks.setText("Snack");
        lblSnacks.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblSnacks.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblSnacksMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout lblSnackLayout = new javax.swing.GroupLayout(lblSnack);
        lblSnack.setLayout(lblSnackLayout);
        lblSnackLayout.setHorizontalGroup(
            lblSnackLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lblSnackLayout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(lblSnacks)
                .addContainerGap(26, Short.MAX_VALUE))
        );
        lblSnackLayout.setVerticalGroup(
            lblSnackLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblSnacks, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE)
        );

        jPanel10.setBackground(new java.awt.Color(68, 68, 78));

        lblBakaran.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblBakaran.setForeground(new java.awt.Color(255, 255, 255));
        lblBakaran.setText("Bakaran");
        lblBakaran.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblBakaran.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblBakaranMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap(25, Short.MAX_VALUE)
                .addComponent(lblBakaran)
                .addGap(21, 21, 21))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblBakaran, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE)
        );

        jPanel11.setBackground(new java.awt.Color(68, 68, 78));
        jPanel11.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel11MouseClicked(evt);
            }
        });

        lblMinuman.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblMinuman.setForeground(new java.awt.Color(255, 255, 255));
        lblMinuman.setText("Minuman");
        lblMinuman.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(lblMinuman)
                .addContainerGap(22, Short.MAX_VALUE))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblMinuman, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE)
        );

        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });

        scrollProduk.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jPanel17.setBackground(new java.awt.Color(230, 230, 230));

        jLabel21.setText("Rp. 15.000");

        jLabel22.setText("Seblak");

        jButton8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel23.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel17Layout.createSequentialGroup()
                        .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel21)
                            .addComponent(jLabel22))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel17Layout.createSequentialGroup()
                        .addComponent(jLabel23)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel17Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addComponent(jLabel23)
                .addGap(18, 18, 18)
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel17Layout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel21)))
                .addContainerGap())
        );

        jPanel18.setBackground(new java.awt.Color(230, 230, 230));

        jLabel24.setText("Rice Bowl");

        jLabel25.setText("Rp. 15.000");

        jButton9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel26.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel24)
                    .addComponent(jLabel25))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton9, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel26)
                .addContainerGap(19, Short.MAX_VALUE))
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel18Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel26)
                .addGap(18, 18, 18)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton9, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel18Layout.createSequentialGroup()
                        .addComponent(jLabel24)
                        .addGap(4, 4, 4)
                        .addComponent(jLabel25)))
                .addContainerGap())
        );

        jPanel19.setBackground(new java.awt.Color(230, 230, 230));

        jButton10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel27.setText("Es Teh Manis");

        jLabel28.setText("Rp 5000");

        jLabel29.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel19Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel27)
                    .addComponent(jLabel28))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel29)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel19Layout.createSequentialGroup()
                .addContainerGap(22, Short.MAX_VALUE)
                .addComponent(jLabel29)
                .addGap(18, 18, 18)
                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel19Layout.createSequentialGroup()
                        .addComponent(jLabel27)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel28)))
                .addGap(5, 5, 5))
        );

        jPanel20.setBackground(new java.awt.Color(230, 230, 230));

        jLabel30.setText("Chiken Katsu + Nasi");

        jLabel31.setText("Rp. 15.000");

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel32.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel20Layout = new javax.swing.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel20Layout.createSequentialGroup()
                        .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel31)
                            .addComponent(jLabel30))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel20Layout.createSequentialGroup()
                        .addComponent(jLabel32)
                        .addGap(0, 8, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel20Layout.setVerticalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel20Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addComponent(jLabel32)
                .addGap(18, 18, 18)
                .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel20Layout.createSequentialGroup()
                        .addComponent(jLabel30)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel31)))
                .addContainerGap())
        );

        jPanel21.setBackground(new java.awt.Color(230, 230, 230));

        jLabel33.setText("Rice Bowl");

        jLabel34.setText("Rp. 15.000");

        jButton11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel35.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel21Layout = new javax.swing.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel21Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel33)
                    .addComponent(jLabel34))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton11, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel21Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel35)
                .addContainerGap(19, Short.MAX_VALUE))
        );
        jPanel21Layout.setVerticalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel21Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel35)
                .addGap(18, 18, 18)
                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton11, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel21Layout.createSequentialGroup()
                        .addComponent(jLabel33)
                        .addGap(4, 4, 4)
                        .addComponent(jLabel34)))
                .addContainerGap())
        );

        jPanel22.setBackground(new java.awt.Color(230, 230, 230));

        jLabel36.setText("Chiken Katsu + Nasi");

        jLabel37.setText("Rp. 15.000");

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel38.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel22Layout = new javax.swing.GroupLayout(jPanel22);
        jPanel22.setLayout(jPanel22Layout);
        jPanel22Layout.setHorizontalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel22Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel22Layout.createSequentialGroup()
                        .addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel37)
                            .addComponent(jLabel36))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel22Layout.createSequentialGroup()
                        .addComponent(jLabel38)
                        .addGap(0, 8, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel22Layout.setVerticalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel22Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addComponent(jLabel38)
                .addGap(18, 18, 18)
                .addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel22Layout.createSequentialGroup()
                        .addComponent(jLabel36)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel37)))
                .addContainerGap())
        );

        jPanel23.setBackground(new java.awt.Color(230, 230, 230));

        jButton12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel39.setText("Es Teh Manis");

        jLabel40.setText("Rp 5000");

        jLabel41.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel23Layout = new javax.swing.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel23Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel39)
                    .addComponent(jLabel40))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton12, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel23Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel41)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel23Layout.setVerticalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel23Layout.createSequentialGroup()
                .addContainerGap(22, Short.MAX_VALUE)
                .addComponent(jLabel41)
                .addGap(18, 18, 18)
                .addGroup(jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton12, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel23Layout.createSequentialGroup()
                        .addComponent(jLabel39)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel40)))
                .addGap(5, 5, 5))
        );

        jPanel24.setBackground(new java.awt.Color(230, 230, 230));

        jLabel42.setText("Rp. 15.000");

        jLabel43.setText("Seblak");

        jButton13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel44.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel24Layout = new javax.swing.GroupLayout(jPanel24);
        jPanel24.setLayout(jPanel24Layout);
        jPanel24Layout.setHorizontalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel24Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel24Layout.createSequentialGroup()
                        .addGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel42)
                            .addComponent(jLabel43))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton13, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel24Layout.createSequentialGroup()
                        .addComponent(jLabel44)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel24Layout.setVerticalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel24Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addComponent(jLabel44)
                .addGap(18, 18, 18)
                .addGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton13, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel24Layout.createSequentialGroup()
                        .addComponent(jLabel43)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel42)))
                .addContainerGap())
        );

        jPanel25.setBackground(new java.awt.Color(230, 230, 230));

        jLabel45.setText("Rp. 15.000");

        jLabel46.setText("Seblak");

        jButton14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel47.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel25Layout = new javax.swing.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel25Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel25Layout.createSequentialGroup()
                        .addGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel45)
                            .addComponent(jLabel46))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton14, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel25Layout.createSequentialGroup()
                        .addComponent(jLabel47)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel25Layout.setVerticalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel25Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addComponent(jLabel47)
                .addGap(18, 18, 18)
                .addGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton14, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel25Layout.createSequentialGroup()
                        .addComponent(jLabel46)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel45)))
                .addContainerGap())
        );

        jPanel26.setBackground(new java.awt.Color(230, 230, 230));

        jButton15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel48.setText("Es Teh Manis");

        jLabel49.setText("Rp 5000");

        jLabel50.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel26Layout = new javax.swing.GroupLayout(jPanel26);
        jPanel26.setLayout(jPanel26Layout);
        jPanel26Layout.setHorizontalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel26Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel48)
                    .addComponent(jLabel49))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton15, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel26Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel50)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel26Layout.setVerticalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel26Layout.createSequentialGroup()
                .addContainerGap(22, Short.MAX_VALUE)
                .addComponent(jLabel50)
                .addGap(18, 18, 18)
                .addGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton15, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel26Layout.createSequentialGroup()
                        .addComponent(jLabel48)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel49)))
                .addGap(5, 5, 5))
        );

        jPanel27.setBackground(new java.awt.Color(230, 230, 230));

        jLabel51.setText("Rice Bowl");

        jLabel52.setText("Rp. 15.000");

        jButton16.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel53.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel27Layout = new javax.swing.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel27Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel51)
                    .addComponent(jLabel52))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton16, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel27Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jLabel53)
                .addContainerGap(19, Short.MAX_VALUE))
        );
        jPanel27Layout.setVerticalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel27Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel53)
                .addGap(18, 18, 18)
                .addGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton16, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel27Layout.createSequentialGroup()
                        .addComponent(jLabel51)
                        .addGap(4, 4, 4)
                        .addComponent(jLabel52)))
                .addContainerGap())
        );

        jPanel28.setBackground(new java.awt.Color(230, 230, 230));

        jLabel54.setText("Chiken Katsu + Nasi");

        jLabel55.setText("Rp. 15.000");

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/tambah.png"))); // NOI18N

        jLabel56.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/ayam katsu (1).jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel28Layout = new javax.swing.GroupLayout(jPanel28);
        jPanel28.setLayout(jPanel28Layout);
        jPanel28Layout.setHorizontalGroup(
            jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel28Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel28Layout.createSequentialGroup()
                        .addGroup(jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel55)
                            .addComponent(jLabel54))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel28Layout.createSequentialGroup()
                        .addComponent(jLabel56)
                        .addGap(0, 8, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel28Layout.setVerticalGroup(
            jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel28Layout.createSequentialGroup()
                .addContainerGap(21, Short.MAX_VALUE)
                .addComponent(jLabel56)
                .addGap(18, 18, 18)
                .addGroup(jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel28Layout.createSequentialGroup()
                        .addComponent(jLabel54)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel55)))
                .addContainerGap())
        );

        javax.swing.GroupLayout panelProdukContainerLayout = new javax.swing.GroupLayout(panelProdukContainer);
        panelProdukContainer.setLayout(panelProdukContainerLayout);
        panelProdukContainerLayout.setHorizontalGroup(
            panelProdukContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelProdukContainerLayout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addGroup(panelProdukContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel25, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel28, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(panelProdukContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel27, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(91, Short.MAX_VALUE))
        );
        panelProdukContainerLayout.setVerticalGroup(
            panelProdukContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelProdukContainerLayout.createSequentialGroup()
                .addGap(0, 390, Short.MAX_VALUE)
                .addGroup(panelProdukContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelProdukContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 6, Short.MAX_VALUE)
                .addGroup(panelProdukContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelProdukContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 6, Short.MAX_VALUE)
                .addGroup(panelProdukContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel27, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel28, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelProdukContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel25, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel26, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        scrollProduk.setViewportView(panelProdukContainer);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(17, 17, 17)
                                .addComponent(lblSnack, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(23, 23, 23)
                                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 289, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(scrollProduk, javax.swing.GroupLayout.PREFERRED_SIZE, 516, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtSearch))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblSnack, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(scrollProduk, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitBTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitBTActionPerformed
                 int jawaban = javax.swing.JOptionPane.showConfirmDialog(
            this, 
            "Apakah Anda yakin ingin kembali ke Dashboard?", 
            "Konfirmasi", 
            javax.swing.JOptionPane.YES_NO_OPTION, 
            javax.swing.JOptionPane.QUESTION_MESSAGE
        );
        
        if (jawaban == javax.swing.JOptionPane.YES_OPTION) {
            new DashboardKaryawan(1, "Karyawan").setVisible(true); 
            this.dispose();
        }
    }//GEN-LAST:event_exitBTActionPerformed

    private void txtMemberPhoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMemberPhoneActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtMemberPhoneActionPerformed

    private void submitBTNActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitBTNActionPerformed
        prosesPembayaran();
    }//GEN-LAST:event_submitBTNActionPerformed

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtSearchActionPerformed

    private void comboPaymentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboPaymentActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_comboPaymentActionPerformed

    private void lblHeavyMealMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblHeavyMealMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_lblHeavyMealMouseClicked

    private void lblBakaranMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblBakaranMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_lblBakaranMouseClicked

    private void lblSnacksMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblSnacksMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_lblSnacksMouseClicked

    private void jPanel11MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel11MouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_jPanel11MouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new TransaksiPenjualan().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> comboPayment;
    private javax.swing.JButton exitBT;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblBakaran;
    private javax.swing.JLabel lblDiscountVal;
    private javax.swing.JLabel lblHeavyMeal;
    private javax.swing.JLabel lblMemberStatus;
    private javax.swing.JLabel lblMinuman;
    private javax.swing.JPanel lblSnack;
    private javax.swing.JLabel lblSnacks;
    private javax.swing.JLabel lblSubtotalVal;
    private javax.swing.JLabel lblTotalVal;
    private javax.swing.JPanel panelCartContainer;
    private javax.swing.JPanel panelProdukContainer;
    private javax.swing.JScrollPane scrollProduk;
    private javax.swing.JButton submitBTN;
    private javax.swing.JLabel transaksiLB;
    private javax.swing.JLabel transaksiLB6;
    private javax.swing.JTextField txtMemberPhone;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}