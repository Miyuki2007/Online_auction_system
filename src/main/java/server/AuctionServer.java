package server;
import model.manager.AuctionManager;
import protocol.Response;
import protocol.responses.NotificationResponse;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionServer {
    private static final int PORT = 12345;
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean running = false;
    public void start(){
        AuctionManager manager = AuctionManager.getInstance();
        running = true;
        scheduler.scheduleAtFixedRate(() -> {
            try {
                manager.checkAllExpirations();
            } catch (Exception e) {
                System.err.println("Lỗi scheduler checkAllExpirations:");
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);
        System.out.println("✅ Đã khởi động scheduler kiểm tra auction hết hạn (mỗi 5s).");
        try(ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server đang chạy trên port: " + PORT);
            System.out.println("Đang chờ client kết nối...");
            while (running){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client đang kết nối: " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket,this);
                connectedClients.add(handler);
                threadPool.submit(handler);
                System.out.println("Tổng số client đang kết nối thành công: " + connectedClients.size());
            }
        } catch (IOException e){
            System.err.println("Lỗi server: " + e.getMessage());
        } finally{
            shutdown();
        }
    }
    public void broadcastToAll(Response notification){
        for(ClientHandler client: connectedClients){
            client.sendResponse(notification);
        }
    }
    public void broadcastToAuction(String auctionId, protocol.Response notification){
        int count = 0;
        for (ClientHandler client: connectedClients){
            if (auctionId.equals(client.getWatchAuctionId())){
                client.sendResponse(notification);
                count++;
            }
        }
        System.out.println(" Broadcast tới " + count + " client đang xem auction " + auctionId);
    }
    public void removeClient(ClientHandler client){
        connectedClients.remove(client);
        System.out.println(" Client ngắt kết nối. Còn lại: " + connectedClients.size());
    }
    public void shutdown(){
        running = false;
        scheduler.shutdownNow();
        threadPool.shutdown();
        try{
            if (!threadPool.awaitTermination(5,TimeUnit.SECONDS)){
                threadPool.shutdownNow();
            }
        }catch(InterruptedException e){
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Server đã dừng.");

    }
    public static void main(String[] args){
        AuctionServer server = new AuctionServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.start();
    }
}
