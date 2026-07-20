package Transaksi;

import Auth.Session;
import Connection.DBConnect;
import Master.MasterProduk;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TransaksiPenjualanController implements Initializable {

    // ===================== FXML — HEADER TRANSAKSI =====================
    @FXML private TextField  txtIDPenjualan;
    @FXML private DatePicker dpTanggal;
    @FXML private TextField  txtTotal;
    @FXML private Button     btnSimpan;

    // ===================== FXML — DETAIL PENJUALAN =====================
    @FXML private GridPane   gridDetail;        // container detail (untuk disable/enable)
    @FXML private TextField  txtIDPenjualanDetail;
    @FXML private TextField  txtIDProduk;
    @FXML private TextField  txtJumlah;
    @FXML private TextField  txtHargaJual;
    @FXML private TextField  txtSubtotal;
    @FXML private Button     btnSelesai;
    @FXML private Button     btnBatal;
    @FXML private Button     btnTambahProduk;

    // ===================== FXML — PANEL KARTU PRODUK (dinamis, dari Master Produk) =====================
    @FXML private javafx.scene.layout.VBox    vbKartuProduk;
    @FXML private TextField   txtCariProduk;
    @FXML private ToggleGroup tgProduk;

    // ===================== FXML — TABEL DETAIL TRANSAKSI (bawah, tanpa kolom ID) =====================
    @FXML private TableView<Map<String, String>> tblDetailPenjualan;
    @FXML private TableColumn<Map<String, String>, String> colNamaProdukDetail;
    @FXML private TableColumn<Map<String, String>, String> colJumlahDetail;
    @FXML private TableColumn<Map<String, String>, String> colHargaDetail;

    // ===================== STYLE KARTU (disamakan persis dengan Transaksi Pengolahan Limbah) =====================
    private static final String CARD_NORMAL =
            "-fx-background-color:white;" +
                    "-fx-border-color:#E8E8E8; -fx-border-width:1;" +
                    "-fx-border-radius:10; -fx-background-radius:10;" +
                    "-fx-padding:0;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);";

    private static final String CARD_HOVER =
            "-fx-background-color:#FAFFFA;" +
                    "-fx-border-color:#2E7D32; -fx-border-width:1;" +
                    "-fx-border-radius:10; -fx-background-radius:10;" +
                    "-fx-padding:0;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);";

    private static final String CARD_SELECTED =
            "-fx-background-color:#E8F5E9;" +
                    "-fx-border-color:#2E7D32; -fx-border-width:2;" +
                    "-fx-border-radius:10; -fx-background-radius:10;" +
                    "-fx-padding:0;" +
                    "-fx-effect: dropshadow(gaussian,rgba(46,125,50,0.2),8,0,0,2);";

    private static final String CARD_DISABLED =
            "-fx-background-color:#F5F5F5;" +
                    "-fx-border-color:#E0E0E0; -fx-border-width:1;" +
                    "-fx-border-radius:10; -fx-background-radius:10;" +
                    "-fx-padding:0;" +
                    "-fx-opacity:0.55;";

    private static final double IMG_SIZE = 64.0;

    private static final String RUPIAH_PREFIX = "Rp ";
    /** Status produk yang boleh dijual (konsisten dengan Transaksi Pengolahan Limbah / Master Produk). */
    private static final String STATUS_AKTIF = "Aktif";

    /**
     * Menyeragamkan nama satuan: "Kilo" (apapun huruf besar/kecilnya) ditampilkan sebagai "Kg".
     * Satuan lain ditampilkan apa adanya.
     */
    private static String normalizeSatuan(String satuan) {
        if (satuan == null) return satuan;
        if (satuan.equalsIgnoreCase("Kilo")) return "Kg";
        return satuan;
    }

    private void addNumericOnly(TextField field, int maxLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9.]", "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    // ===================== DATA & STATE =====================
    private final DBConnect db = new DBConnect();
    private BigDecimal totalPenjualan = BigDecimal.ZERO;
    private List<DetailPending> detailPending = new ArrayList<>();
    private ObservableList<Map<String, String>> detailPenjualanData = FXCollections.observableArrayList();
    private List<VBox> daftarKartu = new ArrayList<>();
    private MasterProduk produkTerpilih = null;
    /** Kartu yang sedang dipilih (pola sama dengan Transaksi Pengolahan Limbah, tanpa ToggleButton/ToggleGroup). */
    private VBox kartuAktif = null;

    private List<MasterProduk> daftarProduk = new ArrayList<>();
    // ID Limbah -> Nama Limbah, dipakai untuk menyusun teks "Komposisi :" pada kartu produk
    private final LinkedHashMap<String, String> namaLimbahMap = new LinkedHashMap<>();
    String idKaryawan = Session.getIdKaryawanLogin();

    // ======================== INNER CLASS ========================
    private static class DetailPending {
        String idProduk;
        int jumlah;
        BigDecimal harga;
        BigDecimal subtotal;

        DetailPending(String idProduk, int jumlah, BigDecimal harga, BigDecimal subtotal) {
            this.idProduk = idProduk;
            this.jumlah = jumlah;
            this.harga = harga;
            this.subtotal = subtotal;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadAutoIDPenjualan();
        isiTanggalOtomatis();
        loadNamaLimbahMap();
        loadMasterProduk();
        buatKartuProduk();
        setupTabelDetailPenjualan();
        setupFormDetailInputListeners();
        setDetailPanelEnabled(false);

        // ID Penjualan di form Detail Penjualan harus selalu ikut ID Penjualan
        // di header (txtIDPenjualan) — dibuat read-only karena hanya tampilan turunan.
        txtIDPenjualanDetail.textProperty().bind(txtIDPenjualan.textProperty());
        txtIDPenjualanDetail.setEditable(false);

        // Batasi input Jumlah di Transaksi Penjualan agar hanya angka (dan titik desimal).
        addNumericOnly(txtJumlah, 4);
    }

    /** Memuat pemetaan ID_Limbah -> Nama_Limbah, dipakai untuk teks "Komposisi :" di kartu produk. */
    private void loadNamaLimbahMap() {
        namaLimbahMap.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                namaLimbahMap.put(db.result.getString("ID_Limbah"), db.result.getString("Nama_Limbah"));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Limbah", e.getMessage());
        }
    }

    /** Menyusun teks komposisi ("Endapan, Kotoran Udang") dari CSV ID_Limbah pada produk. */
    private String buildKomposisiText(String idLimbahCsv) {
        if (idLimbahCsv == null || idLimbahCsv.trim().isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (String id : idLimbahCsv.split(",")) {
            id = id.trim();
            if (id.isEmpty()) continue;
            String nama = namaLimbahMap.getOrDefault(id, id);
            if (sb.length() > 0) sb.append(", ");
            sb.append(nama);
        }
        return sb.length() > 0 ? sb.toString() : "-";
    }

    private void isiTanggalOtomatis() {
        dpTanggal.setValue(java.time.LocalDate.now());
    }

    private void loadMasterProduk() {
        daftarProduk.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Produk}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                String status = db.result.getString("Status");
                if (!STATUS_AKTIF.equalsIgnoreCase(status)) continue;

                MasterProduk produk = new MasterProduk(
                        db.result.getString("ID_Produk"),           // param 1: idProduk
                        db.result.getString("Nama_Produk"),         // param 2: namaProduk
                        db.result.getString("Stok"),                // param 3: stok (STRING)
                        db.result.getString("Harga_Jual"),          // param 4: hargaJual
                        db.result.getString("Satuan"),              // param 5: satuan
                        db.result.getString("Keterangan"),          // param 6: keterangan
                        db.result.getString("ID_Limbah"),           // param 7: idLimbah
                        db.result.getString("Status")               // param 8: status
                );

                // Path gambar disamakan sumbernya dengan Master Produk (kolom Path_Gambar di DB)
                try {
                    String pathGambar = db.result.getString("Path_Gambar");
                    produk.setPathGambar(pathGambar != null ? pathGambar : "");
                } catch (SQLException ignored) {
                    produk.setPathGambar("");
                }

                daftarProduk.add(produk);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Master Produk", e.getMessage());
        }
    }

    private void buatKartuProduk() {
        vbKartuProduk.getChildren().clear();
        daftarKartu.clear();
        for (MasterProduk produk : daftarProduk) {
            VBox kartu = buatKartuProdukkk(produk);
            vbKartuProduk.getChildren().add(kartu);
            daftarKartu.add(kartu);
        }
    }

    private VBox buatKartuProdukkk(MasterProduk produk) {
        // ── Gambar produk (sudut membulat asli, sumber sama dengan Master Produk) ──
        StackPane gambarBox = createImageView(produk);

        // ── Nama produk ──
        Label lblNama = createLabelNamaProduk(produk);

        // ── Komposisi ──
        Label lblKomposisiJudul = new Label("Komposisi :");
        lblKomposisiJudul.setStyle("-fx-font-size:11px; -fx-text-fill:#9E9E9E;");

        Label lblKomposisiIsi = new Label("• " + buildKomposisiText(produk.getIdLimbah()));
        lblKomposisiIsi.setStyle("-fx-font-size:11px; -fx-text-fill:#555555;");
        lblKomposisiIsi.setWrapText(true);

        VBox komposisiBox = new VBox(1, lblKomposisiJudul, lblKomposisiIsi);

        // ── Stok (badge berwarna: hijau normal, oranye menipis, merah habis) ──
        Label lblStokJudul = new Label("Stok :");
        lblStokJudul.setStyle("-fx-font-size:11px; -fx-text-fill:#9E9E9E;");

        int nilaiStok = stokProduk(produk);
        String warnaStok, bgStok, teksStok;
        if (nilaiStok <= 0) {
            warnaStok = "#C62828"; bgStok = "#FFEBEE"; teksStok = "Stok Habis";
        } else if (nilaiStok < 10) {
            warnaStok = "#E65100"; bgStok = "#FFF3E0"; teksStok = nilaiStok + " (Menipis)";
        } else {
            warnaStok = "#2E7D32"; bgStok = "#E8F5E9"; teksStok = String.valueOf(nilaiStok);
        }

        Label lblStokIsi = new Label(teksStok);
        lblStokIsi.setStyle(
                "-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:" + warnaStok + ";" +
                        "-fx-background-color:" + bgStok + ";" +
                        "-fx-padding:2 8 2 8; -fx-background-radius:8;");

        // ── Badge satuan: sejajar Pengolahan Limbah — didorong ke kanan-bawah via spacer ──
        Label lblSatuan = new Label(normalizeSatuan(produk.getSatuan()));
        lblSatuan.setStyle(
                "-fx-background-color:#E8F5E9; -fx-text-fill:#2E7D32;" +
                        "-fx-font-size:10px; -fx-font-weight:bold;" +
                        "-fx-padding:2 10 2 10; -fx-background-radius:10;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Stok digabung satu baris dengan badge satuan (bukan baris terpisah) supaya
        // jumlah baris & tinggi kartu tetap sama persis dengan kartu Pengolahan Limbah.
        Region badgeSpacer = new Region();
        HBox.setHgrow(badgeSpacer, Priority.ALWAYS);

        HBox badgeRow = new HBox(6, lblStokJudul, lblStokIsi, badgeSpacer, lblSatuan);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4, lblNama, komposisiBox, spacer, badgeRow);
        info.setAlignment(Pos.TOP_LEFT);
        info.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox isiKartu = new HBox(12, gambarBox, info);
        isiKartu.setAlignment(Pos.TOP_LEFT);
        isiKartu.setMaxWidth(Double.MAX_VALUE);
        isiKartu.setPadding(new Insets(12));

        // Kartu = VBox polos (sama persis dengan pola Transaksi Pengolahan Limbah), BUKAN
        // ToggleButton/Control — supaya tidak ada skin bawaan Control yang menambah tinggi
        // di luar konten aslinya. Seleksi kartu ditangani manual via klik (kartuAktif),
        // bukan lewat ToggleGroup.
        VBox kartu = new VBox(isiKartu);
        kartu.setMaxWidth(Double.MAX_VALUE);
        kartu.setCursor(javafx.scene.Cursor.HAND);
        kartu.setUserData(produk);

        // Produk tanpa stok tidak bisa dipilih untuk dijual
        boolean stokHabis = nilaiStok <= 0;
        if (stokHabis) {
            kartu.setStyle(CARD_DISABLED);
        } else {
            kartu.setStyle(CARD_NORMAL);

            kartu.setOnMouseEntered(event -> {
                if (kartu != kartuAktif) kartu.setStyle(CARD_HOVER);
            });
            kartu.setOnMouseExited(event -> {
                if (kartu != kartuAktif) kartu.setStyle(CARD_NORMAL);
            });
            kartu.setOnMouseClicked(event -> pilihKartuProduk(produk, kartu));
        }

        return kartu;
    }

    /** Menandai kartu sebagai terpilih & mengisi form detail — pola sama dengan
     *  pilihProdukUntukTransaksi() di Transaksi Pengolahan Limbah. */
    private void pilihKartuProduk(MasterProduk produk, VBox kartu) {
        if (kartuAktif != null) kartuAktif.setStyle(CARD_NORMAL);

        kartuAktif     = kartu;
        produkTerpilih = produk;
        kartu.setStyle(CARD_SELECTED);

        txtIDProduk.setText(produk.getIdProduk());
        txtHargaJual.setText(formatRibuan(hargaMentahKeAngkaBulat(produk.getHargaJual())));
        setDetailPanelEnabled(true);
    }


    /** Kotak gambar produk dengan sudut membulat asli (clip), sumber sama dengan Master Produk. */
    private StackPane createImageView(MasterProduk produk) {
        ImageView iv = new ImageView();
        iv.setFitWidth(IMG_SIZE);
        iv.setFitHeight(IMG_SIZE);
        iv.setPreserveRatio(false);

        boolean gambarLoaded = false;
        String gambarPath = produk.getPathGambar();
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
            String resName = produk.getNamaProduk().replaceAll("\\s+", "_") + ".png";
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/Gambar_Produk/" + resName);
                if (is != null) iv.setImage(new Image(is));
            } catch (Exception ignored) {}
        }

        Rectangle clip = new Rectangle(IMG_SIZE, IMG_SIZE);
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        iv.setClip(clip);

        StackPane gambarBox = new StackPane(iv);
        gambarBox.setMinSize(IMG_SIZE, IMG_SIZE);
        gambarBox.setMaxSize(IMG_SIZE, IMG_SIZE);
        gambarBox.setStyle(
                "-fx-background-color:#F5F5F5;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:#EEEEEE; -fx-border-radius:14;");
        return gambarBox;
    }

    private Label createLabelNamaProduk(MasterProduk produk) {
        Label lbl = new Label(produk.getNamaProduk());
        lbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1B5E20;");
        lbl.setWrapText(true);
        return lbl;
    }

    private void setupTabelDetailPenjualan() {
        colNamaProdukDetail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("nama")));
        colJumlahDetail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("jumlah")));
        colHargaDetail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get("harga")));
        tblDetailPenjualan.setItems(detailPenjualanData);
    }

    private void setupFormDetailInputListeners() {
        txtJumlah.textProperty().addListener((obs, oldV, newV) -> hitungSubtotal());
    }

    private void setDetailPanelEnabled(boolean enabled) {
        gridDetail.setDisable(!enabled);
        if (!enabled) bersihkanFormDetail();
    }

    private void hitungSubtotal() {
        try {
            String jumlahText = txtJumlah.getText().trim();
            String hargaText  = bersihkanAngka(txtHargaJual.getText());
            if (jumlahText.isEmpty() || hargaText.isEmpty()) { txtSubtotal.clear(); return; }
            BigDecimal jumlah = new BigDecimal(jumlahText);
            BigDecimal harga  = new BigDecimal(hargaText);
            BigDecimal subtotal = jumlah.multiply(harga).setScale(0, RoundingMode.HALF_UP);
            txtSubtotal.setText(formatRibuan(subtotal.toPlainString()));
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

    // ===================== TOMBOL SELESAI (tambahkan/gabungkan satu detail) =====================
    @FXML
    private void handleSelesai() {
        if (!validateDetail()) return;

        String idProduk    = txtIDProduk.getText();
        int    jumlahBaru  = Integer.parseInt(txtJumlah.getText().trim());
        BigDecimal harga   = new BigDecimal(bersihkanAngka(txtHargaJual.getText()));
        BigDecimal subBaru = new BigDecimal(bersihkanAngka(txtSubtotal.getText()));

        String namaProduk = produkTerpilih != null ? produkTerpilih.getNamaProduk() : idProduk;
        String satuan     = produkTerpilih != null ? produkTerpilih.getSatuan() : "";

        // Belum di-INSERT ke database di sini — baru benar-benar disimpan saat
        // tombol "Simpan" ditekan, setelah header transaksi tersimpan lebih dulu.
        DetailPending existing = cariDetailPending(idProduk);
        int jumlahAkhir;
        BigDecimal subAkhir;

        if (existing != null) {
            existing.jumlah   += jumlahBaru;
            existing.harga     = harga;
            existing.subtotal  = existing.subtotal.add(subBaru);
            jumlahAkhir = existing.jumlah;
            subAkhir    = existing.subtotal;

            // Update baris yang sudah ada di tabel Detail Transaksi
            for (Map<String, String> baris : detailPenjualanData) {
                if (idProduk.equals(baris.get("idProduk"))) {
                    baris.put("jumlah", jumlahAkhir + (satuan.isEmpty() ? "" : " " + satuan));
                    baris.put("harga", formatRibuan(subAkhir.toPlainString()));
                    break;
                }
            }
            tblDetailPenjualan.refresh();
        } else {
            detailPending.add(new DetailPending(idProduk, jumlahBaru, harga, subBaru));
            jumlahAkhir = jumlahBaru;
            subAkhir    = subBaru;

            String jumlahStr = jumlahAkhir + (satuan.isEmpty() ? "" : " " + satuan);
            String hargaStr  = formatRibuan(subAkhir.toPlainString());
            detailPenjualanData.add(buatBarisDetailPenjualan(idProduk, namaProduk, jumlahStr, hargaStr));
        }

        // Akumulasi total keseluruhan transaksi (tampilan sementara, permanen setelah "Simpan")
        totalPenjualan = totalPenjualan.add(subBaru);
        txtTotal.setText(formatRibuan(totalPenjualan.toPlainString()));

        showAlert(Alert.AlertType.INFORMATION, "Ditambahkan",
                "Produk ditambahkan ke daftar transaksi.\nKlik \"Simpan\" untuk menyimpan seluruh transaksi ke database.");
        bersihkanFormDetail();
    }

    // ===================== TOMBOL BATAL =====================
    @FXML
    private void handleBatal() {
        bersihkanFormDetail();
    }

    private void bersihkanFormDetail() {
        txtIDProduk.clear();
        txtJumlah.clear();
        txtHargaJual.clear();
        txtSubtotal.clear();
        produkTerpilih = null;
        if (kartuAktif != null) {
            kartuAktif.setStyle(CARD_NORMAL);
            kartuAktif = null;
        }
    }

    // ===================== TOMBOL TAMBAH PRODUK =====================
    @FXML
    private void handleTambahProduk() {
        handleSelesai();
    }

    // ===================== TOMBOL SIMPAN (dengan fn_TotalPenjualan) =====================
    @FXML
    private void handleSimpan() {
        if (!validateHeader()) return;
        if (detailPending.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan",
                    "Belum ada detail penjualan yang ditambahkan.\nTambahkan produk lalu klik Selesai sebelum Simpan.");
            return;
        }
        try {
            String tanggal = dpTanggal.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);

            db.conn.setAutoCommit(false);
            try {

                db.cstat = db.conn.prepareCall("{CALL sp_Insert_Penjualan(?,?,?,?)}");

                db.cstat.setString(1, txtIDPenjualan.getText());
                db.cstat.setString(2, idKaryawan);
                db.cstat.setDate(3, java.sql.Date.valueOf(tanggal));
                db.cstat.setBigDecimal(4, totalPenjualan);

                db.cstat.executeUpdate();

                for (DetailPending d : detailPending) {
                    db.cstat = db.conn.prepareCall("{CALL sp_Insert_DetailPenjualan(?,?,?,?,?)}");
                    db.cstat.setString(1, txtIDPenjualan.getText());
                    db.cstat.setString(2, d.idProduk);
                    db.cstat.setInt(3, d.jumlah);
                    db.cstat.setBigDecimal(4, d.harga);
                    db.cstat.setBigDecimal(5, d.subtotal);
                    db.cstat.executeUpdate();

                    // Kurangi stok produk sejumlah yang terjual (pola sama dengan
                    // sp_Kurangi_StokLimbah di Transaksi Pengolahan Limbah).
                    db.cstat = db.conn.prepareCall("{CALL sp_Kurangi_StokProduk(?,?)}");
                    db.cstat.setString(1, d.idProduk);
                    db.cstat.setInt(2, d.jumlah);
                    db.cstat.executeUpdate();
                }

                db.conn.commit();

                // 3) Setelah semua detail tersimpan, gunakan fn_TotalPenjualan
                //    untuk memverifikasi dan tampilkan total dari database
                BigDecimal totalDariDatabase = callFunctionTotalPenjualan(txtIDPenjualan.getText());
                txtTotal.setText(formatRibuan(totalDariDatabase.toPlainString()));

            } catch (SQLException e) {
                db.conn.rollback();
                throw e;
            } finally {
                db.conn.setAutoCommit(true);
            }

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Transaksi penjualan berhasil disimpan.");

            // Muat ulang master produk & kartu supaya stok yang tampil di layar
            // langsung mengikuti stok terbaru dari database, lalu reset form
            // (termasuk ID Penjualan yang otomatis berganti ke AutoID berikutnya).
            loadMasterProduk();
            buatKartuProduk();
            resetSemua();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    /**
     * Memanggil SQL Function fn_TotalPenjualan untuk menghitung total dari database.
     * Function ini SUM-kan semua Subtotal dari dtl_tr_Penjualan_Produk
     * berdasarkan ID_Penjualan yang diberikan.
     *
     * @param idPenjualan ID Penjualan
     * @return Total dari database, atau ZERO jika error
     */
    private BigDecimal callFunctionTotalPenjualan(String idPenjualan) {
        try {
            // Panggil function menggunakan SELECT
            String query = "SELECT dbo.fn_TotalPenjualan(?) AS Total";
            java.sql.PreparedStatement ps = db.conn.prepareStatement(query);
            ps.setString(1, idPenjualan);
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal("Total");
                if (total != null) {
                    return total;
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Gagal memanggil fn_TotalPenjualan: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    // ===================== VALIDASI =====================
    private boolean validateHeader() {
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
            int stokTersedia   = stokProduk(produkTerpilih);
            int sudahDipesan   = jumlahPendingUntukProduk(txtIDProduk.getText());
            int sisaBolehDitambah = stokTersedia - sudahDipesan;
            if (jumlah > sisaBolehDitambah) {
                showAlert(Alert.AlertType.WARNING, "Validasi",
                        "Jumlah stok tidak mencukupi.\nSisa stok yang bisa ditambahkan: " + sisaBolehDitambah + " " + produkTerpilih.getSatuan() + ".");
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
        dpTanggal.setValue(null);
        txtTotal.clear();
        totalPenjualan = BigDecimal.ZERO;

        bersihkanFormDetail();
        // txtIDPenjualanDetail TIDAK di-clear manual — field ini sudah di-bind
        // ke txtIDPenjualan (lihat initialize()), jadi otomatis ikut ter-update
        // saat loadAutoIDPenjualan() dipanggil di bawah.
        detailPending.clear();
        detailPenjualanData.clear();
        tblDetailPenjualan.refresh();

        setDetailPanelEnabled(false);
        loadAutoIDPenjualan();
        isiTanggalOtomatis();
    }

    // ===================== UTILITY =====================
    private DetailPending cariDetailPending(String idProduk) {
        return detailPending.stream()
                .filter(d -> d.idProduk.equals(idProduk))
                .findFirst()
                .orElse(null);
    }

    private int stokProduk(MasterProduk produk) {
        if (produk == null) return 0;
        try {
            String stokText = produk.getStok();
            if (stokText == null || stokText.isEmpty()) return 0;
            return Integer.parseInt(stokText.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int jumlahPendingUntukProduk(String idProduk) {
        return detailPending.stream()
                .filter(d -> d.idProduk.equals(idProduk))
                .mapToInt(d -> d.jumlah)
                .sum();
    }

    /**
     * Mem-parsing harga MENTAH dari database (mis. "45000.0000") menjadi angka bulat rupiah.
     * TIDAK BOLEH pakai bersihkanAngka() di sini, karena bersihkanAngka() membuang titik
     * desimal begitu saja sehingga "45000.0000" salah jadi "450000000" (menyambung digit
     * di belakang koma ke angka utuh). Di sini titik desimal diperlakukan dengan benar
     * lewat BigDecimal, lalu dibulatkan ke rupiah (tanpa sen).
     */
    private String hargaMentahKeAngkaBulat(String hargaMentahDariDb) {
        if (hargaMentahDariDb == null || hargaMentahDariDb.trim().isEmpty()) return "0";
        try {
            BigDecimal nilai = new BigDecimal(hargaMentahDariDb.trim());
            return nilai.setScale(0, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            // Fallback terakhir jika format tidak terduga
            return bersihkanAngka(hargaMentahDariDb);
        }
    }

    private String bersihkanAngka(String angka) {
        if (angka == null) return "0";
        return angka.replaceAll("[^0-9]", "");
    }

    private String formatRibuan(String angka) {
        if (angka == null || angka.isEmpty()) return "0";
        try {
            long num = Long.parseLong(angka);
            return String.format("%,d", num).replace(',', '.');
        } catch (NumberFormatException e) {
            return angka;
        }
    }

    private Map<String, String> buatBarisDetailPenjualan(String idProduk, String namaProduk, String jumlah, String harga) {
        Map<String, String> baris = new HashMap<>();
        baris.put("idProduk", idProduk);
        baris.put("nama", namaProduk);
        baris.put("jumlah", jumlah);
        baris.put("harga", harga);
        return baris;
    }

    /**
     * Handle pencarian produk berdasarkan text di txtCariProduk.
     * Filter product cards yang ditampilkan berdasarkan nama produk atau ID produk.
     */
    @FXML
    private void handleCariProduk() {
        String keyword = txtCariProduk.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            // Jika kosong, tampilkan semua produk
            buatKartuProduk();
            return;
        }

        List<MasterProduk> produkFiltered = daftarProduk.stream()
                .filter(p -> p.getNamaProduk().toLowerCase().contains(keyword) ||
                        p.getIdProduk().toLowerCase().contains(keyword))
                .collect(java.util.stream.Collectors.toList());

        vbKartuProduk.getChildren().clear();
        daftarKartu.clear();

        for (MasterProduk produk : produkFiltered) {
            VBox kartu = buatKartuProdukkk(produk);
            vbKartuProduk.getChildren().add(kartu);
            daftarKartu.add(kartu);
        }

        if (produkFiltered.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Pencarian",
                    "Tidak ada produk yang sesuai dengan '" + keyword + "'");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}