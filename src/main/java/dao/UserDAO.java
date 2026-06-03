package dao;

import model.manager.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    public boolean checkLogin(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

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
        String hashed = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt(12));
        String sql = "INSERT INTO Users (username, password_hash, email, full_name, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashed);
            ps.setString(3, email);
            ps.setString(4, fullName);
            ps.setString(5, role);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public model.user.User authenticate(String username, String password) {
        String sql = "SELECT password_hash, role, full_name, email, balance, locked_balance " +
                "FROM Users WHERE username = ? AND is_active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String dbHash = rs.getString("password_hash");
                boolean ok;
                try {
                    ok = org.mindrot.jbcrypt.BCrypt.checkpw(password, dbHash);
                } catch (IllegalArgumentException e) {
                    ok = false;
                }
                if (!ok) return null;
                return buildUserFromResultSet(username, rs);
            }
        } catch (SQLException e) {
            System.err.println("Loi thuc thi truy van authenticate:");
            e.printStackTrace();
        }
        return null;
    }

    public model.user.User findByUsername(String username) {
        String sql = "SELECT username, role, full_name, email, balance, locked_balance FROM Users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return buildUserFromResultSet(rs.getString("username"), rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private model.user.User buildUserFromResultSet(String username, ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");

        model.user.User user;
        if ("BIDDER".equalsIgnoreCase(role)) {
            user = new model.user.Bidder(username, null, email, fullName);
        } else if ("SELLER".equalsIgnoreCase(role)) {
            user = new model.user.Seller(username, null, email, fullName);
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            user = new model.user.Admin(username, null, email, fullName);
        } else {
            return null;
        }
        user.setBalance(rs.getDouble("balance"));
        user.setLockedBalance(rs.getDouble("locked_balance"));
        return user;
    }

    public int findUserIdByUsername(String username) {
        String sql = "SELECT user_id FROM Users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<UserSummary> getAllUsers() {
        List<UserSummary> users = new ArrayList<>();
        String sql = "SELECT user_id, username, email, full_name, role, is_active, created_at " +
                "FROM Users ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new UserSummary(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getBoolean("is_active"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.err.println("Loi getAllUsers:");
            e.printStackTrace();
        }
        return users;
    }

    public boolean setActive(int userId, boolean active) {
        String sql = "UPDATE Users SET is_active = ? WHERE user_id = ? AND role <> 'ADMIN'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public java.util.Map<String, Integer> countUsersByRole() {
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        String sql = "SELECT role, COUNT(*) AS cnt FROM Users GROUP BY role";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("role"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean checkUsernameExist(String username) {
        String sql = "SELECT 1 FROM Users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkEmailExist(String email) {
        String sql = "SELECT 1 FROM Users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
