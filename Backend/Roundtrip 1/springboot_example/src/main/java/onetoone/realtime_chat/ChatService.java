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

    public ChatMessage saveMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderId));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + receiverId));

        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content);
        msg.setSentAt(LocalDateTime.now());

        return chatMessageRepository.save(msg);
    }

    public List<ChatMessage> getChatHistory(Long userA, Long userB) {
        return chatMessageRepository.findConversation(userA, userB);
    }
}