package model.auction.exception;
// Trường hợp cố đặt giá nhưng phiên đấu giá đã kết thúc hoặc hủy
public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException(String auctionID){
        super("Phiên đấu giá " + auctionID + " không nhận giá thầu.");
    }
}
