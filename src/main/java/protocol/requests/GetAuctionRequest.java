package protocol.requests;

import protocol.Request;

public class GetAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    @Override
    public Type getType() { return Type.GET_AUCTIONS; }
}