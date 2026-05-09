package controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {

    private static SceneManager instance;
    private Stage primaryStage;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }


    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void switchScene(String fxmlFile, String title) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            //Giữ kích thước cửa sổ hiện tại nếu có scene trước
            double width = primaryStage.getScene()!= null ? primaryStage.getWidth():1200;
            double height = primaryStage.getScene()!= null ? primaryStage.getHeight():780;
            Scene scene = new Scene(root,width,height);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            //Cho phép resize tự do
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);

            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Lỗi: Không thể tải file FXML tại /fxml/" + fxmlFile);
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi tải giao diện");
            alert.setHeaderText("Không thể mở: " + fxmlFile);
            alert.setContentText(e.getMessage()!= null? e.getMessage():e.toString());
            alert.showAndWait();
        }
    }
}
