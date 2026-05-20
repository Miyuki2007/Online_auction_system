package model.manager;
import model.auction.Auction;
import model.auction.AutoBid;
import model.auction.BidTransaction;
import model.auction.observer.AuctionObserver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class AutoBidManager implements AuctionObserver {
    private static volatile AutoBidManager instance;
    // Tất cả autobid lưu theo từng phiên
    private final Map<String, List<AutoBid>> autoBidsByAuction = new ConcurrentHashMap<>();
    // Track auction đã đăng kí observer
    private final Set<String> observedAuctions = ConcurrentHashMap.newKeySet();
    //Tránh trigger lặp vô hạn khi autobid kích hoạt autobid khác
    private final ThreadLocal<Boolean> isProcessing = ThreadLocal.withInitial(() -> false);

    private AutoBidManager() {
    }

    public static AutoBidManager getInstance() {
        if (instance == null) {
            synchronized (AutoBidManager.class) {
                if (instance == null) instance = new AutoBidManager();
            }
        }
        return instance;
    }
    public AutoBid register(Auction auction, String bidderId, double maxBid, double increment){
        if (maxBid <= auction.getCurrentHighestBid()){
            throw new IllegalArgumentException(String.format("Giá cao nhất (%.2f) phải lớn hơn giá hiện tại (%.2f)", maxBid,auction.getCurrentHighestBid()));
        }
        //Seller không được autobid phiên của mình
        if (auction.getSellerId().equals(bidderId)){
            throw new IllegalArgumentException("Người bán không thể auto-bid phiên của mình");
        }
        List<AutoBid> existing = autoBidsByAuction.get(auction.getId());
        if (existing != null){
            synchronized (existing){
                boolean hasActive = existing.stream().anyMatch(ab -> ab.getBidderId().equals(bidderId)&& ab.isActive());
                if (hasActive){
                    throw new IllegalArgumentException("Bạn đã có auto-bid đang hoạt động phiên này. Hãy hủy trước khi đăng kí mới");
                }
            }
        }
        AutoBid autoBid = new AutoBid(auction.getId(),bidderId,maxBid,increment);
        autoBidsByAuction.computeIfAbsent(auction.getId(), k-> Collections.synchronizedList(new ArrayList<>())).add(autoBid);
        //Chỉ đăng kí observer 1 lần cho mỗi auction
        if (observedAuctions.add(auction.getId())){
            auction.addObserver(this);
        }
        triggerAutoBid(auction,bidderId);
        return autoBid;

    }
    @Override
    public void onNewBid(Auction auction, BidTransaction bid){
        if (isProcessing.get()) return;
        try{
            isProcessing.set(true);
            triggerAutoBid(auction,bid.getBidderId());
        } finally{
            isProcessing.set(false);
        }
    }
    @Override
    public void onAuctionStateChanged (Auction auction){
        if (auction.isEnded()){
            List<AutoBid> bids = autoBidsByAuction.get(auction.getId());
            if (bids!=null){
                synchronized (bids){
                    bids.forEach(AutoBid::deactivate);
                }
            }
            observedAuctions.remove(auction.getId());
        }
    }
    @Override
    public void onAuctionTimeExtended (Auction auction, long extensionSeconds){}
    private void triggerAutoBid(Auction auction, String excludeBidderId){
        List<AutoBid> bids = autoBidsByAuction.get(auction.getId());
        if (bids == null || bids.isEmpty()) return;
        double currentPrice = auction.getCurrentHighestBid();
        PriorityQueue<AutoBid> queue = new PriorityQueue<>();
        synchronized (bids){
            for (AutoBid ab : bids){
                if (!ab.isActive()) continue;
                if (ab.getBidderId().equals(excludeBidderId)) continue;
                if (ab.getMaxBid()<= currentPrice){
                    ab.deactivate();
                    continue;
                }
                queue.offer(ab);
            }
        }
        if (queue.isEmpty()) return;
        AutoBid winner = queue.poll();
        AutoBid runnerUp = queue.peek();
        double bidAmount;
        if (runnerUp == null){
            bidAmount = Math.min(winner.getMaxBid(),currentPrice + winner.getIncrement());
        } else{
            bidAmount = Math.min(winner.getMaxBid(),
                    runnerUp.getMaxBid() + winner.getIncrement());
        }
        if (bidAmount <= currentPrice){
            winner.deactivate();
            return;
        }
        try{
            auction.placeBid(winner.getBidderId(),bidAmount);
        } catch (Exception e){
            System.err.println("Auto-bid thất bại: " + e.getMessage());
            winner.deactivate();
        }
    }
    public List<AutoBid> getAutoBidsForBidder (String bidderId){
        List<AutoBid> result = new ArrayList<>();
        autoBidsByAuction.values().forEach(list -> {
            synchronized (list){
                list.stream().filter(ab -> ab.getBidderId().equals(bidderId)&& ab.isActive()).forEach(result::add);
            }
        });
        return result;
    }
    public boolean cancel (String autoBidId, String bidderId){
        for (List<AutoBid> list : autoBidsByAuction.values()){
            synchronized (list){
                for (AutoBid ab:list){
                    if (ab.getId().equals(autoBidId) && ab.getBidderId().equals(bidderId))
                    {
                        ab.deactivate();
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

