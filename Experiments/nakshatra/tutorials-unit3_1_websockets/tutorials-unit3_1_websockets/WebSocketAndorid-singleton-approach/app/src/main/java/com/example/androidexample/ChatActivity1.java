package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.handshake.ServerHandshake;

/**
 * MODIFICATIONS by Nakshatra Gupta:
 * - Added message counter that tracks how many messages have been received
 * - Counter displayed in the title bar
 */
public class ChatActivity1 extends AppCompatActivity implements WebSocketListener {

    private Button sendBtn, backMainBtn;
    private EditText msgEtx;
    private TextView msgTv, counterTv;

    // Message counter
    private int messageCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat1);

        /* initialize UI elements */
        sendBtn = (Button) findViewById(R.id.sendBtn);
        backMainBtn = (Button) findViewById(R.id.backMainBtn);
        msgEtx = (EditText) findViewById(R.id.msgEdt);
        msgTv = (TextView) findViewById(R.id.tx1);
        counterTv = (TextView) findViewById(R.id.counterTv);

        // Initialize counter display
        counterTv.setText("Messages received: 0");

        /* connect this activity to the websocket instance */
        WebSocketManager1.getInstance().setWebSocketListener(ChatActivity1.this);

        /* send button listener */
        sendBtn.setOnClickListener(v -> {
            String message = msgEtx.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(ChatActivity1.this, "Message cannot be empty!", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    WebSocketManager1.getInstance().sendMessage(msgEtx.getText().toString());
                    msgEtx.setText(""); // clear text box
                } catch (Exception e) {
                    Log.d("ExceptionSendMessage:", e.getMessage().toString());
                }
            }
        });

        /* back button listener */
        backMainBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onWebSocketMessage(String message) {
        runOnUiThread(() -> {
            // Increment and display message counter
            messageCount++;
            counterTv.setText("Messages received: " + messageCount);

            // Append message to chat
            String s = msgTv.getText().toString();
            msgTv.setText(s + "\n" + message);
        });
    }

    @Override
    public void onWebSocketClose(int code, String reason, boolean remote) {
        String closedBy = remote ? "server" : "local";
        runOnUiThread(() -> {
            String s = msgTv.getText().toString();
            msgTv.setText(s + "---\nconnection closed by " + closedBy + "\nreason: " + reason);
        });
    }

    @Override
    public void onWebSocketOpen(ServerHandshake handshakedata) {}

    @Override
    public void onWebSocketError(Exception ex) {}
}