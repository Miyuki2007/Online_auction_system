package protocol.requests;

import protocol.Request;

public class GetMyAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String sellerId;

    public GetMyAuctionRequest(String sellerId) {
        this.sellerId = sellerId;
    }

    @Override
    public Type getType() { return Type.GET_MY_AUCTIONS; }

    public String getSellerId() { return sellerId; }
}