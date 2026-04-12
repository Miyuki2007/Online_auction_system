package model.BTransaction;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {
    private final Map<String, AuctionProduct> productDatabase = new ConcurrentHashMap<>();

    public void addProduct(AuctionProduct product) {
        productDatabase.put(product.getId(), product);
        System.out.println("Đã thêm sản phẩm: " + product.getId());
    }

    public String participateAuction(String productId, String userId, BigDecimal bidAmount) {
        AuctionProduct product = productDatabase.get(productId);

        if (product == null) {
            return "Lỗi: Sản phẩm không tồn tại.";
        }

        // Ủy quyền việc kiểm tra luật lệ và đặt giá cho chính Product
        boolean isSuccess = product.placeBid(userId, bidAmount);

        if (isSuccess) {
            return "Thành công: Người dùng " + userId + " đang dẫn đầu với giá " + bidAmount;
        } else {
            return "Thất bại: Giá đặt phải cao hơn mức giá cao nhất hiện tại hoặc phiên đã đóng.";
        }
    }

    public void checkAndEndAuctions() {
        for (AuctionProduct product : productDatabase.values()) {

            // Product sẽ tự động kiểm tra giờ và tự đổi status nếu đã hết hạn
            product.checkExpiration();

            if (product.getStatus() == AuctionStatus.FINISHED) {
                if (product.getCurrentLeaderId() != null) {
                    System.out.println("Phiên " + product.getId() + " kết thúc. Người thắng: "
                            + product.getCurrentLeaderId() + " với giá " + product.getCurrentHighestPrice());
                } else {
                    System.out.println("Phiên " + product.getId() + " kết thúc. Không có người tham gia trả giá.");
                }
            }
        }
    }
}