package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatListActivity
 *
 * Used by BOTH user and counsellor sides.
 *
 * USER side:
 *   Shows all counsellors the user has an ACCEPTED appointment with.
 *   Endpoint: GET /api/appointments/user/{userId}/accepted
 *   Response: [{ "partnerId": 7, "partnerName": "Dr. Sarah Jones",
 *                "partnerSubtitle": "Anxiety & Depression" }]
 *
 * COUNSELLOR side:
 *   Shows all users who have an ACCEPTED appointment with this counsellor.
 *   Endpoint: GET /api/appointments/counsellor/{counsellorId}/accepted
 *   Response: [{ "partnerId": 3, "partnerName": "Test User 1",
 *                "partnerSubtitle": "Appointment on 2026-04-10" }]
 *
 * Launch with:
 *   Intent intent = new Intent(this, ChatListActivity.class);
 *   startActivity(intent);
 *   // Role is read from SharedPreferences automatically
 */
public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerChatList;
    private TextView tvEmpty, tvTitle;

    private final List<ChatPerson> personList = new ArrayList<>();
    private ChatPersonAdapter adapter;

    private long   myUserId;
    private String myRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        myUserId = Long.parseLong(prefs.getString("USER_ID", "-1"));
        myRole   = prefs.getString("USER_ROLE", "USER");

        recyclerChatList = findViewById(R.id.recyclerChatList);
        tvEmpty          = findViewById(R.id.tvChatListEmpty);
        tvTitle          = findViewById(R.id.tvChatListTitle);

        // Set title based on role
        if ("COUNSELLOR".equals(myRole)) {
            tvTitle.setText("Chats with Users");
        } else {
            tvTitle.setText("Chats with Counselors");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new ChatPersonAdapter(personList, person -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("partnerUserId", person.partnerId);
            intent.putExtra("partnerName",   person.partnerName);
            startActivity(intent);
        });
        recyclerChatList.setLayoutManager(new LinearLayoutManager(this));
        recyclerChatList.setAdapter(adapter);

        fetchChatPartners();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fetch accepted appointment partners
    // ─────────────────────────────────────────────────────────────────────────
    private void fetchChatPartners() {
        String url;
        if ("COUNSELLOR".equals(myRole)) {
            // GET /api/appointments/counsellor/{counsellorId}/accepted
            url = ApiConstants.BASE_URL +
                    "/api/appointments/counsellor/" + myUserId + "/accepted";
        } else {
            // GET /api/appointments/user/{userId}/accepted
            url = ApiConstants.BASE_URL +
                    "/api/appointments/user/" + myUserId + "/accepted";
        }

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    personList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            ChatPerson p = new ChatPerson();
                            p.partnerId    = obj.getLong("partnerId");
                            p.partnerName  = obj.optString("partnerName",    "");
                            p.subtitle     = obj.optString("partnerSubtitle", "");
                            personList.add(p);
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                },
                error -> {
                    Toast.makeText(this, "Failed to load chats", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void updateEmptyState() {
        boolean empty = personList.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerChatList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ── Model ─────────────────────────────────────────────────────────────────
    static class ChatPerson {
        long   partnerId;
        String partnerName, subtitle;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    interface OnPersonClick { void onClick(ChatPerson p); }

    static class ChatPersonAdapter
            extends RecyclerView.Adapter<ChatPersonAdapter.VH> {

        private final List<ChatPerson> list;
        private final OnPersonClick cb;

        ChatPersonAdapter(List<ChatPerson> list, OnPersonClick cb) {
            this.list = list;
            this.cb   = cb;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_person_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            ChatPerson p = list.get(pos);

            h.tvName.setText(p.partnerName);
            h.tvSubtitle.setText(p.subtitle);

            // Avatar — first letter of name
            String initial = p.partnerName.isEmpty() ? "?" :
                    String.valueOf(p.partnerName.charAt(0)).toUpperCase();
            h.tvAvatar.setText(initial);

            h.itemView.setOnClickListener(v -> cb.onClick(p));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvName, tvSubtitle;

            VH(View v) {
                super(v);
                tvAvatar   = v.findViewById(R.id.tvAvatar);
                tvName     = v.findViewById(R.id.tvPersonName);
                tvSubtitle = v.findViewById(R.id.tvPersonSubtitle);
            }
        }
    }
}