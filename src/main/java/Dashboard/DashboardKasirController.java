package Dashboard;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardKasirController implements Initializable {

    @FXML private StackPane contentArea;

    @FXML private Button btnTambahNasabah;
    @FXML private Button btnSetorLimbah;
    @FXML private Button btnPengolahanLimbah;
    @FXML private Button btnPenjualan;
    @FXML private Button btnPenarikanSaldo;   // ← baru
    @FXML private Button btnLogout;

    private Button activeButton = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showWelcome();
    }

    // ── Handler menu ─────────────────────────────────────────────────────────

    /** Tambah Nasabah — form khusus kasir (hanya Simpan, tanpa ID tampil, tanpa Ubah/Hapus) */
    @FXML
    private void handleTambahNasabah() {
        setActive(btnTambahNasabah);
        loadPage("/kasir/TambahNasabahKasir.fxml");
    }

    @FXML
    private void handleSetorLimbah() {
        setActive(btnSetorLimbah);
        loadPage("/Transaksi/TransaksiSetorLimbah.fxml");
    }

    @FXML
    private void handlePengolahanLimbah() {
        setActive(btnPengolahanLimbah);
        loadPage("/Transaksi/TransaksiPengolahanLimbah.fxml");
    }

    @FXML
    private void handlePenjualan() {
        setActive(btnPenjualan);
        loadPage("/Transaksi/TransaksiPenjualan.fxml");
    }

    /** Penarikan Saldo — form penarikan saldo nasabah */
    @FXML
    private void handlePenarikanSaldo() {
        setActive(btnPenarikanSaldo);
        loadPage("/Transaksi/PenarikanSaldo.fxml");
    }

    @FXML
    private void handleLogout() {
        Alert k = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin logout?", ButtonType.YES, ButtonType.NO);
        k.setTitle("Konfirmasi Logout");
        k.setHeaderText(null);
        k.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) contentArea.getScene().getWindow().hide();
        });
    }

    // ── Helper: load FXML ke content area ────────────────────────────────────
    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            contentArea.getChildren().setAll(page);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error Load Halaman",
                    "Gagal memuat: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    // ── Welcome page ─────────────────────────────────────────────────────────
    private void showWelcome() {
        javafx.scene.layout.VBox welcome = new javafx.scene.layout.VBox(12);
        welcome.setAlignment(javafx.geometry.Pos.CENTER);
        welcome.setStyle("-fx-background-color:#FAFAFA;");

        javafx.scene.control.Label lblJudul = new javafx.scene.control.Label("Selamat Datang, Kasir!");
        lblJudul.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:#1B5E20;");

        javafx.scene.control.Label lblSub = new javafx.scene.control.Label(
                "Pilih menu di sidebar untuk memulai.");
        lblSub.setStyle("-fx-font-size:13px; -fx-text-fill:#757575;");

        welcome.getChildren().addAll(lblJudul, lblSub);
        contentArea.getChildren().setAll(welcome);
    }

    // ── Highlight tombol aktif ────────────────────────────────────────────────
    private void setActive(Button btn) {
        if (activeButton != null) {
            activeButton.setStyle("");
            activeButton.getStyleClass().setAll("sidebar-btn");
        }
        activeButton = btn;
        activeButton.setStyle(
                "-fx-background-color:#1B5E20;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}