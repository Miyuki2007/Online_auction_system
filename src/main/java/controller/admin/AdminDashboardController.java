package controller.admin;

import client.AuctionClient;
import client.Session;
import controller.SceneManager;
import dao.AdminStats;
import dao.UserSummary;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.auction.Auction;
import model.auction.AuctionState;
import model.user.User;
import protocol.Response;
import protocol.requests.AdminForceCancelAuctionRequest;
import protocol.requests.AdminGetAllUsersRequest;
import protocol.requests.AdminGetStatsRequest;
import protocol.requests.AdminSetUserActiveRequest;
import protocol.requests.GetAuctionRequest;
import protocol.responses.SuccessResponse;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controller cho màn hình Admin Dashboard.
 *
 * 3 màn hình con (chuyển bằng menu trên sidebar):
 *  1. Tổng quan: hiển thị các số liệu thống kê (AdminStats)
 *  2. Quản lý user: bảng danh sách user + nút khóa/mở khóa
 *  3. Quản lý phiên: bảng danh sách auction + nút force-cancel
 *
 * Implementation: dùng TabPane bên trong (đã ẩn header bằng CSS),
 * các nút trên sidebar điều khiển tab thông qua tabPane.getSelectionModel().
 *
 * Mọi request đến server đều được verify quyền Admin ở phía server
 * (xem ClientHandler.verifyAdmin). Controller chỉ là tầng presentation.
 */
public class AdminDashboardController {

    // ===== Sidebar / Top =====
    @FXML private Label lblAdminName;
    @FXML private Label lblLastUpdate;
    @FXML private Label lblMessage;
    @FXML private Label lblPageTitle;

    // ===== Sidebar navigation buttons =====
    @FXML private Button btnTabOverview;
    @FXML private Button btnTabUsers;
    @FXML private Button btnTabAuctions;

    // ===== TabPane (header đã ẩn bằng CSS) =====
    @FXML private TabPane tabPane;

    // ===== TAB 1: Stats =====
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalBidders;
    @FXML private Label lblTotalSellers;
    @FXML private Label lblActiveUsers;
    @FXML private Label lblLockedUsers;
    @FXML private Label lblTotalAuctions;
    @FXML private Label lblRunningAuctions;
    @FXML private Label lblFinishedAuctions;
    @FXML private Label lblCanceledAuctions;
    @FXML private Label lblPaidAuctions;
    @FXML private Label lblTotalBidVolume;
    @FXML private Label lblTotalBidCount;

    // ===== TAB 2: User table =====
    @FXML private TableView<UserRow> tblUsers;
    @FXML private TableColumn<UserRow, String> colUserId;
    @FXML private TableColumn<UserRow, String> colUsername;
    @FXML private TableColumn<UserRow, String> colFullName;
    @FXML private TableColumn<UserRow, String> colEmail;
    @FXML private TableColumn<UserRow, String> colRole;
    @FXML private TableColumn<UserRow, String> colStatus;
    @FXML private TableColumn<UserRow, String> colCreatedAt;
    @FXML private Button btnToggleLock;

    // ===== TAB 3: Auction table =====
    @FXML private TableView<AuctionRow> tblAuctions;
    @FXML private TableColumn<AuctionRow, String> colAuctionId;
    @FXML private TableColumn<AuctionRow, String> colItemName;
    @FXML private TableColumn<AuctionRow, String> colSeller;
    @FXML private TableColumn<AuctionRow, String> colCurrentPrice;
    @FXML private TableColumn<AuctionRow, String> colBidCount;
    @FXML private TableColumn<AuctionRow, String> colState;
    @FXML private TableColumn<AuctionRow, String> colEndTime;
    @FXML private Button btnForceCancel;

    // ===== State =====
    private final ObservableList<UserRow> usersData = FXCollections.observableArrayList();
    private final ObservableList<AuctionRow> auctionsData = FXCollections.observableArrayList();

    private static final NumberFormat MONEY = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    void initialize() {
        User u = Session.getInstance().getLoggedInUser();
        if (u == null || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            Platform.runLater(() ->
                    SceneManager.getInstance().switchScene("login.fxml", "Đăng nhập"));
            return;
        }
        lblAdminName.setText(u.getFullName() != null ? u.getFullName() : u.getUsername());

        setupUserTable();
        setupAuctionTable();

        // Mặc định mở tab Tổng quan
        selectTab(0);

        refreshAll();
    }

    // ============================================
    //   SIDEBAR NAVIGATION
    // ============================================
    @FXML
    void handleSelectOverview() { selectTab(0); }

    @FXML
    void handleSelectUsers() { selectTab(1); }

    @FXML
    void handleSelectAuctions() { selectTab(2); }

