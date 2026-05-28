package model.user;
import model.Entity;
public abstract class User extends Entity {
    private String username;
    private transient String password;
    private String email;
    private String fullName;
    protected User(String username, String password, String email, String fullName) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }
    public String getUsername() { return username;}
    public String getPassword() { return password; }
    public void setPassword(String password) {this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) {this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) {this.fullName = fullName;}
    public abstract String getRole();
    @Override
    public String getDisplayInfo() {
        return String.format("[%s] %s (%s)", getRole(), fullName, username);
    }
}
