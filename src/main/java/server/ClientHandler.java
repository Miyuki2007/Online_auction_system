package server;

import model.auction.Auction;
import model.auction.AutoBid;
import model.factory.ItemFactory;
import model.item.Item;
import model.manager.AuctionManager;
import model.manager.AutoBidManager;
import model.user.*;
import model.auction.BidTransaction;
import protocol.Request;
import protocol.Response;
import protocol.requests.*;
import protocol.responses.ErrorResponse;
import protocol.responses.MyBidHistoryResponse;
import protocol.responses.NotificationResponse;
import protocol.responses.SuccessResponse;
import protocol.requests.RegisterAutoBidRequest;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuctionServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String watchAuctionId;
    private volatile String loggedInUsername;
    public String getLoggedInUsername() { return loggedInUsername;}
    private static final java.util.Map<String,java.util.concurrent.atomic.AtomicInteger> FAILED_ATTEMPTS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String,Long> LOCKED_UNTIL = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 5*60_000L; // 5 phút
    public ClientHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            in.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(
                    "maxbytes=10485760;"           // tối đa 10MB (đủ chỗ cho ảnh 5MB + overhead)
                            + "maxdepth=20;"
                            + "maxrefs=10000;"
                            + "java.base/*;"               // các class cơ bản: String, Number, ArrayList, ...
                            + "java.time.**;"              // LocalDateTime, Duration
                            + "protocol.**;"
                            + "model.**;"
                            + "dao.**;"
                            + "!*"                         // reject mọi class khác
            ));
            socket.setSoTimeout(120_000);
            while (true) {
                Request request = (Request) in.readObject();
                Response response = handleRequest(request);
                sendResponse(response);
            }
        } catch (EOFException e) {
            System.out.println("Client ngắt kết nối.");
        } catch (Exception e) {
            System.err.println("Lỗi xử lý client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            server.removeClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private Response handleRequest(Request request) {
        AuctionManager manager = AuctionManager.getInstance();

        try {
            // ✅ Sử dụng instanceof pattern matching (Java 16+)
            if (request instanceof LoginRequest req) {
                return handleLogin(req, manager);
            }
            if (request instanceof RegisterRequest req) {
                return handleRegister(req, manager);
            }
            if (request instanceof GetAuctionRequest) {
                return handleGetAuctions(manager);
            }
            if (request instanceof GetAuctionDetailRequest req) {
                return handleGetAuctionDetail(req, manager);
            }
            if (request instanceof PlaceBidRequest req) {
                return handlePlaceBid(req, manager);
            }
            if (request instanceof CreateAuctionRequest req) {
                return handleCreateAuction(req, manager);
            }
            if (request instanceof CancelAuctionRequest req) {
                return handleCancelAuction(req, manager);
            }
            if (request instanceof GetMyAuctionRequest req) {
                return handleGetMyAuctions(req, manager);
            }
            if (request instanceof RegisterAutoBidRequest req) {
                return handleRegisterAutoBid(req, manager);
            }
            if (request instanceof AdminGetAllUsersRequest req){
                return handleAdminGetAllUsers(req);
            }
            if (request instanceof AdminSetUserActiveRequest req){
                return handleAdminSetUserActive(req);
            }
            if (request instanceof AdminForceCancelAuctionRequest req){
                return handleAdminForceCancelAuction(req,manager);
            }
            if (request instanceof AdminGetStatsRequest req){
                return handleAdminGetStats(req);
            }
            if (request instanceof GetMyBidHistoryRequest req){
                return handleGetMyBidHistory(req,manager);
            }
            if (request instanceof CancelAutoBidRequest req){
                return handleCancelAutoBid(req);
            }
            return new ErrorResponse("Yêu cầu không được hỗ trợ: "
                    + request.getType());
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getSimpleName();
            return new ErrorResponse(msg);
        }
    }

    // ========== HANDLERS ==========

    private Response handleLogin(LoginRequest req, AuctionManager manager) {
        String u = req.getUsername();
        if (u == null || u.isBlank()){
            return new ErrorResponse("Tên đăng nhập không hợp lệ");
        }
        Long lockEnd = LOCKED_UNTIL.get(u);
        if (lockEnd != null && lockEnd > System.currentTimeMillis()){
            long remainSec = (lockEnd - System.currentTimeMillis()) / 1000;
            return new ErrorResponse("Tài khoản bị tạm khóa do nhập sai nhiều lần. " + "Thử lại sau " + remainSec + "giây.");
        }
        try
        {
            User user = manager.authenticateUser(u,req.getPassword());
            FAILED_ATTEMPTS.remove(u);
            LOCKED_UNTIL.remove(u);
            if (user!=null){
                this.loggedInUsername = user.getUsername();
            }
            return new SuccessResponse("Đăng nhập thành công.",user);
        } catch(model.auction.exception.AuthenticationException e){
            int n = FAILED_ATTEMPTS.computeIfAbsent(u,
                    k -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
            if (n>=MAX_ATTEMPTS){
                LOCKED_UNTIL.put(u,System.currentTimeMillis() + LOCK_DURATION_MS);
                FAILED_ATTEMPTS.remove(u);
                return new ErrorResponse("Đã sai " + MAX_ATTEMPTS + " lần." + "Tài khoản bị khóa 5 phút.");
            }
            return new ErrorResponse(e.getMessage() + " (còn " + (MAX_ATTEMPTS-n) + " lần thử)");
        }
    }

    private Response handleRegister(RegisterRequest req, AuctionManager manager) {
        dao.UserDAO userDAO = new dao.UserDAO();

        // 1. Kiểm tra trùng tên đăng nhập
        if (userDAO.checkUsernameExist(req.getUsername())) {
            return new ErrorResponse("Tên đăng nhập đã tồn tại! Vui lòng chọn tên khác.");
        }
        // 2. Kiểm tra trùng email
        if (userDAO.checkEmailExist(req.getEmail())) {
            return new ErrorResponse("Email này đã được đăng ký! Vui lòng sử dụng email khác.");
        }
        User user;
        switch (req.getRole().toUpperCase()) {
            case "BIDDER":
                user = new Bidder(req.getUsername(), req.getPassword(),
                        req.getEmail(), req.getFullName());
                break;
            case "SELLER":
                user = new Seller(req.getUsername(), req.getPassword(),
                        req.getEmail(), req.getFullName());
                break;
            case "ADMIN":
                return new ErrorResponse("Không thể đăng kí tài khoản Admin qua form." + "Vui lòng liên hệ quản trị viên hệ thống.");
            default:
                return new ErrorResponse("Role không hợp lệ: " + req.getRole());
        }
        manager.registerUser(user);
        return new SuccessResponse("Đăng ký thành công.", user);
    }

    private Response handleGetAuctions(AuctionManager manager) {
        List<Auction> auctions = manager.getActiveAuctions();
        return new SuccessResponse("Danh sách phiên đấu giá.", auctions);
    }

    private Response handleGetAuctionDetail(GetAuctionDetailRequest req,
                                            AuctionManager manager) {
        Auction auction = manager.findAuctionById(req.getAuctionId());
        if (auction == null) {
            return new ErrorResponse("Không tìm thấy phiên đấu giá.");
        }
        this.watchAuctionId = auction.getId();
        return new SuccessResponse("Chi tiết phiên đấu giá.", auction);
    }

    private Response handlePlaceBid(PlaceBidRequest req, AuctionManager manager) {
        if (loggedInUsername == null) return new ErrorResponse("Chưa đăng nhập");
        double amt = req.getAmount();
        if (Double.isNaN(amt) || Double.isInfinite(amt) || amt<=0 || amt > 1_000_000_000_000.0){
            return new ErrorResponse("Số tiền không hợp lệ");
        }
        // Kiểm tra seller không được tự đấu giá phiên của chính mình
        Auction targetAuction = manager.findAuctionById(req.getAuctionId());
        if (targetAuction == null) {
            return new ErrorResponse("Không tìm thấy phiên đấu giá.");
        }
        if (loggedInUsername.equals(targetAuction.getSellerId())) {
            return new ErrorResponse("Người bán không thể đặt giá trong chính phiên đấu giá của mình.");
        }

        BidTransaction bid = manager.placeBid(
                req.getAuctionId(),
                loggedInUsername,
                req.getAmount());

        server.broadcastToAuction(req.getAuctionId(),
                new NotificationResponse(
                        NotificationResponse.NotificationType.BID_UPDATE,
                        "Có bid mới", bid));

        return new SuccessResponse("Đặt giá thành công!", bid);
    }

    private Response handleCreateAuction(CreateAuctionRequest req,
                                         AuctionManager manager) {
        if (loggedInUsername == null) return new ErrorResponse("Chưa đăng nhập.");
        //--- VALIDATE INPUT ---
        if (req.getStartingPrice()<=0 || Double.isNaN(req.getStartingPrice()) || Double.isInfinite(req.getStartingPrice()) || req.getStartingPrice()>1_000_000_000_000.0 ){
            return new ErrorResponse("Giá thời điểm không hợp lệ.");
        }
        if (req.getDurationMinutes()<=0 || req.getDurationMinutes() > 60L*24*30){
            return new ErrorResponse("Thời lượng phải từ 1 phút đến 30 ngày");
        }
        if (req.getItemName() == null || req.getItemName().isBlank() || req.getItemName().length() > 255){
            return new ErrorResponse("Tên sản phẩm không hợp lệ");
        }
        if (req.getItemDescription() != null && req.getItemDescription().length()>5000){
            return new ErrorResponse("Mô tả quá dài (Tối đa 5000 ký tự).");
        }
        if (req.getImageData() != null && req.getImageData().length>5_000_000){
            return new ErrorResponse("Ảnh quá lớn (tối đa 5MB)");
        }
        Item item = ItemFactory.createItem(
                req.getItemType(),
                UUID.randomUUID().toString(),
                req.getItemName(),
                req.getItemDescription(),
                req.getStartingPrice(),
                req.getSpecialAttribute(),
                req.getImageData()
        );
        if (loggedInUsername == null) return new ErrorResponse("Chưa đăng nhập.");
        Auction auction = manager.createAuction(
                req.getSellerId(),
                item,
                req.getStartingPrice(),
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(req.getDurationMinutes()),
                req.isAntiSnipeEnabled(),
                req.isAntiSnipeEnabled() ? 30 : 0,
                req.isAntiSnipeEnabled() ? 60 : 0
        );
        server.broadcastToAll(new NotificationResponse(NotificationResponse.NotificationType.AUCTION_CREATED,"Có phiên đấu giá mới",auction));

        return new SuccessResponse("Tạo phiên đấu giá thành công.", auction);
    }

    private Response handleCancelAuction(CancelAuctionRequest req,
                                         AuctionManager manager) {
        Auction auction = manager.findAuctionById(req.getAuctionId());
        if (auction == null) {
            return new ErrorResponse("Không tìm thấy phiên.");
        }
        if (loggedInUsername == null) return new ErrorResponse("Chưa đăng nhập.");
        if (!auction.getSellerId().equals(loggedInUsername)) {
            return new ErrorResponse("Chỉ chủ phiên mới được cancel.");
        }
        auction.cancel();
        dao.AuctionDAO auctionDAO = new dao.AuctionDAO();
        int dbAuctionId = auctionDAO.findAuctionIdByTitleAndSeller(
                auction.getItem().getName(),
                auction.getSellerId()
        );
        if (dbAuctionId > 0) {
            boolean ok = auctionDAO.updateStatus(dbAuctionId, "CANCELED");
            if (ok) {
                System.out.println("✅ Đã hủy auction " + dbAuctionId + " trong DB.");
            }
        } else {
            System.err.println("⚠️ Không tìm thấy auction trong DB để cancel: " + auction.getId());
        }

        return new SuccessResponse("Đã hủy phiên đấu giá.", auction);
    }

    private Response handleGetMyAuctions(GetMyAuctionRequest req,
                                         AuctionManager manager) {
        if (loggedInUsername == null) return new ErrorResponse("Chưa đăng nhập.");
        List<Auction> myAuctions = manager.getActiveAuctions().stream()
                .filter(a -> a.getSellerId().equals(loggedInUsername))
                .toList();
        return new SuccessResponse("Auctions của bạn.", myAuctions);
    }
    private Response handleRegisterAutoBid(RegisterAutoBidRequest req, AuctionManager manager){
        if (loggedInUsername == null) return new ErrorResponse("Chưa đăng nhập.");
        Auction auction = manager.findAuctionById(req.getAuctionId());
        if (auction == null){
            return new ErrorResponse("Không tìm thấy phiên đấu giá");
        }
        if (loggedInUsername.equals(auction.getSellerId())){
            return new ErrorResponse("Bạn không thể autobid phiên của chính mình.");
        }
        AutoBid autoBid = AutoBidManager.getInstance().register(auction,loggedInUsername,req.getMaxBid(),req.getIncrement());
        return new SuccessResponse(
                String.format("Đã đăng kí auto-bid: max %.2f, bước nhảy %.2f", req.getMaxBid(),req.getIncrement()), autoBid);
    }
    private Response handleCancelAutoBid(CancelAutoBidRequest req){
        if (loggedInUsername == null) return new ErrorResponse("Chưa đăng nhập.");
        boolean ok = model.manager.AutoBidManager.getInstance().cancel(req.getAutoBidId(),loggedInUsername);
        if (ok){
            return new SuccessResponse("Đã hủy auto-bid thành công.", null);
        } else{
            return new ErrorResponse("Không tìm thấy auto-bid.");
        }
    }
    private boolean verifyAdmin(){
        if (loggedInUsername==null || loggedInUsername.isBlank()) return false;
        dao.UserDAO userDAO = new dao.UserDAO();
        model.user.User u = userDAO.findByUsername(loggedInUsername);
        return u instanceof model.user.Admin;
    }
    private Response handleAdminGetAllUsers (AdminGetAllUsersRequest req){
        if (!verifyAdmin()){
            return new ErrorResponse("Bạn không có quyền truy cập chức năng này.");
        }
        dao.UserDAO userDao = new dao.UserDAO();
        java.util.List<dao.UserSummary> users = userDao.getAllUsers();
        return new SuccessResponse("Danh sách user.", new java.util.ArrayList<>(users));
    }
    private Response handleAdminSetUserActive (AdminSetUserActiveRequest req){
        if (!verifyAdmin()){
            return new ErrorResponse("Bạn không có quyền truy cập chức năng này.");
        }
        dao.UserDAO userDao = new dao.UserDAO();
        boolean ok = userDao.setActive(req.getTargetUserId(),req.isActive());
        if (!ok){
            return new ErrorResponse("Không thể cập nhập trạng thái user. " +
                    "Có thể user không tồn tại");
        }
        String action = req.isActive() ? "Mở khóa" : "Khóa";
        return new SuccessResponse(action + " user thành công.", req.getTargetUserId());
    }
    private Response handleAdminForceCancelAuction(AdminForceCancelAuctionRequest req, AuctionManager manager){
        if (!verifyAdmin()){
            return new ErrorResponse("Bạn không có quyền truy cập chức năng này.");
        }
        Auction auction = manager.findAuctionById(req.getAuctionId());
        if (auction == null){
            return new ErrorResponse("Không tìm thấy phiên đấu giá nào.");
        }
        if (auction.isEnded()){
            return new ErrorResponse("Phiên đấu giá đã kết thúc, không thể hủy.");
        }
        try{
            auction.cancel();
        } catch(Exception e){
            return new ErrorResponse("Không thể hủy phiên: " + e.getMessage());
        }
        dao.AuctionDAO auctionDAO = new dao.AuctionDAO();
        int dbAuctionId = auctionDAO.findAuctionIdByTitleAndSeller(
                auction.getItem().getName(),auction.getSellerId()
        );
        if (dbAuctionId>0){
            auctionDAO.updateStatus(dbAuctionId,"CANCELED");
        }
        server.broadcastToAuction(req.getAuctionId(),
                new NotificationResponse(
                        NotificationResponse.NotificationType.STATE_CHANGED,
                        "Phiên đấu giá đã bị Admin hủy. Lí do: " +
                                (req.getReason()==null || req.getReason().isBlank()
                                        ? "không có" : req.getReason()), auction.getState()));
        System.out.println("⚠️  Admin " + req.getAdminUsername() +
                " đã dừng auction " + req.getAuctionId());
        return new SuccessResponse("Đã hủy phiên đấu giá.", auction);
    }
    private Response handleAdminGetStats(AdminGetStatsRequest req){
        if (!verifyAdmin()){
            return new ErrorResponse("Bạn không có quyền truy cập chức năng này.");
        }
        dao.UserDAO userDao = new dao.UserDAO();
        dao.AuctionDAO auctionDao = new dao.AuctionDAO();

        java.util.List<dao.UserSummary> allUsers = userDao.getAllUsers();
        int totalUsers = allUsers.size();
        int bidders = 0, sellers = 0, active = 0, locked = 0;
        for (dao.UserSummary u : allUsers){
            if ("BIDDER".equalsIgnoreCase(u.getRole())) bidders++;
            else if("SELLER".equalsIgnoreCase(u.getRole())) sellers++;
            if (u.isActive()) active++;
            else locked++;
        }

        java.util.Map<String,Integer> auctionCounts = auctionDao.countAuctionsByStatus();
        int running = auctionCounts.getOrDefault("RUNNING",0);
        int finished = auctionCounts.getOrDefault("FINISHED",0);
        int canceled = auctionCounts.getOrDefault("CANCELED",0);
        int paid = auctionCounts.getOrDefault("PAID",0);
        int open = auctionCounts.getOrDefault("OPEN",0);
        int totalAuctions = running + finished + canceled + paid + open;
        double[] volume = auctionDao.sumBidVolumeAndCount();
        dao.AdminStats stats = new dao.AdminStats(
                totalUsers,bidders,sellers,active,locked,
                totalAuctions,running,finished,canceled,paid,volume[0],(int) volume[1]);
        return new SuccessResponse("Thống kê hệ thống. ", stats);
    }
    private Response handleGetMyBidHistory(GetMyBidHistoryRequest req, AuctionManager manager){
        if (loggedInUsername == null) return new ErrorResponse("Chưa đăng nhập.");
        Auction auction = manager.findAuctionById(req.getAuctionId());
        if (auction == null) return new ErrorResponse("Không tìm thấy phiên đấu giá.");

        // Auction.id là UUID, không phải DB int — phải tra bằng title + seller (giống placeBid)
        dao.AuctionDAO auctionDAO = new dao.AuctionDAO();
        int auctionDbId = auctionDAO.findAuctionIdByTitleAndSeller(
                auction.getItem().getName(), auction.getSellerId());
        if (auctionDbId <= 0) return new ErrorResponse("Không tìm thấy phiên trong DB.");

        dao.BidDAO bidDAO = new dao.BidDAO();
        // Lấy manual bids từ DB
        List<BidTransaction> myBids = bidDAO.loadBidsByAuctionAndUser(auctionDbId, auction.getId(),loggedInUsername);
        // Lấy auto-bid đang hoạt động
        model.auction.AutoBid activAutoBid = null;
        java.util.List<model.auction.AutoBid> autoBids = model.manager.AutoBidManager.getInstance().getAutoBidsForAuction(auction.getId());
        if (autoBids != null){
            for (model.auction.AutoBid ab : autoBids){
                if (ab.isActive() && loggedInUsername.equals(ab.getBidderId())){
                    activAutoBid = ab;
                    break;
                }
            }
        }
        MyBidHistoryResponse data = new MyBidHistoryResponse(myBids,activAutoBid);
        return new SuccessResponse("Lịch sử bid của bạn.", data);
    }
    // ========== UTILITIES ==========

    public void sendResponse(Response response) {
        try {
            out.writeObject(response);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("Lỗi gửi response: " + e.getMessage());
        }
    }

    public String getWatchAuctionId() { return watchAuctionId; }
}