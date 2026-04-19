package model.auction.observer;
import model.auction.Auction;
import model.auction.BidTransaction;

public interface AuctionObserver{
    void onNewBid(Auction auction, BidTransaction bid);
    void onAuctionStateChanged(Auction auction);
    void onAuctionTimeExtended(Auction auction, long extensionSeconds);
}

