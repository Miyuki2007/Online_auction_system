package model.auction.observer;

import model.auction.BidTransaction;
public interface AuctionSubject{
    void addObserver (AuctionObserver observer);
    void removeObserver(AuctionObserver observer);
    void notifyNewBid(BidTransaction bid);
    void notifyStateChanged();
}
