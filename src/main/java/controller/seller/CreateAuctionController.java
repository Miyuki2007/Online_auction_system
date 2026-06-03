package controller.seller;

import client.AuctionClient;
import client.Session;
import controller.SceneManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.user.User;
import protocol.Response;
import protocol.requests.CreateAuctionRequest;
import protocol.responses.SuccessResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class CreateAuctionController {

    // ===== Sidebar buttons =====
    @FXML private Label lblUser;
    @FXML private Button btnHome;
    @FXML private Button btnMyAuctions;
    @FXML private Button btnCreate;
    @FXML private Button btnAccount;
    @FXML private Button btnSupport;
    @FXML private Button btnLogout;

    // ===== Image components =====
    @FXML private ImageView imagePreview;
    @FXML private VBox lblImagePlaceholder;   // VBox chứa icon + text
    @FXML private Label lblImageInfo;
    @FXML private Button btnChooseImage;
    @FXML private Button btnRemoveImage;

    // ===== Form fields =====
    @FXML private TextField txtItemName;
    @FXML private ComboBox<String> cmbItemType;
    @FXML private Label lblSpecialAttr;
    @FXML private TextField txtSpecialAttr;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtDuration;
    @FXML private CheckBox chkAntiSnipe;
    @FXML private Label lblError;

    // ===== Action buttons =====
    @FXML private Button btnReset;
    @FXML private Button btnCreateSubmit;

    // ===== Data =====
    private byte[] selectedImageData;
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ITEM_TYPES = List.of(
            "ELECTRONICS", "VEHICLE", "ART", "OTHERS"
    );

    // ============================================
    //   INITIALIZE
    // ============================================
    @FXML
    void initialize() {
        // 1. Hiển thị tên user
        User loggedIn = Session.getInstance().getLoggedInUser();
        if (loggedIn != null && "ADMIN".equalsIgnoreCase(loggedIn.getRole())) {
            SceneManager.getInstance().switchScene("admin/dashboard.fxml", "Quản trị hệ thống");
            return;
        }
        if (loggedIn != null) {
            lblUser.setText(loggedIn.getFullName() != null
                    ? loggedIn.getFullName()
                    : loggedIn.getUsername());
        }

        // 2. Khởi tạo ComboBox loại sản phẩm
        cmbItemType.setItems(FXCollections.observableArrayList(ITEM_TYPES));

        // 3. Lắng nghe khi user đổi loại → đổi label thuộc tính đặc biệt
        cmbItemType.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSpecialAttrLabel(newVal);
        });

        // 4. Trạng thái ban đầu của vùng ảnh
        lblImagePlaceholder.setVisible(true);
        btnRemoveImage.setDisable(true);
    }

    /**
     * Đổi label "Thuộc tính đặc biệt" theo loại được chọn.
     * Giúp user hiểu rõ cần điền gì.
     */
    private void updateSpecialAttrLabel(String itemType) {
        if (itemType == null) {
            lblSpecialAttr.setText("Thuộc tính đặc biệt *");
            txtSpecialAttr.setPromptText("Chọn loại sản phẩm trước");
            return;
        }
        switch (itemType) {
            case "ELECTRONICS" -> {
                lblSpecialAttr.setText("Thương hiệu (Brand) *");
                txtSpecialAttr.setPromptText("Ví dụ: Apple, Samsung, Sony");
            }
            case "VEHICLE" -> {
                lblSpecialAttr.setText("Loại phương tiện *");
                txtSpecialAttr.setPromptText("Ví dụ: Xe máy, Ô tô, Xe đạp");
            }
            case "ART" -> {
                lblSpecialAttr.setText("Tác giả (Artist) *");
                txtSpecialAttr.setPromptText("Ví dụ: Picasso, Van Gogh");
            }
            case "OTHERS" -> {
                lblSpecialAttr.setText("Phân loại *");
                txtSpecialAttr.setPromptText("Ví dụ: Đồ cổ, Sách, Quần áo...");
            }
        }
    }

    // ============================================
    //   IMAGE HANDLING
    // ============================================
    @FXML
    void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn hình ảnh sản phẩm");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files",
                        "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(getWindow());
        if (file == null) return;  // User cancel

        // Validate kích thước
        if (file.length() > MAX_IMAGE_SIZE) {
            showError("Ảnh quá lớn! Tối đa 5MB. File hiện tại: "
                    + formatSize(file.length()));
            return;
        }

        try {
            // Đọc file thành byte[]
            byte[] data = Files.readAllBytes(file.toPath());

            // Validate đúng là ảnh
            Image img = new Image(new ByteArrayInputStream(data));
            if (img.isError()) {
                showError("File không phải ảnh hợp lệ");
                return;
            }

            // Lưu data và hiển thị preview
            selectedImageData = data;
            imagePreview.setImage(img);
            lblImagePlaceholder.setVisible(false);
            btnRemoveImage.setDisable(false);

            // Cập nhật info
            lblImageInfo.setText(String.format("✅ %s (%s)",
                    file.getName(), formatSize(file.length())));
            lblImageInfo.setStyle("-fx-text-fill: #27ae60;");

            clearError();
        } catch (IOException e) {
            showError("Không đọc được file: " + e.getMessage());
        }
    }

    @FXML
    void handleRemoveImage() {
        selectedImageData = null;
        imagePreview.setImage(null);
        lblImagePlaceholder.setVisible(true);
        btnRemoveImage.setDisable(true);
        lblImageInfo.setText("Hỗ trợ: JPG, PNG, JPEG (tối đa 5MB)");
        lblImageInfo.setStyle("-fx-text-fill: #888;");
    }

    // ============================================
    //   FORM ACTIONS
    // ============================================
    @FXML
    void handleReset() {
        // Confirm trước khi reset (vì user có thể đã điền nhiều)
        if (hasFormData()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Bạn có chắc muốn xóa toàn bộ thông tin đã nhập?",
                    ButtonType.YES, ButtonType.NO);
            alert.setTitle("Xác nhận làm mới");
            alert.setHeaderText(null);
            alert.initOwner(getWindow());

            if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                return;
            }
        }
        resetForm();
    }

    @FXML
    void handleCreate() {
        clearError();

        // ===== Lấy giá trị từ form =====
        String itemName = safeText(txtItemName);
        String itemType = cmbItemType.getValue();
        String specialAttr = safeText(txtSpecialAttr);
        String description = safeText(txtDescription);
        String priceText = safeText(txtStartingPrice);
        String durationText = safeText(txtDuration);
        boolean antiSnipe = chkAntiSnipe.isSelected();

        // ===== Validate =====
        if (selectedImageData == null) {
            showError("Vui lòng chọn ảnh sản phẩm");
            return;
        }
        if (itemName.isEmpty()) {
            showError("Vui lòng nhập tên sản phẩm");
            txtItemName.requestFocus();
            return;
        }
        if (itemType == null || itemType.isEmpty()) {
            showError("Vui lòng chọn loại sản phẩm");
            cmbItemType.requestFocus();
            return;
        }
        if (specialAttr.isEmpty()) {
            showError("Vui lòng nhập " + lblSpecialAttr.getText().replace(" *", ""));
            txtSpecialAttr.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            showError("Vui lòng nhập mô tả");
            txtDescription.requestFocus();
            return;
        }

        // Parse giá
        double startingPrice;
        try {
            startingPrice = Double.parseDouble(priceText.replace(",", "").replace(".", ""));
            if (startingPrice <= 0) {
                showError("Giá khởi điểm phải lớn hơn 0");
                txtStartingPrice.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showError("Giá khởi điểm phải là số (chỉ nhập số)");
            txtStartingPrice.requestFocus();
            return;
        }

        // Parse thời lượng
        long duration;
        try {
            duration = Long.parseLong(durationText);
            if (duration <= 0) {
                showError("Thời lượng phải lớn hơn 0 phút");
                txtDuration.requestFocus();
                return;
            }
            if (duration > 10080) { // 7 ngày
                showError("Thời lượng tối đa 10080 phút (7 ngày)");
                txtDuration.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showError("Thời lượng phải là số nguyên");
            txtDuration.requestFocus();
            return;
        }

        // ===== Lấy seller ID =====
        User user = Session.getInstance().getLoggedInUser();
        if (user == null) {
            showError("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return;
        }
        String sellerId = user.getUsername();

        // ===== Gửi request =====
        // Disable nút để tránh double-click
        btnCreateSubmit.setDisable(true);
        btnCreateSubmit.setText("⏳  Đang gửi...");

        // Chạy ở thread khác để không block UI
        new Thread(() -> {
            try {
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) {
                    client.connect();
                }
                CreateAuctionRequest req = new CreateAuctionRequest(
                        sellerId, itemType, itemName, description,
                        specialAttr, startingPrice, duration, antiSnipe, selectedImageData);

                Response response = client.sendRequest(req);

                Platform.runLater(() -> {
                    btnCreateSubmit.setDisable(false);
                    btnCreateSubmit.setText("📤  Đăng sản phẩm");

                    if (response == null) {
                        showError("Server không phản hồi. Vui lòng thử lại.");
                        return;
                    }

                    if (response.isOk()) {
                        showSuccess("Đăng sản phẩm thành công! Phiên đấu giá đã được tạo.");
                        resetForm();
                    } else {
                        showError(response.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnCreateSubmit.setDisable(false);
                    btnCreateSubmit.setText("📤  Đăng sản phẩm");
                    showError("Lỗi kết nối: " + e.getMessage());
                });
            }
        }, "CreateAuction-Worker").start();
    }

    // ============================================
    //   SIDEBAR NAVIGATION
    // ============================================
    @FXML
    void handleHome() {
        SceneManager.getInstance().switchScene("home.fxml", "Trang chủ");
    }

    @FXML
    void handleMyAuctions() {
        SceneManager.getInstance().switchScene(
                "seller/my-auction.fxml", "Phiên đấu giá của tôi");
    }

    @FXML
    void handleAccount() {
        showInfo("Tài khoản", "Tính năng đang phát triển");
    }

    @FXML
    void handleSupport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Hỗ trợ");
        alert.setHeaderText("📞 Trung tâm hỗ trợ");
        alert.setContentText(
                "Hotline: 033.4924.224\n" +
                        "Email: uet@vnu.edu.vn\n" +
                        "Giờ làm việc: 7:00 - 19:00 mỗi ngày"
        );
        alert.initOwner(getWindow());
        alert.showAndWait();
    }
    @FXML
    void handleSwitchRole() {
        SceneManager.getInstance().switchScene(
                "bidder/auction-list.fxml", "Tham gia đấu giá");
    }
    @FXML
    void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn đăng xuất?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Đăng xuất");
        alert.setHeaderText(null);
        alert.initOwner(getWindow());

        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            Session.getInstance().clear();
            SceneManager.getInstance().switchScene(
                    "login.fxml", "Đăng nhập - Hệ thống đấu giá");
        }
    }

    // ============================================
    //   HELPER METHODS
    // ============================================
    private boolean hasFormData() {
        return selectedImageData != null
                || !safeText(txtItemName).isEmpty()
                || cmbItemType.getValue() != null
                || !safeText(txtSpecialAttr).isEmpty()
                || !safeText(txtDescription).isEmpty()
                || !safeText(txtStartingPrice).isEmpty()
                || !safeText(txtDuration).isEmpty()
                || chkAntiSnipe.isSelected();
    }

    private void resetForm() {
        // Reset image
        handleRemoveImage();

        // Reset fields
        txtItemName.clear();
        cmbItemType.getSelectionModel().clearSelection();
        cmbItemType.setValue(null);
        txtSpecialAttr.clear();
        txtDescription.clear();
        txtStartingPrice.clear();
        txtDuration.clear();
        chkAntiSnipe.setSelected(false);

        // Reset label đặc biệt
        lblSpecialAttr.setText("Thuộc tính đặc biệt *");
        txtSpecialAttr.setPromptText("Brand / Artist / Type tùy theo loại...");

        clearError();
    }

    private String safeText(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String safeText(TextArea area) {
        return area.getText() == null ? "" : area.getText().trim();
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            lblError.setText("⚠ " + msg);
            lblError.setStyle("-fx-text-fill: #e74c3c; -fx-padding: 4 0;");
        });
    }

    private void clearError() {
        lblError.setText("");
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.setContentText("✅ " + msg);
        alert.initOwner(getWindow());
        alert.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.initOwner(getWindow());
        alert.showAndWait();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private Window getWindow() {
        return btnCreateSubmit != null && btnCreateSubmit.getScene() != null
                ? btnCreateSubmit.getScene().getWindow()
                : null;
    }
}
