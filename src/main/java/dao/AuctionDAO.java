package dao;

import model.manager.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    // Lấy danh sách các phiên đấu giá đang diễn ra (Đã sửa 'ACTIVE' thành 'RUNNING' cho khớp với cấu trúc Database)
    public void getActiveAuctions() {
        String sql = "SELECT * FROM Auctions WHERE status = 'RUNNING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                System.out.println("Sản phẩm: " + rs.getString("title") +
                        " - Giá hiện tại: " + rs.getDouble("current_price"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Hàm cập nhật giá mới khi có người đặt giá cao hơn
    public boolean updateCurrentPrice(int auctionId, double bidAmount, int bidderId) {
        String sql = "UPDATE Auctions SET current_price = ?, winner_id = ? WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, bidAmount);
            ps.setInt(2, bidderId);
            ps.setInt(3, auctionId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Hàm thêm một phiên đấu giá mới vào cơ sở dữ liệu (Đã sửa đổi)
    public int insertAuction(String sellerUsername, String categoryName, String title,
                             String description, double startingPrice, long durationMinutes) {
        String sql = "INSERT INTO Auctions (seller_id, category_id, title, description, starting_price, current_price, start_time, end_time, status) " +
                "SELECT u.user_id, c.category_id, ?, ?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? MINUTE), 'OPEN' " +
                "FROM Users u, Categories c " +
                "WHERE u.username = ? AND c.name = ?";

        // 1. Bổ sung thêm Statement.RETURN_GENERATED_KEYS để yêu cầu MySQL trả ngược lại ID tự sinh
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Truyền dữ liệu vào các dấu ?
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setDouble(3, startingPrice);
            ps.setDouble(4, startingPrice); // current_price ban đầu = starting_price
            ps.setLong(5, durationMinutes); // Số phút
            ps.setString(6, sellerUsername);
            ps.setString(7, categoryName);

            int affectedRows = ps.executeUpdate();

            // 2. Nếu lệnh INSERT thành công, tiến hành bóc tách lấy ID ra để return
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1); // Trả về ID chính xác kiểu int từ MySQL
                    }
                }
            }
            return -1; // Trả về -1 nếu chèn dữ liệu không thành công

        } catch (SQLException e) {
            System.err.println("❌ LỖI KHI THÊM PHIÊN ĐẤU GIÁ VÀO DB:");
            e.printStackTrace();
        }
        return -1; // Trả về -1 khi dính ngoại lệ lỗi thay vì return false như trước
    }
    public List<model.auction.Auction> getUnfinishedAuctionsFromDB() {
        List<model.auction.Auction> list = new ArrayList<>();

        // Quét tất cả các phiên chưa kết thúc từ cơ sở dữ liệu
        String sql = "SELECT * FROM Auctions WHERE status IN ('OPEN', 'RUNNING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // 1. Đọc dữ liệu từ database
                String auctionId = String.valueOf(rs.getInt("auction_id"));
                String sellerId = String.valueOf(rs.getInt("seller_id"));
                String title = rs.getString("title");
                String description = rs.getString("description");
                double startingPrice = rs.getDouble("starting_price");

                // 🌟 Đọc trực tiếp kiểu dữ liệu DATETIME/TIMESTAMP từ MySQL sang LocalDateTime của Java
                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();

                // 2. Khởi tạo đối tượng Item tương ứng (Mock object để khớp với Constructor của Auction)
                model.item.Item item = new model.item.Item("ITEM_" + auctionId, title, description, startingPrice) {
                    @Override
                    public void displayDetails() {
                        System.out.println("Sản phẩm: " + title + " - " + description);
                    }
                };

                // 3. Khởi tạo Auction bằng Constructor 9 tham số mới của bạn
                // Mặc định truyền anti-snipe tạm thời là false, 0, 0 (hoặc chỉnh lại theo cột database của bạn nếu có)
                model.auction.Auction auction = new model.auction.Auction(
                        auctionId, sellerId, item, startingPrice,
                        startTime, endTime, false, 0, 0
                );

                // 4. Đồng bộ lại giá tiền hiện tại và người dẫn đầu đã lưu trong DB
                // (Giúp RAM không bị mất số tiền đang trả giá hiện tại khi restart server)
                // Lưu ý: Bạn cần viết thêm hàm setter trong Auction.java cho các thuộc tính này nếu chưa có,
                // hoặc bỏ qua nếu cấu trúc hệ thống của bạn tự xử lý qua lịch sử Bid.

                list.add(auction);
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi khôi phục danh sách phiên đấu giá từ Database:");
            e.printStackTrace();
        }
        return list;
    }
    public boolean updateAuctionStatus(int auctionId, String status) {
        String sql = "UPDATE Auctions SET status = ? WHERE auction_id = ?";
        try (Connection conn = model.manager.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, auctionId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi cập nhật trạng thái FINISHED cho auction_id: " + auctionId);
            e.printStackTrace();
        }
        return false;
    }
}