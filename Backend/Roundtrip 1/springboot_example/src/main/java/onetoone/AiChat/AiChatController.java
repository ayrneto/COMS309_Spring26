package onetoone.AiChat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import onetoone.AiChat.dto.AiChatRequest;
import onetoone.AiChat.dto.AiChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for the Calmify AI Chat feature.
 *
 * Base path: /api/ai-chat
 *
 * Endpoints:
 *   POST   /api/ai-chat/{userId}          — send a message, get AI reply
 *   GET    /api/ai-chat/{userId}/history  — fetch full chat history for user
 *   DELETE /api/ai-chat/{userId}/history  — clear all AI chat history for user
 */
@RestController
@RequestMapping("/api/ai-chat")
@Tag(name = "AI Chat", description = "Mental health AI assistant powered by Gemini")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    // ── POST /api/ai-chat/{userId} ────────────────────────────────────────────

    /**
     * Send a message to the AI assistant.
     *
     * Request body:  { "message": "I feel really stressed today" }
     * Response body: { "id": 1, "userMessage": "...", "aiReply": "...", "sentAt": "..." }
     *
     * Example curl:
     *   curl -X POST http://localhost:8080/api/ai-chat/1 \
     *        -H "Content-Type: application/json" \
     *        -d '{"message": "I feel really stressed today"}'
     */
    @PostMapping("/{userId}")
    @Operation(
            summary = "Send a message to the AI assistant",
            description = "Sends the user's message to Gemini and returns the AI reply. " +
                    "Crisis keywords are intercepted and return a safe fixed response without calling Gemini."
    )
    public ResponseEntity<AiChatResponse> chat(
            @PathVariable Long userId,
            @RequestBody AiChatRequest request) {

        AiChatResponse response = aiChatService.chat(userId, request.getMessage());
        return ResponseEntity.ok(response);
    }

    // ── GET /api/ai-chat/{userId}/history ─────────────────────────────────────

    /**
     * Retrieve the full AI chat history for a user (oldest first).
     *
     * Example curl:
     *   curl http://localhost:8080/api/ai-chat/1/history
     */
    @GetMapping("/{userId}/history")
    @Operation(
            summary = "Get AI chat history",
            description = "Returns all AI chat exchanges for the given user, ordered oldest-first."
    )
    public ResponseEntity<List<AiChatResponse>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(aiChatService.getHistory(userId));
    }

    // ── DELETE /api/ai-chat/{userId}/history ──────────────────────────────────

    /**
     * Clear all AI chat history for a user.
     *
     * Example curl:
     *   curl -X DELETE http://localhost:8080/api/ai-chat/1/history
     */
    @DeleteMapping("/{userId}/history")
    @Operation(
            summary = "Clear AI chat history",
            description = "Deletes all stored AI chat messages for the given user."
    )
    public ResponseEntity<String> clearHistory(@PathVariable Long userId) {
        aiChatService.clearHistory(userId);
        return ResponseEntity.ok("AI chat history cleared for user " + userId);
    }
}