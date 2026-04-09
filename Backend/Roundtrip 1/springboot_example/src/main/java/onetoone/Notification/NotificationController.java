package onetoone.Notification;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/create")
    public Notification createNotification(
            @RequestParam Long userId,
            @RequestParam String message) {

        return notificationService.createNotification(userId, message);
    }

    @PutMapping("/mark-as-read")
    public void markAsRead(@RequestParam Long id) {
        notificationService.markNotificationAsRead(id);
    }
}