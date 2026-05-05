package model.manager;

import model.auction.Auction;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AuctionManager {

    private final ConcurrentMap<String, Auction> activeAuctions;

    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
    }

    private static class InstanceHolder {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    public static AuctionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // 2. Thêm phiên đấu giá mới với thao tác put()
    public void startAuction(Auction auction) {
        // Dùng putIfAbsent để đề phòng trường hợp vô tình start 2 lần cùng một ID
        Auction existing = activeAuctions.putIfAbsent(auction.getItemID(), auction);

        if (existing == null) {
            System.out.println("Auction " + auction.getItemID() + " started");
        } else {
            System.out.println("Warning: Auction " + auction.getItemID() + " is already running!");
        }
    }

    // 3. THÊM MỚI: Lấy trực tiếp phiên đấu giá theo ID với tốc độ O(1)
    public Auction getAuctionById(String itemId) {
        return activeAuctions.get(itemId);
    }

    // 4. Trả về danh sách an toàn (chỉ đọc) cho UI hoặc API
    public Collection<Auction> getActiveAuctions() {
        return Collections.unmodifiableCollection(activeAuctions.values());
    }

    // 5. THÊM MỚI: Kết thúc phiên đấu giá một cách an toàn
    public void endAuction(String itemId) {
        Auction removed = activeAuctions.remove(itemId);
        if (removed != null) {
            System.out.println("Auction " + itemId + " has ended.");
        }
    }
    public void checkAllExpirations() {
        LocalDateTime now = LocalDateTime.now();

        // Duyệt qua tất cả các phiên đấu giá đang diễn ra
        for (Auction auction : activeAuctions.values()) {

            // Nếu thời gian hiện tại đã vượt qua thời gian kết thúc
            if (now.isAfter(auction.getEndTime())) {

                System.out.println("⏳ Auction " + auction.getItemID() + " has expired!");

                // 1. Thực hiện logic kết thúc (tìm người thắng, tính tiền, gửi thông báo...)

                // 2. Xóa phiên đấu giá khỏi danh sách active (ConcurrentHashMap tự xử lý an toàn)
                activeAuctions.remove(auction.getItemID());
            }
        }
    }
}