package Transaksi;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TransaksiPengolahanLimbahController implements Initializable {

    // ===================== FXML — FORM KIRI =====================
    @FXML private TextField  txtIDPengolahan;
    @FXML private TextField  txtIDProduk;
    @FXML private DatePicker jpTanggal;
    @FXML private ComboBox<String> cmbJenisProduk;
    @FXML private TextField  txtKuantitas;
    @FXML private ComboBox<String> cmbSatuan;
    @FXML private TextArea   txtKeterangan;

    // ===================== FXML — PANEL KANAN (kartu produk) =====================
    @FXML private GridPane    gpKartuProduk;
    @FXML private TextField   txtCariProduk;
    @FXML private ToggleGroup tgProduk;

    @FXML private ToggleButton cardPupukOrganikPadat;
    @FXML private ToggleButton cardPupukOrganikCair;
    @FXML private ToggleButton cardKompos;
    @FXML private ToggleButton cardBooster;
    @FXML private ToggleButton cardPupukNitrogen;
    @FXML private ToggleButton cardPupukKalsium;

    // ===================== KONSTANTA JENIS PRODUK =====================
    private static final String PUPUK_ORGANIK_PADAT       = "Pupuk Organik Padat";
    private static final String PUPUK_ORGANIK_CAIR        = "Pupuk Organik Cair";
    private static final String KOMPOS                    = "Kompos";
    private static final String BOOSTER                   = "Booster";
    private static final String PUPUK_NITROGEN_TINGGI_UDANG = "Pupuk Nitrogen Tinggi Udang";
    private static final String PUPUK_KALSIUM             = "Pupuk Kalsium";

    // ===================== STYLE KARTU (sama persis dengan MasterProdukController) =====================
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

    private static final double CARD_HEIGHT = 120.0;
    private static final double IMG_SIZE    =  80.0;

    // ===================== INNER CLASS: TEMPLATE PRODUK =====================
    /**
     * Menyimpan metadata satu kartu produk:
     *  - namaProduk : label yang tampil di kartu & nilai yang masuk ke cmbJenisProduk
     *  - satuan     : "Kg" atau "Liter", langsung dipilihkan di cmbSatuan
     *  - keterangan : komposisi ditampilkan di kartu (tidak ditulis ke form)
     *  - namaFile   : nama file gambar di /Gambar_Produk/
     */
    private static class ProdukTemplate {
        final String namaProduk, satuan, keterangan, namaFile;
        ProdukTemplate(String n, String s, String k, String f) {
            namaProduk = n; satuan = s; keterangan = k; namaFile = f;
        }
    }

    // ===================== ATURAN PENGURANGAN BAHAN LIMBAH =====================
    private static class BahanReduction {
        final String namaBahan;
        final BigDecimal jumlahPerUnit;
        final String satuanBahan;
        BahanReduction(String nb, BigDecimal j, String sb) {
            namaBahan = nb; jumlahPerUnit = j; satuanBahan = sb;
        }
    }

    private static final Map<String, List<BahanReduction>> ATURAN_PENGURANGAN = new HashMap<>();
    static {
        ATURAN_PENGURANGAN.put(PUPUK_ORGANIK_PADAT, Arrays.asList(
                new BahanReduction("Lumpur",       new BigDecimal("0.5"), "Kg"),
                new BahanReduction("Kotoran",      new BigDecimal("0.5"), "Kg")
        ));
        ATURAN_PENGURANGAN.put(PUPUK_ORGANIK_CAIR, Arrays.asList(
                new BahanReduction("Air Limbah Tambak", new BigDecimal("0.5"), "Liter")
        ));
        ATURAN_PENGURANGAN.put(KOMPOS, Arrays.asList(
                new BahanReduction("Lumpur",       new BigDecimal("0.5"), "Kg")
        ));
        ATURAN_PENGURANGAN.put(PUPUK_NITROGEN_TINGGI_UDANG, Arrays.asList(
                new BahanReduction("Kotoran",      new BigDecimal("0.5"), "Kg"),
                new BahanReduction("Bangkai Udang",new BigDecimal("0.5"), "Kg")
        ));
        ATURAN_PENGURANGAN.put(PUPUK_KALSIUM, Arrays.asList(
                new BahanReduction("Cangkang",     BigDecimal.ONE,        "Kg")
        ));
        ATURAN_PENGURANGAN.put(BOOSTER, Arrays.asList(
                new BahanReduction("Cangkang",     BigDecimal.ONE,        "Kg")
        ));
    }

    // ===================== STATE =====================
    private final DBConnect db = new DBConnect();
    private final List<ToggleButton> daftarKartu = new ArrayList<>();

    // ===================== INITIALIZE =====================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupComboJenisProduk();
        setupComboSatuan();
        addNumericOnly(txtKuantitas, 8);

        setupKartuProduk();   // bangun kartu di panel kanan
        loadAutoIDPengolahan();
    }

    // ===================== SETUP COMBO =====================
    private void setupComboJenisProduk() {
        cmbJenisProduk.setItems(FXCollections.observableArrayList(
                PUPUK_ORGANIK_PADAT, PUPUK_ORGANIK_CAIR, KOMPOS,
                BOOSTER, PUPUK_NITROGEN_TINGGI_UDANG, PUPUK_KALSIUM
        ));
    }

    private void setupComboSatuan() {
        cmbSatuan.setItems(FXCollections.observableArrayList("Botol", "Kg", "Liter"));
    }

    private void addNumericOnly(TextField field, int maxLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9.]", "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    // ===================== SETUP KARTU PRODUK =====================
    private void setupKartuProduk() {
        List<ToggleButton> cards = Arrays.asList(
                cardPupukOrganikPadat, cardPupukOrganikCair,
                cardKompos, cardBooster,
                cardPupukNitrogen, cardPupukKalsium
        );

        // Template: urutan HARUS sama dengan urutan cards di atas
        ProdukTemplate[] templates = {
                new ProdukTemplate(PUPUK_ORGANIK_PADAT,         "Kg",    "Lumpur, Kotoran",         "Pupuk_Organik_Padat.png"),
                new ProdukTemplate(PUPUK_ORGANIK_CAIR,          "Liter", "Air Limbah Tambak",       "Pupuk_Organik_Cair.png"),
                new ProdukTemplate(KOMPOS,                      "Kg",    "Lumpur",                  "Pupuk_Kompos.png"),
                new ProdukTemplate(BOOSTER,                     "Kg",    "Cangkang Udang",          "Pupuk_Booster.png"),
                new ProdukTemplate(PUPUK_NITROGEN_TINGGI_UDANG, "Kg",    "Kotoran, Bangkai Udang",  "Pupuk_Nitrogen_Padat.png"),
                new ProdukTemplate(PUPUK_KALSIUM,               "Kg",    "Cangkang Udang",          "Pupuk_Kalsium.png")
        };

        for (int i = 0; i < cards.size(); i++) {
            ToggleButton card = cards.get(i);
            ProdukTemplate t  = templates[i];
            card.setUserData(t);
            card.setStyle(CARD_NORMAL);

            // Ukuran kartu
            card.setPrefHeight(CARD_HEIGHT);
            card.setMinHeight(CARD_HEIGHT);
            card.setMaxHeight(CARD_HEIGHT);
            card.setMaxWidth(Double.MAX_VALUE);

            card.setGraphic(buildKartuGraphic(t));

            // Hover style
            card.setOnMouseEntered(e -> { if (!card.isSelected()) card.setStyle(CARD_HOVER); });
            card.setOnMouseExited (e -> { if (!card.isSelected()) card.setStyle(CARD_NORMAL); });
        }

        daftarKartu.clear();
        daftarKartu.addAll(cards);

        // ── Listener: saat kartu dipilih → auto-fill form ──
        tgProduk.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            // Reset semua kartu ke NORMAL dulu
            for (ToggleButton c : cards) c.setStyle(CARD_NORMAL);

            if (newT == null) return;

            ToggleButton sel = (ToggleButton) newT;
            sel.setStyle(CARD_SELECTED);
            ProdukTemplate t = (ProdukTemplate) sel.getUserData();

            String idProdukDariDB = cariIDProdukDariDB(t.namaProduk);

            // Isi form: ID Produk, Jenis Produk, Satuan
            txtIDProduk.setText(idProdukDariDB != null ? idProdukDariDB : "");
            cmbJenisProduk.setValue(t.namaProduk);
            cmbSatuan.setValue(t.satuan);
        });
    }

    private String cariIDProdukDariDB(String namaProduk) {
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Produk}");
            db.result = db.cstat.executeQuery();

            String idExact    = null;
            String idContains = null;

            while (db.result.next()) {
                String namaDB = db.result.getString("Nama_Produk");
                String idDB   = db.result.getString("ID_Produk");
                if (namaDB == null) continue;

                // Cocokkan exact (case-insensitive)
                if (namaDB.trim().equalsIgnoreCase(namaProduk.trim())) {
                    idExact = idDB;
                    break;
                }

                // Fallback: salah satu mengandung yang lain
                String namaDBLower    = namaDB.trim().toLowerCase(Locale.ROOT);
                String namaProdukLower = namaProduk.trim().toLowerCase(Locale.ROOT);
                if (namaDBLower.contains(namaProdukLower) || namaProdukLower.contains(namaDBLower)) {
                    if (idContains == null) idContains = idDB; // ambil yang pertama cocok
                }
            }

            return idExact != null ? idExact : idContains;

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Cari ID Produk", e.getMessage());
            return null;
        }
    }

    private javafx.scene.Node buildKartuGraphic(ProdukTemplate t) {
        // Aksen hijau di sisi kiri
        Region aksen = new Region();
        aksen.setPrefWidth(5);
        aksen.setMinWidth(5);
        aksen.setMaxHeight(Double.MAX_VALUE);
        aksen.setStyle("-fx-background-color:#2E7D32; -fx-background-radius:10 0 0 10;");

        // Gambar produk
        ImageView iv = new ImageView();
        iv.setFitWidth(IMG_SIZE);
        iv.setFitHeight(IMG_SIZE);
        iv.setPreserveRatio(true);
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/Gambar_Produk/" + t.namaFile);
            if (is != null) iv.setImage(new Image(is));
        } catch (Exception ignored) {}

        Label lblNama = new Label(t.namaProduk);
        lblNama.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1B5E20;");
        lblNama.setWrapText(true);
        lblNama.setMaxWidth(Double.MAX_VALUE);

        Label lblKomTitle = new Label("Komposisi :");
        lblKomTitle.setStyle("-fx-font-size:11px; -fx-text-fill:#9E9E9E;");

        Label lblKom = new Label("• " + t.keterangan);
        lblKom.setStyle("-fx-font-size:11px; -fx-text-fill:#757575;");
        lblKom.setWrapText(true);
        lblKom.setMaxWidth(Double.MAX_VALUE);

        Label lblBadge = new Label(t.satuan);
        lblBadge.setStyle(
                "-fx-background-color:#E8F5E9;" +
                        "-fx-text-fill:#2E7D32;" +
                        "-fx-font-size:10px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-padding:2 8 2 8;" +
                        "-fx-background-radius:8;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox badgeRow = new HBox(lblBadge);
        badgeRow.setAlignment(Pos.BOTTOM_RIGHT);

        VBox teks = new VBox(4, lblNama, lblKomTitle, lblKom, spacer, badgeRow);
        teks.setAlignment(Pos.TOP_LEFT);
        teks.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(teks, Priority.ALWAYS);

        HBox root = new HBox(12, aksen, iv, teks);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMaxHeight(CARD_HEIGHT);
        root.setStyle("-fx-padding:12 16 12 0;");
        HBox.setHgrow(root, Priority.ALWAYS);

        return root;
    }

    // ===================== CARI PRODUK (filter kartu) =====================
    @FXML
    private void handleCariProduk() {
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
                col++;
                if (col == 2) { col = 0; row++; }
            }
        }
    }

    // ===================== AUTO ID =====================
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

    // ===================== EVENT: KUANTITAS DIISI =====================
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
        sb.append("Bahan limbah yang akan berkurang untuk ")
                .append(kuantitas).append(" ").append(jenis).append(":\n");
        for (BahanReduction b : aturan) {
            BigDecimal total = b.jumlahPerUnit.multiply(kuantitas).setScale(2, RoundingMode.HALF_UP);
            sb.append("- ").append(b.namaBahan).append(" : ")
                    .append(total).append(" ").append(b.satuanBahan).append("\n");
        }
        showAlert(Alert.AlertType.INFORMATION, "Perkiraan Pengurangan Bahan Limbah", sb.toString());
    }

    private BigDecimal parseKuantitas() {
        try {
            String text = txtKuantitas.getText().trim();
            if (text.isEmpty()) return null;
            BigDecimal val = new BigDecimal(text);
            return val.compareTo(BigDecimal.ZERO) <= 0 ? null : val;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ===================== EVENT: SATUAN DIPILIH =====================
    @FXML
    private void cmbSaatuan() {
        // Bisa ditambahkan logika lanjutan jika diperlukan
    }

    // ===================== SIMPAN TRANSAKSI =====================
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
            // 1. Simpan header transaksi
            db.cstat = db.conn.prepareCall("{CALL sp_Insert_PengolahanLimbah(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, idPengolahan);
            db.cstat.setString(2, idProduk);
            db.cstat.setString(3, tanggal);
            db.cstat.setString(4, jenisProduk);
            db.cstat.setBigDecimal(5, kuantitas);
            db.cstat.setString(6, satuan);
            db.cstat.setString(7, keterangan);
            db.cstat.executeUpdate();

            // 2. Kurangi stok bahan limbah sesuai aturan
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
            showAlert(Alert.AlertType.WARNING, "Validasi", "Pilih produk dari daftar kartu di kanan terlebih dahulu.");
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

        // Deselect kartu yang aktif
        if (tgProduk != null) tgProduk.selectToggle(null);
        for (ToggleButton c : daftarKartu) c.setStyle(CARD_NORMAL);

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