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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class MasterNasabahController implements Initializable {

    // ===================== FXML COMPONENTS =====================
    @FXML private TableView<MasterNasabah> tbNasabah;
    @FXML private TableColumn<MasterNasabah, String> clmNama, clmHP;
    @FXML private TableColumn<MasterNasabah, String> clmNoRek, clmSaldo;
    @FXML private TableColumn<MasterNasabah, String> clmStatus;

    @FXML private TextField txtID, txtNama, txtHP;
    @FXML private TextField txtRT, txtRW;
    @FXML private TextField txtNoRek, txtSaldo;
    @FXML private TextField txtCari;
    @FXML private TextField txtStatus;
    @FXML private ComboBox<String> cmbBank;
    @FXML private ComboBox<String> cmbProvinsi, cmbKabupaten, cmbKecamatan, cmbKelurahan;

    // Tambahan: referensi tombol untuk kontrol disable/enable
    @FXML private Button btnUbah, btnHapus, btnSimpan;

    // ===================== STATE & DEPENDENCIES =====================
    private final ObservableList<MasterNasabah> dataList = FXCollections.observableArrayList();
    private final DBConnect db = new DBConnect();

    private static final String DEFAULT_SALDO = "0";
    private static final int MAX_NOREK_LEN = 20;

    private static final String STATUS_AKTIF = "Aktif";
    private static final String STATUS_NONAKTIF = "Tidak Aktif";

    private boolean isLoadingFromTable = false;
    private boolean isUpdatingNoRek = false;
    private String currentKodeBank = "";

    // Tambahan: menyimpan batas maksimal digit No. Rekening sesuai bank yang aktif
    private int currentMaxNoRekLen = MAX_NOREK_LEN;

    private static final String RUPIAH_PREFIX = "Rp ";

    private static final Map<String, String> KODE_BANK = new HashMap<>();
    static {
        KODE_BANK.put("BCA", "014");
        KODE_BANK.put("BRI", "002");
        KODE_BANK.put("BNI", "009");
        KODE_BANK.put("Permata", "013");
        KODE_BANK.put("CimbNiaga", "022");
        KODE_BANK.put("BSI", "451");
    }

    // Tambahan: batas maksimal digit No. Rekening untuk masing-masing bank
    private static final Map<String, Integer> MAX_DIGIT_BANK = new HashMap<>();
    static {
        MAX_DIGIT_BANK.put("BCA", 13);
        MAX_DIGIT_BANK.put("BRI", 18);
        MAX_DIGIT_BANK.put("BNI", 13);
        MAX_DIGIT_BANK.put("Permata", 13);
        MAX_DIGIT_BANK.put("CimbNiaga", 16);
        MAX_DIGIT_BANK.put("BSI", 13);
    }

    // ===================== INITIALIZE =====================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tbNasabah.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupTableColumns();
        setupInputRestrictions();
        setupBankCombobox();
        setupNoRekening();
        setupComboboxWilayah();
        setupTableSelectionListener();

        loadAutoID();
        loadData();
        generateNoRekening(cmbBank.getValue());

        // Tambahan: listener untuk memantau perubahan input form agar tombol Simpan bereaksi otomatis
        txtNama.textProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        txtHP.textProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        txtRT.textProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        txtRW.textProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        txtNoRek.textProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        cmbBank.valueProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        cmbProvinsi.valueProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        cmbKabupaten.valueProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        cmbKecamatan.valueProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());
        cmbKelurahan.valueProperty().addListener((obs, oldVal, newVal) -> updateButtonStates());

        // Tambahan: set status awal tombol saat form pertama kali dibuka
        updateButtonStates();
    }

    /**
     * Tambahan: Mengatur status aktif/nonaktif tombol Simpan, Ubah, dan Hapus.
     * - Simpan  : aktif jika semua field wajib terisi, DAN tidak ada baris tabel yang sedang dipilih.
     * - Ubah    : aktif jika ada baris tabel yang sedang dipilih.
     * - Hapus   : aktif jika ada baris tabel yang sedang dipilih.
     * - Batal   : tidak diatur di sini, tetap selalu aktif seperti semula.
     */
    private void updateButtonStates() {
        boolean formValid = !txtNama.getText().trim().isEmpty()
                && !txtHP.getText().trim().isEmpty()
                && !txtRT.getText().trim().isEmpty()
                && !txtRW.getText().trim().isEmpty()
                && cmbKelurahan.getValue() != null
                && cmbKecamatan.getValue() != null
                && cmbKabupaten.getValue() != null
                && cmbProvinsi.getValue() != null
                && !txtNoRek.getText().trim().isEmpty()
                && cmbBank.getValue() != null;

        boolean rowSelected = tbNasabah.getSelectionModel().getSelectedItem() != null;

        btnSimpan.setDisable(!formValid || rowSelected);
        btnUbah.setDisable(!rowSelected);
        btnHapus.setDisable(!rowSelected);
    }

    // ── Tabel data nasabah ────────────────────────────────────────────────────
    private void setupTableColumns() {
        // Nama rata kiri (default)
        clmNama.setCellValueFactory(new PropertyValueFactory<>("namaNasabah"));
        clmNama.setStyle("-fx-alignment: CENTER-LEFT;");

        // Tambahan: No. HP rata tengah
        clmHP.setCellValueFactory(new PropertyValueFactory<>("noHp"));
        clmHP.setStyle("-fx-alignment: CENTER;");

        // Tambahan: No. Rekening rata tengah
        clmNoRek.setCellValueFactory(new PropertyValueFactory<>("noRekening"));
        clmNoRek.setStyle("-fx-alignment: CENTER;");

        // Tambahan: Saldo rata kanan + format titik ribuan tanpa prefix Rp (mis. 47.000)
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
    }

    // Tambahan: format angka mentah/berprefix menjadi format ribuan Indonesia (titik), tanpa prefix Rp
    private String formatRibuan(String rawValue) {
        try {
            String onlyDigits = rawValue.replaceAll("[^0-9]", "");
            if (onlyDigits.isEmpty()) return "0";
            BigDecimal value = new BigDecimal(onlyDigits);
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("in", "ID"));
            symbols.setGroupingSeparator('.');
            DecimalFormat df = new DecimalFormat("#,##0", symbols);
            return df.format(value);
        } catch (NumberFormatException e) {
            return rawValue;
        }
    }

    private void setupInputRestrictions() {
        addNumericOnly(txtHP, 13);
        addNumericOnly(txtRT, 3);
        addNumericOnly(txtRW, 3);
        setupSaldoRupiah();
        addLetterOnly(txtNama, 50);
        txtSaldo.setText("Rp"+DEFAULT_SALDO);

        // Status default "Aktif", terkunci total (tidak bisa diedit / difokus / diklik-ubah)
        txtStatus.setText(STATUS_AKTIF);
        txtStatus.setEditable(false);
        txtStatus.setFocusTraversable(false);
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
            updateButtonStates(); // Tambahan: update status tombol setiap seleksi tabel berubah
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
        txtStatus.setText(data.getStatus());

        currentKodeBank = KODE_BANK.getOrDefault(data.getBank(), "000");
        // Tambahan: sinkronkan batas maksimal digit No. Rekening sesuai bank nasabah yang dipilih
        currentMaxNoRekLen = MAX_DIGIT_BANK.getOrDefault(data.getBank(), MAX_NOREK_LEN);
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
            currentMaxNoRekLen = MAX_NOREK_LEN;
            setNoRekText("");
            return;
        }

        currentKodeBank = KODE_BANK.getOrDefault(bank, "000");
        // Tambahan: sinkronkan batas maksimal digit No. Rekening sesuai bank yang dipilih
        currentMaxNoRekLen = MAX_DIGIT_BANK.getOrDefault(bank, MAX_NOREK_LEN);

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
        if (onlyDigits.length() > currentMaxNoRekLen) {
            onlyDigits = onlyDigits.substring(0, currentMaxNoRekLen);
        }

        if (!onlyDigits.startsWith(currentKodeBank)) {
            String sisaAngka = onlyDigits.length() > currentKodeBank.length()
                    ? onlyDigits.substring(currentKodeBank.length())
                    : "";
            onlyDigits = currentKodeBank + sisaAngka;
            if (onlyDigits.length() > currentMaxNoRekLen) {
                onlyDigits = onlyDigits.substring(0, currentMaxNoRekLen);
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
                RUPIAH_PREFIX + rs.getBigDecimal("Saldo").stripTrailingZeros().toPlainString(),
                rs.getString("Bank"),
                rs.getString("Status")
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

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_Nasabah(?,?,?,?,?,?,?,?,?,?,?,?,?)}");
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
            db.cstat.setString(13, STATUS_AKTIF);
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
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin dinonaktifkan terlebih dahulu.");
            return;
        }

        if (STATUS_NONAKTIF.equalsIgnoreCase(txtStatus.getText())) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Nasabah ini sudah berstatus Tidak Aktif.");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Nasabah ID: " + txtID.getText() + " akan dinonaktifkan.\n" +
                        "Data tidak akan dihapus, status berubah menjadi 'Tidak Aktif'.\n\nLanjutkan?",
                ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Nonaktifkan");
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                softDeleteNasabah();
            }
        });
    }

    private void softDeleteNasabah() {
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SoftDelete_Nasabah(?)}");
            db.cstat.setString(1, txtID.getText());
            db.cstat.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Nasabah berhasil dinonaktifkan.");
            clearForm();
            loadData();
            loadAutoID();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Nonaktifkan", e.getMessage());
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

        // Tambahan: tentukan apakah sedang mode Ubah (ada baris tabel yang dipilih)
        // atau mode Tambah baru. Pada mode Ubah, data milik nasabah yang sedang
        // diedit sendiri dikecualikan dari pengecekan duplikat.
        MasterNasabah nasabahDipilih = tbNasabah.getSelectionModel().getSelectedItem();
        String idDikecualikan = nasabahDipilih != null ? nasabahDipilih.getIdNasabah() : null;

        // Tambahan: validasi No. HP tidak boleh sama dengan nasabah lain
        if (noHpSudahAda(txtHP.getText(), idDikecualikan)) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "No. HP sudah digunakan oleh nasabah lain.");
            return false;
        }

        // Tambahan: validasi No. Rekening tidak boleh sama dengan nasabah lain
        if (noRekSudahAda(txtNoRek.getText(), idDikecualikan)) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "No. Rekening sudah digunakan oleh nasabah lain.");
            return false;
        }

        return true;
    }

    /**
     * Tambahan: mengecek apakah No. HP sudah dipakai nasabah lain.
     * @param noHp No. HP yang akan disimpan/diubah
     * @param idDikecualikan ID nasabah yang sedang diedit (diabaikan dari pengecekan), null jika mode tambah baru
     */
    private boolean noHpSudahAda(String noHp, String idDikecualikan) {
        String noHpBaru = noHp.trim();
        for (MasterNasabah n : dataList) {
            if (idDikecualikan != null && n.getIdNasabah().equals(idDikecualikan)) {
                continue; // lewati data nasabah yang sedang diedit
            }
            if (n.getNoHp() != null && n.getNoHp().trim().equals(noHpBaru)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tambahan: mengecek apakah No. Rekening sudah dipakai nasabah lain.
     * @param noRek No. Rekening yang akan disimpan/diubah
     * @param idDikecualikan ID nasabah yang sedang diedit (diabaikan dari pengecekan), null jika mode tambah baru
     */
    private boolean noRekSudahAda(String noRek, String idDikecualikan) {
        String noRekBaru = noRek.trim();
        for (MasterNasabah n : dataList) {
            if (idDikecualikan != null && n.getIdNasabah().equals(idDikecualikan)) {
                continue; // lewati data nasabah yang sedang diedit
            }
            if (n.getNoRekening() != null && n.getNoRekening().trim().equals(noRekBaru)) {
                return true;
            }
        }
        return false;
    }

    private void clearForm() {
        txtNama.clear();
        txtHP.clear();
        txtRT.clear();
        txtRW.clear();
        txtSaldo.setText(RUPIAH_PREFIX + DEFAULT_SALDO);
        txtStatus.setText(STATUS_AKTIF);

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