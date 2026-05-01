package onetoone.AiChat;

import jakarta.persistence.*;
import onetoone.Users.User;

import java.time.LocalDateTime;

/**
 * Stores each AI chat exchange (user message + AI reply) in the DB.
 * One row = one user turn + the AI response to it.
 */
@Entity
@Table(name = "ai_chat_message")
public class AiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The message the user sent */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    /** The AI-generated reply */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String aiReply;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public AiChatMessage() {}

    public AiChatMessage(User user, String userMessage, String aiReply) {
        this.user = user;
        this.userMessage = userMessage;
        this.aiReply = aiReply;
        this.sentAt = LocalDateTime.now();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

    public String getAiReply() { return aiReply; }
    public void setAiReply(String aiReply) { this.aiReply = aiReply; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}