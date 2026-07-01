package Transaksi;

import Connection.DBConnect;
import Master.MasterNasabah;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class TransaksiSetorLimbahController implements Initializable {

    // ---- Harga per satuan ----
    private static final BigDecimal HARGA_CAIR  = new BigDecimal("2000"); // per Liter
    private static final BigDecimal HARGA_PADAT = new BigDecimal("3000"); // per Kg

    // ---- Panel kanan : Header Transaksi ----
    @FXML private TextField txtIDTransaksi;
    @FXML private TextField txtIDNasabah;
    @FXML private TextField txtIDKaryawan;
    @FXML private DatePicker dpTanggal;
    @FXML private TextField txtTotal;

    @FXML private TextField txtCariNasabah;
    @FXML private Button btnCariNasabah;
    @FXML private Button btnBatalTransaksi;
    @FXML private Button btnSelesai;

    @FXML private TableView<MasterNasabah> tbNasabah;
    @FXML private TableColumn<MasterNasabah, String> clmNasabahID, clmNasabahNama, clmNasabahHP, clmNasabahSaldo;

    // ---- Panel kiri : Detail Transaksi ----
    @FXML private TextField txtIDDetail;
    @FXML private TextField txtIDSetorLimbahDetail;
    @FXML private ComboBox<String> cmbJenis;
    @FXML private TextField txtJumlah;
    @FXML private Label lblSatuan;
    @FXML private TextArea txtKeteranganDetail;
    @FXML private TextField txtSubTotal;
    @FXML private Button btnTambahTransaksi;

    @FXML private TableView<DetailSetorLimbah> tbDetail;
    @FXML private TableColumn<DetailSetorLimbah, String> clmDetailID, clmDetailJenis, clmDetailJumlah;
    @FXML private TableColumn<DetailSetorLimbah, String> clmDetailSatuan, clmDetailKeterangan, clmDetailSubTotal;

    private final ObservableList<MasterNasabah> nasabahList = FXCollections.observableArrayList();
    private final ObservableList<DetailSetorLimbah> detailList = FXCollections.observableArrayList();
    private final DBConnect db = new DBConnect();

    private BigDecimal totalTransaksi = BigDecimal.ZERO;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTabelNasabah();
        setupTabelDetail();
        setupComboJenis();

        addNumericOnly(txtJumlah, 10);

        loadAutoIDTransaksi();
        loadDataNasabah();

        // Detail hanya bisa diisi setelah ID Nasabah terisi
        setDetailPanelEnabled(false);

        // Klik baris nasabah -> isi ID Nasabah
        tbNasabah.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtIDNasabah.setText(newVal.getIdNasabah());
                txtIDSetorLimbahDetail.setText(txtIDTransaksi.getText());
                setDetailPanelEnabled(true);
                loadAutoIDDetail();
            }
        });

        // Hitung subtotal otomatis saat jumlah / jenis berubah
        txtJumlah.textProperty().addListener((obs, oldVal, newVal) -> hitungSubTotal());
        cmbJenis.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateLabelSatuan();
            hitungSubTotal();
        });
    }

    // ===================== SETUP =====================

    private void setupTabelNasabah() {
        clmNasabahNama.setCellValueFactory(new PropertyValueFactory<>("namaNasabah"));
        clmNasabahHP.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        clmNasabahSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));
    }

    private void setupTabelDetail() {
        clmDetailID.setCellValueFactory(new PropertyValueFactory<>("idDetail"));
        clmDetailJenis.setCellValueFactory(new PropertyValueFactory<>("jenis"));
        clmDetailJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        clmDetailSatuan.setCellValueFactory(new PropertyValueFactory<>("satuan"));
        clmDetailKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));
        clmDetailSubTotal.setCellValueFactory(new PropertyValueFactory<>("subTotal"));
        tbDetail.setItems(detailList);
    }

    private void setupComboJenis() {
        cmbJenis.setItems(FXCollections.observableArrayList("Cair", "Padat"));
        cmbJenis.getSelectionModel().selectFirst();
        updateLabelSatuan();
    }

    private void setDetailPanelEnabled(boolean enabled) {
        cmbJenis.setDisable(!enabled);
        txtJumlah.setDisable(!enabled);
        txtKeteranganDetail.setDisable(!enabled);
        btnTambahTransaksi.setDisable(!enabled);
    }

    private void updateLabelSatuan() {
        String jenis = cmbJenis.getValue();
        lblSatuan.setText("Padat".equalsIgnoreCase(jenis) ? "Kg" : "Liter");
    }

    private void addNumericOnly(TextField field, int maxLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9]", "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    // ===================== AUTO ID =====================

    private void loadAutoIDTransaksi() {
    try {
        db.result = db.stmt.executeQuery("{CALL sp_AutoID_SetorLimbah}");
        if (db.result.next()) txtIDTransaksi.setText(db.result.getString("ID_Setor"));
        txtIDSetorLimbahDetail.setText(txtIDTransaksi.getText());
    } catch (SQLException e) {
        showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
    }
}

    private void loadAutoIDDetail() {
    try {
        db.cstat = db.conn.prepareCall("{CALL sp_AutoID_DetailSetorLimbah(?)}");
        db.cstat.setString(1, txtIDTransaksi.getText()); // kirim ID_Setor
        db.result = db.cstat.executeQuery();
        if (db.result.next()) txtIDDetail.setText(db.result.getString("ID_Detail_Setor")); // sesuai alias SP
    } catch (SQLException e) {
        showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
    }
}

    // ===================== LOAD / CARI NASABAH =====================
    private void loadDataNasabah() {
        nasabahList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Nasabah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                nasabahList.add(new MasterNasabah(
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
                    db.result.getString("Saldo"),
                    db.result.getString("Bank")
                ));
            }
            tbNasabah.setItems(nasabahList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data", e.getMessage());
        }
    }

    private void cariDataNasabah(String keyword) {
        nasabahList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Search_Nasabah(?)}");
            db.cstat.setString(1, keyword);
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                nasabahList.add(new MasterNasabah(
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
                    db.result.getString("Saldo"),
                    db.result.getString("Bank")
                ));
            }
            tbNasabah.setItems(nasabahList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Cari Data", e.getMessage());
        }
    }

    @FXML
    private void handleCariNasabah() {
        String keyword = txtCariNasabah.getText().trim();
        if (keyword.isEmpty()) {
            loadDataNasabah();
        } else {
            cariDataNasabah(keyword);
        }
    }

    // ===================== HITUNG SUB TOTAL & TOTAL =====================

    private void hitungSubTotal() {
        try {
            String jumlahText = txtJumlah.getText().trim();
            if (jumlahText.isEmpty()) {
                txtSubTotal.setText("");
                return;
            }
            BigDecimal jumlah = new BigDecimal(jumlahText);
            BigDecimal harga = "Padat".equalsIgnoreCase(cmbJenis.getValue()) ? HARGA_PADAT : HARGA_CAIR;
            BigDecimal subTotal = jumlah.multiply(harga);
            txtSubTotal.setText(subTotal.toPlainString());
        } catch (NumberFormatException e) {
            txtSubTotal.setText("");
        }
    }

    private void hitungTotalTransaksi() {
        totalTransaksi = BigDecimal.ZERO;
        for (DetailSetorLimbah d : detailList) {
            try {
                totalTransaksi = totalTransaksi.add(new BigDecimal(d.getSubTotal()));
            } catch (NumberFormatException ignored) {}
        }
        txtTotal.setText(totalTransaksi.toPlainString());
    }

    // ===================== TAMBAH TRANSAKSI (simpan detail) =====================

    @FXML
    private void handleTambahTransaksi() {
        if (!validateDetailForm()) return;
        try {
            String jumlah = txtJumlah.getText().trim();
            String satuan = lblSatuan.getText();
            String jenis = cmbJenis.getValue();
            String keterangan = txtKeteranganDetail.getText().trim();
            String subTotal = txtSubTotal.getText().trim();

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_DetailSetorLimbah(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, txtIDDetail.getText());
            db.cstat.setString(2, txtIDSetorLimbahDetail.getText());
            db.cstat.setString(3, jenis);
            db.cstat.setBigDecimal(4, new BigDecimal(jumlah));
            db.cstat.setString(5, satuan);
            db.cstat.setString(6, keterangan);
            db.cstat.setBigDecimal(7, new BigDecimal(subTotal));
            db.cstat.executeUpdate();

            detailList.add(new DetailSetorLimbah(
                    txtIDDetail.getText(), txtIDSetorLimbahDetail.getText(),
                    jenis, jumlah, satuan, keterangan, subTotal));

            hitungTotalTransaksi();
            btnSelesai.setDisable(false);

            clearDetailForm();
            loadAutoIDDetail();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Tambah Transaksi", e.getMessage());
        }
    }

    private boolean validateDetailForm() {
        if (txtIDNasabah.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih nasabah terlebih dahulu pada tabel di sebelah kanan.");
            return false;
        }
        if (cmbJenis.getValue() == null || txtJumlah.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Jenis dan Jumlah wajib diisi!");
            return false;
        }
        return true;
    }

    private void clearDetailForm() {
        cmbJenis.getSelectionModel().selectFirst();
        txtJumlah.clear();
        txtKeteranganDetail.clear();
        txtSubTotal.clear();
    }

    // ===================== SELESAI (simpan header transaksi) =====================

    @FXML
    private void handleSelesai() {
        if (detailList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Tambahkan minimal satu detail transaksi terlebih dahulu.");
            return;
        }
        if (txtIDKaryawan.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "ID Karyawan wajib diisi.");
            return;
        }
        if (dpTanggal.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Tanggal wajib dipilih.");
            return;
        }

        try {
            String tanggal = dpTanggal.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_SetorLimbah(?,?,?,?,?)}");
            db.cstat.setString(1, txtIDTransaksi.getText());
            db.cstat.setString(2, txtIDNasabah.getText());
            db.cstat.setString(3, txtIDKaryawan.getText());
            db.cstat.setString(4, tanggal);
            db.cstat.setBigDecimal(5, totalTransaksi);
            db.cstat.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Transaksi setor limbah berhasil disimpan.");
            resetSemua();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Selesai", e.getMessage());
        }
    }

    // ===================== BATAL =====================

    @FXML
    private void handleBatalTransaksi() {
        boolean adaIsi = !txtIDNasabah.getText().trim().isEmpty() || !detailList.isEmpty();

        if (!adaIsi) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Tidak ada data yang perlu dibatalkan.");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin membatalkan transaksi ini? Detail yang sudah ditambahkan akan tetap tersimpan di database, namun transaksi tidak akan diselesaikan.",
                ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Batal");
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                resetSemua();
            }
        });
    }

    private void resetSemua() {
        txtIDNasabah.clear();
        txtIDKaryawan.clear();
        dpTanggal.setValue(null);
        txtTotal.clear();
        totalTransaksi = BigDecimal.ZERO;

        detailList.clear();
        clearDetailForm();
        setDetailPanelEnabled(false);
        btnSelesai.setDisable(true);

        tbNasabah.getSelectionModel().clearSelection();
        txtCariNasabah.clear();
        loadDataNasabah();

        loadAutoIDTransaksi();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
