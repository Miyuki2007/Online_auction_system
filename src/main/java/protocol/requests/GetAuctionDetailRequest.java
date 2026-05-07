package protocol.requests;

import protocol.Request;

public class GetAuctionDetailRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;

    public GetAuctionDetailRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    @Override
    public Type getType() { return Type.GET_AUCTION_DETAIL; }

    public String getAuctionId() { return auctionId; }
}