package protocol.responses;

import protocol.Response;

public class SuccessResponse extends Response {
    private static final long serialVersionUID = 1L;

    private final Object data;

    public SuccessResponse(String message, Object data) {
        super(Status.OK, message);
        this.data = data;
    }

    public Object getData() { return data; }

    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> clazz) {
        if (data == null) return null;
        if (!clazz.isInstance(data)) {
            throw new ClassCastException("Data không phải kiểu "
                    + clazz.getSimpleName() + ", mà là "
                    + data.getClass().getSimpleName());
        }
        return (T) data;
    }
}