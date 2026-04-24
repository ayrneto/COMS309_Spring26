package com.example.androidexample;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class AIChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private EditText etMessage;
    private ImageButton btnSend;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        chatContainer = findViewById(R.id.chatContainer);
        scrollView    = findViewById(R.id.scrollView);
        etMessage     = findViewById(R.id.etMessage);
        btnSend       = findViewById(R.id.btnSend);

        requestQueue = Volley.newRequestQueue(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        addBotMessage("Hi! I'm your Calmify AI assistant. I'm here to help answer general wellness questions. How are you feeling today?");

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) return;
            etMessage.setText("");
            addUserMessage(message);
            sendToAI(message);
        });
    }

    private void sendToAI(String userMessage) {
        TextView typingIndicator = addBotMessage("...");

        String url = ApiConstants.BASE_URL + "/api/ai/chat";

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
                        String reply = response.getString("reply");
                        addBotMessage(reply);
                    } catch (JSONException e) {
                        addBotMessage("Sorry, I couldn't understand the response.");
                    }
                },
                error -> {
                    chatContainer.removeView(typingIndicator.getParent() != null
                            ? (View) typingIndicator.getParent() : typingIndicator);
                    addBotMessage("I'm not available right now, but I'm being set up! Please check back soon.");
                }
        );

        requestQueue.add(request);
    }

    private TextView addUserMessage(String text) {
        View bubble = getLayoutInflater().inflate(R.layout.item_chat_user, chatContainer, false);
        TextView tv = bubble.findViewById(R.id.tvMessage);
        tv.setText(text);
        chatContainer.addView(bubble);
        scrollToBottom();
        return tv;
    }

    private TextView addBotMessage(String text) {
        View bubble = getLayoutInflater().inflate(R.layout.item_chat_bot, chatContainer, false);
        TextView tv = bubble.findViewById(R.id.tvMessage);
        tv.setText(text);
        chatContainer.addView(bubble);
        scrollToBottom();
        return tv;
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}