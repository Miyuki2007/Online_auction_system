package dao;

import model.manager.DatabaseConnection;
import java.sql.*;

public class BidDAO {
    public boolean placeBid(int auctionId, int bidderId, double amount) {
        String sql = "INSERT INTO Bids (auction_id, bidder_id, bid_amount) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            ps.setInt(2, bidderId);
            ps.setDouble(3, amount);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}