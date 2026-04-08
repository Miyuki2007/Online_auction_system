package model.auction.exception;
// TH: Chuyển trạng thái không đúng quy trình
public class InvalidStateTransitionException extends RuntimeException{
    public InvalidStateTransitionException(String from, String to ){
        super("Cannot transition from " + from + " to " + to);
    }
}
