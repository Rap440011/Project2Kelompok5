package Connection;

import java.sql.*;

public class DBConnect {

    public Connection conn;
    public Statement stmt;
    public ResultSet result;
    public PreparedStatement pstat;
    public CallableStatement cstat;

    public DBConnect() {
        try {
            // Daftarkan driver secara eksplisit -- jaga-jaga kalau ServiceLoader
            // otomatis (module path / JPMS) tidak menemukannya sendiri.
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            String url = "jdbc:sqlserver://DESKTOP-KMBMNGK:55755;" +
                    "databaseName=BumiMakmur;" +
                    "user=sa;" +
                    "password=earthpeople35;" +
                    "trustServerCertificate=true";

            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            System.out.println("Connecting to database succesfully");
        } catch (Exception e){
            System.out.println("Error saat connect database" + e);
        }
    }

    public static void main(String[] args) {
        DBConnect db = new DBConnect();
    }
}