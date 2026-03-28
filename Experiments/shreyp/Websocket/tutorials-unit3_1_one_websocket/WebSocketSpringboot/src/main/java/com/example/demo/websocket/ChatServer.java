package com.example.demo.websocket;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

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
 * This class is annotated with Spring's `@ServerEndpoint` and `@Component`
 * annotations, making it a WebSocket endpoint that can handle WebSocket
 * connections at the "/chat/{username}" endpoint.
 *
 * Example URL: ws://localhost:8080/chat/username
 *
 * The server provides functionality for broadcasting messages to all connected
 * users and sending messages to specific users.
 */
@ServerEndpoint("/chat/{username}")
@Component
public class ChatServer {

    // Store all socket session and their corresponding username
    // Two maps for the ease of retrieval by key
    private static Map < Session, String > sessionUsernameMap = new Hashtable < > ();
    private static Map < String, Session > usernameSessionMap = new Hashtable < > ();
    private static Map<String, Set<Session>> roomMap = new Hashtable<>();


    private static final String COMMAND_PREFIX = "/";
    // server side logger
    private final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    /**
     * This method is called when a new WebSocket connection is established.
     *
     * @param session represents the WebSocket session for the connected user.
     * @param username username specified in path parameter.
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) throws IOException {
        logger.info("[onOpen] " + username);

        if (usernameSessionMap.containsKey(username)) {
            session.getBasicRemote().sendText("Username already exists");
            session.close();
        } else {
            sessionUsernameMap.put(session, username);
            usernameSessionMap.put(username, session);

            // Send welcome
            sendMessageToPArticularUser(username, "Welcome to the chat server, " + username);

            // Broadcast join
            broadcast("User: " + username + " has Joined the Chat");

            // Feature 1: Broadcast updated user list
            broadcast("Online Users: " + getConnectedUsers());
        }
    }

    /**
     * Handles incoming WebSocket messages from a client.
     *
     * @param session The WebSocket session representing the client's connection.
     * @param message The message received from the client.
     */
    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        if(message == null || message.trim().isEmpty()) return;

        String username = sessionUsernameMap.get(session);
        logger.info("[onMessage] " + username + ": " + message);

        // Feature 3: Check if it's a command
        if(handleCommand(session, message)) return;

        // Feature 2: Room message -> "#roomname message"
        if(message.startsWith("#")) {
            String[] split_msg = message.split("\\s+", 2);
            if(split_msg.length < 2) {
                sendMessageToPArticularUser(username, "Room message must have format: #roomname message");
                return;
            }
            String roomName = split_msg[0].substring(1); // remove #
            String roomMessage = split_msg[1];

            joinRoom(roomName, session); // add user to room if not already
            broadcastToRoom(roomName, username + ": " + roomMessage);
            return;
        }

        // Direct message
        if(message.startsWith("@")) {
            String[] split_msg = message.split("\\s+", 2);
            if(split_msg.length < 2) return; // invalid
            String destUserName = split_msg[0].substring(1); // remove @
            String actualMessage = split_msg[1];
            sendMessageToPArticularUser(destUserName, "[DM from " + username + "]: " + actualMessage);
            sendMessageToPArticularUser(username, "[DM to " + destUserName + "]: " + actualMessage);
            return;
        }

        // Normal broadcast
        broadcast(username + ": " + message);
    }

    /**
     * Handles the closure of a WebSocket connection.
     *
     * @param session The WebSocket session that is being closed.
     */
    @OnClose
    public void onClose(Session session) throws IOException {
        String username = sessionUsernameMap.get(session);
        logger.info("[onClose] " + username);

        sessionUsernameMap.remove(session);
        usernameSessionMap.remove(username);

        broadcast(username + " disconnected");

        // Feature 1: broadcast updated user list
        broadcast("Online Users: " + getConnectedUsers());
    }

    /**
     * Handles WebSocket errors that occur during the connection.
     *
     * @param session   The WebSocket session where the error occurred.
     * @param throwable The Throwable representing the error condition.
     */
    @OnError
    public void onError(Session session, Throwable throwable) {

        // get the username from session-username mapping
        String username = sessionUsernameMap.get(session);

        // do error handling here
        logger.info("[onError]" + username + ": " + throwable.getMessage());
    }

    /**
     * Sends a message to a specific user in the chat (DM).
     *
     * @param username The username of the recipient.
     * @param message  The message to be sent.
     */
    private void sendMessageToPArticularUser(String username, String message) {
        try {
            usernameSessionMap.get(username).getBasicRemote().sendText(message);
        } catch (IOException e) {
            logger.info("[DM Exception] " + e.getMessage());
        }
    }

    /**
     * Broadcasts a message to all users in the chat.
     *
     * @param message The message to be broadcasted to all users.
     */
    private void broadcast(String message) {
        sessionUsernameMap.forEach((session, username) -> {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                logger.info("[Broadcast Exception] " + e.getMessage());
            }
        });
    }

    // Feature 1: Get connected users as a comma-separated string
    private String getConnectedUsers() {
        return String.join(", ", usernameSessionMap.keySet());
    }

    // Feature 2: Join a room
    private void joinRoom(String roomName, Session session) {
        roomMap.computeIfAbsent(roomName, k -> new HashSet<>()).add(session);
    }

    // Feature 2: Broadcast to a specific room
    private void broadcastToRoom(String roomName, String message) {
        if(roomMap.containsKey(roomName)) {
            for(Session session : roomMap.get(roomName)) {
                try {
                    session.getBasicRemote().sendText("[Room " + roomName + "] " + message);
                } catch (IOException e) {
                    logger.info("[Room Broadcast Exception] " + e.getMessage());
                }
            }
        }
    }

    // Feature 3: Handle server commands
    private boolean handleCommand(Session session, String message) throws IOException {
        if(!message.startsWith(COMMAND_PREFIX)) return false; // not a command

        String username = sessionUsernameMap.get(session);
        switch(message.toLowerCase()) {
            case "/who":
                sendMessageToPArticularUser(username, "Online Users: " + getConnectedUsers());
                break;
            case "/help":
                sendMessageToPArticularUser(username, "Commands: /who, /help, @username message, #room message");
                break;
            default:
                sendMessageToPArticularUser(username, "Unknown command: " + message);
        }
        return true; // command handled
    }
}