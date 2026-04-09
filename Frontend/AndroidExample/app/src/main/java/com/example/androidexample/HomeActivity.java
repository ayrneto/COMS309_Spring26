package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class HomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID",    "");
        String email  = prefs.getString("USER_EMAIL", "");
        String name   = prefs.getString("USER_NAME",  "");

        drawerLayout = findViewById(R.id.drawerLayout);

        // Welcome text
        ((TextView) findViewById(R.id.tvWelcome))
                .setText("Welcome" + (name.isEmpty() ? "" : ",\n" + name));

        // Drawer header
        ((TextView) findViewById(R.id.drawerName))
                .setText(name.isEmpty() ? "Hello!" : "Hi, " + name);
        ((TextView) findViewById(R.id.drawerEmail)).setText(email);

        // Hamburger
        ((ImageButton) findViewById(R.id.btnHamburger))
                .setOnClickListener(v -> drawerLayout.open());

        // ── Find a Counselor ──────────────────────────────────────────────────
        findViewById(R.id.drawerItemFindCounsellor).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, CounsellorSearchActivity.class));
        });

        // ── Worry Notes ───────────────────────────────────────────────────────
        findViewById(R.id.drawerItemWorryNotes).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, WorryNotes.class));
        });

        // ── Chat with Counselor → shows list of accepted counselors ───────────
        findViewById(R.id.drawerItemChat).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, ChatListActivity.class));
        });

        // ── Edit Profile ──────────────────────────────────────────────────────
        findViewById(R.id.drawerItemEditProfile).setOnClickListener(v -> {
            drawerLayout.close();
            Intent intent = new Intent(this, EditProfile.class);
            intent.putExtra("userId",   userId);
            intent.putExtra("email",    email);
            intent.putExtra("name",     name);
            intent.putExtra("password", "");
            startActivity(intent);
        });

        String role = prefs.getString("USER_ROLE", "");

    // Show/hide menu items based on role
            if (role.equals("COUNSELLOR")) {
                findViewById(R.id.drawerItemAssignTask).setVisibility(View.VISIBLE);
                findViewById(R.id.drawerItemMyTasks).setVisibility(View.GONE);
            } else {
                findViewById(R.id.drawerItemMyTasks).setVisibility(View.VISIBLE);
                findViewById(R.id.drawerItemAssignTask).setVisibility(View.GONE);
            }

    // My Tasks click listener
            findViewById(R.id.drawerItemMyTasks).setOnClickListener(v -> {
                drawerLayout.close();
                startActivity(new Intent(this, TasksOverview.class));
            });

    // Assign Task click listener
            findViewById(R.id.drawerItemAssignTask).setOnClickListener(v -> {
                drawerLayout.close();
                startActivity(new Intent(this, AssignTaskActivity.class));
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