    /**
     * Chuyển tab và cập nhật trạng thái active của các nút sidebar.
     * Nút active có thêm style class "sidebar-btn-active" (định nghĩa trong dashboard.css).
     */
    private void selectTab(int index) {
        if (tabPane != null) {
            tabPane.getSelectionModel().select(index);
        }
        // Cập nhật tiêu đề và highlight nút tương ứng
        // Trước hết, gỡ active khỏi tất cả các nút
        removeActive(btnTabOverview);
        removeActive(btnTabUsers);
        removeActive(btnTabAuctions);

        switch (index) {
            case 0 -> {
                addActive(btnTabOverview);
                if (lblPageTitle != null) lblPageTitle.setText("📊 Tổng quan");
            }
            case 1 -> {
                addActive(btnTabUsers);
                if (lblPageTitle != null) lblPageTitle.setText("👥 Quản lý người dùng");
            }
            case 2 -> {
                addActive(btnTabAuctions);
                if (lblPageTitle != null) lblPageTitle.setText("🏷 Quản lý phiên đấu giá");
            }
        }
    }

    private void addActive(Button btn) {
        if (btn == null) return;
        if (!btn.getStyleClass().contains("sidebar-btn-active")) {
            btn.getStyleClass().add("sidebar-btn-active");
        }
    }

    private void removeActive(Button btn) {
        if (btn == null) return;
        btn.getStyleClass().remove("sidebar-btn-active");
    }

