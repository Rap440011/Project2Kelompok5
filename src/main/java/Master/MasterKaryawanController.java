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
import java.util.ResourceBundle;

public class MasterKaryawanController implements Initializable {

    @FXML private TableView<MasterKaryawan> tbKaryawan;
    @FXML private TableColumn<MasterKaryawan, String> clmID, clmNama, clmUsn, clmPW;
    @FXML private TableColumn<MasterKaryawan, String> clmRT, clmRW, clmKelurahan, clmKecamatan;
    @FXML private TableColumn<MasterKaryawan, String> clmKabupaten, clmProvinsi, clmHP;
    @FXML private TableColumn<MasterKaryawan, String> clmJabatan, clmJabatan1, clmJabatan11;

    @FXML private TextField txtID, txtNama, txtUsn, txtPW;
    @FXML private TextField txtRT, txtRW, txtKelurahan, txtKecamatan;
    @FXML private TextField txtKabupaten, txtProvinsi, txtHP;
    @FXML private TextField txtCari;
    @FXML private ComboBox<String> cmbJabatan, cmbStatus, cmbJK;

    private ObservableList<MasterKaryawan> dataList = FXCollections.observableArrayList();
    private DBConnect db = new DBConnect();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        clmID.setCellValueFactory(new PropertyValueFactory<>("idKaryawan"));
        clmNama.setCellValueFactory(new PropertyValueFactory<>("namaKaryawan"));
        clmUsn.setCellValueFactory(new PropertyValueFactory<>("username"));
        clmPW.setCellValueFactory(new PropertyValueFactory<>("password"));
        clmRT.setCellValueFactory(new PropertyValueFactory<>("rt"));
        clmRW.setCellValueFactory(new PropertyValueFactory<>("rw"));
        clmKelurahan.setCellValueFactory(new PropertyValueFactory<>("kelurahan"));
        clmKecamatan.setCellValueFactory(new PropertyValueFactory<>("kecamatan"));
        clmKabupaten.setCellValueFactory(new PropertyValueFactory<>("kabupaten"));
        clmProvinsi.setCellValueFactory(new PropertyValueFactory<>("provinsi"));
        clmHP.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        clmJabatan.setCellValueFactory(new PropertyValueFactory<>("jabatan"));
        clmJabatan1.setCellValueFactory(new PropertyValueFactory<>("status"));
        clmJabatan11.setCellValueFactory(new PropertyValueFactory<>("jenisKelamin"));

        cmbJabatan.setItems(FXCollections.observableArrayList("Kasir", "Manager", "Admin"));
        cmbJabatan.getSelectionModel().selectFirst();

        cmbStatus.setItems(FXCollections.observableArrayList("Aktif", "Tidak Aktif"));
        cmbStatus.getSelectionModel().selectFirst();

        cmbJK.setItems(FXCollections.observableArrayList("Laki-laki", "Perempuan"));
        cmbJK.getSelectionModel().selectFirst();

        addNumericOnly(txtRT, 3);
        addNumericOnly(txtRW, 3);
        addNumericOnly(txtHP, 13);
        addLetterOnly(txtNama, 50);
        addLetterOnly(txtKelurahan, 30);
        addLetterOnly(txtKecamatan, 26);
        addLetterOnly(txtKabupaten, 33);
        addLetterOnly(txtProvinsi, 20);

        loadAutoID();
        loadData();

        tbKaryawan.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtID.setText(newVal.getIdKaryawan());
                txtNama.setText(newVal.getNamaKaryawan());
                txtUsn.setText(newVal.getUsername());
                txtPW.setText(newVal.getPassword());
                txtRT.setText(newVal.getRt());
                txtRW.setText(newVal.getRw());
                txtKelurahan.setText(newVal.getKelurahan());
                txtKecamatan.setText(newVal.getKecamatan());
                txtKabupaten.setText(newVal.getKabupaten());
                txtProvinsi.setText(newVal.getProvinsi());
                txtHP.setText(newVal.getNoHp());
                cmbJabatan.setValue(newVal.getJabatan());
                cmbStatus.setValue(newVal.getStatus());
                cmbJK.setValue(newVal.getJenisKelamin());
            }
        });
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
        db.cstat.setString(10, txtKelurahan.getText());
        db.cstat.setString(11, txtKecamatan.getText());
        db.cstat.setString(12, txtKabupaten.getText());
        db.cstat.setString(13, txtProvinsi.getText());
        db.cstat.setString(14, cmbStatus.getValue());
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
        db.cstat.setString(10, txtKelurahan.getText());
        db.cstat.setString(11, txtKecamatan.getText());
        db.cstat.setString(12, txtKabupaten.getText());
        db.cstat.setString(13, txtProvinsi.getText());
        db.cstat.setString(14, cmbStatus.getValue());
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
        showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin dihapus terlebih dahulu.");
        return;
    }
    Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
            "Yakin ingin menghapus karyawan ID: " + txtID.getText() + "?",
            ButtonType.YES, ButtonType.NO);
    konfirmasi.setTitle("Konfirmasi Hapus");
    konfirmasi.showAndWait().ifPresent(bt -> {
        if (bt == ButtonType.YES) {
            try {
                db.cstat = db.conn.prepareCall("{CALL sp_Delete_Karyawan(?)}");
                db.cstat.setString(1, txtID.getText());
                db.cstat.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data karyawan berhasil dihapus.");
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
                      !txtUsn.getText().trim().isEmpty()      ||
                      !txtPW.getText().trim().isEmpty()       ||
                      !txtRT.getText().trim().isEmpty()       ||
                      !txtRW.getText().trim().isEmpty()       ||
                      !txtKelurahan.getText().trim().isEmpty()||
                      !txtKecamatan.getText().trim().isEmpty()||
                      !txtKabupaten.getText().trim().isEmpty()||
                      !txtProvinsi.getText().trim().isEmpty() ||
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
            txtKelurahan.getText().trim().isEmpty() ||
            txtKecamatan.getText().trim().isEmpty() ||
            txtKabupaten.getText().trim().isEmpty() ||
            txtProvinsi.getText().trim().isEmpty()  ||
            txtHP.getText().trim().isEmpty()        ||
            cmbJabatan.getValue() == null           ||
            cmbStatus.getValue()  == null           ||
            cmbJK.getValue()      == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Semua data harus diisi!");
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtNama.clear(); txtUsn.clear(); txtPW.clear();
        txtRT.clear();   txtRW.clear();
        txtKelurahan.clear(); txtKecamatan.clear();
        txtKabupaten.clear(); txtProvinsi.clear(); txtHP.clear();
        cmbJabatan.getSelectionModel().selectFirst();
        cmbStatus.getSelectionModel().selectFirst();
        cmbJK.getSelectionModel().selectFirst();
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