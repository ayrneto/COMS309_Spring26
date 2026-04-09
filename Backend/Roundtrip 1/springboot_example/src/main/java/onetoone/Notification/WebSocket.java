package onetoone.Notification;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * FIX: Removed .withSockJS() so the endpoint is a true WebSocket.
 *
 * With SockJS removed, clients can now connect directly via:
 *   ws://localhost:8080/ws
 *
 * If you need SockJS (for browser fallback), clients must use
 * the SockJS client library and connect via HTTP, not ws://.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocket implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket")  // changed from /ws to /websocket
                .setAllowedOriginPatterns("*");
    }
}