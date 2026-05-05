package client;

import control.Scenemanager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Scenemanager.getInstance().setPrimaryStage(primaryStage);

        primaryStage.setAlwaysOnTop(true);

        Scenemanager.getInstance().switchScene("login.fxml", "Đăng nhập - Hệ thống đấu giá");
    }

    public static void main(String[] args) {
        launch(args);
    }
}