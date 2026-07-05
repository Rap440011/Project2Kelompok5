package Transaksi;

import Connection.DBConnect;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

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

    // ===================== FXML — PANEL KARTU PRODUK =====================
    @FXML private GridPane    gpKartuProduk;
    @FXML private TextField   txtCariProduk;
    @FXML private ToggleGroup tgProduk;

    @FXML private ToggleButton cardPupukOrganikPadat;
    @FXML private ToggleButton cardPupukOrganikCair;
    @FXML private ToggleButton cardKompos;
    @FXML private ToggleButton cardBooster;
    @FXML private ToggleButton cardPupukNitrogen;
    @FXML private ToggleButton cardPupukKalsium;

    // ===================== STYLE KARTU (sama dengan MasterProdukController) =====================
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

    private static final double CARD_HEIGHT = 100.0;
    private static final double IMG_SIZE    =  72.0;

    // ===================== INNER CLASS: TEMPLATE PRODUK =====================
    private static class ProdukTemplate {
        final String namaProduk, satuan, keterangan, namaFile;
        ProdukTemplate(String n, String s, String k, String f) {
            namaProduk = n; satuan = s; keterangan = k; namaFile = f;
        }
    }

    // ===================== STATE =====================
    private final DBConnect db = new DBConnect();
    private final List<ToggleButton> daftarKartu = new ArrayList<>();

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

        // Bangun kartu produk
        setupKartuProduk();

        // Panel detail & kartu nonaktif di awal
        setPanelDetailEnabled(false);

        loadAutoIDPenjualan();
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

    // ===================== ENABLE / DISABLE PANEL DETAIL & KARTU =====================
    private void setPanelDetailEnabled(boolean enabled) {
        // Field detail
        txtJumlah.setDisable(!enabled);
        btnSelesai.setDisable(!enabled);
        btnBatal.setDisable(!enabled);
        btnTambahProduk.setDisable(!enabled);

        // Kartu produk
        for (ToggleButton card : daftarKartu) {
            card.setDisable(!enabled);
            card.setStyle(enabled ? CARD_NORMAL : CARD_DISABLED);
            card.setMouseTransparent(!enabled);
        }

        if (!enabled && tgProduk != null) {
            tgProduk.selectToggle(null);
        }
    }

    // ===================== SETUP KARTU PRODUK =====================
    private void setupKartuProduk() {
        List<ToggleButton> cards = Arrays.asList(
                cardPupukOrganikPadat, cardPupukOrganikCair,
                cardKompos, cardBooster,
                cardPupukNitrogen, cardPupukKalsium
        );

        ProdukTemplate[] templates = {
            new ProdukTemplate("Pupuk Organik Padat",        "Kg",    "Lumpur, Kotoran",        "Pupuk_Organik_Padat.png"),
            new ProdukTemplate("Pupuk Organik Cair",         "Liter", "Air Limbah Tambak",      "Pupuk_Organik_Cair.png"),
            new ProdukTemplate("Kompos",                     "Kg",    "Lumpur",                 "Pupuk_Kompos.png"),
            new ProdukTemplate("Booster",                    "Kg",    "Cangkang Udang",         "Pupuk_Booster.png"),
            new ProdukTemplate("Pupuk Nitrogen Tinggi Udang","Kg",    "Kotoran, Bangkai Udang", "Pupuk_Nitrogen_Padat.png"),
            new ProdukTemplate("Pupuk Kalsium",              "Kg",    "Cangkang Udang",         "Pupuk_Kalsium.png")
        };

        for (int i = 0; i < cards.size(); i++) {
            ToggleButton card = cards.get(i);
            ProdukTemplate t  = templates[i];
            card.setUserData(t);
            card.setStyle(CARD_DISABLED);   // nonaktif di awal
            card.setDisable(true);
            card.setMouseTransparent(true);

            card.setPrefHeight(CARD_HEIGHT);
            card.setMinHeight(CARD_HEIGHT);
            card.setMaxHeight(CARD_HEIGHT);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setGraphic(buildKartuGraphic(t));

            card.setOnMouseEntered(e -> {
                if (!card.isDisable() && !card.isSelected()) card.setStyle(CARD_HOVER);
            });
            card.setOnMouseExited(e -> {
                if (!card.isDisable() && !card.isSelected()) card.setStyle(CARD_NORMAL);
            });
        }

        daftarKartu.clear();
        daftarKartu.addAll(cards);

        // Listener: saat kartu diklik → auto-fill ID Produk & Harga Jual
        tgProduk.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            for (ToggleButton c : cards) {
                if (!c.isDisable()) c.setStyle(CARD_NORMAL);
            }
            if (newT == null) return;

            ToggleButton sel = (ToggleButton) newT;
            sel.setStyle(CARD_SELECTED);
            ProdukTemplate t = (ProdukTemplate) sel.getUserData();

            // Ambil ID Produk & Harga Jual dari database
            isikanProdukDariDB(t.namaProduk);
        });
    }

    /**
     * Query sp_SelectAll_Produk, cocokkan nama, lalu isi txtIDProduk & txtHargaJual.
     */
    private void isikanProdukDariDB(String namaProduk) {
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Produk}");
            db.result = db.cstat.executeQuery();

            String     idFound   = null;
            BigDecimal hargaFound = BigDecimal.ZERO;

            // Normalisasi nama template: huruf kecil, buang angka+satuan, rapikan spasi
            String targetNorm = normalisasiNama(namaProduk);

            while (db.result.next()) {
                String namaDB = db.result.getString("Nama_Produk");
                if (namaDB == null) continue;

                String namaDBNorm = normalisasiNama(namaDB);

                // Cocok jika salah satu mengandung yang lain (setelah dinormalisasi)
                if (namaDBNorm.equals(targetNorm)
                        || namaDBNorm.contains(targetNorm)
                        || targetNorm.contains(namaDBNorm)) {
                    idFound    = db.result.getString("ID_Produk");
                    BigDecimal h = db.result.getBigDecimal("Harga_Jual");
                    hargaFound = (h != null) ? h : BigDecimal.ZERO;
                    break;
                }
            }

            if (idFound != null) {
                txtIDProduk.setText(idFound);
                txtHargaJual.setText(hargaFound.toPlainString());
            } else {
                txtIDProduk.clear();
                txtHargaJual.clear();
                showAlert(Alert.AlertType.WARNING, "Peringatan",
                        "Produk '" + namaProduk + "' belum terdaftar di Master Produk.");
            }
            txtJumlah.clear();
            txtSubtotal.clear();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
        }
    }

    /**
     * Normalisasi nama: huruf kecil, buang angka + satuan (1Kg, 500Liter, dst),
     * rapikan spasi — sama persis dengan MasterProdukController.normalisasiNama().
     */
    private String normalisasiNama(String s) {
        return s.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\d+\\s*(kg|liter|l|botol|pcs|ton|gram)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Bangun graphic satu kartu — identik dengan MasterProdukController */
    private javafx.scene.Node buildKartuGraphic(ProdukTemplate t) {
        Region aksen = new Region();
        aksen.setPrefWidth(5); aksen.setMinWidth(5);
        aksen.setMaxHeight(Double.MAX_VALUE);
        aksen.setStyle("-fx-background-color:#2E7D32;-fx-background-radius:10 0 0 10;");

        ImageView iv = new ImageView();
        iv.setFitWidth(IMG_SIZE); iv.setFitHeight(IMG_SIZE);
        iv.setPreserveRatio(true);
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/Gambar_Produk/" + t.namaFile);
            if (is != null) iv.setImage(new Image(is));
        } catch (Exception ignored) {}

        Label lblNama = new Label(t.namaProduk);
        lblNama.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1B5E20;");
        lblNama.setWrapText(true); lblNama.setMaxWidth(Double.MAX_VALUE);

        Label lblKomTitle = new Label("Komposisi :");
        lblKomTitle.setStyle("-fx-font-size:11px;-fx-text-fill:#9E9E9E;");

        Label lblKom = new Label("• " + t.keterangan);
        lblKom.setStyle("-fx-font-size:11px;-fx-text-fill:#757575;");
        lblKom.setWrapText(true); lblKom.setMaxWidth(Double.MAX_VALUE);

        Label lblBadge = new Label(t.satuan);
        lblBadge.setStyle("-fx-background-color:#E8F5E9;-fx-text-fill:#2E7D32;" +
                          "-fx-font-size:10px;-fx-font-weight:bold;" +
                          "-fx-padding:2 8 2 8;-fx-background-radius:8;");

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        HBox badgeRow = new HBox(lblBadge); badgeRow.setAlignment(Pos.BOTTOM_RIGHT);

        VBox teks = new VBox(4, lblNama, lblKomTitle, lblKom, spacer, badgeRow);
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
        int col = 0, row = 0;
        gpKartuProduk.getChildren().clear();
        for (ToggleButton card : daftarKartu) {
            boolean cocok = true;
            if (!keyword.isEmpty() && card.getUserData() instanceof ProdukTemplate) {
                cocok = ((ProdukTemplate) card.getUserData())
                        .namaProduk.toLowerCase(Locale.ROOT).contains(keyword);
            }
            if (cocok) {
                GridPane.setColumnIndex(card, col);
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
        if (tgProduk != null) tgProduk.selectToggle(null);
        for (ToggleButton c : daftarKartu) {
            if (!c.isDisable()) c.setStyle(CARD_NORMAL);
        }
    }

    // ===================== TOMBOL TAMBAH PRODUK (lanjut input detail berikutnya) =====================
    @FXML
    private void handleTambahProduk() {
        // ID Penjualan tetap, hanya refresh ID Detail untuk entri berikutnya
        bersihkanFormDetail();
        loadAutoIDDetail();
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
        try { Integer.parseInt(txtJumlah.getText().trim()); }
        catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Jumlah harus berupa angka.");
            return false;
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

        setPanelDetailEnabled(false);
        headerLengkap = false;

        loadAutoIDPenjualan();
    }

    // ===================== UTIL =====================
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
