package protocol.requests;

import protocol.Request;

public class CreateAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;

    private final String sellerId;
    private final String itemType;        // ELECTRONICS, ART, VEHICLE
    private final String itemName;
    private final String itemDescription;
    private final String specialAttribute; // brand/artist/type tuỳ loại
    private final double startingPrice;
    private final long durationMinutes;
    private final boolean antiSnipeEnabled;

    public CreateAuctionRequest(String sellerId, String itemType,
                                String itemName, String itemDescription,
                                String specialAttribute,
                                double startingPrice, long durationMinutes,
                                boolean antiSnipeEnabled) {
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải > 0");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Thời lượng phải > 0");
        }
        this.sellerId = sellerId;
        this.itemType = itemType;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.specialAttribute = specialAttribute;
        this.startingPrice = startingPrice;
        this.durationMinutes = durationMinutes;
        this.antiSnipeEnabled = antiSnipeEnabled;
    }

    @Override
    public Type getType() { return Type.CREATE_AUCTION; }

    public String getSellerId() { return sellerId; }
    public String getItemType() { return itemType; }
    public String getItemName() { return itemName; }
    public String getItemDescription() { return itemDescription; }
    public String getSpecialAttribute() { return specialAttribute; }
    public double getStartingPrice() { return startingPrice; }
    public long getDurationMinutes() { return durationMinutes; }
    public boolean isAntiSnipeEnabled() { return antiSnipeEnabled; }
}