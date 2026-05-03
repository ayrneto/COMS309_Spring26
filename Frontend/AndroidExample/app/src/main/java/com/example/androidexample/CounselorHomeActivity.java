package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONObject;

public class CounselorHomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView tvAppointmentBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counsellor_home);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId         = prefs.getString("USER_ID",    "");
        String email          = prefs.getString("USER_EMAIL", "");
        String displayName    = prefs.getString("COUNSELOR_DISPLAY_NAME", "");
        String specialization = prefs.getString("COUNSELOR_SPECIALIZATION", "");
        String bio            = prefs.getString("COUNSELOR_BIO", "");
        String profilePicUrl  = prefs.getString("COUNSELOR_PROFILE_PIC", "");
        String status         = prefs.getString("COUNSELOR_STATUS", "AVAILABLE");

        drawerLayout       = findViewById(R.id.drawerLayout);
        tvAppointmentBadge = findViewById(R.id.tvAppointmentBadge);

        // Welcome text
        String name = displayName.isEmpty() ? "Counsellor" : displayName;
        ((TextView) findViewById(R.id.tvWelcome)).setText("Welcome,\n" + name);

        // Drawer header
        ((TextView) findViewById(R.id.drawerName))
                .setText(displayName.isEmpty() ? "Hello!" : "Hi, " + displayName);
        ((TextView) findViewById(R.id.drawerEmail)).setText(email);

        // Hamburger
        ((ImageButton) findViewById(R.id.btnHamburger))
                .setOnClickListener(v -> drawerLayout.open());

        // ── Appointment Requests ──────────────────────────────────────────────
        findViewById(R.id.drawerItemAppointments).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, AppointmentRequestsActivity.class));
        });

        // ── Chat with User → shows list of accepted users ─────────────────────
        findViewById(R.id.drawerItemChat).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, ChatListActivity.class));
        });

        // ── Assign Task ───────────────────────────────────────────────────────
        findViewById(R.id.drawerItemAssignTask).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, AssignTaskActivity.class));
        });

        // ── Edit Profile ──────────────────────────────────────────────────────
        findViewById(R.id.drawerItemEditProfile).setOnClickListener(v -> {
            drawerLayout.close();
            Intent intent = new Intent(this, CounselorEditProfileActivity.class);
            intent.putExtra("userId",           Long.parseLong(userId.isEmpty() ? "-1" : userId));
            intent.putExtra("displayName",      displayName);
            intent.putExtra("specialization",   specialization);
            intent.putExtra("bio",              bio);
            intent.putExtra("profilePictureUrl", profilePicUrl);
            intent.putExtra("status",           status);
            startActivity(intent);
        });

        // Shared WorryNotes
        findViewById(R.id.drawerItemSharedNotes).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, SharedNotesActivity.class));
        });

        // ── Log Out ───────────────────────────────────────────────────────────
        findViewById(R.id.drawerItemLogout).setOnClickListener(v -> {
            drawerLayout.close();
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        });

        // Load pending request count for badge
        fetchPendingCount(userId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        fetchPendingCount(prefs.getString("USER_ID", ""));
    }

    private void fetchPendingCount(String counselorId) {
        if (counselorId.isEmpty()) return;

        String url = ApiConstants.BASE_URL +
                "/api/appointments/counsellor/" + counselorId;

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    int pendingCount = 0;
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            if ("PENDING".equalsIgnoreCase(obj.optString("status"))) {
                                pendingCount++;
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    if (pendingCount > 0) {
                        tvAppointmentBadge.setText(String.valueOf(pendingCount));
                        tvAppointmentBadge.setVisibility(android.view.View.VISIBLE);
                    } else {
                        tvAppointmentBadge.setVisibility(android.view.View.GONE);
                    }
                },
                error -> tvAppointmentBadge.setVisibility(android.view.View.GONE)
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }
}