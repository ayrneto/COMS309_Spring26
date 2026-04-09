package onetoone.realtime_chat.dto;

public class ChatMessageRequest {
    private String type;       // "message" | "typing" | "read"
    private Long senderId;
    private Long receiverId;
    private String content;
    private Boolean isTyping;  // only used when type = "typing"

    // File attachment fields — only present when type = "message" and a file was sent
    private String fileUrl;    // full URL returned by POST /api/chat/upload
    private String fileName;   // original filename e.g. "resume.pdf"
    private String fileType;   // extension e.g. "PDF"

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

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
}