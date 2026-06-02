package dao;

import model.auction.AutoBid;
import model.manager.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO quản lý việc lưu trữ và khôi phục AutoBid từ Database.
 *
 * Yêu cầu bảng AutoBids trong DB (chạy đoạn SQL bên dưới nếu chưa có):
 *
 * CREATE TABLE AutoBids (
 *     autobid_id   INT AUTO_INCREMENT PRIMARY KEY,
 *     uuid         VARCHAR(36)    NOT NULL UNIQUE,   -- UUID từ AutoBid.getId()
 *     auction_id   INT            NOT NULL,
 *     bidder_id    INT            NOT NULL,
 *     max_bid      DECIMAL(15,2)  NOT NULL,
 *     increment    DECIMAL(15,2)  NOT NULL,
 *     is_active    BOOLEAN        DEFAULT TRUE,
 *     created_at   DATETIME       DEFAULT NOW(),
 *     FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id) ON DELETE CASCADE,
 *     FOREIGN KEY (bidder_id)  REFERENCES Users(user_id)       ON DELETE CASCADE
 * );
 * CREATE INDEX idx_autobid_auction ON AutoBids(auction_id);
 * CREATE INDEX idx_autobid_bidder  ON AutoBids(bidder_id);
 */
public class AutoBidDAO {

    // -------------------------------------------------------------------------
    // INSERT
    // -------------------------------------------------------------------------

