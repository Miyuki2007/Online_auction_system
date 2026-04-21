package client;
import protocol.Response;
import protocol.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class AuctionClient {
    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public void connect() throws IOException{
        socket = new Socket(HOST,PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        new Thread (new ServerListener(in)).start();
    }
    public Response sendRequest(Request request) throws IOException,ClassNotFoundException{
        out.writeObject(request);
        out.flush();
        return (Response) in.readObject();
    }
    public void disconnect(){
        try{
            if (socket!=null) socket.close();
        } catch (IOException ignored){}
    }
}
