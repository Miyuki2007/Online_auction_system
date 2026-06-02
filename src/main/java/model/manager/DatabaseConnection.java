package model.manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = getEnvOrDefault("DB_URL", "jdbc:mysql://localhost:3306/auctiondb");
    private static final String USER = getEnvOrDefault("DB_USER", "root");
    private static final String PASSWORD = System.getenv("DB_PASS");

    static {
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new RuntimeException("Vui lòng set biến môi trường DB_PASS.");
        }
    }

    private DatabaseConnection() {}

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String urlWithTimeout = URL.contains("?")
                    ? URL + "&connectTimeout=5000&socketTimeout=10000&useSSL=false&allowPublicKeyRetrieval=true"
                    : URL + "?connectTimeout=5000&socketTimeout=10000&useSSL=false&allowPublicKeyRetrieval=true";
            return DriverManager.getConnection(urlWithTimeout, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("❌ Lỗi kết nối Database: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}