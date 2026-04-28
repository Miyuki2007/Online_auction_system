package server;
import client.ClientHandler;
import model.manager.AuctionManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class AuctionServer {
    private static final int PORT = 12345;
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    public void start(){
        AuctionManager.getInstance();
        try(ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server đang chạy trên port: " + PORT);
            while (true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client đang kết nối: " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket,this);
                connectedClients.add(handler);
                threadPool.execute(handler);
            }
        } catch (IOException e){
            System.err.println("Lỗi server: " + e.getMessage());
        }
    }
    public void broadcastToAll(protocol.Response notification){
        for(ClientHandler client: connectedClients){
            client.sendResponse(notification);
        }
    }
    public void broadcastToAuction(String auctionId, protocol.Response notification){
        for (ClientHandler client: connectedClients){
            if (auctionId.equals(client.getWatchAuctionId())){
                client.sendResponse(notification);
            }
        }
    }
    public void removeClient(ClientHandler client){
        connectedClients.remove(client);
    }
    public static void main(String[] args){
        new AuctionServer().start();
    }
}
