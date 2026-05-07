package protocol.responses;

import protocol.Response;

/**
 * Notification do Server push xuống Client (không phải đáp lại request).
 * Ví dụ: bid mới, auction kết thúc, time extended...
 */
public class NotificationResponse extends Response {
    private static final long serialVersionUID = 1L;

    /**
     * Loại notification để client biết cách xử lý:
     - BID_UPDATE: có bid mới
     - STATE_CHANGED: auction đổi trạng thái
     - TIME_EXTENDED: anti-snipe kéo dài thời gian
     - AUCTION_CREATED: có auction mới được tạo
     */
    public enum NotificationType {
        BID_UPDATE,
        STATE_CHANGED,
        TIME_EXTENDED,
        AUCTION_CREATED
    }

    private final NotificationType notificationType;
    private final Object data;

    public NotificationResponse(NotificationType type, String message, Object data) {
        super(Status.NOTIFICATION, message);
        this.notificationType = type;
        this.data = data;
    }

    public NotificationType getNotificationType() { return notificationType; }
    public Object getData() { return data; }


    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> clazz) {
        if (data == null) return null;
        if (!clazz.isInstance(data)) {
            throw new ClassCastException("Data không phải kiểu "
                    + clazz.getSimpleName());
        }
        return (T) data;
    }
}