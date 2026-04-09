package onetoone.Tasks;

import onetoone.Notification.NotificationService;
import onetoone.Tasks.dto.TaskCreateRequest;
import onetoone.Tasks.dto.TaskUpdateRequest;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       NotificationService notificationService,
                       SimpMessagingTemplate messagingTemplate) {
        this.taskRepository      = taskRepository;
        this.userRepository      = userRepository;
        this.notificationService = notificationService;
        this.messagingTemplate   = messagingTemplate;
    }

    public Task createTask(TaskCreateRequest req) {
        User user = userRepository.findByEmail(req.getUserEmail());
        if (user == null) {
            throw new RuntimeException("User not found with email: " + req.getUserEmail());
        }

        Task task = new Task(
                user.getId(),
                req.getTitle(),
                req.getDescription(),
                req.getDueDate(),
                req.getReminderDateTime()
        );
        Task saved = taskRepository.save(task);

        pushWebSocket(user.getId(), saved, "TASK_ASSIGNED");
        notificationService.createNotification(user.getId(), "New task assigned: " + saved.getTitle());

        return saved;
    }

    public List<Task> getTasksForUser(Long userId) {
        return taskRepository.findByUserId(userId);
    }

    public Task updateTaskStatus(Long taskId, TaskStatus newStatus) {
        Task task = findOrThrow(taskId);
        task.setStatus(newStatus);
        return taskRepository.save(task);
    }

    public Task updateTask(Long taskId, TaskUpdateRequest req) {
        Task task = findOrThrow(taskId);
        if (req.getTitle()            != null) task.setTitle(req.getTitle());
        if (req.getDescription()      != null) task.setDescription(req.getDescription());
        if (req.getDueDate()          != null) task.setDueDate(req.getDueDate());
        if (req.getReminderDateTime() != null) task.setReminderDateTime(req.getReminderDateTime());
        return taskRepository.save(task);
    }

    @Scheduled(fixedRate = 60_000)
    public void sendDueReminders() {
        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusMinutes(1);

        List<Task> upcoming = taskRepository.findByReminderDateTimeBetween(now, windowEnd);

        for (Task task : upcoming) {
            if (task.getStatus() == TaskStatus.COMPLETED) continue;

            pushWebSocket(task.getUserId(), task, "TASK_REMINDER");
            notificationService.createNotification(
                    task.getUserId(),
                    "Reminder: \"" + task.getTitle() + "\" is due soon!"
            );
        }
    }

    private Task findOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
    }

    private void pushWebSocket(Long userId, Task task, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType",   eventType);
        payload.put("taskId",      task.getId());
        payload.put("title",       task.getTitle());
        payload.put("description", task.getDescription());
        payload.put("dueDate",     task.getDueDate() != null ? task.getDueDate().toString() : null);
        payload.put("status",      task.getStatus().getDisplayName());

        messagingTemplate.convertAndSend("/topic/user/" + userId, payload);
    }
}