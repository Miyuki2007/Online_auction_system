package model.user;

public class Admin extends User{
    public Admin(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
    }
    @Override
    public String getRole() {return "ADMIN"; }
}
