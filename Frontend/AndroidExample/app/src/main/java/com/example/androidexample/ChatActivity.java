package com.example.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "CHAT_DEBUG";

    // Slash commands supported by Shrey's backend
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
    private final Runnable hideTypingRunnable    = () ->
            tvTypingIndicator.setVisibility(View.INVISIBLE);
    private final Runnable sendTypingStartRunnable = this::sendTypingStart;
    private final Runnable sendTypingStopRunnable  = this::sendTypingStop;

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

        // RecyclerView
        chatAdapter = new ChatAdapter(messageList, myUserId);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(llm);
        recyclerMessages.setAdapter(chatAdapter);

        // Typing + slash command autocomplete
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String input = s.toString();

                // Slash command autocomplete
                if (input.startsWith("/")) {
                    List<String> matches = new ArrayList<>();
                    for (String cmd : COMMANDS) {
                        if (cmd.startsWith(input)) matches.add(cmd);
                    }
                    if (!matches.isEmpty()) {
                        showCommandSuggestions(matches);
                    } else {
                        hideCommandSuggestions();
                    }
                } else {
                    hideCommandSuggestions();
                }

                // Typing indicator
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
    // Slash command autocomplete
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
    // Send message (regular or slash command — same JSON either way)
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
        Log.d(TAG, "Sent: " + payload);

        hideCommandSuggestions();
        sendTypingStop();
        mainHandler.removeCallbacks(sendTypingStartRunnable);
        mainHandler.removeCallbacks(sendTypingStopRunnable);

        // Only append to local view for non-command messages
        // Commands get a system response back instead
        if (!text.startsWith("/")) {
            appendMessage(new ChatMessage(myUserId, partnerUserId, text,
                    timeFmt.format(new Date()), false));
        }

        etMessage.setText("");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Typing events
    // ─────────────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    // Read receipt
    // ─────────────────────────────────────────────────────────────────────────
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

            // No "type" field = real chat message
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
                    // Show as grey info bubble — /help, /ping, /status responses
                    String sysText = obj.optString("text", "");
                    appendSystemMessage(sysText);
                    break;

                case "command":
                    if ("clear".equals(obj.optString("action"))) {
                        // /clear — wipe local view only, DB untouched
                        messageList.clear();
                        chatAdapter.notifyDataSetChanged();
                    }
                    break;

                case "error":
                    String errText = obj.optString("text", "Unknown error");
                    Log.e(TAG, "Server error: " + errText);
                    Toast.makeText(this, errText, Toast.LENGTH_SHORT).show();
                    break;

                default:
                    Log.d(TAG, "Unknown type: " + type);
                    break;
            }

        } catch (JSONException e) {
            Log.e(TAG, "Parse error: " + e.getMessage() + " raw: " + text);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Append a system info bubble (grey, centered)
    // Used for /help, /ping, /status responses
    // ─────────────────────────────────────────────────────────────────────────
    private void appendSystemMessage(String text) {
        // null senderId/receiverId signals a system message to the adapter
        messageList.add(new ChatMessage(-1, -1, text, "", false));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parse a real chat message
    // ─────────────────────────────────────────────────────────────────────────
    private ChatMessage parseMessage(JSONObject obj) throws JSONException {
        long   senderId   = obj.getLong("senderId");
        long   receiverId = obj.getLong("receiverId");
        String content    = obj.getString("content");
        String rawTime    = obj.optString("sentAt", "");

        String displayTime;
        try {
            if (rawTime.startsWith("[")) {
                displayTime = ""; // Jackson array format before @JsonFormat
            } else {
                SimpleDateFormat iso =
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date d = iso.parse(rawTime);
                displayTime = (d != null) ? timeFmt.format(d) : rawTime;
            }
        } catch (Exception e) {
            displayTime = rawTime;
        }

        return new ChatMessage(senderId, receiverId, content, displayTime, false);
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

        // senderId == -1 signals a system message
        boolean isSystem() { return senderId == -1; }

        ChatMessage(long senderId, long receiverId,
                    String content, String displayTime, boolean isRead) {
            this.senderId    = senderId;
            this.receiverId  = receiverId;
            this.content     = content;
            this.displayTime = displayTime;
            this.isRead      = isRead;
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MsgViewHolder> {

        private static final int VIEW_SENT     = 0;
        private static final int VIEW_RECEIVED = 1;
        private static final int VIEW_SYSTEM   = 2;

        private final List<ChatMessage> messages;
        private final long myUserId;

        ChatAdapter(List<ChatMessage> messages, long myUserId) {
            this.messages = messages;
            this.myUserId = myUserId;
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage msg = messages.get(position);
            if (msg.isSystem())             return VIEW_SYSTEM;
            if (msg.senderId == myUserId)   return VIEW_SENT;
            return VIEW_RECEIVED;
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

            // Hide all first
            h.layoutSent.setVisibility(View.GONE);
            h.layoutReceived.setVisibility(View.GONE);
            h.layoutSystem.setVisibility(View.GONE);

            if (msg.isSystem()) {
                h.layoutSystem.setVisibility(View.VISIBLE);
                h.tvSystemMessage.setText(msg.content);

            } else if (msg.senderId == myUserId) {
                h.layoutSent.setVisibility(View.VISIBLE);
                h.tvSentMessage.setText(msg.content);
                h.tvSentTime.setText(msg.displayTime);
                if (msg.isRead) {
                    h.tvReadReceipt.setText("✓✓ Read");
                    h.tvReadReceipt.setTextColor(0xFF4A6E60);
                } else {
                    h.tvReadReceipt.setText("✓");
                    h.tvReadReceipt.setTextColor(0xFF9BB5A7);
                }

            } else {
                h.layoutReceived.setVisibility(View.VISIBLE);
                h.tvReceivedMessage.setText(msg.content);
                h.tvReceivedTime.setText(msg.displayTime);
                h.tvSenderName.setVisibility(View.GONE);
            }
        }

        @Override public int getItemCount() { return messages.size(); }

        static class MsgViewHolder extends RecyclerView.ViewHolder {
            android.view.View       layoutSent, layoutReceived, layoutSystem;
            android.widget.TextView tvSentMessage, tvSentTime, tvReadReceipt;
            android.widget.TextView tvReceivedMessage, tvReceivedTime, tvSenderName;
            android.widget.TextView tvSystemMessage;

            MsgViewHolder(android.view.View v) {
                super(v);
                layoutSent        = v.findViewById(R.id.layoutSent);
                layoutReceived    = v.findViewById(R.id.layoutReceived);
                layoutSystem      = v.findViewById(R.id.layoutSystem);
                tvSentMessage     = v.findViewById(R.id.tvSentMessage);
                tvSentTime        = v.findViewById(R.id.tvSentTime);
                tvReadReceipt     = v.findViewById(R.id.tvReadReceipt);
                tvReceivedMessage = v.findViewById(R.id.tvReceivedMessage);
                tvReceivedTime    = v.findViewById(R.id.tvReceivedTime);
                tvSenderName      = v.findViewById(R.id.tvSenderName);
                tvSystemMessage   = v.findViewById(R.id.tvSystemMessage);
            }
        }
    }
}