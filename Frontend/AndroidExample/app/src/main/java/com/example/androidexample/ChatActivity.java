package com.example.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ChatActivity — live WebSocket chat between a user and their assigned counselor.
 *
 * How to launch from HomeActivity (user side):
 *   Intent intent = new Intent(this, ChatActivity.class);
 *   intent.putExtra("partnerUserId", counselorUserId);  // long
 *   intent.putExtra("partnerName", "Dr. Smith");
 *   startActivity(intent);
 *
 * How to launch from CounselorHomeActivity:
 *   Intent intent = new Intent(this, ChatActivity.class);
 *   intent.putExtra("partnerUserId", targetUserId);
 *   intent.putExtra("partnerName", userName);
 *   startActivity(intent);
 */
public class ChatActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private TextView tvChatPartnerName;
    private TextView tvOnlineStatus;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<ChatMessage> messageList = new ArrayList<>();
    private ChatAdapter chatAdapter;

    // ── Session ───────────────────────────────────────────────────────────────
    private long myUserId;
    private long partnerUserId;
    private String partnerName = "Chat";

    // ── WebSocket ─────────────────────────────────────────────────────────────
    private WebSocketClient wsClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Time formatter ────────────────────────────────────────────────────────
    private final SimpleDateFormat timeFmt =
            new SimpleDateFormat("h:mm a", Locale.getDefault());

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Read session
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        myUserId = Long.parseLong(prefs.getString("USER_ID", "-1"));

        // Read Intent extras
        partnerUserId = getIntent().getLongExtra("partnerUserId", -1L);
        partnerName   = getIntent().getStringExtra("partnerName");
        if (partnerName == null || partnerName.isEmpty()) partnerName = "Chat";

        // Bind views
        recyclerMessages  = findViewById(R.id.recyclerMessages);
        etMessage         = findViewById(R.id.etMessage);
        tvChatPartnerName = findViewById(R.id.tvChatPartnerName);
        tvOnlineStatus    = findViewById(R.id.tvOnlineStatus);
        ImageButton btnBack = findViewById(R.id.btnBack);

        tvChatPartnerName.setText(partnerName);
        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());

        // Set up RecyclerView
        chatAdapter = new ChatAdapter(messageList, myUserId);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true); // newest messages at the bottom
        recyclerMessages.setLayoutManager(llm);
        recyclerMessages.setAdapter(chatAdapter);

        if (partnerUserId == -1) {
            Toast.makeText(this, "Error: partner ID missing", Toast.LENGTH_LONG).show();
            return;
        }

        // Load history first, then open WebSocket
        fetchHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectWebSocket();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REST — load chat history
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/chat/history?userA={myUserId}&userB={partnerUserId}
     *
     * Expected JSON array:
     * [
     *   { "id": 1, "senderId": 3, "receiverId": 7,
     *     "content": "Hello!", "sentAt": "2026-04-04T14:23:00" },
     *   ...
     * ]
     */
    private void fetchHistory() {
        String url = ApiConstants.chatHistory(myUserId, partnerUserId);

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    messageList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            messageList.add(parseMessage(response.getJSONObject(i)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    scrollToBottom();
                    connectWebSocket(); // connect after history is loaded
                },
                error -> {
                    // History failed — still connect so live chat works
                    Toast.makeText(this, "Could not load history", Toast.LENGTH_SHORT).show();
                    connectWebSocket();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WebSocket
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Connects to: ws://HOST:8080/ws/chat/{senderId}/{receiverId}
     *
     * The server should:
     *   1. Accept the connection and map (senderId, receiverId) → this session.
     *   2. On receiving a text frame, persist the message and forward it to
     *      the receiverId's open session (if any).
     *   3. Echo back the saved message with a "sentAt" timestamp so both sides
     *      have a canonical time.
     *
     * JSON the client sends:
     *   { "senderId": Long, "receiverId": Long, "content": String }
     *
     * JSON the server echoes / forwards:
     *   { "senderId": Long, "receiverId": Long, "content": String, "sentAt": String }
     */
    private void connectWebSocket() {
        String wsUrl = ApiConstants.wsChat(myUserId, partnerUserId);

        try {
            wsClient = new WebSocketClient(URI.create(wsUrl)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
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
                        // Reconnect after 3 s
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
            Toast.makeText(this, "WebSocket error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void disconnectWebSocket() {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sending a message
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
            payload.put("senderId",   myUserId);
            payload.put("receiverId", partnerUserId);
            payload.put("content",    text);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        wsClient.send(payload.toString());

        // Optimistic local append — don't wait for echo
        appendMessage(new ChatMessage(
                myUserId, partnerUserId, text, timeFmt.format(new Date())
        ));

        etMessage.setText("");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Receiving a message
    // ─────────────────────────────────────────────────────────────────────────

    private void handleIncomingMessage(String text) {
        try {
            ChatMessage msg = parseMessage(new JSONObject(text));
            // Skip our own messages — already appended optimistically
            if (msg.senderId == myUserId) return;
            appendMessage(msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ChatMessage parseMessage(JSONObject obj) throws JSONException {
        long   senderId  = obj.getLong("senderId");
        long   receiverId = obj.getLong("receiverId");
        String content   = obj.getString("content");
        String rawTime   = obj.optString("sentAt", "");

        String displayTime;
        try {
            SimpleDateFormat iso =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date d = iso.parse(rawTime);
            displayTime = (d != null) ? timeFmt.format(d) : rawTime;
        } catch (Exception e) {
            displayTime = rawTime;
        }

        return new ChatMessage(senderId, receiverId, content, displayTime);
    }

    private void appendMessage(ChatMessage msg) {
        messageList.add(msg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (!messageList.isEmpty()) {
            recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ChatMessage model
    // ─────────────────────────────────────────────────────────────────────────

    static class ChatMessage {
        final long   senderId;
        final long   receiverId;
        final String content;
        final String displayTime;

        ChatMessage(long senderId, long receiverId, String content, String displayTime) {
            this.senderId    = senderId;
            this.receiverId  = receiverId;
            this.content     = content;
            this.displayTime = displayTime;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RecyclerView Adapter
    // ─────────────────────────────────────────────────────────────────────────

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
                h.layoutSent.setVisibility(android.view.View.VISIBLE);
                h.layoutReceived.setVisibility(android.view.View.GONE);
                h.tvSentMessage.setText(msg.content);
                h.tvSentTime.setText(msg.displayTime);
            } else {
                h.layoutSent.setVisibility(android.view.View.GONE);
                h.layoutReceived.setVisibility(android.view.View.VISIBLE);
                h.tvReceivedMessage.setText(msg.content);
                h.tvReceivedTime.setText(msg.displayTime);
                h.tvSenderName.setVisibility(android.view.View.GONE);
            }
        }

        @Override
        public int getItemCount() { return messages.size(); }

        static class MsgViewHolder extends RecyclerView.ViewHolder {
            android.view.View       layoutSent, layoutReceived;
            android.widget.TextView tvSentMessage, tvSentTime;
            android.widget.TextView tvReceivedMessage, tvReceivedTime, tvSenderName;

            MsgViewHolder(android.view.View v) {
                super(v);
                layoutSent        = v.findViewById(R.id.layoutSent);
                layoutReceived    = v.findViewById(R.id.layoutReceived);
                tvSentMessage     = v.findViewById(R.id.tvSentMessage);
                tvSentTime        = v.findViewById(R.id.tvSentTime);
                tvReceivedMessage = v.findViewById(R.id.tvReceivedMessage);
                tvReceivedTime    = v.findViewById(R.id.tvReceivedTime);
                tvSenderName      = v.findViewById(R.id.tvSenderName);
            }
        }
    }
}