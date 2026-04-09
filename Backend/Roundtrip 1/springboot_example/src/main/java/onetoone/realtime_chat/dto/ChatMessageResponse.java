package onetoone.realtime_chat.dto;

import onetoone.realtime_chat.ChatMessage;
import java.time.LocalDateTime;

public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime sentAt;

    // File attachment fields — null for regular text messages
    private String fileUrl;
    private String fileName;
    private String fileType;

    public ChatMessageResponse(ChatMessage msg) {
        this.id         = msg.getId();
        this.senderId   = msg.getSender().getId();
        this.receiverId = msg.getReceiver().getId();
        this.content    = msg.getContent();
        this.sentAt     = msg.getSentAt();
        this.fileUrl    = msg.getFileUrl();
        this.fileName   = msg.getFileName();
        this.fileType   = msg.getFileType();
    }

    public Long getId()           { return id; }
    public Long getSenderId()     { return senderId; }
    public Long getReceiverId()   { return receiverId; }
    public String getContent()    { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getFileUrl()    { return fileUrl; }
    public String getFileName()   { return fileName; }
    public String getFileType()   { return fileType; }
}