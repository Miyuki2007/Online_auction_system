package model.manager;

import model.auction.Auction;
import model.auction.BidTransaction;
import model.item.Item;
import model.user.User;
import model.auction.exception.AuthenticationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
// dùng singleton để tạo manager
public class AuctionManager {
    private static volatile AuctionManager instance;

    private List<Auction> activeAuctions;
    private List<User> registeredUsers;

    private AuctionManager() {
        activeAuctions = Collections.synchronizedList(new ArrayList<>());
        registeredUsers = Collections.synchronizedList(new ArrayList<>());
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }
    public void registerUser(User user){
        if (getUserByUsername(user.getUsername())!= null){
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        registeredUsers.add(user);
    }
    public User getUserByUsername(String username){
        for (User user : registeredUsers){
            if (user.getUsername().equals(username)){
                return user;
            }
        }
        return null;
    }
    public User authenticateUser(String username,String password){
        User user = getUserByUsername(username);
        if (user==null){
            throw new AuthenticationException("Tên đăng nhập không tồn tại.");

        }
        if (!user.authenticate(password)){
            throw new AuthenticationException("Mật khẩu không đúng");
        }
        return user;
    }

    public void startAuction(Auction auction) {
        activeAuctions.add(auction);
        System.out.println("Auction" + auction.getItemID() + "started");
    }
    public Auction findAuctionById(String id){
        for (Auction a : activeAuctions){
            if (a.getId().equals(id))
            {
                return a;
            }
        }
        return null;
    }
    public BidTransaction placeBid(String auctionId, String bidderId, double amount){
        Auction a = findAuctionById(auctionId);
        if (a==null){
            throw new IllegalArgumentException("Phiên đấu giá không tồn tại.");
        }
        return a.placeBid(bidderId,amount);
    }
    public Auction createAuction(String sellerId, Item item, double startingPrice, LocalDateTime start, LocalDateTime end, boolean antiSnipe, long threshold, long extension){
        Auction a = new Auction(sellerId,item,startingPrice,start,end,antiSnipe,threshold,extension);
        activeAuctions.add(a);
        a.start();
        return a;
    }


    public List<Auction> getActiveAuctions() {
        return Collections.unmodifiableList(activeAuctions);
    }
    public List<User> getRegisteredUsers(){
        return Collections.unmodifiableList(registeredUsers);
    }
}
