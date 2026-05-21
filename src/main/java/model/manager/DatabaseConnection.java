package model.manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Lấy thông tin từ Biến môi trường, nếu không có thì dùng giá trị mặc định (localhost)
    private static final String URL = getEnvOrDefault("DB_URL", "jdbc:mysql://localhost:3306/auctiondb");
    private static final String USER = getEnvOrDefault("DB_USER", "root");
    private static final String PASSWORD = getEnvOrDefault("DB_PASS", "Hoangngoc07@vb");

    private static Connection connection = null;

    private DatabaseConnection() {}

    // Hàm hỗ trợ lấy biến môi trường hoặc dùng giá trị mặc định
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("❌ Lỗi kết nối Database: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}