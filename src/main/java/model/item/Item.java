package model.item;
import model.Entity;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private byte[] imageData;

    public Item(String id, String name, String description, double startingPrice) {
        super(id);
        setName(name); // Sử dụng setter để tận dụng validation
        setDescription(description);
        setStartingPrice(startingPrice);
        this.currentPrice = startingPrice;
    }
    public Item(String id, String name, String description, double startingPrice, byte[] imageData) {
        this(id, name, description, startingPrice);
        this.imageData = imageData;
    }
    // --- GETTERS ---
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentPrice() { return currentPrice; }

    // --- SETTERS WITH VALIDATION ---
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }
        this.name = name;
    }

    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Mô tả không được để trống.");
        }
        this.description = description;
    }

    public void setStartingPrice(double startingPrice) {
        if (startingPrice < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không được âm.");
        }
        this.startingPrice = startingPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        if (currentPrice < this.currentPrice) {
            throw new IllegalArgumentException("Giá mới phải cao hơn hoặc bằng giá hiện tại.");
        }
        this.currentPrice = currentPrice;
    }
    public byte[] getImageData() {
        return imageData;
    }
    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }
    public boolean hasImage() {
        return imageData != null && imageData.length > 0;
    }

    @Override
    public String getDisplayInfo() {
        return String.format("[%s] %s - Giá hiện tại: %.2f",
                getClass().getSimpleName(), name, currentPrice);
    }
    public abstract void displayDetails();
}