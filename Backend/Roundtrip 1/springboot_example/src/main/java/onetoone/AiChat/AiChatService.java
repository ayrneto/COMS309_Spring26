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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired
    private AiChatMessageRepository aiChatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final String SYSTEM_PROMPT =
            "You are a supportive mental health assistant inside a student wellness app called Calmify. " +
                    "Your role: provide emotional support and encouragement, and suggest simple healthy coping strategies " +
                    "(breathing, journaling, walking, talking to someone). " +
                    "Keep every response calm, warm, and easy to understand — 2 to 4 sentences maximum. " +
                    "Rules: do NOT diagnose mental health conditions, do NOT provide medical or clinical advice, " +
                    "do NOT act as a therapist. " +
                    "Tone: warm, supportive, non-judgmental. Never dismiss feelings. Never sound robotic. " +
                    "Goal: help the user feel heard and guide them toward small positive steps.";

    private static final List<String> CRISIS_KEYWORDS = List.of(
            "suicide", "suicidal", "kill myself", "end my life", "want to die",
            "self harm", "self-harm", "cutting myself", "hurt myself",
            "giving up", "want to hurt myself", "no reason to live"
    );

    // Appended to Gemini's reply when crisis keywords detected — frontend can style this differently
    private static final String CRISIS_HOTLINE_APPEND =
            "\n\n If you're in crisis, please reach out: " +
                    "988 Suicide & Crisis Lifeline — call or text 988 (US). " +
                    "You don't have to face this alone.";

    // Number of past exchanges sent to Gemini for context (keep low for free tier)
    private static final int HISTORY_WINDOW = 5;

    // ── Public API ────────────────────────────────────────────────────────────

    public AiChatResponse chat(Long userId, String userMessage) {

        // 1. Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + userId));

        // 2. Validate
        if (userMessage == null || userMessage.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }
        userMessage = userMessage.trim();

        // 3. Check crisis keywords BEFORE calling Gemini
        boolean isCrisis = containsCrisisKeyword(userMessage);

        // 4. Fetch last N exchanges from DB to give Gemini conversation context
        List<AiChatMessage> recentHistory = getRecentHistory(userId);

        // 5. Call Gemini with full history so it understands references like "it" or "that"
        String aiReply = callGemini(userMessage, recentHistory);

        // 6. Append crisis hotline to whatever Gemini replied (don't replace — let AI reply stand)
        if (isCrisis) {
            aiReply = aiReply + CRISIS_HOTLINE_APPEND;
        }

        // 7. Save and return
        AiChatMessage saved = aiChatMessageRepository.save(
                new AiChatMessage(user, userMessage, aiReply));

        return new AiChatResponse(saved);
    }

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
     * Returns the last HISTORY_WINDOW exchanges from the DB for this user.
     */
    private List<AiChatMessage> getRecentHistory(Long userId) {
        List<AiChatMessage> all = aiChatMessageRepository.findByUserIdOrderBySentAtAsc(userId);
        int size = all.size();
        if (size <= HISTORY_WINDOW) return all;
        return all.subList(size - HISTORY_WINDOW, size);
    }

    /**
     * Calls Gemini with conversation history so it has full context.
     *
     * Contents array sent to Gemini:
     *   [systemPrimer, systemAck, user: past msg 1, model: past reply 1, ..., user: current msg]
     */
    @SuppressWarnings("unchecked")
    private String callGemini(String userMessage, List<AiChatMessage> history) {

        RestTemplate restTemplate = new RestTemplate();

        List<Map<String, Object>> contents = new ArrayList<>();

        // System prompt injected as fake user→model exchange (free tier workaround)
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", "Instructions for this conversation: " + SYSTEM_PROMPT))
        ));
        contents.add(Map.of(
                "role", "model",
                "parts", List.of(Map.of("text",
                        "Understood. I am Calmify's supportive assistant. I will keep responses warm, brief, and safe."))
        ));

        // Inject previous exchanges so Gemini understands context ("it", "that", "my anxiety" etc.)
        for (AiChatMessage past : history) {
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", past.getUserMessage()))
            ));
            contents.add(Map.of(
                    "role", "model",
                    "parts", List.of(Map.of("text", past.getAiReply()))
            ));
        }

        // Current message goes last
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        Map<String, Object> generationConfig = Map.of("maxOutputTokens", 300);
        Map<String, Object> requestBody = Map.of(
                "contents", contents,
                "generationConfig", generationConfig
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GEMINI_URL + geminiApiKey, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
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
            System.out.println("GEMINI ERROR TYPE: " + e.getClass().getName());
            System.out.println("GEMINI ERROR MSG: " + e.getMessage());
            return "I'm here for you! Sometimes taking a short walk or writing down your thoughts can help. " +
                    "What's on your mind?";
        }
    }
}