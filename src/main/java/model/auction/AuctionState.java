package model.auction;
/*
Quy trình một vòng đấu giá
Open - running - finished - paid/ canceled
 */
// Enumeration: kiểu dữ liệu đặc biệt trong Java để định nghĩa một tập hợp các hằng số cố định, không thay đổi
//--> Bắt buộc phải chọn các định nghĩa sẵn trong enum
public enum AuctionState {
    OPEN,             // Phiên đấu giá được tạo
    RUNNING,          // Đang diễn ra phiên đấu giá
    FINISHED,         // Hết thời gian đấu giá
    PAID,             // Người thắng đã thanh toán
    CANCELED;         // Phiên bị hủy
    //Chuyển trạng thái
    public boolean canTransition(AuctionState next){
        return switch(this){
            case OPEN -> next == RUNNING || next == CANCELED;
            case RUNNING -> next == FINISHED || next == CANCELED;
            case FINISHED -> next == PAID || next == CANCELED;
            case PAID, CANCELED -> false;
        };
    }
}
