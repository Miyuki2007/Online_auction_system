 package controller;

import client.AuctionClient;
import client.Session;
import javafx.stage.Window;
import protocol.Response;
import protocol.requests.RegisterRequest;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Button btnRegister;
    @FXML private Label lblError;
    @FXML private Hyperlink linkLogin;

    private AuctionClient client;

    @FXML
    void initialize() {
        cmbRole.setItems(FXCollections.observableArrayList("Bidder", "Seller"));
        cmbRole.getSelectionModel().selectFirst();

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
    void handleRegister() {
        lblError.setText("");

        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String role = cmbRole.getValue();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Vui lòng điền đầy đủ tất cả các trường.");
            return;
        }
        if (username.length() < 3) {
            showError("Tên đăng nhập phải có ít nhất 3 ký tự.");
            return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Email không hợp lệ.");
            return;
        }
        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp.");
            return;
        }
        if (role == null) {
            showError("Vui lòng chọn vai trò.");
            return;
        }
        if (!client.isConnected()) {
            showError("Chưa kết nối được tới server.");
            return;
        }

        try {
            RegisterRequest registerRequest = new RegisterRequest(
                    username, password, email, fullName, role
            );
            Response response = client.sendRequest(registerRequest);

            if (response == null) {
                showError("Server không phản hồi. Hãy thử lại.");
                return;
            }
            if (response.getStatus() == Response.Status.OK) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đăng ký thành công! Hãy đăng nhập để tiếp tục.");
                handleGoToLogin();
            } else {
                showError(response.getMessage());
            }
        } catch (Exception e) {
            showError("Lỗi khi gửi yêu cầu: " + e.getMessage());
        }
    }

    @FXML
    void handleGoToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnRegister.getScene().getWindow();
            stage.setTitle("Đăng nhập - Hệ thống đấu giá");
            stage.setScene(new Scene(root, 800, 500));
        } catch (Exception e) {
            showError("Không thể mở trang đăng nhập: " + e.getMessage());
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
        Window owner = btnRegister.getScene().getWindow();
        alert.initOwner(owner);
        alert.showAndWait();
    }
}
