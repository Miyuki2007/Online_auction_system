package model.BTransaction; // Nhớ đổi tên package cho khớp với cây thư mục của bạn

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BidTransaction implements Comparable<BidTransaction> {
    private final String id;
    private final String bidderId;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;

    public BidTransaction(String bidderId, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getBidderId() { return bidderId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public int compareTo(BidTransaction other) {
        int cmp = other.amount.compareTo(this.amount);
        return cmp != 0 ? cmp : this.timestamp.compareTo(other.timestamp);
    }
}