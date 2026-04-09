package onetoone.Tasks;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import onetoone.Tasks.dto.TaskCreateRequest;
import onetoone.Tasks.dto.TaskResponse;
import onetoone.Tasks.dto.TaskStatusRequest;
import onetoone.Tasks.dto.TaskUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Tasks", description = "Task assignment, retrieval, and reminder management")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Create / assign a task to a user by email")
    @PostMapping("/api/tasks")
    public ResponseEntity<?> createTask(@RequestBody TaskCreateRequest req) {
        if (req.getUserEmail() == null || req.getUserEmail().isBlank())
            return ResponseEntity.badRequest().body("{\"message\":\"userEmail is required\"}");
        if (req.getTitle() == null || req.getTitle().isBlank())
            return ResponseEntity.badRequest().body("{\"message\":\"title is required\"}");
        try {
            Task created = taskService.createTask(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(created));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @Operation(summary = "Get all tasks assigned to a user")
    @GetMapping("/api/users/{userId}/tasks")
    public ResponseEntity<List<TaskResponse>> getUserTasks(@PathVariable Long userId) {
        List<TaskResponse> tasks = taskService.getTasksForUser(userId)
                .stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tasks);
    }

    @Operation(summary = "Update task status")
    @PutMapping("/api/tasks/{taskId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long taskId,
                                          @RequestBody TaskStatusRequest req) {
        if (req.getStatus() == null || req.getStatus().isBlank())
            return ResponseEntity.badRequest().body("{\"message\":\"status is required\"}");
        try {
            TaskStatus newStatus = TaskStatus.fromString(req.getStatus());
            Task updated = taskService.updateTaskStatus(taskId, newStatus);
            return ResponseEntity.ok(TaskResponse.from(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("{\"message\":\"" + e.getMessage() + "\"}");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @Operation(summary = "Update task fields")
    @PutMapping("/api/tasks/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable Long taskId,
                                        @RequestBody TaskUpdateRequest req) {
        try {
            Task updated = taskService.updateTask(taskId, req);
            return ResponseEntity.ok(TaskResponse.from(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"message\":\"" + e.getMessage() + "\"}");
        }
    }
}