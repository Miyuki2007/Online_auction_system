package model.auction.observer;
import model.BTransaction.BidTransaction;
public interface AuctionSubject{
    void addObserver (AuctionObserver observer);
    void removeObserver(AuctionObserver observer);
    void notifyNewBid(BidTransaction bid);
    void notifyStateChanged();
}