    // ============================================
    //   TABLE SETUP
    // ============================================
    private void setupUserTable() {
        colUserId.setCellValueFactory(c -> c.getValue().userIdProperty());
        colUsername.setCellValueFactory(c -> c.getValue().usernameProperty());
        colFullName.setCellValueFactory(c -> c.getValue().fullNameProperty());
        colEmail.setCellValueFactory(c -> c.getValue().emailProperty());
        colRole.setCellValueFactory(c -> c.getValue().roleProperty());
        colStatus.setCellValueFactory(c -> c.getValue().statusProperty());
        colCreatedAt.setCellValueFactory(c -> c.getValue().createdAtProperty());
        tblUsers.setItems(usersData);

        tblUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                btnToggleLock.setDisable(true);
                btnToggleLock.setText("Khóa / Mở khóa user");
            } else if ("ADMIN".equalsIgnoreCase(newV.getRole())) {
                btnToggleLock.setDisable(true);
                btnToggleLock.setText("Không thể khóa Admin");
            } else {
                btnToggleLock.setDisable(false);
                btnToggleLock.setText(newV.isActive()
                        ? "🔒 Khóa user này"
                        : "🔓 Mở khóa user này");
            }
        });
    }

    private void setupAuctionTable() {
        colAuctionId.setCellValueFactory(c -> c.getValue().auctionIdProperty());
        colItemName.setCellValueFactory(c -> c.getValue().itemNameProperty());
        colSeller.setCellValueFactory(c -> c.getValue().sellerProperty());
        colCurrentPrice.setCellValueFactory(c -> c.getValue().currentPriceProperty());
        colBidCount.setCellValueFactory(c -> c.getValue().bidCountProperty());
        colState.setCellValueFactory(c -> c.getValue().stateProperty());
        colEndTime.setCellValueFactory(c -> c.getValue().endTimeProperty());
        tblAuctions.setItems(auctionsData);

        tblAuctions.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                btnForceCancel.setDisable(true);
            } else {
                String s = newV.getState();
                btnForceCancel.setDisable(!("RUNNING".equals(s) || "OPEN".equals(s)));
            }
        });
    }

    // ============================================
    //   LOAD DATA
    // ============================================
    @FXML
    void handleRefresh() {
        refreshAll();
    }

    private void refreshAll() {
        loadStats();
        loadUsers();
        loadAuctions();
        lblLastUpdate.setText("Cập nhật: " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void loadStats() {
        new Thread(() -> {
            try {
                User admin = Session.getInstance().getLoggedInUser();
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) client.connect();

                Response res = client.sendRequest(
                        new AdminGetStatsRequest(admin.getUsername()));
                if (res != null && res.isOk()) {
                    SuccessResponse ok = (SuccessResponse) res;
                    AdminStats s = ok.getDataAs(AdminStats.class);
                    Platform.runLater(() -> renderStats(s));
                } else {
                    Platform.runLater(() -> showMessage(
                            "Không tải được thống kê: " +
                                    (res != null ? res.getMessage() : "không phản hồi"), true));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showMessage("Lỗi: " + e.getMessage(), true));
            }
        }, "Admin-LoadStats").start();
    }

    private void renderStats(AdminStats s) {
        if (s == null) return;
        lblTotalUsers.setText(String.valueOf(s.getTotalUsers()));
        lblTotalBidders.setText(String.valueOf(s.getTotalBidders()));
        lblTotalSellers.setText(String.valueOf(s.getTotalSellers()));
        lblActiveUsers.setText(String.valueOf(s.getActiveUsers()));
        lblLockedUsers.setText(String.valueOf(s.getLockedUsers()));
        lblTotalAuctions.setText(String.valueOf(s.getTotalAuctions()));
        lblRunningAuctions.setText(String.valueOf(s.getRunningAuctions()));
        lblFinishedAuctions.setText(String.valueOf(s.getFinishedAuctions()));
        lblCanceledAuctions.setText(String.valueOf(s.getCanceledAuctions()));
        lblPaidAuctions.setText(String.valueOf(s.getPaidAuctions()));
        lblTotalBidVolume.setText(MONEY.format(s.getTotalBidVolume()) + " ₫");
        lblTotalBidCount.setText(String.valueOf(s.getTotalBidCount()));
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        new Thread(() -> {
            try {
                User admin = Session.getInstance().getLoggedInUser();
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) client.connect();

                Response res = client.sendRequest(
                        new AdminGetAllUsersRequest(admin.getUsername()));
                if (res != null && res.isOk()) {
                    SuccessResponse ok = (SuccessResponse) res;
                    List<UserSummary> list = (List<UserSummary>) ok.getData();
                    Platform.runLater(() -> {
                        usersData.clear();
                        for (UserSummary u : list) {
                            usersData.add(new UserRow(u));
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showMessage(
                        "Lỗi tải danh sách user: " + e.getMessage(), true));
            }
        }, "Admin-LoadUsers").start();
    }

    @SuppressWarnings("unchecked")
    private void loadAuctions() {
        new Thread(() -> {
            try {
                AuctionClient client = Session.getInstance().getClient();
                if (!client.isConnected()) client.connect();

                Response res = client.sendRequest(new GetAuctionRequest());
                if (res != null && res.isOk()) {
                    SuccessResponse ok = (SuccessResponse) res;
                    List<Auction> list = (List<Auction>) ok.getData();
                    Platform.runLater(() -> {
                        auctionsData.clear();
                        for (Auction a : list) {
                            auctionsData.add(new AuctionRow(a));
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showMessage(
                        "Lỗi tải danh sách phiên: " + e.getMessage(), true));
            }
        }, "Admin-LoadAuctions").start();
    }

    // ============================================
    //   ACTIONS
    // ============================================
    @FXML
    void handleToggleLock() {
        UserRow row = tblUsers.getSelectionModel().getSelectedItem();
        if (row == null) return;
        if ("ADMIN".equalsIgnoreCase(row.getRole())) {
            showMessage("Không thể khóa tài khoản Admin.", true);
            return;
        }
        boolean newActive = !row.isActive();
        String action = newActive ? "MỞ KHÓA" : "KHÓA";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                String.format("Xác nhận %s user \"%s\" (%s)?",
                        action, row.getUsername(), row.getFullName()),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận thao tác");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        btnToggleLock.setDisable(true);
        new Thread(() -> {
            try {
                User admin = Session.getInstance().getLoggedInUser();
                AuctionClient client = Session.getInstance().getClient();
                Response res = client.sendRequest(
                        new AdminSetUserActiveRequest(
                                admin.getUsername(), row.getUserIdInt(), newActive));
                Platform.runLater(() -> {
                    if (res != null && res.isOk()) {
                        showMessage("✅ " + res.getMessage(), false);
                        loadUsers();
                        loadStats();
                    } else {
                        showMessage("❌ " +
                                (res != null ? res.getMessage() : "Không phản hồi"), true);
                        btnToggleLock.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Lỗi: " + e.getMessage(), true);
                    btnToggleLock.setDisable(false);
                });
            }
        }, "Admin-Toggle").start();
    }

    @FXML
    void handleForceCancel() {
        AuctionRow row = tblAuctions.getSelectionModel().getSelectedItem();
        if (row == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Hủy phiên đấu giá");
        dialog.setHeaderText("Hủy phiên: " + row.getItemName());
        dialog.setContentText("Lý do (tùy chọn):");
        Optional<String> reason = dialog.showAndWait();
        if (reason.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                String.format(
                        "Xác nhận HỦY phiên đấu giá \"%s\"?%n" +
                                "Thao tác này không thể hoàn tác.",
                        row.getItemName()),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận hủy");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        btnForceCancel.setDisable(true);
        new Thread(() -> {
            try {
                User admin = Session.getInstance().getLoggedInUser();
                AuctionClient client = Session.getInstance().getClient();
                Response res = client.sendRequest(
                        new AdminForceCancelAuctionRequest(
                                admin.getUsername(), row.getAuctionIdRaw(), reason.get()));
                Platform.runLater(() -> {
                    if (res != null && res.isOk()) {
                        showMessage("✅ " + res.getMessage(), false);
                        loadAuctions();
                        loadStats();
                    } else {
                        showMessage("❌ " +
                                (res != null ? res.getMessage() : "Không phản hồi"), true);
                        btnForceCancel.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showMessage("Lỗi: " + e.getMessage(), true);
                    btnForceCancel.setDisable(false);
                });
            }
        }, "Admin-ForceCancel").start();
    }

    @FXML
    void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn đăng xuất?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            Session.getInstance().clear();
            SceneManager.getInstance().switchScene(
                    "login.fxml", "Đăng nhập - Hệ thống đấu giá");
        }
    }

    // ============================================
    //   HELPERS
    // ============================================
    private void showMessage(String msg, boolean isError) {
        lblMessage.setStyle("-fx-padding: 8 14; -fx-text-fill: "
                + (isError ? "#e74c3c" : "#27ae60") + "; -fx-font-weight: bold;");
        lblMessage.setText(msg);
    }

    // ============================================
    //   INNER CLASSES (Table Row Models)
    // ============================================
    public static class UserRow {
        private final UserSummary src;
        private final SimpleStringProperty userId, username, fullName, email, role, status, createdAt;

        public UserRow(UserSummary s) {
            this.src = s;
            this.userId = new SimpleStringProperty(String.valueOf(s.getUserId()));
            this.username = new SimpleStringProperty(s.getUsername());
            this.fullName = new SimpleStringProperty(s.getFullName() != null ? s.getFullName() : "");
            this.email = new SimpleStringProperty(s.getEmail());
            this.role = new SimpleStringProperty(s.getRole());
            this.status = new SimpleStringProperty(s.isActive() ? "🟢 Hoạt động" : "🔴 Đã khóa");
            this.createdAt = new SimpleStringProperty(
                    s.getCreatedAt() != null ? s.getCreatedAt().format(DATE_FMT) : "");
        }

        public SimpleStringProperty userIdProperty() { return userId; }
        public SimpleStringProperty usernameProperty() { return username; }
        public SimpleStringProperty fullNameProperty() { return fullName; }
        public SimpleStringProperty emailProperty() { return email; }
        public SimpleStringProperty roleProperty() { return role; }
        public SimpleStringProperty statusProperty() { return status; }
        public SimpleStringProperty createdAtProperty() { return createdAt; }

        public int getUserIdInt() { return src.getUserId(); }
        public String getUsername() { return src.getUsername(); }
        public String getFullName() { return src.getFullName() != null ? src.getFullName() : ""; }
        public String getRole() { return src.getRole(); }
        public boolean isActive() { return src.isActive(); }
    }

    public static class AuctionRow {
        private final String auctionIdRaw;
        private final String itemNameRaw;
        private final String stateRaw;
        private final SimpleStringProperty auctionId, itemName, seller, currentPrice, bidCount, state, endTime;

        public AuctionRow(Auction a) {
            this.auctionIdRaw = a.getId();
            this.itemNameRaw = a.getItem().getName();
            this.stateRaw = a.getState().name();

            this.auctionId = new SimpleStringProperty(
                    a.getId().length() > 8 ? a.getId().substring(0, 8) + "..." : a.getId());
            this.itemName = new SimpleStringProperty(a.getItem().getName());
            this.seller = new SimpleStringProperty(a.getSellerId());
            this.currentPrice = new SimpleStringProperty(MONEY.format(a.getCurrentHighestBid()) + " ₫");
            this.bidCount = new SimpleStringProperty(String.valueOf(a.getBidCount()));
            this.state = new SimpleStringProperty(stateLabel(a.getState()));
            this.endTime = new SimpleStringProperty(
                    a.getEndTime() != null ? a.getEndTime().format(DATE_FMT) : "");
        }

        private static String stateLabel(AuctionState s) {
            return switch (s) {
                case OPEN -> "🟡 Sắp diễn ra";
                case RUNNING -> "🟢 Đang chạy";
                case FINISHED -> "⚫ Đã kết thúc";
                case PAID -> "💰 Đã thanh toán";
                case CANCELED -> "🔴 Đã hủy";
            };
        }

        public SimpleStringProperty auctionIdProperty() { return auctionId; }
        public SimpleStringProperty itemNameProperty() { return itemName; }
        public SimpleStringProperty sellerProperty() { return seller; }
        public SimpleStringProperty currentPriceProperty() { return currentPrice; }
        public SimpleStringProperty bidCountProperty() { return bidCount; }
        public SimpleStringProperty stateProperty() { return state; }
        public SimpleStringProperty endTimeProperty() { return endTime; }

        public String getAuctionIdRaw() { return auctionIdRaw; }
        public String getItemName() { return itemNameRaw; }
        public String getState() { return stateRaw; }
    }
}
