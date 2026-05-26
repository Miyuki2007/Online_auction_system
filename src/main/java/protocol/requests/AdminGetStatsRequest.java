package protocol.requests;

import protocol.Request;
public class AdminGetStatsRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final String adminUsername;
    public AdminGetStatsRequest(String adminUsername){
        this.adminUsername = adminUsername;
    }
    @Override
    public Type getType(){ return Type.ADMIN_GET_STATS;
    }
    public String getAdminUsername() {
        return adminUsername;
    }
}
