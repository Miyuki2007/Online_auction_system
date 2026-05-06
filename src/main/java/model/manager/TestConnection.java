package model.manager;

import java.sql.Connection;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("Đang kiểm tra kết nối...");

        // Gọi phương thức kết nối từ class DatabaseConnection của bạn
        Connection conn = DatabaseConnection.getConnection();

        // Kiểm tra kết quả trả về
        if (conn != null) {
            System.out.println("🎉 TUYỆT VỜI! Đã kết nối thành công tới cơ sở dữ liệu auctiondb!");
        } else {
            System.out.println("💥 THẤT BẠI! Hãy kiểm tra lại các lỗi được in ra ở trên.");
        }
    }
}