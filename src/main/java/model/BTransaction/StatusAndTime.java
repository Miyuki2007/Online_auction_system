package model.BTransaction;

enum AuctionStatus {
    OPEN, RUNNING, FINISHED, PAID, CANCELED
}

interface AuctionObserver {
    void onNewBid(AuctionProduct product, BidTransaction bid);
    void onTimeExtended(AuctionProduct product, long extraSeconds);
    void onStatusChanged(AuctionProduct product);
}