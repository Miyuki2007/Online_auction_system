package control;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Scenemanager {

    private static Scenemanager instance;
    private Stage primaryStage;

    private Scenemanager() {}

    public static Scenemanager getInstance() {
        if (instance == null) {
            instance = new Scenemanager();
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

            Scene scene = new Scene(root);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Lỗi: Không thể tải file FXML tại /fxml/" + fxmlFile);
            e.printStackTrace();
        }
    }
}
