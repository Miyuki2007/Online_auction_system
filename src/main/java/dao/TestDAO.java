package dao;

import java.util.Scanner;

public class TestDAO {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("--- CHƯƠNG TRÌNH KIỂM THỬ HỆ THỐNG ĐẤU GIÁ ---");

        // 1. Test kết nối và lấy danh sách đấu giá
        AuctionDAO auctionDAO = new AuctionDAO();
        System.out.println("\n1. Đang tải danh sách phiên đấu giá hoạt động...");
        auctionDAO.getActiveAuctions();

        // 2. Test chức năng Đăng nhập (UserDAO)
        UserDAO userDAO = new UserDAO();
        System.out.println("\n2. Kiểm tra đăng nhập:");
        System.out.print("Nhập username: ");
        String user = sc.nextLine();
        System.out.print("Nhập password: ");
        String pass = sc.nextLine();

        if (userDAO.checkLogin(user, pass)) {
            System.out.println("✅ Đăng nhập thành công!");

            // 3. Test đặt giá (BidDAO)
            System.out.println("\n3. Thử nghiệm đặt giá cho sản phẩm ID 1:");
            System.out.print("Nhập số tiền bạn muốn trả: ");
            double amount = sc.nextDouble();

            BidDAO bidDAO = new BidDAO();
            // Giả sử ID người dùng là 1, ID phiên đấu giá là 1
            boolean bidSuccess = bidDAO.placeBid(1, 1, amount);

            if (bidSuccess) {
                // Nếu lưu lịch sử bid thành công, cập nhật giá mới vào bảng Auctions
                boolean updateSuccess = auctionDAO.updateCurrentPrice(1, amount, 1);
                if (updateSuccess) {
                    System.out.println("🎉 Đặt giá thành công! Giá sản phẩm đã được cập nhật.");
                }
            } else {
                System.out.println("❌ Lỗi khi đặt giá.");
            }
        } else {
            System.out.println("❌ Sai tài khoản hoặc mật khẩu.");
        }

        sc.close();
    }
}