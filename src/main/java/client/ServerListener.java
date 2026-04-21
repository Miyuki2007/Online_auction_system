package client;
import protocol.Response;
import java.io.ObjectInputStream;
public class ServerListener implements Runnable{
    private final ObjectInputStream in;
    public ServerListener(ObjectInputStream in){
        this.in=in;
    }
    @Override
    public void run(){
        try{
            while(true){
                Object obj = in.readObject();
                if (obj instanceof Response){
                    Response response = (Response) obj;
                    if (response.getStatus() == Response.Status.NOTIFICATION){
                        System.out.println("Notification: " + response.getMessage());
                    }
                }
            }
        } catch(Exception e){
            System.out.println("Mất kết nối server.");
        }
    }
}
