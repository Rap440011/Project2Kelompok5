package Master;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;
import java.util.function.UnaryOperator;

public class MasterProdukController implements Initializable {

    // ── Form kiri ─────────────────────────────────────────────────────────────
    @FXML private TextField    txtID;
    @FXML private TextField    txtNama;
    @FXML private ComboBox<String> cmbSatuan;
    @FXML private TextField    txtHarga;
    @FXML private Label        lblGambar;       // nama file yang dipilih
    @FXML private Button       btnUpload;
    @FXML private Button       btnBatal;
    @FXML private Button       btnHapus;
    @FXML private Button       btnUbah;
    @FXML private Button       btnSimpan;

    // Tabel relasi produk-limbah (kiri bawah)
    @FXML private TableView<RelasiProdukLimbah>           tbRelasi;
    @FXML private TableColumn<RelasiProdukLimbah, String> clmRelasiIDProduk;
    @FXML private TableColumn<RelasiProdukLimbah, String> clmRelasiIDLimbah;
    @FXML private TableColumn<RelasiProdukLimbah, String> clmRelasiPresentase;

    // ── Panel tengah: daftar limbah ───────────────────────────────────────────
    @FXML private VBox         vbDaftarLimbah;  // container tombol-tombol limbah

    // ── Panel kanan: kartu produk ─────────────────────────────────────────────
    @FXML private VBox         vbKartuProduk;   // container kartu produk

    // ── State ─────────────────────────────────────────────────────────────────
    private final DBConnect db = new DBConnect();
    private final ObservableList<MasterProduk>       dataList   = FXCollections.observableArrayList();
    private final ObservableList<RelasiProdukLimbah> relasiList = FXCollections.observableArrayList();

    private File   filGambarDipilih = null;
    private String pathGambarAktif  = "";

    // Multi-select limbah: id -> tombol, id -> nama
    private final LinkedHashMap<String, Button> limbahButtonMap = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> namaLimbahMap   = new LinkedHashMap<>();
    // Urutan limbah yang sedang dipilih (LinkedHashSet -> menjaga urutan klik)
    private final LinkedHashSet<String> idLimbahTerpilih = new LinkedHashSet<>();

    // Produk yang sedang dipilih dari panel kanan (untuk Ubah/Hapus)
    private MasterProduk produkDipilih = null;

    private static final String RUPIAH_PREFIX = "Rp ";

    // Folder permanen untuk menyimpan gambar produk yang diupload user.
    // Dibuat relatif terhadap direktori kerja aplikasi (bisa diubah ke path absolut sesuai kebutuhan deployment).
    private static final String FOLDER_GAMBAR_PRODUK = "gambar_produk";

    private static final String STYLE_TOMBOL_NONAKTIF =
            "-fx-background-color:white; -fx-border-color:#E0E0E0;" +
                    "-fx-border-radius:8; -fx-background-radius:8;" +
                    "-fx-text-fill:#333333; -fx-cursor:hand;";
    private static final String STYLE_TOMBOL_AKTIF =
            "-fx-background-color:#E8F5E9; -fx-border-color:#2E7D32; -fx-border-width:2;" +
                    "-fx-border-radius:8; -fx-background-radius:8;" +
                    "-fx-text-fill:#2E7D32; -fx-font-weight:bold; -fx-cursor:hand;";
    private static final String STYLE_TOMBOL_HOVER =
            "-fx-background-color:#F1FAF5; -fx-border-color:#E0E0E0;" +
                    "-fx-border-radius:8; -fx-background-radius:8;" +
                    "-fx-text-fill:#333333; -fx-cursor:hand;";

    // Style label nama file gambar - HARUS sama persis baik sebelum maupun
    // sesudah produk dipilih, supaya tampilan tidak berubah bentuk.
    private static final String STYLE_LABEL_GAMBAR =
            "-fx-font-size:11px; -fx-text-fill:#999; -fx-font-style:italic;";

