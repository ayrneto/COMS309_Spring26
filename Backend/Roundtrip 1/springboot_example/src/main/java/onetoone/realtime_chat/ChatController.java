package onetoone.realtime_chat;

import onetoone.realtime_chat.dto.ChatMessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // GET /api/chat/history?userA=3&userB=7
    @GetMapping("/history")
    public List<ChatMessageResponse> getHistory(
            @RequestParam Long userA,
            @RequestParam Long userB) {

        return chatService.getChatHistory(userA, userB)
                .stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }
}