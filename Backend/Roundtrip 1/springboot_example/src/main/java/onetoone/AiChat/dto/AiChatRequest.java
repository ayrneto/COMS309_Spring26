package onetoone.AiChat.dto;

/**
 * Request body for POST /api/ai-chat/{userId}
 */
public class AiChatRequest {

    private String message;

    public AiChatRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}