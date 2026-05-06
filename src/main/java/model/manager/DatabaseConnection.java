package model.manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // 1. Khai báo thông tin kết nối (ĐÃ SỬA LẠI CHUẨN)
    private static final String URL = "jdbc:mysql://localhost:3306/auctiondb";
    private static final String USER = "root";
    private static final String PASSWORD = "Hoangngoc07@vb"; // Nhớ điền mật khẩu MySQL của bạn vào đây

    // ... các phần dưới giữ nguyên ...
    // 2. Biến tĩnh lưu trữ kết nối duy nhất
    private static Connection connection = null;

    // 3. Constructor private để ngăn việc dùng từ khóa 'new' từ bên ngoài class
    private DatabaseConnection() {
    }

    // 4. Hàm cấp phát kết nối
    public static Connection getConnection() {
        // Nếu chưa có kết nối nào hoặc kết nối đã bị đóng, thì mới tạo mới
        try {
            if (connection == null || connection.isClosed()) {
                // Tải driver (có thể bỏ qua với các bản JDBC mới, nhưng viết vào cho an toàn)
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Thực hiện kết nối
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Kết nối MySQL thành công!");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Không tìm thấy thư viện MySQL JDBC Driver!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ Kết nối thất bại. Kiểm tra lại URL, Username hoặc Password!");
            e.printStackTrace();
        }

        return connection;
    }
}