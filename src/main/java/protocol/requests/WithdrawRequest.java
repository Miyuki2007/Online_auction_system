package protocol.requests;

import protocol.Request;

public class WithdrawRequest extends Request {
    private static final long serialVersionUID = 1L;
    private final double amount;

    public WithdrawRequest(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien rut phai > 0");
        }
        this.amount = amount;
    }

    public double getAmount() { return amount; }

    @Override
    public Type getType() { return Type.WITHDRAW; }
}
