package dao;

import model.manager.DatabaseConnection;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class BidDAO {
    public boolean placeBid(int auctionId, int bidderId, double amount) {
        String sql = "INSERT INTO Bids (auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            ps.setInt(2, bidderId);
            ps.setDouble(3, amount);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
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
                    java.time.LocalDateTime bidTime = rs.getTimestamp("bid_time") != null
                            ? rs.getTimestamp("bid_time").toLocalDateTime()
                            : java.time.LocalDateTime.now();
                    model.auction.BidTransaction bid = new model.auction.BidTransaction(
                            auctionUuid, bidderUsername, amount, bidTime);
                    bids.add(bid);
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }
    public List<model.auction.BidTransaction> loadBidsByAuctionAndUser(
            int auctionDbId, String auctionUuid, String username) {

        List<model.auction.BidTransaction> bids = new ArrayList<>();
        String sql =
                "SELECT b.bid_amount, b.bid_time " +
                        "FROM Bids b " +
                        "JOIN Users u ON b.bidder_id = u.user_id " +
                        "WHERE b.auction_id = ? AND u.username = ? " +
                        "ORDER BY b.bid_time ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionDbId);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double amount = rs.getDouble("bid_amount");
                    java.time.LocalDateTime bidTime = rs.getTimestamp("bid_time") != null
                            ? rs.getTimestamp("bid_time").toLocalDateTime()
                            : java.time.LocalDateTime.now();
                    model.auction.BidTransaction bid =
                            new model.auction.BidTransaction(auctionUuid, username, amount, bidTime);
                    bids.add(bid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }
}