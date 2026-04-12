package model.auction.exception;
//TH Giá đặt không hợp lệ (Thấp hơn hoặt bằng giá hiện tại)
public class InvalidBidException extends RuntimeException {
    public InvalidBidException (String msg, Object... args){

        super(String.format(msg, args));
    }
}
