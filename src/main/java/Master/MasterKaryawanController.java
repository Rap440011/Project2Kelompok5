package Master;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.*;
import java.util.List;
import java.util.ResourceBundle;

public class MasterKaryawanController implements Initializable {

    @FXML private TableView<MasterKaryawan> tbKaryawan;
    @FXML private TableColumn<MasterKaryawan, String> clmNama, clmUsn;
    @FXML private TableColumn<MasterKaryawan, String> clmHP;
    @FXML private TableColumn<MasterKaryawan, String> clmJabatan, clmJabatan1, clmJabatan11;

    @FXML private TextField txtID, txtNama, txtUsn, txtPW;
    @FXML private TextField txtRT, txtRW, txtHP;
    @FXML private TextField txtCari;
    @FXML private TextField txtStatus;

    @FXML private ComboBox<String> cmbJabatan, cmbJK;

    @FXML private ComboBox<String> cmbProvinsi, cmbKabupaten, cmbKecamatan, cmbKelurahan;

    private ObservableList<MasterKaryawan> dataList = FXCollections.observableArrayList();
    private DBConnect db = new DBConnect();

    // Supaya listener tidak saling memicu reset saat form diisi dari baris tabel
    private boolean isLoadingFromTable = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        clmNama.setCellValueFactory(new PropertyValueFactory<>("namaKaryawan"));
        clmUsn.setCellValueFactory(new PropertyValueFactory<>("username"));
        clmHP.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        clmJabatan.setCellValueFactory(new PropertyValueFactory<>("jabatan"));
        clmJabatan1.setCellValueFactory(new PropertyValueFactory<>("status"));
        clmJabatan11.setCellValueFactory(new PropertyValueFactory<>("jenisKelamin"));

        cmbJabatan.setItems(FXCollections.observableArrayList("Kasir", "Manager", "Admin"));
        cmbJabatan.getSelectionModel().selectFirst();

        // Status default "Aktif" dan tidak bisa diubah oleh user
        txtStatus.setText("Aktif");
        txtStatus.setEditable(false);

        cmbJK.setItems(FXCollections.observableArrayList("Laki-laki", "Perempuan"));
        cmbJK.getSelectionModel().selectFirst();

        addNumericOnly(txtRT, 3);
        addNumericOnly(txtRW, 3);
        addNumericOnly(txtHP, 13);
        addLetterOnly(txtNama, 50);

        setupComboboxWilayah();
        loadAutoID();
        loadData();

