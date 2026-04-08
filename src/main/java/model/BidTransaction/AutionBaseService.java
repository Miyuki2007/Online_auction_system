import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

// Trạng thái của phiên đấu giá
enum AuctionStatus {
    OPEN, RUNNING, FINISHED, PAID, CANCELED
}

//Sản phẩm đấu giá
class AuctionProduct {
    private String id;
    private String name;
    private BigDecimal startingPrice;
    private BigDecimal currentHighestPrice;
    private String currentLeaderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String winnerId;

    // Sử dụng ReentrantLock để xử lý đồng thời (Race Condition) khi đặt giá
    private final ReentrantLock bidLock = new ReentrantLock();

    public AuctionProduct(String id, String name, BigDecimal startingPrice,
                          LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentHighestPrice = startingPrice; // Ban đầu giá cao nhất là giá khởi điểm
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
    }

    // Getters
    public String getId() { return id; }
    public AuctionStatus getStatus() { return status; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public BigDecimal getCurrentHighestPrice() { return currentHighestPrice; }
    public String getCurrentLeaderId() { return currentLeaderId; }

    public void setStatus(AuctionStatus status) { this.status = status; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }

    /// sử lý khi có nhiều dữ liệu đặt giá cùng lúc
    public boolean placeBid(String userId, BigDecimal bidAmount) {
        bidLock.lock(); // Khóa sản phẩm này lại khi có người đang kiểm tra và đặt giá
        try {
            // 1. Kiểm tra tính hợp lệ của giá đấu (Phải cao hơn giá hiện tại)
            if (bidAmount.compareTo(this.currentHighestPrice) > 0) {
                this.currentHighestPrice = bidAmount;
                this.currentLeaderId = userId;
                return true; // Đặt giá thành công
            }
            return false; // Đặt giá thất bại (giá không đủ cao)
        } finally {
            bidLock.unlock(); // Luôn luôn mở khóa dù có lỗi xảy ra
        }
    }
}