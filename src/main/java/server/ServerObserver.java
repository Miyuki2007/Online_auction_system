package server;

import model.auction.Auction;
import model.auction.BidTransaction;
import model.auction.observer.AuctionObserver;
import protocol.responses.NotificationResponse;
import protocol.responses.NotificationResponse.NotificationType;

public class ServerObserver implements AuctionObserver{
    private final AuctionServer server;

    public ServerObserver(AuctionServer server) {
        this.server = server;
    }

    @Override
    public void onNewBid(Auction auction, BidTransaction bid){
        server.broadcastToAuction(
                auction.getId(),
                new NotificationResponse(NotificationType.BID_UPDATE, "Có bid mới", bid)
        );
    }

    @Override
    public void onAuctionStateChanged(Auction auction){
        server.broadcastToAuction(
                auction.getId(),
                new NotificationResponse(NotificationType.STATE_CHANGED, "Trạng thái thay đổi", auction.getState())
        );
    }

    @Override
    public void onAuctionTimeExtended(Auction auction, long extensionSeconds){
    }
}