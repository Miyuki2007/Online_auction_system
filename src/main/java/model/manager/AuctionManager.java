package model.manager;

import dao.AutoBidDAO;
import dao.UserDAO;
import model.auction.Auction;
import model.auction.AutoBid;
import model.auction.BidTransaction;
import model.item.Item;
import model.user.User;
import model.auction.exception.AuthenticationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
                    instance.loadAuctionsFromDB();
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
        dao.AuctionDAO auctionDAO = new dao.AuctionDAO();

        for (Auction auction : activeAuctions.values()) {
            if (auction.getEndTime().isBefore(now) && !auction.isEnded()) {
                auction.end();  // chuyển state RAM sang FINISHED

                // ⭐ Lưu trạng thái FINISHED xuống DB
                int dbAuctionId = auctionDAO.findAuctionIdByTitleAndSeller(
                        auction.getItem().getName(),
                        auction.getSellerId()
                );
                if (dbAuctionId > 0) {
                    auctionDAO.updateStatus(dbAuctionId, "FINISHED");
                    System.out.println("✅ Auction " + auction.getId() +
                            " đã kết thúc và lưu DB (auction_id=" + dbAuctionId + ").");
                } else {
                    System.err.println("⚠️ Không tìm thấy auction trong DB để update FINISHED: " + auction.getId());
                }
            }
        }
    }

    // --- CÁC METHOD CŨ ---

    public void registerUser(User user) {
        // 1. Khởi tạo DAO
        UserDAO userDAO = new UserDAO();
        //2. Kiểm tra Username đã tồn tại chưa
        if (registeredUsers.containsKey(user.getUsername())
                || userDAO.checkUsernameExist(user.getUsername())) {
            throw new IllegalArgumentException(
                    "Username '" + user.getUsername() + "' đã tồn tại.");
        }
        // 3. Thực hiện lưu vào Database
        boolean isSaved = userDAO.registerUser(
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );

        if (isSaved) {
            registeredUsers.put(user.getUsername(),user);
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
        if (!userDAO.checkUsernameExist(username))
        {
            throw new AuthenticationException("Tên đăng nhập không tồn tại.");
        }
        User user = userDAO.authenticate(username, password); // Sử dụng hàm authenticate mình đã hướng dẫn ở bài trước

        if (user == null) {
            // Bạn có thể giữ ném lỗi AuthenticationException như cũ
            throw new AuthenticationException("Mật khẩu không chính xác.");
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

        // 1. Đặt giá trong RAM trước (sẽ throw exception nếu invalid)
        BidTransaction record = auction.placeBid(bidderId, amount);

        // 2. Nếu OK, lưu xuống DB đồng bộ — lịch sử bid sẵn sàng ngay
        if (record != null) {
            try {
                dao.AuctionDAO auctionDAO = new dao.AuctionDAO();
                dao.UserDAO userDAO = new dao.UserDAO();
                dao.BidDAO bidDAO = new dao.BidDAO();

                int dbAuctionId = auctionDAO.findAuctionIdByTitleAndSeller(
                        auction.getItem().getName(), auction.getSellerId());
                int dbBidderId = userDAO.findUserIdByUsername(bidderId);

                if (dbAuctionId > 0 && dbBidderId > 0) {
                    boolean bidSaved = bidDAO.placeBid(dbAuctionId, dbBidderId, amount);
                    boolean priceUpdated = auctionDAO.updateCurrentPrice(dbAuctionId, amount, dbBidderId);
                    boolean endTimeUpdated = auctionDAO.updateEndTime(dbAuctionId, auction.getEndTime());

                    if (bidSaved && priceUpdated && endTimeUpdated) {
                        System.out.println("✅ Đã lưu bid vào DB: auction=" + dbAuctionId +
                                ", bidder=" + dbBidderId + ", amount=" + amount);
                    } else {
                        System.err.println("⚠️ Bid lưu DB không đầy đủ.");
                    }
                } else {
                    System.err.println("⚠️ Không tìm thấy auction/bidder trong DB.");
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi lưu bid vào DB: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return record;
    }

    public Auction createAuction(String sellerId, Item item, double startingPrice,
                                 LocalDateTime start, LocalDateTime end,
                                 boolean antiSnipe, long threshold, long extension) {

        // 1. GỌI DAO ĐỂ LƯU XUỐNG DATABASE TRƯỚC
        dao.AuctionDAO auctionDAO = new dao.AuctionDAO();

        // Tính toán số phút (durationMinutes) từ thời gian start và end
        long durationMinutes = java.time.Duration.between(start, end).toMinutes();
        Auction auction = new Auction(sellerId, item, startingPrice, start, end,
                antiSnipe, threshold, extension);
        // Gọi hàm insertAuction (đã được sửa) của DAO
        boolean isSaved = auctionDAO.insertAuction(
                sellerId,
                item.getClass().getSimpleName().toUpperCase(), // Lấy loại, ví dụ "ELECTRONICS"
                item.getName(),
                item.getDescription(),
                startingPrice,
                durationMinutes,
                item.getImageData(),
                antiSnipe,
                threshold,
                extension,
                auction.getId()
        );

        // 2. KIỂM TRA KẾT QUẢ VÀ LƯU VÀO RAM NẾU THÀNH CÔNG
        if (isSaved) {
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
    private void loadAuctionsFromDB() {
        dao.AuctionDAO auctionDAO = new dao.AuctionDAO();
        AutoBidDAO autoBidDAO = new AutoBidDAO();
        List<Auction> loaded = auctionDAO.loadAllAuctionsFromDB();

        for (Auction auction : loaded) {
            activeAuctions.put(auction.getId(), auction);

            // Chỉ load AutoBid cho các phiên chưa kết thúc
            if (!auction.isEnded()) {
                // Tìm dbAuctionId thực tế trong Database
                int dbAuctionId = auctionDAO.findAuctionIdByTitleAndSeller(
                        auction.getItem().getName(),
                        auction.getSellerId()
                );

                if (dbAuctionId > 0) {
                    // Tùy vào hàm loadActiveAutoBidsByAuction của bạn nhận tham số nào (1 hay 2 tham số)
                    // Mình giả sử nó nhận 2 tham số như ở vòng lặp thứ 2 của bạn
                    List<AutoBid> autoBids = autoBidDAO.loadActiveAutoBidsByAuction(dbAuctionId, auction.getId());

                    // Tránh NullPointerException nếu AutoBidManager chưa được khởi tạo đúng
                    if (autoBids != null && !autoBids.isEmpty()) {
                        // Cần đảm bảo bạn đã import AutoBidManager
                        AutoBidManager.getInstance().restoreAutoBids(auction, autoBids);
                    }
                }
            }
        }
    }
}