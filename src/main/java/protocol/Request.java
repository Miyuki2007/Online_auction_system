package protocol;

import java.io.Serializable;

public abstract class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, REGISTER,
        GET_AUCTIONS, GET_AUCTION_DETAIL,
        PLACE_BID,
        CREATE_AUCTION, CANCEL_AUCTION,
        GET_MY_AUCTIONS,
        REGISTER_AUTO_BID,
        ADMIN_GET_ALL_USERS,
        ADMIN_SET_USER_ACTIVE,
        ADMIN_FORCE_CANCEL_AUCTION,
        ADMIN_GET_STATS,
        GET_MY_BID_HISTORY,
        CANCEL_AUTO_BID,
        DEPOSIT,
        WITHDRAW,
        GET_WALLET
    }
    public abstract Type getType();
}
