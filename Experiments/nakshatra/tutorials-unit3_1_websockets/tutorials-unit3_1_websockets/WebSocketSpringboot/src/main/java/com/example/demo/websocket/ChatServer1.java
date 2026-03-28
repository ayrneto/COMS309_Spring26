package com.example.demo.websocket;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;
import java.util.Map;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Represents a WebSocket chat server for handling real-time communication
 * between users. Each user connects to the server using their unique
 * username.
 *
 * MODIFICATIONS by Nakshatra Gupta:
 * - Added timestamps to all broadcast messages
 * - Added /users command to list currently online users
 */
@ServerEndpoint("/chat/1/{username}")
@Component
public class ChatServer1 {

    private static Map<Session, String> sessionUsernameMap = new Hashtable<>();
    private static Map<String, Session> usernameSessionMap = new Hashtable<>();

    private final Logger logger = LoggerFactory.getLogger(ChatServer1.class);

    // Helper: get current time as formatted string e.g. [09:44 AM]
    private String getTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        return "[" + LocalTime.now().format(formatter) + "]";
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) throws IOException {
        logger.info("[onOpen] " + username);

        if (usernameSessionMap.containsKey(username)) {
            session.getBasicRemote().sendText("Username already exists");
            session.close();
        } else {
            sessionUsernameMap.put(session, username);
            usernameSessionMap.put(username, session);

            // Personalized welcome message with timestamp
            sendMessageToPArticularUser(username, getTimestamp() + " Welcome to Nakshatra's chat server, " + username + "! 👋");

            // Notify everyone with timestamp
            broadcast(getTimestamp() + " 🟢 User: " + username + " has joined the chat!");
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        String username = sessionUsernameMap.get(session);
        logger.info("[onMessage] " + username + ": " + message);

        // /users command — list all online users
        if (message.equalsIgnoreCase("/users")) {
            String userList = String.join(", ", usernameSessionMap.keySet());
            sendMessageToPArticularUser(username, getTimestamp() + " 👥 Online users: " + userList);
        }
        // Direct message format: @username message
        else if (message.startsWith("@")) {
            String[] split_msg = message.split("\\s+");
            StringBuilder actualMessageBuilder = new StringBuilder();
            for (int i = 1; i < split_msg.length; i++) {
                actualMessageBuilder.append(split_msg[i]).append(" ");
            }
            String destUserName = split_msg[0].substring(1);
            String actualMessage = actualMessageBuilder.toString();
            sendMessageToPArticularUser(destUserName, getTimestamp() + " [DM from " + username + "]: " + actualMessage);
            sendMessageToPArticularUser(username, getTimestamp() + " [DM to " + destUserName + "]: " + actualMessage);
        }
        // Broadcast to everyone with timestamp
        else {
            broadcast(getTimestamp() + " " + username + ": " + message);
        }
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        String username = sessionUsernameMap.get(session);
        logger.info("[onClose] " + username);

        sessionUsernameMap.remove(session);
        usernameSessionMap.remove(username);

        broadcast(getTimestamp() + " 🔴 " + username + " has left the chat.");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        String username = sessionUsernameMap.get(session);
        logger.info("[onError] " + username + ": " + throwable.getMessage());
    }

    private void sendMessageToPArticularUser(String username, String message) {
        try {
            usernameSessionMap.get(username).getBasicRemote().sendText(message);
        } catch (IOException e) {
            logger.info("[DM Exception] " + e.getMessage());
        }
    }

    private void broadcast(String message) {
        sessionUsernameMap.forEach((session, username) -> {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                logger.info("[Broadcast Exception] " + e.getMessage());
            }
        });
    }
}