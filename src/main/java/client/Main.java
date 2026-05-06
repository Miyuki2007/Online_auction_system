package client;

import controller.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;


import model.manager.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//public class Main extends Application {
//
//    @Override
//    public void start(Stage primaryStage) throws Exception {
//        SceneManager.getInstance().setPrimaryStage(primaryStage);
//        SceneManager.getInstance().switchScene("login.fxml", "Đăng nhập - Hệ thống đấu giá");
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}


public class Main {
    public static void main(String[] args) {
        // Lấy đối tượng kết nối
        Connection conn = DatabaseConnection.getConnection();

        if (conn != null) {
            try {
                // 1. Thử Thêm một danh mục mới vào DB
                String insertSql = "INSERT INTO Categories (name, description) VALUES (?, ?)";
                // Dùng PreparedStatement để chống lỗi bảo mật SQL Injection
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setString(1, "Điện thoại");
                insertStmt.setString(2, "Các thiết bị di động thông minh");

                int rowsInserted = insertStmt.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("Đã thêm danh mục mới thành công!");
                }

                // 2. Thử Đọc dữ liệu lên xem đã vào chưa
                String selectSql = "SELECT * FROM Categories";
                PreparedStatement selectStmt = conn.prepareStatement(selectSql);
                ResultSet rs = selectStmt.executeQuery();

                System.out.println("--- DANH SÁCH CATEGORIES ---");
                while (rs.next()) {
                    int id = rs.getInt("category_id");
                    String name = rs.getString("name");
                    String desc = rs.getString("description");

                    System.out.println(id + " | " + name + " | " + desc);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}