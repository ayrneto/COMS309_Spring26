package onetoone.realtime_chat;

import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Save a plain text message (no file attachment).
     */
    public ChatMessage saveMessage(Long senderId, Long receiverId, String content) {
        return saveMessage(senderId, receiverId, content, null, null, null);
    }

    /**
     * Save a message that may carry an optional file attachment.
     * Pass null for fileUrl/fileName/fileType when there is no file.
     */
    public ChatMessage saveMessage(Long senderId, Long receiverId, String content,
                                   String fileUrl, String fileName, String fileType) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderId));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + receiverId));

        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content != null ? content : "");  // content can be empty for file-only messages
        msg.setSentAt(LocalDateTime.now());
        msg.setFileUrl(fileUrl);
        msg.setFileName(fileName);
        msg.setFileType(fileType);

        return chatMessageRepository.save(msg);
    }

    public List<ChatMessage> getChatHistory(Long userA, Long userB) {
        return chatMessageRepository.findConversation(userA, userB);
    }
}