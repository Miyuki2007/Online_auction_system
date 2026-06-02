package protocol.responses;
import model.auction.Auction;
import model.auction.AutoBid;
import model.auction.BidTransaction;
import java.io.Serializable;
import java.util.List;
public class MyBidHistoryResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    //Các lần manual bid (autobid được kích hoạt) của user
    private final List<BidTransaction> manualBids;
    //AutoBid đang đăng kí (null nếu chưa đăng kí)
    private final AutoBid activAutoBid;

    public MyBidHistoryResponse(List<BidTransaction> manualBids, AutoBid activAutoBid) {
        this.manualBids = manualBids;
        this.activAutoBid = activAutoBid;
    }

    public List<BidTransaction> getManualBids() {
        return manualBids;
    }
    public AutoBid getActivAutoBid() {
        return activAutoBid;
    }
}
