package onetoone.realtime_chat.dto;

import onetoone.realtime_chat.ChatMessage;
import java.time.LocalDateTime;

public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime sentAt;

    public ChatMessageResponse(ChatMessage msg) {
        this.id         = msg.getId();
        this.senderId   = msg.getSender().getId();
        this.receiverId = msg.getReceiver().getId();
        this.content    = msg.getContent();
        this.sentAt     = msg.getSentAt();
    }

    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public Long getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
}