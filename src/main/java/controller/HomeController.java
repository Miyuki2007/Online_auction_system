package controller;

import client.AuctionClient;
import client.Session;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import model.auction.Auction;
import model.auction.AuctionState;
import model.auction.BidTransaction;
import model.user.User;
import protocol.Response;
import protocol.requests.GetAuctionRequest;
import protocol.responses.SuccessResponse;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Trang chủ — màn hình đầu sau khi đăng nhập.
 *
 * Tính năng:
 *  - Banner chào mừng + thống kê tổng quan
 *  - 2 card lớn: lối vào Đấu giá (Bidder) và Bán hàng (Seller)
 *  - "Bid History Visualization": LineChart hiển thị diễn biến giá của một phiên
 *    + ComboBox cho user chọn phiên muốn xem (mặc định = phiên có nhiều bid nhất)
 *    + Auto-refresh mỗi 5s cho phiên đang RUNNING
 *    + Server-push qua AuctionClient.setOnNotification (Observer pattern)
 */
public class HomeController {

    // ===== Sidebar =====
    @FXML private Label lblUser;

    // ===== Top bar =====
    @FXML private Label lblStats;

    // ===== Welcome + cards =====
    @FXML private Label lblWelcome;
    @FXML private Label lblBidderStat;
    @FXML private Label lblSellerStat;

    // ===== Chart area =====
    @FXML private ComboBox<Auction> cmbAuctionPicker;
    @FXML private Button btnRefreshChart;
    @FXML private VBox boxChartEmpty;
    @FXML private Label lblChartEmpty;
    @FXML private LineChart<String, Number> bidChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private HBox boxChartStats;
    @FXML private Label lblChartStart;
    @FXML private Label lblChartCurrent;
    @FXML private Label lblChartIncrease;
    @FXML private Label lblChartBidCount;

    // ===== Message =====
    @FXML private Label lblMessage;

    // ===== State =====
    private final ObservableList<Auction> allAuctions = FXCollections.observableArrayList();
    private Auction selectedChartAuction;
    private Timeline autoRefreshTimer;

