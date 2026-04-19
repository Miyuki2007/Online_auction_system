package model.manager;

import model.auction.Auction;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
// dùng singleton để tạo manager
public class AuctionManager {
    private static volatile AuctionManager instance;

    private List<Auction> activeAuctions;

    private AuctionManager() {
        activeAuctions = Collections.synchronizedList(new ArrayList<>());
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

    public void startAuction(Auction auction) {
        activeAuctions.add(auction);
        System.out.println("Auction" + auction.getItemID() + "started");
    }

    public List<Auction> getActiveAuctions() {
        return activeAuctions;
    }
}
