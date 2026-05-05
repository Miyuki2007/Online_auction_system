package client;
import protocol.Response;
import java.io.ObjectInputStream;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class ServerListener implements Runnable{
    private final ObjectInputStream in;
    private final BlockingQueue<Response> responseQueue;
    private final Consumer<Response> onNotification;

    private volatile boolean running = true;

    public ServerListener(ObjectInputStream in, BlockingQueue<Response> responseQueue, Consumer<Response> onNotification) {
        this.in = in;
        this.responseQueue = responseQueue;
        this.onNotification = onNotification;
    }

    public void stopListening()
    {
        running = false;
    }
    @Override
    public void run(){
        try{
            while(running){
                Object obj = in.readObject();
                if (obj instanceof Response){
                    Response response = (Response) obj;
                    if (response.getStatus() == Response.Status.NOTIFICATION){
                        if (onNotification!=null) {
                            onNotification.accept(response);
                        }
                    }
                    else {
                        responseQueue.put(response);
                    }
                }
            }
        } catch(Exception e){
            if(running){
                System.out.println("Mất kết nối server.");
            }
        }
    }
}
