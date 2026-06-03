package model.user;
import model.Entity;
public abstract class User extends Entity {
    private String username;
    private transient String password;
    private String email;
    private String fullName;
    private double balance;
    private double lockedBalance;
    protected User(String username, String password, String email, String fullName) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.balance = 0.0;
        this.lockedBalance = 0.0;
    }
    public String getUsername() { return username;}
    public String getPassword() { return password; }
    public void setPassword(String password) {this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) {this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) {this.fullName = fullName;}
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public double getLockedBalance() { return lockedBalance; }
    public void setLockedBalance(double lockedBalance) { this.lockedBalance = lockedBalance; }
    public double getAvailableBalance() { return Math.max(0.0, balance - lockedBalance); }
    public abstract String getRole();
    public boolean authenticate(String password){
        if (this.password == null || password == null) return false;
        return this.password.equals(password);
    }
    @Override
    public String getDisplayInfo() {
        return String.format("[%s] %s (%s)", getRole(), fullName, username);
    }
}
