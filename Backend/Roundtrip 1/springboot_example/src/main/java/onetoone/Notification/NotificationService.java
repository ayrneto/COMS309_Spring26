package onetoone.Notification;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository repo,
                               SimpMessagingTemplate messagingTemplate) {
        this.repo = repo;
        this.messagingTemplate = messagingTemplate;
    }

    // Create general notification
    public Notification createNotification(Long userId, String message) {
        Notification notif = new Notification(userId, message, NotificationType.GENERAL);
        repo.save(notif);

        sendRealtimeNotification(userId, notif);

        return notif;
    }

    // Create daily reminder
    public Notification createDailyReminder(Long userId, String message) {
        Notification notif = new Notification(userId, message, NotificationType.DAILY_REMINDER);
        repo.save(notif);

        sendRealtimeNotification(userId, notif);

        return notif;
    }

    private void sendRealtimeNotification(Long userId, Notification notif) {
        NotificationMessage msg = new NotificationMessage(
                userId,
                notif.getMessage(),
                notif.getType().name()
        );

        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                msg
        );
    }

    public List<Notification> getUserNotifications(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(Long id) {
        Notification notif = repo.findById(id).orElseThrow();
        notif.setRead(true);
        repo.save(notif);
    }

    public void deleteNotification(Long id) {
        repo.deleteById(id);
    }
}
