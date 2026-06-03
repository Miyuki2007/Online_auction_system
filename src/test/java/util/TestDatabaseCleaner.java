package util;

import model.manager.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;

 // Helper class để clean dữ liệu test trong database trước mỗi test.
public class TestDatabaseCleaner {

    /**
     * Xóa sạch dữ liệu trong các bảng (giữ schema + Categories mặc định).
     * Gọi trong @BeforeEach của test cần DB sạch.
     */
    public static void cleanAll() {
        // Safety check: chỉ cho phép clean DB test
        String url = System.getenv("DB_URL");
        if (url == null || !url.contains("auctiondb_test")) {
            throw new RuntimeException(
                    "TestDatabaseCleaner CHỈ được dùng với database test!\n" +
                            "Hiện tại DB_URL = " + url + "\n" +
                            "Set DB_URL=jdbc:mysql://localhost:3306/auctiondb_test " +
                            "trong Environment variables của IntelliJ JUnit config."
            );
        }

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            throw new RuntimeException("Không kết nối được DB test — kiểm tra DB_URL, DB_USER, DB_PASS");
        }

        try (conn; Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("TRUNCATE TABLE WalletTransactions");
            stmt.execute("TRUNCATE TABLE AutoBids");
            stmt.execute("TRUNCATE TABLE Bids");
            stmt.execute("TRUNCATE TABLE Payments");
            stmt.execute("TRUNCATE TABLE Auctions");
            stmt.execute("TRUNCATE TABLE Users");
            stmt.execute("TRUNCATE TABLE Categories");  // ← truncate luôn
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            stmt.execute("""
    INSERT INTO Categories (name, description) VALUES
        ('ELECTRONICS', 'Đồ điện tử, công nghệ'),
        ('VEHICLE',     'Phương tiện giao thông'),
        ('ART',         'Các tác phẩm nghệ thuật'),
        ('OTHERS',      'Các sản phẩm khác')
        """);
        } catch (Exception e) {
            throw new RuntimeException("Không thể clean test database: " + e.getMessage(), e);
        }
    }
}
