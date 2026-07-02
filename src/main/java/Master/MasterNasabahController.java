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

    // ===================== FXML COMPONENTS =====================
    @FXML private TableView<MasterNasabah> tbNasabah;
    @FXML private TableColumn<MasterNasabah, String> clmNama, clmHP;
    @FXML private TableColumn<MasterNasabah, String> clmNoRek, clmSaldo;

    @FXML private TextField txtID, txtNama, txtHP;
    @FXML private TextField txtRT, txtRW;
    @FXML private TextField txtNoRek, txtSaldo;
    @FXML private TextField txtCari;
    @FXML private ComboBox<String> cmbBank;
    @FXML private ComboBox<String> cmbProvinsi, cmbKabupaten, cmbKecamatan, cmbKelurahan;

    // ===================== STATE & DEPENDENCIES =====================
    private final ObservableList<MasterNasabah> dataList = FXCollections.observableArrayList();
    private final DBConnect db = new DBConnect();

    private static final String DEFAULT_SALDO = "0";
    private static final int MAX_NOREK_LEN = 20;

    private boolean isLoadingFromTable = false;
    private boolean isUpdatingNoRek = false;
    private String currentKodeBank = "";

    private static final String RUPIAH_PREFIX = "Rp";

    private static final Map<String, String> KODE_BANK = new HashMap<>();
    static {
        KODE_BANK.put("BCA", "014");
        KODE_BANK.put("BRI", "002");
        KODE_BANK.put("BNI", "009");
        KODE_BANK.put("Permata", "013");
        KODE_BANK.put("CimbNiaga", "022");
        KODE_BANK.put("BSI", "451");
    }

    // ===================== INITIALIZE =====================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupInputRestrictions();
        setupBankCombobox();
        setupNoRekening();
        setupComboboxWilayah();
        setupTableSelectionListener();

        loadAutoID();
        loadData();
        generateNoRekening(cmbBank.getValue());
    }

    private void setupTableColumns() {
        clmNama.setCellValueFactory(new PropertyValueFactory<>("namaNasabah"));
        clmHP.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        clmNoRek.setCellValueFactory(new PropertyValueFactory<>("noRekening"));
        clmSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));
    }

    private void setupInputRestrictions() {
        addNumericOnly(txtHP, 13);
        addNumericOnly(txtRT, 3);
        addNumericOnly(txtRW, 3);
        setupSaldoRupiah();
        addLetterOnly(txtNama, 50);
        txtSaldo.setText("Rp"+DEFAULT_SALDO);
    }

    private void setupBankCombobox() {
        cmbBank.setItems(FXCollections.observableArrayList(
                "BCA", "BRI", "BNI", "Permata", "CimbNiaga", "BSI"));
        cmbBank.getSelectionModel().selectFirst();

        cmbBank.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isLoadingFromTable) return;
            generateNoRekening(newVal);
        });
    }

    private void setupNoRekening() {

        txtNoRek.setEditable(true);
        addNoRekLockedPrefix();
    }

    private void setupTableSelectionListener() {
        tbNasabah.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            fillFormFromSelection(newVal);
        });
    }

    private void fillFormFromSelection(MasterNasabah data) {
        isLoadingFromTable = true;

        txtID.setText(data.getIdNasabah());
        txtNama.setText(data.getNamaNasabah());
        txtHP.setText(data.getNoHp());
        txtRT.setText(data.getRt());
        txtRW.setText(data.getRw());
        txtSaldo.setText(RUPIAH_PREFIX + sanitizeRupiahInput(data.getSaldo(), 18).replace(RUPIAH_PREFIX, ""));
        cmbBank.setValue(data.getBank());

        currentKodeBank = KODE_BANK.getOrDefault(data.getBank(), "000");
        isUpdatingNoRek = true;
        txtNoRek.setText(data.getNoRekening());
        isUpdatingNoRek = false;

        cmbProvinsi.setValue(data.getProvinsi());
        cmbKabupaten.setItems(FXCollections.observableArrayList(
                WilayahData.getKabupatenList(data.getProvinsi())));
        cmbKabupaten.setValue(data.getKabupaten());

        cmbKecamatan.setItems(FXCollections.observableArrayList(
                WilayahData.getKecamatanList(data.getKabupaten())));
        cmbKecamatan.setValue(data.getKecamatan());

        cmbKelurahan.setItems(FXCollections.observableArrayList(
                WilayahData.getKelurahanList(data.getKecamatan())));
        cmbKelurahan.setValue(data.getKelurahan());

        isLoadingFromTable = false;
    }

    private void generateNoRekening(String bank) {
        if (bank == null || bank.isEmpty()) {
            currentKodeBank = "";
            setNoRekText("");
            return;
        }

        currentKodeBank = KODE_BANK.getOrDefault(bank, "000");

        setNoRekText(currentKodeBank);
    }

    private void setNoRekText(String text) {
        isUpdatingNoRek = true;
        txtNoRek.setText(text);
        isUpdatingNoRek = false;
    }

    private void addNoRekLockedPrefix() {
        txtNoRek.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingNoRek || currentKodeBank.isEmpty()) return;

            String filtered = sanitizeNoRekening(newVal);
            if (!filtered.equals(newVal)) {
                isUpdatingNoRek = true;
                txtNoRek.setText(filtered);
                txtNoRek.positionCaret(filtered.length());
                isUpdatingNoRek = false;
            }
        });

        txtNoRek.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            if (!currentKodeBank.isEmpty() && newPos.intValue() < currentKodeBank.length()) {
                txtNoRek.positionCaret(currentKodeBank.length());
            }
        });
    }

    private String sanitizeNoRekening(String rawInput) {
        String onlyDigits = rawInput.replaceAll("[^0-9]", "");
        if (onlyDigits.length() > MAX_NOREK_LEN) {
            onlyDigits = onlyDigits.substring(0, MAX_NOREK_LEN);
        }

        if (!onlyDigits.startsWith(currentKodeBank)) {
            String sisaAngka = onlyDigits.length() > currentKodeBank.length()
                    ? onlyDigits.substring(currentKodeBank.length())
                    : "";
            onlyDigits = currentKodeBank + sisaAngka;
            if (onlyDigits.length() > MAX_NOREK_LEN) {
                onlyDigits = onlyDigits.substring(0, MAX_NOREK_LEN);
            }
        }

        return onlyDigits;
    }

    // ===================== SALDO (RUPIAH FORMAT) =====================
    private void setupSaldoRupiah() {
        txtSaldo.setText(RUPIAH_PREFIX + DEFAULT_SALDO);
        addRupiahLockedPrefix(txtSaldo, 18);
    }

    private void addRupiahLockedPrefix(TextField field, int maxDigitLen) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtered = sanitizeRupiahInput(newVal, maxDigitLen);
            if (!filtered.equals(newVal)) {
                field.setText(filtered);
                field.positionCaret(filtered.length());
            }
        });

        field.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            if (newPos.intValue() < RUPIAH_PREFIX.length()) {
                field.positionCaret(RUPIAH_PREFIX.length());
            }
        });
    }

    private String sanitizeRupiahInput(String rawInput, int maxDigitLen) {
        String onlyDigits = rawInput.replaceAll("[^0-9]", "");
        if (onlyDigits.length() > maxDigitLen) {
            onlyDigits = onlyDigits.substring(0, maxDigitLen);
        }
        return RUPIAH_PREFIX + onlyDigits;
    }

    private String getSaldoRawValue() {
        String raw = txtSaldo.getText().replace(RUPIAH_PREFIX, "").trim();
        return raw.isEmpty() ? DEFAULT_SALDO : raw;
    }

    // ===================== WILAYAH (PROVINSI/KAB/KEC/KEL) =====================
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

    // ===================== INPUT FILTER HELPERS =====================
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

    // ===================== DATA LOADING =====================
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
                dataList.add(mapResultSetToNasabah(db.result));
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
                dataList.add(mapResultSetToNasabah(db.result));
            }
            tbNasabah.setItems(dataList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Cari Data", e.getMessage());
        }
    }

    private MasterNasabah mapResultSetToNasabah(ResultSet rs) throws SQLException {
        return new MasterNasabah(
                rs.getString("ID_Nasabah"),
                rs.getString("Nama_Nasabah"),
                rs.getString("No_HP"),
                rs.getString("RT"),
                rs.getString("RW"),
                rs.getString("Kelurahan"),
                rs.getString("Kecamatan"),
                rs.getString("Kabupaten"),
                rs.getString("Provinsi"),
                rs.getString("No_Rekening"),
                RUPIAH_PREFIX + rs.getString("Saldo"),
                rs.getString("Bank")
        );
    }

    // ===================== EVENT HANDLERS (CRUD) =====================
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
            String saldoText = getSaldoRawValue();

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Nasabah(?,?,?,?,?,?,?,?,?,?,?,?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText());
            db.cstat.setString(3, txtHP.getText());
            db.cstat.setString(4, txtNoRek.getText());
            db.cstat.setString(5, cmbBank.getValue());
            db.cstat.setBigDecimal(6, new BigDecimal(saldoText));
            db.cstat.setString(7, txtRT.getText());
            db.cstat.setString(8, txtRW.getText());
            db.cstat.setString(9, cmbKelurahan.getValue());
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
            db.cstat.setString(1, txtID.getText());
            db.cstat.setString(2, txtNama.getText());
            db.cstat.setString(3, txtHP.getText());
            db.cstat.setString(4, txtNoRek.getText());
            db.cstat.setString(5, cmbBank.getValue());
            db.cstat.setString(6, txtRT.getText());
            db.cstat.setString(7, txtRW.getText());
            db.cstat.setString(8, cmbKelurahan.getValue());
            db.cstat.setString(9, cmbKecamatan.getValue());
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
                deleteNasabah();
            }
        });
    }

    private void deleteNasabah() {
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

    @FXML
    private void handleBatal() {
        if (!formHasContent()) {
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

    private boolean formHasContent() {
        return !txtNama.getText().trim().isEmpty()
                || !txtHP.getText().trim().isEmpty()
                || !txtRT.getText().trim().isEmpty()
                || !txtRW.getText().trim().isEmpty()
                || cmbKelurahan.getValue() != null
                || cmbKecamatan.getValue() != null
                || cmbKabupaten.getValue() != null
                || cmbProvinsi.getValue() != null;
    }

    // ===================== VALIDATION & CLEAR =====================
    private boolean validateForm() {
        boolean isInvalid = txtNama.getText().trim().isEmpty()
                || txtHP.getText().trim().isEmpty()
                || txtRT.getText().trim().isEmpty()
                || txtRW.getText().trim().isEmpty()
                || cmbKelurahan.getValue() == null
                || cmbKecamatan.getValue() == null
                || cmbKabupaten.getValue() == null
                || cmbProvinsi.getValue() == null
                || txtNoRek.getText().trim().isEmpty()
                || cmbBank.getValue() == null;

        if (isInvalid) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Semua data harus diisi!");
            return false;
        }
        return true;
    }

    private void clearForm() {
        txtNama.clear();
        txtHP.clear();
        txtRT.clear();
        txtRW.clear();
        txtSaldo.setText(RUPIAH_PREFIX + DEFAULT_SALDO);

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

    // ===================== UTIL =====================
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}