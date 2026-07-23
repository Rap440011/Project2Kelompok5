package Login;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main_LoginExample extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login/LoginView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 900, 500);
        scene.setFill(null); // transparan di luar area root jika ada radius nanti

        // Window login: tanpa title bar OS, kecil, di tengah, tidak resizable
        primaryStage.initStyle(StageStyle.UNDECORATED);
        LoginController.configureLoginStage(primaryStage);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}