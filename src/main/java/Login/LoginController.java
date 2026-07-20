package Login;

import Auth.Session;
import Connection.DBConnect;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;

    private final DBConnect db = new DBConnect();

    /**
     * Path FXML dashboard berdasarkan Jabatan karyawan.
     * Sesuai MasterKaryawanController: cmbJabatan berisi "Kasir", "Manager", "Admin".
     * Jabatan "Admin" -> DashboardAdmin, selain itu (Kasir/Manager) -> DashboardKasir.
     */
    private static final String DASHBOARD_ADMIN = "/Dashboard/DashboardAdmin.fxml";
    private static final String DASHBOARD_KASIR = "/Dashboard/DashboardKasir.fxml";

    private static final String JABATAN_ADMIN = "Admin";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setText("");
        lblError.setVisible(false);

        txtPassword.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });

        txtUsername.textProperty().addListener((obs, oldVal, newVal) -> sembunyikanError());
        txtPassword.textProperty().addListener((obs, oldVal, newVal) -> sembunyikanError());
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
            // Kolom disesuaikan persis dengan tabel yang dipakai MasterKaryawanController:
            // ID_Karyawan, Nama_Karyawan, Username, Password, Jabatan, Status.
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
     * Membuka dashboard sesuai Jabatan. "Admin" -> Dashboard Admin,
     * "Kasir" / "Manager" / lainnya -> Dashboard Kasir.
     */
    private void bukaDashboard(String jabatan) {
        boolean isAdmin = jabatan != null
                && jabatan.trim().toLowerCase(Locale.ROOT).equals(JABATAN_ADMIN.toLowerCase(Locale.ROOT));
        String path = isAdmin ? DASHBOARD_ADMIN : DASHBOARD_KASIR;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(isAdmin ? "Bumi Makmur - Admin" : "Bumi Makmur - Kasir");
            stage.setMaximized(true);
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
}