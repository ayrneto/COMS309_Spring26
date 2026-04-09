package onetoone.Tasks.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskCreateRequest {

    private String userEmail;
    private String title;
    private String description;
    private LocalDate dueDate;
    private LocalDateTime reminderDateTime;

    public TaskCreateRequest() {}

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getReminderDateTime() { return reminderDateTime; }
    public void setReminderDateTime(LocalDateTime reminderDateTime) {
        this.reminderDateTime = reminderDateTime;
    }
}