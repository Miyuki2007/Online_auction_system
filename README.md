# 🏷️ Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

## 1. Mô Tả Hệ Thống

Hệ thống đấu giá trực tuyến là một nền tảng cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua sản phẩm trong thời gian xác định. Người bán đăng sản phẩm, người mua đặt giá theo thời gian thực, và giá bán cuối cùng được quyết định thông qua quá trình đấu giá công khai.

### Phạm Vi Hệ Thống

- **Ba vai trò người dùng:** Bidder (Người mua), Seller (Người bán), Admin (Quản trị viên)
- **Kiến trúc Client–Server** giao tiếp qua Java Socket (port `12345`)
- **Đấu giá real-time** — cập nhật tức thì cho tất cả client đang xem cùng phiên
- **Auto-Bidding** — đặt giá tự động theo ngưỡng tối đa
- **Anti-Sniping** — tự động kéo dài thời gian khi có bid vào phút cuối
- **Giao diện đồ họa JavaFX** với FXML và CSS

---

## 2. Công Nghệ Sử Dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Giao diện | JavaFX 21 + FXML |
| Cơ sở dữ liệu | MySQL 8.x |
| Build tool | Apache Maven 3.8+ |
| Bảo mật mật khẩu | BCrypt (jbcrypt 0.4) |
| Unit Test | JUnit 5 (Jupiter 5.10.2) |
| Kết nối DB | MySQL Connector/J 8.3.0 |

---

## 3. Tải Phần Mềm

| Phần mềm | Phiên bản | Link tải |
|---|---|---|
| **JDK 21** | 21 (LTS) | https://adoptium.net/temurin/releases/?version=21 |
| **Apache Maven** | 3.8+ | https://maven.apache.org/download.cgi |
| **MySQL Community Server** | 8.0+ | https://dev.mysql.com/downloads/mysql/ |

> JavaFX **không cần tải riêng** — Maven tự tải về khi build.

---

## 4. Cài Đặt JDK 21

### 🪟 Windows
1. Truy cập https://adoptium.net/temurin/releases/?version=21
2. Chọn **Windows** → **x64** → **JDK** → tải file `.msi`
3. Chạy file `.msi`, nhấn **Next** liên tục
4. Ở bước **"Custom Setup"**, đảm bảo **"Add to PATH"** được bật (mặc định đã bật)
5. Nhấn **Install** → **Finish**

### 🍎 macOS
1. Truy cập https://adoptium.net/temurin/releases/?version=21
2. Chọn **macOS** → **x64** (Intel) hoặc **aarch64** (Apple Silicon M1/M2/M3) → **JDK** → tải file `.pkg`
3. Chạy file `.pkg`, làm theo hướng dẫn

### 🐧 Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install -y wget apt-transport-https
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/trusted.gpg.d/adoptium.gpg
echo "deb https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install -y temurin-21-jdk
```

### 🐧 Linux (Fedora/RHEL/CentOS)
```bash
sudo dnf install -y java-21-openjdk-devel
```

**Kiểm tra (tất cả hệ điều hành):**
```bash
java -version
```
Kết quả mong muốn: `openjdk version "21.x.x"`

---

## 5. Cài Đặt Apache Maven

### 🪟 Windows
1. Tải file `.zip` từ https://maven.apache.org/download.cgi, giải nén vào ví dụ `C:\maven`
2. Thêm vào PATH:
   - Nhấn `Windows + S` → tìm **"Edit the system environment variables"**
   - Nhấn **"Environment Variables..."** → chọn **`Path`** trong *System variables* → nhấn **Edit**
   - Nhấn **New**, thêm: `C:\maven\bin`
   - Nhấn **OK** hết các cửa sổ → mở **CMD mới**

### 🍎 macOS
```bash
brew install maven
```
> Nếu chưa có Homebrew, cài tại: https://brew.sh

### 🐧 Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install -y maven
```

### 🐧 Linux (Fedora/RHEL/CentOS)
```bash
sudo dnf install -y maven
```

**Kiểm tra (tất cả hệ điều hành):**
```bash
mvn -version
```
Kết quả mong muốn: `Apache Maven 3.x.x`

---

## 6. Cài Đặt MySQL

### 🪟 Windows — MSI Installer

