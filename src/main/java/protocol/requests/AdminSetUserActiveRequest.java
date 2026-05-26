package protocol.requests;
import protocol.Request;
/*
- Admin yêu cầu khóa (active = false) hoặc mở khóa (active = true) một user
 */
public class AdminSetUserActiveRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final String adminUsername;
    private final int targetUserId;
    private final boolean active;

    public AdminSetUserActiveRequest(String adminUsername, int targetUserId, boolean active) {
        this.adminUsername = adminUsername;
        this.targetUserId = targetUserId;
        this.active = active;
    }
    @Override
    public Type getType() {return Request.Type.ADMIN_SET_USER_ACTIVE;}

    public String getAdminUsername() {
        return adminUsername;
    }

    public int getTargetUserId() {
        return targetUserId;
    }

    public boolean isActive() {
        return active;
    }
}
