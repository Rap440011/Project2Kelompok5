package Master;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class MasterLimbahController implements Initializable {

    @FXML private TableView<MasterLimbah> tbLimbah;
    @FXML private TableColumn<MasterLimbah, String> clmID, clmNama, clmKategori, clmSatuan, clmJumlah, clmHarga, clmKeterangan;

    @FXML private TextField  txtID, txtNama, txtHarga, txtJumlah;
    @FXML private TextField  txtCari;
    @FXML private TextArea   txtketerangan;
    @FXML private ComboBox<String> cmbjenis;

    private final ObservableList<MasterLimbah> dataList = FXCollections.observableArrayList();
    private final DBConnect db = new DBConnect();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tbLimbah.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        clmID.setCellValueFactory(new PropertyValueFactory<>("idLimbah"));
        clmNama.setCellValueFactory(new PropertyValueFactory<>("namaLimbah"));
        clmKategori.setCellValueFactory(new PropertyValueFactory<>("jenisLimbah"));
        clmSatuan.setCellValueFactory(new PropertyValueFactory<>("satuan"));
        clmJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        clmHarga.setCellValueFactory(new PropertyValueFactory<>("harga"));
        clmKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));

        cmbjenis.setItems(FXCollections.observableArrayList("Padat", "Cair"));
        cmbjenis.getSelectionModel().selectFirst();

        // Satuan tidak bisa diedit manual & tampil abu-abu
        txtJumlah.setEditable(false);
        txtJumlah.setStyle("-fx-background-color:#F5F5F5; -fx-opacity:1; " +
                "-fx-border-color:#E0E0E0; -fx-border-radius:6; -fx-background-radius:6; -fx-font-size:12px;");

        // Satuan otomatis mengikuti kategori limbah
        cmbjenis.valueProperty().addListener((obs, oldVal, newVal) -> updateSatuanOtomatis(newVal));
        updateSatuanOtomatis(cmbjenis.getValue());

        addNumericOnly(txtHarga);
        addAlphaOnly(txtNama);

        loadAutoID();
        loadData();

        tbLimbah.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtID.setText(newVal.getIdLimbah());
                txtNama.setText(newVal.getNamaLimbah());
                cmbjenis.setValue(newVal.getJenisLimbah());
                txtJumlah.setText(newVal.getSatuan()); // menampilkan satuan (Kilo/Liter)
                txtHarga.setText(newVal.getHarga());
                txtketerangan.setText(newVal.getKeterangan());
            }
        });
    }

    /** Menentukan satuan otomatis: Cair -> Liter, Padat -> Kilo */
    private void updateSatuanOtomatis(String kategori) {
        if (kategori == null) return;
        txtJumlah.setText(kategori.equalsIgnoreCase("Cair") ? "Liter" : "Kilo");
    }

    private void addNumericOnly(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^0-9]", "");
            if (!filtered.equals(newVal)) field.setText(filtered);
        });
    }

    private void addAlphaOnly(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = newVal.replaceAll("[^a-zA-Z ]", "");
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
                        db.result.getString("Nama_Limbah"),
                        db.result.getString("Kategori"),
                        db.result.getString("Satuan"),
                        db.result.getString("Harga"),
                        db.result.getString("Keterangan"),
                        String.valueOf(db.result.getInt("Jumlah"))
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
                        db.result.getString("Nama_Limbah"),
                        db.result.getString("Kategori"),
                        db.result.getString("Satuan"),
                        db.result.getString("Harga"),
                        db.result.getString("Keterangan"),
                        String.valueOf(db.result.getInt("Jumlah"))
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
            // sp_Update_Limbah: ID, Nama, Kategori, Jumlah, Satuan, Harga, Keterangan
            db.cstat = db.conn.prepareCall("{CALL sp_Update_Limbah(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText());
            db.cstat.setString(3, cmbjenis.getValue());
            db.cstat.setInt   (4, 0); // Jumlah default 0
            db.cstat.setString(5, txtJumlah.getText()); // Satuan otomatis
            db.cstat.setBigDecimal(6, new java.math.BigDecimal(txtHarga.getText().trim()));
            db.cstat.setString(7, txtketerangan.getText());
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
        boolean adaIsi = !txtNama.getText().trim().isEmpty()      ||
                cmbjenis.getValue() != null && !cmbjenis.getValue().isEmpty() ||
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

    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;
        try {
            // sp_Insert_Limbah: ID, Nama, Kategori, Jumlah, Satuan, Harga, Keterangan
            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Limbah(?,?,?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText().trim());
            db.cstat.setString(3, cmbjenis.getValue());
            db.cstat.setInt   (4, 0);                       // Jumlah default 0
            db.cstat.setString(5, txtJumlah.getText());      // Satuan otomatis (Liter/Kilo)
            db.cstat.setBigDecimal(6, new java.math.BigDecimal(txtHarga.getText().trim()));
            db.cstat.setString(7, txtketerangan.getText().trim().isEmpty()
                    ? null : txtketerangan.getText().trim());
            db.cstat.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data limbah berhasil disimpan.");
            clearForm();
            loadData();
            loadAutoID();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error Format", "Harga harus berupa angka valid.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    private boolean validateForm() {
        if (txtNama.getText().trim().isEmpty()    ||
                cmbjenis.getValue()              == null  ||
                txtHarga.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Semua data harus diisi!");
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtNama.clear();
        txtHarga.clear();
        txtketerangan.clear();
        cmbjenis.getSelectionModel().selectFirst();
        updateSatuanOtomatis(cmbjenis.getValue());
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