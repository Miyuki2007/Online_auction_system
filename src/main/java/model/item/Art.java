package model.item;


public class Art extends Item {
    private String artist;

    public Art(String id, String name, String description, double price, String artist) {
        super(id, name, description, price);
        setArtist(artist);
    }

    public String getArtist() { return artist; }

    public void setArtist(String artist) {
        if (artist == null || artist.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tác giả không được để trống.");
        }
        this.artist = artist;
    }
    @Override
    public void displayDetails(){
        System.out.println("Tên sản phẩm: " + getName() + " Loại vật phẩm: Nghệ Thuật");
        System.out.println("Id: " + getId());
        System.out.println("Mô tả: " + getDescription() + " Tác giả: " + artist);
        System.out.println("Giá khởi điểm: " + getStartingPrice());
        System.out.println("Giá hiện tại: " + getCurrentPrice());
    }
}
