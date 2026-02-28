package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnProfile = findViewById(R.id.btn_profile);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String email = prefs.getString("USER_EMAIL", "");

        if (email == null) email = "";
        if (!email.isEmpty()) {
            tvWelcome.setText("Welcome\n" + email);
        } else {
            tvWelcome.setText("Welcome");
        }

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
        });
    }
}