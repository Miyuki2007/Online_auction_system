package model.auction.AuctionService;

import model.auction.Auction;
import model.auction.AuctionState;
import model.auction.BidTransaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {
    private final Map<String, Auction> auctionDatabase = new ConcurrentHashMap<>();

    public void addAuction(Auction auction) {
        auctionDatabase.put(auction.getId(), auction);
        System.out.println("Đã thêm sản phẩm: " + auction.getId());
    }

    public BidTransaction participateAuction(String auctionId, String userId, double bidAmount) {
        Auction auction = auctionDatabase.get(auctionId);

        if (auction == null) {
            throw new IllegalArgumentException("Phiên đấu giá không tồn tại. ");
        }
        return auction.placeBid(userId,bidAmount);
    }

    public void checkAndEndAuctions() {
        for (Auction auction : auctionDatabase.values()) {
            // auction sẽ tự động kiểm tra giờ và tự đổi status nếu đã hết hạn
            auction.checkExpiration();

            if (auction.getState() == AuctionState.FINISHED) {
                if (auction.getCurrentWinnerId() != null) {
                    System.out.println("Phiên " + auction.getId() + " kết thúc. Người thắng: "
                            + auction.getCurrentWinnerId() + " với giá " + auction.getCurrentHighestBid());
                } else {
                    System.out.println("Phiên " + auction.getId() + " kết thúc. Không có người tham gia trả giá.");
                }
            }
        }
    }
}