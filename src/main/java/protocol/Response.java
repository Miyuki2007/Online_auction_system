package protocol;

import java.io.Serializable;

public abstract class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { OK, ERROR, NOTIFICATION }

    private final Status status;
    private final String message;

    protected Response(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }

    public boolean isOk() {
        return status == Status.OK;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isNotification() {
        return status == Status.NOTIFICATION;
    }
}