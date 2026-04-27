package onetoone.AiChat;

import onetoone.AiChat.dto.AiChatResponse;
import onetoone.Users.User;
import onetoone.Users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Handles all AI chat logic:
 *  1. Safety-checks the user's message for crisis keywords
 *  2. Calls the Gemini 1.5 Flash REST API (free tier)
 *  3. Persists the exchange to the database
 */
@Service
public class AiChatService {

    // ── Injected config ───────────────────────────────────────────────────────

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired
    private AiChatMessageRepository aiChatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    /**
     * The system prompt that shapes every Gemini response.
     * Keeps replies short, warm, and safe for a student wellness context.
     */
    private static final String SYSTEM_PROMPT =
            "You are a supportive mental health assistant inside a student wellness app called Calmify. " +
                    "Your role: provide emotional support and encouragement, and suggest simple healthy coping strategies " +
                    "(breathing, journaling, walking, talking to someone). " +
                    "Keep every response calm, warm, and easy to understand — 2 to 4 sentences maximum. " +
                    "Rules: do NOT diagnose mental health conditions, do NOT provide medical or clinical advice, " +
                    "do NOT act as a therapist, do NOT give crisis hotline instructions yourself. " +
                    "Tone: warm, supportive, non-judgmental. Never dismiss feelings. Never sound robotic. " +
                    "Goal: help the user feel heard and guide them toward small positive steps.";

    /** Keywords that trigger an immediate fixed safe response instead of calling the AI */
    private static final List<String> CRISIS_KEYWORDS = List.of(
            "suicide", "suicidal", "kill myself", "end my life", "want to die",
            "self harm", "self-harm", "cutting myself", "hurt myself"
    );

    private static final String CRISIS_RESPONSE =
            "I'm really sorry you're feeling this way — you don't have to go through this alone. " +
                    "Please consider reaching out to someone you trust, a counsellor, or a mental health professional. " +
                    "You matter, and help is available.";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Main method: validates user, safety-checks message, calls Gemini, saves and returns result.
     */
    public AiChatResponse chat(Long userId, String userMessage) {

        // 1. Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + userId));

        // 2. Trim and basic validation
        if (userMessage == null || userMessage.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }
        userMessage = userMessage.trim();

        // 3. Crisis keyword check — bypass AI entirely
        String aiReply;
        if (containsCrisisKeyword(userMessage)) {
            aiReply = CRISIS_RESPONSE;
        } else {
            // 4. Call Gemini
            aiReply = callGemini(userMessage);
        }

        // 5. Persist and return
        AiChatMessage saved = aiChatMessageRepository.save(
                new AiChatMessage(user, userMessage, aiReply));

        return new AiChatResponse(saved);
    }

    /**
     * Returns the full AI chat history for a user, oldest-first.
     */
    public List<AiChatResponse> getHistory(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
        return aiChatMessageRepository
                .findByUserIdOrderBySentAtAsc(userId)
                .stream()
                .map(AiChatResponse::new)
                .toList();
    }

    /**
     * Deletes all AI chat history for a user.
     */
    public void clearHistory(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
        List<AiChatMessage> messages = aiChatMessageRepository.findByUserIdOrderBySentAtAsc(userId);
        aiChatMessageRepository.deleteAll(messages);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean containsCrisisKeyword(String message) {
        String lower = message.toLowerCase();
        return CRISIS_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * Calls the Gemini 1.5 Flash API.
     * We embed the system prompt as the first "model" turn (few-shot priming trick)
     * because the free REST endpoint does not have a dedicated system-instruction field.
     */
    @SuppressWarnings("unchecked")
    private String callGemini(String userMessage) {

        RestTemplate restTemplate = new RestTemplate();

        // Build request body — Gemini expects { "contents": [ { "parts": [ {"text": "..."} ] } ] }
        // We prime with a user→model exchange so the model "knows" its persona.
        Map<String, Object> systemPrimer = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", "Instructions for this conversation: " + SYSTEM_PROMPT))
        );
        Map<String, Object> systemAck = Map.of(
                "role", "model",
                "parts", List.of(Map.of("text",
                        "Understood. I am Calmify's supportive assistant. I will keep responses warm, brief, and safe."))
        );
        Map<String, Object> userTurn = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        );

        // Keep responses short — cap at 300 tokens
        Map<String, Object> generationConfig = Map.of("maxOutputTokens", 300);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(systemPrimer, systemAck, userTurn),
                "generationConfig", generationConfig
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GEMINI_URL + geminiApiKey, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Navigate: body → candidates[0] → content → parts[0] → text
                List<Map<String, Object>> candidates =
                        (List<Map<String, Object>>) response.getBody().get("candidates");

                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }

            return "I'm here for you. It sounds like something is on your mind — " +
                    "try taking a few slow, deep breaths, and feel free to share more when you're ready.";

        } catch (Exception e) {
            // Fallback if Gemini is unreachable or quota is exceeded
            return "I'm here for you! Sometimes taking a short walk or writing down your thoughts can help. " +
                    "What's on your mind?";
        }
    }
}