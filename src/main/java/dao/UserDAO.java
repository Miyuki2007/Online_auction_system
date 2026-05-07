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
        String sql = "SELECT * FROM Users WHERE username = ? AND password_hash = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // 2. Lấy thông tin từ các cột trong Database
                    String role = rs.getString("role");
                    String fullName = rs.getString("full_name");
                    String email = rs.getString("email");

                    // 3. Trả về đúng loại đối tượng (Bidder hoặc Seller) dựa trên vai trò
                    // Giả sử model của bạn có constructor: (username, password, email, fullName)
                    if ("BIDDER".equalsIgnoreCase(role)) {
                        return new model.user.Bidder(username, password, email, fullName);
                    } else if ("SELLER".equalsIgnoreCase(role)) {
                        return new model.user.Seller(username, password, email, fullName);
                    }
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
                        return new model.user.Bidder(username, password, email, fullName);
                    } else {
                        return new model.user.Seller(username, password, email, fullName);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}