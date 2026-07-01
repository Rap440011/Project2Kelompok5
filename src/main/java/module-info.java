module com.example.project2kelompok5 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.project2kelompok5 to javafx.fxml;
    opens Dashboard to javafx.fxml;
    opens Master to javafx.fxml;
    opens Connection to javafx.fxml;
    opens Transaksi to javafx.fxml;

    exports com.example.project2kelompok5;
    exports Dashboard;
    exports Master;
    exports Connection;
    exports Transaksi;
}