package Kasir;

import Connection.DBConnect;
import Master.WilayahData;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class TambahNasabahKasirController implements Initializable {

    // ── FXML — form (TANPA txtID, TANPA btnUbah, TANPA btnHapus) ─────────────
    @FXML private TextField    txtNama;
    @FXML private TextField    txtHP;
    @FXML private TextField    txtRT;
    @FXML private TextField    txtRW;
    @FXML private TextField    txtNoRek;
    @FXML private TextField    txtSaldo;    // read-only, selalu Rp 0
    @FXML private ComboBox<String> cmbBank;
    @FXML private ComboBox<String> cmbProvinsi;
    @FXML private ComboBox<String> cmbKabupaten;
    @FXML private ComboBox<String> cmbKecamatan;
    @FXML private ComboBox<String> cmbKelurahan;

    @FXML private Button btnSimpan;
    @FXML private Button btnBatal;

    // ── Konstanta ─────────────────────────────────────────────────────────────
    private static final String RUPIAH_PREFIX  = "Rp ";
    private static final String DEFAULT_SALDO  = "0";
    private static final String STATUS_AKTIF   = "Aktif";
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

    // ── State ─────────────────────────────────────────────────────────────────
    private final DBConnect db = new DBConnect();
    private String  currentKodeBank = "";
    private boolean isUpdatingNoRek = false;
    private boolean isLoadingWilayah = false;

    // ── Initialize ────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupSaldo();
        setupBank();
        setupNoRek();
        setupWilayah();
        setupInputFilter();
    }

    // ── Setup saldo (selalu Rp 0, tidak bisa diedit) ─────────────────────────
    private void setupSaldo() {
        txtSaldo.setText(RUPIAH_PREFIX + DEFAULT_SALDO);
        txtSaldo.setEditable(false);
    }

    // ── Setup bank combo + auto kode rekening ─────────────────────────────────
    private void setupBank() {
        cmbBank.setItems(FXCollections.observableArrayList(
                "BCA", "BRI", "BNI", "Permata", "CimbNiaga", "BSI"));
        cmbBank.getSelectionModel().selectFirst();
        generateNoRek(cmbBank.getValue());

        cmbBank.valueProperty().addListener((obs, oldV, newV) -> generateNoRek(newV));
    }

    private void generateNoRek(String bank) {
        if (bank == null) { currentKodeBank = ""; setNoRek(""); return; }
        currentKodeBank = KODE_BANK.getOrDefault(bank, "000");
        setNoRek(currentKodeBank);
    }

    private void setNoRek(String text) {
        isUpdatingNoRek = true;
        txtNoRek.setText(text);
        isUpdatingNoRek = false;
    }

    // ── Setup no rekening: prefix kode bank terkunci, sisanya bebas diketik ──
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
        if (digits.length() > MAX_NOREK_LEN) digits = digits.substring(0, MAX_NOREK_LEN);
        if (!digits.startsWith(currentKodeBank)) {
            String sisa = digits.length() > currentKodeBank.length()
                    ? digits.substring(currentKodeBank.length()) : "";
            digits = currentKodeBank + sisa;
            if (digits.length() > MAX_NOREK_LEN) digits = digits.substring(0, MAX_NOREK_LEN);
        }
        return digits;
    }

    // ── Setup wilayah (cascade) ───────────────────────────────────────────────
    private void setupWilayah() {
        cmbProvinsi.setItems(FXCollections.observableArrayList(WilayahData.getProvinsiList()));

        cmbProvinsi.valueProperty().addListener((obs, oldV, newV) -> {
            if (isLoadingWilayah) return;
            cmbKabupaten.setItems(FXCollections.observableArrayList(WilayahData.getKabupatenList(newV)));
            cmbKabupaten.setValue(null);
            cmbKecamatan.setItems(FXCollections.observableArrayList());
            cmbKecamatan.setValue(null);
            cmbKelurahan.setItems(FXCollections.observableArrayList());
            cmbKelurahan.setValue(null);
        });

        cmbKabupaten.valueProperty().addListener((obs, oldV, newV) -> {
            if (isLoadingWilayah) return;
            cmbKecamatan.setItems(FXCollections.observableArrayList(WilayahData.getKecamatanList(newV)));
            cmbKecamatan.setValue(null);
            cmbKelurahan.setItems(FXCollections.observableArrayList());
            cmbKelurahan.setValue(null);
        });

        cmbKecamatan.valueProperty().addListener((obs, oldV, newV) -> {
            if (isLoadingWilayah) return;
            cmbKelurahan.setItems(FXCollections.observableArrayList(WilayahData.getKelurahanList(newV)));
            cmbKelurahan.setValue(null);
        });
    }

    // ── Filter input ──────────────────────────────────────────────────────────
    private void setupInputFilter() {
        // Nama: hanya huruf & spasi, max 50
        addFilter(txtNama, "[^a-zA-Z\\s.\\-]", 50);
        // HP: hanya angka, max 13
        addFilter(txtHP, "[^0-9]", 13);
        // RT, RW: hanya angka, max 3
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

    // ── Auto ID dari DB (dipanggil saat akan simpan, bukan saat form dibuka) ─
    private String getAutoID() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_Nasabah}");
            if (db.result.next()) return db.result.getString("ID_Nasabah");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
        return null;
    }

    // ── Simpan ────────────────────────────────────────────────────────────────
    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;

        // Ambil ID otomatis tepat sebelum insert
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
            db.cstat.setBigDecimal(6, new BigDecimal(DEFAULT_SALDO)); // saldo selalu 0
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

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    // ── Batal ─────────────────────────────────────────────────────────────────
    @FXML
    private void handleBatal() {
        boolean adaIsi = !txtNama.getText().trim().isEmpty()
                || !txtHP.getText().trim().isEmpty()
                || !txtRT.getText().trim().isEmpty()
                || !txtRW.getText().trim().isEmpty()
                || cmbProvinsi.getValue() != null;

        if (!adaIsi) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Form sudah kosong.");
            return;
        }

        Alert k = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin mengosongkan form?", ButtonType.YES, ButtonType.NO);
        k.setTitle("Konfirmasi Batal");
        k.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) clearForm(); });
    }

    // ── Validasi ─────────────────────────────────────────────────────────────
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
        if (cmbProvinsi.getValue() == null || cmbKabupaten.getValue() == null
                || cmbKecamatan.getValue() == null || cmbKelurahan.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi",
                    "Provinsi, Kabupaten, Kecamatan, dan Kelurahan wajib dipilih."); return false;
        }
        if (txtRT.getText().trim().isEmpty() || txtRW.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "RT dan RW wajib diisi."); return false;
        }
        return true;
    }

    // ── Clear form ────────────────────────────────────────────────────────────
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
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
