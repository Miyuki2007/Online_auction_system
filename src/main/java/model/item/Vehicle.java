package model.item;


public class Vehicle extends Item {
    private String type;
    public Vehicle(String id, String name, String description, double price, String type) {
        super(id, name, description, price);
        this.type = type;
    }
    @Override
    public void displayDetails() {
        System.out.println("Tên sản phẩm: " + getName() + " Loại vật phẩm: Phương tiện");
        System.out.println("Id: " + getId());
        System.out.println("Mô tả: " + getDescription() + " Loại: " + type);
        System.out.println("Giá khởi điểm: " + getStartingPrice());
        System.out.println("Giá hiện tại: " + getCurrentPrice());
    }
}
