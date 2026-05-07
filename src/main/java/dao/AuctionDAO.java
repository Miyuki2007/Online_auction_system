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
}