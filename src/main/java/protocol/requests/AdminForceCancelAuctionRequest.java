package protocol.requests;
import protocol.Request;
public class AdminForceCancelAuctionRequest extends Request{
    private static final long serialVersionUID = 1L;
    private final String adminUsername;
    private final String auctionId;
    private final String reason;

    public AdminForceCancelAuctionRequest(String adminUsername, String auctionId, String reason) {
        this.adminUsername = adminUsername;
        this.auctionId = auctionId;
        this.reason = reason;
    }
    @Override
    public Type getType(){return Request.Type.ADMIN_FORCE_CANCEL_AUCTION;}

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getReason() {
        return reason;
    }
}
