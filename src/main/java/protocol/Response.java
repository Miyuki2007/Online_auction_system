package protocol;
import java.io.Serializable;
public class Response implements  Serializable{
    private static final long serialVersionUID = 1L;
    public enum Status {OK, ERROR, NOTIFICATION}
    private final Status status;
    private final String message;
    private final Object data;
    public Response(Status status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
    public static Response ok(String msg, Object data){
        return new Response(Status.OK,msg,data);
    }
    public static Response error(String msg){
        return new Response(Status.ERROR,msg,null);
    }
    public static Response notification(String msg, Object data){
        return new Response(Status.NOTIFICATION,msg,data);
    }
    public Status getStatus() {
        return status;
    }
    public String getMessage() {
        return message;
    }
    public Object getData() {
        return data;
    }
}