package onetoone.Notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean isRead;

    private LocalDateTime createdAt;

    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public Notification(Long userId, String message, NotificationType type) {
        this();
        this.userId = userId;
        this.message = message;
        this.type = type;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setRead(boolean isRead){
        this.isRead = isRead;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isRead() {
        return isRead;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public NotificationType getType()
    {
        return type;
    }

    public String getMessage() {
        return message;
    }
}