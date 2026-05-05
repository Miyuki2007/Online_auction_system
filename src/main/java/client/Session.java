package client;

import model.auction.Auction;
import model.user.Admin;
import model.user.Bidder;
import model.user.Seller;
import model.user.User;

/**
 * Session quản lý trạng thái phiên làm việc phía Client.
 * Áp dụng Design Pattern: Singleton & Thread-safe.
 */
public class Session {
    private static Session instance;

    private User loggedInUser;
    private AuctionClient auctionClient;
    private Auction selectedAuction;

    private Session() {
    }

    public static synchronized Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    // ===== USER =====

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

    // ===== AUCTION CLIENT =====


    public AuctionClient getClient() {
        if (auctionClient == null) {
            auctionClient = new AuctionClient();
        }
        return auctionClient;
    }

    public AuctionClient getAuctionClient() {
        return getClient();
    }

    public void setAuctionClient(AuctionClient client) {
        this.auctionClient = client;
    }

    // ===== SELECTED AUCTION (truyền giữa các scene) =====

    public Auction getSelectedAuction() {
        return selectedAuction;
    }

    public void setSelectedAuction(Auction auction) {
        this.selectedAuction = auction;
    }

    // ===== CLEAR (đăng xuất / thoát app) =====

    public void clear() {
        loggedInUser = null;
        selectedAuction = null;

        if (auctionClient != null) {
            auctionClient.disconnect();
            auctionClient = null;
        }
    }
}