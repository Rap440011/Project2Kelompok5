package Transaksi;

import Connection.DBConnect;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import Auth.Session;

public class PenarikanSaldoController implements Initializable {

    // ── Form kiri ─────────────────────────────────────────────────────────────
    @FXML private TextField   txtIDTransaksi;
    @FXML private TextField   txtNamaNasabah;
    @FXML private DatePicker  dpTanggalPenarikan;
    @FXML private TextField   txtJumlahPenarikan;
    @FXML private Button      btnTarikSemuaSaldo;
    @FXML private TextField   txtKeterangan;
    @FXML private Button      btnBatal;
    @FXML private Button      btnSimpan;

    // ── Tabel nasabah (kanan) ────────────────────────────────────────────────
    @FXML private TableView<PenarikanSaldo>           tbNasabah;
    @FXML private TableColumn<PenarikanSaldo, String>  clmNama;
    @FXML private TableColumn<PenarikanSaldo, String>  clmNoHp;
    @FXML private TableColumn<PenarikanSaldo, String>  clmSaldo;

    // ── State ─────────────────────────────────────────────────────────────────
    private final DBConnect db = new DBConnect();
    private final ObservableList<PenarikanSaldo> dataNasabah = FXCollections.observableArrayList();
    private PenarikanSaldo nasabahDipilih = null;

    String idKaryawan = Session.getIdKaryawanLogin();
    private static final String RUPIAH_PREFIX = "Rp ";

