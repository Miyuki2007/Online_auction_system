package model.factory;

import model.item.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class ItemFactoryTest {

    @Test
    @DisplayName("Tạo Electronics qua factory")
    void create_electronics(){
        Item item = ItemFactory.createItem("ELECTRONICS","i1","Laptop","Gaming Laptop",1000.0,"Lenovo");
        assertNotNull(item);
        assertInstanceOf(Electronics.class,item);
        assertEquals("Laptop",item.getName());
        assertEquals(1000.0,item.getStartingPrice());
        assertEquals("Lenovo",((Electronics) item).getBrand());
    }

    @Test
    @DisplayName("Tạo vehicle qua factory")
    void create_vehilce(){
        Item item = ItemFactory.createItem("VEHICLE","i2","Vinfast VF3","Mẫu oto mới Vinfast",300000000.0,"Car");
        assertInstanceOf(Vehicle.class,item);
        assertEquals("Car",((Vehicle) item).getType());
    }

    @Test
    @DisplayName("Tạo Art qua factory")
    void create_art(){
        Item item = ItemFactory.createItem("ART","i3","Mona Lisa","Tranh sơn dầu",1000.0,"Leonardo Da Vinci");
        assertInstanceOf(Art.class,item);
        assertEquals("Leonardo Da Vinci",((Art) item).getArtist());
    }

    @Test
    @DisplayName("Tạo Others qua factory")
    void create_others(){
        Item item = ItemFactory.createItem("OTHERS","i4","Bình hoa cổ","Bình hoa sứ thời nhà Thanh",200.0,"Unknown");
        assertInstanceOf(Others.class,item);
    }

    @Test
    @DisplayName("Type case - insensitive: 'electronics' và 'Electronics' đều được")
    void create_caseInsensitive(){
        Item lower = ItemFactory.createItem("electronics","i1","Laptop","Gaming Laptop",1000.0,"Lenovo");
        Item mixed = ItemFactory.createItem("Electronics","i2","Laptop","Gaming Laptop",1000.0,"Lenovo");
        assertInstanceOf(Electronics.class,lower);
        assertInstanceOf(Electronics.class,mixed);
    }
    @Test
    @DisplayName("Type không hợp lệ - throw IllegalArgumentException")
    void create_invalidType(){
        assertThrows(IllegalArgumentException.class,()-> ItemFactory.createItem("INVALID_TYPE","i","n","d",100.0,"s"));
    }

    @Test
    @DisplayName("Type null - throw IllegalArgumentException")
    void create_nullType(){
        assertThrows(IllegalArgumentException.class,()-> ItemFactory.createItem(null,"i","n","d",100.0,"s"));
    }

    @Test
    @DisplayName("Overload có imageDate - lưu đúng data nếu độ dài lớn hơn 0")
    void create_withImageData_setsImage(){
        byte[] fakeImg = new byte[]{1,2,3,4,5};
        Item item = ItemFactory.createItem("ART","i3","Mona Lisa","Tranh sơn dầu",1000.0,"Leonardo Da Vinci",fakeImg);
        assertTrue(item.hasImage());
        assertArrayEquals(fakeImg,item.getImageData());
    }

    @Test
    @DisplayName("Overload có imageDate = null - không gắn ảnh")
    void create_withNullImage_noImage(){
        Item item = ItemFactory.createItem("ART","i3","Mona Lisa","Tranh sơn dầu",1000.0,"Leonardo Da Vinci",null);
        assertFalse(item.hasImage());
    }

    @Test
    @DisplayName("Overload có imageDate mảng rỗng - không gắn ảnh")
    void create_withEmptyImage_noImage(){
        Item item = ItemFactory.createItem("ART","i3","Mona Lisa","Tranh sơn dầu",1000.0,"Leonardo Da Vinci",new byte[0]);
        assertFalse(item.hasImage());
    }


}
