package client;
import protocol.Response;
import protocol.Request;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AuctionClient {
    private static final String HOST = getConfig("server.host", "SERVER_HOST", "localhost");
    private static final int PORT = parsePort(getConfig("server.port", "SERVER_PORT", "12345"));

    private static final long REQUEST_TIMEOUT_SEC = 5;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();
    private Consumer<Response> onNotification;

    private ServerListener listener;
    private Thread listenerThread;
    private volatile boolean connected = false;

    public void setOnNotification(Consumer<Response> onNotification){
        this.onNotification = onNotification;
    }
    public boolean isConnected(){
        return connected && socket != null && !socket.isClosed();
    }
    public void connect() throws IOException{
        if (isConnected()){
            return;
        }
        socket = new Socket(HOST,PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        listener = new ServerListener(in, responseQueue, this::handleNotification);
        listenerThread = new Thread(listener,"AuctionClient - Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
        connected = true;
    }
    public Response sendRequest(Request request) throws IOException,InterruptedException{
        if (!isConnected()){
            throw new IOException("Chưa kết nối tới server");
        }
        synchronized (out)
        {
            out.writeObject(request);
            out.flush();
        }

        return responseQueue.poll(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
    }
    public void disconnect() {
        connected = false;
        try {
            if (listener != null) {
                listener.stopListening();
            }
            Thread.sleep(20);
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private void handleNotification(Response notification){
        if (onNotification != null){
            onNotification.accept(notification);
        } else {
            System.out.println("[Notification] " + notification.getMessage());
        }
    }

    private static String getConfig(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        return defaultValue;
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port ngoai khoang hop le: " + value);
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SERVER_PORT/server.port khong hop le: " + value, e);
        }
    }
}
