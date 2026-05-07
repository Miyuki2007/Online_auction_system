package protocol.responses;

import protocol.Response;


public class ErrorResponse extends Response {
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public ErrorResponse(String message) {
        super(Status.ERROR, message);
        this.errorCode = null;
    }

    public ErrorResponse(String errorCode, String message) {
        super(Status.ERROR, message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}