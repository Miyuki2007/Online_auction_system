package controller.bidder;

import client.AuctionClient;
import client.Session;
import controller.SceneManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.Pair;
import model.auction.Auction;
import model.auction.AuctionState;
import model.auction.BidTransaction;
import model.auction.AutoBid;
import model.user.Bidder;
import model.user.User;
import protocol.Response;
import protocol.requests.GetAuctionDetailRequest;
import protocol.requests.GetMyBidHistoryRequest;
import protocol.requests.PlaceBidRequest;
import protocol.requests.RegisterAutoBidRequest;
import protocol.requests.CancelAutoBidRequest;
import protocol.responses.MyBidHistoryResponse;
import protocol.responses.NotificationResponse;
import protocol.responses.SuccessResponse;


import java.io.ByteArrayInputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Trang chi tiết phiên đấu giá (layout giống create-auction).
 * Ảnh nhỏ cột trái + thông tin chi tiết dạng "label + giá trị" cột phải.
 * Form đặt giá inline (TextField) thay vì dialog popup.
 */
public class AuctionDetailController {

    // ===== Sidebar =====
    @FXML
    private Label lblUser;
    @FXML
    private Label lblBalance;
    @FXML
    private Button btnHome;
    @FXML
    private Button btnAuctionList;
    @FXML
    private Button btnAccount;
    @FXML
    private Button btnSupport;
    @FXML
    private Button btnSwitchRole;
    @FXML
    private Button btnLogout;

    // ===== Top bar =====
    @FXML
    private Label lblBreadcrumb;
    @FXML
    private Label lblStateBadge;

    // ===== Ảnh =====
    @FXML
    private StackPane imageBox;
    @FXML
    private ImageView imageView;
    @FXML
    private VBox boxNoImage;
    @FXML
    private Label lblAuctionId;
    @FXML
    private Label lblAntiSnipe;

    // ===== Thông tin =====
    @FXML
    private Label lblItemName;
    @FXML
    private Label lblItemType;
    @FXML
    private Label lblSeller;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblStartingPrice;
    @FXML
    private Label lblBidCount;
    @FXML
    private Label lblTopBidder;
    @FXML
    private Label lblStartTime;
    @FXML
    private Label lblEndTime;

    // ===== Khu vực đặt giá =====
    @FXML
    private Label lblCurrentPrice;
    @FXML
    private Label lblRemainTime;
    @FXML
    private TextField txtBidAmount;
    @FXML
    private Label lblBidHint;
    @FXML
    private Button btnPlaceBid;
    @FXML
    private Button btnAutoBid;
    @FXML
    private Button btnCancelAutoBid;
    // ===== Message =====
    @FXML
    private Label lblMessage;

    // ==== Lịch sử bid ====
    @FXML private TableView<BidTransaction> tblBidHistory;
    @FXML private TableColumn<BidTransaction,String> colBidTime;
    @FXML private TableColumn<BidTransaction,String> colBidAmount;
    @FXML private Label lblAutoBidStatus;
    @FXML private Button btnRefreshHistory;
    private Auction auction;
    private Timeline countdownTimer;

    private static final NumberFormat MONEY_FORMAT =
            NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    void initialize() {
        // User info trên sidebar
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

        // Lấy auction từ Session
        auction = Session.getInstance().getSelectedAuction();
        if (auction == null) {
            showMessage("Không có phiên đấu giá nào được chọn. Quay lại danh sách...", true);
            Timeline t = new Timeline(new KeyFrame(Duration.seconds(1),
                    e -> SceneManager.getInstance().switchScene(
                            "bidder/auction-list.fxml", "Tham gia đấu giá")));
            t.play();
            return;
        }

        renderAuction();
        startCountdownTimer();

        registerNotificationHandler();

        txtBidAmount.textProperty().addListener((obs,oldV,newV)-> validateBidInput(newV));
        initBidHistoryTable();
        loadMyBidHistory();

        // Khi user nhập giá, validate và hiển thị hint ngay
        txtBidAmount.textProperty().addListener((obs, oldV, newV) -> validateBidInput(newV));
        registerAsWatcher();
    }

