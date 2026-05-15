package dao;

import model.manager.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    // Lấy danh sách các phiên đấu giá đang diễn ra (ACTIVE)
    public void getActiveAuctions() {
        String sql = "SELECT * FROM Auctions WHERE status = 'ACTIVE'";
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
    // Hàm thêm một phiên đấu giá (sản phẩm) mới vào cơ sở dữ liệu
    // Hàm thêm phiên đấu giá mới (Đã sửa để khớp với Giao diện)
    public boolean insertAuction(String sellerUsername, String categoryName, String title,
                                 String description, double startingPrice, long durationMinutes) {
        String sql = "INSERT INTO Auctions (seller_id, category_id, title, description, starting_price, current_price, start_time, end_time, status) " +
                "SELECT u.user_id, c.category_id, ?, ?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? MINUTE), 'OPEN' " +
                "FROM Users u, Categories c " +
                "WHERE u.username = ? AND c.name = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Truyền dữ liệu vào các dấu ?
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setDouble(3, startingPrice);
            ps.setDouble(4, startingPrice); // current_price ban đầu = starting_price
            ps.setLong(5, durationMinutes); // Số phút
            ps.setString(6, sellerUsername);
            ps.setString(7, categoryName);

            int result = ps.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ LỖI KHI THÊM PHIÊN ĐẤU GIÁ VÀO DB:");
            e.printStackTrace();
        }
        return false;
    }
}