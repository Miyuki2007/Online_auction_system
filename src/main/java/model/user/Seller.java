package model.user;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class Seller extends User {
    private final List<String> listedItemIds;
    private double revenue;

    public Seller(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
        this.listedItemIds = new ArrayList<>();
        this.revenue = 0.0;
    }
    @Override
    public String getRole() {return "SELLER"; }
    public void addListedItem(String itemId ){ listedItemIds.add(itemId); }
    public List<String> getListItemIds(){
        return Collections.unmodifiableList(listedItemIds);
    }
    public double getRevenue(){return revenue;}
    public void addRevenue(double amount) {this.revenue += amount; }
}
