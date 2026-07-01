package Master;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class MasterLimbahController implements Initializable {

    @FXML private TableView<MasterLimbah>           tbLimbah;
    @FXML private TableColumn<MasterLimbah, String> clmID, clmJenis, clmJumlah, clmHarga, clmKeterangan;

    @FXML private TextField  txtID, txtHarga, txtJumlah;
    @FXML private TextField  txtCari;
    @FXML private TextArea   txtketerangan;
    @FXML private ComboBox<String> cmbjenis;

    private final ObservableList<MasterLimbah> dataList = FXCollections.observableArrayList();
    private final DBConnect db = new DBConnect();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tbLimbah.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        clmID.setCellValueFactory(new PropertyValueFactory<>("idLimbah"));
        clmJenis.setCellValueFactory(new PropertyValueFactory<>("jenisLimbah"));
        clmJumlah.setCellValueFactory(new PropertyValueFactory<>("satuan"));
        clmHarga.setCellValueFactory(new PropertyValueFactory<>("harga"));
        clmKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));

        cmbjenis.setItems(FXCollections.observableArrayList("Padat", "Cair"));
        cmbjenis.getSelectionModel().selectFirst();

        addNumericOnly(txtHarga);
        addNumericOnly(txtJumlah);

        loadAutoID();
        loadData();

        tbLimbah.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtID.setText(newVal.getIdLimbah());
                cmbjenis.setValue(newVal.getJenisLimbah());
                txtJumlah.setText(newVal.getSatuan());
                txtHarga.setText(newVal.getHarga());
                txtketerangan.setText(newVal.getKeterangan());
            }
        });
    }

    private void addNumericOnly(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9]", "");
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    private void loadAutoID() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Limbah}");
            if (db.result.next()) txtID.setText(db.result.getString("ID_Limbah"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    private void loadData() {
        dataList.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Limbah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                dataList.add(new MasterLimbah(
                    db.result.getString("ID_Limbah"),
                    db.result.getString("Jenis_Limbah"),
                    db.result.getString("Satuan"),
                    db.result.getString("Harga"),
                    db.result.getString("Keterangan")
                ));
            }
            tbLimbah.setItems(dataList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data", e.getMessage());
        }
    }

    private void cariData(String keyword) {
    dataList.clear();
    try {
        db.cstat = db.conn.prepareCall("{CALL sp_Search_Limbah(?)}");
        db.cstat.setString(1, keyword);
        db.result = db.cstat.executeQuery();
        while (db.result.next()) {
            dataList.add(new MasterLimbah(
                db.result.getString("ID_Limbah"),
                db.result.getString("Jenis_Limbah"),
                db.result.getString("Satuan"),
                db.result.getString("Harga"),
                db.result.getString("Keterangan")
            ));
        }
        tbLimbah.setItems(dataList);
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
    private void handleUbah() {
        if (txtID.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin diubah terlebih dahulu.");
            return;
        }
        if (!validateForm()) return;
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Update_Limbah(?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, cmbjenis.getValue());
            db.cstat.setString(3, txtJumlah.getText());
            db.cstat.setString(4, txtHarga.getText());
            db.cstat.setString(5, txtketerangan.getText());
            db.cstat.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data limbah berhasil diubah.");
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
            "Yakin ingin menghapus limbah ID: " + txtID.getText() + "?",
            ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    db.cstat = db.conn.prepareCall("{CALL sp_Delete_Limbah(?)}");
                    db.cstat.setString(1, txtID.getText());
                    db.cstat.executeUpdate();
                    showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data limbah berhasil dihapus.");
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
    boolean adaIsi = cmbjenis.getValue() != null && !cmbjenis.getValue().isEmpty() ||
                      !txtJumlah.getText().trim().isEmpty()      ||
                      !txtHarga.getText().trim().isEmpty()       ||
                      !txtketerangan.getText().trim().isEmpty();

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
        if (cmbjenis.getValue()              == null  ||
            txtJumlah.getText().trim().isEmpty()      ||
            txtHarga.getText().trim().isEmpty()       ||
            txtketerangan.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Semua data harus diisi!");
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtHarga.clear();
        txtJumlah.clear();
        txtketerangan.clear();
        cmbjenis.getSelectionModel().selectFirst();
        tbLimbah.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}