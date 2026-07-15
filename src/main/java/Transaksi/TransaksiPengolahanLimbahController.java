package Transaksi;

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

public class TransaksiPengolahanLimbahController implements Initializable {

    @FXML private TextField  txtIDPengolahan;
    @FXML private TextField  txtIDProduk;
    @FXML private DatePicker jpTanggal;
    @FXML private TextField  txtNamaProduk;
    @FXML private TextField  txtKuantitas;
    @FXML private TextField  txtSatuan;
    @FXML private TextArea   txtKeterangan;

    @FXML private VBox       vbKartuProduk;
    @FXML private TextField  txtCariProduk;

    @FXML private TableView<Map<String, String>> tblDetailTransaksi;
    @FXML private TableColumn<Map<String, String>, String> colIDDetail;
    @FXML private TableColumn<Map<String, String>, String> colIDPengolahanDetail;
    @FXML private TableColumn<Map<String, String>, String> colIDLimbah;
    @FXML private TableColumn<Map<String, String>, String> colKuantitasLimbah;
    @FXML private TableColumn<Map<String, String>, String> colSatuanLimbah;

    @FXML private TableView<Map<String, String>> tblStokLimbah;
    @FXML private TableColumn<Map<String, String>, String> colNamaLimbahStok;
    @FXML private TableColumn<Map<String, String>, String> colStokLimbah;

    private final DBConnect db = new DBConnect();

    private final ObservableList<Map<String, String>> detailTransaksiData = FXCollections.observableArrayList();

    private final ObservableList<Map<String, String>> stokLimbahData = FXCollections.observableArrayList();

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

    private static final String RUPIAH_PREFIX = "Rp ";

    private static final String STATUS_AKTIF = "Aktif";

    private final List<MasterProduk> daftarProduk     = new ArrayList<>();
    private final Map<MasterProduk, VBox> kartuPerProduk = new LinkedHashMap<>();

    private final LinkedHashMap<String, String>  namaLimbahMap  = new LinkedHashMap<>(); // ID_Limbah -> Nama_Limbah
    private final LinkedHashMap<String, String>  satuanLimbahMap = new LinkedHashMap<>(); // ID_Limbah -> Satuan
    private final LinkedHashMap<String, BigDecimal> stokLimbahMap  = new LinkedHashMap<>(); // ID_Limbah -> Stok (Jumlah)

    private VBox kartuAktif = null;
    private MasterProduk produkTerpilih = null;

    private static class KomposisiBahan {
        final String idLimbah, namaBahan, satuanBahan;
        final BigDecimal qty;
        KomposisiBahan(String idLimbah, String namaBahan, String satuanBahan, BigDecimal qty) {
            this.idLimbah = idLimbah;
            this.namaBahan = namaBahan;
            this.satuanBahan = satuanBahan;
            this.qty = qty;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        addNumericOnly(txtKuantitas, 8);

        txtKuantitas.textProperty().addListener((obs, oldVal, newVal) -> refreshDetailTransaksiTable());

        setupTabelDetailTransaksi();
        setupTabelStokLimbah();

        loadDataLimbahReferensi();
        loadDataProduk();

        loadAutoIDPengolahan();
    }

