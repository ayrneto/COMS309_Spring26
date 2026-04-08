package onetoone.Notification;

public class NotificationMessage {

    private Long userId;
    private String message;
    private String type;

    public NotificationMessage() {}

    public NotificationMessage(Long userId, String message, String type) {
        this.userId = userId;
        this.message = message;
        this.type = type;
    }

    // Getters & Setters
}
