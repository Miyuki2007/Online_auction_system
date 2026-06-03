package model.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bidder extends User {
    private final List<String> bidHistory;

    public Bidder(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
        this.bidHistory = new ArrayList<>();
    }

    @Override
    public String getRole() { return "BIDDER"; }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Tien gui can phai lon hon 0");
        setBalance(getBalance() + amount);
    }

    public void addBidToHistory(String bidID) { bidHistory.add(bidID); }

    public List<String> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }
}
