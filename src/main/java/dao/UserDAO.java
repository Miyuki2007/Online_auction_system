package dao;

import model.manager.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    // Ví dụ hàm kiểm tra đăng nhập
    public boolean checkLogin(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Hàm cập nhật số dư (balance) khi nạp tiền hoặc trừ tiền
    public boolean updateBalance(int userId, double newBalance) {
        String sql = "UPDATE Users SET balance = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, newBalance);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    public boolean registerUser(String username, String password, String email, String fullName, String role) {
        // Câu lệnh SQL để lưu user mới
        String hashed = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt(12));
        String sql = "INSERT INTO Users (username, password_hash, email, full_name, role) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password); // Lưu ý: thực tế nên dùng BCrypt để mã hóa pass
            ps.setString(3, email);
            ps.setString(4, fullName);
            ps.setString(5, role);

            int result = ps.executeUpdate();
            return result > 0; // Trả về true nếu lưu thành công
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

    }
    public model.user.User authenticate(String username, String password) {
        // 1. Câu lệnh SQL lấy đầy đủ thông tin để khởi tạo Object User
        String sql = "SELECT password_hash, role, full_name, email FROM Users " +
                "WHERE username = ? AND is_active = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())  return null;
                String dbHash = rs.getString("password_hash");
                //BCrypt.checkpw an toàn với hash bị malformed (trả về false)
                boolean ok;
                try {
                    ok = org.mindrot.jbcrypt.BCrypt.checkpw(password, dbHash);
                }catch(IllegalArgumentException e) {
                    ok = false; //// dbHash không phải BCrypt (data cũ plain-text) → fail
                }
                if (!ok) return null;
                // 2. Lấy thông tin từ các cột trong Database
                String role = rs.getString("role");
                String fullName = rs.getString("full_name");
                String email = rs.getString("email");
                // 3. Trả về đúng loại đối tượng (Bidder hoặc Seller) dựa trên vai trò
                // Giả sử model của bạn có constructor: (username, null, email, fullName): Không truyền password thật vào object trả về
                    if ("BIDDER".equalsIgnoreCase(role)) {
                        return new model.user.Bidder(username, null, email, fullName);
                    } else if ("SELLER".equalsIgnoreCase(role)) {
                        return new model.user.Seller(username, null, email, fullName);
                    }
                    else if ("ADMIN".equalsIgnoreCase(role)) {
                        return new model.user.Admin(username, null, email, fullName);
                    }
                }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi thực thi truy vấn authenticate:");
            e.printStackTrace();
        }
        return null; // Trả về null nếu không tìm thấy user hoặc sai mật khẩu
    }
    public model.user.User findByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    String fullName = rs.getString("full_name");
                    String email = rs.getString("email");
                    String password = rs.getString("password_hash");

                    if ("BIDDER".equalsIgnoreCase(role)) {
                        return new model.user.Bidder(username, null, email, fullName);
                    } else if ("SELLER".equalsIgnoreCase(role)){
                        return new model.user.Seller(username, null, email, fullName);
                    } else if ("ADMIN".equalsIgnoreCase(role)){
                        return new model.user.Admin(username, null, email, fullName);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    // Tìm user_id từ username
    public int findUserIdByUsername(String username) {
        String sql = "SELECT user_id FROM Users WHERE username = ?";
        try (java.sql.Connection conn = model.manager.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    // Lấy danh sách tất cả user kèm trạng thái active
    public java.util.List<dao.UserSummary> getAllUsers(){
        java.util.List<dao.UserSummary> users = new java.util.ArrayList<>();
        String sql = "SELECT user_id, username, email, full_name, role, is_active, created_at " + "FROM Users ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()){
                dao.UserSummary u = new dao.UserSummary(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getBoolean("is_active"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                users.add(u);
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getAllUsers:");
            e.printStackTrace();
        }
        return users;
    }

    //Khóa/ mở khóa một user
    public boolean setActive(int userId, boolean active){
        String sql = "UPDATE Users SET is_active = ? WHERE user_id = ? AND role <> 'ADMIN'";
        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setBoolean(1,active);
            ps.setInt(2,userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }
    // Đếm số lượng user theo role để hiển thị thống kê trên dashboard
    public java.util.Map<String,Integer> countUsersByRole(){
        java.util.Map<String,Integer> result = new java.util.HashMap<>();
        String sql = "SELECT role, COUNT(*) AS cnt FROM Users GROUP BY role";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()){
                result.put(rs.getString("role"),rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
    // Hàm kiểm tra trùng tên đăng nhập
    public boolean checkUsernameExist(String username) {
        String sql = "SELECT 1 FROM Users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Trả về true nếu đã tồn tại
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Hàm kiểm tra trùng email
    public boolean checkEmailExist(String email) {
        String sql = "SELECT 1 FROM Users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Trả về true nếu đã tồn tại
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}