        tbKaryawan.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                isLoadingFromTable = true;
                txtID.setText(newVal.getIdKaryawan());
                txtNama.setText(newVal.getNamaKaryawan());
                txtUsn.setText(newVal.getUsername());
                txtPW.setText(newVal.getPassword());
                txtRT.setText(newVal.getRt());
                txtRW.setText(newVal.getRw());
                txtHP.setText(newVal.getNoHp());
                cmbJabatan.setValue(newVal.getJabatan());
                cmbJK.setValue(newVal.getJenisKelamin());
                txtStatus.setText(newVal.getStatus());

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
        List<String> list = WilayahData.getKabupatenList(provinsi);
        cmbKabupaten.setItems(FXCollections.observableArrayList(list));
    }

    private void refreshKecamatan(String kabupaten) {
        List<String> list = WilayahData.getKecamatanList(kabupaten);
        cmbKecamatan.setItems(FXCollections.observableArrayList(list));
    }

    private void refreshKelurahan(String kecamatan) {
        List<String> list = WilayahData.getKelurahanList(kecamatan);
        cmbKelurahan.setItems(FXCollections.observableArrayList(list));
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
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Karyawan}");
            if (db.result.next()) txtID.setText(db.result.getString("ID_Karyawan"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    private void loadData() {
        dataList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Karyawan}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                dataList.add(new MasterKaryawan(
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
            tbKaryawan.setItems(dataList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data", e.getMessage());
        }
    }

    private void cariData(String keyword) {
        dataList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Search_Karyawan(?)}");
            db.cstat.setString(1, keyword);
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                dataList.add(new MasterKaryawan(
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
            tbKaryawan.setItems(dataList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Cari Data", e.getMessage());
        }
    }

    @FXML
    private void handleCari() {
        String keyword = txtCari.getText().trim();
        if (keyword.isEmpty()) loadData();
        else cariData(keyword);
    }

    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Karyawan(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");
            db.cstat.setString(1,  txtID.getText());
            db.cstat.setString(2,  txtNama.getText());
            db.cstat.setString(3,  txtUsn.getText());
            db.cstat.setString(4,  txtPW.getText());
            db.cstat.setString(5,  cmbJabatan.getValue());
            db.cstat.setString(6,  cmbJK.getValue());
            db.cstat.setString(7,  txtHP.getText());
            db.cstat.setString(8,  txtRT.getText());
            db.cstat.setString(9,  txtRW.getText());
            db.cstat.setString(10, cmbKelurahan.getValue());
            db.cstat.setString(11, cmbKecamatan.getValue());
            db.cstat.setString(12, cmbKabupaten.getValue());
            db.cstat.setString(13, cmbProvinsi.getValue());
            db.cstat.setString(14, "Aktif"); // selalu simpan sebagai Aktif
            db.cstat.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data karyawan berhasil disimpan.");
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
            db.cstat = db.conn.prepareCall("{CALL sp_Update_Karyawan(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");
            db.cstat.setString(1,  txtID.getText());
            db.cstat.setString(2,  txtNama.getText());
            db.cstat.setString(3,  txtUsn.getText());
            db.cstat.setString(4,  txtPW.getText());
            db.cstat.setString(5,  cmbJabatan.getValue());
            db.cstat.setString(6,  cmbJK.getValue());
            db.cstat.setString(7,  txtHP.getText());
            db.cstat.setString(8,  txtRT.getText());
            db.cstat.setString(9,  txtRW.getText());
            db.cstat.setString(10, cmbKelurahan.getValue());
            db.cstat.setString(11, cmbKecamatan.getValue());
            db.cstat.setString(12, cmbKabupaten.getValue());
            db.cstat.setString(13, cmbProvinsi.getValue());
            db.cstat.setString(14, txtStatus.getText());
            db.cstat.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data karyawan berhasil diubah.");
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
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin dinonaktifkan terlebih dahulu.");
            return;
        }

        // Cegah menonaktifkan yang sudah tidak aktif
        if ("Tidak Aktif".equalsIgnoreCase(txtStatus.getText())) {
            showAlert(Alert.AlertType.INFORMATION, "Info",
                    "Karyawan ini sudah berstatus Tidak Aktif.");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Karyawan ID: " + txtID.getText() + " akan dinonaktifkan.\n" +
                "Data tidak akan dihapus, status berubah menjadi 'Tidak Aktif'.\n\nLanjutkan?",
                ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Nonaktifkan");
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    db.cstat = db.conn.prepareCall("{CALL sp_SoftDelete_Karyawan(?)}");
                    db.cstat.setString(1, txtID.getText());
                    db.cstat.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "Berhasil",
                            "Karyawan berhasil dinonaktifkan.");
                    clearForm();
                    loadData();
                    loadAutoID();
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Error Nonaktifkan", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBatal() {
        boolean adaIsi = !txtNama.getText().trim().isEmpty()       ||
                          !txtUsn.getText().trim().isEmpty()       ||
                          !txtPW.getText().trim().isEmpty()        ||
                          !txtRT.getText().trim().isEmpty()        ||
                          !txtRW.getText().trim().isEmpty()        ||
                          cmbKelurahan.getValue() != null          ||
                          cmbKecamatan.getValue() != null          ||
                          cmbKabupaten.getValue() != null          ||
                          cmbProvinsi.getValue()  != null          ||
                          !txtHP.getText().trim().isEmpty();

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
            txtUsn.getText().trim().isEmpty()       ||
            txtPW.getText().trim().isEmpty()        ||
            txtRT.getText().trim().isEmpty()        ||
            txtRW.getText().trim().isEmpty()        ||
            cmbKelurahan.getValue() == null         ||
            cmbKecamatan.getValue() == null         ||
            cmbKabupaten.getValue() == null         ||
            cmbProvinsi.getValue()  == null         ||
            txtHP.getText().trim().isEmpty()        ||
            cmbJabatan.getValue() == null           ||
            cmbJK.getValue()      == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Semua data harus diisi!");
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtNama.clear(); txtUsn.clear(); txtPW.clear();
        txtRT.clear();   txtRW.clear();  txtHP.clear();

        // Reset combobox alamat -> kembali menampilkan semua data
        isLoadingFromTable = true;
        cmbProvinsi.setValue(null);
        refreshKabupaten(null);
        cmbKabupaten.setValue(null);
        refreshKecamatan(null);
        cmbKecamatan.setValue(null);
        refreshKelurahan(null);
        cmbKelurahan.setValue(null);
        isLoadingFromTable = false;

        cmbJabatan.getSelectionModel().selectFirst();
        cmbJK.getSelectionModel().selectFirst();
        // Reset status kembali ke default Aktif
        txtStatus.setText("Aktif");
        tbKaryawan.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}