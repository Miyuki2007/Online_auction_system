package server;

import model.auction.Auction;
import model.auction.BidTransaction;
import model.auction.observer.AuctionObserver;
import protocol.Response;

public class ServerObserver implements AuctionObserver{
    private final AuctionServer server;

    public ServerObserver(AuctionServer server) {
        this.server = server;
    }
    @Override
    public void onNewBid(Auction auction, BidTransaction bid){
        server.broadcastToAuction(
                auction.getId(),Response.notification("BID_UPDATE",bid)
        );
    }
    @Override
    public void onAuctionStateChanged(Auction auction){
        server.broadcastToAuction(
                auction.getId(),Response.notification("STATE_CHANGED",auction.getState())
        );
    }

    @Override
    public void onAuctionTimeExtended(Auction auction, long extensionSeconds){
        server.broadcastToAuction(
                auction.getId(),Response.notification("TIME_EXTENDED",extensionSeconds)
        );
    }
}
