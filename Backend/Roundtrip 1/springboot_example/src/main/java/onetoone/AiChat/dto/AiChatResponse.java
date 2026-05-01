package onetoone.AiChat.dto;

import onetoone.AiChat.AiChatMessage;

import java.time.LocalDateTime;

/**
 * Response body returned from POST /api/ai-chat/{userId}
 * and from GET /api/ai-chat/{userId}/history
 */
public class AiChatResponse {

    private Long id;
    private String userMessage;
    private String aiReply;
    private LocalDateTime sentAt;

    public AiChatResponse() {}

    public AiChatResponse(AiChatMessage msg) {
        this.id = msg.getId();
        this.userMessage = msg.getUserMessage();
        this.aiReply = msg.getAiReply();
        this.sentAt = msg.getSentAt();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getUserMessage() { return userMessage; }
    public String getAiReply() { return aiReply; }
    public LocalDateTime getSentAt() { return sentAt; }
}