package server;

import client.AuctionClient;
import protocol.Request;
import protocol.Response;

import java.util.concurrent.CountDownLatch;

public class MultiClientTest {
    public static void main(String[] args) throws Exception {
        System.out.println("---Test 2 client kết nối đồng thời---\n");
        CountDownLatch latch = new CountDownLatch(2);

        //Client 1
        Thread c1 = new Thread(() -> {
            try {
                runClient("Client1", "123456", "client1@gmail.com", "NguyenVanA", "BIDDER");
            } catch (Exception e) {
                System.err.println("Client 1 lỗi: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        Thread c2 = new Thread(() -> {
            try {
                runClient("Client2", "654321", "client2@gmail.com", "TranThiB", "SELLER");
            } catch (Exception e) {
                System.err.println("Client 2 lỗi: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
        c1.start();
        c2.start();
        latch.await();
        System.out.println("\n---Test hoàn tất---");
    }
    private static void runClient(String username, String password, String email,String fullName,String role) throws Exception{
        AuctionClient client = new AuctionClient();
        client.connect();
        System.out.println("[ " + username + " ]: Kết nối thành công");

        Request registerReq = new Request(Request.Type.REGISTER,username,password,email,fullName,role);
        Response registerRes = client.sendRequest(registerReq);
        System.out.println("[ " + username + " ] đăng kí: " + registerRes.getStatus() + " - " + registerRes.getMessage());

        Request loginReq = new Request(Request.Type.LOGIN,username,password);
        Response loginRes = client.sendRequest(loginReq);
        System.out.println("[ " + username + " ] đăng nhập: " + loginRes.getStatus() + " - " + loginRes.getMessage());

        Request listReq = new Request(Request.Type.GET_AUCTIONS);
        Response listRes = client.sendRequest(listReq);
        System.out.println("[ " + username + " ] GET_AUCTION: " + listRes.getStatus() + " - " + listRes.getMessage());

        client.disconnect();
        System.out.println("[ " + username + " ] Ngắt kết nối\n");
    }
}