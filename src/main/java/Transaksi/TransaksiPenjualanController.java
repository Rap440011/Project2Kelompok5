package Transaksi;

import Connection.DBConnect;
import Master.MasterProduk;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TransaksiPenjualanController implements Initializable {

    // ===================== FXML — HEADER TRANSAKSI =====================
    @FXML private TextField  txtIDPenjualan;
    @FXML private TextField  txtIDKaryawan;
    @FXML private DatePicker dpTanggal;
    @FXML private TextField  txtTotal;
    @FXML private Button     btnSimpan;

    // ===================== FXML — DETAIL PENJUALAN =====================
    @FXML private GridPane   gridDetail;        // container detail (untuk disable/enable)
    @FXML private TextField  txtIDDetail;
    @FXML private TextField  txtIDPenjualanDetail;
    @FXML private TextField  txtIDProduk;
    @FXML private TextField  txtJumlah;
    @FXML private TextField  txtHargaJual;
    @FXML private TextField  txtSubtotal;
    @FXML private Button     btnSelesai;
    @FXML private Button     btnBatal;
    @FXML private Button     btnTambahProduk;

    // ===================== FXML — PANEL KARTU PRODUK (dinamis, dari Master Produk) =====================
    @FXML private GridPane    gpKartuProduk;
    @FXML private TextField   txtCariProduk;
    @FXML private ToggleGroup tgProduk;

    // ===================== FXML — TABEL DETAIL TRANSAKSI (bawah, tanpa kolom ID) =====================
    @FXML private TableView<Map<String, String>> tblDetailPenjualan;
    @FXML private TableColumn<Map<String, String>, String> colNamaProdukDetail;
    @FXML private TableColumn<Map<String, String>, String> colJumlahDetail;
    @FXML private TableColumn<Map<String, String>, String> colHargaDetail;

    // ===================== STYLE KARTU (dipertahankan persis seperti sebelumnya) =====================
    private static final String CARD_NORMAL =
            "-fx-background-color:white;" +
                    "-fx-border-color:#E0E0E0;" +
                    "-fx-border-radius:12;" +
                    "-fx-background-radius:12;" +
                    "-fx-padding:0;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);";

    private static final String CARD_HOVER =
            "-fx-background-color:#F1FAF5;" +
                    "-fx-border-color:#2E7D32;" +
                    "-fx-border-radius:12;" +
                    "-fx-background-radius:12;" +
                    "-fx-padding:0;" +
                    "-fx-effect: dropshadow(gaussian,rgba(46,125,50,0.18),10,0,0,3);";

    private static final String CARD_SELECTED =
            "-fx-background-color:#E8F5E9;" +
                    "-fx-border-color:#2E7D32;" +
                    "-fx-border-width:2.5;" +
                    "-fx-border-radius:12;" +
                    "-fx-background-radius:12;" +
                    "-fx-padding:0;" +
                    "-fx-effect: dropshadow(gaussian,rgba(46,125,50,0.25),12,0,0,4);";

    private static final String CARD_DISABLED =
            "-fx-background-color:#F5F5F5;" +
                    "-fx-border-color:#E0E0E0;" +
                    "-fx-border-radius:12;" +
                    "-fx-background-radius:12;" +
                    "-fx-padding:0;" +
                    "-fx-opacity:0.55;";

    private static final double CARD_HEIGHT = 124.0;
    private static final double IMG_SIZE    =  72.0;

    private static final String RUPIAH_PREFIX = "Rp ";
    /** Status produk yang boleh dijual (konsisten dengan Transaksi Pengolahan Limbah / Master Produk). */
    private static final String STATUS_AKTIF = "Aktif";

    // ===================== STATE =====================
    private final DBConnect db = new DBConnect();
    private final List<ToggleButton> daftarKartu = new ArrayList<>();
    private final List<MasterProduk> daftarProduk = new ArrayList<>();

    /** Data baris untuk tblDetailPenjualan — satu baris per klik "Selesai" (Nama Produk, Jumlah, Harga). */
    private final ObservableList<Map<String, String>> detailPenjualanData = FXCollections.observableArrayList();

    /** Produk (Master Produk) yang sedang dipilih dari kartu, dipakai untuk mengisi tabel detail. */
    private MasterProduk produkTerpilih = null;

    /** Referensi Master Limbah (ID_Limbah -> Nama_Limbah), untuk menyusun teks komposisi kartu. */
    private final LinkedHashMap<String, String> namaLimbahMap = new LinkedHashMap<>();

    /** Total yang terakumulasi dari semua detail yang sudah "Selesai". */
    private BigDecimal totalPenjualan = BigDecimal.ZERO;

    /** Apakah header (karyawan + tanggal) sudah terisi sehingga panel detail aktif. */
    private boolean headerLengkap = false;

    // ===================== INITIALIZE =====================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Numeric-only untuk txtJumlah
        addNumericOnly(txtJumlah, 8);

        // Hitung subtotal otomatis saat jumlah diketik
        txtJumlah.textProperty().addListener((obs, oldV, newV) -> hitungSubtotal());

        // Pantau kelengkapan header
        txtIDKaryawan.textProperty().addListener((obs, oldV, newV) -> cekHeader());
        dpTanggal.valueProperty().addListener((obs, oldV, newV) -> cekHeader());

        // Referensi nama limbah (untuk teks komposisi pada kartu)
        loadDataLimbahReferensi();

        // Tabel Detail Transaksi (Nama Produk, Jumlah, Harga — tanpa kolom ID)
        setupTabelDetailPenjualan();

        // Bangun kartu produk secara dinamis dari Master Produk
        loadDataProduk();

        // Listener pemilihan kartu
        tgProduk.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            for (ToggleButton c : daftarKartu) {
                if (!c.isDisable()) c.setStyle(CARD_NORMAL);
            }
            if (newT == null) return;

            ToggleButton sel = (ToggleButton) newT;
            sel.setStyle(CARD_SELECTED);
            MasterProduk p = (MasterProduk) sel.getUserData();
            isikanProdukTerpilih(p);
        });

        // Panel detail & kartu nonaktif di awal
        setPanelDetailEnabled(false);

        loadAutoIDPenjualan();
    }

    // ===================== TABEL DETAIL TRANSAKSI (bawah, tanpa kolom ID) =====================
    /** Menghubungkan kolom TableView dengan key pada Map setiap baris (tanpa class model baru). */
    private void setupTabelDetailPenjualan() {
        colNamaProdukDetail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault("namaProduk", "")));
        colJumlahDetail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault("jumlah", "")));
        colHargaDetail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault("harga", "")));
        tblDetailPenjualan.setItems(detailPenjualanData);
    }

    /** Helper: bikin satu baris (Map) untuk tblDetailPenjualan. */
    private Map<String, String> buatBarisDetailPenjualan(String namaProduk, String jumlah, String harga) {
        Map<String, String> baris = new LinkedHashMap<>();
        baris.put("namaProduk", namaProduk);
        baris.put("jumlah", jumlah);
        baris.put("harga", harga);
        return baris;
    }

    // ===================== CEK HEADER =====================
    private void cekHeader() {
        boolean karyawanOk = !txtIDKaryawan.getText().trim().isEmpty();
        boolean tanggalOk  = dpTanggal.getValue() != null;
        headerLengkap = karyawanOk && tanggalOk;

        setPanelDetailEnabled(headerLengkap);

        if (headerLengkap) {
            // Isi ID Penjualan di panel detail
            txtIDPenjualanDetail.setText(txtIDPenjualan.getText());
            if (txtIDDetail.getText().isEmpty()) loadAutoIDDetail();
        }
    }

    /** Nilai stok produk (dari MasterProduk.getStok()), aman terhadap format tak terduga. */
    private int stokProduk(MasterProduk p) {
        try { return Integer.parseInt(p.getStok().trim()); } catch (Exception e) { return 0; }
    }

    // ===================== ENABLE / DISABLE PANEL DETAIL & KARTU =====================
    private void setPanelDetailEnabled(boolean enabled) {
        // Field detail
        txtJumlah.setDisable(!enabled);
        btnSelesai.setDisable(!enabled);
        btnBatal.setDisable(!enabled);
        btnTambahProduk.setDisable(!enabled);

        // Kartu produk — kartu yang stoknya 0 tetap abu-abu/nonaktif walau header sudah lengkap
        for (ToggleButton card : daftarKartu) {
            MasterProduk p = (MasterProduk) card.getUserData();
            boolean stokHabis = p != null && stokProduk(p) <= 0;
            boolean cardEnabled = enabled && !stokHabis;

            card.setDisable(!cardEnabled);
            card.setStyle(cardEnabled ? CARD_NORMAL : CARD_DISABLED);
            card.setMouseTransparent(!cardEnabled);
        }

        if (!enabled && tgProduk != null) {
            tgProduk.selectToggle(null);
        }
    }

    // ===================== LOAD REFERENSI MASTER LIMBAH (untuk teks komposisi kartu) =====================
    private void loadDataLimbahReferensi() {
        namaLimbahMap.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                namaLimbahMap.put(db.result.getString("ID_Limbah"), db.result.getString("Nama_Limbah"));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data Limbah", e.getMessage());
        }
    }

    // ===================== LOAD & BANGUN KARTU PRODUK (dinamis dari Master Produk) =====================
    /**
     * Mengambil daftar produk dari Master Produk (hanya status Aktif) dan membangun
     * kartu ToggleButton untuk masing-masing, dengan style/layout yang identik dengan
     * versi sebelumnya (CARD_NORMAL/HOVER/SELECTED/DISABLED, ikon aksen hijau, gambar,
     * nama, komposisi, badge satuan).
     */
    private void loadDataProduk() {
        daftarProduk.clear();
        daftarKartu.clear();
        gpKartuProduk.getChildren().clear();

        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Produk}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                String status = db.result.getString("Status");
                if (status != null && !status.trim().equalsIgnoreCase(STATUS_AKTIF)) continue;

                MasterProduk p = new MasterProduk(
                        db.result.getString("ID_Produk"),
                        db.result.getString("Nama_Produk"),
                        String.valueOf(db.result.getInt("Stok")),
                        RUPIAH_PREFIX + db.result.getBigDecimal("Harga_Jual")
                                .stripTrailingZeros().toPlainString(),
                        db.result.getString("Satuan"),
                        db.result.getString("Keterangan"),
                        db.result.getString("ID_Limbah"),
                        status
                );

                String pathGambar = "";
                try {
                    String hasil = db.result.getString("Path_Gambar");
                    if (hasil != null) pathGambar = hasil;
                } catch (SQLException ignored) {}
                p.setPathGambar(pathGambar);

                daftarProduk.add(p);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Produk", e.getMessage());
        }

        tampilkanSemuaKartu();
    }

    /** Menyusun ulang gpKartuProduk (1 kolom) dari isi daftarProduk saat ini. */
    private void tampilkanSemuaKartu() {
        daftarKartu.clear();
        gpKartuProduk.getChildren().clear();

        int row = 0;
        for (MasterProduk p : daftarProduk) {
            ToggleButton card = buildKartuProduk(p);
            daftarKartu.add(card);
            GridPane.setColumnIndex(card, 0);
            GridPane.setRowIndex(card, row);
            gpKartuProduk.getChildren().add(card);
            row++;
        }

        // Ikuti status header saat ini (aktif/nonaktif) untuk kartu yang baru dibangun
        setPanelDetailEnabled(headerLengkap);
    }

    /** Bangun satu kartu ToggleButton untuk sebuah produk Master Produk. */
    private ToggleButton buildKartuProduk(MasterProduk p) {
        ToggleButton card = new ToggleButton();
        card.setToggleGroup(tgProduk);
        card.setUserData(p);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(CARD_DISABLED);   // nonaktif sampai header lengkap (atau tetap abu-abu kalau stok habis)
        card.setDisable(true);
        card.setMouseTransparent(true);

        card.setPrefHeight(CARD_HEIGHT);
        card.setMinHeight(CARD_HEIGHT);
        card.setMaxHeight(CARD_HEIGHT);
        card.setGraphic(buildKartuGraphic(p));

        boolean stokHabis = stokProduk(p) <= 0;
        card.setOnMouseEntered(e -> {
            if (!card.isDisable() && !card.isSelected() && !stokHabis) card.setStyle(CARD_HOVER);
        });
        card.setOnMouseExited(e -> {
            if (!card.isDisable() && !card.isSelected() && !stokHabis) card.setStyle(CARD_NORMAL);
        });

        return card;
    }

    /** Isi ID Produk & Harga Jual di form detail begitu kartu produk (Master Produk) dipilih. */
    private void isikanProdukTerpilih(MasterProduk p) {
        produkTerpilih = p;
        txtIDProduk.setText(p.getIdProduk());
        String hargaRaw = p.getHargaJual().replace(RUPIAH_PREFIX, "").trim();
        txtHargaJual.setText(hargaRaw);
        txtJumlah.clear();
        txtSubtotal.clear();
    }

    /** Teks komposisi ("• Lumpur, Kotoran") berdasarkan daftar ID_Limbah (dipisah koma) pada produk. */
    private String buildKomposisiText(String idLimbahCsv) {
        if (idLimbahCsv == null || idLimbahCsv.trim().isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (String id : idLimbahCsv.split(",")) {
            String trimmed = id.trim();
            if (trimmed.isEmpty()) continue;
            String nama = namaLimbahMap.getOrDefault(trimmed, trimmed);
            if (sb.length() > 0) sb.append(", ");
            sb.append(nama);
        }
        return sb.length() > 0 ? sb.toString() : "-";
    }

    /** Bangun graphic satu kartu — layout identik dengan versi sebelumnya, sumber data dari Master Produk. */
    private javafx.scene.Node buildKartuGraphic(MasterProduk p) {
        Region aksen = new Region();
        aksen.setPrefWidth(5); aksen.setMinWidth(5);
        aksen.setMaxHeight(Double.MAX_VALUE);
        aksen.setStyle("-fx-background-color:#2E7D32;-fx-background-radius:10 0 0 10;");

        ImageView iv = new ImageView();
        iv.setFitWidth(IMG_SIZE); iv.setFitHeight(IMG_SIZE);
        iv.setPreserveRatio(true);

        boolean gambarLoaded = false;
        String gambarPath = p.getPathGambar();
        if (gambarPath != null && !gambarPath.isEmpty()) {
            try {
                File f = new File(gambarPath);
                if (f.exists()) {
                    iv.setImage(new Image(f.toURI().toString()));
                    gambarLoaded = true;
                }
            } catch (Exception ignored) {}
        }
        if (!gambarLoaded) {
            try {
                String resName = p.getNamaProduk().replaceAll("\\s+", "_") + ".png";
                java.io.InputStream is = getClass().getResourceAsStream("/Gambar_Produk/" + resName);
                if (is != null) iv.setImage(new Image(is));
            } catch (Exception ignored) {}
        }

        Label lblNama = new Label(p.getNamaProduk());
        lblNama.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1B5E20;");
        lblNama.setWrapText(true); lblNama.setMaxWidth(Double.MAX_VALUE);

        Label lblKomTitle = new Label("Komposisi :");
        lblKomTitle.setStyle("-fx-font-size:11px;-fx-text-fill:#9E9E9E;");

        Label lblKom = new Label("• " + buildKomposisiText(p.getIdLimbah()));
        lblKom.setStyle("-fx-font-size:11px;-fx-text-fill:#757575;");
        lblKom.setWrapText(true); lblKom.setMaxWidth(Double.MAX_VALUE);

        // ── Badge Stok (Stok Habis / Menipis / normal) — sama seperti Master Produk ──
        Label lblStokJudul = new Label("Stok :");
        lblStokJudul.setStyle("-fx-font-size:11px;-fx-text-fill:#9E9E9E;");

        int nilaiStok = stokProduk(p);
        String warnaStok, bgStok, teksStok;
        if (nilaiStok <= 0) {
            warnaStok = "#C62828"; bgStok = "#FFEBEE"; teksStok = "Stok Habis";
        } else if (nilaiStok < 10) {
            warnaStok = "#E65100"; bgStok = "#FFF3E0"; teksStok = nilaiStok + " " + p.getSatuan() + " (Menipis)";
        } else {
            warnaStok = "#2E7D32"; bgStok = "#E8F5E9"; teksStok = nilaiStok + " " + p.getSatuan();
        }

        Label lblStokIsi = new Label(teksStok);
        lblStokIsi.setStyle(
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + warnaStok + ";" +
                        "-fx-background-color:" + bgStok + ";" +
                        "-fx-padding:1 8 1 8;-fx-background-radius:8;");

        HBox stokRow = new HBox(6, lblStokJudul, lblStokIsi);
        stokRow.setAlignment(Pos.CENTER_LEFT);

        Label lblBadge = new Label(p.getSatuan());
        lblBadge.setStyle("-fx-background-color:#E8F5E9;-fx-text-fill:#2E7D32;" +
                "-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-padding:2 8 2 8;-fx-background-radius:8;");

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        HBox badgeRow = new HBox(lblBadge); badgeRow.setAlignment(Pos.BOTTOM_RIGHT);

        VBox teks = new VBox(4, lblNama, lblKomTitle, lblKom, stokRow, spacer, badgeRow);
        teks.setAlignment(Pos.TOP_LEFT);
        teks.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(teks, Priority.ALWAYS);

        HBox root = new HBox(12, aksen, iv, teks);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMaxHeight(CARD_HEIGHT);
        root.setStyle("-fx-padding:10 14 10 0;");
        HBox.setHgrow(root, Priority.ALWAYS);
        return root;
    }

    // ===================== CARI PRODUK =====================
    @FXML
    private void handleCariProduk() {
        if (!headerLengkap) return;
        String keyword = txtCariProduk.getText().trim().toLowerCase(Locale.ROOT);

        gpKartuProduk.getChildren().clear();
        int row = 0;
        for (ToggleButton card : daftarKartu) {
            boolean cocok = true;
            if (!keyword.isEmpty() && card.getUserData() instanceof MasterProduk) {
                cocok = ((MasterProduk) card.getUserData())
                        .getNamaProduk().toLowerCase(Locale.ROOT).contains(keyword);
            }
            if (cocok) {
                GridPane.setColumnIndex(card, 0);
                GridPane.setRowIndex(card, row);
                gpKartuProduk.getChildren().add(card);
                row++;   // 1 kolom → setiap kartu di baris baru
            }
        }
    }

    // ===================== HITUNG SUBTOTAL =====================
    private void hitungSubtotal() {
        try {
            String jumlahText = txtJumlah.getText().trim();
            String hargaText  = txtHargaJual.getText().trim();
            if (jumlahText.isEmpty() || hargaText.isEmpty()) { txtSubtotal.clear(); return; }
            BigDecimal jumlah = new BigDecimal(jumlahText);
            BigDecimal harga  = new BigDecimal(hargaText);
            txtSubtotal.setText(jumlah.multiply(harga).toPlainString());
        } catch (NumberFormatException e) {
            txtSubtotal.clear();
        }
    }

    // ===================== AUTO ID =====================
    private void loadAutoIDPenjualan() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Penjualan}");
            if (db.result.next()) {
                txtIDPenjualan.setText(db.result.getString("ID_Penjualan"));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID Penjualan", e.getMessage());
        }
    }

    private void loadAutoIDDetail() {
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_AutoID_DetailPenjualan(?)}");
            db.cstat.setString(1, txtIDPenjualan.getText());
            db.result = db.cstat.executeQuery();
            if (db.result.next()) {
                txtIDDetail.setText(db.result.getString("ID_Detail_Penjualan"));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID Detail", e.getMessage());
        }
    }

    // ===================== TOMBOL SELESAI (simpan satu detail) =====================
    @FXML
    private void handleSelesai() {
        if (!validateDetail()) return;
        try {
            String idDetail  = txtIDDetail.getText();
            String idJual    = txtIDPenjualan.getText();
            String idProduk  = txtIDProduk.getText();
            int    jumlah    = Integer.parseInt(txtJumlah.getText().trim());
            BigDecimal harga = new BigDecimal(txtHargaJual.getText().trim());
            BigDecimal sub   = new BigDecimal(txtSubtotal.getText().trim());

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_DetailPenjualan(?,?,?,?,?,?)}");
            db.cstat.setString(1, idDetail);
            db.cstat.setString(2, idJual);
            db.cstat.setString(3, idProduk);
            db.cstat.setInt(4, jumlah);
            db.cstat.setBigDecimal(5, harga);
            db.cstat.setBigDecimal(6, sub);
            db.cstat.executeUpdate();

            // Akumulasi total
            totalPenjualan = totalPenjualan.add(sub);
            txtTotal.setText(totalPenjualan.toPlainString());

            // Tambahkan baris ke tabel Detail Transaksi (tanpa ID — hanya nama produk, jumlah, harga)
            String namaProduk = produkTerpilih != null ? produkTerpilih.getNamaProduk() : idProduk;
            String satuan     = produkTerpilih != null ? produkTerpilih.getSatuan() : "";
            String jumlahStr  = jumlah + (satuan.isEmpty() ? "" : " " + satuan);
            String hargaStr   = RUPIAH_PREFIX + sub.toPlainString();
            detailPenjualanData.add(buatBarisDetailPenjualan(namaProduk, jumlahStr, hargaStr));

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Detail penjualan berhasil ditambahkan.");
            bersihkanFormDetail();
            loadAutoIDDetail();   // ID detail baru, ID penjualan tetap

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Selesai", e.getMessage());
        }
    }

    // ===================== TOMBOL BATAL (kosongkan form detail) =====================
    @FXML
    private void handleBatal() {
        bersihkanFormDetail();
    }

    /**
     * Bersihkan hanya field input detail.
     * ID Detail di-generate ulang; ID Penjualan & totalPenjualan TIDAK berubah.
     */
    private void bersihkanFormDetail() {
        txtIDProduk.clear();
        txtJumlah.clear();
        txtHargaJual.clear();
        txtSubtotal.clear();
        produkTerpilih = null;
        if (tgProduk != null) tgProduk.selectToggle(null);
        for (ToggleButton c : daftarKartu) {
            if (!c.isDisable()) c.setStyle(CARD_NORMAL);
        }
    }

    // ===================== TOMBOL TAMBAH PRODUK (simpan item saat ini, lanjut input berikutnya) =====================
    @FXML
    private void handleTambahProduk() {
        // Sama seperti "Selesai": simpan produk yang sedang diisi ke database & tabel,
        // lalu form otomatis dikosongkan dan ID Detail baru dibuat untuk produk berikutnya.
        handleSelesai();
    }

    // ===================== TOMBOL SIMPAN (simpan header transaksi) =====================
    @FXML
    private void handleSimpan() {
        if (!validateHeader()) return;
        if (totalPenjualan.compareTo(BigDecimal.ZERO) == 0) {
            showAlert(Alert.AlertType.WARNING, "Peringatan",
                    "Belum ada detail penjualan yang ditambahkan.\n" +
                            "Tambahkan produk lalu klik Selesai sebelum Simpan.");
            return;
        }
        try {
            String tanggal = dpTanggal.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Penjualan(?,?,?,?)}");
            db.cstat.setString(1, txtIDPenjualan.getText());
            db.cstat.setString(2, txtIDKaryawan.getText().trim());
            db.cstat.setString(3, tanggal);
            db.cstat.setBigDecimal(4, totalPenjualan);
            db.cstat.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Transaksi penjualan berhasil disimpan.");
            resetSemua();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    // ===================== VALIDASI =====================
    private boolean validateHeader() {
        if (txtIDKaryawan.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "ID Karyawan wajib diisi.");
            return false;
        }
        if (dpTanggal.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Tanggal wajib dipilih.");
            return false;
        }
        return true;
    }

    private boolean validateDetail() {
        if (txtIDProduk.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Pilih produk dari daftar terlebih dahulu.");
            return false;
        }
        if (txtJumlah.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Jumlah wajib diisi.");
            return false;
        }
        int jumlah;
        try { jumlah = Integer.parseInt(txtJumlah.getText().trim()); }
        catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Jumlah harus berupa angka.");
            return false;
        }
        if (produkTerpilih != null) {
            int stokTersedia = stokProduk(produkTerpilih);
            if (jumlah > stokTersedia) {
                showAlert(Alert.AlertType.WARNING, "Validasi",
                        "Jumlah stok tidak mencukupi.\n" +
                                "Stok tersedia: " + stokTersedia + " " + produkTerpilih.getSatuan() + ".");
                return false;
            }
        }
        if (txtSubtotal.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Subtotal belum terhitung. Pastikan jumlah sudah diisi.");
            return false;
        }
        return true;
    }

    // ===================== RESET SEMUA =====================
    private void resetSemua() {
        txtIDKaryawan.clear();
        dpTanggal.setValue(null);
        txtTotal.clear();
        totalPenjualan = BigDecimal.ZERO;

        bersihkanFormDetail();
        txtIDDetail.clear();
        txtIDPenjualanDetail.clear();
        detailPenjualanData.clear();

        setPanelDetailEnabled(false);
        headerLengkap = false;

        loadAutoIDPenjualan();

        loadDataProduk();
    }

    private void addNumericOnly(TextField field, int maxLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9]", "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}