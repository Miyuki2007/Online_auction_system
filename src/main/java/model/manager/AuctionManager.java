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
        return auction.placeBid(bidderId, amount);
    }

    public Auction createAuction(String sellerId, Item item, double startingPrice,
                                 LocalDateTime start, LocalDateTime end,
                                 boolean antiSnipe, long threshold, long extension) {

        // 1. GỌI DAO ĐỂ LƯU XUỐNG DATABASE TRƯỚC
        dao.AuctionDAO auctionDAO = new dao.AuctionDAO();

        // Tính toán số phút (durationMinutes) từ thời gian start và end
        long durationMinutes = java.time.Duration.between(start, end).toMinutes();

        // Gọi hàm insertAuction (đã được sửa) của DAO
        boolean isSaved = auctionDAO.insertAuction(
                sellerId,
                item.getClass().getSimpleName().toUpperCase(), // Lấy loại, ví dụ "ELECTRONICS"
                item.getName(),
                item.getDescription(),
                startingPrice,
                durationMinutes
        );

        // 2. KIỂM TRA KẾT QUẢ VÀ LƯU VÀO RAM NẾU THÀNH CÔNG
        if (isSaved) {
            Auction auction = new Auction(sellerId, item, startingPrice, start, end, antiSnipe, threshold, extension);
            activeAuctions.put(auction.getId(), auction);
            auction.start();
            System.out.println("✅ Đã lưu phiên đấu giá mới vào Database và RAM thành công: " + auction.getId());
            return auction;
        } else {
            // Ném lỗi để Server báo về cho Client biết là tạo thất bại
            throw new RuntimeException("Lỗi: Không thể lưu phiên đấu giá vào cơ sở dữ liệu MySQL.");
        }
    }

    public List<Auction> getActiveAuctions() {
        return Collections.unmodifiableList(new ArrayList<>(activeAuctions.values()));
    }
    public List<User> getRegisteredUsers() {
        return Collections.unmodifiableList(new ArrayList<>(registeredUsers.values()));
    }
}