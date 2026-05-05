package model.user;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class UserTest {
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
}