1. Truy cập https://dev.mysql.com/downloads/installer/
2. Tải file **`mysql-installer-community-8.x.x.msi`** (bản lớn ~450MB, không cần internet khi cài)
3. Chạy file `.msi` vừa tải
4. Chọn **"Server only"** → nhấn **Next** → **Execute**
5. Sau khi cài xong nhấn **Next** đến bước **"Type and Networking"**, giữ mặc định port **3306** → **Next**
6. Ở bước **"Accounts and Roles"**: nhập mật khẩu root vào ô **"MySQL Root Password"** và **"Repeat Password"** → **ghi nhớ mật khẩu này** → **Next**
7. Ở bước **"Windows Service"**: giữ mặc định **"Start the MySQL Server at System Startup"** ✅ → **Next** → **Execute** → **Finish**
8. Thêm MySQL vào PATH:
   - Mở **Environment Variables** (như Mục 5)
   - Thêm vào **Path**: `C:\Program Files\MySQL\MySQL Server 8.0\bin`
   - Mở **CMD mới**

> ✅ MySQL chạy ngầm tự động, không cần khởi động thủ công mỗi lần.

### 🍎 macOS
```bash
brew install mysql
brew services start mysql
```

Đặt mật khẩu root lần đầu:
```bash
mysql_secure_installation
```
Làm theo hướng dẫn, chọn mật khẩu và **ghi nhớ lại**.

### 🐧 Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

Đặt mật khẩu root lần đầu:
```bash
sudo mysql_secure_installation
```

Nếu không đặt được mật khẩu qua lệnh trên, chạy:
```bash
sudo mysql
```
```sql
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'mật_khẩu_của_bạn';
FLUSH PRIVILEGES;
exit;
```

### 🐧 Linux (Fedora/RHEL/CentOS)
```bash
sudo dnf install -y mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld
sudo mysql_secure_installation
```

**Kiểm tra đăng nhập (tất cả hệ điều hành):**
```bash
mysql -u root -p
```
Nhập mật khẩu vừa đặt. Vào được màn hình `mysql>` là thành công → gõ `exit;` để thoát.

---

## 7. Tạo Database

Mở Terminal (hoặc CMD trên Windows), đăng nhập MySQL:
```bash
mysql -u root -p
```

Chạy script tạo database — thay đường dẫn theo máy bạn:

**🪟 Windows:**
```sql
SOURCE C:/Users/tên_user/Downloads/Online_auction_system/auctiondb.sql;
```

**🍎 macOS / 🐧 Linux:**
```sql
SOURCE /home/tên_user/Downloads/Online_auction_system/auctiondb.sql;
```

Kiểm tra tạo thành công:
```sql
USE auctiondb;
SHOW TABLES;
```

Kết quả thấy các bảng: `Users`, `Auctions`, `Bids`, `AutoBids`, `Categories`, `Payments`.

Thoát MySQL:
```sql
exit;
```

> **Tài khoản Admin mặc định** đã được tạo sẵn trong script:
> - Username: `admin`
> - Password: `admin123`

---

## 8. Cấu Trúc Thư Mục

```
Online_auction_system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── client/           # Main, AuctionClient, Session, ServerListener
│   │   │   ├── controller/       # JavaFX controllers (login, register, home, admin, seller, bidder)
│   │   │   ├── dao/              # Data Access Objects (AuctionDAO, BidDAO, UserDAO, AutoBidDAO...)
│   │   │   ├── model/            # Domain model (User, Auction, Item, BidTransaction, AutoBid...)
│   │   │   ├── protocol/         # Lớp Request / Response và các subtype
│   │   │   └── server/           # AuctionServer, ClientHandler, ServerObserver
│   │   └── resources/
│   │       └── fxml/             # Giao diện FXML + CSS
│   └── test/
│       └── java/                 # Unit tests (JUnit 5)
├── auctiondb.sql                 # Script tạo database và bảng
├── auctiondb_test_schema.sql     # Schema dùng cho test
├── pom.xml
└── README.md
```

---

## 9. Cấu Hình Biến Môi Trường DB

Ứng dụng đọc thông tin kết nối từ biến môi trường. Biến `DB_PASS` là **bắt buộc**.

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/auctiondb` | Địa chỉ MySQL |
| `DB_USER` | `root` | Tên người dùng MySQL |
| `DB_PASS` | *(bắt buộc đặt)* | Mật khẩu MySQL đã đặt ở Mục 6 |

---
**🪟 Windows:**
Thêm vào Environment Variables:
- Nhấn `Windows + S` → tìm **"Edit the system environment variables"**
- Nhấn **"User variables for Administrator""** → nhấn **New...**
- Ở "Variable name:" là Biến, "Variable value:" là Mặc định`
- Nhấn **OK** hết các cửa sổ → mở **CMD mới**
> Thêm vào giúp mỗi lần mở Terminal không phải chạy lại.

**🍎 macOS / 🐧 Linux:**
```bash
export DB_PASS=mật_khẩu_của_bạn
```
> Lệnh này chỉ có hiệu lực trong **cửa sổ Terminal hiện tại**. Mỗi lần mở Terminal mới đều phải chạy lại.

---

