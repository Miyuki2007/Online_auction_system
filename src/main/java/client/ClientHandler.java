package client;
import model.auction.Auction;
import model.manager.AuctionManager;
import model.user.*;
import model.item.Item;
import model.factory.ItemFactory;
import model.auction.BidTransaction;
import protocol.Response;
import protocol.Request;
import server.AuctionServer;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
public class ClientHandler implements Runnable{
    private final Socket socket;
    private final AuctionServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String watchAuctionId;
    public ClientHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
    }
    @Override
    public void run(){
        try{
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            while(true){
                Request  request = (Request) in.readObject();
                Response response = handleRequest(request);
                sendResponse(response);
            }
        } catch (EOFException e){
            System.out.println("Client ngắt kết nối");
        } catch (Exception e){
            System.err.println("Lỗi xử lí Client: "+ e.getMessage());
        } finally{
            server.removeClient(this);
            try{ socket.close();} catch(IOException ignored){}
        }
    }
    private Response handleRequest(Request request){
        AuctionManager manager = AuctionManager.getInstance();
        try{
            switch(request.getType()){
                case LOGIN: {
                    User user = manager.authenticateUser(request.getParam(0), request.getParam(1));
                    return Response.ok("Đăng nhâp thành công",user);

                }
                case REGISTER:{
                    User user;
                    String role = request.getParam(4).toUpperCase();
                    switch(role){
                        case "BIDDER":
                            user = new Bidder(request.getParam(0),request.getParam(1), request.getParam(2),request.getParam(3));
                            break;
                        case "SELLER":
                            user = new Bidder(request.getParam(0),request.getParam(1), request.getParam(2),request.getParam(3));
                            break;
                        default:
                            return Response.error("Vai trò không hợp lệ.");
                    }
                    manager.registerUser(user);
                    return Response.ok("Đăng kí thành công", user);
                }
                case GET_AUCTIONS:{
                    List<Auction> auctions = manager.getActiveAuctions();
                    return Response.ok("DANH SÁCH PHIÊN ĐẤU GIÁ", auctions);
                }
                case GET_AUCTION_DETAIL:{
                    Auction auction = manager.findAuctionById(request.getParam(0));
                    if (auction == null ) return Response.error("Không tìm thấy phiên đấu giá. ");
                    this.watchAuctionId = auction.getId();
                    return Response.ok("Chi tiết phiên",auction);
                }
                case PLACE_BID:{
                    BidTransaction bid = manager.placeBid(
                            request.getParam(0),
                            request.getParam(1),
                            Double.parseDouble(request.getParam(2)));
                    server.broadcastToAuction(request.getParam(0),
                            Response.notification("Có bid mới", bid));
                    return Response.ok("Đặt giá thành công!",bid);
                }
                case CREATE_ITEM:{
                    Item item = ItemFactory.createItem(
                            request.getParam(0),request.getParam(1),
                            request.getParam(2),request.getParam(3),
                            Double.parseDouble(request.getParam(4)),
                            request.getParam(5)
                    );
                    return Response.ok("Tạo sản phẩm thành công!", item);
                }
                case CREATE_AUCTION:{
                    Item item = ItemFactory.createItem(
                            request.getParam(1),java.util.UUID.randomUUID().toString(),
                            request.getParam(2),request.getParam(3),
                            Double.parseDouble(request.getParam(4)),
                            request.getParam(5)
                    );
                    Auction auction = manager.createAuction(
                            request.getParam(0),item,
                            Double.parseDouble(request.getParam(6)),
                            LocalDateTime.now(),
                            LocalDateTime.now().plusMinutes(Long.parseLong(request.getParam(7))), false,0,0);
                    return Response.ok("Tạo phiên đấu giá thành công.", auction);
                }
                default:
                    return Response.error("Yêu cầu không hợp lệ.");
            }
        }catch (Exception e){
            return Response.error(e.getMessage());
        }
    }
    public void sendResponse(Response response){
        try{
            out.writeObject(response);
            out.flush();
            out.reset();
        } catch (IOException e){
            System.err.println("Lỗi gửi response: "+ e.getMessage());
        }
    }
    public String getWatchAuctionId(){ return  watchAuctionId;}
}
