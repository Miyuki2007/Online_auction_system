package client;

import controller.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager.getInstance().setPrimaryStage(primaryStage);
        SceneManager.getInstance().switchScene("login.fxml", "Đăng nhập - Hệ thống đấu giá");
    }

    public static void main(String[] args) {
        launch(args);
    }
}