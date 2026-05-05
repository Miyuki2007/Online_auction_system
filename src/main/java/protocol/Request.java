package protocol;
import java.io.Serializable;
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Type{
        LOGIN, REGISTER,
        GET_AUCTIONS, GET_AUCTION_DETAIL,
        PLACE_BID, CREATE_AUCTION, CREATE_ITEM
    }
    private final Type type;
    private final String[] params; // Tham số dạng String array
    public Request(Type type, String... params) {
        this.type = type;
        this.params = params;
    }
    public Type getType() { return type; }
    public String[] getParams() {return params; }
    public String getParam(int id){
        if (params == null || id < 0 || id >= params.length){
            throw new IllegalArgumentException("Tham số không hợp lệ: index =" + id);
        }
        return params[id];
    }
}
