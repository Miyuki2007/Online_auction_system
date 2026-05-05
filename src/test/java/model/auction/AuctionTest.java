package model.auction;

import model.auction.exception.AuctionClosedException;
import model.auction.exception.InvalidBidException;
import model.auction.exception.InvalidStateTransitionException;
import model.auction.observer.AuctionObserver;
import model.item.Electronics;
import model.item.Item;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
class AuctionTest {
    private Auction auction;
    private Item testItem;
    private final String Seller_ID = "seller-01";

    @BeforeEach
    void setUp(){
        testItem = new Electronics("item-001","LaptopLegion","LaptopGamming",1000.0,"Lenovo");
        auction = new Auction(
                Seller_ID,testItem,1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                false,0,0
        );
        auction.start();
    }
    //==== Test đặt giá hợp lệ ====
    @Test
    @DisplayName("Đặt giá hợp lệ - cao hơn giá hiện tại")
    void placeBid_validAmount_success(){
        BidTransaction bid = auction.placeBid("bidder-001",1500.0);
        assertNotNull(bid);
        assertEquals(1500.0,auction.getCurrentHighestBid());
        assertEquals("bidder-001",auction.getCurrentWinnerId());
        assertEquals(1,auction.getBidCount());
    }
    @Test
    @DisplayName("Đặt giá nhiều lần - giá tăng dần")
    void placeBid_multipleBids_priceIncreases(){
        auction.placeBid("bidder-001",1500.0);
        auction.placeBid("bidder-002",2000.0);
        auction.placeBid("bidder-003",2500.0);
        assertEquals(2500.0,auction.getCurrentHighestBid());
        assertEquals("bidder-003",auction.getCurrentWinnerId());
        assertEquals(3,auction.getBidCount());
    }
    //==== Test đặt giá không hợp lệ ====
    @Test
    @DisplayName("Đặt giá thấp hơn giá hiện tại - throw InvalidBidException")
    void placeBid_lowAmount_throwsException(){
        auction.placeBid("bidder-001",1500.0);
        assertThrows(InvalidBidException.class,()->{auction.placeBid("bidder-002",1200.0);});
    }
    @Test
    @DisplayName("Đặt giá bằng giá hiện tại - throw InvalidBidException")
    void placeBid_equalAmount_throwsException(){
        assertThrows(InvalidBidException.class,()->{auction.placeBid("bidder-001",1000.0);});
    }
    @Test
    @DisplayName("Seller tự đấu giá - throw InvalidBidException")
    void placeBid_sellerBids_throwsException(){
        assertThrows(InvalidBidException.class,()->{auction.placeBid(Seller_ID,2000.0);});
    }
    @Test
    @DisplayName("Đặt giá khi phiên chưa RUNNING - throw AuctionClosedException")
    void placeBid_auctionNotRunning_throwsException(){
        //Tạo auction mới, không gọi start() -> trạng thái open
        Auction openAuction = new Auction(
                Seller_ID,testItem,1000.0,
                LocalDateTime.now(),LocalDateTime.now().plusHours(1),
                false,0,0
        );
        assertThrows(AuctionClosedException.class,()-> {
            openAuction.placeBid("bidder-001",1500.0);
        });
    }
    @Test
    @DisplayName("Đặt giá khi phiên đã FINISHED - throw AuctionClosedException")
    void placeBid_auctionFinished_throwsException(){
        auction.finishAuction();
        assertThrows(AuctionClosedException.class,()->{
            auction.placeBid("bidder-001",1500.0);
        });
    }
    //==== Test chuyển trạng thái ====
    @Test
    @DisplayName("Chuyển trạng thái hợp lệ: OPEN -> RUNNING -> FINISHED -> PAID")
    void stateTransition_validFlow(){
        Auction a = new Auction(
                Seller_ID,testItem,1000.0,
                LocalDateTime.now(),LocalDateTime.now().plusHours(1),
                false,0,0
        );
        assertEquals(AuctionState.OPEN,a.getState());
        a.start();
        assertEquals(AuctionState.RUNNING,a.getState());
        a.finishAuction();
        assertEquals(AuctionState.FINISHED,a.getState());
        a.markPaid();
        assertEquals(AuctionState.PAID,a.getState());
    }
    @Test
    @DisplayName("Chuyển trạng thái không hợp lệ: OPEN -> FINISHED - throw Exception")
    void stateTransition_invalidFlow_throwsException(){
        Auction a = new Auction(
                Seller_ID,testItem,1000.0,
                LocalDateTime.now(),LocalDateTime.now().plusHours(1),
                false,0,0
        );
        //OPEN không thể sang FINISHED
        assertThrows(InvalidStateTransitionException.class,()->{a.finishAuction();});
    }
    @Test
    @DisplayName("Cancel từ bất kì trạng thái nào (trừ PAID/CANCELED")
    void cancel_fromAnyState(){
        Auction a1 = new Auction(Seller_ID,testItem,1000.0,
                LocalDateTime.now(),LocalDateTime.now().plusHours(1),false,0,0);
        a1.cancel();
        assertEquals(AuctionState.CANCELED,a1.getState());
        Auction a2 = new Auction(Seller_ID,testItem,1000.0,
                LocalDateTime.now(),LocalDateTime.now().plusHours(1),false,0,0);
        a2.start();
        a2.cancel();
        assertEquals(AuctionState.CANCELED,a2.getState());
    }
    @Test
    @DisplayName("Không thể chuyển trạng thái từ CANCELED")
    void stateTransition_fromCanceled_throwsException(){
        auction.cancel();
        assertThrows(InvalidStateTransitionException.class,()->auction.start());
    }
    //==== Test Observer ====
    @Test
    @DisplayName("Observer nhận thông báo khi có bid mới")
    void observer_notifiedOnNewBid(){
        AtomicInteger bidCount = new AtomicInteger(0);
        auction.addObserver(new AuctionObserver() {
            @Override
            public void onNewBid(Auction auction, BidTransaction bid) {
                bidCount.incrementAndGet();
            }
            @Override
            public void onAuctionStateChanged(Auction auction) {}
            @Override
            public void onAuctionTimeExtended(Auction auction, long extensionSeconds) {
            }
        });
        auction.placeBid("bidder-001",1500.0);
        auction.placeBid("bidder-002",2000.0);
        assertEquals(2,bidCount.get());
    }
    @Test
    @DisplayName("Observer nhận thông báo khi đổi trạng thái")
    void observer_notifiedOnStateChange() {
        AtomicInteger stateChangeCount = new AtomicInteger(0);
        auction.addObserver(new AuctionObserver() {
            @Override
            public void onNewBid(Auction auction, BidTransaction bid) {}
            @Override
            public void onAuctionStateChanged(Auction auction) {
                stateChangeCount.incrementAndGet();
            }
            @Override
            public void onAuctionTimeExtended(Auction auction, long extensionSeconds) {}
        });
        auction.finishAuction();
        assertEquals(1,stateChangeCount.get());
    }
    //==== Test concurrent bidding ====
    @Test
    @DisplayName("Nhiều bidder đặt giá đồng thời - không mất bid, không race condition")
    void concurrentBidding_noLostUpdate() throws InterruptedException{
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        for (int i=0;i<threadCount;i++){
            final double bidAmount = 1000.0 + (i+1) * 100;
            final String bidderId = "bidder-"+i;
            executor.submit(() -> {
                try{
                    auction.placeBid(bidderId,bidAmount);
                    successCount.incrementAndGet();
                } catch (Exception e){
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();
        //Giá cao nhất phải là bid lớn nhất thành công
        assertTrue(auction.getCurrentHighestBid() >= 1100.0);
        // Tổng success + fail = tổng thread
        assertEquals(threadCount,successCount.get() + failCount.get());
        //Không có 2 nời cùng thắng
        assertNotNull(auction.getCurrentWinnerId());
    }
    //==== Test anti - sniping ====
    @Test
    @DisplayName("Anti-sniping gia hạn thời gian khi bid trong giây cuối")
    void antiSnipe_extendsTime(){
        //Tạo auction kêết thúc trong 20 giây, anti-snipe 30/60s
        Auction snipeAuction = new Auction(Seller_ID,testItem,1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusSeconds(20),
                true,30,60);
        snipeAuction.start();
        LocalDateTime endBefore = snipeAuction.getEndTime();
        snipeAuction.placeBid("bidder-001",1500.0);
        LocalDateTime endAfter = snipeAuction.getEndTime();
        //Thời gian kết thúc phải kéo dài thêm 60s
        assertTrue(endAfter.isAfter(endBefore));
    }
    //==== Test winning bid ====
    @Test
    @DisplayName("getWinningBid trả về Bid cao nhất")
    void getWinningBid_returnsHighest(){
        auction.placeBid("bidder-001",1500.0);
        auction.placeBid("bidder-002",2000.0);

        assertThrows(InvalidBidException.class, ()-> auction.placeBid("bidder-003",1800.0));
        var winning = auction.getWinningBid();
        assertEquals(2000.0,winning.get().getAmount());
        assertEquals("bidder-002",winning.get().getBidderId());
    }
    @Test
    @DisplayName("getWinning trả về empty khi không có bid")
    void getWinningBid_noBids_returnsEmpty(){
        assertTrue(auction.getWinningBid().isEmpty());
    }





}