package model.auction;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
public class AutoBid implements  Comparable<AutoBid>, Serializable{
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String auctionId;
    private final String bidderId;
    private final double maxBid;
    private final double increment;
    private final LocalDateTime createAt;
    private boolean active;

    public AutoBid(String auctionId, String bidderId, double maxBid, double increment) {
        if (maxBid<=0) throw new IllegalArgumentException("Giá tối đa phải lớn hơn 0");
        if (increment<=0) throw new IllegalArgumentException("Bước nhảy phải lớn hơn 0");
        this.id = UUID.randomUUID().toString();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.createAt = LocalDateTime.now();
        this.active = true;
    }
    @Override
    public int compareTo(AutoBid other){
        int cmp = Double.compare(other.maxBid,this.maxBid);
        return cmp != 0 ? cmp : this.createAt.compareTo(other.createAt);
    }
    public void deactivate (){
        this.active = false;
    }

    public String getId() {
        return id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public double getIncrement() {
        return increment;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public boolean isActive() {
        return active;
    }
}
