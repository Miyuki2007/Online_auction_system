package protocol.requests;
import protocol.Request;
public class GetMyBidHistoryRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final String auctionId;
    public GetMyBidHistoryRequest(String auctionId){
        this.auctionId = auctionId;
    }
    @Override
    public Type getType() {
        return Request.Type.GET_MY_BID_HISTORY;
    }
    public String getAuctionId() { return auctionId;}
}
