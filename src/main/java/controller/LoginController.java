package controller;

import client.AuctionClient;
import client.Session;
import javafx.scene.Node;
import javafx.stage.Window;
import model.user.User;
import protocol.Request;
import protocol.Response;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;
    @FXML private Hyperlink linkRegister;

    private AuctionClient client;

    @FXML
    void initialize() {
        // Dùng client chia sẻ trong Session, không tạo socket mới mỗi màn hình
        client = Session.getInstance().getClient();
        try {
            if (!client.isConnected()) {
                client.connect();
            }
        } catch (Exception e) {
            showError("Không thể kết nối tới server: " + e.getMessage());
        }
    }

    @FXML
    void handleLogin() {
        lblError.setText("");

        String username = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }
        if (!client.isConnected()) {
            showError("Chưa kết nối được tới server.");
            return;
        }

        try {
            Request loginRequest = new Request(Request.Type.LOGIN, username, password);
            Response response = client.sendRequest(loginRequest);

            if (response == null) {
                showError("Server không phản hồi. Hãy thử lại.");
                return;
            }
            if (response.getStatus() == Response.Status.OK) {
                User user = (User) response.getData();
                Session.getInstance().setLoggedInUser(user);
                showAlert(Alert.AlertType.INFORMATION,
                        "Thành công",
                        "Xin chào " + user.getFullName() + " (" + user.getRole() + ")");
                // TODO: chuyển sang dashboard tương ứng với role khi nhóm xây xong UI
                // goToDashboard(user);
            } else {
                showError(response.getMessage());
            }
        } catch (Exception e) {
            showError("Lỗi khi gửi yêu cầu: " + e.getMessage());
        }
    }

    @FXML
    void handleGoToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setTitle("Đăng ký - Hệ thống đấu giá");
            stage.setScene(new Scene(root, 800, 600));
        } catch (Exception e) {
            showError("Không thể mở trang đăng ký: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> lblError.setText(message));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Window owner = btnLogin.getScene().getWindow();
        alert.initOwner(owner);
        alert.showAndWait();
    }
}