    // Style kartu produk (panel kanan) — mengikuti desain pada Gambar 1
    private static final String STYLE_KARTU_NORMAL =
            "-fx-background-color:white;" +
                    "-fx-border-color:#E8E8E8; -fx-border-width:1;" +
                    "-fx-border-radius:10; -fx-background-radius:10;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);";
    private static final String STYLE_KARTU_HOVER =
            "-fx-background-color:#FAFFFA;" +
                    "-fx-border-color:#E8E8E8; -fx-border-width:1;" +
                    "-fx-border-radius:10; -fx-background-radius:10;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.07),6,0,0,2);";
    private static final String STYLE_KARTU_AKTIF =
            "-fx-background-color:#E8F5E9;" +
                    "-fx-border-color:#2E7D32; -fx-border-width:2;" +
                    "-fx-border-radius:10; -fx-background-radius:10;" +
                    "-fx-effect: dropshadow(gaussian,rgba(46,125,50,0.2),8,0,0,2);";

    // Kartu produk yang sedang aktif/terpilih di panel kanan
    private VBox kartuAktif = null;

    // ── Inner class relasi ────────────────────────────────────────────────────
    public static class RelasiProdukLimbah {
        private final javafx.beans.property.SimpleStringProperty idProduk;
        private final javafx.beans.property.SimpleStringProperty idLimbah;
        private final javafx.beans.property.SimpleStringProperty qty;

        RelasiProdukLimbah(String idProduk, String idLimbah, String qty) {
            this.idProduk = new javafx.beans.property.SimpleStringProperty(idProduk);
            this.idLimbah = new javafx.beans.property.SimpleStringProperty(idLimbah);
            this.qty      = new javafx.beans.property.SimpleStringProperty(qty);
        }
        public String getIdProduk() { return idProduk.get(); }
        public String getIdLimbah() { return idLimbah.get(); }
        public String getQty()      { return qty.get(); }
        public void   setQty(String qty) { this.qty.set(qty); }

        public javafx.beans.property.StringProperty idProdukProperty() { return idProduk; }
        public javafx.beans.property.StringProperty idLimbahProperty() { return idLimbah; }
        public javafx.beans.property.StringProperty qtyProperty()      { return qty; }
    }

    /**
     * Cell editor khusus untuk kolom Qty: hanya menerima digit dan satu titik desimal.
     * Karakter huruf/simbol lain ditolak oleh TextFormatter sehingga tidak akan
     * pernah muncul di kolom input sama sekali (bukan sekadar divalidasi setelah diketik).
     */
    private class QtyEditingCell extends TableCell<RelasiProdukLimbah, String> {
        private TextField textField;

        @Override
        public void startEdit() {
            if (isEmpty()) return;
            super.startEdit();
            createTextField();
            setText(null);
            setGraphic(textField);
            textField.requestFocus();
            textField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                if (textField != null) textField.setText(item);
                setText(null);
                setGraphic(textField);
            } else {
                setText(item);
                setGraphic(null);
            }
        }

