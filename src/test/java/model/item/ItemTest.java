package model.item;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {
    // --- ELECTRONICS ---
    @Nested
    @DisplayName("Electronics")
    class ElectronicsTests{
        @Test
        @DisplayName("Tạo Electronics hợp lệ")
        void create(){
            Electronics e = new Electronics("e1","Laptop","Gaming",1000.0,"Lenovo");
            assertEquals("Laptop",e.getName());
            assertEquals(1000.0,e.getStartingPrice());
            assertEquals(1000.0,e.getCurrentPrice());
            assertEquals("Lenovo",e.getBrand());
        }

        @Test
        @DisplayName("Brand null - throw IllegalArgumentException")
        void brand_null(){
            assertThrows(IllegalArgumentException.class,()-> new Electronics("e1","Laptop","Mô tả",1000.0,null));
        }

        @Test
        @DisplayName("Brand rỗng - throw IllegalArgumentException")
        void brand_brank(){
            assertThrows(IllegalArgumentException.class,()-> new Electronics("e1","Laptop","Mô tả",1000.0,"  "));
        }
    }
    // --- VEHICLE ---
    @Nested
    @DisplayName("Vehicle")
    class VehicleTests{
        @Test
        @DisplayName("Tạo Vehilce hợp lệ")
        void create(){
            Vehicle v = new Vehicle("v1","Vinfast VF3","Mẫu oto mới vinfast",300000000.0,"Car");
            assertEquals("Car",v.getType());
            assertEquals(300000000.0,v.getStartingPrice());
        }

        @Test
        @DisplayName("Type rỗng - throw IllegalArgumentException")
        void type_blank(){
            assertThrows(IllegalArgumentException.class,()-> new Vehicle("v1","Vinfast","Mô tả",1000.0,""));
        }
    }
    // --- ART ---
    @Nested
    @DisplayName("Art")
    class ArtTests{
        @Test
        @DisplayName("Tạo Art hợp lệ")
        void create(){
            Art a = new Art("a1","SunFlower","Sơn dầu",1000.0,"Van Gogh");
            assertEquals("Van Gogh",a.getArtist());
        }

        @Test
        @DisplayName("Artist rỗng - throw IllegalArgumentException")
        void artist_blank(){
            assertThrows(IllegalArgumentException.class,()-> new Art("a1","Tranh","Mô tả",1000.0,""));
        }
    }
    // --- Hành vi Item phổ biến ---
    @Nested
    @DisplayName("Item (common behavior)")
    class CommonItemTests{
        @Test
        @DisplayName("Tên sản phẩm rỗng - throw IllegalArgumentException")
        void name_blank(){
            assertThrows(IllegalArgumentException.class,()-> new Electronics("e1","","Mô tả",1000.0,"Brand"));
            assertThrows(IllegalArgumentException.class,()-> new Electronics("e1",null,"Mô tả",1000.0,"Brand"));
        }
        @Test
        @DisplayName("Mô tả rỗng - throw IllegalArgumentException")
        void description_blank(){
            assertThrows(IllegalArgumentException.class,()-> new Electronics("e1","name","",1000.0,"Brand"));
        }

        @Test
        @DisplayName("Giá khởi điểm âm - throw IllegalArgumentException")
        void startingPrice_negativeThrows(){
            assertThrows(IllegalArgumentException.class,()-> new Electronics("e1","name","Mô tả",-1000.0,"Brand"));
        }
        @Test
        @DisplayName("Giá khởi điểm bằng 0")
        void startingPrice_Zero(){
            Electronics e = new Electronics("e1","name","Mô tả",0.0,"Brand");
            assertEquals(0.0,e.getStartingPrice());
        }

        @Test
        @DisplayName("setCurrentPrice cao hơn giá hiện tại")
        void setCurrentPrice(){
            Electronics e = new Electronics("e1","name","Mô tả",1000.0,"Brand");
            e.setCurrentPrice(1500.0);
            assertEquals(1500.0,e.getCurrentPrice());
        }

        @Test
        @DisplayName("setCurrentPrice thấp hơn giá hiện tại - throw IllegalArgumentException")
        void setCurrentPrice_lower(){
            Electronics e = new Electronics("e1","name","Mô tả",1000.0,"Brand");
            e.setCurrentPrice(1500.0);
            assertThrows(IllegalArgumentException.class,()-> e.setCurrentPrice(1200.0));
        }

        @Test
        @DisplayName("hasImage trả false khi không có ảnh, true khi có")
        void hasImage(){
            Electronics e = new Electronics("e1","name","Mô tả",1000.0,"Brand");
            assertFalse(e.hasImage());
            e.setImageData(new byte[]{1,2,3});
            assertTrue(e.hasImage());
        }

        @Test
        @DisplayName("getDisplayInfo format theo class")
        void displayInfo(){
            Electronics e = new Electronics("e1","name","Mô tả",1000.0,"Brand");
            String info = e.getDisplayInfo();
            assertTrue(info.contains("Electronics"));
            assertTrue(info.contains("name"));
        }

        @Test
        @DisplayName("Hai Item cùng ID")
        void equals_byId(){
            Electronics a = new Electronics("same-id","Laptop","Mô tả",1000.0,"Brand");
            Electronics b = new Electronics("same-id","Phone","Mô tả",2000.0,"Brand");
            assertEquals(a,b);
            assertEquals(a.hashCode(),b.hashCode());
        }

        @Test
        @DisplayName("Hai item khác ID")
        void notEquals_differentId(){
            Electronics a = new Electronics("id1","Laptop","Mô tả",1000.0,"Brand");
            Electronics b = new Electronics("id2","Laptop","Mô tả",1000.0,"Brand");
            assertNotEquals(a,b);
        }

    }
}
