package Master;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class MasterProdukController implements Initializable {

    // ── FXML GridPane menggantikan FlowPane ──────────────────────────────────
    @FXML private GridPane gpKartuProduk;

    @FXML private TextField txtID, txtNama, txtStock, txtHrgJual, txtCari;
    @FXML private ComboBox<String> cmbSatuan;
    @FXML private TextArea txtKeterangan;

    @FXML private ToggleGroup tgProdukTemplate;
    @FXML private ToggleButton cardPupukOrganikPadat;
    @FXML private ToggleButton cardPupukOrganikCair;
    @FXML private ToggleButton cardKompos;
    @FXML private ToggleButton cardBooster;
    @FXML private ToggleButton cardPupukNitrogen;
    @FXML private ToggleButton cardPupukKalsium;

    // ── Style kartu ──────────────────────────────────────────────────────────
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

    // ── Dimensi kartu (lebar mengikuti GridPane 50%, tinggi fixed) ───────────
    private static final double CARD_HEIGHT = 120.0;
    private static final double IMG_SIZE    =  80.0;

    private static final String RUPIAH_PREFIX = "Rp ";
    private static final String DEFAULT_SALDO = "0";

    private final List<ToggleButton> daftarKartuProduk = new ArrayList<>();
    private ObservableList<MasterProduk> dataList = FXCollections.observableArrayList();
    private DBConnect db = new DBConnect();

    // ── Inner class template ─────────────────────────────────────────────────
    private static class ProdukTemplate {
        final String namaProduk, satuan, keterangan, namaFileGambar;
        ProdukTemplate(String n, String s, String k, String f) {
            namaProduk = n; satuan = s; keterangan = k; namaFileGambar = f;
        }
    }

    // ── initialize ───────────────────────────────────────────────────────────
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

    // ── Setup kartu ──────────────────────────────────────────────────────────
    private void setupKartuProduk() {
        List<ToggleButton> cards = Arrays.asList(
                cardPupukOrganikPadat, cardPupukOrganikCair, cardKompos,
                cardBooster, cardPupukNitrogen, cardPupukKalsium
        );

        ProdukTemplate[] templates = {
                new ProdukTemplate("Pupuk Organik Padat",       "Kg",    "Lumpur, Kotoran",         "Pupuk_Organik_Padat.png"),
                new ProdukTemplate("Pupuk Organik Cair",        "Liter", "Air Limbah Tambak",       "Pupuk_Organik_Cair.png"),
                new ProdukTemplate("Kompos",                    "Kg",    "Lumpur",                  "Pupuk_Kompos.png"),
                new ProdukTemplate("Booster",                   "Kg",    "Cangkang Udang",          "Pupuk_Booster.png"),
                new ProdukTemplate("Pupuk Nitrogen Tinggi Udang","Kg",   "Kotoran, Bangkai Udang",  "Pupuk_Nitrogen_Padat.png"),
                new ProdukTemplate("Pupuk Kalsium",             "Kg",    "Cangkang Udang",          "Pupuk_Kalsium.png")
        };

        for (int i = 0; i < cards.size(); i++) {
            ToggleButton card = cards.get(i);
            ProdukTemplate t  = templates[i];
            card.setUserData(t);
            card.setStyle(CARD_NORMAL);

            // Tinggi fixed, lebar mengikuti kolom GridPane (maxWidth=Infinity sudah di FXML)
            card.setPrefHeight(CARD_HEIGHT);
            card.setMinHeight(CARD_HEIGHT);
            card.setMaxHeight(CARD_HEIGHT);
            card.setMaxWidth(Double.MAX_VALUE);   // stretch penuh kolom

            card.setGraphic(buildKartuGraphic(t));

            // Hover effect
            card.setOnMouseEntered(e -> {
                if (!card.isSelected()) card.setStyle(CARD_HOVER);
            });
            card.setOnMouseExited(e -> {
                if (!card.isSelected()) card.setStyle(CARD_NORMAL);
            });
        }

        daftarKartuProduk.clear();
        daftarKartuProduk.addAll(cards);

        tgProdukTemplate.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            for (ToggleButton c : cards) c.setStyle(CARD_NORMAL);
            if (newT != null) {
                ToggleButton sel = (ToggleButton) newT;
                sel.setStyle(CARD_SELECTED);
                ProdukTemplate t = (ProdukTemplate) sel.getUserData();
                txtNama.setText(t.namaProduk);
                cmbSatuan.setValue(t.satuan);
                txtKeterangan.setText(t.keterangan);
            }
        });
    }

    /** Bangun graphic satu kartu: aksen hijau | gambar | teks (nama + komposisi) | badge satuan */
    private javafx.scene.Node buildKartuGraphic(ProdukTemplate t) {
        // Aksen hijau kiri
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
            java.io.InputStream is = getClass().getResourceAsStream("/Gambar_Produk/" + t.namaFileGambar);
            if (is != null) iv.setImage(new Image(is));
        } catch (Exception ignored) {}

        // Nama produk
        Label lblNama = new Label(t.namaProduk);
        lblNama.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1B5E20;");
        lblNama.setWrapText(true);
        lblNama.setMaxWidth(Double.MAX_VALUE);

        // Label "Komposisi :"
        Label lblKomTitle = new Label("Komposisi :");
        lblKomTitle.setStyle("-fx-font-size:11px; -fx-text-fill:#9E9E9E;");

        // Isi komposisi
        Label lblKom = new Label("• " + t.keterangan);
        lblKom.setStyle("-fx-font-size:11px; -fx-text-fill:#757575;");
        lblKom.setWrapText(true);
        lblKom.setMaxWidth(Double.MAX_VALUE);

        // Badge satuan (pojok kanan bawah)
        Label lblBadge = new Label(t.satuan);
        lblBadge.setStyle(
                "-fx-background-color:#E8F5E9;" +
                        "-fx-text-fill:#2E7D32;" +
                        "-fx-font-size:10px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-padding:2 8 2 8;" +
                        "-fx-background-radius:8;");

        // Spacer untuk mendorong badge ke bawah
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

    // ── Cari / filter ────────────────────────────────────────────────────────
    @FXML
    private void handleCari() {
        String keyword = txtCari.getText().trim().toLowerCase(Locale.ROOT);

        int col = 0, row = 0;
        gpKartuProduk.getChildren().clear();

        for (ToggleButton card : daftarKartuProduk) {
            Object ud = card.getUserData();
            boolean cocok = true;
            if (!keyword.isEmpty() && ud instanceof ProdukTemplate) {
                cocok = ((ProdukTemplate) ud).namaProduk
                        .toLowerCase(Locale.ROOT).contains(keyword);
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

    // ── Validasi input ───────────────────────────────────────────────────────
    private void addNumericOnly(TextField f, int max) {
        f.textProperty().addListener((o, oldV, newV) -> {
            String filtered = newV.replaceAll("[^0-9]", "");
            if (filtered.length() > max) filtered = filtered.substring(0, max);
            if (!filtered.equals(newV)) f.setText(filtered);
        });
    }

    private void addMaxLength(TextField f, int max) {
        f.textProperty().addListener((o, oldV, newV) -> {
            if (newV.length() > max) f.setText(newV.substring(0, max));
        });
    }

    private void setupHargaJualRupiah() {
        txtHrgJual.setText(RUPIAH_PREFIX);
        txtHrgJual.textProperty().addListener((o, oldV, newV) -> {
            String angka = newV.replaceAll("[^0-9]", "");
            if (angka.length() > 18) angka = angka.substring(0, 18);
            String hasil = RUPIAH_PREFIX + angka;
            if (!hasil.equals(newV)) {
                txtHrgJual.setText(hasil);
                txtHrgJual.positionCaret(hasil.length());
            }
        });
        txtHrgJual.caretPositionProperty().addListener((o, oldP, newP) -> {
            if (newP.intValue() < RUPIAH_PREFIX.length())
                txtHrgJual.positionCaret(RUPIAH_PREFIX.length());
        });
    }

    private String getHargaRawValue() {
        String raw = txtHrgJual.getText().replace(RUPIAH_PREFIX, "").trim();
        return raw.isEmpty() ? DEFAULT_SALDO : raw;
    }

    // ── Auto ID ──────────────────────────────────────────────────────────────
    private void loadAutoID() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Produk}");
            if (db.result.next()) txtID.setText(db.result.getString("ID_Produk"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    // ── Load data ────────────────────────────────────────────────────────────
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
                                .stripTrailingZeros().toPlainString(),
                        db.result.getString("Satuan"),
                        db.result.getString("Keterangan")
                ));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data", e.getMessage());
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────
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
            clearForm(); loadData(); loadAutoID();
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
            clearForm(); loadData(); loadAutoID();
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
        Alert k = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin menghapus produk ID: " + txtID.getText() + "?",
                ButtonType.YES, ButtonType.NO);
        k.setTitle("Konfirmasi Hapus");
        k.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    db.cstat = db.conn.prepareCall("{CALL sp_Delete_Produk(?)}");
                    db.cstat.setString(1, txtID.getText());
                    db.cstat.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data produk berhasil dihapus.");
                    clearForm(); loadData(); loadAutoID();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Error Hapus", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBatal() {
        boolean adaIsi = !txtNama.getText().trim().isEmpty() ||
                !txtStock.getText().trim().isEmpty() ||
                !txtKeterangan.getText().trim().isEmpty();
        if (!adaIsi) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Tidak ada data yang perlu dibatalkan.");
            return;
        }
        Alert k = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin membatalkan dan mengosongkan semua input?",
                ButtonType.YES, ButtonType.NO);
        k.setTitle("Konfirmasi Batal");
        k.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) { clearForm(); loadAutoID(); }
        });
    }

    private boolean validateForm() {
        if (txtNama.getText().trim().isEmpty()  ||
                txtStock.getText().trim().isEmpty() ||
                cmbSatuan.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Semua data wajib harus diisi!");
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtNama.clear(); txtStock.clear();
        txtHrgJual.setText(RUPIAH_PREFIX + DEFAULT_SALDO);
        txtKeterangan.clear();
        cmbSatuan.getSelectionModel().selectFirst();
        if (tgProdukTemplate != null) tgProdukTemplate.selectToggle(null);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}