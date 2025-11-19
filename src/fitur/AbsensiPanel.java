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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class AbsensiPanel extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AbsensiPanel.class.getName());
    private DefaultTableModel model;
    /**
     * Creates new form AbsensiPanel
     */
    public AbsensiPanel() {
        initComponents();
        this.setLocationRelativeTo(null); // Tengah layar
        this.setExtendedState(MAXIMIZED_BOTH); // Full screen
        
        // 1. Inisialisasi Tabel dan UI saat pertama dibuka
        aturTabel();
        loadDataTabel();
        
        // 2. Set Tanggal Otomatis Hari Ini
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        tanggalTF1.setText(sdf.format(new Date()));
        
        // 3. Kunci Field agar tidak bisa diedit manual
        tanggalTF1.setEditable(false);
        namaTF.setEditable(false);
        
        // 4. Fokus kursor ke ID agar langsung bisa ketik
        idKaryawanTF.requestFocus();
    }
    
    public AbsensiPanel(int userId, String nama) {
        this(); // Panggil constructor utama agar inisialisasi jalan
    }
    
        // ========================================================================
    // BAGIAN LOGIKA (DATABASE & FUNGSI) - DIPERBAIKI
    // ========================================================================

    /**
     * Mengatur header tabel dan mengunci sel agar tidak bisa diedit
     */
    private void aturTabel() {
        String[] judulKolom = {"ID Karyawan", "Nama", "Tanggal", "Waktu", "Status", "Alasan"};
        model = new DefaultTableModel(null, judulKolom) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Semua sel dikunci
            }
        };
        tableAbsensi.setModel(model);
    }

    /**
     * Menampilkan data riwayat absensi dari database ke tabel
     * PERBAIKAN: Menggunakan tabel 'absensi' dan 'users'
     */
    private void loadDataTabel() {
        // Bersihkan baris lama
        model.setRowCount(0);
        
        // Query gabungan tabel absensi dan users
        // Kita ambil nama dari tabel 'users' berdasarkan 'karyawan_id'
        String sql = "SELECT u.users_id, u.nama, a.tanggal_kerja, a.waktu_check_in, a.status, a.catatan " +
                     "FROM absensi a " +
                     "JOIN users u ON a.karyawan_id = u.users_id " +
                     "ORDER BY a.waktu_check_in DESC";

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Object[] baris = {
                    rs.getInt("users_id"),
                    rs.getString("nama"),
                    rs.getDate("tanggal_kerja"),
                    rs.getTime("waktu_check_in"),
                    rs.getString("status"),
                    rs.getString("catatan")
                };
                model.addRow(baris);
            }
        } catch (SQLException e) {
            logger.severe("Gagal load tabel: " + e.getMessage());
        }
    }

    /**
     * Mencari Nama Karyawan otomatis saat ID diketik & ditekan Enter
     * PERBAIKAN: Menggunakan tabel 'users'
     */
    private void cariNamaKaryawan() {
        String idInput = idKaryawanTF.getText();
        
        if (idInput.isEmpty()) return;

        // Cari di tabel 'users'
        String sql = "SELECT nama FROM users WHERE users_id = ?";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, Integer.parseInt(idInput));
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Jika ID ditemukan, isi Nama otomatis
                namaTF.setText(rs.getString("nama"));
                alasanTF.requestFocus(); // Pindah kursor ke alasan
            } else {
                JOptionPane.showMessageDialog(this, "ID Karyawan tidak ditemukan!", "Error", JOptionPane.ERROR_MESSAGE);
                namaTF.setText("");
                idKaryawanTF.requestFocus();
            }
            
        } catch (NumberFormatException e) {
             JOptionPane.showMessageDialog(this, "ID harus berupa angka!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            logger.severe("Error cari karyawan: " + e.getMessage());
        }
    }

    /**
     * Menyimpan data absensi saat tombol Submit diklik
     * PERBAIKAN: Menggunakan tabel 'absensi'
     */
    private void simpanAbsensi() {
        String idStr = idKaryawanTF.getText();
        String status = absenBox.getSelectedItem().toString();
        String alasan = alasanTF.getText();
        
        // Validasi Input
        if (idStr.isEmpty() || namaTF.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mohon isi ID Karyawan lalu tekan Enter!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Validasi jika Sakit/Izin tapi tidak ada alasan
        if ((status.equalsIgnoreCase("Sakit") || status.equalsIgnoreCase("Izin")) && alasan.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Untuk status Sakit/Izin, mohon isi alasannya.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Insert ke tabel 'absensi'
        // Kolom: karyawan_id, tanggal_kerja, waktu_check_in, status, catatan
        String sql = "INSERT INTO absensi (karyawan_id, tanggal_kerja, waktu_check_in, status, catatan) VALUES (?, CURDATE(), NOW(), ?, ?)";
        
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, Integer.parseInt(idStr));
            // ENUM di database Anda menggunakan huruf Besar Awal (Hadir, Izin, Sakit, Alpha) 
            // atau huruf kecil semua? Sesuaikan disini.
            // Jika database enum('Hadir',...), biarkan 'status'. Jika ('hadir',...), gunakan 'status.toLowerCase()'
            stmt.setString(2, status); 
            stmt.setString(3, alasan);
            
            int berhasil = stmt.executeUpdate();
            
            if (berhasil > 0) {
                JOptionPane.showMessageDialog(this, "Absensi Berhasil Disimpan!");
                loadDataTabel(); // Refresh tabel
                
                // Reset Form
                idKaryawanTF.setText("");
                namaTF.setText("");
                alasanTF.setText("");
                absenBox.setSelectedIndex(0);
                idKaryawanTF.requestFocus();
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal simpan: " + e.getMessage());
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

        jColorChooser1 = new javax.swing.JColorChooser();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        exitBT = new javax.swing.JButton();
        jPanel6 = new fitur.RoundedPanel(30) ;
        lb = new javax.swing.JLabel();
        jPanel1 = new fitur.RoundedPanel(30) ;
        jLabel4 = new javax.swing.JLabel();
        idKaryawanTF = new fitur.RoundedTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        alasanTF = new fitur.RoundedTextField();
        namaTF = new fitur.RoundedTextField();
        submitAbsen = new javax.swing.JButton();
        absenBox = new javax.swing.JComboBox<>();
        jLabel7 = new javax.swing.JLabel();
        tanggalTF1 = new fitur.RoundedTextField();
        jPanel7 = new fitur.RoundedPanel(30) ;
        absensiLB = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableAbsensi = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel3.setBackground(new java.awt.Color(211, 218, 217));

        jLabel10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/absen.png"))); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel10)
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10)
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

        lb.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lb.setForeground(new java.awt.Color(255, 255, 255));
        lb.setText("Absensi");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(lb)
                .addContainerGap(17, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        jPanel1.setBackground(new java.awt.Color(217, 217, 217));

        jLabel4.setText("ID Karyawan");

        idKaryawanTF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                idKaryawanTFActionPerformed(evt);
            }
        });
        idKaryawanTF.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                idKaryawanTFKeyPressed(evt);
            }
        });

        jLabel5.setText("Nama");

        jLabel6.setText("Tanggal");

        alasanTF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                alasanTFActionPerformed(evt);
            }
        });

        namaTF.setBackground(new java.awt.Color(153, 153, 153));
        namaTF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                namaTFActionPerformed(evt);
            }
        });

        submitAbsen.setText("Submit");
        submitAbsen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitAbsenActionPerformed(evt);
            }
        });

        absenBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Hadir", "Izin", "Sakit", "Alpha" }));
        absenBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                absenBoxActionPerformed(evt);
            }
        });

        jLabel7.setText("Alasan");

        tanggalTF1.setBackground(new java.awt.Color(153, 153, 153));
        tanggalTF1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tanggalTF1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(alasanTF)
                            .addComponent(namaTF)
                            .addComponent(idKaryawanTF)
                            .addComponent(tanggalTF1)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel5))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(130, 130, 130)
                                .addComponent(submitAbsen, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(absenBox, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 126, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(idKaryawanTF, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addGap(5, 5, 5)
                .addComponent(namaTF, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tanggalTF1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(absenBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(alasanTF, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(submitAbsen)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel7.setBackground(new java.awt.Color(217, 217, 217));

        absensiLB.setText("Absensi Nama Karyawan");

        tableAbsensi.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "ID Karyawan", "Nama", "Tanggal", "Waktu"
            }
        ));
        jScrollPane1.setViewportView(tableAbsensi);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(absensiLB)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 669, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(absensiLB)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 459, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18))
        );

        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void submitAbsenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitAbsenActionPerformed
        simpanAbsensi(); 
    }//GEN-LAST:event_submitAbsenActionPerformed

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

    private void namaTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_namaTFActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_namaTFActionPerformed

    private void idKaryawanTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_idKaryawanTFActionPerformed
        cariNamaKaryawan(); 
    }//GEN-LAST:event_idKaryawanTFActionPerformed

    private void alasanTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_alasanTFActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_alasanTFActionPerformed

    private void absenBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_absenBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_absenBoxActionPerformed

    private void tanggalTF1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tanggalTF1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tanggalTF1ActionPerformed

    private void idKaryawanTFKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_idKaryawanTFKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_idKaryawanTFKeyPressed

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
        java.awt.EventQueue.invokeLater(() -> new AbsensiPanel().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> absenBox;
    private javax.swing.JLabel absensiLB;
    private javax.swing.JTextField alasanTF;
    private javax.swing.JButton exitBT;
    private javax.swing.JTextField idKaryawanTF;
    private javax.swing.JColorChooser jColorChooser1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lb;
    private javax.swing.JTextField namaTF;
    private javax.swing.JButton submitAbsen;
    private javax.swing.JTable tableAbsensi;
    private javax.swing.JTextField tanggalTF1;
    // End of variables declaration//GEN-END:variables
}
