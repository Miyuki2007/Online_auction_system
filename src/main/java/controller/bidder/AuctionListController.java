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
    @FXML private Label lblSelectedInfo;
    @FXML private Button btnViewDetail;
    @FXML private Button btnPlaceBid;
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

        // Lắng nghe khi user chọn 1 dòng
        tblAuctions.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldA, newA) -> updateActionButtons(newA));

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

    // ============================================
    //   ACTION BUTTONS STATE
    // ============================================
    private void updateActionButtons(Auction selected) {
        if (selected == null) {
            lblSelectedInfo.setText("Chưa chọn phiên nào");
            btnViewDetail.setDisable(true);
            btnPlaceBid.setDisable(true);
            return;
        }

        lblSelectedInfo.setText("Đã chọn: " + selected.getItem().getName());
        btnViewDetail.setDisable(false);

        // Chỉ cho đặt giá nếu phiên đang RUNNING và còn thời gian
        boolean canBid = selected.getState() == AuctionState.RUNNING
                && LocalDateTime.now().isBefore(selected.getEndTime());

        // Không cho seller tự đấu giá phiên của mình (phòng trường hợp dùng cùng tài khoản)
        User user = Session.getInstance().getLoggedInUser();
        if (user != null && user.getUsername().equals(selected.getSellerId())) {
            canBid = false;
        }

        btnPlaceBid.setDisable(!canBid);
    }

    @FXML
    void handleRefresh() {
        loadAuctions();
    }

    // ============================================
    //   VIEW DETAIL
    // ============================================
    @FXML
    void handleViewDetail() {
        Auction selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert detail = new Alert(Alert.AlertType.INFORMATION);
        detail.setTitle("Chi tiết phiên đấu giá");
        detail.setHeaderText(selected.getItem().getName());

        StringBuilder content = new StringBuilder();
        content.append("📋 ID: ").append(selected.getId().substring(0, 8)).append("...\n");
        content.append("📦 Loại: ").append(translateType(
                selected.getItem().getClass().getSimpleName())).append("\n");
        content.append("📝 Mô tả: ").append(selected.getItem().getDescription()).append("\n");
        content.append("👤 Người bán: ").append(selected.getSellerId()).append("\n\n");

        content.append("💰 Giá khởi điểm: ").append(formatMoney(selected.getStartingPrice())).append("\n");
        content.append("💰 Giá hiện tại: ").append(formatMoney(selected.getCurrentHighestBid())).append("\n");
        content.append("👥 Số lượt bid: ").append(selected.getBidCount()).append("\n");
        if (selected.getCurrentWinnerId() != null) {
            content.append("🏆 Người dẫn đầu: ").append(selected.getCurrentWinnerId()).append("\n");
        }
        content.append("\n");
        content.append("🕐 Bắt đầu: ").append(selected.getStartTime()).append("\n");
        content.append("🕐 Kết thúc: ").append(selected.getEndTime()).append("\n");
        content.append("⏱ Còn lại: ").append(formatRemainTime(selected)).append("\n\n");
        content.append("📊 Trạng thái: ").append(translateState(selected.getState().name())).append("\n");
        content.append("🛡 Anti-sniping: ").append(
                selected.isAntiSnipeEnabled() ? "BẬT" : "TẮT");

        detail.setContentText(content.toString());
        detail.initOwner(getWindow());
        detail.getDialogPane().setMinWidth(450);
        detail.showAndWait();
    }

    // ============================================
    //   PLACE BID
    // ============================================
    @FXML
    void handlePlaceBid() {
        Auction selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        User user = Session.getInstance().getLoggedInUser();
        if (user == null) {
            showMessage("Phiên đăng nhập đã hết hạn", true);
            return;
        }

        // Kiểm tra trạng thái lần nữa (phòng race condition)
        if (selected.getState() != AuctionState.RUNNING) {
            showMessage("Phiên đấu giá không còn nhận giá thầu", true);
            return;
        }
        if (user.getUsername().equals(selected.getSellerId())) {
            showMessage("Bạn không thể đấu giá phiên của chính mình", true);
            return;
        }

        // Tạo dialog nhập giá
        double currentPrice = selected.getCurrentHighestBid();
        double suggestedBid = currentPrice + Math.max(1000, currentPrice * 0.05); // gợi ý +5% hoặc +1000

        TextInputDialog dialog = new TextInputDialog(
                String.valueOf((long) suggestedBid));
        dialog.setTitle("Đặt giá");
        dialog.setHeaderText("Đặt giá cho: " + selected.getItem().getName());
        dialog.setContentText(String.format(
                "Giá hiện tại: %s\n" +
                        "Nhập giá của bạn (phải > %s):",
                formatMoney(currentPrice), formatMoney(currentPrice)));
        dialog.initOwner(getWindow());
        dialog.getDialogPane().setMinWidth(420);

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        // Parse giá - chỉ xóa khoảng trắng và dấu phân cách hàng nghìn (dấu phẩy)
        // KHÔNG xóa dấu chấm thập phân
        double amount;
        try {
            String raw = result.get().trim().replace(",", "").replace(" ", "");
            amount = Double.parseDouble(raw);
            if (amount <= currentPrice) {
                showMessage("Giá đặt phải lớn hơn giá hiện tại " + formatMoney(currentPrice), true);
                return;
            }
        } catch (NumberFormatException e) {
            showMessage("Giá đặt không hợp lệ. Vui lòng nhập số.", true);
            return;
        }

        // Xác nhận
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                String.format(
                        "Xác nhận đặt giá %s cho phiên \"%s\"?\n\n" +
                                "Lưu ý: Sau khi đặt giá thành công, bạn không thể rút lại.",
                        formatMoney(amount), selected.getItem().getName()),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận đặt giá");
        confirm.setHeaderText(null);
        confirm.initOwner(getWindow());
        confirm.getDialogPane().setMinWidth(450);

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        // Gửi request lên server
        final double finalAmount = amount;
        final String auctionId = selected.getId();

        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("⏳ Đang đặt...");

        new Thread(() -> {
            try {
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) {
                    client.connect();
                }

                PlaceBidRequest req = new PlaceBidRequest(
                        auctionId, user.getUsername(), finalAmount);
                Response response = client.sendRequest(req);

                Platform.runLater(() -> {
                    btnPlaceBid.setText("💰 Đặt giá");

                    if (response == null) {
                        btnPlaceBid.setDisable(false);
                        showMessage("Server không phản hồi", true);
                        return;
                    }

                    if (response.isOk()) {
                        SuccessResponse success = (SuccessResponse) response;
                        BidTransaction bid = success.getDataAs(BidTransaction.class);
                        showMessage(String.format(
                                "✅ Đặt giá %s thành công! Bid ID: %s",
                                formatMoney(bid.getAmount()),
                                bid.getId().substring(0, 8)), false);
                        // Reload để cập nhật giá mới
                        loadAuctions();
                    } else {
                        btnPlaceBid.setDisable(false);
                        showMessage(response.getMessage(), true);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnPlaceBid.setDisable(false);
                    btnPlaceBid.setText("💰 Đặt giá");
                    showMessage("Lỗi: " + e.getMessage(), true);
                });
            }
        }, "PlaceBid-Worker").start();
    }

    // ============================================
    //   SIDEBAR NAVIGATION
    // ============================================
    @FXML
    void handleHome() {
        showInfo("Trang chủ", "Tính năng đang phát triển");
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