    // ============================================
    //   RENDER
    // ============================================
    private void renderAuction() {
        lblBreadcrumb.setText(auction.getItem().getName());
        lblStateBadge.setText(translateState(auction.getState().name()));
        lblStateBadge.setStyle(getStateBadgeStyle(auction.getState()));

        // Ảnh
        byte[] imgData = auction.getItem().getImageData();
        if (imgData != null && imgData.length > 0) {
            try {
                imageView.setImage(new Image(new ByteArrayInputStream(imgData)));
                imageView.setVisible(true);
                imageView.setManaged(true);
                boxNoImage.setVisible(false);
                boxNoImage.setManaged(false);
            } catch (Exception e) {
                showNoImage();
            }
        } else {
            showNoImage();
        }

        lblAuctionId.setText(auction.getId().length() > 12
                ? auction.getId().substring(0, 12) + "..." : auction.getId());
        lblAntiSnipe.setText(auction.isAntiSnipeEnabled() ? "BẬT" : "TẮT");

        // Thông tin chính
        lblItemName.setText(auction.getItem().getName());
        lblItemType.setText(translateType(auction.getItem().getClass().getSimpleName()));
        lblSeller.setText(auction.getSellerId());
        lblDescription.setText(auction.getItem().getDescription() != null
                && !auction.getItem().getDescription().isEmpty()
                ? auction.getItem().getDescription()
                : "(Không có mô tả)");

        // Giá + bid
        lblStartingPrice.setText(formatMoney(auction.getStartingPrice()));
        lblCurrentPrice.setText(formatMoney(auction.getCurrentHighestBid()));
        lblBidCount.setText(String.valueOf(auction.getBidCount()));
        lblTopBidder.setText(auction.getCurrentWinnerId() != null
                && !auction.getCurrentWinnerId().isEmpty()
                ? auction.getCurrentWinnerId() : "--");

        // Thời gian
        lblStartTime.setText(auction.getStartTime().format(DATE_FORMAT));
        lblEndTime.setText(auction.getEndTime().format(DATE_FORMAT));
        lblRemainTime.setText(formatRemainTime(auction));

        // Gợi ý giá đặt
        suggestBidAmount();
        updateBidButton();
    }

    private void showNoImage() {
        imageView.setImage(null);
        imageView.setVisible(false);
        imageView.setManaged(false);
        boxNoImage.setVisible(true);
        boxNoImage.setManaged(true);
    }

    /**
     * Tự điền giá gợi ý vào TextField (current + 5% hoặc +1000).
     */
    private void suggestBidAmount() {
        double current = auction.getCurrentHighestBid();
        double suggested = current + Math.max(1000, current * 0.05);
        txtBidAmount.setText(String.valueOf((long) suggested));
    }