    /**
     * Disalin dari MasterLimbahController: mengubah string digit murni menjadi
     * format dengan pemisah ribuan (titik) untuk field yang sedang diketik user.
     * Contoh: "150000" -> "150.000".
     */
    private static String formatRibuan(String digitsOnly) {
        if (digitsOnly.isEmpty()) return "";
        digitsOnly = digitsOnly.replaceFirst("^0+(?=\\d)", "");
        return digitsOnly.replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".");
    }

    /** Format BigDecimal (mis. saldo nasabah) menjadi angka dengan pemisah ribuan, mis. "150.000". */
    private static String formatRupiah(BigDecimal value) {
        if (value == null) value = BigDecimal.ZERO;
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###",
                new java.text.DecimalFormatSymbols(new java.util.Locale("in", "ID")));
        return df.format(value.setScale(0, java.math.RoundingMode.HALF_UP));
    }

    // ── Initialize ────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTabelNasabah();
        setupJumlahRupiah();
        dpTanggalPenarikan.setValue(LocalDate.now());
        loadAutoID();
        loadDataNasabah();
        setFormEnabled(false); // form baru aktif setelah nasabah dipilih
    }

    // ── Setup tabel nasabah ───────────────────────────────────────────────────
    private void setupTabelNasabah() {
        clmNama.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNamaNasabah()));
        clmNoHp.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNoHp()));
        clmSaldo.setCellValueFactory(data -> {
            BigDecimal nilai = new BigDecimal(data.getValue().getSaldo());
            return new SimpleStringProperty(formatRupiah(nilai));
        });
        tbNasabah.setItems(dataNasabah);

        // Make columns fit container width without empty space
        tbNasabah.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        tbNasabah.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) pilihNasabah(newV);
        });
    }

    // ── Format rupiah untuk field Jumlah Penarikan ───────────────────────────
    private void setupJumlahRupiah() {
        txtJumlahPenarikan.setText(RUPIAH_PREFIX);
        txtJumlahPenarikan.textProperty().addListener((obs, oldV, newV) -> {
            String angka = newV.replaceAll("[^0-9]", "");
            if (angka.length() > 15) angka = angka.substring(0, 15);
            String hasil = RUPIAH_PREFIX + formatRibuan(angka);
            if (!hasil.equals(newV)) {
                txtJumlahPenarikan.setText(hasil);
                txtJumlahPenarikan.positionCaret(hasil.length());
            }
        });
        txtJumlahPenarikan.caretPositionProperty().addListener((obs, oldP, newP) -> {
            if (newP.intValue() < RUPIAH_PREFIX.length())
                txtJumlahPenarikan.positionCaret(RUPIAH_PREFIX.length());
        });
    }

    // ── Auto ID transaksi ─────────────────────────────────────────────────────
    private void loadAutoID() {
        try {
            db.result = db.stmt.executeQuery("{CALL sp_AutoID_PenarikanSaldo}");
            if (db.result.next()) txtIDTransaksi.setText(db.result.getString("ID_PenarikanSaldo"));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Auto ID", e.getMessage());
        }
    }

    // ── Load daftar nasabah ke tabel kanan (hanya yang Status = 'Aktif') ──────
    private void loadDataNasabah() {
        dataNasabah.clear();
        try {
            db.cstat = db.conn.prepareCall("{CALL sp_SelectAll_Nasabah}");
            db.result = db.cstat.executeQuery();
            while (db.result.next()) {
                // Filter hanya nasabah dengan status aktif
                String status = db.result.getString("Status");
                if ("Aktif".equalsIgnoreCase(status)) {
                    dataNasabah.add(new PenarikanSaldo(
                            db.result.getString("ID_Nasabah"),
                            db.result.getString("Nama_Nasabah"),
                            db.result.getString("No_HP"),
                            db.result.getBigDecimal("Saldo").stripTrailingZeros().toPlainString()
                    ));
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Nasabah", e.getMessage());
        }
    }

    // ── Klik baris nasabah di tabel ───────────────────────────────────────────
    private void pilihNasabah(PenarikanSaldo n) {
        nasabahDipilih = n;
        txtNamaNasabah.setText(n.getNamaNasabah());

        boolean bisaTransaksi = new BigDecimal(n.getSaldo()).compareTo(BigDecimal.ZERO) > 0;
        setFormEnabled(bisaTransaksi);

        if (!bisaTransaksi) {
            showAlert(Alert.AlertType.WARNING, "Saldo Kosong",
                    "Nasabah \"" + n.getNamaNasabah() + "\" tidak memiliki saldo. " +
                            "Transaksi penarikan tidak bisa dilakukan.");
        }
    }

    /** Aktif/nonaktifkan bagian form yang hanya boleh diisi jika nasabah punya saldo > 0 */
    private void setFormEnabled(boolean enabled) {
        txtJumlahPenarikan.setDisable(!enabled);
        btnTarikSemuaSaldo.setDisable(!enabled);
        btnSimpan.setDisable(!enabled);
    }

    // ── Tombol: Tarik Semua Saldo ─────────────────────────────────────────────
    @FXML
    private void handleTarikSemuaSaldo() {
        if (nasabahDipilih == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih nasabah dari tabel terlebih dahulu.");
            return;
        }
        txtJumlahPenarikan.setText(RUPIAH_PREFIX + nasabahDipilih.getSaldo());
    }

    // ── Simpan transaksi ──────────────────────────────────────────────────────
    @FXML
    private void handleSimpan() {
        if (!validateForm()) return;

        try {
            BigDecimal jumlah = new BigDecimal(getJumlahRaw());

            db.cstat = db.conn.prepareCall("{CALL sp_Insert_PenarikanSaldo(?,?,?,?,?,?)}");

            db.cstat.setString(1, txtIDTransaksi.getText());
            db.cstat.setString(2, nasabahDipilih.getIdNasabah());
            db.cstat.setString(3, idKaryawan);
            db.cstat.setDate(4, java.sql.Date.valueOf(dpTanggalPenarikan.getValue()));
            db.cstat.setBigDecimal(5, jumlah);
            db.cstat.setString(6, txtKeterangan.getText().trim());

            db.cstat.executeUpdate();

            // 2) Kurangi saldo nasabah sebesar jumlah yang ditarik
            db.cstat = db.conn.prepareCall("{CALL sp_Kurangi_SaldoNasabah(?,?)}");
            db.cstat.setString(1, nasabahDipilih.getIdNasabah());
            db.cstat.setBigDecimal(2, jumlah);
            db.cstat.executeUpdate();

            showAlert(Alert.AlertType.INFORMATION, "Berhasil",
                    "Penarikan saldo berhasil disimpan.\nID Transaksi: " + txtIDTransaksi.getText());

            clearForm();
            loadDataNasabah();
            loadAutoID();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error Simpan", e.getMessage());
        }
    }

    // ── Batal ─────────────────────────────────────────────────────────────────
    @FXML
    private void handleBatal() {
        clearForm();
        loadAutoID();
    }

    // ── Validasi ─────────────────────────────────────────────────────────────
    private boolean validateForm() {
        if (nasabahDipilih == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Pilih nasabah dari tabel di kanan terlebih dahulu.");
            return false;
        }
        BigDecimal saldoNasabah = new BigDecimal(nasabahDipilih.getSaldo());
        if (saldoNasabah.compareTo(BigDecimal.ZERO) <= 0) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Nasabah ini tidak memiliki saldo.");
            return false;
        }
        if (dpTanggalPenarikan.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Tanggal Penarikan wajib diisi.");
            return false;
        }
        String rawJumlah = getJumlahRaw();
        if (rawJumlah.isEmpty() || rawJumlah.equals("0")) {
            showAlert(Alert.AlertType.WARNING, "Validasi", "Jumlah Penarikan wajib diisi.");
            return false;
        }
        BigDecimal jumlah = new BigDecimal(rawJumlah);
        if (jumlah.compareTo(saldoNasabah) > 0) {
            showAlert(Alert.AlertType.WARNING, "Validasi",
                    "Jumlah penarikan tidak boleh melebihi saldo nasabah (" +
                            RUPIAH_PREFIX + formatRupiah(saldoNasabah) + ").");
            return false;
        }
        return true;
    }

    private String getJumlahRaw() {
        return txtJumlahPenarikan.getText().replace(RUPIAH_PREFIX, "").replace(".", "").trim();
    }

    private void clearForm() {
        txtNamaNasabah.clear();
        dpTanggalPenarikan.setValue(LocalDate.now());
        txtJumlahPenarikan.setText(RUPIAH_PREFIX);
        txtKeterangan.clear();
        nasabahDipilih = null;
        tbNasabah.getSelectionModel().clearSelection();
        setFormEnabled(false);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}