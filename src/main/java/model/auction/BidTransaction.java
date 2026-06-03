package model.auction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class BidTransaction implements Comparable<BidTransaction>, Serializable {
    private static final long serialVersionUID=1L;
    private final String id;
    private final String auctionId; // Rất quan trọng để tracking
    private final String bidderId;
    private final double amount;    // Dùng double để khớp với kiểu dữ liệu trong class Auction
    private final LocalDateTime timestamp;
    private LocalDateTime newEndTime;

    public BidTransaction(String auctionId, String bidderId, double amount) {
        this.id = UUID.randomUUID().toString();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }
    public BidTransaction(String auctionId, String bidderId, double amount, LocalDateTime timestamp) {
        this.id = UUID.randomUUID().toString();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.newEndTime = null; // ← THÊM
    }

    // Các hàm Getter
    public String getId() { return id; }
    public String getAuctionId() { return auctionId; }
    public String getBidderId() { return bidderId; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public LocalDateTime getNewEndTime() { return newEndTime; }       // ← THÊM
    public void setNewEndTime(LocalDateTime newEndTime) {             // ← THÊM
        this.newEndTime = newEndTime;
    }
    @Override
    public int compareTo(BidTransaction other) {
        // So sánh giá giảm dần. Nếu giá bằng nhau, ai đặt trước xếp trên
        int cmp = Double.compare(other.amount, this.amount);
        return cmp != 0 ? cmp : this.timestamp.compareTo(other.timestamp);
    }
}