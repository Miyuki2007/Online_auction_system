package protocol.requests;

import protocol.Request;

public class PlaceBidRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String auctionId;
    private final String bidderId;
    private final double amount;

    public PlaceBidRequest(String auctionId, String bidderId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền bid phải > 0");
        }
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    @Override
    public Type getType() { return Type.PLACE_BID; }

    public String getAuctionId() { return auctionId; }
    public String getBidderId() { return bidderId; }
    public double getAmount() { return amount; }
}