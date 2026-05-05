package model.auction.exception;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException (String msg){
        super(msg);
    }
}
