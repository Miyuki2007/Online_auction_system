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
    public java.util.List<model.auction.BidTransaction> loadBidsByAuctionId(int auctionId, String auctionUuid) {
        java.util.List<model.auction.BidTransaction> bids = new java.util.ArrayList<>();
        String sql = "SELECT b.bid_amount, b.bid_time, u.username AS bidder_username " +
                "FROM Bids b " +
                "JOIN Users u ON b.bidder_id = u.user_id " +
                "WHERE b.auction_id = ? " +
                "ORDER BY b.bid_time ASC";
        try (java.sql.Connection conn = model.manager.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String bidderUsername = rs.getString("bidder_username");
                    double amount = rs.getDouble("bid_amount");
                    // Tạo BidTransaction
                    model.auction.BidTransaction bid = new model.auction.BidTransaction(
                            auctionUuid, bidderUsername, amount);
                    bids.add(bid);
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }
}