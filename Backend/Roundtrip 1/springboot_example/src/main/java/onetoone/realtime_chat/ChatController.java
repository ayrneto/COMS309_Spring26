package onetoone.realtime_chat;

import onetoone.realtime_chat.dto.ChatMessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/history")
    public List<ChatMessageResponse> getHistory(
            @RequestParam Long userA,
            @RequestParam Long userB) {
        return chatService.getChatHistory(userA, userB)
                .stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    // NEW — lets frontend/you verify who is actually in the session map
    @GetMapping("/online-users")
    public Map<String, Object> getOnlineUsers() {
        Map<String, Object> result = new HashMap<>();
        result.put("connectedUserIds", ChatServer.userSessionMap.keySet());
        return result;
    }
}