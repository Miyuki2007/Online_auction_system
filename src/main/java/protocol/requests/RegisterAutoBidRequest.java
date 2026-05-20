package protocol.requests;
import protocol.Request;

import java.io.Serializable;

public class RegisterAutoBidRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final String auctionId;
    private final String bidderId;
    private final double maxBid;
    private final double increment;

    public RegisterAutoBidRequest(String auctionId, String bidderId, double maxBid, double increment) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxBid = maxBid;
        this.increment = increment;
    }
    @Override
    public Type getType(){ return Type.REGISTER_AUTO_BID; }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public double getIncrement() {
        return increment;
    }
}
