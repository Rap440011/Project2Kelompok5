module com.example.project2kelompok5 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.microsoft.sqlserver.jdbc;
    requires javafx.swing;
    requires java.desktop;
    requires net.sf.jasperreports.core;

    opens com.example.project2kelompok5 to javafx.fxml;
    opens Dashboard to javafx.fxml;
    opens Master to javafx.fxml;
    opens Connection to javafx.fxml;
    opens Transaksi to javafx.fxml;
    opens Kasir to javafx.fxml;
    opens Login to javafx.fxml;
    opens Auth to javafx.fxml;

    exports com.example.project2kelompok5;
    exports Dashboard;
    exports Master;
    exports Connection;
    exports Transaksi;
    exports Kasir;
    exports Login;
    exports Auth;
}