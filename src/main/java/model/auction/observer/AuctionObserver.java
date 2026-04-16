package model.auction.observer;
import model.auction.Auction;
import model.auction.Auction.BidRecord;
public class AuctionObserver {
    public interface AuctionObserver{
        void onNewBid(Auction auction, BidRecord bid);
        void onAuctionStateChanged(Auction auction);
        void onAuctionTimeExtended(Auction auction, long extensionSeconds);
    }
}
