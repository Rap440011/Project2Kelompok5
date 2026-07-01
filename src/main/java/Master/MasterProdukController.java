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
import java.util.ResourceBundle;

public class MasterProdukController implements Initializable {

    @FXML private TableView<MasterProduk> tbProduk;
    @FXML private TableColumn<MasterProduk, String> clmID, clmNama, clmStock;
    @FXML private TableColumn<MasterProduk, String> clmHrgJual, clmSatuan, clmKeterangan;

    @FXML private TextField txtID, txtNama, txtStock, txtHrgJual;
    @FXML private TextField txtCari;
    @FXML private ComboBox<String> cmbSatuan;
    @FXML private TextArea txtKeterangan;

    private ObservableList<MasterProduk> dataList = FXCollections.observableArrayList();
    private DBConnect db = new DBConnect();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        clmID.setCellValueFactory(new PropertyValueFactory<>("idProduk"));
        clmNama.setCellValueFactory(new PropertyValueFactory<>("namaProduk"));
        clmStock.setCellValueFactory(new PropertyValueFactory<>("stok"));
        clmHrgJual.setCellValueFactory(new PropertyValueFactory<>("hargaJual"));
        clmSatuan.setCellValueFactory(new PropertyValueFactory<>("satuan"));
        clmKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));

        cmbSatuan.setItems(FXCollections.observableArrayList("Kg", "Liter"));
        cmbSatuan.getSelectionModel().selectFirst();

        addNumericOnly(txtStock, 10);
        addNumericOnly(txtHrgJual, 18);
        addLetterOnly(txtNama, 50);

        loadAutoID();
        loadData();

        tbProduk.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtID.setText(newVal.getIdProduk());
                txtNama.setText(newVal.getNamaProduk());
                txtStock.setText(newVal.getStok());
                txtHrgJual.setText(newVal.getHargaJual());
                cmbSatuan.setValue(newVal.getSatuan());
                txtKeterangan.setText(newVal.getKeterangan());
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
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Produk}");
            if (db.result.next()) txtID.setText(db.result.getString("ID_Produk"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

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
                    db.result.getString("Harga_Jual"),
                    db.result.getString("Satuan"),
                    db.result.getString("Keterangan")
                ));
            }
            tbProduk.setItems(dataList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Data", e.getMessage());
        }
    }

    private void cariData(String keyword) {
    dataList.clear();
    try {
        db.cstat = db.conn.prepareCall("{CALL sp_Search_Produk(?)}");
        db.cstat.setString(1, keyword);
        db.result = db.cstat.executeQuery();
        while (db.result.next()) {
            dataList.add(new MasterProduk(
                db.result.getString("ID_Produk"),
                db.result.getString("Nama_Produk"),
                db.result.getString("Stok"),
                db.result.getString("Harga_Jual"),
                db.result.getString("Satuan"),
                db.result.getString("Keterangan")
            ));
        }
        tbProduk.setItems(dataList);
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
            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Produk(?,?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText());
            db.cstat.setInt(3, Integer.parseInt(txtStock.getText()));
            db.cstat.setBigDecimal(4, new BigDecimal(txtHrgJual.getText()));
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
            db.cstat.setBigDecimal(4, new BigDecimal(txtHrgJual.getText()));
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
        txtHrgJual.clear();
        txtKeterangan.clear();
        cmbSatuan.getSelectionModel().selectFirst();
        tbProduk.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}