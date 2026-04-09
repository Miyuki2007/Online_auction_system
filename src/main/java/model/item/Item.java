package model.item;
import model.Entity;
public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    public Item(String id, String name, String description, double startingPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
    }
    @Override
    public String getDisplayInfo() {
        return String.format("[%s] %s - Giá hiện tại: %.2f",
                getClass().getSimpleName(), name, currentPrice);
    }
    public abstract void displayDetails();
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public void setCurrentPrice(double currentPrice) {
        if (currentPrice <0){
            throw new IllegalArgumentException("Giá không được âm");
        }
        this.currentPrice = currentPrice;
    }
}