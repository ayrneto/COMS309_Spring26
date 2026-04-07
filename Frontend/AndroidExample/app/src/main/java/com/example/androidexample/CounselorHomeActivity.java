package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class CounselorHomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counsellor_home);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID",    "");
        String email  = prefs.getString("USER_EMAIL", "");
        String name   = prefs.getString("USER_NAME",  "");

        // Counselor-specific cached profile fields
        String displayName    = prefs.getString("COUNSELOR_DISPLAY_NAME", name);
        String specialization = prefs.getString("COUNSELOR_SPECIALIZATION", "");
        String bio            = prefs.getString("COUNSELOR_BIO", "");
        String profilePicUrl  = prefs.getString("COUNSELOR_PROFILE_PIC", "");
        String status         = prefs.getString("COUNSELOR_STATUS", "AVAILABLE");

        drawerLayout = findViewById(R.id.drawerLayout);

        // Welcome text
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Welcome,\n" + (displayName.isEmpty() ? "Counsellor" : displayName));

        // Drawer header
        ((TextView) findViewById(R.id.drawerName)).setText(
                displayName.isEmpty() ? "Hello!" : "Hi, " + displayName);
        ((TextView) findViewById(R.id.drawerEmail)).setText(email);

        // Hamburger
        ((ImageButton) findViewById(R.id.btnHamburger))
                .setOnClickListener(v -> drawerLayout.open());

        // ── Chat with User ────────────────────────────────────────────────────
        findViewById(R.id.drawerItemChat).setOnClickListener(v -> {
            drawerLayout.close();
            // TODO: replace with real assigned user ID once Shrey's endpoint is ready
            long assignedUserId = prefs.getLong("ASSIGNED_USER_ID", -1L);
            if (assignedUserId == -1L) {
                Toast.makeText(this, "No user assigned yet", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("partnerUserId", assignedUserId);
            intent.putExtra("partnerName",
                    prefs.getString("ASSIGNED_USER_NAME", "User"));
            startActivity(intent);
        });

        // ── Edit Profile ──────────────────────────────────────────────────────
        findViewById(R.id.drawerItemEditProfile).setOnClickListener(v -> {
            drawerLayout.close();
            Intent intent = new Intent(this, CounselorEditProfileActivity.class);
            intent.putExtra("userId",          Long.parseLong(userId.isEmpty() ? "-1" : userId));
            intent.putExtra("displayName",     displayName);
            intent.putExtra("specialization",  specialization);
            intent.putExtra("bio",             bio);
            intent.putExtra("profilePictureUrl", profilePicUrl);
            intent.putExtra("status",          status);
            startActivity(intent);
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
    }
}