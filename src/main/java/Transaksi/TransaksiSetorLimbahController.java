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

    // ── Data Jenis Limbah dari Master Limbah (menggantikan konstanta hardcode) ──
    private final List<LimbahItem> limbahList = new ArrayList<>();

    // ── Daftar Keterangan (komposisi) dari Master Produk, dipakai untuk
    //    menyaring Jenis Limbah supaya hanya yang benar-benar dipakai
    //    sebagai bahan baku produk yang muncul di combobox. ──
    private final List<String> keteranganProdukList = new ArrayList<>();

    private BigDecimal totalTransaksi = BigDecimal.ZERO;
    private static final String RUPIAH_PREFIX = "Rp ";

    /**
     * Model ringan untuk satu baris data Master Limbah.
     * NOTE: sesuaikan nama stored procedure & nama kolom di loadDataLimbah()
     * dengan yang sebenarnya dipakai pada Master Limbah kamu jika berbeda.
     */
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

        // Muat data referensi dari database SEBELUM combobox kategori disusun,
        // supaya updateJenisLimbahCombo() punya data untuk difilter.
        loadDataLimbah();
        loadKeteranganProduk();

        setupComboJenis();
        addNumericOnly(txtJumlah, 10);

        loadAutoIDTransaksi();
        loadDataNasabah();

        // Panel detail nonaktif di awal
        setDetailPanelEnabled(false);

        // ── Listener: cek kelengkapan header setiap kali ada perubahan ──
        tbNasabah.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtIDNasabah.setText(newVal.getIdNasabah());
                cekKelengkapanHeader();
            }
        });

        // Saat ID Karyawan diketik
        txtIDKaryawan.textProperty().addListener((obs, oldVal, newVal) -> cekKelengkapanHeader());

        // Saat tanggal dipilih
        dpTanggal.valueProperty().addListener((obs, oldVal, newVal) -> cekKelengkapanHeader());

        // Hitung subtotal otomatis
        txtJumlah.textProperty().addListener((obs, oldVal, newVal) -> hitungSubTotal());

        cmbJenis.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateJenisLimbahCombo();
            hitungSubTotal();
        });

        // Saat user (kategori Padat) mengganti pilihan Jenis Limbah,
        // satuan & harga per unit ikut menyesuaikan data limbah yang dipilih.
        cmbJenisLimbah.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateLabelSatuanDanHarga();
            hitungSubTotal();
        });
    }

    // ===================== CEK KELENGKAPAN HEADER =====================

    /**
     * Panel detail hanya aktif jika ketiga field header sudah terisi:
     * ID Nasabah (pilih dari tabel), ID Karyawan (ketik), dan Tanggal (pilih).
     */
    private void cekKelengkapanHeader() {
        boolean nasabahTerisi   = !txtIDNasabah.getText().trim().isEmpty();
        boolean karyawanTerisi  = !txtIDKaryawan.getText().trim().isEmpty();
        boolean tanggalTerpilih = dpTanggal.getValue() != null;

        boolean headerLengkap = nasabahTerisi && karyawanTerisi && tanggalTerpilih;

        if (headerLengkap) {
            // Isi ID Setor Limbah di panel detail dan generate ID Detail pertama
            txtIDSetorLimbahDetail.setText(txtIDTransaksi.getText());
            // Load ID Detail hanya jika panel baru saja diaktifkan (detailList kosong)
            if (detailList.isEmpty()) {
                loadAutoIDDetail();
            }
            setDetailPanelEnabled(true);
        } else {
            setDetailPanelEnabled(false);
        }
    }

    // ===================== SETUP =====================

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

    /**
     * Aktif/nonaktifkan seluruh panel Input Detail Limbah.
     * Saat nonaktif, semua field berwarna abu-abu (JavaFX default disabled style).
     */
    private void setDetailPanelEnabled(boolean enabled) {
        txtIDDetail.setDisable(!enabled);
        txtIDSetorLimbahDetail.setDisable(!enabled);
        cmbJenis.setDisable(!enabled);
        txtJumlah.setDisable(!enabled);
        txtKeteranganDetail.setDisable(!enabled);
        txtSubTotal.setDisable(!enabled);
        btnTambahTransaksi.setDisable(!enabled);

        if (enabled) {
            // Reapply aturan aktif/kunci cmbJenisLimbah sesuai kategori Padat/Cair yang aktif
            updateJenisLimbahCombo();
        } else {
            // Panel keseluruhan memang nonaktif (belum ada header) -> boleh abu-abu total
            cmbJenisLimbah.setDisable(true);
            cmbJenisLimbah.setMouseTransparent(true);
            cmbJenisLimbah.setFocusTraversable(false);
        }
    }

    /**
     * Isi ulang combobox Jenis Limbah berdasarkan kategori yang dipilih
     * (Padat/Cair), diambil dari data Master Limbah dan disaring supaya
     * hanya menampilkan jenis limbah yang benar-benar dipakai sebagai
     * komposisi di Master Produk.
     *
     * Kategori CAIR  -> combobox terkunci pada satu-satunya pilihan.
     *                   Sengaja TIDAK memakai setDisable(true) supaya teks
     *                   tetap terang (tidak abu-abu); dikunci lewat
     *                   mouseTransparent + focusTraversable agar user tidak
     *                   bisa membuka dropdown maupun mengetik.
     * Kategori PADAT -> combobox aktif penuh, user bebas memilih.
     */
    private void updateJenisLimbahCombo() {
        boolean isPadat = "Padat".equalsIgnoreCase(cmbJenis.getValue());
        String satuanTarget = isPadat ? "kg" : "liter";

        List<String> namaJenisLimbah = new ArrayList<>();
        for (LimbahItem li : limbahList) {
            boolean satuanCocok = li.satuan != null && li.satuan.trim().equalsIgnoreCase(satuanTarget);
            boolean dipakaiDiProduk = dipakaiDiProduk(li.jenisLimbah);
            if (satuanCocok && dipakaiDiProduk) {
                namaJenisLimbah.add(li.jenisLimbah);
            }
        }

        cmbJenisLimbah.setItems(FXCollections.observableArrayList(namaJenisLimbah));

        if (!namaJenisLimbah.isEmpty()) {
            cmbJenisLimbah.getSelectionModel().selectFirst();
        } else {
            cmbJenisLimbah.getSelectionModel().clearSelection();
        }

        if (isPadat) {
            cmbJenisLimbah.setDisable(false);
            cmbJenisLimbah.setMouseTransparent(false);
            cmbJenisLimbah.setFocusTraversable(true);
        } else {
            // "Nonaktif" secara fungsional, tapi tampilan tetap terang.
            cmbJenisLimbah.setDisable(false);
            cmbJenisLimbah.setMouseTransparent(true);
            cmbJenisLimbah.setFocusTraversable(false);
        }

        updateLabelSatuanDanHarga();
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

            LimbahItem terpilih = getLimbahTerpilih();
            BigDecimal harga = (terpilih != null && terpilih.harga != null) ? terpilih.harga : BigDecimal.ZERO;

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
            String idSetor     = txtIDSetorLimbahDetail.getText(); // TIDAK berubah
            String jenis       = cmbJenisLimbah.getValue(); // simpan JENIS LIMBAH spesifik, bukan hanya kategori
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

            // Tambahkan ke tabel lokal
            detailList.add(new DetailSetorLimbah(idDetail, idSetor, jenis, jumlah, satuan, keterangan, subTotal));
            hitungTotalTransaksi();
            btnSelesai.setDisable(false);

            // Hanya bersihkan input detail & generate ID Detail baru
            // ID Setor Limbah (txtIDSetorLimbahDetail) TIDAK diubah
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

    /** Hanya bersihkan field input (Jumlah, Keterangan, SubTotal).
     *  Kategori & Jenis Limbah tetap mengikuti pilihan terakhir supaya
     *  user bisa cepat input beberapa limbah dengan kategori yang sama.
     *  ID Detail dan ID Setor Limbah dibiarkan — ID Detail akan di-refresh oleh loadAutoIDDetail(). */
    private void clearInputDetail() {
        txtJumlah.clear();
        txtKeteranganDetail.clear();
        txtSubTotal.clear();
    }

    // ===================== SELESAI (simpan header transaksi) =====================

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

    // ===================== BATAL =====================

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

        // Muat ulang data referensi (siapa tahu Master Limbah / Master Produk
        // berubah sejak transaksi terakhir dibuka) dan reset kategori ke default.
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