    private static final NumberFormat MONEY =
            NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    void initialize() {
        User user = Session.getInstance().getLoggedInUser();
        if (user == null) {
            SceneManager.getInstance().switchScene("login.fxml", "Đăng nhập");
            return;
        }

        // Sidebar / banner
        String displayName = user.getFullName() != null && !user.getFullName().isEmpty()
                ? user.getFullName() : user.getUsername();
        lblUser.setText(displayName);
        lblWelcome.setText("Xin chào, " + displayName + "! 👋");

        // ComboBox: hiển thị tên item + số bid
        cmbAuctionPicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(Auction a) {
                if (a == null) return "";
                return String.format("%s (%d bid)",
                        a.getItem().getName(), a.getBidCount());
            }
            @Override
            public Auction fromString(String s) { return null; }
        });
        cmbAuctionPicker.valueProperty().addListener((obs, oldA, newA) -> {
            if (newA != null && newA != selectedChartAuction) {
                selectedChartAuction = newA;
                renderChart();
            }
        });

        // Lắng nghe notification từ server (Observer pattern qua socket)
        registerServerNotificationHandler();

        // Auto-refresh: mỗi 5s reload dữ liệu nếu phiên đang chọn đang RUNNING
        startAutoRefreshTimer();

        // Lần tải đầu tiên
        loadAuctions();
    }

    // ============================================
    //   LOAD DATA
    // ============================================
    private void loadAuctions() {
        new Thread(() -> {
            try {
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) client.connect();

                Response res = client.sendRequest(new GetAuctionRequest());
                if (res != null && res.isOk()) {
                    SuccessResponse ok = (SuccessResponse) res;
                    @SuppressWarnings("unchecked")
                    List<Auction> auctions = (List<Auction>) ok.getData();
                    Platform.runLater(() -> onAuctionsLoaded(auctions));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        showMessage("Không tải được dữ liệu: " + e.getMessage(), true));
            }
        }, "Home-Load-Auctions").start();
    }

    private void onAuctionsLoaded(List<Auction> auctions) {
        if (auctions == null) auctions = List.of();
        allAuctions.setAll(auctions);

        // Stats tổng quan
        long running = auctions.stream()
                .filter(a -> a.getState() == AuctionState.RUNNING).count();
        long open = auctions.stream()
                .filter(a -> a.getState() == AuctionState.OPEN).count();
        long ended = auctions.stream()
                .filter(a -> a.getState() == AuctionState.FINISHED
                        || a.getState() == AuctionState.PAID
                        || a.getState() == AuctionState.CANCELED).count();
        lblStats.setText(String.format(
                "Đang chạy: %d | Sắp diễn ra: %d | Đã kết thúc: %d",
                running, open, ended));
        lblBidderStat.setText(String.valueOf(running));

        String username = Session.getInstance().getLoggedInUser().getUsername();
        long mine = auctions.stream()
                .filter(a -> a.getSellerId() != null && a.getSellerId().equals(username))
                .count();
        lblSellerStat.setText(String.valueOf(mine));

        // Lọc các phiên có thể hiển thị biểu đồ:
        // - đang RUNNING, FINISHED, hoặc PAID
        // - đã có ít nhất 1 bid
        List<Auction> chartable = auctions.stream()
                .filter(a -> a.getState() == AuctionState.RUNNING
                        || a.getState() == AuctionState.FINISHED
                        || a.getState() == AuctionState.PAID)
                .filter(a -> a.getBidCount() > 0)
                .sorted(Comparator.comparingInt(Auction::getBidCount).reversed())
                .toList();

        populateComboBox(chartable);
    }

    private void populateComboBox(List<Auction> chartable) {
        cmbAuctionPicker.getItems().setAll(chartable);

        if (chartable.isEmpty()) {
            // Hiện empty state
            selectedChartAuction = null;
            bidChart.setVisible(false);
            bidChart.setManaged(false);
            boxChartStats.setVisible(false);
            boxChartStats.setManaged(false);
            boxChartEmpty.setVisible(true);
            boxChartEmpty.setManaged(true);
            lblChartEmpty.setText("Chưa có phiên đấu giá nào có bid để hiển thị biểu đồ.\n"
                    + "Hãy quay lại sau khi có người đặt giá!");
            return;
        }

        // Có dữ liệu - chọn lại phiên đang xem (nếu vẫn còn) hoặc mặc định phiên đầu (HOT nhất)
        Auction toSelect = null;
        if (selectedChartAuction != null) {
            for (Auction a : chartable) {
                if (a.getId().equals(selectedChartAuction.getId())) {
                    toSelect = a;
                    break;
                }
            }
        }
        if (toSelect == null) toSelect = chartable.get(0);

        selectedChartAuction = toSelect;
        cmbAuctionPicker.setValue(toSelect);
        renderChart();
    }

    // ============================================
    //   CHART RENDERING
    // ============================================
    private void renderChart() {
        if (selectedChartAuction == null) return;

        Auction a = selectedChartAuction;
        List<BidTransaction> bids = new ArrayList<>(a.getBidHistory());
        bids.sort(Comparator.comparing(BidTransaction::getTimestamp));

        if (bids.isEmpty()) {
            boxChartEmpty.setVisible(true);
            boxChartEmpty.setManaged(true);
            bidChart.setVisible(false);
            bidChart.setManaged(false);
            boxChartStats.setVisible(false);
            boxChartStats.setManaged(false);
            return;
        }

        // Tạo series với điểm đầu là "Khởi điểm" (starting price)
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(a.getItem().getName());

        series.getData().add(new XYChart.Data<>("Khởi điểm", a.getStartingPrice()));
        for (BidTransaction bid : bids) {
            String time = bid.getTimestamp().format(TIME_FMT);
            series.getData().add(new XYChart.Data<>(time, bid.getAmount()));
        }

        // Update title + clear series cũ
        bidChart.setTitle("Diễn biến giá: " + a.getItem().getName());
        bidChart.getData().clear();
        bidChart.getData().add(series);

        // Style line + symbol bằng CSS inline trên các node (sau khi đã add vào chart)
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    Tooltip tip = new Tooltip(d.getXValue() + "\n" + formatMoney(d.getYValue().doubleValue()));
                    Tooltip.install(d.getNode(), tip);
                }
            }
        });

        // Hiện chart, ẩn empty state
        bidChart.setVisible(true);
        bidChart.setManaged(true);
        boxChartEmpty.setVisible(false);
        boxChartEmpty.setManaged(false);

        // Update stats bên dưới
        double startPrice = a.getStartingPrice();
        double currentPrice = a.getCurrentHighestBid();
        double increasePct = startPrice > 0 ? ((currentPrice - startPrice) / startPrice) * 100 : 0;

        lblChartStart.setText(formatMoney(startPrice));
        lblChartCurrent.setText(formatMoney(currentPrice));
        lblChartIncrease.setText(String.format("+%.1f%%", increasePct));
        lblChartBidCount.setText(String.valueOf(bids.size()));

        boxChartStats.setVisible(true);
        boxChartStats.setManaged(true);
    }

    @FXML
    private void handleRefreshChart() {
        loadAuctions();
    }

    // ============================================
    //   REALTIME UPDATE
    // ============================================
    /**
     * Đăng ký listener nhận notification BID_UPDATE / AUCTION_CREATED từ server.
     * Khi có bid mới hoặc phiên mới, tự reload danh sách.
     */
    private void registerServerNotificationHandler() {
        AuctionClient client = Session.getInstance().getClient();
        if (client == null) return;
        client.setOnNotification(notification -> Platform.runLater(this::loadAuctions));
    }

    /**
     * Auto refresh chart mỗi 5 giây nếu phiên đang được xem là RUNNING.
     * Đề phòng trường hợp notification từ server không đến được (mất kết nối tạm).
     */
    private void startAutoRefreshTimer() {
        autoRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (selectedChartAuction != null
                    && selectedChartAuction.getState() == AuctionState.RUNNING) {
                loadAuctions();
            }
        }));
        autoRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimer.play();
    }

    private void stopTimer() {
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
    }

    // ============================================
    //   NAVIGATION
    // ============================================
    @FXML
    private void handleGoBidder() {
        stopTimer();
        SceneManager.getInstance().switchScene(
                "bidder/auction-list.fxml", "Tham gia đấu giá");
    }

    @FXML
    private void handleGoSeller() {
        stopTimer();
        SceneManager.getInstance().switchScene(
                "seller/my-auction.fxml", "Phiên đấu giá của tôi");
    }

    @FXML
    private void handleAccount() {
        showInfo("Tính năng quản lý tài khoản đang được phát triển.");
    }

    @FXML
    private void handleSupport() {
        showInfo("Liên hệ hỗ trợ: support@auction.local\nGiờ làm việc: 7:00 - 19:00 mỗi ngày");
    }
    @FXML
    private void handleSwitchRole() {
        stopTimer();
        SceneManager.getInstance().switchScene(
                "seller/my-auction.fxml", "Phiên đấu giá của tôi");
    }
    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn đăng xuất?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            stopTimer();
            Session.getInstance().clear();
            SceneManager.getInstance().switchScene(
                    "login.fxml", "Đăng nhập - Hệ thống đấu giá");
        }
    }

    // ============================================
    //   HELPERS
    // ============================================
    private String formatMoney(double value) {
        return MONEY.format(value) + " ₫";
    }

    private void showMessage(String msg, boolean isError) {
        lblMessage.setStyle("-fx-padding: 8 30; -fx-text-fill: "
                + (isError ? "#e74c3c" : "#27ae60") + ";");
        lblMessage.setText(msg);

        new Timeline(new KeyFrame(Duration.seconds(5),
                e -> lblMessage.setText(""))).play();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }


