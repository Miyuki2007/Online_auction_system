package model.auction;
import model.auction.exception.AuctionClosedException;
import model.auction.exception.InvalidStateTransitionException;
import model.auction.exception.InvalidBidException;
import model.auction.observer.AuctionObserver;
import model.auction.observer.AuctionSubject;
import model.item.Item;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import model.auction;
/*
Thread-safe: Dùng ReentrantLock để tránh lost update
khi nhiều bidder đặt giá đồng thời
Observer: notify tất cả client khi có bid mới hoặc đổi trạng thái
 */
public class Auction implements AuctionSubject,Serializable {
    private static final long serialVersionUID = 1L;
    //  Thông tin cơ bản
    private final String id;
    private final String sellerId;
    private final Item item;
    private final double startingPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Anti-sniping: Chống đặt giá phút chót
    //ANTI_SNIPE_THRESHOLD_SEC: Đặt giá trong 30 giây cuối của phiên đấu giá
    //ANTI_SNIPE_EXTENSION_SEC: Phiên được gia hạn thêm 60 giây
    // Trong trường hợp bidder A đặt giá khi hệ thống còn 15s
    // --> Hệ thống gia hạn thêm giờ để người khác đặt giá
    private final boolean antiSnipeEnabled;
    private final long antiSnipeThresholdSec;
    private final long antiSnipeExtensionSec;

    // Trạng thái
    private AuctionState state;
    private double currentHighestBid;
    private String currentWinnerId;

    //Lịch sử Bid
    private final List<BidTransaction> bidHistory;

    //Concurrency: vấn đề xảy ra khi nhiều bidder đặt giá cùng một lúc
    private final ReentrantLock bidLock;

    //Observer
    private transient List<AuctionObserver> observers;