    /**
     * Lưu một AutoBid mới xuống DB ngay khi người dùng đăng ký.
     *
     * @param autoBid      đối tượng AutoBid vừa tạo trong RAM
     * @param dbAuctionId  auction_id (INTEGER) trong bảng Auctions
     * @param dbBidderId   user_id    (INTEGER) trong bảng Users
     * @return true nếu lưu thành công
     */
    public boolean insertAutoBid(AutoBid autoBid, int dbAuctionId, int dbBidderId) {
        String sql = "INSERT INTO AutoBids (uuid, auction_id, bidder_id, max_bid, increment, is_active, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, autoBid.getId());
            ps.setInt   (2, dbAuctionId);
            ps.setInt   (3, dbBidderId);
            ps.setDouble(4, autoBid.getMaxBid());
            ps.setDouble(5, autoBid.getIncrement());
            ps.setBoolean(6, autoBid.isActive());
            ps.setTimestamp(7, Timestamp.valueOf(autoBid.getCreateAt()));

            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                System.out.println("✅ AutoBid lưu DB: uuid=" + autoBid.getId()
                        + ", auction=" + dbAuctionId + ", bidder=" + dbBidderId);
            }
            return ok;

        } catch (SQLException e) {
            System.err.println("❌ Lỗi insertAutoBid: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // LOAD (dùng khi server khởi động lại)
    // -------------------------------------------------------------------------

    /**
     * Load tất cả AutoBid đang active của một phiên đấu giá.
     * Dùng trong AuctionManager.loadAuctionsFromDB() để khôi phục trạng thái.
     *
     * @param dbAuctionId  auction_id (INTEGER) trong bảng Auctions
     * @param auctionUuid  UUID của Auction trong RAM (gán vào AutoBid.auctionId)
     * @return danh sách AutoBid còn active
     */
    public List<AutoBid> loadActiveAutoBidsByAuction(int dbAuctionId, String auctionUuid) {
        List<AutoBid> result = new ArrayList<>();

        String sql = "SELECT ab.uuid, u.username AS bidder_username, "
                + "       ab.max_bid, ab.increment, ab.is_active, ab.created_at "
                + "FROM AutoBids ab "
                + "JOIN Users u ON ab.bidder_id = u.user_id "
                + "WHERE ab.auction_id = ? AND ab.is_active = TRUE "
                + "ORDER BY ab.created_at ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dbAuctionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String uuid           = rs.getString("uuid");
                    String bidderUsername = rs.getString("bidder_username");
                    double maxBid         = rs.getDouble("max_bid");
                    double increment      = rs.getDouble("increment");
                    boolean isActive      = rs.getBoolean("is_active");
                    LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toLocalDateTime()
                            : LocalDateTime.now();

                    // Dùng restore-constructor để giữ nguyên uuid và createdAt gốc
                    AutoBid ab = new AutoBid(uuid, auctionUuid, bidderUsername,
                            maxBid, increment, createdAt, isActive);
                    result.add(ab);
                }
            }

            System.out.println("✅ Đã load " + result.size()
                    + " auto-bid active cho auction_id=" + dbAuctionId);

        } catch (SQLException e) {
            System.err.println("❌ Lỗi loadActiveAutoBidsByAuction: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Load tất cả AutoBid (kể cả inactive) của một phiên — dùng để hiển thị lịch sử.
     */
    public List<AutoBid> loadAllAutoBidsByAuction(int dbAuctionId, String auctionUuid) {
        List<AutoBid> result = new ArrayList<>();

        String sql = "SELECT ab.uuid, u.username AS bidder_username, "
                + "       ab.max_bid, ab.increment, ab.is_active, ab.created_at "
                + "FROM AutoBids ab "
                + "JOIN Users u ON ab.bidder_id = u.user_id "
                + "WHERE ab.auction_id = ? "
                + "ORDER BY ab.created_at ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dbAuctionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String uuid           = rs.getString("uuid");
                    String bidderUsername = rs.getString("bidder_username");
                    double maxBid         = rs.getDouble("max_bid");
                    double increment      = rs.getDouble("increment");
                    boolean isActive      = rs.getBoolean("is_active");
                    LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toLocalDateTime()
                            : LocalDateTime.now();

                    AutoBid ab = new AutoBid(uuid, auctionUuid, bidderUsername,
                            maxBid, increment, createdAt, isActive);
                    result.add(ab);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Lỗi loadAllAutoBidsByAuction: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // DEACTIVATE
    // -------------------------------------------------------------------------

    /**
     * Đánh dấu một AutoBid là inactive (is_active = false) theo UUID.
     * Gọi khi: người dùng huỷ, giá vượt maxBid, hoặc phiên kết thúc.
     *
     * @param autoBidUuid  AutoBid.getId() — UUID dạng String
     * @return true nếu cập nhật thành công
     */
    public boolean deactivateByUuid(String autoBidUuid) {
        String sql = "UPDATE AutoBids SET is_active = FALSE WHERE uuid = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, autoBidUuid);
            boolean ok = ps.executeUpdate() > 0;
            if (ok) {
                System.out.println("✅ AutoBid deactivated trong DB: uuid=" + autoBidUuid);
            } else {
                System.err.println("⚠️ Không tìm thấy AutoBid để deactivate: uuid=" + autoBidUuid);
            }
            return ok;

        } catch (SQLException e) {
            System.err.println("❌ Lỗi deactivateByUuid: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deactivate toàn bộ AutoBid của một phiên đấu giá khi phiên kết thúc.
     *
     * @param dbAuctionId  auction_id (INTEGER) trong bảng Auctions
     * @return số dòng được cập nhật
     */
    public int deactivateAllByAuction(int dbAuctionId) {
        String sql = "UPDATE AutoBids SET is_active = FALSE "
                + "WHERE auction_id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dbAuctionId);
            int rows = ps.executeUpdate();
            System.out.println("✅ Deactivated " + rows
                    + " auto-bid cho auction_id=" + dbAuctionId);
            return rows;

        } catch (SQLException e) {
            System.err.println("❌ Lỗi deactivateAllByAuction: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // QUERY HELPERS
    // -------------------------------------------------------------------------

    /**
     * Kiểm tra xem người dùng có auto-bid active trong phiên này chưa.
     * Dùng để validate trước khi đăng ký mới (tránh duplicate).
     */
    public boolean hasActiveAutoBid(int dbAuctionId, int dbBidderId) {
        String sql = "SELECT 1 FROM AutoBids "
                + "WHERE auction_id = ? AND bidder_id = ? AND is_active = TRUE "
                + "LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, dbAuctionId);
            ps.setInt(2, dbBidderId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("❌ Lỗi hasActiveAutoBid: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}