    /**
     * Kiểm tra giá user đang gõ, hiển thị hint phù hợp.
     */
    private void validateBidInput(String text) {
        double current = auction.getCurrentHighestBid();
        if (text == null || text.trim().isEmpty()) {
            lblBidHint.setText("Tối thiểu phải > " + formatMoney(current));
            lblBidHint.setStyle("-fx-text-fill: #888;");
            return;
        }

        try {
            double amount = Double.parseDouble(text.trim().replace(",", "").replace(" ", ""));
            if (amount <= current) {
                lblBidHint.setText("⚠ Giá phải lớn hơn " + formatMoney(current));
                lblBidHint.setStyle("-fx-text-fill: #e74c3c;");
            } else {
                lblBidHint.setText("✓ Bạn sẽ đặt giá: " + formatMoney(amount));
                lblBidHint.setStyle("-fx-text-fill: #27ae60;");
            }
        } catch (NumberFormatException e) {
            lblBidHint.setText("⚠ Vui lòng nhập số hợp lệ");
            lblBidHint.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    /**
     * Quyết định nút bid enable/disable + style.
     */
    private void updateBidButton() {
        User user = Session.getInstance().getLoggedInUser();

        if (user == null) {
            disableBid("Bạn cần đăng nhập để đặt giá.");
            return;
        }
        if (auction.getState() != AuctionState.RUNNING) {
            disableBid("Phiên này không nhận giá thầu (trạng thái: "
                    + translateState(auction.getState().name()) + ").");
            return;
        }
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            disableBid("Phiên đã hết thời gian.");
            return;
        }
        if (user.getUsername().equals(auction.getSellerId())) {
            disableBid("Bạn không thể đấu giá phiên của chính mình.");
            return;
        }

        // OK
        btnPlaceBid.setDisable(false);
        btnAutoBid.setDisable(false);
        txtBidAmount.setDisable(false);
        btnPlaceBid.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white;" +
                        " -fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 12;" +
                        " -fx-background-radius: 6; -fx-cursor: hand;"
        );
        btnPlaceBid.setText("💰  ĐẶT GIÁ");
    }

    private void disableBid(String reason) {
        btnPlaceBid.setDisable(true);
        txtBidAmount.setDisable(true);
        btnPlaceBid.setStyle(
                "-fx-background-color: #bdc3c7; -fx-text-fill: white;" +
                        " -fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 12;" +
                        " -fx-background-radius: 6;"
        );
        btnPlaceBid.setText("🚫  KHÔNG THỂ ĐẶT GIÁ");
        lblBidHint.setText(reason);
        lblBidHint.setStyle("-fx-text-fill: #e67e22;");
    }

