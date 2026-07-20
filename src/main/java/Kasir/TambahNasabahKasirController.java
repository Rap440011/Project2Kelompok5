package Kasir;

import Connection.DBConnect;
import Master.MasterNasabah;
import Master.WilayahData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class TambahNasabahKasirController implements Initializable {

    @FXML private TextField    txtNama;
    @FXML private TextField    txtHP;
    @FXML private TextField    txtRT;
    @FXML private TextField    txtRW;
    @FXML private TextField    txtNoRek;
    @FXML private TextField    txtSaldo;
    @FXML private ComboBox<String> cmbBank;
    @FXML private ComboBox<String> cmbProvinsi;
    @FXML private ComboBox<String> cmbKabupaten;
    @FXML private ComboBox<String> cmbKecamatan;
    @FXML private ComboBox<String> cmbKelurahan;

    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;
    @FXML private Button btnUbah;
    @FXML private Button btnHapus;

    @FXML private TableView<MasterNasabah> tbNasabah;
    @FXML private TableColumn<MasterNasabah, String> clmNama;
    @FXML private TableColumn<MasterNasabah, String> clmHP;
    @FXML private TableColumn<MasterNasabah, String> clmNoRek;
    @FXML private TableColumn<MasterNasabah, String> clmSaldo;
    @FXML private TableColumn<MasterNasabah, String> clmStatus;
    @FXML private TextField txtCari;
    @FXML private Button    btnCari;

    private static final String RUPIAH_PREFIX  = "Rp ";
    private static final String DEFAULT_SALDO  = "0";
    private static final String STATUS_AKTIF   = "Aktif";
    private static final String STATUS_NONAKTIF = "Tidak Aktif";
    private static final int    MAX_NOREK_LEN  = 20;

    private static final Map<String, String> KODE_BANK = new HashMap<>();
    static {
        KODE_BANK.put("BCA",      "014");
        KODE_BANK.put("BRI",      "002");
        KODE_BANK.put("BNI",      "009");
        KODE_BANK.put("Permata",  "013");
        KODE_BANK.put("CimbNiaga","022");
        KODE_BANK.put("BSI",      "451");
    }

    private static final Map<String, Integer> MAX_DIGIT_BANK = new HashMap<>();
    static {
        MAX_DIGIT_BANK.put("BCA", 13);
        MAX_DIGIT_BANK.put("BRI", 18);
        MAX_DIGIT_BANK.put("BNI", 13);
        MAX_DIGIT_BANK.put("Permata", 13);
        MAX_DIGIT_BANK.put("CimbNiaga", 16);
        MAX_DIGIT_BANK.put("BSI", 13);
    }

    private final DBConnect db = new DBConnect();
    private String  currentKodeBank = "";
    private boolean isUpdatingNoRek = false;
    private boolean isLoadingWilayah = false;
    private boolean isLoadingFromTable = false;

    private int currentMaxNoRekLen = MAX_NOREK_LEN;

    private final ObservableList<MasterNasabah> dataList = FXCollections.observableArrayList();

    private MasterNasabah selectedNasabah = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupSaldo();
        setupBank();
        setupNoRek();
        setupWilayah();
        setupInputFilter();
        setupTableColumns();
        loadData();

        txtNama.textProperty().addListener((o, ov, nv) -> updateButtonStates());
        txtHP.textProperty().addListener((o, ov, nv) -> updateButtonStates());
        txtRT.textProperty().addListener((o, ov, nv) -> updateButtonStates());
        txtRW.textProperty().addListener((o, ov, nv) -> updateButtonStates());
        txtNoRek.textProperty().addListener((o, ov, nv) -> updateButtonStates());
        cmbBank.valueProperty().addListener((o, ov, nv) -> updateButtonStates());
        cmbProvinsi.valueProperty().addListener((o, ov, nv) -> updateButtonStates());
        cmbKabupaten.valueProperty().addListener((o, ov, nv) -> updateButtonStates());
        cmbKecamatan.valueProperty().addListener((o, ov, nv) -> updateButtonStates());
        cmbKelurahan.valueProperty().addListener((o, ov, nv) -> updateButtonStates());

        tbNasabah.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedNasabah = newV;
            if (newV != null) {
                fillFormFromSelection(newV);
            }
            updateButtonStates();
        });

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean formValid = !txtNama.getText().trim().isEmpty()
                && !txtHP.getText().trim().isEmpty()
                && !txtRT.getText().trim().isEmpty()
                && !txtRW.getText().trim().isEmpty()
                && cmbProvinsi.getValue() != null
                && cmbKabupaten.getValue() != null
                && cmbKecamatan.getValue() != null
                && cmbKelurahan.getValue() != null
                && cmbBank.getValue() != null
                && txtNoRek.getText().trim().length() > currentKodeBank.length();

        boolean rowSelected = selectedNasabah != null;

        btnSimpan.setDisable(!formValid || rowSelected);
        btnUbah.setDisable(!rowSelected);
        btnHapus.setDisable(!rowSelected);
    }

    private void setupSaldo() {
        txtSaldo.setText(RUPIAH_PREFIX + DEFAULT_SALDO);
        txtSaldo.setEditable(false);
    }

    private void setupBank() {
        cmbBank.setItems(FXCollections.observableArrayList(
                "BCA", "BRI", "BNI", "Permata", "CimbNiaga", "BSI"));
        cmbBank.getSelectionModel().selectFirst();
        generateNoRek(cmbBank.getValue());
        cmbBank.valueProperty().addListener((obs, oldV, newV) -> {
            if (isLoadingFromTable) return;
            generateNoRek(newV);
        });
    }

    private void generateNoRek(String bank) {
        if (bank == null) {
            currentKodeBank = "";
            currentMaxNoRekLen = MAX_NOREK_LEN;
            setNoRek("");
            return;
        }
        currentKodeBank = KODE_BANK.getOrDefault(bank, "000");
        currentMaxNoRekLen = MAX_DIGIT_BANK.getOrDefault(bank, MAX_NOREK_LEN);
        setNoRek(currentKodeBank);
    }

    private void setNoRek(String text) {
        isUpdatingNoRek = true;
        txtNoRek.setText(text);
        isUpdatingNoRek = false;
    }

    private void setupNoRek() {
        txtNoRek.textProperty().addListener((obs, oldV, newV) -> {
            if (isUpdatingNoRek || currentKodeBank.isEmpty()) return;
            String filtered = sanitizeNoRek(newV);
            if (!filtered.equals(newV)) {
                isUpdatingNoRek = true;
                txtNoRek.setText(filtered);
                txtNoRek.positionCaret(filtered.length());
                isUpdatingNoRek = false;
            }
        });
        txtNoRek.caretPositionProperty().addListener((obs, oldP, newP) -> {
            if (!currentKodeBank.isEmpty() && newP.intValue() < currentKodeBank.length())
                txtNoRek.positionCaret(currentKodeBank.length());
        });
    }

    private String sanitizeNoRek(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() > currentMaxNoRekLen) digits = digits.substring(0, currentMaxNoRekLen);
        if (!digits.startsWith(currentKodeBank)) {
            String sisa = digits.length() > currentKodeBank.length()
                    ? digits.substring(currentKodeBank.length()) : "";
            digits = currentKodeBank + sisa;
            if (digits.length() > currentMaxNoRekLen) digits = digits.substring(0, currentMaxNoRekLen);
        }
        return digits;
    }

    private void setupWilayah() {
        cmbProvinsi.setItems(FXCollections.observableArrayList(WilayahData.getProvinsiList()));

        cmbProvinsi.valueProperty().addListener((obs, oldV, newV) -> {
            if (isLoadingWilayah || isLoadingFromTable) return;
            cmbKabupaten.setItems(FXCollections.observableArrayList(WilayahData.getKabupatenList(newV)));
            cmbKabupaten.setValue(null);
            cmbKecamatan.setItems(FXCollections.observableArrayList());
            cmbKecamatan.setValue(null);
            cmbKelurahan.setItems(FXCollections.observableArrayList());
            cmbKelurahan.setValue(null);
        });

        cmbKabupaten.valueProperty().addListener((obs, oldV, newV) -> {
            if (isLoadingWilayah || isLoadingFromTable) return;
            cmbKecamatan.setItems(FXCollections.observableArrayList(WilayahData.getKecamatanList(newV)));
            cmbKecamatan.setValue(null);
            cmbKelurahan.setItems(FXCollections.observableArrayList());
            cmbKelurahan.setValue(null);
        });

        cmbKecamatan.valueProperty().addListener((obs, oldV, newV) -> {
            if (isLoadingWilayah || isLoadingFromTable) return;
            cmbKelurahan.setItems(FXCollections.observableArrayList(WilayahData.getKelurahanList(newV)));
            cmbKelurahan.setValue(null);
        });
    }

    private void setupInputFilter() {
        addFilter(txtNama, "[^a-zA-Z\\s.\\-]", 50);
        addFilter(txtHP, "[^0-9]", 13);
        addFilter(txtRT, "[^0-9]", 3);
        addFilter(txtRW, "[^0-9]", 3);
    }

    private void addFilter(TextField f, String blockPattern, int maxLen) {
        f.textProperty().addListener((obs, oldV, newV) -> {
            String filtered = newV.replaceAll(blockPattern, "");
            if (filtered.length() > maxLen) filtered = filtered.substring(0, maxLen);
            if (!filtered.equals(newV)) f.setText(filtered);
        });
    }

    // ── Tabel data nasabah ────────────────────────────────────────────────────
    private void setupTableColumns() {
        clmNama.setCellValueFactory(new PropertyValueFactory<>("namaNasabah"));

        // Tambahan: No. HP rata tengah
        clmHP.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        clmHP.setStyle("-fx-alignment: CENTER;");

        // Tambahan: No. Rekening rata tengah
        clmNoRek.setCellValueFactory(new PropertyValueFactory<>("noRekening"));
        clmNoRek.setStyle("-fx-alignment: CENTER;");

        // Tambahan: Saldo rata kanan + format titik ribuan (mis. 47.000)
        clmSaldo.setCellValueFactory(new PropertyValueFactory<>("saldo"));
        clmSaldo.setStyle("-fx-alignment: CENTER-RIGHT;");
        clmSaldo.setCellFactory(col -> new TableCell<MasterNasabah, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isEmpty() ? null : formatRibuan(item));
            }
        });

        // Tambahan: Status rata tengah
        clmStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        clmStatus.setStyle("-fx-alignment: CENTER;");

        tbNasabah.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // Tambahan: format angka menjadi format ribuan Indonesia (titik sebagai pemisah), tanpa prefix Rp
    private String formatRibuan(String rawNumber) {
        try {
            BigDecimal value = new BigDecimal(rawNumber);
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("in", "ID"));
            symbols.setGroupingSeparator('.');
            DecimalFormat df = new DecimalFormat("#,##0", symbols);
            return df.format(value);
        } catch (NumberFormatException e) {
            return rawNumber;
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
                rs.getBigDecimal("Saldo").stripTrailingZeros().toPlainString(),
                rs.getString("Bank"),
                rs.getString("Status")
        );
    }

    @FXML
    private void handleCari() {
        String keyword = txtCari.getText().trim();
        if (keyword.isEmpty()) loadData(); else cariData(keyword);
    }

    private void fillFormFromSelection(MasterNasabah data) {
        isLoadingFromTable = true;

        txtNama.setText(data.getNamaNasabah());
        txtHP.setText(data.getNoHp());
        txtRT.setText(data.getRt());
        txtRW.setText(data.getRw());
        txtSaldo.setText(RUPIAH_PREFIX + formatRibuan(data.getSaldo()));

        cmbBank.setValue(data.getBank());
        currentKodeBank = KODE_BANK.getOrDefault(data.getBank(), "000");
        currentMaxNoRekLen = MAX_DIGIT_BANK.getOrDefault(data.getBank(), MAX_NOREK_LEN);
        isUpdatingNoRek = true;
        txtNoRek.setText(data.getNoRekening());
        isUpdatingNoRek = false;

        cmbProvinsi.setValue(data.getProvinsi());
        cmbKabupaten.setItems(FXCollections.observableArrayList(WilayahData.getKabupatenList(data.getProvinsi())));
        cmbKabupaten.setValue(data.getKabupaten());
        cmbKecamatan.setItems(FXCollections.observableArrayList(WilayahData.getKecamatanList(data.getKabupaten())));
        cmbKecamatan.setValue(data.getKecamatan());
        cmbKelurahan.setItems(FXCollections.observableArrayList(WilayahData.getKelurahanList(data.getKecamatan())));
        cmbKelurahan.setValue(data.getKelurahan());

        isLoadingFromTable = false;
    }

    private String getAutoID() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Nasabah}");
            if (db.result.next()) return db.result.getString("ID_Nasabah");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
        return null;
    }

    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;

        String idNasabah = getAutoID();
        if (idNasabah == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal mendapatkan ID Nasabah otomatis.");
            return;
        }

        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Nasabah(?,?,?,?,?,?,?,?,?,?,?,?,?)}");
            db.cstat.setString(1, idNasabah);
            db.cstat.setString(2, txtNama.getText().trim());
            db.cstat.setString(3, txtHP.getText().trim());
            db.cstat.setString(4, txtNoRek.getText().trim());
            db.cstat.setString(5, cmbBank.getValue());
            db.cstat.setBigDecimal(6, new BigDecimal(DEFAULT_SALDO));
            db.cstat.setString(7, txtRT.getText().trim());
            db.cstat.setString(8, txtRW.getText().trim());
            db.cstat.setString(9, cmbKelurahan.getValue());
            db.cstat.setString(10, cmbKecamatan.getValue());
            db.cstat.setString(11, cmbKabupaten.getValue());
            db.cstat.setString(12, cmbProvinsi.getValue());
            db.cstat.setString(13, STATUS_AKTIF);
            db.cstat.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Berhasil",
                    "Nasabah berhasil didaftarkan!\nID Nasabah: " + idNasabah);
            clearForm();
            loadData();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    @FXML
    private void handleUbah() {
        if (selectedNasabah == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin diubah terlebih dahulu.");
            return;
        }
        if (!validateForm()) return;

        try {
            db.cstat = db.conn.prepareCall("{CALL sp_Update_Nasabah(?,?,?,?,?,?,?,?,?,?,?)}");
            db.cstat.setString(1, selectedNasabah.getIdNasabah());
            db.cstat.setString(2, txtNama.getText().trim());
            db.cstat.setString(3, txtHP.getText().trim());
            db.cstat.setString(4, txtNoRek.getText().trim());
            db.cstat.setString(5, cmbBank.getValue());
            db.cstat.setString(6, txtRT.getText().trim());
            db.cstat.setString(7, txtRW.getText().trim());
            db.cstat.setString(8, cmbKelurahan.getValue());
            db.cstat.setString(9, cmbKecamatan.getValue());
            db.cstat.setString(10, cmbKabupaten.getValue());
            db.cstat.setString(11, cmbProvinsi.getValue());
            db.cstat.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data nasabah berhasil diubah.");
            clearForm();
            loadData();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Ubah", e.getMessage());
        }
    }

    @FXML
    private void handleHapus() {
        if (selectedNasabah == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin dinonaktifkan terlebih dahulu.");
            return;
        }
        if (STATUS_NONAKTIF.equalsIgnoreCase(selectedNasabah.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Nasabah ini sudah berstatus Tidak Aktif.");
            return;
        }

        Alert k = new Alert(Alert.AlertType.CONFIRMATION,
                "Nasabah ID: " + selectedNasabah.getIdNasabah() + " akan dinonaktifkan.\n" +
                        "Data tidak akan dihapus, status berubah menjadi 'Tidak Aktif'.\n\nLanjutkan?",
                ButtonType.YES, ButtonType.NO);
        k.setTitle("Konfirmasi Nonaktifkan");
        k.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) softDeleteNasabah();
        });
    }

    private void softDeleteNasabah() {
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SoftDelete_Nasabah(?)}");
            db.cstat.setString(1, selectedNasabah.getIdNasabah());
            db.cstat.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Nasabah berhasil dinonaktifkan.");
            clearForm();
            loadData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Nonaktifkan", e.getMessage());
        }
    }

    @FXML
    private void handleBatal() {
        boolean adaIsi = !txtNama.getText().trim().isEmpty()
                || !txtHP.getText().trim().isEmpty()
                || !txtRT.getText().trim().isEmpty()
                || !txtRW.getText().trim().isEmpty()
                || cmbProvinsi.getValue() != null
                || selectedNasabah != null;

        if (!adaIsi) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Form sudah kosong.");
            return;
        }

        Alert k = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin mengosongkan form?", ButtonType.YES, ButtonType.NO);
        k.setTitle("Konfirmasi Batal");
        k.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) clearForm(); });
    }

    private boolean validateForm() {
        if (txtNama.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Nama nasabah wajib diisi."); return false;
        }
        if (txtHP.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "No. HP wajib diisi."); return false;
        }
        if (txtHP.getText().trim().length() < 9) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "No. HP tidak valid (minimal 9 digit)."); return false;
        }
        if (cmbBank.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Bank wajib dipilih."); return false;
        }
        if (txtNoRek.getText().trim().length() <= currentKodeBank.length()) {
            showAlert(Alert.AlertType.WARNING, "Validasi",
                    "No. Rekening belum dilengkapi setelah kode bank (" + currentKodeBank + ")."); return false;
        }
        if (txtNoRek.getText().trim().length() > currentMaxNoRekLen) {
            showAlert(Alert.AlertType.WARNING, "Validasi",
                    "No. Rekening melebihi batas maksimal " + currentMaxNoRekLen + " digit untuk bank ini."); return false;
        }
        if (cmbProvinsi.getValue() == null || cmbKabupaten.getValue() == null
                || cmbKecamatan.getValue() == null || cmbKelurahan.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi",
                    "Provinsi, Kabupaten, Kecamatan, dan Kelurahan wajib dipilih."); return false;
        }
        if (txtRT.getText().trim().isEmpty() || txtRW.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "RT dan RW wajib diisi."); return false;
        }

        String idDikecualikan = selectedNasabah != null ? selectedNasabah.getIdNasabah() : null;

        if (noHpSudahAda(txtHP.getText().trim(), idDikecualikan)) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "No. HP sudah digunakan oleh nasabah lain."); return false;
        }
        if (noRekSudahAda(txtNoRek.getText().trim(), idDikecualikan)) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "No. Rekening sudah digunakan oleh nasabah lain."); return false;
        }
        return true;
    }

    private boolean noHpSudahAda(String noHp, String idDikecualikan) {
        for (MasterNasabah n : dataList) {
            if (idDikecualikan != null && n.getIdNasabah().equals(idDikecualikan)) continue;
            if (n.getNoHp() != null && n.getNoHp().trim().equals(noHp)) return true;
        }
        return false;
    }

    private boolean noRekSudahAda(String noRek, String idDikecualikan) {
        for (MasterNasabah n : dataList) {
            if (idDikecualikan != null && n.getIdNasabah().equals(idDikecualikan)) continue;
            if (n.getNoRekening() != null && n.getNoRekening().trim().equals(noRek)) return true;
        }
        return false;
    }

    private void clearForm() {
        txtNama.clear();
        txtHP.clear();
        txtRT.clear();
        txtRW.clear();
        txtSaldo.setText(RUPIAH_PREFIX + DEFAULT_SALDO);
        cmbBank.getSelectionModel().selectFirst();
        generateNoRek(cmbBank.getValue());

        isLoadingWilayah = true;
        cmbProvinsi.setValue(null);
        cmbKabupaten.setValue(null);  cmbKabupaten.setItems(FXCollections.observableArrayList());
        cmbKecamatan.setValue(null);  cmbKecamatan.setItems(FXCollections.observableArrayList());
        cmbKelurahan.setValue(null);  cmbKelurahan.setItems(FXCollections.observableArrayList());
        isLoadingWilayah = false;

        selectedNasabah = null;
        tbNasabah.getSelectionModel().clearSelection();
        updateButtonStates();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}