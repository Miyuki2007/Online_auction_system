package model.factory;

import model.item.Item;
import model.item.Electronics;
import model.item.Vehicle;
import model.item.Art;

public class ItemFactory {
    public static Item createItem(String type, String id, String name, String desc, double price, String specialAttr) {
        if (type == null) {
            return null;
        }
        String itemType = type.toUpperCase();
        switch (itemType) {
            case "ELECTRONICS":
                return new Electronics(id, name, desc, price, specialAttr);
            case "VEHICLE":
                return new Vehicle(id, name, desc, price, specialAttr);
            case "ART":
                return new Art(id, name, desc, price, specialAttr);
            default:
                throw new IllegalArgumentException("Loại sản phẩm '" + type + "' không hợp lệ!");
        }
    }
}