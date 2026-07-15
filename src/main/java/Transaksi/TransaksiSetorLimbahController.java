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
    @FXML private TextField txtNamaNasabah; // menampilkan Nama Nasabah, bukan ID
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

    /**
     * ID Nasabah yang sesungguhnya dipilih (dipakai untuk disimpan ke database).
     * txtNamaNasabah hanya menampilkan nama nasabah untuk kemudahan baca,
     * jadi ID-nya harus disimpan terpisah di sini.
     */
    private String idNasabahTerpilih = null;

    /** Status nasabah yang boleh tampil / dipakai bertransaksi. */
    private static final String STATUS_AKTIF = "Aktif";

    /** Style abu-abu untuk field yang tidak bisa diedit (readonly), konsisten dengan MasterLimbahController. */
    private static final String STYLE_READONLY =
            "-fx-background-color:#F5F5F5; -fx-opacity:1; -fx-border-color:#E0E0E0; " +
                    "-fx-border-radius:6; -fx-background-radius:6; -fx-font-size:12px;";

    private static class LimbahItem {
        final String idLimbah, namaLimbah, jenisLimbah, satuan, keterangan;
        final BigDecimal harga;
        LimbahItem(String idLimbah, String namaLimbah, String jenisLimbah, String satuan,
                   BigDecimal harga, String keterangan) {
            this.idLimbah = idLimbah;
            this.namaLimbah = namaLimbah;
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

        applyReadonlyStyles();
        setDetailPanelEnabled(false);

        tbNasabah.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                idNasabahTerpilih = newVal.getIdNasabah();
                txtNamaNasabah.setText(newVal.getNamaNasabah());
                cekKelengkapanHeader();
            }
        });

        txtIDKaryawan.textProperty().addListener((obs, oldVal, newVal) -> cekKelengkapanHeader());

        dpTanggal.valueProperty().addListener((obs, oldVal, newVal) -> cekKelengkapanHeader());

        txtJumlah.textProperty().addListener((obs, oldVal, newVal) -> hitungSubTotal());

        cmbJenis.valueProperty().addListener((obs, oldVal, newVal) -> updateJenisLimbahCombo());

        cmbJenisLimbah.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateLabelSatuanDanHarga();
            hitungSubTotal();
        });
    }

    /**
     * Menandai field-field yang bersifat readonly (editable="false") dengan
     * gaya abu-abu, mengikuti pola styling txtJumlah di MasterLimbahController.
     * Style emphasis yang sudah ada (bold/warna teks) tetap dipertahankan.
     */
    private void applyReadonlyStyles() {
        txtIDTransaksi.setStyle(STYLE_READONLY);
        txtNamaNasabah.setStyle(STYLE_READONLY);
        txtIDDetail.setStyle(STYLE_READONLY);
        txtIDSetorLimbahDetail.setStyle(STYLE_READONLY);
        txtTotal.setStyle(STYLE_READONLY + "-fx-font-weight:bold;-fx-text-fill:#1B5E20;");
        txtSubTotal.setStyle(STYLE_READONLY + "-fx-font-weight:bold;-fx-text-fill:#1565C0;");
    }

    private void cekKelengkapanHeader() {
        boolean nasabahTerisi   = idNasabahTerpilih != null && !idNasabahTerpilih.trim().isEmpty();
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
        }
    }

    /**
     * Mengisi combo "Nama Limbah" berdasarkan kategori (Padat/Cair) yang
     * sedang dipilih di cmbJenis. Data diambil dari Master Limbah (limbahList),
     * bukan daftar statis, sehingga otomatis mengikuti data di database.
     */
    private void updateJenisLimbahCombo() {
        boolean isPadat = "Padat".equalsIgnoreCase(cmbJenis.getValue());
        String kategoriTerpilih = isPadat ? "Padat" : "Cair";

        ObservableList<String> namaLimbahFiltered = FXCollections.observableArrayList();
        for (LimbahItem li : limbahList) {
            if (li.jenisLimbah != null && li.jenisLimbah.equalsIgnoreCase(kategoriTerpilih)) {
                namaLimbahFiltered.add(li.namaLimbah);
            }
        }

        cmbJenisLimbah.setItems(namaLimbahFiltered);

        if (!namaLimbahFiltered.isEmpty()) {
            cmbJenisLimbah.getSelectionModel().selectFirst();
        } else {
            cmbJenisLimbah.setValue(null);
        }

        cmbJenisLimbah.setDisable(false);
        cmbJenisLimbah.setMouseTransparent(false);
        cmbJenisLimbah.setFocusTraversable(true);

        updateLabelSatuanDanHarga();
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

    /** Update label satuan mengikuti Nama Limbah yang terpilih saat ini (data dari Master Limbah). */
    private void updateLabelSatuanDanHarga() {
        LimbahItem terpilih = getLimbahTerpilih();
        if (terpilih != null) {
            lblSatuan.setText(terpilih.satuan);
        } else {
            // Fallback sebelum data limbah tersedia / belum ada pilihan cocok
            lblSatuan.setText("Padat".equalsIgnoreCase(cmbJenis.getValue()) ? "Kg" : "Liter");
        }
    }

    /** Ambil data LimbahItem yang sedang dipilih di cmbJenisLimbah (berdasarkan Nama Limbah), atau null jika tidak ada. */
    private LimbahItem getLimbahTerpilih() {
        String namaTerpilih = cmbJenisLimbah.getValue();
        if (namaTerpilih == null) return null;
        for (LimbahItem li : limbahList) {
            if (li.namaLimbah != null && li.namaLimbah.equalsIgnoreCase(namaTerpilih)) {
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
     * Memuat data Master Limbah (ID_Limbah, Nama_Limbah, Kategori, Satuan,
     * Harga, Keterangan) untuk mengisi combobox Nama Limbah, difilter
     * menurut kategori (Padat/Cair) yang dipilih pengguna.
     */
    private void loadDataLimbah() {
        limbahList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                limbahList.add(new LimbahItem(
                        db.result.getString("ID_Limbah"),
                        db.result.getString("Nama_Limbah"),
                        db.result.getString("Kategori"),
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

    /** Hanya menampilkan nasabah dengan status Aktif. */
    private boolean isNasabahAktif(String status) {
        return status != null && status.trim().equalsIgnoreCase(STATUS_AKTIF);
    }

    private void loadDataNasabah() {
        nasabahList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Nasabah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                if (!isNasabahAktif(db.result.getString("Status"))) continue;

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
                        db.result.getString("Bank"),
                        db.result.getString("Status")
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
                if (!isNasabahAktif(db.result.getString("Status"))) continue;

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
                        db.result.getString("Bank"),
                        db.result.getString("Status")
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

    // ===================== TAMBAH TRANSAKSI (susun satu baris detail — BELUM disimpan ke DB) =====================

    @FXML
    private void handleTambahTransaksi() {
        if (!validateDetailForm()) return;

        String idDetail    = txtIDDetail.getText();
        String idSetor     = txtIDSetorLimbahDetail.getText();
        String namaLimbah  = cmbJenisLimbah.getValue();
        String jumlah      = txtJumlah.getText().trim();
        String satuan      = lblSatuan.getText();
        String keterangan  = txtKeteranganDetail.getText().trim();
        String subTotal    = txtSubTotal.getText().trim();

        // PENTING: baris detail TIDAK di-insert ke database di sini.
        // dtl_tr_Setor_Limbah punya FOREIGN KEY ke tr_Setor_Limbah.ID_Setor, sedangkan
        // header (tr_Setor_Limbah) baru benar-benar disimpan saat tombol "Selesai" diklik
        // (lihat handleSelesai()). Kalau detail di-insert duluan di sini, ID_Setor-nya
        // belum ada di tabel induk -> INSERT ditolak (FOREIGN KEY constraint violation).
        // Jadi detail hanya disusun di memori (detailList) dulu, dan semuanya baru
        // dikirim ke database sekaligus setelah header berhasil disimpan.
        detailList.add(new DetailSetorLimbah(idDetail, idSetor, namaLimbah, jumlah, satuan, keterangan, subTotal));
        hitungTotalTransaksi();
        btnSelesai.setDisable(false);

        clearInputDetail();

        // Generate ID Detail berikutnya secara LOKAL (bukan query ulang ke database),
        // karena selama header belum tersimpan, dtl_tr_Setor_Limbah juga belum berisi
        // baris apa pun untuk ID_Setor ini -> sp_AutoID_DetailSetorLimbah akan selalu
        // mengembalikan urutan yang sama kalau dipanggil berulang di titik ini.
        String idDetailBerikutnya = idDetailBerikutnyaLokal(idDetail);
        if (idDetailBerikutnya != null) {
            txtIDDetail.setText(idDetailBerikutnya);
        } else {
            // fallback: kalau format ID tidak sesuai dugaan, tetap coba ambil dari DB
            loadAutoIDDetail();
        }
    }

    /**
     * Increment 2 digit terakhir dari ID Detail saat ini (format: ...NN, hasil
     * sp_AutoID_DetailSetorLimbah) tanpa perlu query ulang ke database. Dipakai selama
     * beberapa baris detail disusun di memori sebelum header (dan detail-detailnya)
     * benar-benar di-INSERT ke database saat "Selesai".
     */
    private String idDetailBerikutnyaLokal(String idSaatIni) {
        if (idSaatIni == null || idSaatIni.length() < 2) return null;
        try {
            String prefix = idSaatIni.substring(0, idSaatIni.length() - 2);
            int urut = Integer.parseInt(idSaatIni.substring(idSaatIni.length() - 2)) + 1;
            return prefix + String.format("%02d", urut);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Cek apakah ID_Karyawan yang diketik user benar-benar terdaftar di tb_Karyawan.
     * Diperlukan karena txtIDKaryawan adalah TextField bebas ketik (bukan pilihan dari
     * daftar), sedangkan tr_Setor_Limbah.ID_Karyawan punya FOREIGN KEY ke tb_Karyawan.
     * Tanpa validasi ini, ID yang salah ketik baru ketahuan lewat error SQL mentah saat
     * INSERT (FOREIGN KEY constraint violation).
     */
    private boolean validasiKaryawanAda(String idKaryawan) {
        if (idKaryawan == null || idKaryawan.trim().isEmpty()) return false;
        try (java.sql.PreparedStatement ps = db.conn.prepareStatement(
                "SELECT COUNT(1) AS Jumlah FROM tb_Karyawan WHERE ID_Karyawan = ?")) {
            ps.setString(1, idKaryawan.trim());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("Jumlah") > 0;
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Validasi Karyawan", e.getMessage());
        }
        return false;
    }

    private boolean validateDetailForm() {
        if (idNasabahTerpilih == null || idNasabahTerpilih.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih nasabah terlebih dahulu.");
            return false;
        }
        if (cmbJenis.getValue() == null || cmbJenisLimbah.getValue() == null
                || txtJumlah.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Kategori, Nama Limbah, dan Jumlah wajib diisi!");
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
        String idKaryawan = txtIDKaryawan.getText().trim();
        if (!validasiKaryawanAda(idKaryawan)) {
            showAlert(Alert.AlertType.WARNING, "Validasi",
                    "ID Karyawan '" + idKaryawan + "' tidak ditemukan di Master Karyawan.\n" +
                            "Periksa kembali ID yang diketik.");
            return;
        }
        try {
            String tanggal = dpTanggal.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);

            // 1) Simpan HEADER lebih dulu — dtl_tr_Setor_Limbah.ID_Setor (FOREIGN KEY)
            //    hanya bisa diisi kalau baris ini sudah ada di tr_Setor_Limbah.
            db.cstat = db.conn.prepareCall("{CALL sp_Insert_SetorLimbah(?,?,?,?,?)}");
            db.cstat.setString(1, txtIDTransaksi.getText());
            db.cstat.setString(2, idNasabahTerpilih);
            db.cstat.setString(3, txtIDKaryawan.getText());
            db.cstat.setString(4, tanggal);
            db.cstat.setBigDecimal(5, totalTransaksi);
            db.cstat.executeUpdate();

            // 2) Baru setelah header ada, simpan seluruh baris detail yang sudah disusun.
            //    ID_Setor sama untuk semua baris dalam transaksi ini, jadi ambil langsung
            //    dari txtIDTransaksi (tidak bergantung pada getter idSetor di DetailSetorLimbah).
            String idSetorHeader = txtIDTransaksi.getText();
            for (DetailSetorLimbah d : detailList) {
                db.cstat = db.conn.prepareCall("{CALL sp_Insert_DetailSetorLimbah(?,?,?,?,?,?,?)}");
                db.cstat.setString(1, d.getIdDetail());
                db.cstat.setString(2, idSetorHeader);
                db.cstat.setString(3, d.getJenis());
                db.cstat.setBigDecimal(4, new BigDecimal(d.getJumlah()));
                db.cstat.setString(5, d.getSatuan());
                db.cstat.setString(6, d.getKeterangan());
                db.cstat.setBigDecimal(7, new BigDecimal(d.getSubTotal()));
                db.cstat.executeUpdate();
            }

            // 3) Saldo nasabah bertambah sesuai total transaksi setor limbah.
            db.cstat = db.conn.prepareCall("{CALL sp_Tambah_SaldoNasabah(?,?)}");
            db.cstat.setString(1, idNasabahTerpilih);
            db.cstat.setBigDecimal(2, totalTransaksi);
            db.cstat.executeUpdate();

            // 4) Stok tiap jenis limbah di Master Limbah bertambah sesuai jumlah yang disetor.
            for (DetailSetorLimbah d : detailList) {
                db.cstat = db.conn.prepareCall("{CALL sp_Tambah_StokLimbah(?,?)}");
                db.cstat.setString(1, d.getJenis());
                db.cstat.setInt(2, Integer.parseInt(d.getJumlah()));
                db.cstat.executeUpdate();
            }

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Transaksi setor limbah berhasil disimpan.");
            resetSemua();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Selesai", e.getMessage());
        }
    }

    @FXML
    private void handleBatalTransaksi() {
        boolean adaIsi = (idNasabahTerpilih != null && !idNasabahTerpilih.trim().isEmpty()) || !detailList.isEmpty();
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
        idNasabahTerpilih = null;
        txtNamaNasabah.clear();
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