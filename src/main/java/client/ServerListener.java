package client;
import protocol.Response;
import java.io.ObjectInputStream;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
public class ServerListener implements Runnable{
    private final ObjectInputStream in;
    private final BlockingQueue<Response> responseQueue;
    private final Consumer<Response> onNotification;

    public ServerListener(ObjectInputStream in, BlockingQueue<Response> responseQueue, Consumer<Response> onNotification) {
        this.in = in;
        this.responseQueue = responseQueue;
        this.onNotification = onNotification;
    }

    @Override
    public void run(){
        try{
            while(true){
                Object obj = in.readObject();
                if (obj instanceof Response){
                    Response response = (Response) obj;
                    if (response.getStatus() == Response.Status.NOTIFICATION){
                        onNotification.accept(response);
                    }
                    else {
                        responseQueue.put(response);
                    }
                }
            }
        } catch(Exception e){
            System.out.println("Mất kết nối server.");
        }
    }
}
