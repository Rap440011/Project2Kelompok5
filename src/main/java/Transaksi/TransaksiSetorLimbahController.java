package Transaksi;

import Auth.Session;
import Connection.DBConnect;
import Master.MasterNasabah;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class TransaksiSetorLimbahController implements Initializable {

    // ---- Header Transaksi ----
    @FXML private TextField txtIDTransaksi;
    @FXML private TextField txtNamaNasabah;
    @FXML private TextField txtTanggal;
    @FXML private TextField txtTotal;

    @FXML private TextField txtCariNasabah;
    @FXML private Button btnBatalTransaksi;
    @FXML private Button btnSelesai;

    @FXML private TableView<MasterNasabah> tbNasabah;
    @FXML private TableColumn<MasterNasabah, String> clmNasabahNama, clmNasabahHP, clmNasabahSaldo;

    // ---- Detail Transaksi ----
    @FXML private TextField txtIDSetorLimbahDetail;
    @FXML private ComboBox<String> cmbJenis;
    @FXML private ComboBox<String> cmbJenisLimbah;
    @FXML private TextField txtJumlah;
    @FXML private Label lblSatuan;
    @FXML private TextArea txtKeteranganDetail;
    @FXML private TextField txtSubTotal;
    @FXML private Button btnTambahTransaksi;

    @FXML private TableView<DetailSetorLimbah> tbDetail;
    @FXML private TableColumn<DetailSetorLimbah, String> clmDetailJenis, clmDetailJumlah;
    @FXML private TableColumn<DetailSetorLimbah, String> clmDetailSatuan, clmDetailKeterangan, clmDetailSubTotal;

    private final ObservableList<MasterNasabah> nasabahList = FXCollections.observableArrayList();
    private final ObservableList<DetailSetorLimbah> detailList = FXCollections.observableArrayList();
    private final DBConnect db = new DBConnect();

    private final List<LimbahItem> limbahList = new ArrayList<>();
    private final List<String> keteranganProdukList = new ArrayList<>();

    /** Total keseluruhan setor limbah pada transaksi ini (jumlah sub total semua detail yang SUDAH ditambahkan). */
    private BigDecimal totalTransaksi = BigDecimal.ZERO;

    /** Sub total yang sedang diketik di form detail (belum ditambahkan ke tabel detail). */
    private BigDecimal subTotalHitung = BigDecimal.ZERO;

    /** Saldo nasabah terpilih SEBELUM transaksi ini (disimpan untuk keperluan lain, tidak dipakai di preview Total header). */
    private BigDecimal saldoNasabahTerpilih = BigDecimal.ZERO;

    private static final String RUPIAH_PREFIX = "Rp ";

    private String idNasabahTerpilih = null;

    private LocalDate tanggalTransaksi;

    private static final String STATUS_AKTIF = "Aktif";

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
        setTanggalOtomatis();

        applyReadonlyStyles();
        setDetailPanelEnabled(false);

        tbNasabah.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            // Jaga-jaga: hanya nasabah berstatus Aktif yang boleh mengaktifkan panel Input Detail Limbah,
            // walaupun tabel di kanan sudah difilter hanya menampilkan nasabah Aktif.
            if (!isNasabahAktif(newVal.getStatus())) {
                showAlert(Alert.AlertType.WARNING, "Nasabah Tidak Aktif",
                        "Nasabah \"" + newVal.getNamaNasabah() + "\" berstatus tidak aktif. " +
                                "Input Detail Limbah tidak dapat diaktifkan.");
                idNasabahTerpilih = null;
                txtNamaNasabah.clear();
                setDetailPanelEnabled(false);
                return;
            }

            idNasabahTerpilih = newVal.getIdNasabah();
            txtNamaNasabah.setText(newVal.getNamaNasabah());
            saldoNasabahTerpilih = parseRupiah(newVal.getSaldo());
            // Nasabah aktif sudah terpilih -> panel Input Detail Limbah diaktifkan (lihat cekKelengkapanHeader()).
            cekKelengkapanHeader();
            updateTampilanTotal();
        });

        txtJumlah.textProperty().addListener((obs, oldVal, newVal) -> hitungSubTotal());

        cmbJenis.valueProperty().addListener((obs, oldVal, newVal) -> updateJenisLimbahCombo());

        cmbJenisLimbah.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateLabelSatuanDanHarga();
            hitungSubTotal();
        });
    }

    private void setTanggalOtomatis() {
        tanggalTransaksi = LocalDate.now();
        txtTanggal.setText(tanggalTransaksi.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private static String formatRupiah(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        DecimalFormat df = new DecimalFormat("#,###", new DecimalFormatSymbols(new Locale("in", "ID")));
        return df.format(value.setScale(0, RoundingMode.HALF_UP));
    }

    /**
     * Menyeragamkan nama satuan tampilan: "Kilo" (apapun huruf besar/kecilnya)
     * ditampilkan sebagai "Kg". Satuan lain ditampilkan apa adanya.
     */
    private static String normalizeSatuan(String satuan) {
        if (satuan == null) return satuan;
        return "Kilo".equalsIgnoreCase(satuan.trim()) ? "Kg" : satuan;
    }

    /**
     * Hanya satuan "Kg" dan "Liter" yang diizinkan dipakai pada transaksi ini.
     * Nilai yang dikirim ke sini HARUS sudah melalui normalizeSatuan() terlebih dahulu
     * (supaya "Kilo" sudah menjadi "Kg" sebelum dicek).
     */
    private static boolean isSatuanDiizinkan(String satuanTernormalisasi) {
        return "Kg".equalsIgnoreCase(satuanTernormalisasi) || "Liter".equalsIgnoreCase(satuanTernormalisasi);
    }

    /**
     * Mengubah teks nominal (mis. "47.500") menjadi BigDecimal.
     * Dipakai untuk membaca kembali nilai saldo nasabah yang ditampilkan di tabel nasabah.
     */
    private BigDecimal parseRupiah(String rupiahText) {
        if (rupiahText == null) return BigDecimal.ZERO;
        String cleaned = rupiahText.replace(RUPIAH_PREFIX, "").replace(".", "").trim();
        if (cleaned.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Menampilkan Total (Rp) di header sebagai akumulasi Sub Total pada panel
     * Input Detail Limbah: total detail yang sudah "Tambah ke Transaksi"
     * (totalTransaksi) ditambah subtotal yang sedang diketik saat ini
     * (subTotalHitung, live preview sebelum diklik tombol tambah).
     */
    private void updateTampilanTotal() {
        BigDecimal tampil = totalTransaksi.add(subTotalHitung);
        txtTotal.setText(formatRupiah(tampil));
    }

    private void applyReadonlyStyles() {
        txtIDTransaksi.setStyle(STYLE_READONLY);
        txtNamaNasabah.setStyle(STYLE_READONLY);
        txtTanggal.setStyle(STYLE_READONLY);
        txtIDSetorLimbahDetail.setStyle(STYLE_READONLY);
        txtTotal.setStyle(STYLE_READONLY + "-fx-font-weight:bold;-fx-text-fill:#1B5E20;");
        txtSubTotal.setStyle(STYLE_READONLY + "-fx-font-weight:bold;-fx-text-fill:#1565C0;");
    }

    /**
     * Panel "Input Detail Limbah" aktif cukup dengan memilih nasabah (Aktif) dari tabel.
     * Validasi ID Karyawan (Session) tetap dilakukan terpisah saat tombol "Selesai dan Simpan
     * Transaksi" ditekan (lihat handleSelesai() / validasiKaryawanAda()), sehingga panel tidak
     * ikut terkunci hanya karena Session belum sempat terisi.
     */
    private void cekKelengkapanHeader() {
        boolean nasabahTerisi = idNasabahTerpilih != null && !idNasabahTerpilih.trim().isEmpty();

        if (nasabahTerisi) {
            txtIDSetorLimbahDetail.setText(txtIDTransaksi.getText());
            setDetailPanelEnabled(true);
        } else {
            setDetailPanelEnabled(false);
        }
    }

    private void setupTabelNasabah() {
        clmNasabahNama.setCellValueFactory(new PropertyValueFactory<>("namaNasabah"));

        clmNasabahHP.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        clmNasabahHP.setCellFactory(col -> new TableCell<MasterNasabah, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        clmNasabahSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));
        clmNasabahSaldo.setCellFactory(col -> new TableCell<MasterNasabah, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }

    private void setupTabelDetail() {
        clmDetailJenis.setCellValueFactory(new PropertyValueFactory<>("jenis"));

        clmDetailJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        clmDetailJumlah.setCellFactory(col -> new TableCell<DetailSetorLimbah, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        clmDetailSatuan.setCellValueFactory(new PropertyValueFactory<>("satuan"));
        clmDetailSatuan.setCellFactory(col -> new TableCell<DetailSetorLimbah, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : normalizeSatuan(item));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        clmDetailKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));
        clmDetailKeterangan.setCellFactory(col -> new TableCell<DetailSetorLimbah, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-alignment: CENTER-LEFT;");
            }
        });

        clmDetailSubTotal.setCellValueFactory(new PropertyValueFactory<>("subTotal"));
        clmDetailSubTotal.setCellFactory(col -> new TableCell<DetailSetorLimbah, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                } else {
                    try {
                        setText(formatRupiah(new BigDecimal(item)));
                    } catch (NumberFormatException e) {
                        setText(item);
                    }
                }
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        tbDetail.setItems(detailList);
    }

    private void setupComboJenis() {
        cmbJenis.setItems(FXCollections.observableArrayList("Cair", "Padat"));
        cmbJenis.getSelectionModel().selectFirst();
        updateJenisLimbahCombo();
    }

    private void setDetailPanelEnabled(boolean enabled) {
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

    private void updateLabelSatuanDanHarga() {
        LimbahItem terpilih = getLimbahTerpilih();
        if (terpilih != null) {
            lblSatuan.setText(normalizeSatuan(terpilih.satuan));
        } else {
            lblSatuan.setText("Padat".equalsIgnoreCase(cmbJenis.getValue()) ? "Kg" : "Liter");
        }
    }

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

    // ===================== LOAD DATA REFERENSI =====================

    private void loadDataLimbah() {
        limbahList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                // "Kilo" diseragamkan menjadi "Kg"; satuan selain Kg/Liter tidak dipakai di transaksi ini.
                String satuan = normalizeSatuan(db.result.getString("Satuan"));
                if (!isSatuanDiizinkan(satuan)) continue;

                limbahList.add(new LimbahItem(
                        db.result.getString("ID_Limbah"),
                        db.result.getString("Nama_Limbah"),
                        db.result.getString("Kategori"),
                        satuan,
                        db.result.getBigDecimal("Harga"),
                        db.result.getString("Keterangan")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data Limbah", e.getMessage());
        }
    }

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
                        formatRupiah(db.result.getBigDecimal("Saldo")),
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
                        formatRupiah(db.result.getBigDecimal("Saldo")),
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
            if (jumlahText.isEmpty()) {
                txtSubTotal.setText("");
                subTotalHitung = BigDecimal.ZERO;
                updateTampilanTotal();
                return;
            }

            BigDecimal jumlah = new BigDecimal(jumlahText);

            LimbahItem terpilih = getLimbahTerpilih();
            BigDecimal harga = (terpilih != null && terpilih.harga != null) ? terpilih.harga : BigDecimal.ZERO;

            subTotalHitung = jumlah.multiply(harga);
            txtSubTotal.setText(formatRupiah(subTotalHitung));
        } catch (NumberFormatException e) {
            txtSubTotal.setText("");
            subTotalHitung = BigDecimal.ZERO;
        }
        updateTampilanTotal();
    }

    private void hitungTotalTransaksi() {
        totalTransaksi = BigDecimal.ZERO;
        for (DetailSetorLimbah d : detailList) {
            try { totalTransaksi = totalTransaksi.add(new BigDecimal(d.getSubTotal())); }
            catch (NumberFormatException ignored) {}
        }
        updateTampilanTotal();
    }

    // ===================== TAMBAH TRANSAKSI =====================

    @FXML
    private void handleTambahTransaksi() {
        if (!validateDetailForm()) return;

        String idSetor     = txtIDSetorLimbahDetail.getText();
        String namaLimbah  = cmbJenisLimbah.getValue();
        String jumlahInput = txtJumlah.getText().trim();
        String satuan      = normalizeSatuan(lblSatuan.getText());
        String keterangan  = txtKeteranganDetail.getText().trim();

        LimbahItem limbahTerpilih = getLimbahTerpilih();
        if (limbahTerpilih == null || limbahTerpilih.idLimbah == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Nama Limbah tidak valid.");
            return;
        }
        String idLimbah = limbahTerpilih.idLimbah;

        DetailSetorLimbah existing = cariDetailByIdLimbah(idLimbah);

        if (existing != null) {
            // ID Limbah sudah ada di daftar detail -> gabungkan baris (PK dtl_tr_Setor_Limbah
            // adalah kombinasi ID_Setor + ID_Limbah, sehingga tidak boleh ada baris duplikat
            // untuk ID_Limbah yang sama pada satu transaksi).
            // Yang berubah HANYA Jumlah dan Sub Total, field lain (Satuan, Keterangan)
            // tetap mengikuti baris yang sudah ada.
            BigDecimal jumlahGabungan = parseBigDecimalAman(existing.getJumlah())
                    .add(parseBigDecimalAman(jumlahInput));
            BigDecimal subTotalGabungan = parseBigDecimalAman(existing.getSubTotal())
                    .add(subTotalHitung);

            DetailSetorLimbah gabungan = new DetailSetorLimbah(
                    idSetor,
                    existing.getIdLimbah(),
                    existing.getJenis(),
                    jumlahGabungan.toPlainString(),
                    existing.getSatuan(),
                    existing.getKeterangan(),
                    subTotalGabungan.toPlainString()
            );

            int idx = detailList.indexOf(existing);
            detailList.set(idx, gabungan);
        } else {
            // ID Limbah baru -> tambah baris baru seperti biasa.
            detailList.add(new DetailSetorLimbah(
                    idSetor, idLimbah, namaLimbah, jumlahInput, satuan, keterangan,
                    subTotalHitung.toPlainString()
            ));
        }

        hitungTotalTransaksi();
        btnSelesai.setDisable(false);

        clearInputDetail();
    }

    /** Mencari baris detail yang sudah ada berdasarkan ID Limbah (PK/FK pada dtl_tr_Setor_Limbah). */
    private DetailSetorLimbah cariDetailByIdLimbah(String idLimbah) {
        if (idLimbah == null) return null;
        for (DetailSetorLimbah d : detailList) {
            if (idLimbah.equalsIgnoreCase(d.getIdLimbah())) {
                return d;
            }
        }
        return null;
    }

    private BigDecimal parseBigDecimalAman(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

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

        String idKaryawan = Session.getIdKaryawanLogin();
        if (!validasiKaryawanAda(idKaryawan)) {
            showAlert(Alert.AlertType.ERROR, "Sesi Tidak Valid",
                    "ID Karyawan login tidak ditemukan / tidak valid. Silakan login ulang.");
            return;
        }

        try {
            String tanggal = tanggalTransaksi.format(DateTimeFormatter.ISO_LOCAL_DATE);

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_SetorLimbah(?,?,?,?,?)}");
            db.cstat.setString(1, txtIDTransaksi.getText());
            db.cstat.setString(2, idNasabahTerpilih);
            db.cstat.setString(3, idKaryawan);
            db.cstat.setString(4, tanggal);
            db.cstat.setBigDecimal(5, totalTransaksi);
            db.cstat.executeUpdate();

            String idSetorHeader = txtIDTransaksi.getText();
            for (DetailSetorLimbah d : detailList) {
                db.cstat = db.conn.prepareCall("{CALL sp_Insert_DetailSetorLimbah(?,?,?,?,?,?)}");
                db.cstat.setString(1, idSetorHeader);
                db.cstat.setString(2, d.getIdLimbah());
                db.cstat.setBigDecimal(3, new BigDecimal(d.getJumlah()));
                db.cstat.setString(4, d.getSatuan());
                db.cstat.setString(5, d.getKeterangan());
                db.cstat.setBigDecimal(6, new BigDecimal(d.getSubTotal()));
                db.cstat.executeUpdate();
            }

            db.cstat = db.conn.prepareCall("{CALL sp_Tambah_SaldoNasabah(?,?)}");
            db.cstat.setString(1, idNasabahTerpilih);
            db.cstat.setBigDecimal(2, totalTransaksi);
            db.cstat.executeUpdate();

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
        setTanggalOtomatis();
        totalTransaksi = BigDecimal.ZERO;
        subTotalHitung = BigDecimal.ZERO;
        saldoNasabahTerpilih = BigDecimal.ZERO;
        txtTotal.clear();

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