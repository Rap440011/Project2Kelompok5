package Login;

import Auth.Session;
import Connection.DBConnect;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private Button btnTogglePassword;
    @FXML private Group eyeOpenIcon;
    @FXML private Group eyeClosedIcon;
    @FXML private Button btnLogin;
    @FXML private Button btnClose;
    @FXML private Label lblError;

    private final DBConnect db = new DBConnect();

    // Status tampilan password (true = terlihat sebagai teks biasa)
    private boolean passwordVisible = false;

    private static final String DASHBOARD_ADMIN = "/Dashboard/DashboardAdmin.fxml";
    private static final String DASHBOARD_KASIR = "/Dashboard/DashboardKasir.fxml";
    private static final String JABATAN_ADMIN = "Admin";

    // Untuk drag window (karena window undecorated / tanpa title bar)
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setText("");
        lblError.setVisible(false);

        // Sinkronkan TextField "mata terbuka" dengan PasswordField asli,
        // supaya nilai yang diketik tetap sama walau tampilan berpindah.
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());

        txtPassword.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });
        txtPasswordVisible.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });

        txtUsername.textProperty().addListener((obs, oldVal, newVal) -> sembunyikanError());
        txtPassword.textProperty().addListener((obs, oldVal, newVal) -> sembunyikanError());
    }

    // ===== TOGGLE SHOW/HIDE PASSWORD =====
    @FXML
    private void handleTogglePassword(ActionEvent event) {
        passwordVisible = !passwordVisible;

        txtPassword.setVisible(!passwordVisible);
        txtPassword.setManaged(!passwordVisible);
        txtPasswordVisible.setVisible(passwordVisible);
        txtPasswordVisible.setManaged(passwordVisible);

        eyeOpenIcon.setVisible(!passwordVisible);
        eyeClosedIcon.setVisible(passwordVisible);

        // Pastikan fokus tetap di field yang sedang aktif, kursor di posisi akhir teks
        if (passwordVisible) {
            txtPasswordVisible.requestFocus();
            txtPasswordVisible.positionCaret(txtPasswordVisible.getText().length());
        } else {
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText().length());
        }
    }

    // ===== DRAG WINDOW (tanpa title bar) =====
    @FXML
    private void handleMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    // ===== TOMBOL CLOSE CUSTOM =====
    @FXML
    private void handleClose(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    private void handleLoginAction(ActionEvent event) {
        handleLogin();
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            tampilkanError("Username dan Password wajib diisi.");
            return;
        }

        try {
            java.sql.PreparedStatement ps = db.conn.prepareStatement(
                    "SELECT ID_Karyawan, Nama_Karyawan, Jabatan, Status " +
                            "FROM tb_Karyawan WHERE Username = ? AND Password = ?");
            ps.setString(1, username);
            ps.setString(2, password);
            db.result = ps.executeQuery();

            if (db.result.next()) {
                String status = db.result.getString("Status");
                if (status != null && status.trim().equalsIgnoreCase("Tidak Aktif")) {
                    tampilkanError("Akun ini sudah dinonaktifkan. Hubungi admin.");
                    return;
                }

                String idKaryawan = db.result.getString("ID_Karyawan");
                String jabatan = db.result.getString("Jabatan");

                Session.setIdKaryawanLogin(idKaryawan);

                bukaDashboard(jabatan);
            } else {
                tampilkanError("Username atau Password salah.");
            }
        } catch (SQLException e) {
            tampilkanError("Gagal terhubung ke database: " + e.getMessage());
        }
    }

    /**
     * Membuka dashboard sesuai Jabatan dalam Stage BARU yang maximized dan
     * memiliki title bar normal (decorated), lalu menutup window login.
     */
    private void bukaDashboard(String jabatan) {
        boolean isAdmin = jabatan != null
                && jabatan.trim().toLowerCase(Locale.ROOT).equals(JABATAN_ADMIN.toLowerCase(Locale.ROOT));
        String path = isAdmin ? DASHBOARD_ADMIN : DASHBOARD_KASIR;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();

            Stage dashboardStage = new Stage();
            dashboardStage.initStyle(StageStyle.DECORATED);
            dashboardStage.setScene(new Scene(root));
            dashboardStage.setTitle(isAdmin ? "Bumi Makmur - Admin" : "Bumi Makmur - Kasir");
            dashboardStage.setMaximized(true);
            dashboardStage.show();

            // Tutup window login (undecorated, kecil)
            Stage loginStage = (Stage) btnLogin.getScene().getWindow();
            loginStage.close();
        } catch (IOException e) {
            tampilkanError("Gagal membuka halaman utama: " + e.getMessage());
        }
    }

    private void tampilkanError(String pesan) {
        lblError.setText(pesan);
        lblError.setVisible(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> sembunyikanError());
        pause.play();
    }

    private void sembunyikanError() {
        lblError.setVisible(false);
    }

    /**
     * Method untuk mengatur ukuran dan posisi login window.
     * Dipanggil dari Main/Launcher sebelum stage.show().
     */
    public static void configureLoginStage(Stage stage) {
        stage.setWidth(900);
        stage.setHeight(500);
        stage.setResizable(false);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
    }
}