    public Auction(String sellerID, Item item,
                   double startingPrice, LocalDateTime startTime,
                   LocalDateTime endTime, boolean antiSnipeEnabled,
                   long antiSnipeThresholdSec, long antiSnipeExtensionSec) {
        this.id = UUID.randomUUID().toString();
        this.sellerId = sellerID;
        this.item = item;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.antiSnipeEnabled = antiSnipeEnabled;
        this.antiSnipeThresholdSec = antiSnipeThresholdSec;
        this.antiSnipeExtensionSec = antiSnipeExtensionSec;
        this.state = AuctionState.OPEN;
        this.currentHighestBid = startingPrice;
        this.currentWinnerId = null;
        this.bidHistory = Collections.synchronizedList(new ArrayList<>());
        this.bidLock = new ReentrantLock(true);
        this.observers = new CopyOnWriteArrayList<>();
    }
        // Đặt giá
        public BidTransaction placeBid(String bidderId, double amount){
            bidLock.lock();
            try{
                //1.Phiên phải đang Running
                if (state != AuctionState.RUNNING){
                    throw new AuctionClosedException(id);
                }
                //2.Kiểm tra hết giờ chưa
                if (LocalDateTime.now().isAfter(endTime)){
                    finishAuction();
                    throw new AuctionClosedException(id);
                }
                //3.Seller không được tự đấu giá
                if (bidderId.equals(sellerId)){
                    throw new InvalidBidException("Người bán không thể đấu giá trong chính phiên đấu giá của mình.");
                }
                //4. Giá phải cao hơn giá hiện tại
                if (amount <= currentHighestBid){
                    throw new InvalidBidException("Giá đấu giá %.2f phải cao hơn giá hiện tại %.2f", amount, currentHighestBid);
                }
                //5. Tạo record và cập nhật
                BidTransaction record = new BidTransaction(id,bidderId, amount);
                bidHistory.add(record);
                currentHighestBid = amount;
                currentWinnerId = bidderId;
                //6. Anti-sniping ch chạy nếu seller đã bật
                if (antiSnipeEnabled){
                    checkAntiSnipe();
                }
                notifyNewBid(record);
                return record;
            }finally{
                bidLock.unlock();
            }
        }
        //Chuyển trạng thái
    public void start(){
        transitionTo(AuctionState.RUNNING);
        this.startTime = LocalDateTime.now();
    }
    public void finishAuction(){
        transitionTo(AuctionState.FINISHED);
    }
    public void markPaid(){
        transitionTo(AuctionState.PAID);
    }
    public void cancel(){
        transitionTo(AuctionState.CANCELED);
    }
    private void transitionTo(AuctionState newState){
        if (!state.canTransition(newState)){
            throw new InvalidStateTransitionException(state.name(),newState.name());
        }
        this.state = newState;
        notifyStateChanged();
    }
    //Anti-sniping
    private void checkAntiSnipe(){
        long secsRemain = Duration.between(LocalDateTime.now(), endTime).getSeconds();
        if (secsRemain > 0 && secsRemain <= antiSnipeThresholdSec){
            endTime = endTime.plusSeconds(antiSnipeExtensionSec);
            for (AuctionObserver obs : getObservers()){
                obs.onAuctionTimeExtended(this,antiSnipeExtensionSec);
            }
        }
    }
    //Tự động đóng phiên
    public void checkExpiration(){
        bidLock.lock();
        try{
            if (state == AuctionState.RUNNING && LocalDateTime.now().isAfter(endTime)){
                finishAuction();
            }
        } finally {
            bidLock.unlock();
        }
    }
    //Observer
    private List<AuctionObserver> getObservers(){
        if (observers == null){
            observers = new CopyOnWriteArrayList<>();
        }
        return observers;
    }
    @Override
    public void addObserver(AuctionObserver observer){
        getObservers().add(observer);
    }
    @Override
    public void removeObserver(AuctionObserver observer){
        getObservers().remove(observer);
    }
    @Override
    public void notifyNewBid(BidTransaction bid){
        for (AuctionObserver obs : getObservers()){
            obs.onNewBid(this,bid);
        }
    }
    @Override
    public void notifyStateChanged(){
        for (AuctionObserver obs : getObservers()){
            obs.onAuctionStateChanged(this);
        }
    }
    //Query
    public Optional<BidTransaction> getWinningBid(){
        if (bidHistory.isEmpty()) return Optional.empty();
        return bidHistory.stream().max(Comparator.comparingDouble(BidTransaction::getAmount));
    }
    public List<BidTransaction> getBidHistory(){
        return Collections.unmodifiableList(bidHistory);
    }
    public int getBidCount(){ return bidHistory.size();}
    public boolean isActive(){
        return state == AuctionState.RUNNING && LocalDateTime.now().isBefore(endTime);
    }
    public long getRemainSeconds(){
        if (!isActive()) return 0;
        return Duration.between(LocalDateTime.now(),endTime).getSeconds();
    }
    //Getter
    public String getId() { return id; }
    public String getSellerId() { return sellerId;}
    public String getItemID() { return item.getId(); }
    public Item getItem() { return item; }
    public double getStartingPrice() { return startingPrice; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public boolean isAntiSnipeEnabled() { return antiSnipeEnabled; }
    public long getAntiSnipeThresholdSec() { return antiSnipeThresholdSec; }
    public long getAntiSnipeExtensionSec() { return antiSnipeExtensionSec; }
    public AuctionState getState() { return state; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public String getCurrentWinnerId() { return currentWinnerId; }
    @Override
    public String toString() {
        return String.format(
                "[Phiên đấu giá] %s | %s | %s | Giá cao nhất: %.2f | Số lượt đấu giá: %d | AntiSnipe: %s",
                id.substring(0, 8), item.getName(), state,
                currentHighestBid, bidHistory.size(),
                antiSnipeEnabled
                        ? antiSnipeThresholdSec + "s/" + antiSnipeExtensionSec + "s"
                        : "Tắt"
        );
    }
}
