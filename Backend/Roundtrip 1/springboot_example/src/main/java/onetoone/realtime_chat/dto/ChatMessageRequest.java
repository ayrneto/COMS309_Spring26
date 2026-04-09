package onetoone.realtime_chat.dto;

public class ChatMessageRequest {
    private String type;      // "message" | "typing" | "read"
    private Long senderId;
    private Long receiverId;
    private String content;
    private Boolean isTyping; // only used when type = "typing"

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getIsTyping() { return isTyping; }
    public void setIsTyping(Boolean isTyping) { this.isTyping = isTyping; }
}