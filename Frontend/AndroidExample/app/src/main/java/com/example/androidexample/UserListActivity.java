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

public class UserListActivity extends AppCompatActivity {

    private RecyclerView recyclerUsers;
    private TextView tvEmpty;
    private final List<UserRow> userList = new ArrayList<>();
    private UserRowAdapter adapter;
    private long myUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        myUserId = Long.parseLong(prefs.getString("USER_ID", "-1"));

        recyclerUsers = findViewById(R.id.recyclerUsers);
        tvEmpty       = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new UserRowAdapter(userList, user -> {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("TARGET_USER_ID",    user.userId);
            intent.putExtra("TARGET_USER_NAME",  user.name);
            intent.putExtra("TARGET_USER_EMAIL", user.email);
            intent.putExtra("APPOINTMENT_DATE",  user.appointmentDate);
            startActivity(intent);
        });

        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setAdapter(adapter);

        fetchUsers();
    }

    // ── Fetch accepted users via appointments endpoint ────────────────────────

    private void fetchUsers() {
        String url = ApiConstants.BASE_URL +
                "/api/appointments/counsellor/" + myUserId + "/accepted";

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    userList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            UserRow row = new UserRow();
                            row.userId          = obj.optLong("userId", -1);
                            row.name            = obj.optString("userName", "Unknown User");
                            row.email           = obj.optString("userEmail", "");
                            row.appointmentDate = obj.optString("date", "") + " " +
                                    obj.optString("timeSlot", "");
                            if (row.userId != -1) userList.add(row);
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    // Deduplicate by userId (multiple appointments → same user once)
                    List<UserRow> deduped = new ArrayList<>();
                    List<Long> seen = new ArrayList<>();
                    for (UserRow r : userList) {
                        if (!seen.contains(r.userId)) {
                            seen.add(r.userId);
                            deduped.add(r);
                        }
                    }
                    userList.clear();
                    userList.addAll(deduped);

                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerUsers.setVisibility(userList.isEmpty() ? View.GONE : View.VISIBLE);
                },
                error -> {
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                    tvEmpty.setVisibility(View.VISIBLE);
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ── Model ─────────────────────────────────────────────────────────────────
    static class UserRow {
        long   userId;
        String name, email, appointmentDate;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    interface OnUserClick { void onClick(UserRow u); }

    static class UserRowAdapter extends RecyclerView.Adapter<UserRowAdapter.VH> {
        private final List<UserRow> list;
        private final OnUserClick cb;

        UserRowAdapter(List<UserRow> list, OnUserClick cb) {
            this.list = list;
            this.cb   = cb;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            UserRow u = list.get(pos);
            h.tvName.setText(u.name);
            h.tvSubtitle.setText(u.email.isEmpty() ? "Tap to view profile" : u.email);
            String initial = u.name.isEmpty() ? "?" :
                    String.valueOf(u.name.charAt(0)).toUpperCase();
            h.tvAvatar.setText(initial);
            h.itemView.setOnClickListener(v -> cb.onClick(u));
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