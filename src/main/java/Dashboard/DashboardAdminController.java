package Dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardAdminController implements Initializable {

    @FXML private StackPane contentArea;

    @FXML private Button btnMenuKaryawan;
    @FXML private Button btnMenuNasabah;
    @FXML private Button btnMenuProduk;
    @FXML private Button btnMenuLimbah;
    @FXML private Button btnLogout;

    // Path ke masing-masing FXML master (sesuaikan jika lokasi package berbeda)
    private static final String FXML_KARYAWAN = "/Master/MasterKaryawan.fxml";
    private static final String FXML_NASABAH  = "/Master/MasterNasabah.fxml";
    private static final String FXML_PRODUK   = "/Master/MasterProduk.fxml";
    private static final String FXML_LIMBAH   = "/Master/MasterLimbah.fxml";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Tampilkan halaman default saat dashboard pertama kali dibuka
        loadView(FXML_KARYAWAN, btnMenuKaryawan);
    }

    @FXML
    private void handleMenuKaryawan(ActionEvent e) {
        loadView(FXML_KARYAWAN, btnMenuKaryawan);
    }

    @FXML
    private void handleMenuNasabah(ActionEvent e) {
        loadView(FXML_NASABAH, btnMenuNasabah);
    }

    @FXML
    private void handleMenuProduk(ActionEvent e) {
        loadView(FXML_PRODUK, btnMenuProduk);
    }

    @FXML
    private void handleMenuLimbah(ActionEvent e) {
        loadView(FXML_LIMBAH, btnMenuLimbah);
    }

    private void loadView(String fxmlPath, Button activeButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            setActiveButton(activeButton);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal Memuat Halaman",
                    "Tidak dapat memuat halaman: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    private void setActiveButton(Button active) {
        for (Button b : new Button[]{btnMenuKaryawan, btnMenuNasabah, btnMenuProduk, btnMenuLimbah}) {
            if (b == null) continue;
            b.getStyleClass().remove("sidebar-btn-active");
        }
        if (active != null && !active.getStyleClass().contains("sidebar-btn-active")) {
            active.getStyleClass().add("sidebar-btn-active");
        }
    }

    @FXML
    private void handleLogout(ActionEvent e) {
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION,
                "Apakah Anda yakin ingin logout?", ButtonType.YES, ButtonType.NO);
        konfirmasi.setTitle("Konfirmasi Logout");
        konfirmasi.setHeaderText(null);
        konfirmasi.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    Node source = (Node) e.getSource();
                    Stage stage = (Stage) source.getScene().getWindow();

                    // Sesuaikan path ke halaman Login jika berbeda
                    Parent loginView = FXMLLoader.load(getClass().getResource("/Login/Login.fxml"));
                    Scene scene = new Scene(loginView);
                    stage.setScene(scene);
                    stage.setTitle("Login");
                    stage.centerOnScreen();
                } catch (IOException ex) {
                    showAlert(Alert.AlertType.ERROR, "Gagal Logout",
                            "Tidak dapat memuat halaman login: " + ex.getMessage());
                }
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
