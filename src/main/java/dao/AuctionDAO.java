package dao;

import model.manager.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.auction.Auction;
import model.item.Item;
import java.time.LocalDateTime;

public class AuctionDAO {

    // Load tất cả phiên đấu giá từ DB
    public List<Auction> loadAllAuctionsFromDB() {
        List<Auction> auctions = new ArrayList<>();

        // Bước 1: Đọc toàn bộ data auction vào danh sách tạm (đóng ResultSet ngay)
        List<int[]> auctionIds = new ArrayList<>();   // chỉ để chứa dbAuctionId tương ứng từng auction
        String sql = "SELECT a.*, u.username AS seller_username, c.name AS category_name " +
                "FROM Auctions a " +
                "JOIN Users u ON a.seller_id = u.user_id " +
                "LEFT JOIN Categories c ON a.category_id = c.category_id " +
                "WHERE a.status IN ('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int dbAuctionId = rs.getInt("auction_id");
                String sellerUsername = rs.getString("seller_username");
                String categoryName = rs.getString("category_name");
                String title = rs.getString("title");
                String description = rs.getString("description");
                double startingPrice = rs.getDouble("starting_price");
                double currentPrice = rs.getDouble("current_price");
                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                byte[] imageData = rs.getBytes("image_data");
                String dbStatus = rs.getString("status");
                boolean antiSnipeEnabled = rs.getBoolean("anti_snipe_enabled");
                long antiSnipeThreshold = rs.getLong("anti_snipe_threshold_sec");
                long antiSnipeExtension = rs.getLong("anti_snipe_extension_sec");

                Item item = model.factory.ItemFactory.createItem(
                        categoryName, java.util.UUID.randomUUID().toString(),
                        title, description, startingPrice, "N/A",
                        imageData
                );

                Auction auction = new Auction(sellerUsername, item, startingPrice,
                        startTime, endTime, antiSnipeEnabled, antiSnipeThreshold, antiSnipeExtension);

                if ("RUNNING".equals(dbStatus)) {
                    auction.start();
                } else if ("FINISHED".equals(dbStatus)) {
                    auction.start();
                    auction.end();  // OPEN → RUNNING → FINISHED
                } else if ("PAID".equals(dbStatus)) {
                    auction.start();
                    auction.end();
                    // gọi method chuyển sang PAID nếu có
                } else if ("CANCELED".equals(dbStatus)) {
                    auction.cancel();  // OPEN → CANCELED
                }
                if (currentPrice > startingPrice) {
                    auction.setCurrentHighestBid(currentPrice);
                }

                auctions.add(auction);
                auctionIds.add(new int[]{dbAuctionId});  // lưu id để lát nữa load bids
            }
            System.out.println("✅ Đã đọc " + auctions.size() + " phiên đấu giá từ DB.");
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi load auctions từ DB:");
            e.printStackTrace();
            return auctions;
        }

        // Bước 2: Sau khi ResultSet ngoài đã đóng, mới load bid history cho từng auction
        BidDAO bidDAO = new BidDAO();
        for (int i = 0; i < auctions.size(); i++) {
            Auction auction = auctions.get(i);
            int dbAuctionId = auctionIds.get(i)[0];
            List<model.auction.BidTransaction> bids = bidDAO.loadBidsByAuctionId(dbAuctionId, auction.getId());
            auction.restoreBidHistory(bids);
        }
        System.out.println("✅ Đã load bid history cho " + auctions.size() + " phiên.");

        return auctions;
    }

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

    // Insert phiên đấu giá mới — kèm ảnh
    public boolean insertAuction(String sellerUsername, String categoryName, String title,
                                 String description, double startingPrice, long durationMinutes,
                                 byte[] imageData,
                                 boolean antiSnipeEnabled, long antiSnipeThreshold, long antiSnipeExtension) {
        String sql = "INSERT INTO Auctions (seller_id, category_id, title, description, image_data, " +
                "starting_price, current_price, start_time, end_time, status, " +
                "anti_snipe_enabled, anti_snipe_threshold_sec, anti_snipe_extension_sec) " +
                "SELECT u.user_id, c.category_id, ?, ?, ?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? MINUTE), 'RUNNING', " +
                "?, ?, ? " +
                "FROM Users u, Categories c " +
                "WHERE u.username = ? AND c.name = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setString(2, description);
            ps.setBytes(3, imageData);
            ps.setDouble(4, startingPrice);
            ps.setDouble(5, startingPrice);
            ps.setLong(6, durationMinutes);
            ps.setBoolean(7, antiSnipeEnabled);
            ps.setLong(8, antiSnipeThreshold);
            ps.setLong(9, antiSnipeExtension);
            ps.setString(10, sellerUsername);
            ps.setString(11, categoryName);

            int result = ps.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ LỖI KHI THÊM PHIÊN ĐẤU GIÁ VÀO DB:");
            e.printStackTrace();
        }
        return false;
    }
    // Tìm auction_id trong DB dựa theo title và seller username
    public int findAuctionIdByTitleAndSeller(String title, String sellerUsername) {
        String sql = "SELECT a.auction_id FROM Auctions a " +
                "JOIN Users u ON a.seller_id = u.user_id " +
                "WHERE a.title = ? AND u.username = ? " +
                "ORDER BY a.created_at DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, sellerUsername);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    public boolean updateEndTime(int auctionId, LocalDateTime newEndTime) {
        String sql = "UPDATE Auctions SET end_time = ? WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(newEndTime));
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean updateStatus(int auctionId, String newStatus) {
        String sql = "UPDATE Auctions SET status = ? WHERE auction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    //Đếm số auction theo tưừng trạng thái
    public java.util.Map<String,Integer> countAuctionsByStatus(){
        java.util.Map<String,Integer> result = new java.util.HashMap<>();
        String sql = "SELECT status, COUNT(*) AS cnt FROM Auctions GROUP BY status";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("status"), rs.getInt("cnt"));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return result;
    }
    //Tính tổng giá trị các bid đã đặt + tổng số bid
    public double[] sumBidVolumeAndCount(){
        double[] result = new double[]{0.0,0.0};
        String sql = "SELECT COALESCE(SUM(bid_amount), 0) AS total_volume, COUNT(*) AS total_count FROM Bids";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            if (rs.next()){
                result[0] = rs.getDouble("total_volume");
                result[1] = rs.getInt("total_count");
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return result;

    }



}