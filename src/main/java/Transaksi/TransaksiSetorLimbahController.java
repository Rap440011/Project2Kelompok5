package Transaksi;

import Connection.DBConnect;
import Master.MasterNasabah;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class TransaksiSetorLimbahController implements Initializable {

    // ---- Header Transaksi ----
    @FXML private TextField txtIDTransaksi;
    @FXML private TextField txtIDNasabah;
    @FXML private TextField txtIDKaryawan;
    @FXML private DatePicker dpTanggal;
    @FXML private TextField txtTotal;

    @FXML private TextField txtCariNasabah;
    @FXML private Button btnBatalTransaksi;
    @FXML private Button btnSelesai;

    @FXML private TableView<MasterNasabah> tbNasabah;
    @FXML private TableColumn<MasterNasabah, String> clmNasabahNama, clmNasabahHP, clmNasabahSaldo;

    // ---- Detail Transaksi ----
    @FXML private TextField txtIDDetail;
    @FXML private TextField txtIDSetorLimbahDetail;
    @FXML private ComboBox<String> cmbJenis;
    @FXML private ComboBox<String> cmbJenisLimbah;
    @FXML private TextField txtJumlah;
    @FXML private Label lblSatuan;
    @FXML private TextArea txtKeteranganDetail;
    @FXML private TextField txtSubTotal;
    @FXML private Button btnTambahTransaksi;

    @FXML private TableView<DetailSetorLimbah> tbDetail;
    @FXML private TableColumn<DetailSetorLimbah, String> clmDetailID, clmDetailJenis, clmDetailJumlah;
    @FXML private TableColumn<DetailSetorLimbah, String> clmDetailSatuan, clmDetailKeterangan, clmDetailSubTotal;

    private final ObservableList<MasterNasabah> nasabahList = FXCollections.observableArrayList();
    private final ObservableList<DetailSetorLimbah> detailList = FXCollections.observableArrayList();
    private final DBConnect db = new DBConnect();

    private final List<LimbahItem> limbahList = new ArrayList<>();

    private final List<String> keteranganProdukList = new ArrayList<>();

    private BigDecimal totalTransaksi = BigDecimal.ZERO;
    private static final String RUPIAH_PREFIX = "Rp ";

    private static final java.util.Map<String, BigDecimal> HARGA_LIMBAH;
    static {
        HARGA_LIMBAH = new java.util.HashMap<>();
        HARGA_LIMBAH.put("Cangkang Udang",    new BigDecimal("10000"));
        HARGA_LIMBAH.put("Endapan / Lumpur",  new BigDecimal("2000"));
        HARGA_LIMBAH.put("Kotoran Udang",     new BigDecimal("5000"));
        HARGA_LIMBAH.put("Bangkai Udang",     new BigDecimal("2500"));
        HARGA_LIMBAH.put("Air Limbah Tambak", new BigDecimal("2500"));
    }

    private static class LimbahItem {
        final String idLimbah, jenisLimbah, satuan, keterangan;
        final BigDecimal harga;
        LimbahItem(String idLimbah, String jenisLimbah, String satuan, BigDecimal harga, String keterangan) {
            this.idLimbah = idLimbah;
            this.jenisLimbah = jenisLimbah;
            this.satuan = satuan;
            this.harga = harga;
            this.keterangan = keterangan;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTabelNasabah();
        setupTabelDetail();

        loadDataLimbah();
        loadKeteranganProduk();

        setupComboJenis();
        addNumericOnly(txtJumlah, 10);

        loadAutoIDTransaksi();
        loadDataNasabah();

        setDetailPanelEnabled(false);

        tbNasabah.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtIDNasabah.setText(newVal.getIdNasabah());
                cekKelengkapanHeader();
            }
        });

        txtIDKaryawan.textProperty().addListener((obs, oldVal, newVal) -> cekKelengkapanHeader());

        dpTanggal.valueProperty().addListener((obs, oldVal, newVal) -> cekKelengkapanHeader());

        txtJumlah.textProperty().addListener((obs, oldVal, newVal) -> hitungSubTotal());

        cmbJenis.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateJenisLimbahCombo();
            hitungSubTotal();
        });

        cmbJenisLimbah.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateLabelSatuanDanHarga();
            hitungSubTotal();
        });
    }

    private void cekKelengkapanHeader() {
        boolean nasabahTerisi   = !txtIDNasabah.getText().trim().isEmpty();
        boolean karyawanTerisi  = !txtIDKaryawan.getText().trim().isEmpty();
        boolean tanggalTerpilih = dpTanggal.getValue() != null;

        boolean headerLengkap = nasabahTerisi && karyawanTerisi && tanggalTerpilih;

        if (headerLengkap) {

            txtIDSetorLimbahDetail.setText(txtIDTransaksi.getText());

            if (detailList.isEmpty()) {
                loadAutoIDDetail();
            }
            setDetailPanelEnabled(true);
        } else {
            setDetailPanelEnabled(false);
        }
    }

    private void setupTabelNasabah() {
        clmNasabahNama.setCellValueFactory(new PropertyValueFactory<>("namaNasabah"));
        clmNasabahHP.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        clmNasabahSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));
    }

    private void setupTabelDetail() {
        clmDetailID.setCellValueFactory(new PropertyValueFactory<>("idDetail"));
        clmDetailJenis.setCellValueFactory(new PropertyValueFactory<>("jenis"));
        clmDetailJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        clmDetailSatuan.setCellValueFactory(new PropertyValueFactory<>("satuan"));
        clmDetailKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));
        clmDetailSubTotal.setCellValueFactory(new PropertyValueFactory<>("subTotal"));
        tbDetail.setItems(detailList);
    }

    private void setupComboJenis() {
        cmbJenis.setItems(FXCollections.observableArrayList("Cair", "Padat"));
        cmbJenis.getSelectionModel().selectFirst();
        updateJenisLimbahCombo();
    }


    private void setDetailPanelEnabled(boolean enabled) {
        txtIDDetail.setDisable(!enabled);
        txtIDSetorLimbahDetail.setDisable(!enabled);
        cmbJenis.setDisable(!enabled);
        txtJumlah.setDisable(!enabled);
        txtKeteranganDetail.setDisable(!enabled);
        txtSubTotal.setDisable(!enabled);
        btnTambahTransaksi.setDisable(!enabled);

        if (enabled) {
            updateJenisLimbahCombo();
        } else {
            cmbJenisLimbah.setDisable(true);
            cmbJenisLimbah.setMouseTransparent(true);
            cmbJenisLimbah.setFocusTraversable(false);
        }
    }

    private void updateJenisLimbahCombo() {
        boolean isPadat = "Padat".equalsIgnoreCase(cmbJenis.getValue());

        if (isPadat) {
            cmbJenisLimbah.setItems(FXCollections.observableArrayList(
                    "Cangkang Udang",
                    "Endapan / Lumpur",
                    "Kotoran Udang",
                    "Bangkai Udang"
            ));
            cmbJenisLimbah.getSelectionModel().selectFirst();

            cmbJenisLimbah.setDisable(false);
            cmbJenisLimbah.setMouseTransparent(false);
            cmbJenisLimbah.setFocusTraversable(true);

            lblSatuan.setText("Kg");

        } else {
            cmbJenisLimbah.setItems(FXCollections.observableArrayList("Air Limbah Tambak"));
            cmbJenisLimbah.getSelectionModel().selectFirst();

            cmbJenisLimbah.setDisable(false);
            cmbJenisLimbah.setMouseTransparent(true);
            cmbJenisLimbah.setFocusTraversable(false);

            lblSatuan.setText("Liter");
        }

        hitungSubTotal();
    }

    /**
     * Cek apakah suatu jenis limbah dipakai sebagai bahan baku (komposisi)
     * di salah satu produk pada Master Produk. Keterangan produk berisi
     * daftar bahan dipisah koma, mis. "Lumpur, Kotoran".
     */
    private boolean dipakaiDiProduk(String namaJenisLimbah) {
        if (namaJenisLimbah == null) return false;
        String target = namaJenisLimbah.trim().toLowerCase(Locale.ROOT);

        for (String keterangan : keteranganProdukList) {
            if (keterangan == null) continue;
            for (String bahan : keterangan.split(",")) {
                if (bahan.trim().toLowerCase(Locale.ROOT).equals(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Update label satuan dan harga per unit mengikuti Jenis Limbah yang terpilih saat ini. */
    private void updateLabelSatuanDanHarga() {
        LimbahItem terpilih = getLimbahTerpilih();
        if (terpilih != null) {
            lblSatuan.setText(terpilih.satuan);
        } else {
            // Fallback sebelum data limbah tersedia / belum ada pilihan cocok
            lblSatuan.setText("Padat".equalsIgnoreCase(cmbJenis.getValue()) ? "Kg" : "Liter");
        }
    }

    /** Ambil data LimbahItem yang sedang dipilih di cmbJenisLimbah, atau null jika tidak ada. */
    private LimbahItem getLimbahTerpilih() {
        String namaTerpilih = cmbJenisLimbah.getValue();
        if (namaTerpilih == null) return null;
        for (LimbahItem li : limbahList) {
            if (li.jenisLimbah != null && li.jenisLimbah.equalsIgnoreCase(namaTerpilih)) {
                return li;
            }
        }
        return null;
    }

    private void addNumericOnly(TextField field, int maxLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9]", "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    // ===================== AUTO ID =====================

    private void loadAutoIDTransaksi() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_SetorLimbah}");
            if (db.result.next()) txtIDTransaksi.setText(db.result.getString("ID_Setor"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    private void loadAutoIDDetail() {
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_AutoID_DetailSetorLimbah(?)}");
            db.cstat.setString(1, txtIDTransaksi.getText());
            db.result = db.cstat.executeQuery();
            if (db.result.next()) txtIDDetail.setText(db.result.getString("ID_Detail_Setor"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    // ===================== LOAD DATA REFERENSI (Master Limbah & Master Produk) =====================

    /**
     * Memuat data Master Limbah untuk mengisi combobox Jenis Limbah.
     * ASUMSI nama stored procedure & kolom mengikuti pola Master Produk
     * (ID_Limbah, Jenis_Limbah, Satuan, Harga, Keterangan). Sesuaikan
     * nama SP "sp_SelectAll_Limbah" dan nama kolom di bawah ini jika
     * berbeda dengan yang ada di database kamu.
     */
    private void loadDataLimbah() {
        limbahList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                limbahList.add(new LimbahItem(
                        db.result.getString("ID_Limbah"),
                        db.result.getString("Jenis_Limbah"),
                        db.result.getString("Satuan"),
                        db.result.getBigDecimal("Harga"),
                        db.result.getString("Keterangan")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data Limbah", e.getMessage());
        }
    }

    /**
     * Memuat kolom Keterangan (komposisi) dari seluruh produk di Master
     * Produk, dipakai untuk menyaring Jenis Limbah supaya combobox hanya
     * menampilkan jenis limbah yang benar-benar dipakai sebagai bahan baku.
     */
    private void loadKeteranganProduk() {
        keteranganProdukList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Produk}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                keteranganProdukList.add(db.result.getString("Keterangan"));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data Produk", e.getMessage());
        }
    }

    // ===================== LOAD / CARI NASABAH =====================

    private void loadDataNasabah() {
        nasabahList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Nasabah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                nasabahList.add(new MasterNasabah(
                    db.result.getString("ID_Nasabah"),
                    db.result.getString("Nama_Nasabah"),
                    db.result.getString("No_HP"),
                    db.result.getString("RT"),
                    db.result.getString("RW"),
                    db.result.getString("Kelurahan"),
                    db.result.getString("Kecamatan"),
                    db.result.getString("Kabupaten"),
                    db.result.getString("Provinsi"),
                    db.result.getString("No_Rekening"),
                    RUPIAH_PREFIX + db.result.getBigDecimal("Saldo").stripTrailingZeros().toPlainString(),
                    db.result.getString("Bank")
                ));
            }
            tbNasabah.setItems(nasabahList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data", e.getMessage());
        }
    }

    private void cariDataNasabah(String keyword) {
        nasabahList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Search_Nasabah(?)}");
            db.cstat.setString(1, keyword);
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                nasabahList.add(new MasterNasabah(
                    db.result.getString("ID_Nasabah"),
                    db.result.getString("Nama_Nasabah"),
                    db.result.getString("No_HP"),
                    db.result.getString("RT"),
                    db.result.getString("RW"),
                    db.result.getString("Kelurahan"),
                    db.result.getString("Kecamatan"),
                    db.result.getString("Kabupaten"),
                    db.result.getString("Provinsi"),
                    db.result.getString("No_Rekening"),
                    db.result.getString("Saldo"),
                    db.result.getString("Bank")
                ));
            }
            tbNasabah.setItems(nasabahList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Cari Data", e.getMessage());
        }
    }

    @FXML
    private void handleCariNasabah() {
        String keyword = txtCariNasabah.getText().trim();
        if (keyword.isEmpty()) loadDataNasabah();
        else cariDataNasabah(keyword);
    }

    // ===================== HITUNG SUB TOTAL & TOTAL =====================

    private void hitungSubTotal() {
        try {
            String jumlahText = txtJumlah.getText().trim();
            if (jumlahText.isEmpty()) { txtSubTotal.setText(""); return; }

            BigDecimal jumlah = new BigDecimal(jumlahText);

            String jenisTerpilih = cmbJenisLimbah.getValue();
            BigDecimal harga = (jenisTerpilih != null)
                    ? HARGA_LIMBAH.getOrDefault(jenisTerpilih, BigDecimal.ZERO)
                    : BigDecimal.ZERO;

            txtSubTotal.setText(jumlah.multiply(harga).toPlainString());
        } catch (NumberFormatException e) {
            txtSubTotal.setText("");
        }
    }

    private void hitungTotalTransaksi() {
        totalTransaksi = BigDecimal.ZERO;
        for (DetailSetorLimbah d : detailList) {
            try { totalTransaksi = totalTransaksi.add(new BigDecimal(d.getSubTotal())); }
            catch (NumberFormatException ignored) {}
        }
        txtTotal.setText(totalTransaksi.toPlainString());
    }

    // ===================== TAMBAH TRANSAKSI (simpan satu detail) =====================

    @FXML
    private void handleTambahTransaksi() {
        if (!validateDetailForm()) return;
        try {
            String idDetail    = txtIDDetail.getText();
            String idSetor     = txtIDSetorLimbahDetail.getText();
            String jenis       = cmbJenisLimbah.getValue();
            String jumlah      = txtJumlah.getText().trim();
            String satuan      = lblSatuan.getText();
            String keterangan  = txtKeteranganDetail.getText().trim();
            String subTotal    = txtSubTotal.getText().trim();

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_DetailSetorLimbah(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, idDetail);
            db.cstat.setString(2, idSetor);
            db.cstat.setString(3, jenis);
            db.cstat.setBigDecimal(4, new BigDecimal(jumlah));
            db.cstat.setString(5, satuan);
            db.cstat.setString(6, keterangan);
            db.cstat.setBigDecimal(7, new BigDecimal(subTotal));
            db.cstat.executeUpdate();

            detailList.add(new DetailSetorLimbah(idDetail, idSetor, jenis, jumlah, satuan, keterangan, subTotal));
            hitungTotalTransaksi();
            btnSelesai.setDisable(false);

            clearInputDetail();
            loadAutoIDDetail();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Tambah Transaksi", e.getMessage());
        }
    }

    private boolean validateDetailForm() {
        if (txtIDNasabah.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih nasabah terlebih dahulu.");
            return false;
        }
        if (cmbJenis.getValue() == null || cmbJenisLimbah.getValue() == null
                || txtJumlah.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Kategori, Jenis Limbah, dan Jumlah wajib diisi!");
            return false;
        }
        return true;
    }

    private void clearInputDetail() {
        txtJumlah.clear();
        txtKeteranganDetail.clear();
        txtSubTotal.clear();
    }

    @FXML
    private void handleSelesai() {
        if (detailList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Tambahkan minimal satu detail transaksi terlebih dahulu.");
            return;
        }
        try {
            String tanggal = dpTanggal.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_SetorLimbah(?,?,?,?,?)}");
            db.cstat.setString(1, txtIDTransaksi.getText());
            db.cstat.setString(2, txtIDNasabah.getText());
            db.cstat.setString(3, txtIDKaryawan.getText());
            db.cstat.setString(4, tanggal);
            db.cstat.setBigDecimal(5, totalTransaksi);
            db.cstat.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Transaksi setor limbah berhasil disimpan.");
            resetSemua();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Selesai", e.getMessage());
        }
    }

    @FXML
    private void handleBatalTransaksi() {
        boolean adaIsi = !txtIDNasabah.getText().trim().isEmpty() || !detailList.isEmpty();
        if (!adaIsi) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Tidak ada data yang perlu dibatalkan.");
            return;
        }
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin membatalkan transaksi ini?\nDetail yang sudah ditambahkan tetap tersimpan di database.",
                ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Batal");
        konfirmasi.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) resetSemua(); });
    }

    private void resetSemua() {
        txtIDNasabah.clear();
        txtIDKaryawan.clear();
        dpTanggal.setValue(null);
        txtTotal.clear();
        totalTransaksi = BigDecimal.ZERO;

        txtIDDetail.clear();
        txtIDSetorLimbahDetail.clear();
        detailList.clear();
        clearInputDetail();

        loadDataLimbah();
        loadKeteranganProduk();
        cmbJenis.getSelectionModel().selectFirst();
        updateJenisLimbahCombo();

        setDetailPanelEnabled(false);
        btnSelesai.setDisable(true);

        tbNasabah.getSelectionModel().clearSelection();
        txtCariNasabah.clear();
        loadDataNasabah();
        loadAutoIDTransaksi();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}