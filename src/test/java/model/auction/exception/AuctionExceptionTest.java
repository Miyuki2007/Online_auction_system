package model.auction.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class AuctionExceptionTest {
    @Nested
    @DisplayName("AuctionClosedException")
    class AuctionClosedTests{
        @Test
        @DisplayName("Message chứa auction ID đã truyền vào")
        void message_containsAuctionID(){
            AuctionClosedException ace = new AuctionClosedException("auction-123");
            assertTrue(ace.getMessage().contains("auction-123"));
        }

        @Test
        @DisplayName("RuntimeException")
        void RunTimeException(){
            assertTrue(RuntimeException.class.isAssignableFrom(AuctionClosedException.class));
        }
    }

    @Nested
    @DisplayName("InvalidBidException")
    class InvalidBidTests{
        @Test
        @DisplayName("Hỗ trợ format string với args")
        void messageWithArgs(){
            InvalidBidException ibe = new InvalidBidException("Giá %.2f phải cao hơn %.2f", 1000.0, 1500.0);
            assertTrue(ibe.getMessage().contains("1000"));
            assertTrue(ibe.getMessage().contains("1500"));
        }

        @Test
        @DisplayName("Message không format khi không có args")
        void messageWithoutArgs(){
            InvalidBidException ibe = new InvalidBidException("Lỗi đặt giá");
            assertEquals("Lỗi đặt giá", ibe.getMessage());
        }

        @Test
        @DisplayName("RuntimeException")
        void RunTimeException(){
            assertTrue(RuntimeException.class.isAssignableFrom(InvalidBidException.class));
        }
    }

    @Nested
    @DisplayName("InvalidStateTransitionException")
    class InvalidStateTransitionTests{
        @Test
        @DisplayName("Message chứa cả from và to state")
        void message_containsFromAndTo(){
            InvalidStateTransitionException ex = new InvalidStateTransitionException("OPEN","FINISHED");
            assertTrue(ex.getMessage().contains("OPEN"));
            assertTrue(ex.getMessage().contains("FINISHED"));
        }

        @Test
        @DisplayName("RuntimeException")
        void RunTimeException(){
            assertTrue(RuntimeException.class.isAssignableFrom(InvalidStateTransitionException.class));
        }
    }

    @Nested
    @DisplayName("AuthenticationException")
    class AuthenticationTests{
        @Test
        @DisplayName("Giữ nguyên message")
        void message_unchanged(){
            AuthenticationException ae = new AuthenticationException("Sai mật khẩu");
            assertEquals("Sai mật khẩu", ae.getMessage());
        }

        @Test
        @DisplayName("RuntimeException")
        void RunTimeException(){
            assertTrue(RuntimeException.class.isAssignableFrom(InvalidStateTransitionException.class));
        }
    }
}
