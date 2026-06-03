package model.manager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = getEnvOrDefault("DB_URL", "jdbc:mysql://localhost:3306/auctiondb");
    private static final String USER = getEnvOrDefault("DB_USER", "root");
    private static final String PASSWORD = System.getenv("DB_PASS");
    private static volatile boolean walletSchemaChecked = false;

    static {
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new RuntimeException("Vui long set bien moi truong DB_PASS.");
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
            Connection conn = DriverManager.getConnection(urlWithTimeout, USER, PASSWORD);
            ensureWalletSchema(conn);
            return conn;
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Loi ket noi Database: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void ensureWalletSchema(Connection conn) throws SQLException {
        if (walletSchemaChecked) return;
        synchronized (DatabaseConnection.class) {
            if (walletSchemaChecked) return;

            if (!columnExists(conn, "Users", "locked_balance")) {
                try (java.sql.Statement st = conn.createStatement()) {
                    st.executeUpdate("ALTER TABLE Users ADD COLUMN locked_balance DECIMAL(15,2) DEFAULT 0.00");
                }
            }

            if (!tableExists(conn, "WalletTransactions")) {
                try (java.sql.Statement st = conn.createStatement()) {
                    st.executeUpdate("""
                            CREATE TABLE WalletTransactions (
                                transaction_id INT AUTO_INCREMENT PRIMARY KEY,
                                user_id INT NOT NULL,
                                auction_id INT NULL,
                                type ENUM('DEPOSIT','WITHDRAW','HOLD','RELEASE','PAY_SELLER') NOT NULL,
                                amount DECIMAL(15, 2) NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                note VARCHAR(255),
                                FOREIGN KEY (user_id) REFERENCES Users(user_id),
                                FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id)
                            )
                            """);
                }
            }

            walletSchemaChecked = true;
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tableName, columnName)) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tableName.toLowerCase(), columnName)) {
            return rs.next();
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, tableName, null)) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, tableName.toLowerCase(), null)) {
            return rs.next();
        }
    }
}
