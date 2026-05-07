package protocol.requests;

import protocol.Request;

public class CancelAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final String sellerId;   // để verify quyền cancel

    public CancelAuctionRequest(String auctionId, String sellerId) {
        this.auctionId = auctionId;
        this.sellerId = sellerId;
    }

    @Override
    public Type getType() { return Type.CANCEL_AUCTION; }

    public String getAuctionId() { return auctionId; }
    public String getSellerId() { return sellerId; }
}