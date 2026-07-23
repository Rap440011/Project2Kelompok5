package com.example.project2kelompok5;

import Login.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/Login/LoginView.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 900, 500);

        stage.initStyle(StageStyle.UNDECORATED);
        LoginController.configureLoginStage(stage);

        stage.setScene(scene);
        stage.show();
    }
}