package protocol.requests;

import protocol.Request;

public class DepositRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final double amount;

    public DepositRequest(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien nap phai > 0");
        }
        this.amount = amount;
    }

    public double getAmount() { return amount; }

    @Override
    public Type getType() { return Type.DEPOSIT; }
}
