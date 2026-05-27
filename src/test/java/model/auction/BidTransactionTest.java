package model.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
class BidTransactionTest {
    private final String AUCTION_ID = "auction-001";
    private final String BIDDER_ID = "bidder-001";
    @Test
    @DisplayName("Constructor gán đúng các giá trị và sinh ID/timestamp")
    void create_validParams_setsAllFields(){
        LocalDateTime before = LocalDateTime.now();
        BidTransaction bt = new BidTransaction(AUCTION_ID,BIDDER_ID,1500.0);
        LocalDateTime after = LocalDateTime.now();
        assertNotNull(bt.getId());
        assertFalse(bt.getId().isBlank());
        assertEquals(AUCTION_ID,bt.getAuctionId());
        assertEquals(BIDDER_ID,bt.getBidderId());
        assertEquals(1500.0,bt.getAmount());
        assertNotNull(bt.getTimestamp());
        //timestamp nằm trong khoảng [before,after]
        assertFalse(bt.getTimestamp().isBefore(before));
        assertFalse(bt.getTimestamp().isAfter(after));
    }

    @Test
    @DisplayName("Mỗi BidTransaction có ID duy nhất")
    void create_idIsUnique(){
        BidTransaction a = new BidTransaction(AUCTION_ID,BIDDER_ID,1000.0);
        BidTransaction b = new BidTransaction(AUCTION_ID,BIDDER_ID,1000.0);
        assertNotEquals(a.getId(),b.getId());
    }

    @Test
    @DisplayName("compareTo: giá cao hơn xếp trước (giảm dần)")
    void compareTo_higherAmountFirst(){
        BidTransaction low = new BidTransaction(AUCTION_ID,"bidder-01",1000.0);
        BidTransaction high = new BidTransaction(AUCTION_ID,"bidder-02",2000.0);
        assertTrue(high.compareTo(low)<0);
        assertTrue(low.compareTo(high)>0);
    }

    @Test
    @DisplayName("compareTo: giá bằng nhau - bid đặt sớm hơn xếp trước")
    void compareTo_sameAmount() throws InterruptedException{
        BidTransaction first = new BidTransaction(AUCTION_ID,"bidder-01",1500.0);
        Thread.sleep(5);
        BidTransaction second = new BidTransaction(AUCTION_ID,"bidder-02",1500.0);
        assertTrue(first.compareTo(second) < 0);
    }

    @Test
    @DisplayName("Collections.sort xếp danh sách bid theo giá giảm dần")
    void sort_returnsByAmountDescending(){
        List<BidTransaction> bids = new ArrayList<>();
        bids.add(new BidTransaction(AUCTION_ID,"bidder-01",1500.0));
        bids.add(new BidTransaction(AUCTION_ID,"bidder-02",3000.0));
        bids.add(new BidTransaction(AUCTION_ID,"bidder-03",2000.0));
        Collections.sort(bids);
        assertEquals(3000.0,bids.get(0).getAmount());
        assertEquals(2000.0,bids.get(1).getAmount());
        assertEquals(1500.0,bids.get(2).getAmount());
    }
}
