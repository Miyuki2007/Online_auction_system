package protocol.requests;

import protocol.Request;

public class LoginRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Type getType() { return Type.LOGIN; }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}