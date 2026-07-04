package Transaksi;

import Connection.DBConnect;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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

    // ── FXML Form kiri ────────────────────────────────────────────────────────
    @FXML private TextField    txtIDPengolahan;
    @FXML private TextField    txtIDProduk;
    @FXML private DatePicker   jpTanggal;
    @FXML private ComboBox<String> cmbJenisProduk;
    @FXML private TextField    txtKuantitas;
    @FXML private ComboBox<String> cmbSatuan;
    @FXML private TextArea     txtKeterangan;

    // ── FXML Panel kanan ──────────────────────────────────────────────────────
    @FXML private GridPane     gpKartuProduk;
    @FXML private TextField    txtCariProduk;
    @FXML private ToggleGroup  tgProduk;

    @FXML private ToggleButton cardPupukOrganikPadat;
    @FXML private ToggleButton cardPupukOrganikCair;
    @FXML private ToggleButton cardKompos;
    @FXML private ToggleButton cardBooster;
    @FXML private ToggleButton cardPupukNitrogen;
    @FXML private ToggleButton cardPupukKalsium;

    // ── FXML Detail Transaksi (bawah form, read-only, otomatis) ──────────────
    @FXML private TableView<DetailTransaksi>              tblDetailTransaksi;
    @FXML private TableColumn<DetailTransaksi, String>     colIDDetail;
    @FXML private TableColumn<DetailTransaksi, String>     colIDPengolahanDetail;
    @FXML private TableColumn<DetailTransaksi, String>     colIDLimbah;
    @FXML private TableColumn<DetailTransaksi, String>     colKuantitasLimbah;
    @FXML private TableColumn<DetailTransaksi, String>     colSatuanLimbah;

    // ── Konstanta nama produk ─────────────────────────────────────────────────
    private static final String PUPUK_ORGANIK_PADAT        = "Pupuk Organik Padat";
    private static final String PUPUK_ORGANIK_CAIR         = "Pupuk Organik Cair";
    private static final String KOMPOS                     = "Kompos";
    private static final String BOOSTER                    = "Booster";
    private static final String PUPUK_NITROGEN_TINGGI_UDANG = "Pupuk Nitrogen Tinggi Udang";
    private static final String PUPUK_KALSIUM              = "Pupuk Kalsium";

    // ── Style kartu ───────────────────────────────────────────────────────────
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

    // ── Inner class template produk ───────────────────────────────────────────
    private static class ProdukTemplate {
        final String namaProduk, satuan, keterangan, namaFile;
        ProdukTemplate(String n, String s, String k, String f) {
            namaProduk = n; satuan = s; keterangan = k; namaFile = f;
        }
    }

    // ── Aturan pengurangan bahan limbah ──────────────────────────────────────
    private static class BahanReduction {
        final String namaBahan;
        final BigDecimal jumlahPerUnit;
        final String satuanBahan;
        BahanReduction(String nb, BigDecimal j, String sb) {
            namaBahan = nb; jumlahPerUnit = j; satuanBahan = sb;
        }
    }

    /**
     * Model baris "Detail Transaksi" yang ditampilkan di TableView.
     * Seluruhnya otomatis/read-only — tidak ada field yang bisa diisi user.
     */
    public static class DetailTransaksi {
        private final SimpleStringProperty idDetail;
        private final SimpleStringProperty idPengolahan;
        private final SimpleStringProperty idLimbah;
        private final SimpleStringProperty namaLimbah;
        private final SimpleStringProperty kuantitasLimbah;
        private final SimpleStringProperty satuanLimbah;

        public DetailTransaksi(String idDetail, String idPengolahan, String idLimbah,
                                String namaLimbah, String kuantitasLimbah, String satuanLimbah) {
            this.idDetail        = new SimpleStringProperty(idDetail);
            this.idPengolahan    = new SimpleStringProperty(idPengolahan);
            this.idLimbah        = new SimpleStringProperty(idLimbah);
            this.namaLimbah      = new SimpleStringProperty(namaLimbah);
            this.kuantitasLimbah = new SimpleStringProperty(kuantitasLimbah);
            this.satuanLimbah    = new SimpleStringProperty(satuanLimbah);
        }

        public String getIdDetail()        { return idDetail.get(); }
        public String getIdPengolahan()    { return idPengolahan.get(); }
        public String getIdLimbah()        { return idLimbah.get(); }
        public String getNamaLimbah()      { return namaLimbah.get(); }
        public String getKuantitasLimbah() { return kuantitasLimbah.get(); }
        public String getSatuanLimbah()    { return satuanLimbah.get(); }

        public SimpleStringProperty idDetailProperty()        { return idDetail; }
        public SimpleStringProperty idPengolahanProperty()     { return idPengolahan; }
        public SimpleStringProperty idLimbahProperty()         { return idLimbah; }
        public SimpleStringProperty namaLimbahProperty()       { return namaLimbah; }
        public SimpleStringProperty kuantitasLimbahProperty()  { return kuantitasLimbah; }
        public SimpleStringProperty satuanLimbahProperty()     { return satuanLimbah; }
    }

    private static final Map<String, List<BahanReduction>> ATURAN = new HashMap<>();
    static {
        ATURAN.put(PUPUK_ORGANIK_PADAT, Arrays.asList(
                new BahanReduction("Lumpur",  new BigDecimal("0.5"), "Kg"),
                new BahanReduction("Kotoran", new BigDecimal("0.5"), "Kg")));
        ATURAN.put(PUPUK_ORGANIK_CAIR, Arrays.asList(
                new BahanReduction("Air Limbah Tambak", new BigDecimal("0.5"), "Liter")));
        ATURAN.put(KOMPOS, Arrays.asList(
                new BahanReduction("Lumpur", new BigDecimal("0.5"), "Kg")));
        ATURAN.put(PUPUK_NITROGEN_TINGGI_UDANG, Arrays.asList(
                new BahanReduction("Kotoran",       new BigDecimal("0.5"), "Kg"),
                new BahanReduction("Bangkai Udang", new BigDecimal("0.5"), "Kg")));
        ATURAN.put(PUPUK_KALSIUM, Arrays.asList(
                new BahanReduction("Cangkang", BigDecimal.ONE, "Kg")));
        ATURAN.put(BOOSTER, Arrays.asList(
                new BahanReduction("Cangkang", BigDecimal.ONE, "Kg")));
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private final DBConnect db = new DBConnect();
    private final List<ToggleButton> daftarKartu = new ArrayList<>();

    /**
     * Cache ID produk dari DB: key = nama produk (lowercase trimmed), value = ID_Produk.
     * Di-load sekali saat initialize, menghindari query per-klik kartu.
     */
    private final Map<String, String> cacheIDProduk = new HashMap<>();

    /**
     * Cache ID limbah/bahan dari DB: key = nama bahan (lowercase trimmed), value = ID_Limbah.
     * Dipakai untuk mengisi "ID Limbah" pada Detail Transaksi secara otomatis.
     */
    private final Map<String, String> cacheIDLimbah = new HashMap<>();

    /** Data Detail Transaksi yang otomatis terbentuk berdasarkan produk & kuantitas yang dipilih. */
    private final ObservableList<DetailTransaksi> daftarDetailTransaksi = FXCollections.observableArrayList();

    // ── Initialize ────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupComboJenisProduk();
        setupComboSatuan();
        addNumericOnly(txtKuantitas, 8);
        setupTabelDetailTransaksi();

        loadCacheIDProduk();   // ← load semua ID produk dari DB ke cache
        loadCacheIDLimbah();   // ← load semua ID limbah/bahan dari DB ke cache
        setupKartuProduk();
        loadAutoIDPengolahan();

        // Setiap kali kuantitas berubah, kuantitas limbah pada Detail Transaksi
        // ikut ter-update secara live (tanpa membentuk ulang ID).
        txtKuantitas.textProperty().addListener((obs, oldV, newV) -> updateKuantitasDetailTransaksi());
    }

    // ── Combo setup ───────────────────────────────────────────────────────────
    private void setupComboJenisProduk() {
        cmbJenisProduk.setItems(FXCollections.observableArrayList(
                PUPUK_ORGANIK_PADAT, PUPUK_ORGANIK_CAIR, KOMPOS,
                BOOSTER, PUPUK_NITROGEN_TINGGI_UDANG, PUPUK_KALSIUM));
    }

    private void setupComboSatuan() {
        cmbSatuan.setItems(FXCollections.observableArrayList("Kg", "Liter"));
    }

    private void addNumericOnly(TextField f, int maxLen) {
        f.textProperty().addListener((obs, oldV, newV) -> {
            String filtered = newV.replaceAll("[^0-9.]", "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newV)) f.setText(filtered);
        });
    }

    // ── Setup tabel Detail Transaksi ──────────────────────────────────────────
    private void setupTabelDetailTransaksi() {
        colIDDetail.setCellValueFactory(new PropertyValueFactory<>("idDetail"));
        colIDPengolahanDetail.setCellValueFactory(new PropertyValueFactory<>("idPengolahan"));
        colIDLimbah.setCellValueFactory(new PropertyValueFactory<>("idLimbah"));
        colKuantitasLimbah.setCellValueFactory(new PropertyValueFactory<>("kuantitasLimbah"));
        colSatuanLimbah.setCellValueFactory(new PropertyValueFactory<>("satuanLimbah"));

        tblDetailTransaksi.setItems(daftarDetailTransaksi);
        tblDetailTransaksi.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tblDetailTransaksi.setPlaceholder(new Label("Pilih produk untuk membentuk Detail Transaksi secara otomatis"));
    }

    // ── Cache ID produk ───────────────────────────────────────────────────────
    private void loadCacheIDProduk() {
        cacheIDProduk.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Produk}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                String namaDB = db.result.getString("Nama_Produk");
                String idDB   = db.result.getString("ID_Produk");
                if (namaDB != null && idDB != null) {
                    cacheIDProduk.put(normalisasi(namaDB), idDB);
                }
            }
            // Debug — cetak semua isi cache agar mudah verifikasi nama di DB
            System.out.println("=== Cache ID Produk (" + cacheIDProduk.size() + " item) ===");
            cacheIDProduk.forEach((k, v) -> System.out.println("  [" + v + "] = \"" + k + "\""));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Produk", e.getMessage());
        }
    }

    /**
     * Cache ID limbah/bahan dari DB.
     * NOTE: sesuaikan nama stored procedure ("sp_SelectAll_Limbah") dan nama kolom
     * ("ID_Limbah", "Nama_Limbah") dengan skema database Anda bila berbeda.
     */
    private void loadCacheIDLimbah() {
        cacheIDLimbah.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                String namaDB = db.result.getString("Nama_Limbah");
                String idDB   = db.result.getString("ID_Limbah");
                if (namaDB != null && idDB != null) {
                    cacheIDLimbah.put(normalisasi(namaDB), idDB);
                }
            }
            System.out.println("=== Cache ID Limbah (" + cacheIDLimbah.size() + " item) ===");
            cacheIDLimbah.forEach((k, v) -> System.out.println("  [" + v + "] = \"" + k + "\""));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Limbah", e.getMessage());
        }
    }

    /** Normalisasi: lowercase, trim, hapus karakter non-printable, collapse whitespace. */
    private String normalisasi(String s) {
        if (s == null) return "";
        return s.trim()
                .replaceAll("[\\x00-\\x1F\\x7F]", "")   // hapus control characters
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    /**
     * Cari ID dari sebuah cache nama→ID — 3 strategi bertingkat:
     * 1. Exact match setelah normalisasi
     * 2. Semua kata dari nama yang dicari ada di key cache (word-based)
     * 3. Mayoritas kata cocok >= 60% (partial fallback)
     */
    private String cariIDDariCache(Map<String, String> cache, String nama) {
        if (nama == null || nama.trim().isEmpty()) return null;
        String cari = normalisasi(nama);

        // 1. Exact match
        if (cache.containsKey(cari)) return cache.get(cari);

        // 2. Word-based: semua kata dari "cari" harus ada di key cache
        String[] kata = cari.split("\\s+");
        String bestID = null;
        int    bestCount = 0;

        for (Map.Entry<String, String> entry : cache.entrySet()) {
            String key = entry.getKey();
            int cocok = 0;
            for (String k : kata) if (key.contains(k)) cocok++;
            if (cocok == kata.length && cocok > bestCount) {
                bestCount = cocok;
                bestID = entry.getValue();
            }
        }
        if (bestID != null) return bestID;

        // 3. Partial fallback: >= 60% kata cocok
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            String key = entry.getKey();
            int cocok = 0;
            for (String k : kata) if (key.contains(k)) cocok++;
            if (cocok > bestCount && cocok >= Math.ceil(kata.length * 0.6)) {
                bestCount = cocok;
                bestID = entry.getValue();
            }
        }
        return bestID;
    }

    private String cariIDProduk(String namaProduk) {
        System.out.println("Mencari ID Produk untuk: \"" + normalisasi(namaProduk) + "\"");
        String id = cariIDDariCache(cacheIDProduk, namaProduk);
        System.out.println("  -> " + id);
        return id;
    }

    private String cariIDLimbah(String namaBahan) {
        System.out.println("Mencari ID Limbah untuk: \"" + normalisasi(namaBahan) + "\"");
        String id = cariIDDariCache(cacheIDLimbah, namaBahan);
        System.out.println("  -> " + id);
        return id;
    }

    // ── Setup kartu produk ────────────────────────────────────────────────────
    private void setupKartuProduk() {
        List<ToggleButton> cards = Arrays.asList(
                cardPupukOrganikPadat, cardPupukOrganikCair,
                cardKompos, cardBooster,
                cardPupukNitrogen, cardPupukKalsium);

        ProdukTemplate[] templates = {
            new ProdukTemplate(PUPUK_ORGANIK_PADAT,         "Kg",    "Lumpur, Kotoran",        "Pupuk_Organik_Padat.png"),
            new ProdukTemplate(PUPUK_ORGANIK_CAIR,          "Liter", "Air Limbah Tambak",      "Pupuk_Organik_Cair.png"),
            new ProdukTemplate(KOMPOS,                      "Kg",    "Lumpur",                 "Pupuk_Kompos.png"),
            new ProdukTemplate(BOOSTER,                     "Kg",    "Cangkang Udang",         "Pupuk_Booster.png"),
            new ProdukTemplate(PUPUK_NITROGEN_TINGGI_UDANG, "Kg",    "Kotoran, Bangkai Udang", "Pupuk_Nitrogen_Padat.png"),
            new ProdukTemplate(PUPUK_KALSIUM,               "Kg",    "Cangkang Udang",         "Pupuk_Kalsium.png")
        };

        for (int i = 0; i < cards.size(); i++) {
            ToggleButton card = cards.get(i);
            ProdukTemplate t  = templates[i];
            card.setUserData(t);
            card.setStyle(CARD_NORMAL);
            card.setPrefHeight(CARD_HEIGHT);
            card.setMinHeight(CARD_HEIGHT);
            card.setMaxHeight(CARD_HEIGHT);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setGraphic(buildKartuGraphic(t));

            card.setOnMouseEntered(e -> { if (!card.isSelected()) card.setStyle(CARD_HOVER);  });
            card.setOnMouseExited (e -> { if (!card.isSelected()) card.setStyle(CARD_NORMAL); });
        }

        daftarKartu.clear();
        daftarKartu.addAll(cards);

        tgProduk.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            for (ToggleButton c : cards) c.setStyle(CARD_NORMAL);
            if (newT == null) {
                daftarDetailTransaksi.clear();
                return;
            }

            ToggleButton sel = (ToggleButton) newT;
            sel.setStyle(CARD_SELECTED);
            ProdukTemplate t = (ProdukTemplate) sel.getUserData();

            // Gunakan cache (bukan query baru per klik)
            String id = cariIDProduk(t.namaProduk);
            txtIDProduk.setText(id != null ? id : "");
            cmbJenisProduk.setValue(t.namaProduk);
            cmbSatuan.setValue(t.satuan);

            // Bentuk Detail Transaksi secara otomatis sesuai produk yang dipilih
            rebuildDetailTransaksi();
        });
    }

    private void rebuildDetailTransaksi() {
        daftarDetailTransaksi.clear();

        String jenis = cmbJenisProduk.getValue();
        if (jenis == null) return;

        List<BahanReduction> aturan = ATURAN.get(jenis);
        if (aturan == null || aturan.isEmpty()) return;

        BigDecimal kuantitasProduk = parseKuantitas();
        String idPengolahan = txtIDPengolahan.getText();

        // Ambil satu ID Detail dasar dari SP, lalu untuk baris ke-2 dst.
        // ID di-increment secara lokal (bukan panggil SP lagi) karena SP biasanya
        // menghitung "ID berikutnya" dari data yang sudah tersimpan di DB — memanggilnya
        // berulang sebelum data ini disimpan akan menghasilkan ID yang sama berulang.
        String idDetailBase = generateIDDetailTransaksiBase(idPengolahan);

        int offset = 0;
        for (BahanReduction b : aturan) {
            String idLimbah = cariIDLimbah(b.namaBahan);
            BigDecimal kuantitasLimbah = kuantitasProduk == null
                    ? BigDecimal.ZERO
                    : b.jumlahPerUnit.multiply(kuantitasProduk).setScale(2, RoundingMode.HALF_UP);

            String idDetail = tambahOffsetID(idDetailBase, offset);
            offset++;

            daftarDetailTransaksi.add(new DetailTransaksi(
                    idDetail,
                    idPengolahan,
                    idLimbah != null ? idLimbah : "",
                    b.namaBahan,
                    kuantitasLimbah.toPlainString(),
                    b.satuanBahan));
        }
    }

    /**
     * Update kuantitas limbah pada baris Detail Transaksi yang sudah ada, tanpa
     * membentuk ulang ID — dipanggil setiap kali teks kuantitas produk berubah.
     */
    private void updateKuantitasDetailTransaksi() {
        if (daftarDetailTransaksi.isEmpty()) return;

        String jenis = cmbJenisProduk.getValue();
        List<BahanReduction> aturan = ATURAN.get(jenis);
        if (aturan == null) return;

        BigDecimal kuantitasProduk = parseKuantitas();
        for (int i = 0; i < daftarDetailTransaksi.size() && i < aturan.size(); i++) {
            BahanReduction b = aturan.get(i);
            BigDecimal kuantitasLimbah = kuantitasProduk == null
                    ? BigDecimal.ZERO
                    : b.jumlahPerUnit.multiply(kuantitasProduk).setScale(2, RoundingMode.HALF_UP);
            daftarDetailTransaksi.get(i).kuantitasLimbahProperty().set(kuantitasLimbah.toPlainString());
        }
    }

    private String generateIDDetailTransaksiBase(String ID_Pengolahan) {
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_AutoID_DetailPengolahan(?)}");
            db.cstat.setString(1, ID_Pengolahan);
            db.result = db.cstat.executeQuery();
            if (db.result.next()) {
                return db.result.getString("ID_Detail_Pengolahan");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID Detail", e.getMessage());
        }
        return "";
    }

    private String tambahOffsetID(String id, int offset) {
        if (id == null || id.isEmpty() || offset == 0) return id;
        int i = id.length();
        while (i > 0 && Character.isDigit(id.charAt(i - 1))) i--;
        String prefix = id.substring(0, i);
        String angka  = id.substring(i);
        if (angka.isEmpty()) return id;
        int panjang = angka.length();
        long nilai = Long.parseLong(angka) + offset;
        String hasil = String.valueOf(nilai);
        while (hasil.length() < panjang) hasil = "0" + hasil;
        return prefix + hasil;
    }

    // ── Bangun graphic kartu ──────────────────────────────────────────────────
    private javafx.scene.Node buildKartuGraphic(ProdukTemplate t) {
        Region aksen = new Region();
        aksen.setPrefWidth(5);
        aksen.setMinWidth(5);
        aksen.setMaxHeight(Double.MAX_VALUE);
        aksen.setStyle("-fx-background-color:#2E7D32; -fx-background-radius:10 0 0 10;");

        ImageView iv = new ImageView();
        iv.setFitWidth(IMG_SIZE);
        iv.setFitHeight(IMG_SIZE);
        iv.setPreserveRatio(true);
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/Gambar_Produk/" + t.namaFile);
            if (is != null) iv.setImage(new Image(is));
        } catch (Exception ignored) {}

        Label lblNama = new Label(t.namaProduk);
        lblNama.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1B5E20;");
        lblNama.setWrapText(true);
        lblNama.setMaxWidth(Double.MAX_VALUE);

        Label lblKomTitle = new Label("Komposisi :");
        lblKomTitle.setStyle("-fx-font-size:10px; -fx-text-fill:#9E9E9E;");

        Label lblKom = new Label("• " + t.keterangan);
        lblKom.setStyle("-fx-font-size:10px; -fx-text-fill:#757575;");
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

        VBox teks = new VBox(3, lblNama, lblKomTitle, lblKom, spacer, badgeRow);
        teks.setAlignment(Pos.TOP_LEFT);
        teks.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(teks, Priority.ALWAYS);

        HBox root = new HBox(10, aksen, iv, teks);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMaxHeight(CARD_HEIGHT);
        root.setStyle("-fx-padding:10 14 10 0;");
        HBox.setHgrow(root, Priority.ALWAYS);

        return root;
    }

    // ── Cari / filter kartu ───────────────────────────────────────────────────
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

    // ── Auto ID ───────────────────────────────────────────────────────────────
    private void loadAutoIDPengolahan() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Pengolahan}");
            if (db.result.next()) txtIDPengolahan.setText(db.result.getString("ID_Pengolahan"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    // ── Event: kuantitas di-enter ─────────────────────────────────────────────
    @FXML
    private void txtKuantitasHasil() {
        String jenis = cmbJenisProduk.getValue();
        if (jenis == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Pilih Jenis Produk terlebih dahulu.");
            return;
        }
        BigDecimal kuantitas = parseKuantitas();
        if (kuantitas == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Kuantitas harus angka lebih dari 0.");
            return;
        }
        List<BahanReduction> aturan = ATURAN.get(jenis);
        if (aturan == null || aturan.isEmpty()) return;

        StringBuilder sb = new StringBuilder("Bahan yang berkurang untuk ")
                .append(kuantitas).append(" ").append(jenis).append(":\n");
        for (BahanReduction b : aturan) {
            BigDecimal total = b.jumlahPerUnit.multiply(kuantitas).setScale(2, RoundingMode.HALF_UP);
            sb.append("  • ").append(b.namaBahan).append(" : ").append(total).append(" ").append(b.satuanBahan).append("\n");
        }
        showAlert(Alert.AlertType.INFORMATION, "Perkiraan Pengurangan Bahan", sb.toString());
    }

    @FXML private void cmbSaatuan() { /* hook untuk logika tambahan */ }

    private BigDecimal parseKuantitas() {
        try {
            String text = txtKuantitas.getText().trim();
            if (text.isEmpty()) return null;
            BigDecimal val = new BigDecimal(text);
            return val.compareTo(BigDecimal.ZERO) <= 0 ? null : val;
        } catch (NumberFormatException e) { return null; }
    }

    // ── Simpan transaksi ──────────────────────────────────────────────────────
    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;
        try {
            BigDecimal kuantitas = parseKuantitas();
            String tanggal = jpTanggal.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_PengolahanLimbah(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, txtIDPengolahan.getText());
            db.cstat.setString(2, txtIDProduk.getText());
            db.cstat.setString(3, tanggal);
            db.cstat.setString(4, cmbJenisProduk.getValue());
            db.cstat.setBigDecimal(5, kuantitas);
            db.cstat.setString(6, cmbSatuan.getValue());
            db.cstat.setString(7, txtKeterangan.getText() == null ? "" : txtKeterangan.getText().trim());
            db.cstat.executeUpdate();

            // Simpan Detail Transaksi (baris otomatis yang tampil di tabel bawah)
            // NOTE: sesuaikan nama SP "sp_Insert_DetailPengolahanLimbah" & parameternya
            // dengan skema database Anda bila berbeda.
            for (DetailTransaksi d : daftarDetailTransaksi) {
                db.cstat = db.conn.prepareCall("{CALL sp_Insert_DetailPengolahanLimbah(?,?,?,?,?)}");
                db.cstat.setString(1, d.getIdDetail());
                db.cstat.setString(2, d.getIdPengolahan());
                db.cstat.setString(3, d.getIdLimbah());
                db.cstat.setBigDecimal(4, new BigDecimal(d.getKuantitasLimbah()));
                db.cstat.setString(5, d.getSatuanLimbah());
                db.cstat.executeUpdate();
            }

            // Kurangi stok bahan limbah
            List<BahanReduction> aturan = ATURAN.get(cmbJenisProduk.getValue());
            if (aturan != null) {
                for (BahanReduction b : aturan) {
                    db.cstat = db.conn.prepareCall("{CALL sp_Kurangi_StokLimbah(?,?)}");
                    db.cstat.setString(1, b.namaBahan);
                    db.cstat.setBigDecimal(2, b.jumlahPerUnit.multiply(kuantitas));
                    db.cstat.executeUpdate();
                }
            }

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Transaksi berhasil disimpan.");
            resetForm();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    private boolean validateForm() {
        if (txtIDProduk.getText() == null || txtIDProduk.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Pilih produk dari kartu di kanan.");
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
            showAlert(Alert.AlertType.WARNING, "Validasi", "Kuantitas harus angka lebih dari 0.");
            return false;
        }
        if (cmbSatuan.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Satuan wajib dipilih.");
            return false;
        }
        if (daftarDetailTransaksi.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Detail Transaksi belum terbentuk. Pilih ulang produk.");
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
        if (tgProduk != null) tgProduk.selectToggle(null);
        for (ToggleButton c : daftarKartu) c.setStyle(CARD_NORMAL);
        daftarDetailTransaksi.clear();
        loadAutoIDPengolahan();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}