    private void setupTabelDetailTransaksi() {
        colIDDetail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault("idDetail", "")));
        colIDPengolahanDetail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault("idPengolahan", "")));
        colIDLimbah.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault("idLimbah", "")));
        colKuantitasLimbah.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault("kuantitas", "")));
        colSatuanLimbah.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault("satuan", "")));
        tblDetailTransaksi.setItems(detailTransaksiData);
    }

    private Map<String, String> buatBarisDetail(String idDetail, String idPengolahan,
                                                String idLimbah, String kuantitas, String satuan) {
        Map<String, String> baris = new LinkedHashMap<>();
        baris.put("idDetail", idDetail);
        baris.put("idPengolahan", idPengolahan);
        baris.put("idLimbah", idLimbah);
        baris.put("kuantitas", kuantitas);
        baris.put("satuan", satuan);
        return baris;
    }

    private void setupTabelStokLimbah() {
        colNamaLimbahStok.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault("namaLimbah", "")));
        colStokLimbah.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrDefault("stok", "")));
        tblStokLimbah.setItems(stokLimbahData);
    }

    private Map<String, String> buatBarisStok(String namaLimbah, String stok) {
        Map<String, String> baris = new LinkedHashMap<>();
        baris.put("namaLimbah", namaLimbah);
        baris.put("stok", stok);
        return baris;
    }

    private void refreshStokLimbahTable() {
        stokLimbahData.clear();
        if (produkTerpilih == null) return;

        List<KomposisiBahan> komposisi = loadKomposisiProduk(produkTerpilih.getIdProduk());
        for (KomposisiBahan b : komposisi) {
            BigDecimal stok = stokLimbahMap.getOrDefault(b.idLimbah, BigDecimal.ZERO);
            stokLimbahData.add(buatBarisStok(b.namaBahan, formatAngka(stok) + " " + b.satuanBahan));
        }
    }

    private void addNumericOnly(TextField field, int maxLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9.]", "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    private String formatAngka(BigDecimal nilai) {
        BigDecimal bd = nilai.stripTrailingZeros();
        if (bd.scale() < 0) bd = bd.setScale(0, RoundingMode.HALF_UP);
        return bd.toPlainString();
    }

    private void loadDataLimbahReferensi() {
        namaLimbahMap.clear();
        satuanLimbahMap.clear();
        stokLimbahMap.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                String idLimbah = db.result.getString("ID_Limbah");
                namaLimbahMap.put(idLimbah, db.result.getString("Nama_Limbah"));
                satuanLimbahMap.put(idLimbah, db.result.getString("Satuan"));
                stokLimbahMap.put(idLimbah, db.result.getBigDecimal("Jumlah"));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data Limbah", e.getMessage());
        }
    }

    private void loadDataProduk() {
        daftarProduk.clear();
        kartuPerProduk.clear();
        vbKartuProduk.getChildren().clear();

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
                VBox kartu = buildKartu(p);
                kartuPerProduk.put(p, kartu);
                vbKartuProduk.getChildren().add(kartu);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Produk", e.getMessage());
        }
    }

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

        Label lblSatuanBadge = new Label(p.getSatuan());
        lblSatuanBadge.setStyle(
                "-fx-background-color:#E8F5E9; -fx-text-fill:#2E7D32;" +
                        "-fx-font-size:10px; -fx-font-weight:bold;" +
                        "-fx-padding:2 10 2 10; -fx-background-radius:10;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox badgeRow = new HBox(lblSatuanBadge);
        badgeRow.setAlignment(Pos.BOTTOM_RIGHT);

        VBox info = new VBox(4, lblNama, komposisiBox, spacer, badgeRow);
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
        kartu.setOnMouseClicked(e -> pilihProdukUntukTransaksi(p, kartu));

        return kartu;
    }

    private void pilihProdukUntukTransaksi(MasterProduk p, VBox kartu) {
        if (kartuAktif != null) kartuAktif.setStyle(STYLE_KARTU_NORMAL);

        kartuAktif      = kartu;
        produkTerpilih  = p;
        kartu.setStyle(STYLE_KARTU_AKTIF);

        txtIDProduk.setText(p.getIdProduk());
        txtNamaProduk.setText(p.getNamaProduk());
        txtSatuan.setText(p.getSatuan());

        refreshDetailTransaksiTable();
    }

    @FXML
    private void handleCariProduk() {
        String keyword = txtCariProduk.getText().trim().toLowerCase(Locale.ROOT);

        vbKartuProduk.getChildren().clear();
        for (MasterProduk p : daftarProduk) {
            boolean cocok = keyword.isEmpty()
                    || p.getNamaProduk().toLowerCase(Locale.ROOT).contains(keyword);
            if (cocok) {
                vbKartuProduk.getChildren().add(kartuPerProduk.get(p));
            }
        }
    }

    private void loadAutoIDPengolahan() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Pengolahan}");
            if (db.result.next()) {
                txtIDPengolahan.setText(db.result.getString("ID_Pengolahan"));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }

        refreshDetailTransaksiTable();
    }

    private int ambilUrutAwalDetailPengolahan(String idPengolahan) {
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_AutoID_DetailPengolahan(?)}");
            db.cstat.setString(1, idPengolahan);
            db.result = db.cstat.executeQuery();
            if (db.result.next()) {
                String idBaru = db.result.getString("ID_Detail_Pengolahan"); // contoh: DT TRP08072026001 01
                String urutStr = idBaru.substring(idBaru.length() - 2);
                return Integer.parseInt(urutStr);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID Detail Pengolahan", e.getMessage());
        } catch (Exception ignored) {

        }
        return 1;
    }

    private String formatIDDetailPengolahan(String idPengolahan, int urut) {
        return "DT" + idPengolahan + String.format("%02d", urut);
    }

    private List<KomposisiBahan> loadKomposisiProduk(String idProduk) {
        List<KomposisiBahan> hasil = new ArrayList<>();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectByID_DetailProduk(?)}");
            db.cstat.setString(1, idProduk);
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                String idLimbah = db.result.getString("ID_Limbah");
                BigDecimal qty = db.result.getBigDecimal("Qty"); // persentase komposisi, kini mendukung desimal
                String namaBahan  = namaLimbahMap.getOrDefault(idLimbah, idLimbah);
                String satuanBahan = satuanLimbahMap.getOrDefault(idLimbah, "");
                hasil.add(new KomposisiBahan(idLimbah, namaBahan, satuanBahan, qty));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Komposisi Produk", e.getMessage());
        }
        return hasil;
    }

    private void refreshDetailTransaksiTable() {
        detailTransaksiData.clear();
        refreshStokLimbahTable();

        if (produkTerpilih == null) return;

        List<KomposisiBahan> komposisi = loadKomposisiProduk(produkTerpilih.getIdProduk());
        if (komposisi.isEmpty()) return;

        String idPengolahan = txtIDPengolahan.getText();
        BigDecimal kuantitas = parseKuantitas();

        int urut = ambilUrutAwalDetailPengolahan(idPengolahan);
        for (KomposisiBahan b : komposisi) {
            String idDetail = formatIDDetailPengolahan(idPengolahan, urut);

            String kuantitasLimbahStr;
            if (kuantitas == null) {
                kuantitasLimbahStr = "-";
            } else {
                BigDecimal total = kuantitas.multiply(b.qty).setScale(2, RoundingMode.HALF_UP);
                kuantitasLimbahStr = total.toPlainString();
            }

            detailTransaksiData.add(buatBarisDetail(
                    idDetail, idPengolahan, b.idLimbah, kuantitasLimbahStr, b.satuanBahan));
            urut++;
        }
    }

    @FXML
    private void txtKuantitasHasil() {
        if (produkTerpilih == null || txtIDProduk.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Pilih produk dari daftar kartu terlebih dahulu.");
            return;
        }
        BigDecimal kuantitas = parseKuantitas();
        if (kuantitas == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Kuantitas Hasil harus berupa angka lebih dari 0.");
            return;
        }

        List<KomposisiBahan> komposisi = loadKomposisiProduk(txtIDProduk.getText());
        if (komposisi.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Info",
                    "Produk ini belum memiliki data komposisi bahan limbah di Master Produk.");
            return;
        }

        if (!validasiStok(kuantitas)) return;

        StringBuilder sb = new StringBuilder();
        sb.append("Bahan limbah yang akan berkurang untuk ")
                .append(kuantitas).append(" ").append(txtNamaProduk.getText()).append(":\n");
        for (KomposisiBahan b : komposisi) {
            BigDecimal total = kuantitas.multiply(b.qty).setScale(2, RoundingMode.HALF_UP);
            sb.append("- ").append(b.namaBahan).append(" (").append(b.qty.stripTrailingZeros().toPlainString())
                    .append(" ").append(b.satuanBahan).append("/unit) : ")
                    .append(total).append(" ").append(b.satuanBahan).append("\n");
        }
        showAlert(Alert.AlertType.INFORMATION, "Perkiraan Pengurangan Bahan Limbah", sb.toString());
    }

    private boolean validasiStok(BigDecimal kuantitas) {
        List<KomposisiBahan> komposisi = loadKomposisiProduk(txtIDProduk.getText());
        StringBuilder kurang = new StringBuilder();
        boolean cukup = true;

        for (KomposisiBahan b : komposisi) {
            BigDecimal totalDibutuhkan = kuantitas.multiply(b.qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal stokTersedia = stokLimbahMap.getOrDefault(b.idLimbah, BigDecimal.ZERO);

            if (totalDibutuhkan.compareTo(stokTersedia) > 0) {
                cukup = false;
                kurang.append("- ").append(b.namaBahan)
                        .append(" : butuh ").append(totalDibutuhkan.toPlainString()).append(" ").append(b.satuanBahan)
                        .append(", stok tersedia ").append(formatAngka(stokTersedia)).append(" ").append(b.satuanBahan)
                        .append("\n");
            }
        }

        if (!cukup) {
            showAlert(Alert.AlertType.ERROR, "Stok Tidak Mencukupi",
                    "Transaksi tidak dapat disimpan karena stok bahan limbah berikut tidak mencukupi:\n\n"
                            + kurang);
        }
        return cukup;
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

    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;

        String idPengolahan = txtIDPengolahan.getText();
        String idProduk     = txtIDProduk.getText();
        String tanggal      = jpTanggal.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String namaProduk   = txtNamaProduk.getText();
        String kuantitasStr = txtKuantitas.getText().trim();
        String satuan       = txtSatuan.getText();
        String keterangan   = txtKeterangan.getText() == null ? "" : txtKeterangan.getText().trim();
        BigDecimal kuantitas = new BigDecimal(kuantitasStr);

        loadDataLimbahReferensi();
        refreshStokLimbahTable();

        if (!validasiStok(kuantitas)) return;

        try {

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_PengolahanLimbah(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, idPengolahan);
            db.cstat.setString(2, idProduk);
            db.cstat.setString(3, tanggal);
            db.cstat.setString(4, namaProduk);
            db.cstat.setBigDecimal(5, kuantitas);
            db.cstat.setString(6, satuan);
            db.cstat.setString(7, keterangan);
            db.cstat.executeUpdate();

            List<KomposisiBahan> komposisi = loadKomposisiProduk(idProduk);
            for (KomposisiBahan b : komposisi) {
                BigDecimal totalKurang = kuantitas.multiply(b.qty).setScale(2, RoundingMode.HALF_UP);
                db.cstat = db.conn.prepareCall("{CALL sp_Kurangi_StokLimbah(?,?)}");
                db.cstat.setString(1, b.namaBahan);
                db.cstat.setBigDecimal(2, totalKurang);
                db.cstat.executeUpdate();
            }

            loadDataLimbahReferensi();
            refreshStokLimbahTable();

            int tambahStok = kuantitas.setScale(0, RoundingMode.HALF_UP).intValue();
            db.cstat = db.conn.prepareCall("{CALL sp_Tambah_StokProduk(?,?)}");
            db.cstat.setString(1, idProduk);
            db.cstat.setInt(2, tambahStok);
            db.cstat.executeUpdate();

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
        if (txtNamaProduk.getText() == null || txtNamaProduk.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Nama Produk wajib dipilih dari kartu produk.");
            return false;
        }
        if (parseKuantitas() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Kuantitas Hasil harus berupa angka lebih dari 0.");
            return false;
        }
        if (txtSatuan.getText() == null || txtSatuan.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Satuan belum terisi otomatis, pilih ulang produknya.");
            return false;
        }
        return true;
    }

    private void resetForm() {
        txtIDProduk.clear();
        jpTanggal.setValue(null);
        txtNamaProduk.clear();
        txtKuantitas.clear();
        txtSatuan.clear();
        txtKeterangan.clear();

        if (kartuAktif != null) kartuAktif.setStyle(STYLE_KARTU_NORMAL);
        kartuAktif = null;
        produkTerpilih = null;

        detailTransaksiData.clear();
        stokLimbahData.clear();

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