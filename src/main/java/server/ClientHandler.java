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

    public ClientHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

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
        User user = manager.authenticateUser(req.getUsername(), req.getPassword());
        return new SuccessResponse("Đăng nhập thành công.", user);
    }

    private Response handleRegister(RegisterRequest req, AuctionManager manager) {
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
        BidTransaction bid = manager.placeBid(
                req.getAuctionId(),
                req.getBidderId(),
                req.getAmount());

        server.broadcastToAuction(req.getAuctionId(),
                new NotificationResponse(
                        NotificationResponse.NotificationType.BID_UPDATE,
                        "Có bid mới", bid));

        return new SuccessResponse("Đặt giá thành công!", bid);
    }

    private Response handleCreateAuction(CreateAuctionRequest req,
                                         AuctionManager manager) {
        Item item = ItemFactory.createItem(
                req.getItemType(),
                UUID.randomUUID().toString(),
                req.getItemName(),
                req.getItemDescription(),
                req.getStartingPrice(),
                req.getSpecialAttribute(),
                req.getImageData()
        );

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
        if (!auction.getSellerId().equals(req.getSellerId())) {
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
        List<Auction> myAuctions = manager.getActiveAuctions().stream()
                .filter(a -> a.getSellerId().equals(req.getSellerId()))
                .toList();
        return new SuccessResponse("Auctions của bạn.", myAuctions);
    }
    private Response handleRegisterAutoBid(RegisterAutoBidRequest req, AuctionManager manager){
        Auction auction = manager.findAuctionById(req.getAuctionId());
        if (auction == null){
            return new ErrorResponse("Không tìm thấy phiên đấu giá");
        }
        AutoBid autoBid = AutoBidManager.getInstance().register(auction,req.getBidderId(),req.getMaxBid(),req.getIncrement());
        return new SuccessResponse(
                String.format("Đã đăng kí auto-bid: max %.2f, bước nhảy %.2f", req.getMaxBid(),req.getIncrement()), autoBid);

    }
    private boolean verifyAdmin(String username){
        if (username==null || username.isBlank()) return false;
        dao.UserDAO userDAO = new dao.UserDAO();
        model.user.User u = userDAO.findByUsername(username);
        return u instanceof model.user.Admin;
    }
    private Response handleAdminGetAllUsers (AdminGetAllUsersRequest req){
        if (!verifyAdmin(req.getAdminUsername())){
            return new ErrorResponse("Bạn không có quyền truy cập chức năng này.");
        }
        dao.UserDAO userDao = new dao.UserDAO();
        java.util.List<dao.UserSummary> users = userDao.getAllUsers();
        return new SuccessResponse("Danh sách user.", new java.util.ArrayList<>(users));
    }
    private Response handleAdminSetUserActive (AdminSetUserActiveRequest req){
        if (!verifyAdmin(req.getAdminUsername())){
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
        if (!verifyAdmin(req.getAdminUsername())){
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
        if (!verifyAdmin(req.getAdminUsername())){
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