## 10. Build Dự Án

Di chuyển vào thư mục gốc chứa `pom.xml`:

**🪟 Windows:**
```cmd
cd C:\đường\dẫn\tới\Online_auction_system
```

**🍎 macOS / 🐧 Linux:**
```bash
cd /đường/dẫn/tới/Online_auction_system
```

Build (bỏ qua test):
```bash
mvn clean package -DskipTests
```

Build kèm chạy unit test:
```bash
mvn clean package
```

---

## 11. Chạy Server và Client

> **Quan trọng:** Phải khởi động **Server trước**, sau đó mới chạy **Client**.

### Bước 1 – Chạy Server

Mở Terminal (hoặc CMD), chạy lần lượt:

**🪟 Windows:**
```cmd
cd C:\đường\dẫn\tới\Online_auction_system
mvn exec:java -Dexec.mainClass="server.AuctionServer"
```

**🍎 macOS / 🐧 Linux:**
```bash
export DB_PASS=mật_khẩu_của_bạn
cd /đường/dẫn/tới/Online_auction_system
mvn exec:java -Dexec.mainClass="server.AuctionServer"
```

✅ Server khởi động thành công khi thấy:
```
✅ Đã khởi động scheduler kiểm tra auction hết hạn (mỗi 5s).
Server đang chạy trên port: 12345
Đang chờ client kết nối...
```

**Giữ nguyên cửa sổ này, không đóng.**

### Bước 2 – Chạy Client

Mở **Terminal mới**, chạy lần lượt:

**🪟 Windows:**
```cmd
set DB_PASS=mật_khẩu_của_bạn
cd C:\đường\dẫn\tới\Online_auction_system
mvn javafx:run
```

**🍎 macOS / 🐧 Linux:**
```bash
export DB_PASS=mật_khẩu_của_bạn
cd /đường/dẫn/tới/Online_auction_system
mvn javafx:run
```

Cửa sổ đăng nhập JavaFX sẽ hiện ra.

### Chạy nhiều Client cùng lúc

Lặp lại **Bước 2** ở các Terminal mới. Server hỗ trợ tối đa 200 client đồng thời.

---

## 12. Tắt Server Khi Dùng Xong

Trong cửa sổ Terminal đang chạy Server, nhấn:
```
Ctrl + C
```
Server tự dọn dẹp và in ra `Server đã dừng.`

---

## 13. Chạy Unit Test

**🪟 Windows:**
```cmd
set DB_PASS=mật_khẩu_của_bạn
cd C:\đường\dẫn\tới\Online_auction_system
mvn test
```

**🍎 macOS / 🐧 Linux:**
```bash
export DB_PASS=mật_khẩu_của_bạn
cd /đường/dẫn/tới/Online_auction_system
mvn test
```

---

## 14. Danh Sách Chức Năng Đã Hoàn Thành

### 👤 Bidder (Người mua)
- [x] Đăng ký / Đăng nhập tài khoản
- [x] Xem danh sách phiên đấu giá đang diễn ra
- [x] Xem chi tiết phiên đấu giá (thông tin sản phẩm, giá hiện tại, thời gian còn lại)
- [x] Đặt giá (bid) theo thời gian thực
- [x] Đăng ký Auto-Bid (đấu giá tự động đến ngưỡng tối đa)
- [x] Hủy Auto-Bid
- [x] Xem lịch sử bid của bản thân
- [x] Nhận thông báo real-time khi có bid mới hoặc phiên kết thúc

### 🛒 Seller (Người bán)
- [x] Tạo phiên đấu giá mới (tiêu đề, mô tả, hình ảnh, giá khởi điểm, thời gian, danh mục)
- [x] Cấu hình Anti-Sniping (ngưỡng và thời gian gia hạn)
- [x] Cấu hình giá Mua Ngay (Buy Now)
- [x] Xem danh sách phiên đấu giá của mình
- [x] Hủy phiên đấu giá

### 🔧 Admin (Quản trị viên)
- [x] Xem dashboard thống kê hệ thống
- [x] Xem danh sách tất cả người dùng
- [x] Kích hoạt / Vô hiệu hóa tài khoản người dùng
- [x] Hủy phiên đấu giá bất kỳ (force cancel)

### ⚙️ Hệ Thống
- [x] Kiến trúc Client–Server qua Java Socket
- [x] Broadcast real-time đến các client đang xem cùng phiên
- [x] Scheduler tự động kiểm tra và đóng phiên hết hạn (mỗi 5 giây)
- [x] Mã hóa mật khẩu BCrypt
- [x] Kết nối database qua biến môi trường
- [x] Unit test JUnit 5 cho model, manager, server

---
## 15. Link báo cáo và video demo
- Link báo cáo: 