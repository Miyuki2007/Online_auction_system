package model.BTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BidTransaction implements Comparable<BidTransaction> {
    private final String id;
    private final String auctionId;
    private final String bidderId;
    private final double amount;
    private final LocalDateTime timestamp;

    public BidTransaction(String auctionId, String bidderId, double amount) {
        this.id = UUID.randomUUID().toString();
        this.auctionId=auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getBidderId() { return bidderId; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public int compareTo(BidTransaction other) {
        int cmp = Double.compare(other.amount,this.amount);
        return cmp != 0 ? cmp : this.timestamp.compareTo(other.timestamp);
    }
}