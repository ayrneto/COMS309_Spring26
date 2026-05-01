package com.example.androidexample;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AIChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private EditText etMessage;
    private ImageButton btnSend;
    private RequestQueue requestQueue;
    private String userId;

    private static final String CRISIS_MARKER = "988 Suicide & Crisis Lifeline";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        chatContainer = findViewById(R.id.chatContainer);
        scrollView    = findViewById(R.id.scrollView);
        etMessage     = findViewById(R.id.etMessage);
        btnSend       = findViewById(R.id.btnSend);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", "1");

        requestQueue = Volley.newRequestQueue(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Clear history button
        findViewById(R.id.btnClearHistory).setOnClickListener(v -> confirmClearHistory());

        // Load past chat history from backend
        loadHistory();

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) return;
            etMessage.setText("");
            addUserMessage(message);
            sendToAI(message);
        });
    }

    // ── Load history ──────────────────────────────────────────────────────────

    private void loadHistory() {
        String url = ApiConstants.BASE_URL + "/api/ai-chat/" + userId + "/history";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (response.length() == 0) {
                        // No history — show greeting
                        addBotMessage("Hi! I'm your Calmify AI assistant. I'm here to help with general wellness questions. How are you feeling today?", false);
                        return;
                    }
                    // Render each past exchange
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject item = response.getJSONObject(i);
                            String userMsg = item.getString("userMessage");
                            String aiReply = item.getString("aiReply");
                            addUserMessage(userMsg);
                            boolean isCrisis = aiReply.contains(CRISIS_MARKER);
                            addBotMessage(aiReply, isCrisis);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> {
                    // If history fails just show greeting
                    addBotMessage("Hi! I'm your Calmify AI assistant. How are you feeling today?", false);
                }
        );

        requestQueue.add(request);
    }

    // ── Send message ──────────────────────────────────────────────────────────

    private void sendToAI(String userMessage) {
        TextView typingIndicator = addBotMessage("...", false);

        String url = ApiConstants.BASE_URL + "/api/ai-chat/" + userId;

        JSONObject body = new JSONObject();
        try {
            body.put("message", userMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    chatContainer.removeView(typingIndicator.getParent() != null
                            ? (View) typingIndicator.getParent() : typingIndicator);
                    try {
                        String reply = response.getString("aiReply");
                        boolean isCrisis = reply.contains(CRISIS_MARKER);
                        addBotMessage(reply, isCrisis);
                    } catch (JSONException e) {
                        addBotMessage("Sorry, I couldn't understand the response.", false);
                    }
                },
                error -> {
                    chatContainer.removeView(typingIndicator.getParent() != null
                            ? (View) typingIndicator.getParent() : typingIndicator);
                    addBotMessage("I'm not available right now. Please try again shortly.", false);
                }
        );

        requestQueue.add(request);
    }

    // ── Clear history ─────────────────────────────────────────────────────────

    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat History")
                .setMessage("Are you sure you want to delete all AI chat history? This cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> clearHistory())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearHistory() {
        String url = ApiConstants.BASE_URL + "/api/ai-chat/" + userId + "/history";

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    chatContainer.removeAllViews();
                    addBotMessage("Chat history cleared. Hi again! How are you feeling today?", false);
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(this, "Failed to clear history", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private TextView addUserMessage(String text) {
        View bubble = getLayoutInflater().inflate(R.layout.item_chat_user, chatContainer, false);
        TextView tv = bubble.findViewById(R.id.tvMessage);
        tv.setText(text);
        chatContainer.addView(bubble);
        scrollToBottom();
        return tv;
    }

    private TextView addBotMessage(String text, boolean isCrisis) {
        View bubble = getLayoutInflater().inflate(R.layout.item_chat_bot, chatContainer, false);
        TextView tv = bubble.findViewById(R.id.tvMessage);
        tv.setText(text);
        if (isCrisis) {
            // Orange background and dark text for crisis messages
            tv.setBackgroundResource(R.drawable.bubble_crisis);
            tv.setTextColor(Color.parseColor("#7A2000"));
        }
        chatContainer.addView(bubble);
        scrollToBottom();
        return tv;
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}