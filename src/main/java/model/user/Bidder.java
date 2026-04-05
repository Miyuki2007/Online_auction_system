package model.user;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
public class Bidder extends User {
    private double balance;
    private final List<String> bidHistory;
    public Bidder(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
        this.balance = 0.0;
        this.bidHistory = new ArrayList<>();
    }
    @Override
    public String getRole() {return "BIDDER"; }
    public double getBalance() {return balance; }
    public void deposit(double amount){
        if (amount <=0) throw new IllegalArgumentException("Deposit must be > 0");
        this.balance += amount;
    }
    public void addBidToHistory(String bidID){ bidHistory.add(bidID); }
    public List<String> getBidHistory(){
        return Collections.unmodifiableList(bidHistory);
    }
}
