package model.auction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class AuctionStateTest {
    @Test
    @DisplayName("OPEN có thể chuyển sang RUNNING hoặc CANCELED")
    void open_canTransition(){
        assertTrue(AuctionState.OPEN.canTransition(AuctionState.RUNNING));
        assertTrue(AuctionState.OPEN.canTransition(AuctionState.CANCELED));
        assertFalse(AuctionState.OPEN.canTransition(AuctionState.FINISHED));
        assertFalse(AuctionState.OPEN.canTransition(AuctionState.PAID));
    }

    @Test
    @DisplayName("RUNNING có thể chuyển sang FINISED hoặc CANCELED")
    void running_canTransition(){
        assertTrue(AuctionState.RUNNING.canTransition(AuctionState.FINISHED));
        assertTrue(AuctionState.RUNNING.canTransition(AuctionState.CANCELED));
        assertFalse(AuctionState.RUNNING.canTransition(AuctionState.OPEN));
        assertFalse(AuctionState.RUNNING.canTransition(AuctionState.PAID));
    }

    @Test
    @DisplayName("FINISHED có thể chuyển sang PAID hoặc CANCELED")
    void finished_canTransition(){
        assertTrue(AuctionState.FINISHED.canTransition(AuctionState.PAID));
        assertTrue(AuctionState.FINISHED.canTransition(AuctionState.CANCELED));
        assertFalse(AuctionState.FINISHED.canTransition(AuctionState.RUNNING));
    }

    @Test
    @DisplayName("PAID và CANCELED không thể chuyển đi đâu")
    void terminal_cannotTransition(){
       for (AuctionState s : AuctionState.values()){
           assertFalse(AuctionState.PAID.canTransition(s));
           assertFalse(AuctionState.CANCELED.canTransition(s));
       }
    }


}
