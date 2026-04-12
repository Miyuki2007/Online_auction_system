package model.auction.observer;
import model.auction.Auction;
import model.BTransaction.BidTransaction;
public interface AuctionObserver {
        void onNewBid(Auction auction, BidTransaction bid);
        void onAuctionStateChanged(Auction auction);
        void onAuctionTimeExtended(Auction auction, long extensionSeconds);
}
