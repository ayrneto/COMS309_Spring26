package onetoone.Tasks.dto;

import onetoone.Tasks.Task;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private LocalDateTime reminderDateTime;
    private String status;

    public TaskResponse() {}

    public static TaskResponse from(Task task) {
        TaskResponse r = new TaskResponse();
        r.id               = task.getId();
        r.title            = task.getTitle();
        r.description      = task.getDescription();
        r.dueDate          = task.getDueDate();
        r.reminderDateTime = task.getReminderDateTime();
        r.status           = task.getStatus().getDisplayName();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}