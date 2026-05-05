package model.manager;

import model.auction.Auction;
import model.auction.BidTransaction;
import model.item.Item;
import model.user.User;
import model.auction.exception.AuthenticationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public void registerUser(User user) {
        if (user == null || user.getUsername() == null) {
            throw new IllegalArgumentException("Người dùng hoặc tên đăng nhập không hợp lệ.");
        }
        // putIfAbsent giải quyết vấn đề race-condition (đảm bảo atomic check-then-act)
        if (registeredUsers.putIfAbsent(user.getUsername(), user) != null) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
    }

    public User getUserByUsername(String username) {
        return registeredUsers.get(username);
    }

    public User authenticateUser(String username, String password) {
        User user = getUserByUsername(username);
        if (user == null) {
            throw new AuthenticationException("Tên đăng nhập không tồn tại.");
        }
        if (!user.authenticate(password)) {
            throw new AuthenticationException("Mật khẩu không đúng.");
        }
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
        Auction auction = new Auction(sellerId, item, startingPrice, start, end, antiSnipe, threshold, extension);
        activeAuctions.put(auction.getId(), auction);
        auction.start();
        return auction;
    }

    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    public List<User> getRegisteredUsers() {
        return new ArrayList<>(registeredUsers.values());
    }
}
