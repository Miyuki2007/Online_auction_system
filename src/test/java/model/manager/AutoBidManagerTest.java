package model.manager;

import model.auction.Auction;
import model.auction.AutoBid;
import model.item.Electronics;
import model.item.Item;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AutoBidManagerTest {
    private AutoBidManager autoBidManager;
    private Auction auction;
    private Item item;
    private final String SELLER_ID = "seller-01";

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();
        autoBidManager = AutoBidManager.getInstance();
        item = new Electronics("item-001", "Laptop", "Mô tả", 1000.0, "Lenovo");
        auction = new Auction(
                SELLER_ID, item, 1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                false, 0, 0);
        auction.start();
    }

    private void resetSingleton() throws Exception {
        Field instance = AutoBidManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    //---Singleton---
    @Test
    @DisplayName("getInstance() trả về cùng một instance(Singleton)")
    void singleton_sameInstance() {
        AutoBidManager m1 = AutoBidManager.getInstance();
        AutoBidManager m2 = AutoBidManager.getInstance();
        assertSame(m1, m2);
    }

    //---Register---
    @Test
    @DisplayName("Đăng kí auto-bid hợp lệ - thành công và active")
    void register_success() {
        AutoBid ab = autoBidManager.register(auction, "bidder-01", 5000.0, 100.0);
        assertNotNull(ab);
        assertTrue(ab.isActive());
        assertEquals("bidder-01", ab.getBidderId());
        assertEquals(5000.0, ab.getMaxBid());
    }

    @Test
    @DisplayName("Đăng kí auto-bid với maxBid <= giá hiện tại - throw IllegalArgumentException")
    void register_maxBidTooLow() {
        assertThrows(IllegalArgumentException.class, () -> autoBidManager.register(auction, "bidder-01", 1000.0, 100.0));
        assertThrows(IllegalArgumentException.class, () -> autoBidManager.register(auction, "bidder-01", 500.0, 100.0));
    }

    @Test
    @DisplayName("Seller không được tự auto-bid phiên của mình")
    void register_sellerSelfBid(){
        assertThrows(IllegalArgumentException.class, () -> autoBidManager.register(auction, SELLER_ID, 5000.0, 100.0));
    }

    @Test
    @DisplayName("Cùng bidder đăng kí 2 auto-bid active cùng phiên - throw IllegalArgumentException")
    void register_sameBidderTwice(){
        autoBidManager.register(auction,"bidder-01",5000.0,100.0);
        assertThrows(IllegalArgumentException.class,()-> autoBidManager.register(auction,"bidder-01",6000.0,100.0));
    }

    @Test
    @DisplayName("Sau khi cancel, bidder cũ đăng kí lại được")
    void register_afterCancel(){
        AutoBid first = autoBidManager.register(auction,"bidder-01",5000.0,100.0);
        boolean ok = autoBidManager.cancel(first.getId(),"bidder-01");
        assertTrue(ok);
        AutoBid second = autoBidManager.register(auction,"bidder-01",6000.0,100.0);
        assertNotNull(second);
        assertTrue(second.isActive());
    }

    //---Trigger logic---
    @Test
    @DisplayName("Auto-bid kích hoạt khi có bid mới từ người khaác và tăng giá phiên")
    void autoBid_triggerOnNewBid(){
        autoBidManager.register(auction,"bidder-01",5000.0,100.0);
        //Bidder đặt khác giá --> auto-bid của bidder-01 phải tự kích hoạt
        auction.placeBid("bidder-02",1500.0);
        //-> Giá hiện tại: >1500
        assertTrue(auction.getCurrentHighestBid() > 1500.0,"Auto-bid phải kích hoạt và đẩy giá cao hơn bidder-02");
        assertEquals("bidder-01",auction.getCurrentWinnerId(),"Bidder có auto-bid maxBid cao hơn phải là người dẫn đầu");
    }

    @Test
    @DisplayName("2 autoBid - người có maxBid cao hơn thắng sau khi bid ngoài mồi liên tục")
    void autoBid_higherMaxBidWins(){
        autoBidManager.register(auction, "bidder-01", 3000.0, 100.0);
        autoBidManager.register(auction, "bidder-02", 5000.0, 100.0);
        // Giải thích: sau register lần 2, hệ thống tự đẩy giá lên 1200 (winner=bidder-02).
        // Để 2 auto-bid "đấu" thực sự, cần bid ngoài mồi liên tục vượt mức hiện tại.
        // Ta dùng bidder-03 mồi nhiều lần để buộc cuộc đua phải đến khi A bỏ cuộc.
        for (double amount = 1300.0; amount <= 3100.0; amount += 200.0) {
            try {
                auction.placeBid("bidder-03", amount);
            } catch (Exception ignored) {}
        }
        assertEquals("bidder-02", auction.getCurrentWinnerId(),
                "Bidder có maxBid cao hơn phải là người dẫn đầu cuối cùng");
        assertTrue(auction.getCurrentHighestBid() > 3000.0,
                "Giá hiện tại phải vượt qua maxBid của bidder-01");
        assertTrue(auction.getCurrentHighestBid() <= 5000.0,
                "Auto-bid không được vượt maxBid của winner");
    }

    @Test
    @DisplayName("AutoBid không vượt quá maxBid")
    void autoBid_neverExceedsMaxBid(){
        autoBidManager.register(auction,"bidder-01",2500.0,100.0);
        auction.placeBid("bidder-02",2600.0);
        assertTrue(auction.getCurrentHighestBid()>=2600.0,"Giá hiện tại phải lớn hơn hoặc bằng bid ngoài");
        List<AutoBid> remaining = autoBidManager.getAutoBidsForBidder("bidder-01");
        assertTrue(remaining.isEmpty(),"AutoBid của bidder-01 phải deactivate sau khi giá vượt maxBid");
    }

    // --- getAutoBidsForBidder
    @Test
    @DisplayName("getAutoBidsForBidder chỉ trả về auto-bid active của bidder đó")
    void getAutoBidsForBidder_onlyActive(){
        autoBidManager.register(auction,"bidder-01",5000.0,100.0);
        //Phiên thứ 2 ở cùng bidder
        Auction auction2 = new Auction(
                SELLER_ID, new Electronics("item-002","Phone","Mô tả",500.0,"Sony"), 500.0,
                LocalDateTime.now(),LocalDateTime.now().plusHours(1), false,0,0
        );
        auction2.start();
        autoBidManager.register(auction2,"bidder-01",2000.0,50.0);
        List<AutoBid> list = autoBidManager.getAutoBidsForBidder("bidder-01");
        assertEquals(2,list.size());
        assertTrue(list.stream().allMatch(AutoBid::isActive));
    }

    @Test
    @DisplayName("getAutoBidsForBidder không trả về autoBid của bidder khác")
    void getAutoBidsForBidder_PerUser(){
        autoBidManager.register(auction,"bidder-01",5000.0,100.0);
        autoBidManager.register(auction,"bidder-02",6000.0,100.0);
        List <AutoBid> list1 = autoBidManager.getAutoBidsForBidder("bidder-01");
        assertEquals(1,list1.size());
        assertEquals("bidder-01",list1.get(0).getBidderId());
    }

    //---Cancel---
    @Test
    @DisplayName("Cancel autoBid bằng đúng owner thành công")
    void cancel_byOwner(){
        AutoBid ab = autoBidManager.register(auction,"bidder-01",5000.0,100.0);
        boolean ok = autoBidManager.cancel(ab.getId(),"bidder-01");
        assertTrue(ok);
        assertFalse(ab.isActive());
    }

    @Test
    @DisplayName("Cancel autoBid bằng người khác thất bại")
    void cancel_byOtherBidder(){
        AutoBid ab = autoBidManager.register(auction,"bidder-01",5000.0,100.0);
        boolean ok = autoBidManager.cancel(ab.getId(),"bidder-02");
        assertFalse(ok);
        assertTrue(ab.isActive(),"AutoBid không được bị deactivate khi cancel sai owner");
    }

    @Test
    @DisplayName("Cancel autoBid không tồn tại - thất bại nhưng không throw")
    void cancel_nonExistent(){
        boolean ok = autoBidManager.cancel("non-existent-id","anyone");
        assertFalse(ok);
    }

}
