package client;

import model.user.User;
import model.user.Admin;
import model.user.Bidder;
import model.user.Seller;
import model.auction.Auction;

/**
 * Session quản lý trạng thái đăng nhập phía Client.
 * Áp dụng Design Pattern: Singleton & Thread-safe.
 */
public class Session {
    private static Session instance;
    private User loggedInUser;
    private AuctionClient client;
    private Auction selectedAuction;

    private Session() {}

    public static synchronized Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
    }

    public boolean isAdmin() {
        return loggedInUser instanceof Admin;
    }

    public boolean isBidder() {
        return loggedInUser instanceof Bidder;
    }

    public boolean isSeller() {
        return loggedInUser instanceof Seller;
    }
    public AuctionClient getClient(){
        if (client==null){
            client = new AuctionClient();
        }
        return client;
    }
    public Auction getSelectedAuction() {
        return selectedAuction;
    }

    public void setSelectedAuction(Auction auction) {
        this.selectedAuction = auction;
    }

    public void clear() {
        loggedInUser = null;
        selectedAuction = null;
        if (client!=null){
            client.disconnect();
            client = null;
        }
    }
}