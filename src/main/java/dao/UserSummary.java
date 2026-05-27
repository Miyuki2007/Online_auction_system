package dao;

import java.io.Serializable;

public class UserSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private boolean active;

    public UserSummary(int userId, String username, String fullName, String email, String role, boolean active) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    // Getters
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
}