package model.manager;

import model.auction.Auction;
import model.auction.BidTransaction;
import model.auction.exception.AuthenticationException;
import model.item.Electronics;
import model.item.Item;
import model.user.Bidder;
import model.user.Seller;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionManagerTest {
    private AuctionManager manager;
    private Seller seller;
    private Bidder bidder1;
    private Bidder bidder2;
    private Item testItem;

    @BeforeEach
    void setUp() throws Exception{
        resetSingleton();
        manager = AuctionManager.getInstance();
        seller = new Seller("Seller01","123","seller@gmail.com","NguoiBan");
        bidder1 = new Bidder("Bidder01","abc","bidder1@gmail.com","NguyenVanA");
        bidder2 = new Bidder("Bidder02","def","bidder2@gmail.com","NguyenVanB");
        testItem = new Electronics("item01","Laptop","Gaming laptop",1000.0,"Lenovo");
    }
    private void resetSingleton() throws Exception{
        Field instance = AuctionManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null,null);
    }
    //====TEST SINGLETON====

    @Test
    @Order(1)
    @DisplayName("getInstance() luôn trả về cùng một instance")
    void singleton_returnsSameInstance(){
        AuctionManager m1 = AuctionManager.getInstance();
        AuctionManager m2 = AuctionManager.getInstance();
        assertSame(m1,m2,"Singleton phải trả về cùng một instance");
    }

    @Test
    @Order(2)
    @DisplayName("Singleton thread-safe: 100 thread cùng gọi getInstance()")
    void singleton_threadSafe() throws InterruptedException{
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AuctionManager[] instances = new AuctionManager[threadCount];
        for (int i=0;i<threadCount;i++){
            final int idx = i;
            executor.submit(() -> {
                instances[idx] = AuctionManager.getInstance();
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();

        for (int i=1;i<threadCount;i++){
            assertSame(instances[0], instances[i],"Thread " + i + " trả về cùng ínstance với thread đầu");
        }
    }
    //==== TEST QUẢN LÝ USER ====
    @Test
    @Order(3)
    @DisplayName("Đăng kí user mới thành công")
    void registerUser_success(){
        manager.registerUser(bidder1);
        assertEquals(1,manager.getRegisteredUsers().size());
        assertNotNull(manager.getUserByUsername("Bidder01"));
    }

    @Test
    @Order(4)
    @DisplayName("Đăng kí nhiều user khác nhau thành công")
    void registerUser_multipleUsers(){
        manager.registerUser(bidder1);
        manager.registerUser(bidder2);
        manager.registerUser(seller);
        assertEquals(3,manager.getRegisteredUsers().size());
    }

    @Test
    @Order(5)
    @DisplayName("Đăng kí username trùng - throw IllegalArgumentException")
    void registerUser_duplicateUsername_throws(){
        manager.registerUser(bidder1);
        Bidder duplicate = new Bidder("Bidder01","789","bidderx@gmail.com","TranThiA");
        assertThrows(IllegalArgumentException.class,()-> manager.registerUser(duplicate), "Lỗi: username trùng nhau");
    }

    @Test
    @Order(6)
    @DisplayName("getUserByUsername - username không tồn tại trả về null")
    void getUserByUsername_notFound(){
        assertNull(manager.getUserByUsername("Không tồn tại"));
    }

    @Test
    @Order(7)
    @DisplayName("GetUserByUsername - username đã tồn tại")
    void getUserByUsername_found(){
        manager.registerUser(bidder1);
        var found = manager.getUserByUsername("Bidder01");
        assertNotNull(found);
        assertEquals("Bidder01",found.getUsername());
        assertEquals("BIDDER",found.getRole());
    }
    //==== TEST AUTHENTICATION ====
    @Test
    @Order(8)
    @DisplayName("Authenticate đúng username + password")
    void authenticate_success(){
        manager.registerUser(bidder1);
        var user = manager.authenticateUser("Bidder01","abc");
        assertNotNull(user);
        assertEquals("Bidder01",user.getUsername());
    }

    @Test
    @Order(9)
    @DisplayName("Authenticate - username không tồn tại - throw AuthenticationException")
    void authenticate_userNotExist(){
        AuthenticationException authenExcep = assertThrows(AuthenticationException.class,() -> manager.authenticateUser("NonExist","any"));
        assertTrue(authenExcep.getMessage().contains("Tên đăng nhập"));
    }

    @Test
    @Order(10)
    @DisplayName("Authenticate - sai password - throw AuthenticationException")
    void authenticate_wrongPassword(){
        manager.registerUser(bidder1);
        AuthenticationException authenExcep = assertThrows(AuthenticationException.class,() -> manager.authenticateUser("Bidder01","wrong"));
        assertTrue(authenExcep.getMessage().contains("Mật khẩu"));
    }

    //==== TEST AUCTION ====
    @Test
    @Order(11)
    @DisplayName("Tạo auction thành công")
    void createAuction_success(){
        Auction auction = manager.createAuction(
                seller.getId(),testItem,1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                false,0,0
        );
        assertNotNull(auction);
        assertEquals(1,manager.getActiveAuctions().size());
    }

    @Test
    @Order(12)
    @DisplayName("Tìm auction theo ID - tồn tại")
    void findAuctionById_found(){
        Auction auction = manager.createAuction(
                seller.getId(),testItem,1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                false,0,0
        );
        Auction found = manager.findAuctionById(auction.getId());
        assertNotNull(found);
        assertEquals(auction.getId(),found.getId());
    }

    @Test
    @Order(13)
    @DisplayName("Tìm auction theo ID - không tồn tại")
    void findAuctionById_notFound(){
        assertNull(manager.findAuctionById("fakeId"));
    }

    //==== TEST PLACE BID QUA MANAGER ====
    @Test
    @Order(14)
    @DisplayName("Đặt giá qua manager - thành công")
    void placeBid_success(){
        Auction auction = manager.createAuction(
                seller.getId(),testItem,1000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                false,0,0
        );
        BidTransaction bid = manager.placeBid(auction.getId(),bidder1.getId(),1500.0);
        assertNotNull(bid);
        assertEquals(1500.0,bid.getAmount());
        assertEquals(1500.0,auction.getCurrentHighestBid());
    }

    @Test
    @Order(15)
    @DisplayName("Đặt giá vào auction không tồn tại - throw IllegalArgumentException")
    void placeBid_auctionNotExist(){
        assertThrows(IllegalArgumentException.class,()->manager.placeBid("fakeId",bidder1.getId(),1000.0));
    }

    //==== TEST CONCURRENT ====
    @Test
    @Order(16)
    @DisplayName("Đăng kí nhiều user đồng thời")
    void registerUser_concurrent() throws InterruptedException{
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i=0;i<threadCount;i++){
            final int idx = i;
            executor.submit(()->{
                try{
                    Bidder b = new Bidder("User" + idx, "pass",idx+"@gmail.com","UserFullName" + idx);
                    manager.registerUser(b);
                    successCount.incrementAndGet();
                }catch (Exception ignored){
                }finally{
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();
        assertEquals(threadCount,successCount.get(),"Tất cả user đăng kí thành công");
        assertEquals(threadCount,manager.getRegisteredUsers().size(),"Số user trong manager phải khớp");
    }

    //==== TEST GET ACTIVE AUCTIONS ====
    @Test
    @Order(18)
    @DisplayName("getActiveAuctions trả về list không thể modify")
    void getActiveAuctions_immutable(){
        manager.createAuction(seller.getId(),testItem,1000.0,LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),false,0,0);
        List<Auction> auctions = manager.getActiveAuctions();
        assertThrows(UnsupportedOperationException.class,()->auctions.add(null),"List trả về phải là unmodifiable");
    }
}
