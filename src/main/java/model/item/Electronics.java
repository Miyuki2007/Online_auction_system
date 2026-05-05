package model.item;


public class Electronics extends Item {
    private String brand;

    public Electronics(String id, String name, String description, double price, String brand) {
        super(id, name, description, price);
        setBrand(brand);
    }

    public String getBrand() { return brand; }

    public void setBrand(String brand) {
        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Nhãn hiệu không được để trống.");
        }
        this.brand = brand;
    }
    @Override
    public void displayDetails() {
        System.out.println("Tên sản phẩm: " + getName() + " Loại vật phẩm: Đồ điện tử");
        System.out.println("Id: " + getId());
        System.out.println("Mô tả: " + getDescription() + " Nhãn hiệu: " + brand);
        System.out.println("Giá khởi điểm: " + getStartingPrice());
        System.out.println("Giá hiện tại: " + getCurrentPrice());
    }
}
