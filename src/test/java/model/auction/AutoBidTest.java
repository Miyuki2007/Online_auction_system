package model.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;
class AutoBidTest {
    private final String AUCTION_ID = "auction-001";
    private final String BIDDER_ID = "bidder-001";

    //---Constructor + Validation---
    @Test
    @DisplayName("Tạo AutoBid hợp lệ - Khởi tạo đủ các thuộc tính")
    void create_validParams_success() {
        AutoBid ab = new AutoBid(AUCTION_ID, BIDDER_ID, 5000.0, 100.0);
        assertNotNull(ab.getId());
        assertEquals(AUCTION_ID, ab.getAuctionId());
        assertEquals(BIDDER_ID, ab.getBidderId());
        assertEquals(5000.0, ab.getMaxBid());
        assertEquals(100.0, ab.getIncrement());
        assertNotNull(ab.getCreateAt());
        assertTrue(ab.isActive(), "AutoBid mới phải active");
    }

    @Test
    @DisplayName("ID của AutoBid là duy nhất giữa các instance")
    void create_isIsUnique() {
        AutoBid a = new AutoBid(AUCTION_ID, BIDDER_ID, 5000.0, 100.0);
        AutoBid b = new AutoBid(AUCTION_ID, BIDDER_ID, 5000.0, 100.0);
        assertNotEquals(a.getId(),b.getId());
    }

    @Test
    @DisplayName("MaxBid <= 0 - throw IllegalArgumentException")
    void create_invalidMaxBid_throws(){
        assertThrows(IllegalArgumentException.class,() -> new AutoBid(AUCTION_ID,BIDDER_ID,0.0,100.0));
        assertThrows(IllegalArgumentException.class,() -> new AutoBid(AUCTION_ID,BIDDER_ID,-500.0,100.0));
    }

    @Test
    @DisplayName("Bước nhảy <= 0 - throw IllegalArgumentException")
    void create_invalidIncrement_throws(){
        assertThrows(IllegalArgumentException.class,() -> new AutoBid(AUCTION_ID,BIDDER_ID,5000.0,0.0));
        assertThrows(IllegalArgumentException.class,() -> new AutoBid(AUCTION_ID,BIDDER_ID,5000.0,-50.0));
    }

    //---compareTo - PriorityQueue
    @Test
    @DisplayName("compareTo: maxBid cao hơn xếp trước (priority cao hơn)")
    void compareTo_higherMaxBidFirst(){
        AutoBid low = new AutoBid(AUCTION_ID,"bidder-01",3000.0,100.0);
        AutoBid high = new AutoBid(AUCTION_ID,"bidder-02",5000.0,100.0);
        assertTrue(high.compareTo(low)<0);
        assertTrue(low.compareTo(high)>0);
    }

    @Test
    @DisplayName("compareTo: maxBid bằng nhau - người đăng kí trước xếp trước (uu tiên thời gian)")
    void compareTo_sameMaxBid() throws InterruptedException{
        AutoBid first = new AutoBid(AUCTION_ID,"bidder-01",5000.0,100.0);
        Thread.sleep(5);
        AutoBid second = new AutoBid(AUCTION_ID,"bidder-02",5000.0,100.0);
        assertTrue(first.compareTo(second)<0,"AutoBid đăng kí trước phải có priority cao hơn khi maxBid bằng nhau");
    }

    @Test
    @DisplayName("PriorityQueue dùng AutoBid trả về theo đúng thứ tự ưu tiên")
    void priorityQueue_pollsHighestMaxBidFirst(){
        PriorityQueue<AutoBid> pq = new PriorityQueue<>();
        pq.offer(new AutoBid(AUCTION_ID,"bidder-01",3000.0,100.0));
        pq.offer(new AutoBid(AUCTION_ID,"bidder-02",7000.0,100.0));
        pq.offer(new AutoBid(AUCTION_ID,"bidder-03",5000.0,100.0));
        assertEquals(7000.0,pq.poll().getMaxBid());
        assertEquals(5000.0,pq.poll().getMaxBid());
        assertEquals(3000.0,pq.poll().getMaxBid());
    }

    //---DEACTIVATE---
    @Test
    @DisplayName("deactivate() chuyển active từ true sang false")
    void deactivate_changesState(){
        AutoBid ab = new AutoBid(AUCTION_ID,BIDDER_ID,5000.0,100.0);
        assertTrue(ab.isActive());
        ab.deactivate();
        assertFalse(ab.isActive());
    }

    @Test
    @DisplayName("deactivate() nhiều lần vẫn ổn định")
    void deactivate_multipleCalls(){
        AutoBid ab = new AutoBid(AUCTION_ID,BIDDER_ID,5000.0,100.0);
        ab.deactivate();
        ab.deactivate();
        ab.deactivate();
        assertFalse(ab.isActive());
    }


}
