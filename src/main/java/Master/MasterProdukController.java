package Master;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class MasterProdukController implements Initializable {

    @FXML private FlowPane fpKartuProduk;

    @FXML private TextField txtID, txtNama, txtStock, txtHrgJual;
    @FXML private TextField txtCari;
    @FXML private ComboBox<String> cmbSatuan;
    @FXML private TextArea txtKeterangan;

    // ===================== PANEL KARTU GAMBAR PRODUK =====================
    @FXML private ToggleGroup tgProdukTemplate;
    @FXML private ToggleButton cardPupukOrganikPadat;
    @FXML private ToggleButton cardPupukOrganikCair;
    @FXML private ToggleButton cardKompos;
    @FXML private ToggleButton cardBooster;
    @FXML private ToggleButton cardPupukNitrogen;
    @FXML private ToggleButton cardPupukKalsium;

    private static final String STYLE_CARD_NORMAL   = "-fx-background-color: white; -fx-border-color:#DDDDDD; -fx-border-radius:8; -fx-background-radius:8; -fx-padding:0;";
    private static final String STYLE_CARD_SELECTED = "-fx-background-color:#C7C7C7; -fx-border-color:#9A9A9A; -fx-border-radius:8; -fx-background-radius:8; -fx-padding:0;";
    private static final String RUPIAH_PREFIX = "Rp ";
    private static final String DEFAULT_SALDO = "0";

    /** Ukuran kartu & gambar DIBUAT TETAP (fixed) supaya semua kartu sama besar,
     *  tidak mengikuti panjang teks nama/komposisi masing-masing produk. */
    private static final double CARD_WIDTH = 260.0;
    private static final double CARD_HEIGHT = 148.0;
    private static final double IMG_SIZE = 78.0;

    /** Menyimpan referensi semua kartu produk, dipakai juga untuk fitur filter pencarian. */
    private final List<ToggleButton> daftarKartuProduk = new ArrayList<>();

    /** Template data produk untuk mengisi form otomatis saat kartu gambar diklik. */
    private static class ProdukTemplate {
        final String namaProduk;
        final String satuan;
        final String keterangan; // komposisi bahan
        final String namaFileGambar;

        ProdukTemplate(String namaProduk, String satuan, String keterangan, String namaFileGambar) {
            this.namaProduk = namaProduk;
            this.satuan = satuan;
            this.keterangan = keterangan;
            this.namaFileGambar = namaFileGambar;
        }
    }

    private ObservableList<MasterProduk> dataList = FXCollections.observableArrayList();
    private DBConnect db = new DBConnect();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbSatuan.setItems(FXCollections.observableArrayList("Kg", "Liter"));
        cmbSatuan.getSelectionModel().selectFirst();

        addNumericOnly(txtStock, 10);
        setupHargaJualRupiah();
        addMaxLength(txtNama, 30);

        setupKartuProduk();

        loadAutoID();
        loadData();
    }

    // ===================== SETUP KARTU GAMBAR PRODUK =====================

    private void setupKartuProduk() {
        List<ToggleButton> cards = Arrays.asList(
                cardPupukOrganikPadat, cardPupukOrganikCair, cardKompos,
                cardBooster, cardPupukNitrogen, cardPupukKalsium
        );

        ProdukTemplate[] templates = {
                new ProdukTemplate("Pupuk Organik Padat", "Kg", "Lumpur, Kotoran", "Pupuk_Organik_Padat.png"),
                new ProdukTemplate("Pupuk Organik Cair", "Liter", "Air Limbah Tambak", "Pupuk_Organik_Cair.png"),
                new ProdukTemplate("Kompos", "Kg", "Lumpur", "Pupuk_Kompos.png"),
                new ProdukTemplate("Booster", "Kg", "Cangkang Udang", "Pupuk_Booster.png"),
                new ProdukTemplate("Pupuk Nitrogen Tinggi Udang", "Kg", "Kotoran, Bangkai Udang", "Pupuk_Nitrogen_Padat.png"),
                new ProdukTemplate("Pupuk Kalsium", "Kg", "Cangkang Udang", "Pupuk_Kalsium.png")
        };

        for (int i = 0; i < cards.size(); i++) {
            ToggleButton card = cards.get(i);
            ProdukTemplate t = templates[i];
            card.setUserData(t);
            card.setStyle(STYLE_CARD_NORMAL);
            // Lebar & tinggi kartu dibuat tetap (fixed) supaya semua kartu berukuran sama persis
            card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
            card.setMinSize(CARD_WIDTH, CARD_HEIGHT);
            card.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
            card.setGraphic(buildKartuGraphic(t));
        }

        daftarKartuProduk.clear();
        daftarKartuProduk.addAll(cards);

        tgProdukTemplate.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            for (ToggleButton card : cards) {
                card.setStyle(STYLE_CARD_NORMAL);
            }
            if (newToggle != null) {
                ToggleButton selected = (ToggleButton) newToggle;
                selected.setStyle(STYLE_CARD_SELECTED);

                ProdukTemplate t = (ProdukTemplate) selected.getUserData();
                txtNama.setText(t.namaProduk);
                cmbSatuan.setValue(t.satuan);
                txtKeterangan.setText(t.keterangan);
            }
        });
    }

    /** Membuat tampilan satu kartu: strip aksen hijau + gambar + nama produk (bold) + baris komposisi. */
    private javafx.scene.Node buildKartuGraphic(ProdukTemplate t) {
        // Strip aksen hijau di sisi kiri kartu, sesuai desain
        Region aksen = new Region();
        aksen.setPrefWidth(4);
        aksen.setMinWidth(4);
        aksen.setStyle("-fx-background-color:#2E7D32; -fx-background-radius:4 0 0 4;");
        aksen.setMaxHeight(Double.MAX_VALUE);

        ImageView iv = new ImageView();
        iv.setFitWidth(IMG_SIZE);
        iv.setFitHeight(IMG_SIZE);
        iv.setPreserveRatio(true);
        String resourcePath = "/Gambar_Produk/" + t.namaFileGambar;
        try {
            java.io.InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                System.out.println("Gambar tidak ditemukan di classpath: " + resourcePath
                        + " -- pastikan folder Gambar_Produk ada di src/main/resources, bukan src/main/java.");
            } else {
                iv.setImage(new Image(is));
            }
        } catch (Exception e) {
            System.out.println("Gagal memuat gambar " + resourcePath + ": " + e.getMessage());
        }

        Label lblNama = new Label(t.namaProduk);
        lblNama.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#2E7D32;");
        lblNama.setWrapText(true);

        Label lblKomposisiTitle = new Label("Komposisi :");
        lblKomposisiTitle.setStyle("-fx-text-fill:#888888; -fx-font-size:11px;");

        Label lblKomposisiIsi = new Label("\u2022 " + t.keterangan);
        lblKomposisiIsi.setStyle("-fx-text-fill:#888888; -fx-font-size:11px;");
        lblKomposisiIsi.setWrapText(true);

        double lebarTeks = CARD_WIDTH - IMG_SIZE - 34;
        lblNama.setMaxWidth(lebarTeks);
        lblKomposisiIsi.setMaxWidth(lebarTeks);

        VBox teks = new VBox(2, lblNama, lblKomposisiTitle, lblKomposisiIsi);
        teks.setMaxWidth(lebarTeks);
        teks.setPrefWidth(lebarTeks);
        teks.setMaxHeight(CARD_HEIGHT - 16);

        HBox kartu = new HBox(10, aksen, iv, teks);
        kartu.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        kartu.setStyle("-fx-padding:10 12 10 0;");
        kartu.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        kartu.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        return kartu;
    }

    // ===================== VALIDASI INPUT =====================

    private void addNumericOnly(TextField field, int maxLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9]", "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    private void addLetterOnly(TextField field, int maxLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^a-zA-Z\\s.\\-]", "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    private void addMaxLength(TextField field, int maxLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > maxLen) {
                field.setText(newVal.substring(0, maxLen));
            }
        });
    }

    private void setupHargaJualRupiah() {
        txtHrgJual.setText(RUPIAH_PREFIX);

        txtHrgJual.textProperty().addListener((obs, oldVal, newVal) -> {

            String angka = newVal.replaceAll("[^0-9]", "");

            if (angka.length() > 18)
                angka = angka.substring(0, 18);

            String hasil = RUPIAH_PREFIX + angka;

            if (!hasil.equals(newVal)) {
                txtHrgJual.setText(hasil);
                txtHrgJual.positionCaret(hasil.length());
            }
        });

        txtHrgJual.caretPositionProperty().addListener((obs, oldPos, newPos) -> {

            if (newPos.intValue() < RUPIAH_PREFIX.length()) {
                txtHrgJual.positionCaret(RUPIAH_PREFIX.length());
            }

        });

    }

    private String getHargaRawValue() {

        String raw = txtHrgJual.getText()
                .replace(RUPIAH_PREFIX, "")
                .trim();

        return raw.isEmpty() ? DEFAULT_SALDO : raw;

    }

    // ===================== AUTO ID =====================

    private void loadAutoID() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Produk}");
            if (db.result.next()) txtID.setText(db.result.getString("ID_Produk"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    // ===================== LOAD / CARI DATA =====================

    private void loadData() {
        dataList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Produk}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                dataList.add(new MasterProduk(
                    db.result.getString("ID_Produk"),
                    db.result.getString("Nama_Produk"),
                    db.result.getString("Stok"),
                    RUPIAH_PREFIX + db.result.getBigDecimal("Harga_Jual")
                    .stripTrailingZeros()
                    .toPlainString(),
                    db.result.getString("Satuan"),
                    db.result.getString("Keterangan")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data", e.getMessage());
        }
    }

    /** Tidak ada lagi tabel untuk menampilkan hasil pencarian, jadi "Cari" difungsikan
     *  untuk menyaring kartu produk yang tampil berdasarkan nama produknya. */
    @FXML
    private void handleCari() {
        String keyword = txtCari.getText().trim().toLowerCase(Locale.ROOT);
        for (ToggleButton card : daftarKartuProduk) {
            Object userData = card.getUserData();
            boolean cocok = true;
            if (!keyword.isEmpty() && userData instanceof ProdukTemplate) {
                ProdukTemplate t = (ProdukTemplate) userData;
                cocok = t.namaProduk.toLowerCase(Locale.ROOT).contains(keyword);
            }
            card.setVisible(cocok);
            card.setManaged(cocok);
        }
    }

    // ===================== CRUD =====================

    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Produk(?,?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText());
            db.cstat.setInt(3, Integer.parseInt(txtStock.getText()));
            db.cstat.setBigDecimal(4, new BigDecimal(getHargaRawValue()));
            db.cstat.setString(5, cmbSatuan.getValue());
            db.cstat.setString(6, txtKeterangan.getText());
            db.cstat.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data produk berhasil disimpan.");
            clearForm();
            loadData();
            loadAutoID();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    @FXML
    private void handleUbah() {
        if (txtID.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin diubah terlebih dahulu.");
            return;
        }
        if (!validateForm()) return;
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Update_Produk(?,?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText());
            db.cstat.setInt(3, Integer.parseInt(txtStock.getText()));
            db.cstat.setBigDecimal(4, new BigDecimal(getHargaRawValue()));
            db.cstat.setString(5, cmbSatuan.getValue());
            db.cstat.setString(6, txtKeterangan.getText());
            db.cstat.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data produk berhasil diubah.");
            clearForm();
            loadData();
            loadAutoID();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Ubah", e.getMessage());
        }
    }

    @FXML
    private void handleHapus() {
        if (txtID.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin dihapus terlebih dahulu.");
            return;
        }
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin menghapus produk ID: " + txtID.getText() + "?",
                ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    db.cstat = db.conn.prepareCall("{CALL sp_Delete_Produk(?)}");
                    db.cstat.setString(1, txtID.getText());
                    db.cstat.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data produk berhasil dihapus.");
                    clearForm();
                    loadData();
                    loadAutoID();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Error Hapus", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBatal() {
        boolean adaIsi = !txtNama.getText().trim().isEmpty()      ||
                          !txtStock.getText().trim().isEmpty()    ||
                          !txtHrgJual.getText().trim().isEmpty()  ||
                          !txtKeterangan.getText().trim().isEmpty();

        if (!adaIsi) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Tidak ada data yang perlu dibatalkan.");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin membatalkan dan mengosongkan semua input?",
                ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Batal");
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                clearForm();
                loadAutoID();
            }
        });
    }

    private boolean validateForm() {
        if (txtNama.getText().trim().isEmpty()    ||
            txtStock.getText().trim().isEmpty()   ||
            txtHrgJual.getText().trim().isEmpty() ||
            cmbSatuan.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Semua data wajib harus diisi!");
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtNama.clear();
        txtStock.clear();
        txtHrgJual.setText(RUPIAH_PREFIX + DEFAULT_SALDO);
        txtKeterangan.clear();
        cmbSatuan.getSelectionModel().selectFirst();
        if (tgProdukTemplate != null) {
            tgProdukTemplate.selectToggle(null);
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