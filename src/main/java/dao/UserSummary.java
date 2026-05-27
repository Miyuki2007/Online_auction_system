package dao;
import java.io.Serializable;
import java.time.LocalDateTime;
/*
- DTO (Data Transfer Object) chứa thông tin tóm tắt của user để Admin xem trong màn hình quản lý.

- Khác với class {@code User}: DTO này có thêm các thông tin
meta (user_id trong DB, trạng thái is_active, ngày tạo) mà
model {@code User} không có. Đồng thời KHÔNG chứa password.

- Serializable để gửi qua socket từ server xuống client.
 */
public class UserSummary implements Serializable{
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final String username;
    private final String email;
    private final String fullName;
    private final String role;
    private final boolean active;
    private final LocalDateTime createdAt;

    public UserSummary(int userId, String username, String email, String fullName, String role, boolean active, LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
