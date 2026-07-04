package Transaksi;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TransaksiPengolahanLimbahController implements Initializable {

    // ===================== FX FIELDS (sesuai fx:id di FXML) =====================
    @FXML private TextField txtIDPengolahan;
    @FXML private TextField txtIDProduk;
    @FXML private DatePicker jpTanggal;
    @FXML private ComboBox<String> cmbJenisProduk;
    @FXML private TextField txtKuantitas;
    @FXML private ComboBox<String> cmbSatuan;
    @FXML private TextArea txtKeterangan;

    private final DBConnect db = new DBConnect();

    // ===================== KONSTANTA JENIS PRODUK =====================
    private static final String PUPUK_ORGANIK_PADAT      = "Pupuk Organik Padat";
    private static final String PUPUK_ORGANIK_CAIR       = "Pupuk Organik Cair";
    private static final String KOMPOS                   = "Kompos";
    private static final String BOOSTER                  = "Booster";
    private static final String PUPUK_NITROGEN_TINGGI_UDANG = "Pupuk Nitrogen Tinggi Udang";
    private static final String PUPUK_KALSIUM             = "Pupuk Kalsium";

    /**
     * Representasi 1 baris pengurangan bahan limbah untuk 1 unit produk yang dihasilkan.
     * namaBahan  -> nama/kode bahan limbah pada master limbah (sesuaikan dengan nama kolom/ID di tabel kamu)
     * jumlahPerUnit -> jumlah bahan yang berkurang untuk SETIAP 1 unit produk
     * satuanBahan -> satuan bahan limbah (Kg / Liter / dst, mengikuti master limbah)
     */
    private static class BahanReduction {
        final String namaBahan;
        final BigDecimal jumlahPerUnit;
        final String satuanBahan;

        BahanReduction(String namaBahan, BigDecimal jumlahPerUnit, String satuanBahan) {
            this.namaBahan = namaBahan;
            this.jumlahPerUnit = jumlahPerUnit;
            this.satuanBahan = satuanBahan;
        }
    }

    // ===================== ATURAN PENGURANGAN BAHAN LIMBAH =====================
    // 1 produk => sekian bahan limbah yang berkurang dari master limbah
    private static final Map<String, List<BahanReduction>> ATURAN_PENGURANGAN = new HashMap<>();
    static {
        ATURAN_PENGURANGAN.put(PUPUK_ORGANIK_PADAT, Arrays.asList(
                new BahanReduction("Lumpur", new BigDecimal("0.5"), "Kg"),
                new BahanReduction("Kotoran", new BigDecimal("0.5"), "Kg")
        ));
        ATURAN_PENGURANGAN.put(PUPUK_ORGANIK_CAIR, Arrays.asList(
                new BahanReduction("Air Limbah Tambak", new BigDecimal("0.5"), "Liter")
        ));
        ATURAN_PENGURANGAN.put(KOMPOS, Arrays.asList(
                new BahanReduction("Lumpur", new BigDecimal("0.5"), "Kg")
        ));
        ATURAN_PENGURANGAN.put(PUPUK_NITROGEN_TINGGI_UDANG, Arrays.asList(
                new BahanReduction("Kotoran", new BigDecimal("0.5"), "Kg"),
                new BahanReduction("Bangkai Udang", new BigDecimal("0.5"), "Kg")
        ));
        ATURAN_PENGURANGAN.put(PUPUK_KALSIUM, Arrays.asList(
                new BahanReduction("Cangkang", BigDecimal.ONE, "Kg")
        ));
        ATURAN_PENGURANGAN.put(BOOSTER, Arrays.asList(
                new BahanReduction("Cangkang", BigDecimal.ONE, "Kg")
        ));
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupComboJenisProduk();
        setupComboSatuan();
        addNumericOnly(txtKuantitas, 8);

        loadAutoIDPengolahan();
    }

    // ===================== SETUP COMBO BOX =====================

    private void setupComboJenisProduk() {
        cmbJenisProduk.setItems(FXCollections.observableArrayList(
                PUPUK_ORGANIK_PADAT,
                PUPUK_ORGANIK_CAIR,
                KOMPOS,
                BOOSTER,
                PUPUK_NITROGEN_TINGGI_UDANG,
                PUPUK_KALSIUM
        ));
    }

    private void setupComboSatuan() {
        cmbSatuan.setItems(FXCollections.observableArrayList("Botol", "Kg"));
    }

    private void addNumericOnly(TextField field, int maxLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            // izinkan angka desimal (mis. 1.5) selain digit murni
            String filtered = newVal.replaceAll("[^0-9.]", "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    // ===================== AUTO ID (memanggil sp_AutoID_Pengolahan) =====================

    private void loadAutoIDPengolahan() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Pengolahan}");
            if (db.result.next()) {
                txtIDPengolahan.setText(db.result.getString("ID_Pengolahan"));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    /**
     * Dipanggil dari luar (mis. saat baris produk di tabel diklik) untuk mengisi ID Produk.
     * Tabel pemilihan produk belum ada di FXML yang dikirim, jadi method ini disiapkan
     * untuk dipanggil dari listener TableView produk saat sudah tersedia.
     */
    public void setIdProdukTerpilih(String idProduk) {
        txtIDProduk.setText(idProduk);
    }

    // ===================== EVENT: KUANTITAS HASIL DIISI (onAction txtKuantitas) =====================

    @FXML
    private void txtKuantitasHasil() {
        String jenis = cmbJenisProduk.getValue();
        if (jenis == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Pilih Jenis Produk terlebih dahulu.");
            return;
        }
        BigDecimal kuantitas = parseKuantitas();
        if (kuantitas == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Kuantitas Hasil harus berupa angka lebih dari 0.");
            return;
        }

        List<BahanReduction> aturan = ATURAN_PENGURANGAN.get(jenis);
        if (aturan == null || aturan.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("Bahan limbah yang akan berkurang untuk ").append(kuantitas).append(" ").append(jenis).append(":\n");
        for (BahanReduction b : aturan) {
            BigDecimal total = b.jumlahPerUnit.multiply(kuantitas).setScale(2, RoundingMode.HALF_UP);
            sb.append("- ").append(b.namaBahan).append(" : ").append(total).append(" ").append(b.satuanBahan).append("\n");
        }
        showAlert(Alert.AlertType.INFORMATION, "Perkiraan Pengurangan Bahan Limbah", sb.toString());
    }

    private BigDecimal parseKuantitas() {
        try {
            String text = txtKuantitas.getText().trim();
            if (text.isEmpty()) return null;
            BigDecimal val = new BigDecimal(text);
            if (val.compareTo(BigDecimal.ZERO) <= 0) return null;
            return val;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ===================== EVENT: SATUAN DIPILIH (onAction cmbSatuan) =====================

    @FXML
    private void cmbSaatuan() {

        String jenis = cmbSatuan.getValue();

    }

    // ===================== SIMPAN TRANSAKSI PENGOLAHAN LIMBAH =====================

    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;

        String idPengolahan = txtIDPengolahan.getText();
        String idProduk     = txtIDProduk.getText();
        String tanggal      = jpTanggal.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String jenisProduk  = cmbJenisProduk.getValue();
        String kuantitasStr = txtKuantitas.getText().trim();
        String satuan       = cmbSatuan.getValue();
        String keterangan   = txtKeterangan.getText() == null ? "" : txtKeterangan.getText().trim();
        BigDecimal kuantitas = new BigDecimal(kuantitasStr);

        try {
            // 1. Simpan header transaksi pengolahan limbah
            db.cstat = db.conn.prepareCall("{CALL sp_Insert_PengolahanLimbah(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, idPengolahan);
            db.cstat.setString(2, idProduk);
            db.cstat.setString(3, tanggal);
            db.cstat.setString(4, jenisProduk);
            db.cstat.setBigDecimal(5, kuantitas);
            db.cstat.setString(6, satuan);
            db.cstat.setString(7, keterangan);
            db.cstat.executeUpdate();

            // 2. Kurangi stok bahan pada master limbah sesuai aturan jenis produk
            List<BahanReduction> aturan = ATURAN_PENGURANGAN.get(jenisProduk);
            if (aturan != null) {
                for (BahanReduction b : aturan) {
                    BigDecimal totalKurang = b.jumlahPerUnit.multiply(kuantitas);
                    db.cstat = db.conn.prepareCall("{CALL sp_Kurangi_StokLimbah(?,?)}");
                    db.cstat.setString(1, b.namaBahan);
                    db.cstat.setBigDecimal(2, totalKurang);
                    db.cstat.executeUpdate();
                }
            }

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Transaksi pengolahan limbah berhasil disimpan.");
            resetForm();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    private boolean validateForm() {
        if (txtIDProduk.getText() == null || txtIDProduk.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "ID Produk wajib diisi/dipilih.");
            return false;
        }
        if (jpTanggal.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Tanggal wajib diisi.");
            return false;
        }
        if (cmbJenisProduk.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Jenis Produk wajib dipilih.");
            return false;
        }
        if (parseKuantitas() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Kuantitas Hasil harus berupa angka lebih dari 0.");
            return false;
        }
        if (cmbSatuan.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Satuan wajib dipilih.");
            return false;
        }
        return true;
    }

    private void resetForm() {
        txtIDProduk.clear();
        jpTanggal.setValue(null);
        cmbJenisProduk.getSelectionModel().clearSelection();
        txtKuantitas.clear();
        cmbSatuan.getSelectionModel().clearSelection();
        txtKeterangan.clear();
        loadAutoIDPengolahan();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}