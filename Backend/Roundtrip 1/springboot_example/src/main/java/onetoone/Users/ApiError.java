package onetoone.Users;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;



@Schema(description = "Structured API error response")
public class ApiError {
    @Schema(description = "Timestamp when the error occurred", example = "2026-04-05T14:23:30")
    private LocalDateTime timestamp;
    @Schema(description = "HTTP status code", example = "404")
    private int status;
    @Schema(description = "Short description of the error", example = "Not Found")
    private String error;
    @Schema(description = "Detailed message about the error", example = "User with id 123 not found")
    private String message;
    @Schema(description = "The API path that triggered the error", example = "/api/users/123")
    private String path;

    public ApiError(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
}