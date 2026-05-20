package model.manager;

import dao.UserDAO;
import model.auction.Auction;
import model.auction.BidTransaction;
import model.item.Item;
import model.user.User;
import model.auction.exception.AuthenticationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// Dùng singleton để tạo manager
public class AuctionManager {
    private static volatile AuctionManager instance;

    // Thay thế List bằng ConcurrentHashMap để tăng tốc độ tìm kiếm O(1) và đảm bảo an toàn luồng (thread-safe)
    private final Map<String, Auction> activeAuctions;
    private final Map<String, User> registeredUsers;

    private AuctionManager() {
        this.activeAuctions = new ConcurrentHashMap<>();
        this.registeredUsers = new ConcurrentHashMap<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    // --- CÁC METHOD BỔ SUNG ---

    /**
     * Lấy người dùng dựa trên ID.
     * Giả định trong model User của bạn, username chính là ID duy nhất.
     */
    public User getUserById(String userId) {
        return registeredUsers.get(userId);
    }

    /**
     * Lấy tất cả các phiên đấu giá (bao gồm cả đang hoạt động và đã kết thúc).
     */
    public List<Auction> getAllAuctions() {
        return Collections.unmodifiableList(new ArrayList<>(activeAuctions.values()));
    }

    /**
     * Lọc danh sách các phiên đấu giá theo ID người bán.
     */
    public List<Auction> getAuctionsBySeller(String sellerId) {
        return activeAuctions.values().stream()
                .filter(auction -> auction.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra thời gian hết hạn của tất cả các phiên đấu giá.
     * Phương thức này có thể được gọi bởi một luồng chạy nền (Background Thread).
     */
    public void checkAllExpirations() {
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : activeAuctions.values()) {
            // Kiểm tra nếu phiên đấu giá đã quá thời gian kết thúc nhưng chưa đóng
            if (auction.getEndTime().isBefore(now) && !auction.isEnded()) {
                auction.end();
                System.out.println("Auction " + auction.getId() + " has automatically ended.");
            }
        }
    }

    // --- CÁC METHOD CŨ ---

    public void registerUser(User user) {
        // 1. Khởi tạo DAO
        UserDAO userDAO = new UserDAO();

        // 2. Thực hiện lưu vào Database
        boolean isSaved = userDAO.registerUser(
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );

        if (isSaved) {
            System.out.println("Đã lưu user " + user.getUsername() + " vào database thành công.");
        } else {
            throw new RuntimeException("Lỗi: Không thể lưu người dùng vào Database.");
        }
    }

    public User getUserByUsername(String username) {
        // Thử lấy từ bộ nhớ trước (Cache)
        User user = registeredUsers.get(username);

        // Nếu không thấy, thử tìm trong Database
        if (user == null) {
            UserDAO userDAO = new UserDAO();
            user = userDAO.findByUsername(username);
        }

        return user;
    }

    public User authenticateUser(String username, String password) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.authenticate(username, password); // Sử dụng hàm authenticate mình đã hướng dẫn ở bài trước

        if (user == null) {
            // Bạn có thể giữ ném lỗi AuthenticationException như cũ
            throw new AuthenticationException("Tên đăng nhập hoặc mật khẩu không chính xác.");
        }


        registeredUsers.put(username, user);

        return user;
    }

    public void startAuction(Auction auction) {
        if (auction == null) return;
        activeAuctions.put(auction.getId(), auction);
        System.out.println("Auction " + auction.getId() + " started");
    }

    public Auction findAuctionById(String id) {
        return activeAuctions.get(id);
    }

    public BidTransaction placeBid(String auctionId, String bidderId, double amount) {
        Auction auction = findAuctionById(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException("Phiên đấu giá không tồn tại.");
        }

        // 1. Đặt giá trên RAM (để kiểm tra logic, anti-snipe, locks...)
        BidTransaction transaction = auction.placeBid(bidderId, amount);

        // 2. Nếu đặt giá thành công trên RAM, tiến hành ĐỒNG BỘ XUỐNG DATABASE
        if (transaction != null) {
            try {
                int aId = Integer.parseInt(auctionId);
                int bId = Integer.parseInt(bidderId);

                // Lưu lượt bid vào bảng Bids
                dao.BidDAO bidDAO = new dao.BidDAO();
                bidDAO.placeBid(aId, bId, amount);

                // Cập nhật lại giá cao nhất và người thắng hiện tại vào bảng Auctions
                dao.AuctionDAO auctionDAO = new dao.AuctionDAO();
                auctionDAO.updateCurrentPrice(aId, amount, bId);

                System.out.println("✅ Đã đồng bộ lượt đặt giá của User " + bidderId + " vào DB.");
            } catch (NumberFormatException e) {
                System.err.println("❌ Lỗi ép kiểu ID khi lưu lịch sử Bid: " + e.getMessage());
            }
        }

        return transaction;
    }

    public Auction createAuction(String sellerId, Item item, double startingPrice,
                                 LocalDateTime start, LocalDateTime end,
                                 boolean antiSnipe, long threshold, long extension) {

        // 1. GỌI DAO ĐỂ LƯU XUỐNG DATABASE TRƯỚC
        dao.AuctionDAO auctionDAO = new dao.AuctionDAO();

        // Tính toán số phút (durationMinutes) từ thời gian start và end
        long durationMinutes = java.time.Duration.between(start, end).toMinutes();

        // Gọi hàm insertAuction (đã được sửa) của DAO
        int generatedId = auctionDAO.insertAuction(
                sellerId,
                item.getClass().getSimpleName().toUpperCase(),
                item.getName(),
                item.getDescription(),
                startingPrice,
                durationMinutes
        );

        // 2. Kiểm tra kết quả và đưa vào RAM
        if (generatedId != -1) {
            // Khởi tạo Auction bằng ID thực tế từ cơ sở dữ liệu (ép kiểu về String nếu object Auction yêu cầu String ID)
            String auctionId = String.valueOf(generatedId);

            Auction auction = new Auction(auctionId, sellerId, item, startingPrice, start, end, antiSnipe, threshold, extension);

            activeAuctions.put(auction.getId(), auction);
            auction.start();
            System.out.println("✅ Đã lưu phiên đấu giá vào DB và RAM với ID: " + auction.getId());
            return auction;
        } else {
            throw new RuntimeException("Lỗi: Không thể lưu phiên đấu giá vào cơ sở dữ liệu MySQL.");
        }
    }

    public List<Auction> getActiveAuctions() {
        return Collections.unmodifiableList(new ArrayList<>(activeAuctions.values()));
    }
    public List<User> getRegisteredUsers() {
        return Collections.unmodifiableList(new ArrayList<>(registeredUsers.values()));
    }
    /**
     * Hàm khôi phục lại các phiên đấu giá từ Database lên RAM khi khởi động lại Server.
     * Hãy gọi hàm này ngay khi khởi chạy Server (trong hàm main của Server).
     */
    public void loadAuctionsFromDatabase() {
        System.out.println("⏳ Đang khôi phục các phiên đấu giá từ Database...");

        // 🌟 KHAI BÁO BIẾN ĐỂ FIX LỖI DÒNG 239
        dao.AuctionDAO auctionDAO = new dao.AuctionDAO();
        List<Auction> pendingAuctions = auctionDAO.getUnfinishedAuctionsFromDB();

        LocalDateTime now = LocalDateTime.now();
        int count = 0;

        // Bắt đầu vòng lặp quét qua từng phiên đấu giá
        for (Auction auction : pendingAuctions) {

            // Tình huống 1: Phiên ĐÃ HẾT GIỜ trong lúc Server đang tắt
            if (auction.getEndTime().isBefore(now)) {
                auction.end(); // Kết thúc trên RAM

                try {
                    int aId = Integer.parseInt(auction.getId());
                    auctionDAO.updateAuctionStatus(aId, "FINISHED");
                    System.out.println("➔ Phiên " + auction.getId() + " đã hết hạn khi server tắt. Đã tự động đóng trong DB.");
                } catch (NumberFormatException e) {
                    System.err.println("❌ Lỗi ép kiểu ID khi cập nhật trạng thái: " + auction.getId());
                }
            }
            // Tình huống 2: Phiên VẪN CÒN THỜI GIAN chạy tiếp
            else {
                activeAuctions.put(auction.getId(), auction);
                auction.start(); // Kích hoạt lại luồng hoặc bộ đếm giờ cho phiên này
                count++;
            }
        } // Vòng lặp FOR kết thúc tại đây

        System.out.println("✅ Khôi phục thành công " + count + " phiên đấu giá đang hoạt động lên RAM.");
    }
}