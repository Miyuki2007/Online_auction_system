package protocol.requests;
import protocol.Request;
public class AdminGetAllUsersRequest extends Request{
    private static final long serialVersionUID = 1L;
    private final String adminUsername;

    public AdminGetAllUsersRequest(String adminUsername) {
        this.adminUsername = adminUsername;
    }
    @Override
    public Type getType(){return  Type.ADMIN_GET_ALL_USERS;}

    public String getAdminUsername() {
        return adminUsername;
    }
}
