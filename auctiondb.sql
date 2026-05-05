-- Tạo database
CREATE DATABASE IF NOT EXISTS auctiondb;
USE auctiondb;

-- Bảng Người dùng (Lưu trữ cả người bán và người mua)
CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(15),
    address TEXT,
    balance DECIMAL(15, 2) DEFAULT 0.00, -- Số dư ví điện tử nếu có
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Danh mục sản phẩm
CREATE TABLE Categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

-- Bảng Phiên đấu giá (Đại diện cho sản phẩm đang được đấu giá)
CREATE TABLE Auctions (
    auction_id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    category_id INT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    starting_price DECIMAL(15, 2) NOT NULL, -- Giá khởi điểm
    current_price DECIMAL(15, 2) NOT NULL,  -- Giá hiện tại (cập nhật liên tục khi có bid)
    buy_now_price DECIMAL(15, 2),           -- Giá mua ngay (tùy chọn)
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status ENUM('PENDING', 'ACTIVE', 'CLOSED', 'CANCELED') DEFAULT 'PENDING',
    winner_id INT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES Categories(category_id) ON DELETE SET NULL,
    FOREIGN KEY (winner_id) REFERENCES Users(user_id) ON DELETE SET NULL
);

-- Bảng Lượt trả giá (Lịch sử đấu giá của từng phiên)
CREATE TABLE Bids (
    bid_id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_id INT NOT NULL,
    bid_amount DECIMAL(15, 2) NOT NULL,
    bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES Users(user_id) ON DELETE CASCADE
);

-- Bảng Giao dịch/Thanh toán (Xử lý khi phiên đấu giá kết thúc thành công)
CREATE TABLE Payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    buyer_id INT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(50),
    payment_status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') DEFAULT 'PENDING',
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES Auctions(auction_id),
    FOREIGN KEY (buyer_id) REFERENCES Users(user_id)
);

-- Tạo Index để tối ưu hóa truy vấn tìm kiếm
CREATE INDEX idx_auction_status ON Auctions(status);
CREATE INDEX idx_auction_endtime ON Auctions(end_time);
CREATE INDEX idx_bids_auction ON Bids(auction_id);