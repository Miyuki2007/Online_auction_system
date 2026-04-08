import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {

    // Giả lập Database lưu trữ sản phẩm (Thread-safe Map)
    private final Map<String, AuctionProduct> productDatabase = new ConcurrentHashMap<>();

    // ==========================================
    // 3.1.2 Quản lý sản phẩm đấu giá
    // ==========================================
    public void addProduct(AuctionProduct product) {
        productDatabase.put(product.getId(), product);
        System.out.println("Đã thêm sản phẩm: " + product.getId());
    }

    // ==========================================
    // 3.1.3 Tham gia đấu giá
    // ==========================================
    public String participateAuction(String productId, String userId, BigDecimal bidAmount) {
        AuctionProduct product = productDatabase.get(productId);

        if (product == null) {
            return "Lỗi: Sản phẩm không tồn tại.";
        }

        LocalDateTime now = LocalDateTime.now();

        // Kiểm tra trạng thái và thời gian
        if (product.getStatus() != AuctionStatus.RUNNING &&
                (now.isBefore(product.getStartTime()) || now.isAfter(product.getEndTime()))) {
            return "Lỗi: Phiên đấu giá không trong thời gian hoạt động.";
        }

        // Cố gắng đặt giá
        boolean isSuccess = product.placeBid(userId, bidAmount);

        if (isSuccess) {
            return "Thành công: Người dùng " + userId + " đang dẫn đầu với giá " + bidAmount;
        } else {
            return "Thất bại: Giá đặt phải cao hơn mức giá cao nhất hiện tại (" + product.getCurrentHighestPrice() + ").";
        }
    }

    // ==========================================
    // 3.1.4 Kết thúc phiên đấu giá
    // ==========================================
    public void checkAndEndAuctions() {
        LocalDateTime now = LocalDateTime.now();

        for (AuctionProduct product : productDatabase.values()) {
            // Tự động đóng phiên khi hết thời gian và trạng thái vẫn đang chạy/mở
            if ((product.getStatus() == AuctionStatus.RUNNING || product.getStatus() == AuctionStatus.OPEN)
                    && now.isAfter(product.getEndTime())) {

                product.setStatus(AuctionStatus.FINISHED);

                // Xác định người thắng cuộc
                if (product.getCurrentLeaderId() != null) {
                    product.setWinnerId(product.getCurrentLeaderId());
                    System.out.println("Phiên " + product.getId() + " kết thúc. Người thắng: "
                            + product.getCurrentLeaderId() + " với giá " + product.getCurrentHighestPrice());
                } else {
                    System.out.println("Phiên " + product.getId() + " kết thúc. Không có người tham gia trả giá.");
                }
            }
        }
    }
}