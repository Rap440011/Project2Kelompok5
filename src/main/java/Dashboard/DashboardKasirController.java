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

    // ===================== FXML =====================
    @FXML private StackPane contentArea;

    @FXML private Button btnSetorLimbah;
    @FXML private Button btnPengolahanLimbah;
    @FXML private Button btnPenjualan;
    @FXML private Button btnLogout;

    /** Tombol yang sedang aktif (untuk highlight sidebar). */
    private Button activeButton = null;

    // ===================== INITIALIZE =====================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Tampilkan halaman selamat datang saat pertama dibuka
        showWelcome();
    }

    // ===================== HANDLER MENU =====================

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

    @FXML
    private void handleLogout() {
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Yakin ingin logout?", ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Logout");
        konfirmasi.setHeaderText(null);
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                // Tutup window dashboard kasir
                contentArea.getScene().getWindow().hide();
            }
        });
    }

    // ===================== HELPER: LOAD FXML KE CONTENT AREA =====================

    /**
     * Memuat file FXML ke dalam {@code contentArea} (StackPane tengah).
     * Logika di dalam setiap controller transaksi TIDAK diubah sama sekali —
     * cukup di-load dan ditampilkan di sini.
     *
     * @param fxmlPath path resource FXML, mis. "/Transaksi/TransaksiSetorLimbah.fxml"
     */
    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();

            contentArea.getChildren().setAll(page);

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR,
                    "Error Load Halaman",
                    "Gagal memuat: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    // ===================== HELPER: WELCOME PAGE =====================

    /**
     * Halaman sambutan sederhana yang tampil saat dashboard pertama dibuka,
     * sebelum user memilih menu apapun.
     */
    private void showWelcome() {
        javafx.scene.layout.VBox welcome = new javafx.scene.layout.VBox(12);
        welcome.setAlignment(javafx.geometry.Pos.CENTER);
        welcome.setStyle("-fx-background-color:#FAFAFA;");

        javafx.scene.control.Label lblJudul = new javafx.scene.control.Label(
                "Selamat Datang, Kasir!");
        lblJudul.setStyle(
                "-fx-font-size:26px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:#1B5E20;");

        javafx.scene.control.Label lblSub = new javafx.scene.control.Label(
                "Pilih menu transaksi di sidebar untuk memulai.");
        lblSub.setStyle(
                "-fx-font-size:13px;" +
                        "-fx-text-fill:#757575;");

        welcome.getChildren().addAll(lblJudul, lblSub);
        contentArea.getChildren().setAll(welcome);
    }

    // ===================== HELPER: HIGHLIGHT TOMBOL AKTIF =====================

    /**
     * Beri highlight pada tombol menu yang sedang aktif,
     * menggunakan style CSS yang sama dengan DashboardAdminController.
     * Tombol non-aktif dikembalikan ke style default sidebar-btn.
     */
    private void setActive(Button btn) {
        if (activeButton != null) {
            // Kembalikan tombol lama ke style normal sidebar
            activeButton.setStyle("");
            activeButton.getStyleClass().setAll("sidebar-btn");
        }
        activeButton = btn;
        // Terapkan warna aktif — sesuaikan dengan warna aktif di style.css Admin
        activeButton.setStyle(
                "-fx-background-color:#1B5E20;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;");
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