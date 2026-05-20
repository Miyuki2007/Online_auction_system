package controller.bidder;

import client.AuctionClient;
import client.Session;
import controller.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Window;
import javafx.util.Duration;
import model.auction.Auction;
import model.auction.AuctionState;
import model.auction.BidTransaction;
import model.user.Bidder;
import model.user.User;
import protocol.Response;
import protocol.requests.GetAuctionRequest;
import protocol.requests.PlaceBidRequest;
import protocol.responses.SuccessResponse;

import java.io.ByteArrayInputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller cho màn hình danh sách phiên đấu giá dành cho Bidder.
 * Bidder có thể: xem danh sách, lọc theo tên/loại/trạng thái,
 * xem chi tiết và đặt giá (place bid).
 */
public class AuctionListController {

    // ===== Sidebar =====
    @FXML private Label lblUser;
    @FXML private Label lblBalance;
    @FXML private Button btnHome;
    @FXML private Button btnAuctionList;
    @FXML private Button btnAccount;
    @FXML private Button btnSupport;
    @FXML private Button btnLogout;

    // ===== Top bar =====
    @FXML private Label lblStats;

    // ===== Toolbar =====
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbTypeFilter;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private Button btnRefresh;

    // ===== Table =====
    @FXML private TableView<Auction> tblAuctions;
    @FXML private TableColumn<Auction, Void> colImage;
    @FXML private TableColumn<Auction, String> colItemName;
    @FXML private TableColumn<Auction, String> colItemType;
    @FXML private TableColumn<Auction, String> colSeller;
    @FXML private TableColumn<Auction, String> colStartingPrice;
    @FXML private TableColumn<Auction, String> colCurrentPrice;
    @FXML private TableColumn<Auction, Integer> colBidCount;
    @FXML private TableColumn<Auction, String> colTimeRemaining;
    @FXML private TableColumn<Auction, String> colState;

    // ===== Footer =====
    @FXML private Label lblMessage;

    // ===== Data =====
    private final ObservableList<Auction> allAuctions = FXCollections.observableArrayList();
    private FilteredList<Auction> filteredAuctions;
    private Timeline countdownTimer;

    private static final NumberFormat MONEY_FORMAT =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    private static final List<String> TYPE_FILTERS = List.of(
            "Tất cả", "Điện tử", "Phương tiện", "Nghệ thuật", "Khác"
    );

    // Bidder chỉ quan tâm các phiên có thể tham gia → KHÔNG có OPEN/CANCELED
    private static final List<String> STATUS_FILTERS = List.of(
            "Tất cả", "RUNNING", "FINISHED", "PAID"
    );

    // ============================================
    //   INITIALIZE
    // ============================================
    @FXML
    void initialize() {
        // Hiển thị tên user + số dư
        User loggedIn = Session.getInstance().getLoggedInUser();
        if (loggedIn != null) {
            lblUser.setText(loggedIn.getFullName() != null
                    ? loggedIn.getFullName() : loggedIn.getUsername());

            if (loggedIn instanceof Bidder bidder) {
                lblBalance.setText("Số dư: " + formatMoney(bidder.getBalance()));
            } else {
                lblBalance.setText("");
            }
        }

        // Setup ComboBox filter
        cmbTypeFilter.setItems(FXCollections.observableArrayList(TYPE_FILTERS));
        cmbTypeFilter.setValue("Tất cả");
        cmbTypeFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters());

