package server;
import model.manager.AuctionManager;
import protocol.Response;
import protocol.responses.NotificationResponse;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

public class AuctionServer {
    private static final int PORT = 12345;
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final ExecutorService threadPool = Executors.newFixedThreadPool(200);
    private final ExecutorService broadcastPool = new ThreadPoolExecutor(
            2,
            10,
            60L, TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(500),
            new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy()
    );
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
    public void broadcastToAuction(String auctionId, Response notification) {
        for (ClientHandler client : connectedClients) {
            if (auctionId.equals(client.getWatchAuctionId())) {
                try {
                    broadcastPool.submit(() -> client.sendResponse(notification));
                } catch (java.util.concurrent.RejectedExecutionException ignored) {
                    // pool đầy — bỏ qua, client sẽ nhận update lần sau
                }
            }
        }
    }
    public void removeClient(ClientHandler client){
        connectedClients.remove(client);
        System.out.println(" Client ngắt kết nối. Còn lại: " + connectedClients.size());
    }
    public void shutdown(){
        running = false;
        scheduler.shutdownNow();
        broadcastPool.shutdown();
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
