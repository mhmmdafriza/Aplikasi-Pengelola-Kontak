import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

public class ContactManagerApp extends javax.swing.JFrame {

    private DefaultTableModel tableModel;

    private boolean isValidPhoneNumber(String phone) {
        return phone.matches("[0-9]+") && (phone.length() == 10 || phone.length() == 12);
    }

      // Fungsi untuk menghapus kontak dari database
    private void deleteContact(int id) {
        String sql = "DELETE FROM contacts WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);  // Menyiapkan ID untuk dihapus
            pstmt.executeUpdate(); // Menghapus kontak dari database
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void exportToCSV() {
        try (FileWriter writer = new FileWriter("contacts.csv")) {
            // Menulis header CSV
            writer.append("ID,Name,Phone,Category\n");

            // Menulis data dari tableModel
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String id = tableModel.getValueAt(i, 0).toString();
                String name = tableModel.getValueAt(i, 1).toString();
                String phone = tableModel.getValueAt(i, 2).toString();
                String category = tableModel.getValueAt(i, 3).toString();
                writer.append(id + "," + name + "," + phone + "," + category + "\n");
            }

            JOptionPane.showMessageDialog(null, "Kontak berhasil diekspor ke contacts.csv!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Terjadi kesalahan saat mengekspor kontak!");
        }
    }

    private void importFromCSV() {
        try (BufferedReader br = new BufferedReader(new FileReader("contacts.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 4) {  // Pastikan data valid
                    String name = data[1];
                    String phone = data[2];
                    String category = data[3];
                    addContact(name, phone, category);  // Menambahkan kontak ke database
                }
            }
            loadContacts();  // Muat ulang kontak setelah impor
            JOptionPane.showMessageDialog(null, "Kontak berhasil diimpor!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Terjadi kesalahan saat mengimpor kontak!");
        }
    }

