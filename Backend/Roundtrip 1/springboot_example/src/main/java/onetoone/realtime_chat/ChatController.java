package onetoone.realtime_chat;

import onetoone.realtime_chat.dto.ChatMessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    // Change this to match your actual server host/port if needed
    private static final String BASE_URL = "http://REDACTED:8080";

    @Autowired
    private ChatService chatService;

    // ── Existing endpoints ────────────────────────────────────────────────────

    @GetMapping("/history")
    public List<ChatMessageResponse> getHistory(
            @RequestParam Long userA,
            @RequestParam Long userB) {
        return chatService.getChatHistory(userA, userB)
                .stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/online-users")
    public Map<String, Object> getOnlineUsers() {
        Map<String, Object> result = new HashMap<>();
        result.put("connectedUserIds", ChatServer.userSessionMap.keySet());
        return result;
    }

    // ── NEW: File upload endpoint ─────────────────────────────────────────────

    /**
     * POST /api/chat/upload
     *
     * Multipart form params:
     *   file       — the actual file (any type)
     *   senderId   — Long  (who is uploading)
     *   receiverId — Long  (who will receive it)
     *
     * Response JSON:
     * {
     *   "fileUrl":  "http://<host>/uploads/1712345678000_resume.pdf",
     *   "fileName": "resume.pdf",
     *   "fileType": "PDF"
     * }
     *
     * NOTE: This endpoint only stores the file on disk and returns the URL.
     * The frontend is responsible for then sending a WebSocket message that
     * includes this fileUrl (plus fileName/fileType) so it gets persisted
     * in the chat_message table and forwarded to the receiver in real time.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senderId") Long senderId,
            @RequestParam("receiverId") Long receiverId) throws IOException {

        if (file.isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "File is empty");
            return ResponseEntity.badRequest().body(err);
        }

        // Build a unique filename to avoid collisions: timestamp_originalName
        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "file";
        String fileName = System.currentTimeMillis() + "_" + originalName;

        // Ensure the uploads directory exists, then write the file
        Path uploadDir = Paths.get("uploads");
        Files.createDirectories(uploadDir);
        Files.copy(file.getInputStream(), uploadDir.resolve(fileName));

        // Build the public URL the frontend will use to download/preview the file
        String fileUrl = BASE_URL + "/uploads/" + fileName;

        Map<String, String> response = new HashMap<>();
        response.put("fileUrl",  fileUrl);
        response.put("fileName", originalName);
        response.put("fileType", getExtension(originalName));

        return ResponseEntity.ok(response);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toUpperCase() : "FILE";
    }
}