    // ============================================
    //   COUNTDOWN
    // ============================================
    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            lblRemainTime.setText(formatRemainTime(auction));
            if (LocalDateTime.now().isAfter(auction.getEndTime())
                    && !btnPlaceBid.isDisabled()) {
                updateBidButton();
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void stopTimer() {
        if (countdownTimer != null) countdownTimer.stop();
    }

    // ============================================
    //   PLACE BID (form inline)
    // ============================================
    @FXML
    void handlePlaceBid() {
        User user = Session.getInstance().getLoggedInUser();
        if (user == null) {
            showMessage("Phiên đăng nhập đã hết hạn", true);
            return;
        }

        // Parse và validate giá
        String raw = txtBidAmount.getText();
        if (raw == null || raw.trim().isEmpty()) {
            showMessage("Vui lòng nhập giá đặt", true);
            txtBidAmount.requestFocus();
            return;
        }

        double currentPrice = auction.getCurrentHighestBid();
        double amount;
        try {
            amount = Double.parseDouble(raw.trim().replace(",", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            showMessage("Giá đặt không hợp lệ. Vui lòng nhập số.", true);
            txtBidAmount.requestFocus();
            return;
        }

        if (amount <= currentPrice) {
            showMessage("Giá đặt phải lớn hơn giá hiện tại " + formatMoney(currentPrice), true);
            txtBidAmount.requestFocus();
            return;
        }

        // Xác nhận
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                String.format(
                        "Xác nhận đặt giá %s cho phiên \"%s\"?%n%n" +
                                "Lưu ý: Sau khi đặt giá thành công, bạn không thể rút lại.",
                        formatMoney(amount), auction.getItem().getName()),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận đặt giá");
        confirm.setHeaderText(null);
        confirm.initOwner(getWindow());
        confirm.getDialogPane().setMinWidth(450);

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        final double finalAmount = amount;
        final String auctionId = auction.getId();

        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("⏳ Đang đặt giá...");
        txtBidAmount.setDisable(true);

        new Thread(() -> {
            try {
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) client.connect();

                PlaceBidRequest req = new PlaceBidRequest(
                        auctionId, user.getUsername(), finalAmount);
                Response response = client.sendRequest(req);

                Platform.runLater(() -> {
                    if (response == null) {
                        showMessage("Server không phản hồi", true);
                        updateBidButton();
                        return;
                    }
                    if (response.isOk()) {
                        SuccessResponse success = (SuccessResponse) response;
                        BidTransaction bid = success.getDataAs(BidTransaction.class);
                        showMessage("✅ Đặt giá thành công: " + formatMoney(finalAmount), false);
                        refreshAuction(this::doLoadMyBidHistory);
                    } else {
                        showMessage("Đặt giá thất bại: " + response.getMessage(), true);
                        updateBidButton();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Lỗi: " + e.getMessage(), true);
                    updateBidButton();
                });
            }
        }, "PlaceBid-Worker").start();
    }

    @FXML
    void handleAutoBid() {
        User user = Session.getInstance().getLoggedInUser();
        if (user == null) {
            showMessage("Bạn cần đăng nhập để đặt giá tự động.", true);
            return;
        }
        if (auction.getState() != AuctionState.RUNNING) {
            showMessage("Phiên không nhận giá thầu.", true);
            return;
        }
        if (user.getUsername().equals(auction.getSellerId())) {
            showMessage("Bạn không thể auto-bid phiên của chính mình.", true);
            return;
        }

        // Dialog nhập maxBid + increment
        Dialog<Pair<Double, Double>> dialog = new Dialog<>();
        dialog.setTitle("Đặt giá tự động (Auto-Bid)");
        dialog.setHeaderText("Hệ thống sẽ tự động đặt giá thay bạn,\n" +
                "chỉ bid vừa đủ để thắng, đến khi đạt mức tối đa.");
        dialog.initOwner(getWindow());

        // Nội dung dialog
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setStyle("-fx-padding: 15;");

        double currentPrice = auction.getCurrentHighestBid();
        double suggestedMax = currentPrice * 2;
        double suggestedInc = Math.max(1000, currentPrice * 0.05);

        Label lblCurrent = new Label("Giá hiện tại: " + formatMoney(currentPrice));
        lblCurrent.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");

        TextField txtMax = new TextField(String.valueOf((long) suggestedMax));
        txtMax.setPromptText("Ví dụ: 5,000,000");
        txtMax.setPrefWidth(200);

        TextField txtInc = new TextField(String.valueOf((long) suggestedInc));
        txtInc.setPromptText("Ví dụ: 100,000");
        txtInc.setPrefWidth(200);

        Label hint = new Label("💡 Hệ thống sẽ bid hộ bạn vừa đủ để vượt người khác,\n" +
                "chỉ dừng khi vượt mức tối đa.");
        hint.setStyle("-fx-text-fill: #888; -fx-font-size: 11;");
        hint.setWrapText(true);

        grid.add(lblCurrent, 0, 0, 2, 1);
        grid.add(new Label("Giá tối đa (₫):"), 0, 1);
        grid.add(txtMax, 1, 1);
        grid.add(new Label("Bước nhảy (₫):"), 0, 2);
        grid.add(txtInc, 1, 2);
        grid.add(hint, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Đăng ký", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL);

        // Convert result
        dialog.setResultConverter(bt -> {
            if (bt != null && bt.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    double max = Double.parseDouble(txtMax.getText().trim().replace(",", ""));
                    double inc = Double.parseDouble(txtInc.getText().trim().replace(",", ""));
                    return new Pair<>(max, inc);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(pair -> {
            if (pair == null) {
                showMessage("Vui lòng nhập số hợp lệ.", true);
                return;
            }
            double maxBid = pair.getKey();
            double increment = pair.getValue();

            if (maxBid <= currentPrice) {
                showMessage("Giá tối đa phải lớn hơn giá hiện tại " + formatMoney(currentPrice), true);
                return;
            }
            if (increment <= 0) {
                showMessage("Bước nhảy phải lớn hơn 0", true);
                return;
            }

            sendAutoBidRequest(maxBid, increment);
        });
    }

    private void sendAutoBidRequest(double maxBid, double increment) {
        User user = Session.getInstance().getLoggedInUser();
        btnAutoBid.setDisable(true);
        btnAutoBid.setText("⏳ Đang đăng ký...");

        new Thread(() -> {
            try {
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) client.connect();

                RegisterAutoBidRequest req = new RegisterAutoBidRequest(
                        auction.getId(), user.getUsername(), maxBid, increment);
                Response response = client.sendRequest(req);

                Platform.runLater(() -> {
                    btnAutoBid.setText("🤖  ĐẶT GIÁ TỰ ĐỘNG");
                    btnAutoBid.setDisable(false);

                    if (response == null) {
                        showMessage("Server không phản hồi", true);
                        return;
                    }
                    if (response.isOk()) {
                        showMessage(String.format(
                                "✅ Đã đăng ký auto-bid! Max: %s, Bước: %s",
                                formatMoney(maxBid), formatMoney(increment)), false);
                        refreshAuction(this::doLoadMyBidHistory);
                    } else {
                        showMessage("Đăng ký thất bại: " + response.getMessage(), true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnAutoBid.setText("🤖  ĐẶT GIÁ TỰ ĐỘNG");
                    btnAutoBid.setDisable(false);
                    showMessage("Lỗi: " + e.getMessage(), true);
                });
            }
        }, "AutoBid-Worker").start();
    }

    /**
     * Refresh auction từ server sau khi bid thành công.
     */
    private void refreshAuction() {
        refreshAuction(null);
    }

    private void refreshAuction(Runnable afterRefresh) {
        new Thread(() -> {
            try {
                AuctionClient client = Session.getInstance().getClient();
                GetAuctionDetailRequest req = new GetAuctionDetailRequest(auction.getId());
                Response response = client.sendRequest(req);

                if (response != null && response.isOk()) {
                    SuccessResponse success = (SuccessResponse) response;
                    Auction updated = success.getDataAs(Auction.class);
                    if (updated != null) {
                        Platform.runLater(() -> {
                            auction = updated;
                            Session.getInstance().setSelectedAuction(updated);
                            renderAuction();
                        });
                    }
                }
                // Chạy callback TUẦN TỰ trên cùng thread, sau khi refresh xong
                if (afterRefresh != null) {
                    afterRefresh.run();
                }
            } catch (Exception ignored) {
            }
        }, "RefreshAuction-Worker").start();
    }

    // ============================================
    //   NOTIFICATION HANDLER (real-time update)
    // ============================================
    private void registerNotificationHandler() {
        AuctionClient client = Session.getInstance().getClient();
        if (client == null) return;

        client.setOnNotification(notification -> {
            if (!(notification instanceof NotificationResponse n)) return;
            if (auction == null) return;

            Platform.runLater(() -> {
                switch (n.getNotificationType()) {

                    case BID_UPDATE -> {
                        BidTransaction bid = n.getDataAs(BidTransaction.class);
                        if (bid != null) {
                            auction.applyBid(bid, null); // applyBid tự đọc bid.getNewEndTime() nếu có
                            renderAuction();
                            // Thông báo bổ sung khi anti-snipe gia hạn
                            if (bid.getNewEndTime() != null) {
                                showMessage("🔔 Bid mới: " + formatMoney(bid.getAmount())
                                        + " | ⏱ Anti-snipe: giờ kết thúc mới "
                                        + bid.getNewEndTime().format(DATE_FORMAT), false);
                            } else {
                                showMessage("🔔 Bid mới: " + formatMoney(bid.getAmount()), false);
                            }
                        }
                    }
                    case TIME_EXTENDED -> {}
/*
                    case TIME_EXTENDED -> {
                        // notification data là Auction đã được gia hạn
                        Auction updated = n.getDataAs(Auction.class);
                        if (updated != null) {
                            auction = updated;
                            Session.getInstance().setSelectedAuction(updated);
                            renderAuction();
                        }
                        showMessage("⏱ Phiên được gia hạn (anti-snipe).", false);
                    }

 */

                    case STATE_CHANGED -> {
                        AuctionState newState = n.getDataAs(AuctionState.class);
                        if (newState != null) {
                            updateBidButton();
                            if (newState == AuctionState.FINISHED || newState == AuctionState.CANCELED) {
                                stopTimer();
                            }
                        }
                        showMessage("ℹ️ " + n.getMessage(), false);
                    }

                    default -> { }
                }
            });
        });
    }
    private void registerAsWatcher() {
        new Thread(() -> {
            try {
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) client.connect();
                Response response = client.sendRequest(
                        new GetAuctionDetailRequest(auction.getId())
                );
                if (response != null && response.isOk()) {
                    SuccessResponse success = (SuccessResponse) response;
                    Auction fresh = success.getDataAs(Auction.class);
                    if (fresh != null) {
                        Platform.runLater(() -> {
                            auction = fresh;
                            Session.getInstance().setSelectedAuction(fresh);
                            renderAuction(); // cập nhật lại với data mới nhất
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi đăng ký watcher: " + e.getMessage());
            }
        }, "RegisterWatcher-Thread").start();
    }
    // ============================================
    //   SIDEBAR HANDLERS
    // ============================================
    @FXML
    void handleBack() {
        stopTimer();
        SceneManager.getInstance().switchScene(
                "bidder/auction-list.fxml", "Tham gia đấu giá");
    }

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
                        "Giờ làm việc: 8:00 - 22:00 mỗi ngày");
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
    //   LỊCH SỬ BID CỦA TÔI
    // ============================================

    private void initBidHistoryTable(){
        if (tblBidHistory == null) return;
        colBidTime.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getTimestamp().format(DATE_FORMAT)
        ));
        colBidAmount.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                formatMoney(cell.getValue().getAmount())
        ));
        tblBidHistory.setPlaceholder(new Label("Bạn chưa đặt giá trong phiên này."));
    }
    private void loadMyBidHistory() {
        if (auction == null || tblBidHistory == null) return;
        if (Session.getInstance().getLoggedInUser() == null) return;
        new Thread(this::doLoadMyBidHistory, "LoadBidHistory-Worker").start();
    }

    private void doLoadMyBidHistory() {
        if (auction == null || tblBidHistory == null) return;
        if (Session.getInstance().getLoggedInUser() == null) return;
        try {
            AuctionClient client = Session.getInstance().getClient();
            if (!client.isConnected()) client.connect();
            GetMyBidHistoryRequest req = new GetMyBidHistoryRequest(auction.getId());
            Response response = client.sendRequest(req);
            Platform.runLater(() -> {
                if (response == null || !response.isOk()) return;
                SuccessResponse success = (SuccessResponse) response;
                MyBidHistoryResponse histData = success.getDataAs(MyBidHistoryResponse.class);
                if (histData == null) return;
                //Cập nhật bảng manual bids
                List<BidTransaction> bids = histData.getManualBids();
                tblBidHistory.getItems().setAll(bids == null ? java.util.Collections.emptyList() : bids);
                //Cập nhật trạng thái auto-bids
                if (lblAutoBidStatus != null){
                    AutoBid ab = histData.getActivAutoBid();
                    if (ab != null && ab.isActive()){
                        lblAutoBidStatus.setText(String.format("🤖 Auto-bid đang hoạt động  |  Tối đa: %s  |  Bước: %s",
                                formatMoney(ab.getMaxBid()), formatMoney(ab.getIncrement())));
                        lblAutoBidStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                        // Hiện nút hủy, disable nút đăng ký
                        if (btnCancelAutoBid != null) {
                            btnCancelAutoBid.setVisible(true);
                            btnCancelAutoBid.setManaged(true);
                            btnCancelAutoBid.setUserData(ab.getId()); // lưu autoBidId
                        }
                        if (btnAutoBid != null) btnAutoBid.setDisable(true);
                    }
                    else{
                        lblAutoBidStatus.setText("Chưa đăng ký auto-bid trong phiên này.");
                        lblAutoBidStatus.setStyle("-fx-text-fill: #888;");
                        // Ẩn nút hủy, bật lại nút đăng ký
                        if (btnCancelAutoBid != null) {
                            btnCancelAutoBid.setVisible(false);
                            btnCancelAutoBid.setManaged(false);
                        }
                        if (btnAutoBid != null) btnAutoBid.setDisable(false);
                    }
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }
    @FXML
    void handleCancelAutoBid(){
        if (btnCancelAutoBid == null || btnCancelAutoBid.getUserData() == null) return;
        String autoBidId = (String) btnCancelAutoBid.getUserData();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn hủy auto-bid đang hoạt động?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận hủy auto-bid");
        confirm.setHeaderText(null);
        confirm.initOwner(getWindow());
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        btnCancelAutoBid.setDisable(true);
        btnCancelAutoBid.setText("⏳ Đang hủy...");

        new Thread(() -> {
            try {
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) client.connect();

                CancelAutoBidRequest req = new CancelAutoBidRequest(autoBidId, auction.getId());
                Response response = client.sendRequest(req);

                Platform.runLater(() -> {
                    btnCancelAutoBid.setDisable(false);
                    btnCancelAutoBid.setText("❌  HỦY ĐẶT GIÁ TỰ ĐỘNG");
                    if (response != null && response.isOk()) {
                        showMessage("✅ Đã hủy auto-bid thành công.", false);
                        loadMyBidHistory();
                    } else {
                        showMessage("Hủy thất bại: " +
                                (response != null ? response.getMessage() : "Server không phản hồi"), true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnCancelAutoBid.setDisable(false);
                    btnCancelAutoBid.setText("❌  HỦY ĐẶT GIÁ TỰ ĐỘNG");
                    showMessage("Lỗi: " + e.getMessage(), true);
                });
            }
        }, "CancelAutoBid-Worker").start();
    }
    @FXML
    void handleRefreshHistory(){
        loadMyBidHistory();
    }
    // ============================================
    //   HELPERS
    // ============================================
    private String formatMoney(double amount) {
        return MONEY_FORMAT.format(amount) + " ₫";
    }

    private String formatRemainTime(Auction a) {
        if (a.getState() == AuctionState.FINISHED || a.getState() == AuctionState.PAID)
            return "Đã kết thúc";
        if (a.getState() == AuctionState.CANCELED) return "Đã hủy";
        if (a.getState() == AuctionState.OPEN) return "Chưa bắt đầu";

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(a.getEndTime())) return "Hết giờ";

        long totalSec = java.time.Duration.between(now, a.getEndTime()).getSeconds();
        long d = totalSec / 86400;
        long h = (totalSec % 86400) / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;

        if (d > 0) return String.format("%d ngày %02d:%02d:%02d", d, h, m, s);
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
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

    private String getStateBadgeStyle(AuctionState state) {
        String base = "-fx-padding: 6 14; -fx-background-radius: 14; -fx-text-fill: white; -fx-font-weight: bold;";
        return switch (state) {
            case RUNNING -> base + " -fx-background-color: #27ae60;";
            case FINISHED -> base + " -fx-background-color: #95a5a6;";
            case PAID -> base + " -fx-background-color: #3498db;";
            case OPEN -> base + " -fx-background-color: #f39c12;";
            case CANCELED -> base + " -fx-background-color: #e74c3c;";
        };
    }

    private void showMessage(String msg, boolean isError) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-padding: 4 0; -fx-text-fill: "
                + (isError ? "#e74c3c" : "#27ae60") + ";");

        new Timeline(new KeyFrame(Duration.seconds(5),
                e -> lblMessage.setText(""))).play();
    }

    private Window getWindow() {
        if (lblItemName != null && lblItemName.getScene() != null)
            return lblItemName.getScene().getWindow();
        return null;
    }

}