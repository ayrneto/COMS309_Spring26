package onetoone.Notification;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/notification")
@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository repo,
                               SimpMessagingTemplate messagingTemplate) {
        this.repo = repo;
        this.messagingTemplate = messagingTemplate;
    }

    public Notification createNotification(@PathVariable Long userId, @PathVariable String message) {
        Notification notif = new Notification(userId, message, NotificationType.GENERAL);
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

    public void markNotificationAsRead(Long notificationId) {
        Notification notif = repo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id " + notificationId));
        notif.setRead(true);
        repo.save(notif);
    }
    public void deleteNotification(Long id) {
        repo.deleteById(id);
    }
}
