package client;
import protocol.Response;
import protocol.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class AuctionClient {
    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();
    private Consumer<Response> onNotification;
    public void setOnNotification(Consumer<Response> onNotification){
        this.onNotification = onNotification;
    }
    public void connect() throws IOException{
        socket = new Socket(HOST,PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        new Thread (new ServerListener(in,responseQueue,this::handleNotification)).start();
    }
    public Response sendRequest(Request request) throws IOException,InterruptedException{
        out.writeObject(request);
        out.flush();
        return responseQueue.take();
    }
    public void disconnect(){
        try{
            if (socket!=null) socket.close();
        } catch (IOException ignored){}
    }
    private void handleNotification(Response notification){
        if (onNotification != null){
            onNotification.accept(notification);
        } else {
            System.out.println("[Notification] " + notification.getMessage());
        }
    }
}
