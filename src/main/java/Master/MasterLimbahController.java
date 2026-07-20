package Master;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class MasterLimbahController implements Initializable {

    @FXML private TableView<MasterLimbah> tbLimbah;
    @FXML private TableColumn<MasterLimbah, String> clmID, clmNama, clmKategori, clmSatuan, clmJumlah, clmHarga, clmKeterangan;

    @FXML private TextField  txtID, txtNama, txtHarga, txtJumlah;
    @FXML private TextField  txtCari;
    @FXML private TextArea   txtketerangan;
    @FXML private ComboBox<String> cmbjenis;

    // Tambahan: referensi tombol untuk kontrol disable/enable
    @FXML private Button btnUbah, btnHapus, btnSimpan;

    private final ObservableList<MasterLimbah> dataList = FXCollections.observableArrayList();
    private final DBConnect db = new DBConnect();

    // Tambahan: guard flag agar listener format Rupiah tidak infinite loop
    private boolean isFormatting = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tbLimbah.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        clmID.setCellValueFactory(new PropertyValueFactory<>("idLimbah"));
        clmNama.setCellValueFactory(new PropertyValueFactory<>("namaLimbah"));
        clmKategori.setCellValueFactory(new PropertyValueFactory<>("jenisLimbah"));
        clmSatuan.setCellValueFactory(new PropertyValueFactory<>("satuan"));
        clmJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        clmHarga.setCellValueFactory(new PropertyValueFactory<>("harga"));
        clmKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));

        cmbjenis.setItems(FXCollections.observableArrayList("Padat", "Cair"));
        cmbjenis.getSelectionModel().selectFirst();

        // Satuan tidak bisa diedit manual & tampil abu-abu
        txtJumlah.setEditable(false);
        txtJumlah.setStyle("-fx-background-color:#F5F5F5; -fx-opacity:1; " +
                "-fx-border-color:#E0E0E0; -fx-border-radius:6; -fx-background-radius:6; -fx-font-size:12px;");

        // Satuan otomatis mengikuti kategori limbah
        cmbjenis.valueProperty().addListener((obs, oldVal, newVal) -> updateSatuanOtomatis(newVal));
        updateSatuanOtomatis(cmbjenis.getValue());

        addRupiahFormat(txtHarga); // Diubah: dari addNumericOnly menjadi format Rupiah otomatis
        addAlphaOnly(txtNama);

        loadAutoID();
        loadData();

        tbLimbah.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtID.setText(newVal.getIdLimbah());
                txtNama.setText(newVal.getNamaLimbah());
                cmbjenis.setValue(newVal.getJenisLimbah());
                txtJumlah.setText(newVal.getSatuan()); // menampilkan satuan (Kg/Liter)
                txtHarga.setText(newVal.getHarga());
                txtketerangan.setText(newVal.getKeterangan());
            }
            updateButtonStates(); // Tambahan: update status tombol setiap seleksi tabel berubah
        });

        // Tambahan: listener untuk memantau perubahan input form agar tombol Simpan bereaksi otomatis
        txtNama.textProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        txtHarga.textProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        cmbjenis.valueProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());

        // Tambahan: set status awal tombol saat form pertama kali dibuka
        updateButtonStates();
    }

    /**
     * Tambahan: Mengatur status aktif/nonaktif tombol Simpan, Ubah, dan Hapus.
     * - Simpan  : aktif jika Nama, Kategori, dan Harga terisi, DAN tidak ada baris tabel yang sedang dipilih.
     * - Ubah    : aktif jika ada baris tabel yang sedang dipilih.
     * - Hapus   : aktif jika ada baris tabel yang sedang dipilih.
     * - Batal   : tidak diatur di sini, tetap selalu aktif seperti semula.
     */
    private void updateButtonStates() {
        boolean formValid = !txtNama.getText().trim().isEmpty()
                && cmbjenis.getValue() != null && !cmbjenis.getValue().trim().isEmpty()
                && !txtHarga.getText().trim().isEmpty();

        boolean rowSelected = tbLimbah.getSelectionModel().getSelectedItem() != null;

        btnSimpan.setDisable(!formValid || rowSelected);
        btnUbah.setDisable(!rowSelected);
        btnHapus.setDisable(!rowSelected);
    }

    /** Menentukan satuan otomatis: Cair -> Liter, Padat -> Kg */
    private void updateSatuanOtomatis(String kategori) {
        if (kategori == null) return;
        txtJumlah.setText(kategori.equalsIgnoreCase("Cair") ? "Liter" : "Kg");
    }

    private void addAlphaOnly(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^a-zA-Z ]", "");
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    /**
     * Tambahan: Memformat input TextField menjadi format Rupiah dengan pemisah ribuan
     * (titik) secara otomatis saat user mengetik, contoh: "10000000" -> "10.000.000".
     * Hanya menerima digit angka, sama seperti addNumericOnly sebelumnya, tapi
     * sekarang langsung menyisipkan separator ribuan secara live.
     */
    private void addRupiahFormat(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isFormatting) return;

            String digitsOnly = newVal.replaceAll("[^0-9]", "");
            String formatted = formatRibuan(digitsOnly);

            if (!formatted.equals(newVal)) {
                isFormatting = true;
                field.setText(formatted);
                field.positionCaret(formatted.length());
                isFormatting = false;
            }
        });
    }

    /**
     * Tambahan: Mengubah string digit murni menjadi format dengan pemisah ribuan (titik).
     * Contoh: "2000" -> "2.000", "10000000" -> "10.000.000".
     * Angka nol di depan (kecuali jika hanya "0") ikut dibuang.
     */
    private String formatRibuan(String digitsOnly) {
        if (digitsOnly.isEmpty()) return "";
        digitsOnly = digitsOnly.replaceFirst("^0+(?=\\d)", "");
        return digitsOnly.replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".");
    }

    /**
     * Mengubah nilai Jumlah (FLOAT) menjadi String tanpa notasi ilmiah dan tanpa
     * angka nol berlebih di belakang koma, mis. 12.50 -> "12.5", 10.00 -> "10".
     */
    private String formatJumlah(double jumlah) {
        BigDecimal bd = BigDecimal.valueOf(jumlah).stripTrailingZeros();
        if (bd.scale() < 0) bd = bd.setScale(0, RoundingMode.HALF_UP);
        return bd.toPlainString();
    }

    /**
     * Mengubah nilai Harga (DECIMAL) menjadi String tanpa 2 angka desimal di belakang koma,
     * mis. 2000.00 -> "2000", 10000.00 -> "10000", lalu diformat dengan pemisah ribuan
     * menjadi "2.000", "10.000", dst. untuk ditampilkan di tabel maupun form.
     */
    private String formatHarga(double harga) {
        BigDecimal bd = BigDecimal.valueOf(harga).setScale(0, RoundingMode.HALF_UP);
        return formatRibuan(bd.toPlainString());
    }

    private void loadAutoID() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Limbah}");
            if (db.result.next()) txtID.setText(db.result.getString("ID_Limbah"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    /**
     * Menyeragamkan data lama: satuan "Kilo" (apapun huruf besar/kecilnya) ditampilkan
     * sebagai "Kg". Satuan lain (mis. "Liter") ditampilkan apa adanya.
     */
    private static String normalizeSatuan(String satuan) {
        if (satuan == null) return satuan;
        return "Kilo".equalsIgnoreCase(satuan.trim()) ? "Kg" : satuan;
    }

    private void loadData() {
        dataList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                dataList.add(new MasterLimbah(
                        db.result.getString("ID_Limbah"),
                        db.result.getString("Nama_Limbah"),
                        db.result.getString("Kategori"),
                        normalizeSatuan(db.result.getString("Satuan")),
                        formatHarga(db.result.getDouble("Harga")),
                        db.result.getString("Keterangan"),
                        formatJumlah(db.result.getDouble("Jumlah"))
                ));
            }
            tbLimbah.setItems(dataList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data", e.getMessage());
        }
    }

    private void cariData(String keyword) {
        dataList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Search_Limbah(?)}");
            db.cstat.setString(1, keyword);
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                dataList.add(new MasterLimbah(
                        db.result.getString("ID_Limbah"),
                        db.result.getString("Nama_Limbah"),
                        db.result.getString("Kategori"),
                        normalizeSatuan(db.result.getString("Satuan")),
                        formatHarga(db.result.getDouble("Harga")),
                        db.result.getString("Keterangan"),
                        formatJumlah(db.result.getDouble("Jumlah"))
                ));
            }
            tbLimbah.setItems(dataList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Cari Data", e.getMessage());
        }
    }

    @FXML
    private void handleCari() {
        String keyword = txtCari.getText().trim();
        if (keyword.isEmpty()) {
            loadData();
        } else {
            cariData(keyword);
        }
    }

    @FXML
    private void handleUbah() {
        if (txtID.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin diubah terlebih dahulu.");
            return;
        }
        if (!validateForm()) return;
        try {
            // sp_Update_Limbah: ID, Nama, Kategori, Jumlah, Satuan, Harga, Keterangan
            db.cstat = db.conn.prepareCall("{CALL sp_Update_Limbah(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText());
            db.cstat.setString(3, cmbjenis.getValue());
            db.cstat.setDouble(4, 0); // Jumlah tidak diubah lewat form ini (dikelola via transaksi)
            db.cstat.setString(5, txtJumlah.getText()); // Satuan otomatis
            // Diubah: buang titik pemisah ribuan sebelum di-parse ke BigDecimal
            db.cstat.setBigDecimal(6, new java.math.BigDecimal(txtHarga.getText().trim().replace(".", "")));
            db.cstat.setString(7, txtketerangan.getText());
            db.cstat.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data limbah berhasil diubah.");
            clearForm();
            loadData();
            loadAutoID();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Ubah", e.getMessage());
        }
    }

    @FXML
    private void handleHapus() {
        if (txtID.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin dihapus terlebih dahulu.");
            return;
        }
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin menghapus limbah ID: " + txtID.getText() + "?",
                ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    db.cstat = db.conn.prepareCall("{CALL sp_Delete_Limbah(?)}");
                    db.cstat.setString(1, txtID.getText());
                    db.cstat.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data limbah berhasil dihapus.");
                    clearForm();
                    loadData();
                    loadAutoID();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Error Hapus", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBatal() {
        boolean adaIsi = !txtNama.getText().trim().isEmpty()      ||
                cmbjenis.getValue() != null && !cmbjenis.getValue().isEmpty() ||
                !txtHarga.getText().trim().isEmpty()       ||
                !txtketerangan.getText().trim().isEmpty();

        if (!adaIsi) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Tidak ada data yang perlu dibatalkan.");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin membatalkan dan mengosongkan semua input?",
                ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Batal");
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                clearForm();
                loadAutoID();
            }
        });
    }

    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;
        try {
            // sp_Insert_Limbah: ID, Nama, Kategori, Jumlah, Satuan, Harga, Keterangan
            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Limbah(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText().trim());
            db.cstat.setString(3, cmbjenis.getValue());
            db.cstat.setDouble(4, 0);                        // Jumlah default 0
            db.cstat.setString(5, txtJumlah.getText());      // Satuan otomatis (Liter/Kg)
            // Diubah: buang titik pemisah ribuan sebelum di-parse ke BigDecimal
            db.cstat.setBigDecimal(6, new java.math.BigDecimal(txtHarga.getText().trim().replace(".", "")));
            db.cstat.setString(7, txtketerangan.getText().trim().isEmpty()
                    ? null : txtketerangan.getText().trim());
            db.cstat.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data limbah berhasil disimpan.");
            clearForm();
            loadData();
            loadAutoID();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error Format", "Harga harus berupa angka valid.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    private boolean validateForm() {
        if (txtNama.getText().trim().isEmpty()    ||
                cmbjenis.getValue()              == null  ||
                txtHarga.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Semua data harus diisi!");
            return false;
        }

        // Tambahan: tentukan apakah sedang mode Ubah (ada baris tabel yang dipilih)
        // atau mode Tambah baru. Pada mode Ubah, data milik limbah yang sedang
        // diedit sendiri dikecualikan dari pengecekan duplikat.
        MasterLimbah limbahDipilih = tbLimbah.getSelectionModel().getSelectedItem();
        String idDikecualikan = limbahDipilih != null ? limbahDipilih.getIdLimbah() : null;

        // Tambahan: validasi Nama Limbah tidak boleh sama dengan data lain
        if (namaLimbahSudahAda(txtNama.getText(), idDikecualikan)) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Nama Limbah sudah digunakan, gunakan nama lain.");
            return false;
        }

        return true;
    }

    /**
     * Tambahan: mengecek apakah Nama Limbah sudah dipakai oleh data limbah lain
     * (perbandingan case-insensitive supaya "Cangkang" dan "cangkang" dianggap sama).
     * @param nama nama limbah yang akan disimpan/diubah
     * @param idDikecualikan ID limbah yang sedang diedit (diabaikan dari pengecekan), null jika mode tambah baru
     */
    private boolean namaLimbahSudahAda(String nama, String idDikecualikan) {
        String namaBaru = nama.trim();
        for (MasterLimbah l : dataList) {
            if (idDikecualikan != null && l.getIdLimbah().equals(idDikecualikan)) {
                continue; // lewati data limbah yang sedang diedit
            }
            if (l.getNamaLimbah() != null && l.getNamaLimbah().trim().equalsIgnoreCase(namaBaru)) {
                return true;
            }
        }
        return false;
    }

    private void clearForm() {
        txtNama.clear();
        txtHarga.clear();
        txtketerangan.clear();
        cmbjenis.getSelectionModel().selectFirst();
        updateSatuanOtomatis(cmbjenis.getValue());
        tbLimbah.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}