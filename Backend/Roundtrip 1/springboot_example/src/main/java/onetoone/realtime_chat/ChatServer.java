package onetoone.realtime_chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import onetoone.realtime_chat.dto.ChatMessageRequest;
import onetoone.realtime_chat.dto.ChatMessageResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

@ServerEndpoint("/ws/chat/{senderId}/{receiverId}")
@Component
public class ChatServer {

    public static Map<Long, Session> userSessionMap = new Hashtable<>();
    private static Map<Session, Long> sessionUserMap = new Hashtable<>();

    private static ChatService chatService;

    @Autowired
    public void setChatService(ChatService service) {
        ChatServer.chatService = service;
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    @OnOpen
    public void onOpen(Session session,
                       @PathParam("senderId") Long senderId,
                       @PathParam("receiverId") Long receiverId) throws IOException {

        logger.info("[onOpen] senderId=" + senderId + " receiverId=" + receiverId);

        userSessionMap.put(senderId, session);
        sessionUserMap.put(session, senderId);

        sendToUser(senderId, "{\"system\": \"Connected as user " + senderId + "\"}");

        logger.info("[onOpen] Active sessions: " + userSessionMap.keySet());
    }

    @OnMessage
    public void onMessage(Session session, String messageJson,
                          @PathParam("senderId") Long senderId,
                          @PathParam("receiverId") Long receiverId) throws IOException {

        logger.info("[onMessage] from user " + senderId + ": " + messageJson);

        if (messageJson == null || messageJson.trim().isEmpty()) return;

        // 1. Parse JSON into request DTO
        ChatMessageRequest dto;
        try {
            dto = objectMapper.readValue(messageJson, ChatMessageRequest.class);
        } catch (Exception e) {
            logger.error("[onMessage] Failed to parse JSON: " + e.getMessage());
            sendToUser(senderId, "{\"error\": \"Invalid JSON format\"}");
            return;
        }

        // 2. Save to database
        ChatMessage saved;
        try {
            saved = chatService.saveMessage(dto.getSenderId(), dto.getReceiverId(), dto.getContent());
        } catch (Exception e) {
            logger.error("[onMessage] DB save failed: " + e.getMessage());
            sendToUser(senderId, "{\"error\": \"Failed to save message\"}");
            return;
        }

        // 3. Build response JSON using response DTO
        String responseJson = objectMapper.writeValueAsString(new ChatMessageResponse(saved));

        // 4. Forward to receiver if they are online
        Long targetReceiverId = dto.getReceiverId();
        if (userSessionMap.containsKey(targetReceiverId)) {
            Session receiverSession = userSessionMap.get(targetReceiverId);
            if (receiverSession.isOpen()) {
                receiverSession.getBasicRemote().sendText(responseJson);
                logger.info("[onMessage] Forwarded to user " + targetReceiverId);
            }
        } else {
            logger.info("[onMessage] User " + targetReceiverId + " not connected — saved to DB only");
        }

        // 5. Echo back to sender
        session.getBasicRemote().sendText(responseJson);
    }

    @OnClose
    public void onClose(Session session,
                        @PathParam("senderId") Long senderId,
                        @PathParam("receiverId") Long receiverId) throws IOException {

        logger.info("[onClose] user " + senderId + " disconnected");

        userSessionMap.remove(senderId);
        sessionUserMap.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable,
                        @PathParam("senderId") Long senderId,
                        @PathParam("receiverId") Long receiverId) {

        logger.error("[onError] user " + senderId + ": " + throwable.getMessage());

        userSessionMap.remove(senderId);
        sessionUserMap.remove(session);
    }

    private void sendToUser(Long userId, String message) {
        Session target = userSessionMap.get(userId);
        if (target != null && target.isOpen()) {
            try {
                target.getBasicRemote().sendText(message);
            } catch (IOException e) {
                logger.error("[sendToUser] Failed for user " + userId + ": " + e.getMessage());
            }
        }
    }
}