package dao;

import model.manager.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;

public class WalletDAO {
    public boolean deposit(String username, double amount) {
        if (!validAmount(amount)) throw new IllegalArgumentException("So tien khong hop le.");
        String updateSql = "UPDATE Users SET balance = balance + ? WHERE username = ? AND role <> 'ADMIN'";
        try (Connection conn = DatabaseConnection.getConnection()) {
            requireConnection(conn);
            conn.setAutoCommit(false);
            try {
                int userId = findUserIdByUsername(conn, username);
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setDouble(1, amount);
                    ps.setString(2, username);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("Khong tim thay bidder de nap tien.");
                    }
                }
                insertWalletTx(conn, userId, null, "DEPOSIT", amount, "Nap tien vao tai khoan");
                conn.commit();
                return true;
            } catch (Exception e) {
                rollback(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public boolean withdraw(String username, double amount) {
        if (!validAmount(amount)) throw new IllegalArgumentException("So tien khong hop le.");
        String updateSql = "UPDATE Users SET balance = balance - ? " +
                "WHERE username = ? AND role <> 'ADMIN' AND balance >= ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            requireConnection(conn);
            conn.setAutoCommit(false);
            try {
                int userId = findUserIdByUsername(conn, username);
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setDouble(1, amount);
                    ps.setString(2, username);
                    ps.setDouble(3, amount);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("So du khong du hoac tai khoan khong duoc rut tien.");
                    }
                }
                insertWalletTx(conn, userId, null, "WITHDRAW", amount, "Rut tien khoi tai khoan");
                conn.commit();
                return true;
            } catch (Exception e) {
                rollback(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public boolean holdForBid(int auctionId, String bidderUsername, String previousWinnerUsername,
                              double previousAmount, double amount, LocalDateTime endTime) {
        if (!validAmount(amount)) throw new IllegalArgumentException("So tien bid khong hop le.");
        try (Connection conn = DatabaseConnection.getConnection()) {
            requireConnection(conn);
            conn.setAutoCommit(false);
            try {
                lockAuction(conn, auctionId);
                int bidderId = findUserIdByUsername(conn, bidderUsername);

                if (previousWinnerUsername != null && !previousWinnerUsername.isBlank()
                        && previousAmount > 0) {
                    int previousWinnerId = findUserIdByUsername(conn, previousWinnerUsername);
                    releaseHold(conn, previousWinnerId, previousAmount);
                    insertWalletTx(conn, previousWinnerId, auctionId, "RELEASE", previousAmount,
                            "Hoan tien do bi vuot gia");
                }

                String holdSql = "UPDATE Users SET balance = balance - ?, locked_balance = locked_balance + ? " +
                        "WHERE user_id = ? AND role <> 'ADMIN' AND balance >= ?";
                try (PreparedStatement ps = conn.prepareStatement(holdSql)) {
                    ps.setDouble(1, amount);
                    ps.setDouble(2, amount);
                    ps.setInt(3, bidderId);
                    ps.setDouble(4, amount);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("So du khong du de dat gia.");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Bids (auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, auctionId);
                    ps.setInt(2, bidderId);
                    ps.setDouble(3, amount);
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Auctions SET current_price = ?, winner_id = ?, end_time = ? WHERE auction_id = ?")) {
                    ps.setDouble(1, amount);
                    ps.setInt(2, bidderId);
                    ps.setTimestamp(3, Timestamp.valueOf(endTime));
                    ps.setInt(4, auctionId);
                    ps.executeUpdate();
                }

                insertWalletTx(conn, bidderId, auctionId, "HOLD", amount, "Giu tien cho bid dang dan dau");
                conn.commit();
                return true;
            } catch (Exception e) {
                rollback(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public boolean settleAuction(int auctionId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            requireConnection(conn);
            conn.setAutoCommit(false);
            try {
                AuctionPaymentInfo info = lockAuction(conn, auctionId);
                if (info.winnerId <= 0 || info.amount <= 0) {
                    updateAuctionStatus(conn, auctionId, "FINISHED");
                    conn.commit();
                    return false;
                }

                String unlockWinnerSql = "UPDATE Users SET locked_balance = GREATEST(locked_balance - ?, 0) " +
                        "WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(unlockWinnerSql)) {
                    ps.setDouble(1, info.amount);
                    ps.setInt(2, info.winnerId);
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("So tien dang giu cua winner khong du.");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Users SET balance = balance + ? WHERE user_id = ?")) {
                    ps.setDouble(1, info.amount);
                    ps.setInt(2, info.sellerId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Payments (auction_id, buyer_id, amount, payment_method, payment_status) " +
                                "VALUES (?, ?, ?, 'WALLET', 'COMPLETED')")) {
                    ps.setInt(1, auctionId);
                    ps.setInt(2, info.winnerId);
                    ps.setDouble(3, info.amount);
                    ps.executeUpdate();
                }

                insertWalletTx(conn, info.sellerId, auctionId, "PAY_SELLER", info.amount,
                        "Nhan tien tu phien dau gia thanh cong");
                updateAuctionStatus(conn, auctionId, "PAID");
                conn.commit();
                return true;
            } catch (Exception e) {
                rollback(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public boolean releaseAuctionHoldAndCancel(int auctionId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            requireConnection(conn);
            conn.setAutoCommit(false);
            try {
                AuctionPaymentInfo info = lockAuction(conn, auctionId);
                if (info.winnerId > 0 && info.amount > 0) {
                    releaseHold(conn, info.winnerId, info.amount);
                    insertWalletTx(conn, info.winnerId, auctionId, "RELEASE", info.amount,
                            "Hoan tien do phien bi huy");
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Auctions SET status = 'CANCELED', winner_id = NULL WHERE auction_id = ?")) {
                    ps.setInt(1, auctionId);
                    ps.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (Exception e) {
                rollback(conn);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private AuctionPaymentInfo lockAuction(Connection conn, int auctionId) throws SQLException {
        String sql = "SELECT auction_id, seller_id, winner_id, current_price FROM Auctions " +
                "WHERE auction_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Khong tim thay phien dau gia trong DB.");
                return new AuctionPaymentInfo(
                        rs.getInt("seller_id"),
                        rs.getInt("winner_id"),
                        rs.getDouble("current_price")
                );
            }
        }
    }

    private int findUserIdByUsername(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM Users WHERE username = ? FOR UPDATE")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Khong tim thay user: " + username);
    }

    private void releaseHold(Connection conn, int userId, double amount) throws SQLException {
        String sql = "UPDATE Users SET balance = balance + LEAST(locked_balance, ?), " +
                "locked_balance = GREATEST(locked_balance - ?, 0) WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setDouble(2, amount);
            ps.setInt(3, userId);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Khong the hoan tien dang giu.");
            }
        }
    }

    private void updateAuctionStatus(Connection conn, int auctionId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE Auctions SET status = ? WHERE auction_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, auctionId);
            ps.executeUpdate();
        }
    }

    private void insertWalletTx(Connection conn, int userId, Integer auctionId, String type,
                                double amount, String note) throws SQLException {
        String sql = "INSERT INTO WalletTransactions (user_id, auction_id, type, amount, note) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (auctionId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, auctionId);
            ps.setString(3, type);
            ps.setDouble(4, amount);
            ps.setString(5, note);
            ps.executeUpdate();
        }
    }

    private boolean validAmount(double amount) {
        return !Double.isNaN(amount) && !Double.isInfinite(amount)
                && amount > 0 && amount <= 1_000_000_000_000.0;
    }

    private void requireConnection(Connection conn) throws SQLException {
        if (conn == null) throw new SQLException("Khong ket noi duoc database.");
    }

    private void rollback(Connection conn) {
        try {
            if (conn != null) conn.rollback();
        } catch (SQLException ignored) {
        }
    }

    private record AuctionPaymentInfo(int sellerId, int winnerId, double amount) {}
}
