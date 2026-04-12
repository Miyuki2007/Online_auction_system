package model.BTransaction;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionProduct {
    private final String id;
    private final String name;
    private final String sellerId;
    private final BigDecimal startingPrice;

    private BigDecimal currentHighestPrice;
    private String currentLeaderId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    // Anti-snipe
    private final boolean antiSnipeEnabled;
    private final long antiSnipeThresholdSec;
    private final long antiSnipeExtensionSec;

    // Concurrency & History
    private final ReentrantLock bidLock = new ReentrantLock(true);
    private final List<BidTransaction> bidHistory = Collections.synchronizedList(new ArrayList<>());
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    public AuctionProduct(String id, String name, String sellerId, BigDecimal startingPrice,
                          LocalDateTime startTime, LocalDateTime endTime,
                          boolean antiSnipeEnabled, long threshold, long extension) {
        this.id = id;
        this.name = name;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.antiSnipeEnabled = antiSnipeEnabled;
        this.antiSnipeThresholdSec = threshold;
        this.antiSnipeExtensionSec = extension;
        this.status = AuctionStatus.RUNNING; // Giả sử tạo xong chạy luôn cho đơn giản
    }

    public boolean placeBid(String bidderId, BigDecimal bidAmount) {
        bidLock.lock();
        try {
            // 1. Kiểm tra trạng thái và thời gian
            if (status != AuctionStatus.RUNNING || LocalDateTime.now().isAfter(endTime)) {
                return false;
            }
            // 2. Không cho phép tự đặt giá món của mình
            if (bidderId.equals(sellerId)) {
                return false;
            }
            // 3. Giá phải cao hơn giá hiện tại (Dùng compareTo cho BigDecimal)
            if (bidAmount.compareTo(currentHighestPrice) <= 0) {
                return false;
            }

            // 4. Cập nhật dữ liệu
            this.currentHighestPrice = bidAmount;
            this.currentLeaderId = bidderId;
            BidTransaction newBid = new BidTransaction(bidderId, bidAmount);
            this.bidHistory.add(newBid);

            // 5. Kích hoạt Anti-snipe nếu cần
            if (antiSnipeEnabled) {
                checkAntiSnipe();
            }

            // 6. Thông báo cho người theo dõi
            notifyNewBid(newBid);
            return true;

        } finally {
            bidLock.unlock();
        }
    }

    private void checkAntiSnipe() {
        long secsRemain = Duration.between(LocalDateTime.now(), endTime).getSeconds();
        if (secsRemain > 0 && secsRemain <= antiSnipeThresholdSec) {
            this.endTime = this.endTime.plusSeconds(antiSnipeExtensionSec);
            for (AuctionObserver obs : observers) {
                obs.onTimeExtended(this, antiSnipeExtensionSec);
            }
        }
    }

    public void checkExpiration() {
        bidLock.lock();
        try {
            if (status == AuctionStatus.RUNNING && LocalDateTime.now().isAfter(endTime)) {
                this.status = AuctionStatus.FINISHED;
                for (AuctionObserver obs : observers) {
                    obs.onStatusChanged(this);
                }
            }
        } finally {
            bidLock.unlock();
        }
    }

    public void addObserver(AuctionObserver observer) { observers.add(observer); }
    private void notifyNewBid(BidTransaction bid) { observers.forEach(o -> o.onNewBid(this, bid)); }

    // Getters
    public String getId() { return id; }
    public AuctionStatus getStatus() { return status; }
    public LocalDateTime getEndTime() { return endTime; }
    public BigDecimal getCurrentHighestPrice() { return currentHighestPrice; }
    public String getCurrentLeaderId() { return currentLeaderId; }
    public List<BidTransaction> getBidHistory() { return Collections.unmodifiableList(bidHistory); }
}