        cmbStatusFilter.setItems(FXCollections.observableArrayList(STATUS_FILTERS));
        cmbStatusFilter.setValue("RUNNING"); // Mặc định chỉ hiện phiên đang chạy
        cmbStatusFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilters());

        // Setup search
        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());

        // Setup table
        setupTable();

        // Click 1 lần vào row -> chuyển sang trang chi tiết
        tblAuctions.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Auction> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    openAuctionDetail(row.getItem());
                }
            });
            return row;
        });

        // Empty placeholder cho table
        Label emptyLabel = new Label("📭  Hiện chưa có phiên đấu giá nào.\nHãy quay lại sau hoặc thử thay đổi bộ lọc!");
        emptyLabel.setStyle("-fx-text-fill: #888; -fx-text-alignment: center; -fx-font-size: 14;");
        emptyLabel.setWrapText(true);
        tblAuctions.setPlaceholder(emptyLabel);

        // Đăng ký nhận notification BID_UPDATE từ server để cập nhật real-time
        registerNotificationHandler();

        // Khởi động countdown timer (cập nhật cột "Còn lại" mỗi giây)
        startCountdownTimer();

        // Load dữ liệu lần đầu
        loadAuctions();
    }

    // ============================================
    //   TABLE SETUP
    // ============================================
    private void setupTable() {
        // Cột Ảnh - hiển thị thumbnail
        colImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitHeight(50);
                imageView.setFitWidth(50);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Auction auction = getTableRow().getItem();
                byte[] imgData = auction.getItem().getImageData();
                if (imgData != null && imgData.length > 0) {
                    try {
                        imageView.setImage(new Image(new ByteArrayInputStream(imgData)));
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(createNoImageLabel());
                    }
                } else {
                    setGraphic(createNoImageLabel());
                }
            }

            private Label createNoImageLabel() {
                Label l = new Label("📷");
                l.setStyle("-fx-text-fill: #ccc; -fx-font-size: 24;");
                return l;
            }
        });

        // Tên sản phẩm
        colItemName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getItem().getName()));

        // Loại
        colItemType.setCellValueFactory(data -> {
            String typeName = data.getValue().getItem().getClass().getSimpleName();
            return new SimpleStringProperty(translateType(typeName));
        });

        // Người bán
        colSeller.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSellerId()));

        // Giá khởi điểm
        colStartingPrice.setCellValueFactory(data ->
                new SimpleStringProperty(formatMoney(data.getValue().getStartingPrice())));

        // Giá hiện tại
        colCurrentPrice.setCellValueFactory(data ->
                new SimpleStringProperty(formatMoney(data.getValue().getCurrentHighestBid())));
        // Tô đậm cột giá hiện tại để controller.bidder dễ nhìn
        colCurrentPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(price);
                    setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                }
            }
        });

        // Số bid
        colBidCount.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getBidCount()));

        // Thời gian còn lại - sẽ được Timeline update
        colTimeRemaining.setCellValueFactory(data -> {
            Auction auction = data.getValue();
            return new SimpleStringProperty(formatRemainTime(auction));
        });

        // Trạng thái với màu sắc
        colState.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getState().name()));
        colState.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String state, boolean empty) {
                super.updateItem(state, empty);
                if (empty || state == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(translateState(state));
                String color = switch (state) {
                    case "OPEN" -> "#3498db";
                    case "RUNNING" -> "#27ae60";
                    case "FINISHED" -> "#95a5a6";
                    case "PAID" -> "#16a085";
                    case "CANCELED" -> "#e74c3c";
                    default -> "#000";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });
    }

    // ============================================
    //   LOAD DATA
    // ============================================
    private void loadAuctions() {
        btnRefresh.setDisable(true);
        btnRefresh.setText("⏳ Đang tải...");

        new Thread(() -> {
            try {
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) {
                    client.connect();
                }

                GetAuctionRequest req = new GetAuctionRequest();
                Response response = client.sendRequest(req);

                Platform.runLater(() -> {
                    btnRefresh.setDisable(false);
                    btnRefresh.setText("🔄 Làm mới");

                    if (response == null) {
                        showMessage("Server không phản hồi", true);
                        return;
                    }

                    if (response.isOk()) {
                        SuccessResponse success = (SuccessResponse) response;
                        @SuppressWarnings("unchecked")
                        List<Auction> auctions = (List<Auction>) success.getData();
                        allAuctions.setAll(auctions);
                        applyFilters();
                        updateStats();
                        showMessage("Đã tải " + auctions.size() + " phiên đấu giá", false);
                    } else {
                        showMessage(response.getMessage(), true);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnRefresh.setDisable(false);
                    btnRefresh.setText("🔄 Làm mới");
                    showMessage("Lỗi kết nối: " + e.getMessage(), true);
                });
            }
        }, "LoadAuctions-Worker").start();
    }

    // ============================================
    //   NOTIFICATION HANDLER (real-time bid update)
    // ============================================
    private void registerNotificationHandler() {
        AuctionClient client = Session.getInstance().getClient();
        if (client == null) return;

        client.setOnNotification(notification -> {
            // Khi có người khác bid → server broadcast → refresh table
            Platform.runLater(() -> {
                tblAuctions.refresh();
                // Hoặc gọi loadAuctions() để lấy dữ liệu mới hoàn toàn
            });
        });
    }

    // ============================================
    //   FILTER LOGIC
    // ============================================
    private void applyFilters() {
        if (filteredAuctions == null) {
            filteredAuctions = new FilteredList<>(allAuctions);
            tblAuctions.setItems(filteredAuctions);
        }

        String searchText = txtSearch.getText() == null ? "" :
                txtSearch.getText().trim().toLowerCase();
        String typeFilter = cmbTypeFilter.getValue();
        String statusFilter = cmbStatusFilter.getValue();

        filteredAuctions.setPredicate(auction -> {
            // 1. KHÔNG hiển thị phiên do CHÍNH controller.bidder này bán (nhưng vì là controller.bidder nên không có)
            //    Ẩn các phiên OPEN và CANCELED khỏi danh sách của Bidder vì không tham gia được
            if (auction.getState() == AuctionState.OPEN
                    || auction.getState() == AuctionState.CANCELED) {
                return false;
            }

            // 2. Lọc theo tên
            if (!searchText.isEmpty()) {
                String name = auction.getItem().getName().toLowerCase();
                if (!name.contains(searchText)) return false;
            }

            // 3. Lọc theo loại sản phẩm
            if (typeFilter != null && !"Tất cả".equals(typeFilter)) {
                String typeName = auction.getItem().getClass().getSimpleName();
                if (!translateType(typeName).equals(typeFilter)) return false;
            }

            // 4. Lọc theo trạng thái
            if (statusFilter != null && !"Tất cả".equals(statusFilter)) {
                if (!auction.getState().name().equals(statusFilter)) return false;
            }
            return true;
        });
    }

    private void updateStats() {
        long total = allAuctions.stream()
                .filter(a -> a.getState() != AuctionState.OPEN
                        && a.getState() != AuctionState.CANCELED)
                .count();
        long running = allAuctions.stream()
                .filter(a -> a.getState() == AuctionState.RUNNING).count();
        long finished = allAuctions.stream()
                .filter(a -> a.getState() == AuctionState.FINISHED
                        || a.getState() == AuctionState.PAID).count();
        lblStats.setText(String.format(
                "Tổng: %d | Đang chạy: %d | Đã kết thúc: %d",
                total, running, finished));
    }

    // ============================================
    //   COUNTDOWN TIMER
    // ============================================
    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            // Refresh column "Còn lại" mỗi giây cho các phiên RUNNING
            tblAuctions.refresh();
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    @FXML
    void handleRefresh() {
        loadAuctions();
    }

    // ============================================
    //   VIEW DETAIL - chuyển sang trang chi tiết
    // ============================================
    /**
     * Mở trang chi tiết cho 1 auction cụ thể.
     * Lưu auction vào Session để AuctionDetailController đọc.
     */
    private void openAuctionDetail(Auction auction) {
        if (auction == null) return;

        Session.getInstance().setSelectedAuction(auction);
        stopTimer();
        SceneManager.getInstance().switchScene(
                "bidder/auction-detail.fxml",
                "Chi tiết: " + auction.getItem().getName());
    }

    /**
     * Giữ lại method @FXML này để tương thích ngược nếu có FXML cũ
     * còn reference đến onAction="#handleViewDetail".
     * Không có lỗi nếu không được gọi.
     */
    @FXML
    void handleViewDetail() {
        Auction selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected != null) openAuctionDetail(selected);
    }

    // ============================================
    //   PLACE BID
    // ============================================
    /**
     * Method này giữ lại chỉ để tương thích ngược nếu FXML cũ còn reference
     * đến onAction="#handlePlaceBid". Việc đặt giá thực sự đã được chuyển
     * sang AuctionDetailController.handlePlaceBid().
     */
    @FXML
    void handlePlaceBid() {
        Auction selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected != null) openAuctionDetail(selected);
    }

    // ============================================
    //   SIDEBAR NAVIGATION
    // ============================================
    @FXML
    void handleHome() {
        stopTimer();
        SceneManager.getInstance().switchScene("home.fxml", "Trang chủ - Hệ thống đấu giá");
    }

    @FXML
    void handleAccount() {
        User user = Session.getInstance().getLoggedInUser();
        if (user == null) return;

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Thông tin tài khoản");
        info.setHeaderText(user.getFullName());

        StringBuilder content = new StringBuilder();
        content.append("👤 Tên đăng nhập: ").append(user.getUsername()).append("\n");
        content.append("📧 Email: ").append(user.getEmail()).append("\n");
        content.append("🎭 Vai trò: ").append(user.getRole()).append("\n");
        if (user instanceof Bidder bidder) {
            content.append("💰 Số dư: ").append(formatMoney(bidder.getBalance())).append("\n");
            content.append("📜 Số lần đã bid: ").append(bidder.getBidHistory().size());
        }
        info.setContentText(content.toString());
        info.initOwner(getWindow());
        info.showAndWait();
    }

    @FXML
    void handleSupport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Hỗ trợ");
        alert.setHeaderText("📞 Trung tâm hỗ trợ");
        alert.setContentText(
                "Hotline: 1900-xxxx\n" +
                        "Email: support@auction.com\n" +
                        "Giờ làm việc: 8:00 - 22:00 mỗi ngày"
        );
        alert.initOwner(getWindow());
        alert.showAndWait();
    }
    @FXML
    void handleSwitchRole() {
        stopTimer();
        SceneManager.getInstance().switchScene(
                "seller/my-auction.fxml", "Phiên đấu giá của tôi");
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
            stopTimer();
            Session.getInstance().clear();
            SceneManager.getInstance().switchScene(
                    "login.fxml", "Đăng nhập - Hệ thống đấu giá");
        }
    }

    // ============================================
    //   HELPER METHODS
    // ============================================
    private String formatMoney(double amount) {
        return MONEY_FORMAT.format(amount) + " ₫";
    }

    private String formatRemainTime(Auction auction) {
        if (auction.getState() == AuctionState.FINISHED
                || auction.getState() == AuctionState.PAID) {
            return "Đã kết thúc";
        }
        if (auction.getState() == AuctionState.CANCELED) {
            return "Đã hủy";
        }
        if (auction.getState() == AuctionState.OPEN) {
            return "Chưa bắt đầu";
        }

        // RUNNING — tính thời gian còn lại
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            return "Hết giờ";
        }

        long totalSeconds = java.time.Duration.between(now, auction.getEndTime())
                .getSeconds();
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) return String.format("%dd %02d:%02d:%02d", days, hours, minutes, seconds);
        if (hours > 0) return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String translateType(String className) {
        return switch (className) {
            case "Electronics" -> "Điện tử";
            case "Vehicle" -> "Phương tiện";
            case "Art" -> "Nghệ thuật";
            case "Others" -> "Khác";
            default -> className;
        };
    }

    private String translateState(String state) {
        return switch (state) {
            case "OPEN" -> "Chờ bắt đầu";
            case "RUNNING" -> "Đang chạy";
            case "FINISHED" -> "Đã kết thúc";
            case "PAID" -> "Đã thanh toán";
            case "CANCELED" -> "Đã hủy";
            default -> state;
        };
    }

    private void showMessage(String msg, boolean isError) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: " + (isError ? "#e74c3c" : "#27ae60")
                + "; -fx-padding: 8 0 0 0;");

        // Tự động ẩn sau 5 giây
        new Timeline(new KeyFrame(Duration.seconds(5),
                e -> lblMessage.setText(""))).play();
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.initOwner(getWindow());
        alert.showAndWait();
    }

    private void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }

    private Window getWindow() {
        return tblAuctions != null && tblAuctions.getScene() != null
                ? tblAuctions.getScene().getWindow() : null;
    }
}
