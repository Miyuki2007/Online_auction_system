package control;

import client.AuctionClient;
import protocol.Request;
import protocol.Response;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {


    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    private AuctionClient client;

    @FXML
    void initialize(){
        client = new AuctionClient();
        try{
            client.connect();
        } catch(Exception e){
            showAlert(Alert.AlertType.ERROR,"Lỗi kết nối", "Không thể kết nối tới server: " + e.getMessage());
        }
    }
    @FXML
    void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        if (username.isEmpty() || password.isEmpty()){
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        try{
            Request loginRequest = new Request (Request.Type.LOGIN,username,password);
            Response response = client.sendRequest(loginRequest);
            if (response.getStatus() == Response.Status.OK){
                showAlert(Alert.AlertType.INFORMATION, "Đăng nhập thành công", response.getMessage());
            }
            else{
                showAlert(Alert.AlertType.INFORMATION, "Đăng nhập thất bại", response.getMessage());
            }
        } catch(Exception e){
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi khi gửi yêu cầu: " +  e.getMessage());
        }
    }
    private void showAlert(Alert.AlertType type, String title, String content){
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();

        });
    }
}