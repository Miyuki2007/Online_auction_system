package model.auction.observer;
import model.auction.Auction.BidRecord;
public interface AuctionSubject {
    void addObserver(AuctionObserver observer);
    void removeObserver(AuctionObserver observer);
    void notifyNewBid(BidRecord bid);
    void notifyStateChanged();
}
