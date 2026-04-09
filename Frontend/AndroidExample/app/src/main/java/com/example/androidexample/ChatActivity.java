package com.example.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView recyclerMessages;
    private EditText     etMessage;
    private TextView     tvChatPartnerName;
    private TextView     tvOnlineStatus;
    private TextView     tvAvailabilityStatus;
    private TextView     tvTypingIndicator;

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

    // ── Typing indicator ──────────────────────────────────────────────────────
    private final Runnable hideTypingRunnable  = () ->
            tvTypingIndicator.setVisibility(View.INVISIBLE);
    private final Runnable sendTypingRunnable  = this::sendTypingEvent;

    // ── Time formatter ────────────────────────────────────────────────────────
    private final SimpleDateFormat timeFmt =
            new SimpleDateFormat("h:mm a", Locale.getDefault());

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        myUserId = Long.parseLong(prefs.getString("USER_ID",   "-1"));
        myRole   = prefs.getString("USER_ROLE", "USER");

        partnerUserId = getIntent().getLongExtra("partnerUserId", -1L);
        partnerName   = getIntent().getStringExtra("partnerName");
        if (partnerName == null || partnerName.isEmpty()) partnerName = "Chat";

        // Bind views
        recyclerMessages     = findViewById(R.id.recyclerMessages);
        etMessage            = findViewById(R.id.etMessage);
        tvChatPartnerName    = findViewById(R.id.tvChatPartnerName);
        tvOnlineStatus       = findViewById(R.id.tvOnlineStatus);
        tvAvailabilityStatus = findViewById(R.id.tvAvailabilityStatus);
        tvTypingIndicator    = findViewById(R.id.tvTypingIndicator);
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

        // Typing detection
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                mainHandler.removeCallbacks(sendTypingRunnable);
                if (s.length() > 0) mainHandler.postDelayed(sendTypingRunnable, 500);
            }
        });

        if (partnerUserId == -1) {
            Toast.makeText(this, "Error: partner ID missing", Toast.LENGTH_LONG).show();
            return;
        }

        // Fetch counsellor availability only on the USER side
        if ("USER".equals(myRole)) fetchAvailability();

        fetchHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacks(hideTypingRunnable);
        mainHandler.removeCallbacks(sendTypingRunnable);
        disconnectWebSocket();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fetch counsellor availability status
    // GET /api/counsellors/{userId}/profile
    // ─────────────────────────────────────────────────────────────────────────
    private void fetchAvailability() {
        String url = ApiConstants.counselorProfile(partnerUserId);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    String status = response.optString("status", "");
                    switch (status.toUpperCase()) {
                        case "AVAILABLE":
                            tvAvailabilityStatus.setText("● Available");
                            tvAvailabilityStatus.setTextColor(0xFF81C784); // light green
                            break;
                        case "BUSY":
                            tvAvailabilityStatus.setText("● Busy");
                            tvAvailabilityStatus.setTextColor(0xFFFFB74D); // amber
                            break;
                        case "OFFLINE":
                            tvAvailabilityStatus.setText("● Offline");
                            tvAvailabilityStatus.setTextColor(0xFFB0BEC5); // grey
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
    // REST — load chat history
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
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    chatAdapter.notifyDataSetChanged();
                    scrollToBottom();
                    connectWebSocket();
                },
                error -> {
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
                @Override
                public void onOpen(ServerHandshake h) {
                    mainHandler.post(() -> tvOnlineStatus.setText("● Connected"));
                }

                @Override
                public void onMessage(String text) {
                    mainHandler.post(() -> handleIncomingMessage(text));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    mainHandler.post(() -> {
                        tvOnlineStatus.setText("○ Disconnected");
                        mainHandler.postDelayed(() -> {
                            if (!isDestroyed()) connectWebSocket();
                        }, 3000);
                    });
                }

                @Override
                public void onError(Exception ex) {
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
    // Send message
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

        // Optimistic local append — starts as "sent" (✓)
        ChatMessage msg = new ChatMessage(myUserId, partnerUserId, text,
                timeFmt.format(new Date()), false);
        appendMessage(msg);
        etMessage.setText("");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send typing event
    // { "type": "typing", "senderId": Long, "receiverId": Long }
    // Shrey: forward to receiver only, do NOT persist
    // ─────────────────────────────────────────────────────────────────────────
    private void sendTypingEvent() {
        if (wsClient == null || !wsClient.isOpen()) return;
        JSONObject payload = new JSONObject();
        try {
            payload.put("type",       "typing");
            payload.put("senderId",   myUserId);
            payload.put("receiverId", partnerUserId);
        } catch (JSONException e) { e.printStackTrace(); }
        wsClient.send(payload.toString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send read receipt when a message is received
    // { "type": "read", "senderId": Long, "receiverId": Long }
    // Shrey: forward to receiver only, do NOT persist
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
        try {
            JSONObject obj  = new JSONObject(text);
            String     type = obj.optString("type", "message");

            if ("typing".equals(type)) {
                tvTypingIndicator.setText(partnerName + " is typing…");
                tvTypingIndicator.setVisibility(View.VISIBLE);
                mainHandler.removeCallbacks(hideTypingRunnable);
                mainHandler.postDelayed(hideTypingRunnable, 3000);
                return;
            }

            if ("read".equals(type)) {
                // Mark the last sent message as read
                for (int i = messageList.size() - 1; i >= 0; i--) {
                    if (messageList.get(i).senderId == myUserId) {
                        messageList.get(i).isRead = true;
                        chatAdapter.notifyItemChanged(i);
                        break;
                    }
                }
                return;
            }

            // Regular message
            ChatMessage msg = parseMessage(obj);
            if (msg.senderId == myUserId) return;

            // Hide typing indicator
            tvTypingIndicator.setVisibility(View.INVISIBLE);
            mainHandler.removeCallbacks(hideTypingRunnable);

            appendMessage(msg);

            // Send read receipt back to the sender
            sendReadReceipt();

        } catch (JSONException e) { e.printStackTrace(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private ChatMessage parseMessage(JSONObject obj) throws JSONException {
        long   senderId   = obj.getLong("senderId");
        long   receiverId = obj.getLong("receiverId");
        String content    = obj.getString("content");
        String rawTime    = obj.optString("sentAt", "");

        String displayTime;
        try {
            SimpleDateFormat iso =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = iso.parse(rawTime);
            displayTime = (d != null) ? timeFmt.format(d) : rawTime;
        } catch (Exception e) { displayTime = rawTime; }

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
        boolean      isRead; // true = partner has read this message

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
            boolean isMine  = (msg.senderId == myUserId);

            if (isMine) {
                h.layoutSent.setVisibility(View.VISIBLE);
                h.layoutReceived.setVisibility(View.GONE);
                h.tvSentMessage.setText(msg.content);
                h.tvSentTime.setText(msg.displayTime);

                // Read receipt: ✓✓ Read (green) or ✓ Sent (grey)
                if (msg.isRead) {
                    h.tvReadReceipt.setText("✓✓ Read");
                    h.tvReadReceipt.setTextColor(0xFF4A6E60);
                } else {
                    h.tvReadReceipt.setText("✓");
                    h.tvReadReceipt.setTextColor(0xFF9BB5A7);
                }
            } else {
                h.layoutSent.setVisibility(View.GONE);
                h.layoutReceived.setVisibility(View.VISIBLE);
                h.tvReceivedMessage.setText(msg.content);
                h.tvReceivedTime.setText(msg.displayTime);
                h.tvSenderName.setVisibility(View.GONE);
            }
        }

        @Override public int getItemCount() { return messages.size(); }

        static class MsgViewHolder extends RecyclerView.ViewHolder {
            android.view.View       layoutSent, layoutReceived;
            android.widget.TextView tvSentMessage, tvSentTime, tvReadReceipt;
            android.widget.TextView tvReceivedMessage, tvReceivedTime, tvSenderName;

            MsgViewHolder(android.view.View v) {
                super(v);
                layoutSent        = v.findViewById(R.id.layoutSent);
                layoutReceived    = v.findViewById(R.id.layoutReceived);
                tvSentMessage     = v.findViewById(R.id.tvSentMessage);
                tvSentTime        = v.findViewById(R.id.tvSentTime);
                tvReadReceipt     = v.findViewById(R.id.tvReadReceipt);
                tvReceivedMessage = v.findViewById(R.id.tvReceivedMessage);
                tvReceivedTime    = v.findViewById(R.id.tvReceivedTime);
                tvSenderName      = v.findViewById(R.id.tvSenderName);
            }
        }
    }
}