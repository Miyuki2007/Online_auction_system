package protocol;

import java.io.Serializable;

public abstract class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, REGISTER,
        GET_AUCTIONS, GET_AUCTION_DETAIL,
        PLACE_BID,
        CREATE_AUCTION, CANCEL_AUCTION,
        GET_MY_AUCTIONS
    }

    public abstract Type getType();
}