package Dashboard;

import Connection.DBConnect;
import Master.MasterKaryawan;
import Master.MasterLimbah;
import Master.MasterNasabah;
import Master.MasterProduk;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.MouseEvent;

import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.swing.JRViewer;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller untuk Dashboard Manager.
 * Menampilkan statistik: Jumlah Karyawan Aktif, Produk, Limbah, dan Nasabah.
 */
public class DashboardManagerController implements Initializable {

    @FXML private Label lblJumlahKaryawan;
    @FXML private Label lblJumlahProduk;
    @FXML private Label lblJumlahLimbah;
    @FXML private Label lblJumlahNasabah;

    @FXML private javafx.scene.layout.VBox cardKaryawan;
    @FXML private javafx.scene.layout.VBox cardProduk;
    @FXML private javafx.scene.layout.VBox cardLimbah;
    @FXML private javafx.scene.layout.VBox cardNasabah;

    @FXML private Button btnLogout;

    // ── Panel Laporan Jasper (muncul full-screen menutupi seluruh area putih) ──
    @FXML private BorderPane rootPane;
    @FXML private Button btnLihatLaporan;
    @FXML private Button btnLihatPenarikan;
    @FXML private Button btnTutupLaporan;
    @FXML private VBox panelLaporan;
    @FXML private Label lblJudulLaporan;
    @FXML private SwingNode swingNodeLaporan;
    @FXML private ProgressIndicator progressLaporan;

    // ── Layer center (dashboard normal vs panel laporan full-screen) ───────────
    @FXML private StackPane stackCenterContent;
    @FXML private ScrollPane scrollDashboard;

    @FXML private HBox rowStatCards;
    @FXML private HBox rowRingkasanLabel;
    @FXML private HBox rowRingkasanCharts;

    /** Path relatif ke file .jasper dari root project (dibaca langsung dari disk, bukan dari classpath,
     *  supaya tidak perlu dipindah ke resources atau rebuild project). Hanya jalan kalau working directory
     *  saat run adalah root project (default IntelliJ run config). */
    private static final String JASPER_REPORT_PATH_SETOR = "src/main/java/Laporan/TransaksiSetorLimbah.jasper";
    private static final String JASPER_REPORT_PATH_PENARIKAN = "src/main/java/Laporan/PenarikanSaldo.jasper";

    // Cache per laporan (key = path .jasper) supaya klik ulang tombol tidak perlu query database lagi
    private final Map<String, JasperPrint> jasperPrintCache = new HashMap<>();

    // Tombol menu laporan yang sedang aktif/ditampilkan di panel (null = panel tertutup)
    private Button menuLaporanAktif = null;

    // ── Section data: kartu produk + tabel tiap master ─────────────────────────
    @FXML private VBox vbDaftarProduk;
    @FXML private VBox placeholderData;
    @FXML private HBox dataSectionRow;

    @FXML private VBox colKaryawan;
    @FXML private VBox colProduk;
    @FXML private VBox colLimbah;
    @FXML private VBox colNasabah;

    @FXML private TableView<MasterKaryawan> tbDashKaryawan;
    @FXML private TableColumn<MasterKaryawan, String> clmDashKaryawanNama, clmDashKaryawanJabatan, clmDashKaryawanStatus;

    @FXML private TableView<MasterLimbah> tbDashLimbah;
    @FXML private TableColumn<MasterLimbah, String> clmDashLimbahNama, clmDashLimbahKategori, clmDashLimbahHarga;

    @FXML private TableView<MasterNasabah> tbDashNasabah;
    @FXML private TableColumn<MasterNasabah, String> clmDashNasabahNama, clmDashNasabahHP, clmDashNasabahStatus;

    // ── Ringkasan statistik (selalu tampil, tidak tergantung klik card) ────────
    @FXML private BarChart<String, Number> chartLimbahKategori;
    @FXML private BarChart<String, Number> chartKaryawanJabatan;
    @FXML private PieChart chartNasabahStatus;

    private final DBConnect db = new DBConnect();