        private void createTextField() {
            textField = new TextField(getItem());
            textField.setStyle("-fx-font-size:12px;");

            // Filter: hanya digit, boleh satu titik desimal. Selain itu ditolak total.
            UnaryOperator<TextFormatter.Change> filter = change -> {
                String newText = change.getControlNewText();
                if (newText.isEmpty() || newText.matches("\\d*(\\.\\d*)?")) {
                    return change;
                }
                return null; // tolak perubahan -> karakter tidak akan muncul
            };
            textField.setTextFormatter(new TextFormatter<>(filter));

            textField.setOnAction(e -> commitEdit(textField.getText()));
            textField.focusedProperty().addListener((obs, oldV, newV) -> {
                if (!newV) commitEdit(textField.getText());
            });
            textField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) cancelEdit();
            });
        }
    }

    // ── Initialize ────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupCombo();
        setupHargaRupiah();
        setupTabelRelasi();
        setupPemicuStatusSimpan();
        loadAutoID();
        loadDataLimbah();
        loadDataProduk();

        // Belum ada produk dipilih -> mode tambah baru (Simpan nonaktif sampai ada input)
        aturSebagaiModeTambah();
    }

    private void aturSebagaiModeTambah() {
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);
        // Simpan tidak langsung diaktifkan; aktif hanya jika form sudah diisi.
        updateStatusSimpanJikaModeTambah();
    }

    private void aturSebagaiModeEdit() {
        btnUbah.setDisable(false);
        btnHapus.setDisable(false);
        btnSimpan.setDisable(true);
    }

    /**
     * Memantau perubahan pada Nama Produk dan Harga Jual supaya tombol Simpan
     * otomatis aktif/nonaktif mengikuti isi form (hanya berlaku saat belum ada
     * produk yang dipilih / mode tambah baru).
     */
    private void setupPemicuStatusSimpan() {
        txtNama.textProperty().addListener((obs, oldV, newV) -> updateStatusSimpanJikaModeTambah());
        txtHarga.textProperty().addListener((obs, oldV, newV) -> updateStatusSimpanJikaModeTambah());
    }

    /**
     * Menentukan status disable tombol Simpan.
     * - Jika sedang mode edit (produk sudah dipilih), Simpan tetap nonaktif
     *   (diatur oleh aturSebagaiModeEdit()), method ini tidak mengubahnya.
     * - Jika mode tambah baru, Simpan aktif hanya bila minimal salah satu dari
     *   Nama Produk, Harga Jual, atau pilihan limbah sudah diisi/dipilih.
     */
    private void updateStatusSimpanJikaModeTambah() {
        if (produkDipilih != null) return; // mode edit, tidak diubah di sini

        boolean sudahAdaInput =
                !txtNama.getText().trim().isEmpty()
                        || !getHargaRaw().isEmpty()
                        || !idLimbahTerpilih.isEmpty();

        btnSimpan.setDisable(!sudahAdaInput);
    }

    // ── Setup ─────────────────────────────────────────────────────────────────
    private void setupCombo() {
        cmbSatuan.setItems(FXCollections.observableArrayList("Kg", "Liter"));
        cmbSatuan.getSelectionModel().selectFirst();
    }

    private void setupHargaRupiah() {
        txtHarga.setText(RUPIAH_PREFIX);
        txtHarga.textProperty().addListener((obs, oldV, newV) -> {
            String angka = newV.replaceAll("[^0-9]", "");
            if (angka.length() > 15) angka = angka.substring(0, 15);
            String hasil = RUPIAH_PREFIX + formatRibuan(angka);
            if (!hasil.equals(newV)) {
                txtHarga.setText(hasil);
                txtHarga.positionCaret(hasil.length());
            }
        });
        txtHarga.caretPositionProperty().addListener((obs, oldP, newP) -> {
            if (newP.intValue() < RUPIAH_PREFIX.length())
                txtHarga.positionCaret(RUPIAH_PREFIX.length());
        });
    }

    /**
     * Disalin dari MasterLimbahController: mengubah string digit murni menjadi
     * format dengan pemisah ribuan (titik). Contoh: "2000" -> "2.000",
     * "10000000" -> "10.000.000". Angka nol di depan (kecuali jika hanya "0") ikut dibuang.
     */
    private String formatRibuan(String digitsOnly) {
        if (digitsOnly.isEmpty()) return "";
        digitsOnly = digitsOnly.replaceFirst("^0+(?=\\d)", "");
        return digitsOnly.replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".");
    }

    /** Tabel relasi: ID Produk & ID Limbah readonly, Qty (jumlah limbah per unit produk) editable, hanya angka >= 0 */
    private void setupTabelRelasi() {
        clmRelasiIDProduk  .setCellValueFactory(new PropertyValueFactory<>("idProduk"));
        clmRelasiIDLimbah  .setCellValueFactory(new PropertyValueFactory<>("idLimbah"));
        clmRelasiPresentase.setCellValueFactory(new PropertyValueFactory<>("qty"));

        // ID Produk & ID Limbah tidak bisa diedit
        clmRelasiIDProduk.setEditable(false);
        clmRelasiIDLimbah.setEditable(false);

        // Qty bisa diedit manual (jumlah limbah yang dibutuhkan per 1 unit produk, decimal(10,2))
        // Menggunakan cell editor custom (QtyEditingCell) supaya huruf tidak bisa diketik sama sekali.
        clmRelasiPresentase.setEditable(true);
        clmRelasiPresentase.setCellFactory(col -> new QtyEditingCell());
        clmRelasiPresentase.setOnEditCommit(evt -> {
            RelasiProdukLimbah row = evt.getRowValue();
            String nilaiBaru = evt.getNewValue() == null ? "" : evt.getNewValue().trim();

            if (!nilaiBaru.matches("\\d+(\\.\\d+)?")) {
                showAlert(Alert.AlertType.WARNING, "Validasi", "Qty harus berupa angka.");
                tbRelasi.refresh();
                return;
            }
            double nilai = Double.parseDouble(nilaiBaru);
            if (nilai <= 0) {
                showAlert(Alert.AlertType.WARNING, "Validasi", "Qty harus lebih dari 0.");
                tbRelasi.refresh();
                return;
            }
            row.setQty(nilaiBaru);
            tbRelasi.refresh();
        });

        tbRelasi.setEditable(true);
        tbRelasi.setItems(relasiList);
    }

    // ── Auto ID ───────────────────────────────────────────────────────────────
    private void loadAutoID() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Produk}");
            if (db.result.next()) txtID.setText(db.result.getString("ID_Produk"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    // ── Load daftar limbah → tampilkan sebagai tombol multi-select di panel tengah ─────
    private void loadDataLimbah() {
        vbDaftarLimbah.getChildren().clear();
        limbahButtonMap.clear();
        namaLimbahMap.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                String idLimbah   = db.result.getString("ID_Limbah");
                String namaLimbah = db.result.getString("Nama_Limbah");
                namaLimbahMap.put(idLimbah, namaLimbah);
                Button btnLimbah  = buildTombolLimbah(idLimbah, namaLimbah);
                limbahButtonMap.put(idLimbah, btnLimbah);
                vbDaftarLimbah.getChildren().add(btnLimbah);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Limbah", e.getMessage());
        }
    }

    private Button buildTombolLimbah(String idLimbah, String namaLimbah) {
        Button btn = new Button(namaLimbah);
        btn.setUserData(idLimbah);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(40);
        btn.setFont(Font.font("Segoe UI", 12));
        btn.setStyle(STYLE_TOMBOL_NONAKTIF);

        btn.setOnMouseEntered(e -> {
            if (!idLimbahTerpilih.contains(idLimbah)) btn.setStyle(STYLE_TOMBOL_HOVER);
        });
        btn.setOnMouseExited(e -> {
            if (!idLimbahTerpilih.contains(idLimbah)) btn.setStyle(STYLE_TOMBOL_NONAKTIF);
        });

        btn.setOnAction(e -> toggleLimbah(idLimbah, btn));

        return btn;
    }

    private void toggleLimbah(String idLimbah, Button btn) {
        toggleLimbah(idLimbah, btn, true);
    }

    private void toggleLimbah(String idLimbah, Button btn, boolean recalc) {
        if (idLimbahTerpilih.contains(idLimbah)) {
            idLimbahTerpilih.remove(idLimbah);
            btn.setStyle(STYLE_TOMBOL_NONAKTIF);
        } else {
            idLimbahTerpilih.add(idLimbah);
            btn.setStyle(STYLE_TOMBOL_AKTIF);
        }
        if (recalc) {
            recalcRelasi();
            updateStatusSimpanJikaModeTambah();
        }
    }

    private void resetPilihanLimbah() {
        for (Button btn : limbahButtonMap.values()) btn.setStyle(STYLE_TOMBOL_NONAKTIF);
        idLimbahTerpilih.clear();
        relasiList.clear();
    }

    /**
     * Bentuk ulang isi tabel relasi berdasarkan limbah yang sedang dipilih.
     * Qty (jumlah limbah yang dibutuhkan per 1 unit produk) diberi nilai awal 1
     * untuk limbah yang baru dipilih. Untuk limbah yang tetap terpilih, nilai Qty
     * yang sudah diedit user sebelumnya dipertahankan (tidak direset ke 1 lagi).
     * Nilai boleh diedit manual sesudahnya lewat tabel.
     */
    private void recalcRelasi() {
        Map<String, String> qtyLama = new LinkedHashMap<>();
        for (RelasiProdukLimbah r : relasiList) qtyLama.put(r.getIdLimbah(), r.getQty());

        relasiList.clear();
        String idProduk = txtID.getText();

        for (String idLimbah : idLimbahTerpilih) {
            String qty = qtyLama.getOrDefault(idLimbah, "1");
            relasiList.add(new RelasiProdukLimbah(idProduk, idLimbah, qty));
        }
    }

    private void loadDataProduk() {
        dataList.clear();
        vbKartuProduk.getChildren().clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Produk}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                MasterProduk p = new MasterProduk(
                        db.result.getString("ID_Produk"),
                        db.result.getString("Nama_Produk"),
                        String.valueOf(db.result.getInt("Stok")),
                        RUPIAH_PREFIX + db.result.getBigDecimal("Harga_Jual")
                                .stripTrailingZeros().toPlainString(),
                        db.result.getString("Satuan"),
                        db.result.getString("Keterangan"),
                        db.result.getString("ID_Limbah"),
                        db.result.getString("Status")
                );

                String pathGambar = "";
                try {
                    String hasil = db.result.getString("Path_Gambar");
                    if (hasil != null) pathGambar = hasil;
                } catch (SQLException ignored) {}
                p.setPathGambar(pathGambar);

                dataList.add(p);
                vbKartuProduk.getChildren().add(buildKartu(p));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Produk", e.getMessage());
        }
    }

    private void loadDetailRelasi(String idProduk) {
        relasiList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectByID_DetailProduk(?)}");
            db.cstat.setString(1, idProduk);
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                String idLimbah = db.result.getString("ID_Limbah");
                String qty = db.result.getBigDecimal("Qty").stripTrailingZeros().toPlainString();

                Button btn = limbahButtonMap.get(idLimbah);
                if (btn != null) toggleLimbah(idLimbah, btn, false); // aktifkan visual saja

                relasiList.add(new RelasiProdukLimbah(idProduk, idLimbah, qty));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Detail Produk", e.getMessage());
        }
    }

    private String simpanGambarProduk(File sumber, String idProduk) {
        if (sumber == null || !sumber.exists()) return null;
        try {
            File folder = new File(FOLDER_GAMBAR_PRODUK);
            if (!folder.exists()) folder.mkdirs();

            String namaAsli = sumber.getName();
            String ekstensi = "";
            int titik = namaAsli.lastIndexOf('.');
            if (titik >= 0) ekstensi = namaAsli.substring(titik);

            File tujuan = new File(folder, idProduk + ekstensi);
            Files.copy(sumber.toPath(), tujuan.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tujuan.getAbsolutePath();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan Gambar", e.getMessage());
            return null;
        }
    }

    /** Ambil nama file saja dari path lengkap, supaya label tidak menampilkan path panjang. */
    private String extractNamaFileGambar(String path) {
        if (path == null || path.trim().isEmpty()) return "";
        return new File(path).getName();
    }

    private String buildKomposisiText(String idLimbahCsv) {
        if (idLimbahCsv == null || idLimbahCsv.trim().isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        String[] idArray = idLimbahCsv.split(",");
        for (int i = 0; i < idArray.length; i++) {
            String id = idArray[i].trim();
            if (id.isEmpty()) continue;
            String nama = namaLimbahMap.getOrDefault(id, id);
            if (sb.length() > 0) sb.append(", ");
            sb.append(nama);
        }
        return sb.length() > 0 ? sb.toString() : "-";
    }

    private VBox buildKartu(MasterProduk p) {

        ImageView iv = new ImageView();
        iv.setFitWidth(64);
        iv.setFitHeight(64);
        iv.setPreserveRatio(false);

        String gambarPath = p.getPathGambar();
        boolean gambarLoaded = false;

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
            String resName = p.getNamaProduk().replaceAll("\\s+", "_") + ".png";
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/Gambar_Produk/" + resName);
                if (is != null) {
                    iv.setImage(new Image(is));
                    gambarLoaded = true;
                }
            } catch (Exception ignored) {}
        }

        Rectangle clip = new Rectangle(64, 64);
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        iv.setClip(clip);

        StackPane gambarBox = new StackPane(iv);
        gambarBox.setMinSize(64, 64);
        gambarBox.setMaxSize(64, 64);
        gambarBox.setStyle(
                "-fx-background-color:#F5F5F5;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:#EEEEEE; -fx-border-radius:14;");

        Label lblNama = new Label(p.getNamaProduk());
        lblNama.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1B5E20;");
        lblNama.setWrapText(true);

        Label lblKomposisiJudul = new Label("Komposisi :");
        lblKomposisiJudul.setStyle("-fx-font-size:11px; -fx-text-fill:#9E9E9E;");

        Label lblKomposisiIsi = new Label("• " + buildKomposisiText(p.getIdLimbah()));
        lblKomposisiIsi.setStyle("-fx-font-size:11px; -fx-text-fill:#555555;");
        lblKomposisiIsi.setWrapText(true);

        VBox komposisiBox = new VBox(1, lblKomposisiJudul, lblKomposisiIsi);

        // ── Tampilan Stok (di bawah Komposisi) ───────────────────────────────
        Label lblStokJudul = new Label("Stok :");
        lblStokJudul.setStyle("-fx-font-size:11px; -fx-text-fill:#9E9E9E;");

        int nilaiStok = 0;
        try { nilaiStok = Integer.parseInt(p.getStok().trim()); } catch (Exception ignored) {}

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
                "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:" + warnaStok + ";" +
                        "-fx-background-color:" + bgStok + ";" +
                        "-fx-padding:1 8 1 8; -fx-background-radius:8;");

        HBox stokRow = new HBox(6, lblStokJudul, lblStokIsi);
        stokRow.setAlignment(Pos.CENTER_LEFT);

        Label lblSatuan = new Label(p.getSatuan());
        lblSatuan.setStyle(
                "-fx-background-color:#E8F5E9; -fx-text-fill:#2E7D32;" +
                        "-fx-font-size:10px; -fx-font-weight:bold;" +
                        "-fx-padding:2 10 2 10; -fx-background-radius:10;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox badgeRow = new HBox(lblSatuan);
        badgeRow.setAlignment(Pos.BOTTOM_RIGHT);

        VBox info = new VBox(4, lblNama, komposisiBox, stokRow, spacer, badgeRow);
        info.setAlignment(Pos.TOP_LEFT);
        info.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox isiKartu = new HBox(12, gambarBox, info);
        isiKartu.setAlignment(Pos.TOP_LEFT);
        isiKartu.setMaxWidth(Double.MAX_VALUE);
        isiKartu.setPadding(new Insets(12));

        VBox kartu = new VBox(isiKartu);
        kartu.setMaxWidth(Double.MAX_VALUE);
        kartu.setStyle(STYLE_KARTU_NORMAL);
        kartu.setCursor(javafx.scene.Cursor.HAND);

        kartu.setOnMouseEntered(e -> {
            if (kartu != kartuAktif) kartu.setStyle(STYLE_KARTU_HOVER);
        });
        kartu.setOnMouseExited(e -> {
            if (kartu != kartuAktif) kartu.setStyle(STYLE_KARTU_NORMAL);
        });
        kartu.setOnMouseClicked(e -> pilihProduk(p, kartu));

        return kartu;
    }

    private void pilihProduk(MasterProduk p, VBox kartu) {

        if (kartuAktif != null) kartuAktif.setStyle(STYLE_KARTU_NORMAL);

        kartuAktif    = kartu;
        produkDipilih = p;
        kartu.setStyle(STYLE_KARTU_AKTIF);

        txtID.setText(p.getIdProduk());
        txtNama.setText(p.getNamaProduk());
        cmbSatuan.setValue(p.getSatuan());
        String rawHarga = p.getHargaJual().replace(RUPIAH_PREFIX, "").trim();
        txtHarga.setText(RUPIAH_PREFIX + rawHarga);
        // Tampilkan hanya nama file (bukan path lengkap) supaya tampilan tetap
        // sama persis dengan kondisi awal (tombol "Pilih Gambar" + label singkat),
        // tidak berubah jadi kotak path panjang.
        String namaFileGambar = extractNamaFileGambar(p.getPathGambar());
        lblGambar.setText(namaFileGambar.isEmpty() ? "Belum ada gambar" : namaFileGambar);
        lblGambar.setStyle(STYLE_LABEL_GAMBAR);
        pathGambarAktif = p.getPathGambar() != null ? p.getPathGambar() : "";
        filGambarDipilih = null;

        resetPilihanLimbah();
        loadDetailRelasi(p.getIdProduk());

        // Produk sudah dipilih -> aktifkan Ubah & Hapus, nonaktifkan Simpan
        aturSebagaiModeEdit();
    }

    @FXML
    private void handleUploadGambar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Pilih Gambar Produk");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Gambar", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fc.showOpenDialog(btnUpload.getScene().getWindow());
        if (file != null) {
            filGambarDipilih = file;
            pathGambarAktif  = file.getAbsolutePath();
            lblGambar.setText(file.getName());
        }
    }

    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;
        if (idLimbahTerpilih.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Pilih minimal satu jenis limbah di panel tengah.");
            return;
        }
        try {
            String pathGambarFinal = "";
            if (filGambarDipilih != null) {
                String hasil = simpanGambarProduk(filGambarDipilih, txtID.getText());
                if (hasil != null) pathGambarFinal = hasil;
            }

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Produk(?,?,?,?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText());
            db.cstat.setInt(3, 0); // stok awal 0
            db.cstat.setBigDecimal(4, new BigDecimal(getHargaRaw()));
            db.cstat.setString(5, cmbSatuan.getValue());
            db.cstat.setString(6, "");
            db.cstat.setString(7, "Aktif");
            db.cstat.setString(8, pathGambarFinal);
            db.cstat.executeUpdate();
            simpanDetailProduk(txtID.getText());

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Produk berhasil disimpan.");
            clearForm(); loadDataProduk(); loadAutoID();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    private void simpanDetailProduk(String idProduk) throws SQLException {
        for (RelasiProdukLimbah r : relasiList) {
            db.cstat = db.conn.prepareCall("{CALL sp_Insert_DetailProduk(?,?,?)}");
            db.cstat.setString(1, idProduk);
            db.cstat.setString(2, r.getIdLimbah());
            db.cstat.setBigDecimal(3, new BigDecimal(r.getQty()));
            db.cstat.executeUpdate();
        }
    }

    @FXML
    private void handleUbah() {
        if (produkDipilih == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih produk dari panel kanan terlebih dahulu.");
            return;
        }
        if (!validateForm()) return;
        if (idLimbahTerpilih.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Pilih minimal satu jenis limbah di panel tengah.");
            return;
        }
        try {

            String pathGambarFinal = pathGambarAktif;
            if (filGambarDipilih != null) {
                String hasil = simpanGambarProduk(filGambarDipilih, txtID.getText());
                if (hasil != null) pathGambarFinal = hasil;
            }

            db.cstat = db.conn.prepareCall("{CALL sp_Update_Produk(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText());
            db.cstat.setBigDecimal(3, new BigDecimal(getHargaRaw()));
            db.cstat.setString(4, cmbSatuan.getValue());
            db.cstat.setString(5, "");
            db.cstat.setString(6, "Aktif");
            db.cstat.setString(7, pathGambarFinal);
            db.cstat.executeUpdate();
            hapusDetailProduk(txtID.getText());
            simpanDetailProduk(txtID.getText());

            produkDipilih.setPathGambar(pathGambarFinal);

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Produk berhasil diubah.");
            clearForm(); loadDataProduk(); loadAutoID();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Ubah", e.getMessage());
        }
    }

    @FXML
    private void handleHapus() {
        if (produkDipilih == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih produk dari panel kanan terlebih dahulu.");
            return;
        }
        Alert k = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin menghapus produk: " + produkDipilih.getNamaProduk() + "?",
                ButtonType.YES, ButtonType.NO);
        k.setTitle("Konfirmasi Hapus");
        k.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    db.cstat = db.conn.prepareCall("{CALL sp_Delete_Produk(?)}");
                    db.cstat.setString(1, produkDipilih.getIdProduk());
                    db.cstat.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Produk berhasil dihapus.");
                    clearForm(); loadDataProduk(); loadAutoID();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Error Hapus", e.getMessage());
                }
            }
        });
    }

    private void hapusDetailProduk(String idProduk) throws SQLException {
        db.cstat = db.conn.prepareCall("{CALL sp_Delete_DetailProduk(?)}");
        db.cstat.setString(1, idProduk);
        db.cstat.executeUpdate();
    }

    @FXML
    private void handleBatal() {
        clearForm();
        loadAutoID();
    }

    private boolean validateForm() {
        if (txtNama.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Nama Produk wajib diisi.");
            return false;
        }
        if (cmbSatuan.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Satuan wajib dipilih.");
            return false;
        }
        String rawHarga = getHargaRaw();
        if (rawHarga.isEmpty() || rawHarga.equals("0")) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Harga Jual wajib diisi.");
            return false;
        }

        // Validasi nama produk tidak boleh sama (case-insensitive).
        // Saat mode Ubah, produk yang sedang diedit dikecualikan dari pengecekan.
        String idDikecualikan = produkDipilih != null ? produkDipilih.getIdProduk() : null;
        if (namaProdukSudahAda(txtNama.getText(), idDikecualikan)) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Nama Produk sudah digunakan, gunakan nama lain.");
            return false;
        }

        return true;
    }

    /**
     * Mengecek apakah nama produk sudah dipakai oleh produk lain.
     * @param nama nama produk yang akan disimpan/diubah
     * @param idProdukDikecualikan ID produk yang sedang diedit (diabaikan dari pengecekan), null jika mode tambah baru
     */
    private boolean namaProdukSudahAda(String nama, String idProdukDikecualikan) {
        for (MasterProduk p : dataList) {
            if (idProdukDikecualikan != null && p.getIdProduk().equals(idProdukDikecualikan)) {
                continue; // lewati produk yang sedang diedit
            }
            if (p.getNamaProduk().trim().equalsIgnoreCase(nama.trim())) {
                return true;
            }
        }
        return false;
    }

    private String getHargaRaw() {
        return txtHarga.getText().replace(RUPIAH_PREFIX, "").replace(".", "").trim();
    }

    private void clearForm() {
        txtNama.clear();
        cmbSatuan.getSelectionModel().selectFirst();
        txtHarga.setText(RUPIAH_PREFIX);
        lblGambar.setText("Belum ada gambar");
        lblGambar.setStyle(STYLE_LABEL_GAMBAR);
        filGambarDipilih = null;
        pathGambarAktif  = "";
        produkDipilih    = null;

        // PENTING: reset style kartu yang sebelumnya aktif ke normal SEBELUM
        // referensinya di-null-kan. Sebelumnya kartuAktif langsung di-null-kan
        // tanpa mengembalikan style-nya, sehingga kartu produk yang terakhir
        // dipilih tetap terlihat hijau/aktif walau produk sudah tidak dipilih.
        if (kartuAktif != null) {
            kartuAktif.setStyle(STYLE_KARTU_NORMAL);
            kartuAktif = null;
        }

        resetPilihanLimbah();

        // Kembali ke mode tambah baru: Ubah & Hapus nonaktif, Simpan aktif
        aturSebagaiModeTambah();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}