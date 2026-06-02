package protocol.requests;
import protocol.Request;
public class CancelAutoBidRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final String autoBidId;
    private final String auctionId;

    public CancelAutoBidRequest(String autoBidId, String auctionId) {
        this.autoBidId = autoBidId;
        this.auctionId = auctionId;
    }
    @Override
    public Type getType(){ return Type.CANCEL_AUTO_BID; }

    public String getAutoBidId() {
        return autoBidId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}