    // ── Style menu sidebar saat aktif — base style (warna/font/hover normal) mengikuti
    //    styleClass "sidebar-btn" dari style.css, PERSIS sama dengan DashboardKasir.
    //    Controller hanya perlu override tampilan untuk item yang sedang aktif/terpilih. ──
    private static final String MENU_ITEM_STYLE_AKTIF =
            "-fx-background-color:#1B5E20; -fx-text-fill:#FFFFFF; -fx-font-weight:bold;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStatistik();
        setupCardHoverEffects();
        setupTabelDashboard();
        loadSemuaDataTabel();
        setupJasperViewerResizeFix();
    }

    /**
     * FIX BUG: SwingNode (JRViewer) tampil putih/kosong walau laporan sudah
     * ter-fill (toolbar & jumlah halaman muncul, tapi area canvas blank).
     * Penyebab: SwingNode tidak otomatis memicu revalidate()/repaint() di sisi
     * Swing setiap kali ukurannya berubah dari sisi JavaFX (misalnya saat
     * panelLaporan baru saja di-set managed=true/visible=true, atau saat
     * window di-resize/maximize). Listener ini memaksa refresh setiap kali
     * ukuran panelLaporan berubah, selama viewer sudah berisi konten.
     */
    private void setupJasperViewerResizeFix() {
        javafx.beans.value.ChangeListener<Number> pemicuRefresh = (obs, oldV, newV) -> {
            javax.swing.JComponent contentSaatIni =
                    (javax.swing.JComponent) swingNodeLaporan.getContent();
            if (contentSaatIni != null) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    contentSaatIni.revalidate();
                    contentSaatIni.repaint();
                });
            }
        };
        panelLaporan.widthProperty().addListener(pemicuRefresh);
        panelLaporan.heightProperty().addListener(pemicuRefresh);
    }

    /**
     * Menandai satu item menu sidebar sebagai aktif (background hijau tua, teks putih
     * bold) dan mengembalikan item lainnya ke style normal bawaan CSS "sidebar-btn"
     * (termasuk efek hover-nya, sama seperti tombol menu di Dashboard Kasir).
     * Panggil dengan {@code null} untuk mengembalikan semua item ke normal
     * (dipakai saat kembali ke dashboard, tidak ada halaman menu yang terbuka).
     */
    private void setMenuSidebarAktif(Button menuAktif) {
        btnLihatLaporan.setStyle(menuAktif == btnLihatLaporan ? MENU_ITEM_STYLE_AKTIF : "");
        btnLihatPenarikan.setStyle(menuAktif == btnLihatPenarikan ? MENU_ITEM_STYLE_AKTIF : "");
    }


    /**
     * Setup hover effects untuk semua cards.
     * Saat cursor mendekat, background berubah hijau, text berubah putih.
     * Saat cursor keluar, kembali ke style original (white background, green text).
     */
    private void setupCardHoverEffects() {
        setupCardHover(cardKaryawan);
        setupCardHover(cardProduk);
        setupCardHover(cardLimbah);
        setupCardHover(cardNasabah);
    }

    /**
     * Setup hover effect untuk satu card.
     * - On Mouse Enter: Background hijau (#2E7D32), text putih
     * - On Mouse Exit: Background putih, text hijau (#2E7D32)
     *
     * PENTING: style asli tiap Label & VBox disimpan SEKALI di awal (snapshot),
     * lalu setiap enter/exit selalu diterapkan dari snapshot itu — bukan dari
     * label.getStyle() yang bisa saja sudah berubah (menghindari style menumpuk
     * atau gagal ter-reset kalau event enter/exit terpicu tidak berurutan).
     */
    // Card statistik yang sedang aktif/dipilih (menentukan tabel apa yang tampil di section data)
    private VBox cardTerpilih = null;

    private void setupCardHover(VBox card) {
        final String originalCardStyle = card.getStyle();
        final String hoverCardStyle = originalCardStyle.replace(
                "-fx-background-color:#FFFFFF;", "-fx-background-color:#2E7D32;");

        // Snapshot style asli setiap Label di dalam card, sekali saja
        final java.util.Map<Label, String> originalLabelStyles = new java.util.LinkedHashMap<Label, String>();
        for (javafx.scene.Node node : card.getChildren()) {
            if (node instanceof Label) {
                Label label = (Label) node;
                originalLabelStyles.put(label, label.getStyle());
            }
        }

        card.setOnMouseEntered(new javafx.event.EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                card.setStyle(hoverCardStyle);
                for (java.util.Map.Entry<Label, String> entry : originalLabelStyles.entrySet()) {
                    // Selalu dibangun dari style ASLI + tambahan putih, bukan dari style saat ini
                    entry.getKey().setStyle(entry.getValue() + " -fx-text-fill:#FFFFFF;");
                }
            }
        });

        card.setOnMouseExited(new javafx.event.EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                // Jika card ini yang sedang terpilih (aktif), pertahankan tampilan "aktif"
                // (sama seperti hover) alih-alih kembali ke style putih original.
                if (card == cardTerpilih) {
                    card.setStyle(hoverCardStyle);
                    for (java.util.Map.Entry<Label, String> entry : originalLabelStyles.entrySet()) {
                        entry.getKey().setStyle(entry.getValue() + " -fx-text-fill:#FFFFFF;");
                    }
                    return;
                }
                card.setStyle(originalCardStyle);
                for (java.util.Map.Entry<Label, String> entry : originalLabelStyles.entrySet()) {
                    // Kembalikan persis ke style asli (bukan hasil regex yang rawan gagal)
                    entry.getKey().setStyle(entry.getValue());
                }
            }
        });

        card.setOnMouseClicked(event -> handleCardClick(card));
    }

    /**
     * Handle click event pada card statistik.
     * Menampilkan section data (tabel/kartu) sesuai master yang kartunya diklik,
     * dan menyembunyikan yang lainnya. Klik ulang pada card yang sama akan
     * menyembunyikan kembali section data (toggle).
     */
    private void handleCardClick(VBox card) {
        VBox kolomTujuan = petakanCardKeKolom(card);
        if (kolomTujuan == null) return;

        if (cardTerpilih == card) {
            // Klik card yang sama lagi -> sembunyikan section data, kembali ke placeholder
            resetTampilanCardTerpilih();
            cardTerpilih = null;
            tampilkanPlaceholder();
            return;
        }

        resetTampilanCardTerpilih();
        cardTerpilih = card;

        placeholderData.setVisible(false);
        placeholderData.setManaged(false);
        dataSectionRow.setVisible(true);
        dataSectionRow.setManaged(true);

        for (VBox kolom : new VBox[]{colKaryawan, colProduk, colLimbah, colNasabah}) {
            boolean tampil = (kolom == kolomTujuan);
            kolom.setVisible(tampil);
            kolom.setManaged(tampil);
        }
    }

    /** Memetakan card statistik yang diklik ke kolom section data yang bersesuaian. */
    private VBox petakanCardKeKolom(VBox card) {
        if (card == cardKaryawan) return colKaryawan;
        if (card == cardProduk) return colProduk;
        if (card == cardLimbah) return colLimbah;
        if (card == cardNasabah) return colNasabah;
        return null;
    }

    /** Mengembalikan card yang sebelumnya aktif ke tampilan normal (jika ada). */
    private void resetTampilanCardTerpilih() {
        if (cardTerpilih != null) {
            VBox cardLama = cardTerpilih;
            // Di-null-kan DULU supaya pengecekan "card == cardTerpilih" di dalam
            // handler exited bernilai false, sehingga style dikembalikan ke normal.
            cardTerpilih = null;
            javafx.event.EventHandler<? super MouseEvent> handler = cardLama.getOnMouseExited();
            if (handler != null) handler.handle(null);
        }
    }

    /** Menampilkan kembali placeholder dan menyembunyikan section data. */
    private void tampilkanPlaceholder() {
        placeholderData.setVisible(true);
        placeholderData.setManaged(true);
        dataSectionRow.setVisible(false);
        dataSectionRow.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SECTION DATA: kartu Daftar Produk + tabel Karyawan / Limbah / Nasabah
    // Semua sudah otomatis scrollable: tabel via TableView bawaan JavaFX,
    // kartu produk via ScrollPane (vbDaftarProduk) yang didefinisikan di FXML.
    // ══════════════════════════════════════════════════════════════════════════

    /** Menghubungkan kolom tabel dashboard ke properti masing-masing model. */
    private void setupTabelDashboard() {
        clmDashKaryawanNama.setCellValueFactory(new PropertyValueFactory<>("namaKaryawan"));
        clmDashKaryawanJabatan.setCellValueFactory(new PropertyValueFactory<>("jabatan"));
        clmDashKaryawanStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tbDashKaryawan.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        clmDashLimbahNama.setCellValueFactory(new PropertyValueFactory<>("namaLimbah"));
        clmDashLimbahKategori.setCellValueFactory(new PropertyValueFactory<>("jenisLimbah"));
        clmDashLimbahHarga.setCellValueFactory(new PropertyValueFactory<>("harga"));
        tbDashLimbah.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        clmDashNasabahNama.setCellValueFactory(new PropertyValueFactory<>("namaNasabah"));
        clmDashNasabahHP.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        clmDashNasabahStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tbDashNasabah.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadSemuaDataTabel() {
        loadTabelKaryawan();
        loadTabelLimbah();
        loadTabelNasabah();
        loadKartuProduk();
        buildRingkasanStatistik();
    }

    private void loadTabelKaryawan() {
        ObservableList<MasterKaryawan> list = FXCollections.observableArrayList();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Karyawan}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                list.add(new MasterKaryawan(
                        db.result.getString("ID_Karyawan"),
                        db.result.getString("Nama_Karyawan"),
                        db.result.getString("Username"),
                        db.result.getString("Password"),
                        db.result.getString("RT"),
                        db.result.getString("RW"),
                        db.result.getString("Kelurahan"),
                        db.result.getString("Kecamatan"),
                        db.result.getString("Kabupaten"),
                        db.result.getString("Provinsi"),
                        db.result.getString("No_HP"),
                        db.result.getString("Jabatan"),
                        db.result.getString("Status"),
                        db.result.getString("Jenis_Kelamin")
                ));
            }
            tbDashKaryawan.setItems(list);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Karyawan", e.getMessage());
        }
    }

    private void loadTabelLimbah() {
        ObservableList<MasterLimbah> list = FXCollections.observableArrayList();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                list.add(new MasterLimbah(
                        db.result.getString("ID_Limbah"),
                        db.result.getString("Nama_Limbah"),
                        db.result.getString("Kategori"),
                        db.result.getString("Satuan"),
                        db.result.getString("Harga"),
                        db.result.getString("Keterangan"),
                        String.valueOf(db.result.getInt("Jumlah"))
                ));
            }
            tbDashLimbah.setItems(list);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Limbah", e.getMessage());
        }
    }

    private void loadTabelNasabah() {
        ObservableList<MasterNasabah> list = FXCollections.observableArrayList();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Nasabah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                list.add(new MasterNasabah(
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
                        "Rp " + db.result.getBigDecimal("Saldo").stripTrailingZeros().toPlainString(),
                        db.result.getString("Bank"),
                        db.result.getString("Status")
                ));
            }
            tbDashNasabah.setItems(list);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Nasabah", e.getMessage());
        }
    }

    /** Memuat daftar produk lalu menampilkannya sebagai kartu di panel kiri (scrollable). */
    private void loadKartuProduk() {
        vbDaftarProduk.getChildren().clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Produk}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                MasterProduk p = new MasterProduk(
                        db.result.getString("ID_Produk"),
                        db.result.getString("Nama_Produk"),
                        String.valueOf(db.result.getInt("Stok")),
                        "Rp " + db.result.getBigDecimal("Harga_Jual").stripTrailingZeros().toPlainString(),
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

                vbDaftarProduk.getChildren().add(buildKartuProdukRingkas(p));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Produk", e.getMessage());
        }
    }

    /** Kartu produk versi ringkas (read-only) untuk ditampilkan di dashboard, lengkap dengan gambar produk. */
    private VBox buildKartuProdukRingkas(MasterProduk p) {
        StackPane gambarBox = buildThumbnailProduk(p, 48);

        Label lblNama = new Label(p.getNamaProduk());
        lblNama.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#1B5E20;");
        lblNama.setWrapText(true);

        Label lblHarga = new Label(p.getHargaJual() + " / " + p.getSatuan());
        lblHarga.setStyle("-fx-font-size:11px; -fx-text-fill:#555;");

        Label lblStok = new Label("Stok: " + p.getStok());
        lblStok.setStyle("-fx-font-size:10px; -fx-text-fill:#888;");

        VBox info = new VBox(3, lblNama, lblHarga, lblStok);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox isiKartu = new HBox(10, gambarBox, info);
        isiKartu.setAlignment(Pos.CENTER_LEFT);
        isiKartu.setPadding(new Insets(8, 10, 8, 8));
        isiKartu.setMaxWidth(Double.MAX_VALUE);

        VBox kartu = new VBox(isiKartu);
        kartu.setMaxWidth(Double.MAX_VALUE);
        kartu.setStyle(
                "-fx-background-color:#FAFAFA; -fx-border-color:#EEEEEE;" +
                        "-fx-border-radius:8; -fx-background-radius:8;");
        return kartu;
    }

    /**
     * Membuat thumbnail gambar produk (persegi, sudut membulat) berukuran ukuranSisi x ukuranSisi.
     * Urutan pencarian gambar: (1) file dari Path_Gambar di database,
     * (2) resource bawaan aplikasi berdasarkan nama produk, (3) kotak abu-abu kosong jika tidak ada.
     */
    private StackPane buildThumbnailProduk(MasterProduk p, double ukuranSisi) {
        ImageView iv = new ImageView();
        iv.setFitWidth(ukuranSisi);
        iv.setFitHeight(ukuranSisi);
        iv.setPreserveRatio(false);

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
            String resName = p.getNamaProduk().replaceAll("\\s+", "_") + ".png";
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/Gambar_Produk/" + resName);
                if (is != null) {
                    iv.setImage(new Image(is));
                    gambarLoaded = true;
                }
            } catch (Exception ignored) {}
        }

        Rectangle clip = new Rectangle(ukuranSisi, ukuranSisi);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        iv.setClip(clip);

        StackPane box = new StackPane(iv);
        box.setMinSize(ukuranSisi, ukuranSisi);
        box.setMaxSize(ukuranSisi, ukuranSisi);
        box.setStyle(
                "-fx-background-color:#F0F0F0; -fx-background-radius:10;" +
                        "-fx-border-color:#E0E0E0; -fx-border-radius:10;");
        return box;
    }

    /**
     * Membangun ketiga chart ringkasan statistik dari data yang SUDAH dimuat
     * ke tabel dashboard (tbDashLimbah, tbDashKaryawan, tbDashNasabah), jadi
     * tidak perlu query ulang ke database.
     * Chart ini selalu tampil, tidak tergantung card mana yang diklik.
     */
    private void buildRingkasanStatistik() {
        // ── Chart 1: Limbah per Kategori (Padat / Cair) ─────────────────────
        java.util.Map<String, Integer> jumlahPerKategori = new java.util.LinkedHashMap<>();
        for (MasterLimbah l : tbDashLimbah.getItems()) {
            String kategori = l.getJenisLimbah() != null ? l.getJenisLimbah() : "Lainnya";
            jumlahPerKategori.merge(kategori, 1, Integer::sum);
        }
        isiBarChart(chartLimbahKategori, jumlahPerKategori, "Jumlah Limbah");

        // ── Chart 2: Karyawan per Jabatan (Kasir / Manager / Admin) ─────────
        java.util.Map<String, Integer> jumlahPerJabatan = new java.util.LinkedHashMap<>();
        for (MasterKaryawan k : tbDashKaryawan.getItems()) {
            String jabatan = k.getJabatan() != null ? k.getJabatan() : "Lainnya";
            jumlahPerJabatan.merge(jabatan, 1, Integer::sum);
        }
        isiBarChart(chartKaryawanJabatan, jumlahPerJabatan, "Jumlah Karyawan");

        // ── Chart 3: Distribusi Status Nasabah (Aktif / Tidak Aktif) ────────
        java.util.Map<String, Integer> jumlahPerStatusNasabah = new java.util.LinkedHashMap<>();
        for (MasterNasabah n : tbDashNasabah.getItems()) {
            String status = n.getStatus() != null ? n.getStatus() : "Lainnya";
            jumlahPerStatusNasabah.merge(status, 1, Integer::sum);
        }
        chartNasabahStatus.getData().clear();
        for (java.util.Map.Entry<String, Integer> entry : jumlahPerStatusNasabah.entrySet()) {
            chartNasabahStatus.getData().add(new PieChart.Data(
                    entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
    }

    /** Helper: mengisi satu BarChart kategori-tunggal dari peta label -> jumlah. */
    private void isiBarChart(BarChart<String, Number> chart, java.util.Map<String, Integer> data, String namaSeri) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(namaSeri);
        for (java.util.Map.Entry<String, Integer> entry : data.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chart.getData().clear();
        chart.getData().add(series);
    }

    /**
     * Memuat semua statistik dari database dan menampilkannya di labels.
     */
    private void loadStatistik() {
        loadJumlahKaryawanAktif();
        loadJumlahProduk();
        loadJumlahLimbah();
        loadJumlahNasabah();
    }

    /**
     * Memuat jumlah karyawan dengan status "Aktif" dari database.
     */
    private void loadJumlahKaryawanAktif() {
        try {
            String query = "SELECT COUNT(*) AS Jumlah FROM tb_Karyawan WHERE Status = 'Aktif'";
            db.result = db.stmt.executeQuery(query);
            if (db.result.next()) {
                int jumlah = db.result.getInt("Jumlah");
                lblJumlahKaryawan.setText(String.valueOf(jumlah));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Karyawan", e.getMessage());
            lblJumlahKaryawan.setText("0");
        }
    }

    /**
     * Memuat jumlah produk dari database.
     */
    private void loadJumlahProduk() {
        try {
            String query = "SELECT COUNT(*) AS Jumlah FROM tb_Produk";
            db.result = db.stmt.executeQuery(query);
            if (db.result.next()) {
                int jumlah = db.result.getInt("Jumlah");
                lblJumlahProduk.setText(String.valueOf(jumlah));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Produk", e.getMessage());
            lblJumlahProduk.setText("0");
        }
    }

    /**
     * Memuat jumlah limbah dari database.
     */
    private void loadJumlahLimbah() {
        try {
            String query = "SELECT COUNT(*) AS Jumlah FROM tb_Limbah";
            db.result = db.stmt.executeQuery(query);
            if (db.result.next()) {
                int jumlah = db.result.getInt("Jumlah");
                lblJumlahLimbah.setText(String.valueOf(jumlah));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Limbah", e.getMessage());
            lblJumlahLimbah.setText("0");
        }
    }

    /**
     * Memuat jumlah nasabah dari database.
     */
    private void loadJumlahNasabah() {
        try {
            String query = "SELECT COUNT(*) AS Jumlah FROM tb_Nasabah";
            db.result = db.stmt.executeQuery(query);
            if (db.result.next()) {
                int jumlah = db.result.getInt("Jumlah");
                lblJumlahNasabah.setText(String.valueOf(jumlah));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Nasabah", e.getMessage());
            lblJumlahNasabah.setText("0");
        }
    }

    /**
     * Handle tombol Logout.
     * TODO: Implementasikan logout dan kembali ke halaman login.
     */
    @FXML
    private void handleLogout() {
        showAlert(Alert.AlertType.INFORMATION, "Info", "Logout belum diimplementasikan.");
        // TODO: Tambahkan logika logout di sini
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PANEL LAPORAN JASPER — muncul full-screen menutupi seluruh area putih saat
    // tombol "Lihat Laporan Transaksi" ditekan
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Handle tombol "Lihat Laporan Transaksi".
     * One-way navigation: jika laporan ini SUDAH terbuka di panel, klik lagi tidak
     * melakukan apa-apa. Untuk kembali ke dashboard, gunakan tombol
     * "← Kembali ke Dashboard" di dalam halaman itu sendiri.
     */
    @FXML
    private void handleLihatLaporan() {
        if (panelLaporan.isVisible() && menuLaporanAktif == btnLihatLaporan) return;

        tampilkanPanelLaporan(btnLihatLaporan, "📄 Laporan Transaksi Setor Limbah", JASPER_REPORT_PATH_SETOR);
    }

    /**
     * Handle tombol "Lihat Penarikan Saldo".
     * Logika pemanggilannya sama persis dengan Lihat Laporan Transaksi (Setor Limbah),
     * hanya beda file .jasper, judul panel, dan cache-nya.
     */
    @FXML
    private void handleLihatPenarikan() {
        if (panelLaporan.isVisible() && menuLaporanAktif == btnLihatPenarikan) return;

        tampilkanPanelLaporan(btnLihatPenarikan, "📄 Laporan Penarikan Saldo", JASPER_REPORT_PATH_PENARIKAN);
    }

    /** Handle tombol "← Kembali ke Dashboard" di header panel laporan. */
    @FXML
    private void handleTutupLaporan() {
        sembunyikanPanelLaporan();
    }

    private void sembunyikanPanelLaporan() {
        setMenuSidebarAktif(null);
        menuLaporanAktif = null;

        panelLaporan.setVisible(false);
        panelLaporan.setManaged(false);
        // Tampilkan kembali dashboard normal di belakangnya.
        scrollDashboard.setVisible(true);
        scrollDashboard.setManaged(true);
    }

    /**
     * Menampilkan panel laporan untuk laporan manapun (Setor Limbah, Penarikan Saldo, dst).
     * Jika laporan yang diminta belum pernah dimuat, laporan diisi (fill) dari file .jasper
     * yang sudah didesain di Jaspersoft Studio, memakai koneksi database yang sama dengan
     * dashboard (db.conn), lalu ditampilkan lewat JRViewer (komponen Swing) yang dibungkus
     * SwingNode agar bisa ditempel di UI JavaFX. Hasil fill di-cache per file .jasper supaya
     * membuka laporan yang sama berkali-kali tidak query ulang ke database.
     *
     * @param tombolMenu tombol sidebar yang memicu (dipakai untuk highlight menu aktif & disable saat loading)
     * @param judulPanel teks judul yang tampil di header panel
     * @param jasperPath path file .jasper yang akan di-fill
     */
    private void tampilkanPanelLaporan(Button tombolMenu, String judulPanel, String jasperPath) {
        setMenuSidebarAktif(tombolMenu);
        menuLaporanAktif = tombolMenu;

        lblJudulLaporan.setText(judulPanel);

        panelLaporan.setVisible(true);
        panelLaporan.setManaged(true);
        scrollDashboard.setVisible(false);
        scrollDashboard.setManaged(false);

        JasperPrint cached = jasperPrintCache.get(jasperPath);
        if (cached != null) {
            tampilkanJasperViewer(cached);
            return;
        }

        progressLaporan.setVisible(true);
        progressLaporan.setManaged(true);
        tombolMenu.setDisable(true);

        // Fill report dijalankan di thread terpisah supaya UI tidak freeze
        // selama proses query & rendering laporan berlangsung.
        Thread threadLaporan = new Thread(() -> {
            File jasperFile = new File(jasperPath);
            if (!jasperFile.exists()) {
                throw new IllegalStateException(
                        "File laporan tidak ditemukan di disk: " + jasperFile.getAbsolutePath() +
                                ". Pastikan aplikasi dijalankan dengan working directory di root project.");
            }

            try (InputStream jasperStream = new java.io.FileInputStream(jasperFile)) {
                Map<String, Object> parameter = new HashMap<>();
                JasperPrint print = JasperFillManager.fillReport(jasperStream, parameter, db.conn);
                jasperPrintCache.put(jasperPath, print);

                Platform.runLater(() -> {
                    tampilkanJasperViewer(print);
                    progressLaporan.setVisible(false);
                    progressLaporan.setManaged(false);
                    tombolMenu.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressLaporan.setVisible(false);
                    progressLaporan.setManaged(false);
                    tombolMenu.setDisable(false);
                    sembunyikanPanelLaporan();
                    showAlert(Alert.AlertType.ERROR, "Gagal Memuat Laporan", e.getMessage());
                });
            }
        }, "thread-laporan-jasper");
        threadLaporan.setDaemon(true);
        threadLaporan.start();
    }

    /**
     * Menempelkan JasperPrint ke JRViewer (Swing) lalu memasangnya ke SwingNode.
     *
     * CATATAN FIX: setelah setContent(), SwingNode tidak otomatis memicu
     * revalidate()/repaint() Swing-side maupun requestLayout() FX-side, jadi
     * toolbar JRViewer bisa muncul tapi area canvas (JScrollPane) tetap putih
     * kosong sampai ada resize manual. Blok di bawah memaksa keduanya terjadi
     * begitu viewer dipasang, supaya laporan langsung tampil tanpa perlu
     * resize window secara manual.
     */
    private void tampilkanJasperViewer(JasperPrint print) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            JRViewer viewer = new JRViewer(print);
            viewer.setZoomRatio(1.0f);
            swingNodeLaporan.setContent(viewer);

            // Paksa Swing menghitung ulang layout & repaint kontennya sendiri
            viewer.revalidate();
            viewer.repaint();

            // Paksa FX-side menghitung ulang layout SwingNode & ukuran sebenarnya,
            // lalu lakukan revalidate/repaint sekali lagi setelah resize diterapkan.
            Platform.runLater(() -> {
                double lebar = panelLaporan.getWidth();
                double tinggi = panelLaporan.getHeight() - 60; // kurangi tinggi header panel
                if (lebar > 0 && tinggi > 0) {
                    swingNodeLaporan.resize(lebar, tinggi);
                }
                if (swingNodeLaporan.getParent() != null) {
                    swingNodeLaporan.getParent().requestLayout();
                }

                javax.swing.SwingUtilities.invokeLater(() -> {
                    viewer.revalidate();
                    viewer.repaint();
                });
            });
        });
    }

    /**
     * Menampilkan alert dialog.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}