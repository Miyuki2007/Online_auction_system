package model.user;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
class UserTest {
    @Test
    @DisplayName("Bidder tạo thành công đúng với vai trò")
    void bidder_creation(){
        Bidder bidder = new Bidder("MinhHong","25021779","minhhong25021779@gmail.com","DaoMinhHong");
        assertEquals("BIDDER",bidder.getRole());
        assertEquals("MinhHong",bidder.getUsername());
        assertEquals(0.0,bidder.getBalance());


    }

    @Test
    @DisplayName("Seller taạo thành công với đúng vai trò")
    void seller_creation(){
        Seller seller = new Seller("HoNgoc","25021916","hongoc25021916@gmail.com","VuHoangHoNgoc");
        assertEquals("SELLER",seller.getRole());
    }

    @Test
    @DisplayName("Admin tạo thành công với đúng vai trò")
    void admin_creation(){
        Admin admin = new Admin("TheCong","25021658","thecong25021658@gmail.com","PhamTheCong");
        assertEquals("ADMIN",admin.getRole());
    }

    @Test
    @DisplayName("Mật khẩu đúng")
    void authenticate_correctPassword(){
        Bidder bidder = new Bidder("MinhHong","25021779","minhhong25021779@gmail.com","DaoMinhHong");
        assertTrue(bidder.authenticate("25021779"));
    }

    @Test
    @DisplayName("Mật khẩu sai")
    void authenticate_wrongPassword(){
        Bidder bidder = new Bidder("MinhHong","25021779","minhhong25021779@gmail.com","DaoMinhHong");
        assertFalse(bidder.authenticate("wrong"));
    }

    @Test
    @DisplayName("Nạp tiền thành công vào tài khoản Bidder")
    void bidder_deposit_valid(){
        Bidder bidder = new Bidder("MinhHong","25021779","minhhong25021779@gmail.com","DaoMinhHong");
        bidder.deposit(500.0);
        assertEquals(500.0,bidder.getBalance());
    }

    @Test
    @DisplayName("Bidder nạp số tiền âm - Hệ thống báo lỗi")
    void bidder_deposit_throwsException(){
        Bidder bidder = new Bidder("MinhHong","25021779","minhhong25021779@gmail.com","DaoMinhHong");
        assertThrows(IllegalArgumentException.class,()-> bidder.deposit(-100.0));
    }

    @Nested
    @DisplayName("Bidder - bid history & balance")
    class BidderTests{
        @Test
        @DisplayName("addBidToHistory lưu bid ID vào lịch sử")
        void addBidToHistory(){
            Bidder b = new Bidder("u","p","1@gmail.com","name");
            b.addBidToHistory("bidder-01");
            b.addBidToHistory("bidder-02");
            List <String> ls = b.getBidHistory();
            assertThrows(UnsupportedOperationException.class,()->ls.add("hacker"));
        }

        @Test
        @DisplayName("deposit nhiều lần - cộng dồn balance")
        void deposit(){
            Bidder b = new Bidder("u","p","1@gmail.com","name");
            b.deposit(100.0);
            b.deposit(250.0);
            b.deposit(50.0);
            assertEquals(400.0,b.getBalance());
        }

        @Test
        @DisplayName("Deposit số 0 - throw IllegalArgumentException")
        void deposit_Zero(){
            Bidder b = new Bidder("u","p","1@gmail.com","name");
            assertThrows(IllegalArgumentException.class,()->b.deposit(0.0));
        }
    }
    @Nested
    @DisplayName("Seller - revenue & listed items")
    class SellerTests{
        @Test
        @DisplayName("Khởi tạo Seller - revenue mặc định bằng 0")
        void create(){
            Seller s = new Seller("u","p","1@gmail.com","name");
            assertEquals(0.0,s.getRevenue());
            assertTrue(s.getListItemIds().isEmpty());
        }

        @Test
        @DisplayName("addRevenue cộng dồn doanh thu")
        void addRevenue(){
            Seller s = new Seller("u","p","1@gmail.com","name");
            s.addRevenue(1000.0);
            s.addRevenue(2500.0);
            assertEquals(3500.0,s.getRevenue());
        }

        @Test
        @DisplayName("addListedItem lưu item ID vào danh sách")
        void addListedItem(){
            Seller s = new Seller("u","p","1@gmail.com","name");
            s.addListedItem("item-01");
            s.addListedItem("item-02");
            List<String> items = s.getListItemIds();
            assertEquals(2,items.size());
            assertTrue(items.contains("item-01"));
            assertTrue(items.contains("item-02"));
        }

        @Test
        @DisplayName("getListItemIds trả về list bất biến")
        void getListItemIds(){
            Seller s = new Seller("u","p","1@gmail.com","name");
            s.addListedItem("item-01");
            List<String> items = s.getListItemIds();
            assertThrows(UnsupportedOperationException.class,()->items.add("hacker"));
        }
    }

    @Nested
    @DisplayName("User - setter & getDisplayInfo")
    class UserTests{
        @Test
        @DisplayName("setPassword + authenticate với password mới")
        void setPasswordAndAuthenticate(){
            Bidder b = new Bidder("u","123","1@gmail.com","name");
            b.setPassword("123");
            assertTrue(b.authenticate("123"));
            assertFalse(b.authenticate("456"));

        }

        @Test
        @DisplayName("setEmail + setFullName cập nhật thông tin")
        void setInfo(){
            Bidder b = new Bidder("u","p","1@gmail.com","name");
            b.setEmail("2@gmail.com");
            b.setFullName("name1");
            assertEquals("2@gmail.com",b.getEmail());
            assertEquals("name1",b.getFullName());
        }

        @Test
        @DisplayName("getDisplayInfo chứa role, fullname, username")
        void displayInfo(){
            Bidder b = new Bidder("user","123","1@gmail.com","user123");
            String info = b.getDisplayInfo();
            assertTrue(info.contains("BIDDER"));
            assertTrue(info.contains("user123"));
            assertTrue(info.contains("user"));
        }
    }

    @Nested
    @DisplayName("Entity - equals & hashcode by Id")
    class EntityTests{
        @Test
        @DisplayName("Mỗi User có ID duy nhất khi tạo mới")
        void newUser(){
            Bidder b1 = new Bidder("u","p","1@gmail.com","name");
            Bidder b2 = new Bidder("u","p","1@gmail.com","name");
            assertNotEquals(b1.getId(),b2.getId());
            assertNotEquals(b1,b2);
        }
        @Test
        @DisplayName("equals(null) trả false, equals (object khác nhau) trả false")
        void equals_null(){
            Bidder b = new Bidder("u","p","1@gmail.com","name");
            assertNotEquals(null,b);
            assertNotEquals("a string", b);
        }
        @Test
        @DisplayName("equals tự nó")
        void equals_reflexive(){
            Bidder b = new Bidder("u","p","1@gmail.com","name");
            assertEquals(b,b);
            assertEquals(b.hashCode(),b.hashCode());
        }

    }
}
