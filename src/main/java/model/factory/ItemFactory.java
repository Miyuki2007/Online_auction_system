package model.factory;
import model.item.Item;
import model.item.Electronics;
import model.item.Vehicle;
import model.item.Art;
import model.item.Others;

public class ItemFactory {
    public static Item createItem(String type, String id, String name, String desc, double price, String specialAttr){
        return createItem(type, id, name, desc, price, specialAttr,null);
    }
    public static Item createItem(String type, String id, String name, String desc, double price, String specialAttr,byte[] imageData) {
        if (type == null) {
            throw new IllegalArgumentException("Loại sản phẩm không được bỏ qua");
        }
        String itemType = type.toUpperCase();
        Item item = switch (itemType) {
            case "OTHERS" -> new Others(id, name, desc, price, specialAttr);
            case "ELECTRONICS" -> new Electronics(id, name, desc, price, specialAttr);
            case "VEHICLE" -> new Vehicle(id, name, desc, price, specialAttr);
            case "ART" -> new Art(id, name, desc, price, specialAttr);
            default -> throw new IllegalArgumentException("Loại sản phẩm '" + type + "' không hợp lệ!");
        };
        if (imageData!=null && imageData.length>0){
            item.setImageData(imageData);
        }
        return item;
    }
}