     public ContactManagerApp() {
        initComponents();
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Phone", "Category"}, 0);
        jTable.setModel(tableModel);
        
        // Memuat kontak setelah inisialisasi
        loadContacts();

        // Tambahkan item ke ComboBox kategori
        jComboBoxCategory.addItem("Keluarga");
        jComboBoxCategory.addItem("Teman");
        jComboBoxCategory.addItem("Kerja");

        // Inisialisasi tableModel dan set ke jTable
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Phone", "Category"}, 0);
        jTable.setModel(tableModel);

        // Load initial contacts
        loadContacts();

        // Tambahkan ActionListener ke tombol "Tambah Kontak"
        jButtonAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Ambil teks dari field input
                String name = jTextFieldName.getText();
                String phone = jTextFieldPhone.getText();
                // Ambil kategori yang dipilih dari combo box
                String category = jComboBoxCategory.getSelectedItem().toString();

                // Validasi nomor telepon
                if (isPhoneValid(phone)) {
                    // Menambahkan kontak jika valid
                    addContact(name, phone, category);
                    // Muat ulang data kontak ke tabel
                    loadContacts();
                } else {
                    // Tampilkan pesan kesalahan jika nomor telepon tidak valid
                    JOptionPane.showMessageDialog(null, "Nomor telepon tidak valid!");
                }
            }
        });

        jButtonImport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importFromCSV();  // Memanggil fungsi impor
            }
        });

        jButtonExport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportToCSV();  // Memanggil fungsi ekspor ke CSV
            }
        });

        // Tambahkan ActionListener untuk tombol "Edit Kontak"
        jButtonEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = jTable.getSelectedRow();
                if (selectedRow != -1) {
                    int id = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
                    String name = jTextFieldName.getText();
                    String phone = jTextFieldPhone.getText();
                    String category = jComboBoxCategory.getSelectedItem().toString();
                    updateContact(id, name, phone, category);
                    loadContacts();
                }
            }
        });

        // Event listener untuk tombol Hapus Kontak
        jButtonDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = jTable.getSelectedRow();  // Mendapatkan baris yang dipilih
                if (selectedRow != -1) {
                    // Mendapatkan ID kontak yang dipilih
                    int id = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());

                    // Menghapus kontak dengan ID yang dipilih
                    deleteContact(id);

                    // Memuat ulang kontak setelah penghapusan
                    loadContacts();  // Pastikan tabel diperbarui dengan data terbaru

                    // Reset pemilihan baris setelah penghapusan
                    jTable.clearSelection(); // Reset pemilihan
                } else {
                    JOptionPane.showMessageDialog(null, "Pilih kontak yang ingin dihapus!");  // Pesan jika tidak ada baris yang dipilih
                }
            }
        });


        jTextFieldPhone.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                // Hanya izinkan angka dan pastikan panjangnya tidak lebih dari 13 digit
                char c = evt.getKeyChar();
                if (!(Character.isDigit(c)) || jTextFieldPhone.getText().length() >= 13) {
                    evt.consume();  // Menghentikan input jika tidak valid
                }
            }
        });

        // Tambahkan ActionListener untuk tombol "Cari Kontak"
        jButtonSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = jTextFieldName.getText();
                searchContact(name);
            }
        });
    }

    // Koneksi ke database SQLite
    public Connection connect() {
        try {
            // Memuat driver SQLite
            Class.forName("org.sqlite.JDBC");
            // Membuat dan mengembalikan koneksi ke database
            Connection conn = DriverManager.getConnection("jdbc:sqlite:contacts.db");
            return conn;
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS contacts ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "phone TEXT NOT NULL,"
                + "category TEXT NOT NULL);";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Menambahkan kontak baru
    private void addContact(String name, String phone, String category) {
        String sql = "INSERT INTO contacts(name, phone, category) VALUES(?,?,?)";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, category);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

     // Fungsi untuk memuat data ke dalam tabel
    private void loadContacts() {
        String sql = "SELECT * FROM contacts";
        tableModel.setRowCount(0);  // Menghapus data lama di tabel
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Menambahkan setiap baris data ke dalam tabel
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("category")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Memperbarui kontak yang sudah ada
    private void updateContact(int id, String name, String phone, String category) {
        String sql = "UPDATE contacts SET name = ?, phone = ?, category = ? WHERE id = ?";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, category);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Mencari kontak berdasarkan nama
    private void searchContact(String name) {
        String sql = "SELECT * FROM contacts WHERE name LIKE ?";
        tableModel.setRowCount(0);  // Reset tabel sebelum memuat hasil pencarian
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("category")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    // Validasi nomor telepon (hanya angka dengan panjang 10-13 digit)
    private boolean isPhoneValid(String phone) {
        return phone.matches("\\d{10,13}");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1Layout = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldName = new javax.swing.JTextField();
        jTextFieldPhone = new javax.swing.JTextField();
        jComboBoxCategory = new javax.swing.JComboBox<>();
        jButtonAdd = new javax.swing.JButton();
        jButtonEdit = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jButtonSearch = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable = new javax.swing.JTable();
        jButtonExport = new javax.swing.JButton();
        jButtonImport = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Nama");

        jLabel2.setText("Nomor Telepon");

        jLabel3.setText("Kategori");

        jTextFieldName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldNameActionPerformed(evt);
            }
        });

        jTextFieldPhone.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextFieldPhoneKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextFieldPhoneKeyTyped(evt);
            }
        });

        jButtonAdd.setText("Tambah kontak");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
            }
        });

        jButtonEdit.setText("Edit kontak");

        jButtonDelete.setText("Hapus Kontak");
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        jButtonSearch.setText("Cari Kontak");
        jButtonSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSearchActionPerformed(evt);
            }
        });

        jTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable);

        jButtonExport.setText("Export");
        jButtonExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExportActionPerformed(evt);
            }
        });

        jButtonImport.setText("Import");

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        jLabel4.setText("APLIKASI PENGELOLA KONTAK");

        jLabel5.setFont(new java.awt.Font("Times New Roman", 1, 16)); // NOI18N
        jLabel5.setText("Nama : Muhammad Afriza Rizqi Pramudya");

        jLabel6.setFont(new java.awt.Font("Times New Roman", 1, 16)); // NOI18N
        jLabel6.setText("Npm : 2210010679");

        jLabel7.setFont(new java.awt.Font("Times New Roman", 1, 16)); // NOI18N
        jLabel7.setText("Kelas : 5B Reguler Pagi Banjarmasin");

        javax.swing.GroupLayout jPanel1LayoutLayout = new javax.swing.GroupLayout(jPanel1Layout);
        jPanel1Layout.setLayout(jPanel1LayoutLayout);
        jPanel1LayoutLayout.setHorizontalGroup(
            jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1LayoutLayout.createSequentialGroup()
                .addGroup(jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel1LayoutLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jScrollPane1))
                        .addGroup(jPanel1LayoutLayout.createSequentialGroup()
                            .addGap(28, 28, 28)
                            .addGroup(jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel2)
                                .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING))
                            .addGap(92, 92, 92)
                            .addGroup(jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jTextFieldName, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)
                                .addComponent(jTextFieldPhone)
                                .addComponent(jComboBoxCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(jPanel1LayoutLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jButtonAdd)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButtonEdit)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButtonSearch)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButtonDelete)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButtonExport)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButtonImport)))
                    .addGroup(jPanel1LayoutLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7)))
                    .addGroup(jPanel1LayoutLayout.createSequentialGroup()
                        .addGap(176, 176, 176)
                        .addComponent(jLabel4)))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        jPanel1LayoutLayout.setVerticalGroup(
            jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1LayoutLayout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel4)
                .addGap(32, 32, 32)
                .addGroup(jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jComboBoxCategory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(28, 28, 28)
                .addGroup(jPanel1LayoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonSearch)
                    .addComponent(jButtonDelete)
                    .addComponent(jButtonExport)
                    .addComponent(jButtonImport)
                    .addComponent(jButtonEdit)
                    .addComponent(jButtonAdd))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 269, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addContainerGap(84, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1Layout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1Layout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        jPanel1Layout = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldName = new javax.swing.JTextField();
        jTextFieldPhone = new javax.swing.JTextField();
        jComboBoxCategory = new javax.swing.JComboBox<>();
        jButtonAdd = new javax.swing.JButton();
        jButtonEdit = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jButtonSearch = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("Nama");
        jLabel2.setText("Nomor Telepon");
        jLabel3.setText("Kategori");
        jButtonAdd.setText("Tambah kontak");
        jButtonEdit.setText("Edit kontak");
        jButtonDelete.setText("Hapus Kontak");
        jButtonSearch.setText("Cari Kontak");

        jTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{
                    {null, null, null, null},
                    {null, null, null, null},
                    {null, null, null, null},
                    {null, null, null, null}
                },
                new String[]{
                    "ID", "Name", "Phone", "Category"
                }
        ));
        jScrollPane1.setViewportView(jTable);


    }//GEN-LAST:event_jButtonDeleteActionPerformed

    private void jTextFieldNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldNameActionPerformed

    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButtonAddActionPerformed

    private void jButtonSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSearchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButtonSearchActionPerformed

    private void jTextFieldPhoneKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldPhoneKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldPhoneKeyPressed

    private void jTextFieldPhoneKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldPhoneKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldPhoneKeyTyped

    private void jButtonExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExportActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButtonExportActionPerformed

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
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ContactManagerApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ContactManagerApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ContactManagerApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ContactManagerApp.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ContactManagerApp().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonEdit;
    private javax.swing.JButton jButtonExport;
    private javax.swing.JButton jButtonImport;
    private javax.swing.JButton jButtonSearch;
    private javax.swing.JComboBox<String> jComboBoxCategory;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1Layout;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable;
    private javax.swing.JTextField jTextFieldName;
    private javax.swing.JTextField jTextFieldPhone;
    // End of variables declaration//GEN-END:variables
}
