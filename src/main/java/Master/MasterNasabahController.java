package Master;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MasterNasabahController implements Initializable {

    @FXML private TableView<MasterNasabah> tbNasabah;
    @FXML private TableColumn<MasterNasabah, String> clmNama, clmHP;

    @FXML private TableColumn<MasterNasabah, String> clmNoRek, clmSaldo;

    @FXML private TextField txtID, txtNama, txtHP;
    @FXML private TextField txtRT, txtRW;
    @FXML private TextField txtNoRek, txtSaldo;
    @FXML private TextField txtCari;
    @FXML private ComboBox<String> cmbBank;

    @FXML private ComboBox<String> cmbProvinsi, cmbKabupaten, cmbKecamatan, cmbKelurahan;

    private ObservableList<MasterNasabah> dataList = FXCollections.observableArrayList();
    private DBConnect db = new DBConnect();

    private static final String DEFAULT_SALDO = "0";

    private boolean isLoadingFromTable = false;

    private static final Map<String, String> KODE_BANK = new HashMap<>();
    static {
        KODE_BANK.put("BCA", "014");
        KODE_BANK.put("BRI", "002");
        KODE_BANK.put("BNI", "009");
        KODE_BANK.put("Permata", "013");
        KODE_BANK.put("CimbNiaga", "022");
        KODE_BANK.put("BSI", "451");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        clmNama.setCellValueFactory(new PropertyValueFactory<>("namaNasabah"));
        clmHP.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        clmNoRek.setCellValueFactory(new PropertyValueFactory<>("noRekening"));
        clmSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));

        addNumericOnly(txtHP, 13);
        addNumericOnly(txtRT, 3);
        addNumericOnly(txtRW, 3);
        addNumericOnly(txtSaldo, 18);
        addLetterOnly(txtNama, 50);

        cmbBank.setItems(FXCollections.observableArrayList(
                "BCA", "BRI", "BNI", "Permata", "CimbNiaga", "BSI"));
        cmbBank.getSelectionModel().selectFirst();

        txtSaldo.setText(DEFAULT_SALDO);
        txtNoRek.setEditable(false);

        cmbBank.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isLoadingFromTable) return;
            generateNoRekening(newVal);
        });

        setupComboboxWilayah();
        loadAutoID();
        loadData();

        generateNoRekening(cmbBank.getValue());
        tbNasabah.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                isLoadingFromTable = true;
                txtID.setText(newVal.getIdNasabah());
                txtNama.setText(newVal.getNamaNasabah());
                txtHP.setText(newVal.getNoHp());
                txtRT.setText(newVal.getRt());
                txtRW.setText(newVal.getRw());
                txtNoRek.setText(newVal.getNoRekening());
                txtSaldo.setText(newVal.getSaldo());
                cmbBank.setValue(newVal.getBank());

                cmbProvinsi.setValue(newVal.getProvinsi());
                cmbKabupaten.setItems(FXCollections.observableArrayList(
                        WilayahData.getKabupatenList(newVal.getProvinsi())));
                cmbKabupaten.setValue(newVal.getKabupaten());

                cmbKecamatan.setItems(FXCollections.observableArrayList(
                        WilayahData.getKecamatanList(newVal.getKabupaten())));
                cmbKecamatan.setValue(newVal.getKecamatan());

                cmbKelurahan.setItems(FXCollections.observableArrayList(
                        WilayahData.getKelurahanList(newVal.getKecamatan())));
                cmbKelurahan.setValue(newVal.getKelurahan());
                isLoadingFromTable = false;
            }
        });
    }

    private void generateNoRekening(String bank) {
        if (bank == null || bank.isEmpty()) {
            txtNoRek.clear();
            return;
        }

        String kodeBank = KODE_BANK.getOrDefault(bank, "000");

        long jumlahExisting = dataList.stream()
                .filter(n -> bank.equals(n.getBank()))
                .count();

        long urutan = jumlahExisting + 1;
        String noRekBaru = kodeBank + String.format("%07d", urutan);

        txtNoRek.setText(noRekBaru);
    }

    private void setupComboboxWilayah() {
        cmbProvinsi.setItems(FXCollections.observableArrayList(WilayahData.getProvinsiList()));
        cmbProvinsi.setPromptText("Semua Provinsi");

        refreshKabupaten(null);
        refreshKecamatan(null);
        refreshKelurahan(null);

        cmbProvinsi.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isLoadingFromTable) return;
            refreshKabupaten(newVal);
            cmbKabupaten.setValue(null);
            refreshKecamatan(null);
            cmbKecamatan.setValue(null);
            refreshKelurahan(null);
            cmbKelurahan.setValue(null);
        });

        cmbKabupaten.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isLoadingFromTable) return;
            refreshKecamatan(newVal);
            cmbKecamatan.setValue(null);
            refreshKelurahan(null);
            cmbKelurahan.setValue(null);
        });

        cmbKecamatan.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isLoadingFromTable) return;
            refreshKelurahan(newVal);
            cmbKelurahan.setValue(null);
        });
    }

    private void refreshKabupaten(String provinsi) {
        cmbKabupaten.setItems(FXCollections.observableArrayList(WilayahData.getKabupatenList(provinsi)));
    }

    private void refreshKecamatan(String kabupaten) {
        cmbKecamatan.setItems(FXCollections.observableArrayList(WilayahData.getKecamatanList(kabupaten)));
    }

    private void refreshKelurahan(String kecamatan) {
        cmbKelurahan.setItems(FXCollections.observableArrayList(WilayahData.getKelurahanList(kecamatan)));
    }

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

    private void loadAutoID() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Nasabah}");
            if (db.result.next()) txtID.setText(db.result.getString("ID_Nasabah"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    private void loadData() {
        dataList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Nasabah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                dataList.add(new MasterNasabah(
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
            tbNasabah.setItems(dataList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data", e.getMessage());
        }
    }

    private void cariData(String keyword) {
        dataList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Search_Nasabah(?)}");
            db.cstat.setString(1, keyword);
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                dataList.add(new MasterNasabah(
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
            tbNasabah.setItems(dataList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Cari Data", e.getMessage());
        }
    }

    @FXML
    private void handleCari() {
        String keyword = txtCari.getText().trim();
        if (keyword.isEmpty()) {
            loadData();
        } else {
            cariData(keyword);
        }
    }

    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;
        try {
            String saldoText = txtSaldo.getText().trim().isEmpty() ? DEFAULT_SALDO : txtSaldo.getText().trim();

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Nasabah(?,?,?,?,?,?,?,?,?,?,?,?)}");
            db.cstat.setString(1,  txtID.getText());
            db.cstat.setString(2,  txtNama.getText());
            db.cstat.setString(3,  txtHP.getText());
            db.cstat.setString(4,  txtNoRek.getText());
            db.cstat.setString(5,  cmbBank.getValue());
            db.cstat.setBigDecimal(6, new BigDecimal(saldoText));
            db.cstat.setString(7,  txtRT.getText());
            db.cstat.setString(8,  txtRW.getText());
            db.cstat.setString(9,  cmbKelurahan.getValue());
            db.cstat.setString(10, cmbKecamatan.getValue());
            db.cstat.setString(11, cmbKabupaten.getValue());
            db.cstat.setString(12, cmbProvinsi.getValue());
            db.cstat.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data nasabah berhasil disimpan.");
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
            db.cstat = db.conn.prepareCall("{CALL sp_Update_Nasabah(?,?,?,?,?,?,?,?,?,?,?)}");
            db.cstat.setString(1,  txtID.getText());
            db.cstat.setString(2,  txtNama.getText());
            db.cstat.setString(3,  txtHP.getText());
            db.cstat.setString(4,  txtNoRek.getText());
            db.cstat.setString(5,  cmbBank.getValue());
            db.cstat.setString(6,  txtRT.getText());
            db.cstat.setString(7,  txtRW.getText());
            db.cstat.setString(8,  cmbKelurahan.getValue());
            db.cstat.setString(9,  cmbKecamatan.getValue());
            db.cstat.setString(10, cmbKabupaten.getValue());
            db.cstat.setString(11, cmbProvinsi.getValue());
            db.cstat.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data nasabah berhasil diubah.");
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
                "Yakin ingin menghapus nasabah ID: " + txtID.getText() + "?",
                ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    db.cstat = db.conn.prepareCall("{CALL sp_Delete_Nasabah(?)}");
                    db.cstat.setString(1, txtID.getText());
                    db.cstat.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data nasabah berhasil dihapus.");
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
                          !txtHP.getText().trim().isEmpty()       ||
                          !txtRT.getText().trim().isEmpty()       ||
                          !txtRW.getText().trim().isEmpty()       ||
                          cmbKelurahan.getValue() != null         ||
                          cmbKecamatan.getValue() != null         ||
                          cmbKabupaten.getValue() != null         ||
                          cmbProvinsi.getValue()  != null;

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
        if (txtNama.getText().trim().isEmpty()      ||
            txtHP.getText().trim().isEmpty()        ||
            txtRT.getText().trim().isEmpty()        ||
            txtRW.getText().trim().isEmpty()        ||
            cmbKelurahan.getValue() == null         ||
            cmbKecamatan.getValue() == null         ||
            cmbKabupaten.getValue() == null         ||
            cmbProvinsi.getValue()  == null         ||
            txtNoRek.getText().trim().isEmpty()     ||
            cmbBank.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Semua data harus diisi!");
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtNama.clear(); txtHP.clear();
        txtRT.clear();   txtRW.clear();
        txtSaldo.setText(DEFAULT_SALDO);
        cmbBank.getSelectionModel().selectFirst();
        generateNoRekening(cmbBank.getValue());

        isLoadingFromTable = true;
        cmbProvinsi.setValue(null);
        refreshKabupaten(null);
        cmbKabupaten.setValue(null);
        refreshKecamatan(null);
        cmbKecamatan.setValue(null);
        refreshKelurahan(null);
        cmbKelurahan.setValue(null);
        isLoadingFromTable = false;

        tbNasabah.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}