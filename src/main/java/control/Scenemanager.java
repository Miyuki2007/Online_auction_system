package control;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Scenemanager {
    // Áp dụng Singleton Pattern theo hướng dẫn tuần 6
    private static Scenemanager instance;
    private Stage primaryStage;

    private Scenemanager() {}

    public static Scenemanager getInstance() {
        if (instance == null) {
            instance = new Scenemanager();
        }
        return instance;
    }

    // Thiết lập Stage chính từ lớp Main
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Chuyển đổi màn hình linh hoạt
     * @param fxmlFile Tên file fxml (ví dụ: "login.fxml")
     * @param title Tiêu đề của cửa sổ
     */
    public void switchScene(String fxmlFile, String title) {
        try {
            // Theo cấu trúc của bạn: resources/fxml/filename.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen(); // Căn giữa màn hình khi chuyển cảnh
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Lỗi: Không thể tải file FXML tại /fxml/" + fxmlFile);
            e.printStackTrace();
        }
    }
}
