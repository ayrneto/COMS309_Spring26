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

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }
}
