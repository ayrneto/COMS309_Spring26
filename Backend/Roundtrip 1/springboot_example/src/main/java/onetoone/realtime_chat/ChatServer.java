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
import java.util.HashMap;
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

    // ── Commands ──────────────────────────────────────────────────────────────
    private static final String HELP_TEXT =
            "Available commands:\n" +
                    "  /help    — Show this list\n" +
                    "  /clear   — Clear chat history from your view\n" +
                    "  /status  — Show your connection status\n" +
                    "  /ping    — Check if the other user is online";

    @OnOpen
    public void onOpen(Session session,
                       @PathParam("senderId") Long senderId,
                       @PathParam("receiverId") Long receiverId) throws IOException {

        logger.info("[onOpen] senderId=" + senderId + " receiverId=" + receiverId);

        userSessionMap.put(senderId, session);
        sessionUserMap.put(session, senderId);

        sendToUser(senderId, buildSystem("Connected as user " + senderId));
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
            sendToUser(senderId, buildError("Invalid JSON format"));
            return;
        }

        String type = dto.getType() != null ? dto.getType() : "message";

        // ── TYPING indicator ──────────────────────────────────────────────────
        if ("typing".equals(type)) {
            Map<String, Object> typingPayload = new HashMap<>();
            typingPayload.put("type", "typing");
            typingPayload.put("senderId", senderId);
            typingPayload.put("isTyping", Boolean.TRUE.equals(dto.getIsTyping()));
            forwardToReceiver(dto.getReceiverId(), typingPayload);
            logger.info("[onMessage] Typing indicator from " + senderId
                    + " → " + dto.getReceiverId() + " isTyping=" + dto.getIsTyping());
            return;
        }

        // ── READ receipt ──────────────────────────────────────────────────────
        if ("read".equals(type)) {
            Map<String, Object> readPayload = new HashMap<>();
            readPayload.put("type", "read");
            readPayload.put("senderId", senderId);
            forwardToReceiver(dto.getReceiverId(), readPayload);
            logger.info("[onMessage] Read receipt from " + senderId + " → " + dto.getReceiverId());
            return;
        }

        // ── COMMAND ───────────────────────────────────────────────────────────
        String content = dto.getContent() != null ? dto.getContent().trim() : "";
        if (content.startsWith("/")) {
            handleCommand(content, senderId, receiverId);
            return;
        }

        // ── REGULAR MESSAGE ───────────────────────────────────────────────────

        // 2. Save to database
        ChatMessage saved;
        try {
            saved = chatService.saveMessage(dto.getSenderId(), dto.getReceiverId(), dto.getContent());
        } catch (Exception e) {
            logger.error("[onMessage] DB save failed: " + e.getMessage());
            sendToUser(senderId, buildError("Failed to save message"));
            return;
        }

        // 3. Build response JSON
        String responseJson = objectMapper.writeValueAsString(new ChatMessageResponse(saved));

        // 4. Forward to receiver if online
        if (userSessionMap.containsKey(receiverId)) {
            Session receiverSession = userSessionMap.get(receiverId);
            if (receiverSession.isOpen()) {
                receiverSession.getBasicRemote().sendText(responseJson);
                logger.info("[onMessage] Forwarded to user " + receiverId);
            }
        } else {
            logger.info("[onMessage] User " + receiverId + " not connected — saved to DB only");
        }

        // 5. Echo back to sender
        session.getBasicRemote().sendText(responseJson);
    }

    @OnClose
    public void onClose(Session session,
                        @PathParam("senderId") Long senderId,
                        @PathParam("receiverId") Long receiverId) throws IOException {

        logger.info("[onClose] user " + senderId + " disconnected");

        // Clear any stuck typing indicator on the other side
        Map<String, Object> offlinePayload = new HashMap<>();
        offlinePayload.put("type", "typing");
        offlinePayload.put("senderId", senderId);
        offlinePayload.put("isTyping", false);
        forwardToReceiver(receiverId, offlinePayload);

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

    // ── Command handler ───────────────────────────────────────────────────────

    private void handleCommand(String command, Long senderId, Long receiverId) {
        // grab first word only so "/help extra args" still works
        String cmd = command.split("\\s+")[0].toLowerCase();

        switch (cmd) {

            case "/help":
                // Send the help text back to sender only — never stored in DB
                sendToUser(senderId, buildSystem(HELP_TEXT));
                logger.info("[command] /help used by user " + senderId);
                break;

            case "/clear":
                // Tell the client to wipe its local view — no DB rows are deleted
                sendToUser(senderId, buildCommand("clear", null));
                logger.info("[command] /clear used by user " + senderId);
                break;

            case "/status":
                // Report connection status of both sides
                boolean senderOnline   = userSessionMap.containsKey(senderId);
                boolean receiverOnline = userSessionMap.containsKey(receiverId);
                String statusMsg = "You are " + (senderOnline ? "online" : "offline") +
                        ". User " + receiverId + " is " +
                        (receiverOnline ? "online" : "offline") + ".";
                sendToUser(senderId, buildSystem(statusMsg));
                logger.info("[command] /status used by user " + senderId);
                break;

            case "/ping":
                // Quick reachability check for the other user
                boolean isOnline = userSessionMap.containsKey(receiverId);
                String pingResult = isOnline
                        ? "User " + receiverId + " is online."
                        : "User " + receiverId + " is offline.";
                sendToUser(senderId, buildSystem(pingResult));
                logger.info("[command] /ping used by user " + senderId);
                break;

            default:
                sendToUser(senderId, buildSystem(
                        "Unknown command: " + cmd + ". Type /help for available commands."));
                break;
        }
    }

    // ── Payload builders ──────────────────────────────────────────────────────

    /**
     * System info bubble — shown only to the recipient, never saved to DB.
     * { "type": "system", "text": "..." }
     */
    private String buildSystem(String text) {
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("type", "system");
            m.put("text", text);
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{\"type\":\"system\",\"text\":\"" + text + "\"}";
        }
    }

    /**
     * Command action for the frontend to act on (e.g. wipe local view).
     * { "type": "command", "action": "clear", "data": null }
     */
    private String buildCommand(String action, Object data) {
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("type", "command");
            m.put("action", action);
            m.put("data", data);
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{\"type\":\"command\",\"action\":\"" + action + "\"}";
        }
    }

    private String buildError(String msg) {
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("type", "error");
            m.put("text", msg);
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"text\":\"" + msg + "\"}";
        }
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

    private void forwardToReceiver(Long receiverId, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            Session receiverSession = userSessionMap.get(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                receiverSession.getBasicRemote().sendText(json);
            }
        } catch (Exception e) {
            logger.error("[forwardToReceiver] Failed for user " + receiverId + ": " + e.getMessage());
        }
    }
}