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
import protocol.requests.LoginRequest;
import protocol.responses.SuccessResponse;

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
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        try {
            AuctionClient client = Session.getInstance().getClient();
            if (!client.isConnected()) {
                client.connect();
            }

            // ✅ Type-safe: dùng class con LoginRequest
            LoginRequest req = new LoginRequest(username, password);
            Response res = client.sendRequest(req);

            if (res.isOk()) {
                // ✅ Type-safe: ép kiểu data
                SuccessResponse success = (SuccessResponse) res;
                User user = success.getDataAs(User.class);

                Session.getInstance().setLoggedInUser(user);

                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đăng nhập thành công! Xin chào, " + user.getUsername() + ".");

            } else {
                lblError.setText(res.getMessage());
            }
        } catch (Exception e) {
            lblError.setText("Lỗi kết nối: " + e.getMessage());
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