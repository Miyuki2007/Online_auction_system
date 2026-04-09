package model.auction.exception;
// TH: Chuyển trạng thái không đúng quy trình
public class InvalidStateTransitionException extends RuntimeException{
    public InvalidStateTransitionException(String from, String to ){
        super("Không thể chuyển đổi từ " + from + " sang " + to);
    }
}
