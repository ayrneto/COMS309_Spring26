package com.example.androidexample;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "CHAT_DEBUG";

    private static final List<String> COMMANDS = Arrays.asList(
            "/help", "/ping", "/status", "/clear"
    );

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView recyclerMessages;
    private EditText     etMessage;
    private TextView     tvChatPartnerName;
    private TextView     tvOnlineStatus;
    private TextView     tvAvailabilityStatus;
    private TextView     tvTypingIndicator;
    private ListView     lvCommandSuggestions;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<ChatMessage> messageList = new ArrayList<>();
    private ChatAdapter chatAdapter;

    // ── Session ───────────────────────────────────────────────────────────────
    private long   myUserId;
    private long   partnerUserId;
    private String partnerName = "Chat";
    private String myRole      = "USER";

    // ── WebSocket ─────────────────────────────────────────────────────────────
    private WebSocketClient wsClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Typing ────────────────────────────────────────────────────────────────
    private final Runnable hideTypingRunnable     = () ->
            tvTypingIndicator.setVisibility(View.INVISIBLE);
    private final Runnable sendTypingStartRunnable = this::sendTypingStart;
    private final Runnable sendTypingStopRunnable  = this::sendTypingStop;

    // ── File picker ───────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) uploadFile(uri);
                    }
            );

    // ── OkHttp for file upload ────────────────────────────────────────────────
    private final OkHttpClient httpClient = new OkHttpClient();

    // ── Time formatter ────────────────────────────────────────────────────────
    private final SimpleDateFormat timeFmt =
            new SimpleDateFormat("h:mm a", Locale.getDefault());

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        myUserId = Long.parseLong(prefs.getString("USER_ID", "-1"));
        myRole   = prefs.getString("USER_ROLE", "USER");

        partnerUserId = getIntent().getLongExtra("partnerUserId", -1L);
        partnerName   = getIntent().getStringExtra("partnerName");
        if (partnerName == null || partnerName.isEmpty()) partnerName = "Chat";

        recyclerMessages     = findViewById(R.id.recyclerMessages);
        etMessage            = findViewById(R.id.etMessage);
        tvChatPartnerName    = findViewById(R.id.tvChatPartnerName);
        tvOnlineStatus       = findViewById(R.id.tvOnlineStatus);
        tvAvailabilityStatus = findViewById(R.id.tvAvailabilityStatus);
        tvTypingIndicator    = findViewById(R.id.tvTypingIndicator);
        lvCommandSuggestions = findViewById(R.id.lvCommandSuggestions);
        ImageButton btnBack  = findViewById(R.id.btnBack);

        tvChatPartnerName.setText(partnerName);
        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());

        // Attach button — opens file picker for any file type
        findViewById(R.id.btnAttach).setOnClickListener(v ->
                filePicker.launch(new String[]{"*/*"})
        );

        chatAdapter = new ChatAdapter(messageList, myUserId);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(llm);
        recyclerMessages.setAdapter(chatAdapter);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String input = s.toString();
                if (input.startsWith("/")) {
                    List<String> matches = new ArrayList<>();
                    for (String cmd : COMMANDS) {
                        if (cmd.startsWith(input)) matches.add(cmd);
                    }
                    if (!matches.isEmpty()) showCommandSuggestions(matches);
                    else hideCommandSuggestions();
                } else {
                    hideCommandSuggestions();
                }
                mainHandler.removeCallbacks(sendTypingStartRunnable);
                mainHandler.removeCallbacks(sendTypingStopRunnable);
                if (input.length() > 0) {
                    mainHandler.postDelayed(sendTypingStartRunnable, 300);
                    mainHandler.postDelayed(sendTypingStopRunnable, 2000);
                } else {
                    sendTypingStop();
                }
            }
        });

        if (partnerUserId == -1) {
            Toast.makeText(this, "Error: partner ID missing", Toast.LENGTH_LONG).show();
            return;
        }

        if ("USER".equals(myRole)) fetchAvailability();
        fetchHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacks(hideTypingRunnable);
        mainHandler.removeCallbacks(sendTypingStartRunnable);
        mainHandler.removeCallbacks(sendTypingStopRunnable);
        sendTypingStop();
        disconnectWebSocket();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File upload
    // POST /api/chat/upload (multipart)
    // Returns: { "fileUrl": "...", "fileName": "...", "fileType": "..." }
    // ─────────────────────────────────────────────────────────────────────────
    private void uploadFile(Uri uri) {
        String fileName = getFileName(uri);
        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null) mimeType = "application/octet-stream";

        Toast.makeText(this, "Uploading " + fileName + "…", Toast.LENGTH_SHORT).show();

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "Could not read file", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] fileBytes = inputStream.readAllBytes();
            inputStream.close();

            String uploadUrl = ApiConstants.BASE_URL + "/api/chat/upload";
            final String finalMimeType = mimeType;

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("senderId",   String.valueOf(myUserId))
                    .addFormDataPart("receiverId", String.valueOf(partnerUserId))
                    .addFormDataPart("file", fileName,
                            RequestBody.create(fileBytes,
                                    MediaType.parse(finalMimeType)))
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() ->
                            Toast.makeText(ChatActivity.this,
                                    "Upload failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        mainHandler.post(() ->
                                Toast.makeText(ChatActivity.this,
                                        "Upload failed (HTTP " + response.code() + ")",
                                        Toast.LENGTH_LONG).show()
                        );
                        return;
                    }

                    String body = response.body().string();
                    mainHandler.post(() -> {
                        try {
                            JSONObject json    = new JSONObject(body);
                            String fileUrl     = json.getString("fileUrl");
                            String fName       = json.optString("fileName",  fileName);
                            String fileType    = json.optString("fileType",  "FILE");

                            // Send file message over WebSocket
                            sendFileMessage(fileUrl, fName, fileType);

                            // Show locally
                            ChatMessage msg = new ChatMessage(
                                    myUserId, partnerUserId, "",
                                    timeFmt.format(new Date()), false,
                                    fileUrl, fName, fileType);
                            appendMessage(msg);

                            Toast.makeText(ChatActivity.this,
                                    "File sent!", Toast.LENGTH_SHORT).show();

                        } catch (JSONException e) {
                            Toast.makeText(ChatActivity.this,
                                    "Upload response error", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error reading file: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send file message over WebSocket
    // { "senderId": X, "receiverId": Y, "content": "", "fileUrl": "...",
    //   "fileName": "...", "fileType": "..." }
    // ─────────────────────────────────────────────────────────────────────────
    private void sendFileMessage(String fileUrl, String fileName, String fileType) {
        if (wsClient == null || !wsClient.isOpen()) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("senderId",   myUserId);
            payload.put("receiverId", partnerUserId);
            payload.put("content",    "");
            payload.put("fileUrl",    fileUrl);
            payload.put("fileName",   fileName);
            payload.put("fileType",   fileType);
        } catch (JSONException e) { e.printStackTrace(); }
        wsClient.send(payload.toString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Download file using DownloadManager
    // ─────────────────────────────────────────────────────────────────────────
    private void downloadFile(String fileUrl, String fileName) {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(fileUrl);
            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setTitle(fileName)
                    .setDescription("Downloading from Calmify")
                    .setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS, fileName)
                    .setAllowedOverMetered(true);
            dm.enqueue(request);
            Toast.makeText(this, "Downloading " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Fallback — open in browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
            startActivity(intent);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get file name from URI
    // ─────────────────────────────────────────────────────────────────────────
    private String getFileName(Uri uri) {
        String name = "file";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx);
            cursor.close();
        }
        return name;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get file type label from file name
    // ─────────────────────────────────────────────────────────────────────────
    private String getFileTypeLabel(String fileName) {
        if (fileName == null) return "FILE";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf"))                          return "PDF";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png"))                   return "IMAGE";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "WORD";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "EXCEL";
        if (lower.endsWith(".mp4") || lower.endsWith(".mov"))  return "VIDEO";
        if (lower.endsWith(".mp3") || lower.endsWith(".wav"))  return "AUDIO";
        if (lower.endsWith(".zip") || lower.endsWith(".rar"))  return "ZIP";
        return "FILE";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Slash command suggestions
    // ─────────────────────────────────────────────────────────────────────────
    private void showCommandSuggestions(List<String> matches) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, matches);
        lvCommandSuggestions.setAdapter(adapter);
        lvCommandSuggestions.setVisibility(View.VISIBLE);
        lvCommandSuggestions.setOnItemClickListener((parent, view, pos, id) -> {
            etMessage.setText(matches.get(pos));
            etMessage.setSelection(etMessage.getText().length());
            hideCommandSuggestions();
        });
    }

    private void hideCommandSuggestions() {
        lvCommandSuggestions.setVisibility(View.GONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fetch counsellor availability
    // ─────────────────────────────────────────────────────────────────────────
    private void fetchAvailability() {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, ApiConstants.counselorProfile(partnerUserId), null,
                response -> {
                    String status = response.optString("status", "");
                    switch (status.toUpperCase()) {
                        case "AVAILABLE":
                            tvAvailabilityStatus.setText("● Available");
                            tvAvailabilityStatus.setTextColor(0xFF81C784);
                            break;
                        case "BUSY":
                            tvAvailabilityStatus.setText("● Busy");
                            tvAvailabilityStatus.setTextColor(0xFFFFB74D);
                            break;
                        case "OFFLINE":
                            tvAvailabilityStatus.setText("● Offline");
                            tvAvailabilityStatus.setTextColor(0xFFB0BEC5);
                            break;
                        default:
                            tvAvailabilityStatus.setText("");
                    }
                },
                error -> tvAvailabilityStatus.setText("")
        );
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load chat history
    // ─────────────────────────────────────────────────────────────────────────
    private void fetchHistory() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                ApiConstants.chatHistory(myUserId, partnerUserId),
                null,
                response -> {
                    messageList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            messageList.add(parseMessage(response.getJSONObject(i)));
                        } catch (JSONException e) {
                            Log.e(TAG, "History parse error: " + e.getMessage());
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    scrollToBottom();
                    connectWebSocket();
                },
                error -> {
                    Log.e(TAG, "History load failed");
                    Toast.makeText(this, "Could not load history", Toast.LENGTH_SHORT).show();
                    connectWebSocket();
                }
        );
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WebSocket
    // ─────────────────────────────────────────────────────────────────────────
    private void connectWebSocket() {
        try {
            wsClient = new WebSocketClient(URI.create(ApiConstants.wsChat(myUserId, partnerUserId))) {
                @Override public void onOpen(ServerHandshake h) {
                    mainHandler.post(() -> tvOnlineStatus.setText("● Connected"));
                }
                @Override public void onMessage(String text) {
                    mainHandler.post(() -> handleIncomingMessage(text));
                }
                @Override public void onClose(int code, String reason, boolean remote) {
                    mainHandler.post(() -> {
                        tvOnlineStatus.setText("○ Disconnected");
                        mainHandler.postDelayed(() -> {
                            if (!isDestroyed()) connectWebSocket();
                        }, 3000);
                    });
                }
                @Override public void onError(Exception ex) {
                    mainHandler.post(() -> tvOnlineStatus.setText("○ Error"));
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            Toast.makeText(this, "WebSocket error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void disconnectWebSocket() {
        if (wsClient != null && wsClient.isOpen()) wsClient.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send text message
    // ─────────────────────────────────────────────────────────────────────────
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        if (wsClient == null || !wsClient.isOpen()) {
            Toast.makeText(this, "Not connected — reconnecting…", Toast.LENGTH_SHORT).show();
            connectWebSocket();
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("type",       "message");
            payload.put("senderId",   myUserId);
            payload.put("receiverId", partnerUserId);
            payload.put("content",    text);
        } catch (JSONException e) { e.printStackTrace(); return; }

        wsClient.send(payload.toString());

        hideCommandSuggestions();
        sendTypingStop();
        mainHandler.removeCallbacks(sendTypingStartRunnable);
        mainHandler.removeCallbacks(sendTypingStopRunnable);

        if (!text.startsWith("/")) {
            appendMessage(new ChatMessage(myUserId, partnerUserId, text,
                    timeFmt.format(new Date()), false));
        }
        etMessage.setText("");
    }

    private void sendTypingStart() { sendTypingEvent(true); }
    private void sendTypingStop()  { sendTypingEvent(false); }

    private void sendTypingEvent(boolean isTyping) {
        if (wsClient == null || !wsClient.isOpen()) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("type",       "typing");
            payload.put("senderId",   myUserId);
            payload.put("receiverId", partnerUserId);
            payload.put("isTyping",   isTyping);
        } catch (JSONException e) { e.printStackTrace(); }
        wsClient.send(payload.toString());
    }

    private void sendReadReceipt() {
        if (wsClient == null || !wsClient.isOpen()) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("type",       "read");
            payload.put("senderId",   myUserId);
            payload.put("receiverId", partnerUserId);
        } catch (JSONException e) { e.printStackTrace(); }
        wsClient.send(payload.toString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Handle incoming WebSocket frame
    // ─────────────────────────────────────────────────────────────────────────
    private void handleIncomingMessage(String text) {
        Log.d(TAG, "Received: " + text);
        try {
            JSONObject obj = new JSONObject(text);

            // No "type" field = real chat message (text or file)
            if (!obj.has("type")) {
                ChatMessage msg = parseMessage(obj);
                if (msg.senderId == myUserId) return;
                tvTypingIndicator.setVisibility(View.INVISIBLE);
                mainHandler.removeCallbacks(hideTypingRunnable);
                appendMessage(msg);
                sendReadReceipt();
                return;
            }

            String type = obj.getString("type");
            switch (type) {
                case "typing":
                    boolean isTyping = obj.optBoolean("isTyping", true);
                    if (isTyping) {
                        tvTypingIndicator.setText(partnerName + " is typing…");
                        tvTypingIndicator.setVisibility(View.VISIBLE);
                        mainHandler.removeCallbacks(hideTypingRunnable);
                        mainHandler.postDelayed(hideTypingRunnable, 3000);
                    } else {
                        tvTypingIndicator.setVisibility(View.INVISIBLE);
                        mainHandler.removeCallbacks(hideTypingRunnable);
                    }
                    break;
                case "read":
                    for (int i = messageList.size() - 1; i >= 0; i--) {
                        if (messageList.get(i).senderId == myUserId) {
                            messageList.get(i).isRead = true;
                            chatAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    break;
                case "system":
                    appendSystemMessage(obj.optString("text", ""));
                    break;
                case "command":
                    if ("clear".equals(obj.optString("action"))) {
                        messageList.clear();
                        chatAdapter.notifyDataSetChanged();
                    }
                    break;
                case "error":
                    Toast.makeText(this, obj.optString("text", "Error"),
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Log.d(TAG, "Unknown type: " + type);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Parse error: " + e.getMessage());
        }
    }

    private void appendSystemMessage(String text) {
        messageList.add(new ChatMessage(-1, -1, text, "", false));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parse message — handles text and file messages
    // ─────────────────────────────────────────────────────────────────────────
    private ChatMessage parseMessage(JSONObject obj) throws JSONException {
        long   senderId   = obj.getLong("senderId");
        long   receiverId = obj.getLong("receiverId");
        String content    = obj.optString("content", "");
        String rawTime    = obj.optString("sentAt", "");
        String fileUrl    = obj.optString("fileUrl",  null);
        String fileName   = obj.optString("fileName", null);
        String fileType   = obj.optString("fileType", null);

        // Auto-detect fileType from name if not provided
        if (fileUrl != null && fileType == null && fileName != null) {
            fileType = getFileTypeLabel(fileName);
        }

        String displayTime;
        try {
            if (rawTime.startsWith("[")) {
                displayTime = "";
            } else {
                SimpleDateFormat iso =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date d = iso.parse(rawTime);
                displayTime = (d != null) ? timeFmt.format(d) : rawTime;
            }
        } catch (Exception e) {
            displayTime = rawTime;
        }

        return new ChatMessage(senderId, receiverId, content, displayTime, false,
                fileUrl, fileName, fileType);
    }

    private void appendMessage(ChatMessage msg) {
        messageList.add(msg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (!messageList.isEmpty())
            recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
    }

    // ── ChatMessage model ─────────────────────────────────────────────────────
    static class ChatMessage {
        final long   senderId, receiverId;
        final String content, displayTime;
        boolean      isRead;
        // file fields — null if this is a text message
        final String fileUrl, fileName, fileType;

        boolean isSystem() { return senderId == -1; }
        boolean isFile()   { return fileUrl != null; }

        // Text message constructor
        ChatMessage(long senderId, long receiverId,
                    String content, String displayTime, boolean isRead) {
            this(senderId, receiverId, content, displayTime, isRead, null, null, null);
        }

        // Full constructor (text or file)
        ChatMessage(long senderId, long receiverId,
                    String content, String displayTime, boolean isRead,
                    String fileUrl, String fileName, String fileType) {
            this.senderId    = senderId;
            this.receiverId  = receiverId;
            this.content     = content;
            this.displayTime = displayTime;
            this.isRead      = isRead;
            this.fileUrl     = fileUrl;
            this.fileName    = fileName;
            this.fileType    = fileType;
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MsgViewHolder> {

        private final List<ChatMessage> messages;
        private final long myUserId;

        ChatAdapter(List<ChatMessage> messages, long myUserId) {
            this.messages = messages;
            this.myUserId = myUserId;
        }

        @Override
        public MsgViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new MsgViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MsgViewHolder h, int position) {
            ChatMessage msg = messages.get(position);

            h.layoutSent.setVisibility(View.GONE);
            h.layoutReceived.setVisibility(View.GONE);
            h.layoutSystem.setVisibility(View.GONE);

            if (msg.isSystem()) {
                h.layoutSystem.setVisibility(View.VISIBLE);
                h.tvSystemMessage.setText(msg.content);

            } else if (msg.senderId == myUserId) {
                h.layoutSent.setVisibility(View.VISIBLE);
                h.tvSentTime.setText(msg.displayTime);

                if (msg.isFile()) {
                    // File bubble
                    h.tvSentMessage.setVisibility(View.GONE);
                    h.cardSentFile.setVisibility(View.VISIBLE);
                    h.tvSentFileName.setText(msg.fileName);
                    h.tvSentFileType.setText(msg.fileType);
                    h.btnSentFileDownload.setOnClickListener(v ->
                            downloadFile(msg.fileUrl, msg.fileName));
                } else {
                    // Text bubble
                    h.tvSentMessage.setVisibility(View.VISIBLE);
                    h.cardSentFile.setVisibility(View.GONE);
                    h.tvSentMessage.setText(msg.content);
                }

                if (msg.isRead) {
                    h.tvReadReceipt.setText("✓✓ Read");
                    h.tvReadReceipt.setTextColor(0xFF4A6E60);
                } else {
                    h.tvReadReceipt.setText("✓");
                    h.tvReadReceipt.setTextColor(0xFF9BB5A7);
                }

            } else {
                h.layoutReceived.setVisibility(View.VISIBLE);
                h.tvReceivedTime.setText(msg.displayTime);
                h.tvSenderName.setVisibility(View.GONE);

                if (msg.isFile()) {
                    // File bubble
                    h.tvReceivedMessage.setVisibility(View.GONE);
                    h.cardReceivedFile.setVisibility(View.VISIBLE);
                    h.tvReceivedFileName.setText(msg.fileName);
                    h.tvReceivedFileType.setText(msg.fileType);
                    h.btnReceivedFileDownload.setOnClickListener(v ->
                            downloadFile(msg.fileUrl, msg.fileName));
                } else {
                    // Text bubble
                    h.tvReceivedMessage.setVisibility(View.VISIBLE);
                    h.cardReceivedFile.setVisibility(View.GONE);
                    h.tvReceivedMessage.setText(msg.content);
                }
            }
        }

        @Override public int getItemCount() { return messages.size(); }

        class MsgViewHolder extends RecyclerView.ViewHolder {
            android.view.View       layoutSent, layoutReceived, layoutSystem;
            android.widget.TextView tvSentMessage, tvSentTime, tvReadReceipt;
            android.widget.TextView tvSentFileName, tvSentFileType, btnSentFileDownload;
            android.view.View       cardSentFile;
            android.widget.TextView tvReceivedMessage, tvReceivedTime, tvSenderName;
            android.widget.TextView tvReceivedFileName, tvReceivedFileType, btnReceivedFileDownload;
            android.view.View       cardReceivedFile;
            android.widget.TextView tvSystemMessage;

            MsgViewHolder(android.view.View v) {
                super(v);
                layoutSent              = v.findViewById(R.id.layoutSent);
                layoutReceived          = v.findViewById(R.id.layoutReceived);
                layoutSystem            = v.findViewById(R.id.layoutSystem);
                tvSentMessage           = v.findViewById(R.id.tvSentMessage);
                tvSentTime              = v.findViewById(R.id.tvSentTime);
                tvReadReceipt           = v.findViewById(R.id.tvReadReceipt);
                cardSentFile            = v.findViewById(R.id.cardSentFile);
                tvSentFileName          = v.findViewById(R.id.tvSentFileName);
                tvSentFileType          = v.findViewById(R.id.tvSentFileType);
                btnSentFileDownload     = v.findViewById(R.id.btnSentFileDownload);
                tvReceivedMessage       = v.findViewById(R.id.tvReceivedMessage);
                tvReceivedTime          = v.findViewById(R.id.tvReceivedTime);
                tvSenderName            = v.findViewById(R.id.tvSenderName);
                cardReceivedFile        = v.findViewById(R.id.cardReceivedFile);
                tvReceivedFileName      = v.findViewById(R.id.tvReceivedFileName);
                tvReceivedFileType      = v.findViewById(R.id.tvReceivedFileType);
                btnReceivedFileDownload = v.findViewById(R.id.btnReceivedFileDownload);
                tvSystemMessage         = v.findViewById(R.id.tvSystemMessage);
            }
        }
    }
}