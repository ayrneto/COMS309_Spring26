package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CounselorHomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counsellor_home);

        tvWelcome  = findViewById(R.id.tvWelcome);
        btnProfile = findViewById(R.id.btn_profile);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String email = prefs.getString("USER_EMAIL", "");

        if (email == null) email = "";
        tvWelcome.setText(!email.isEmpty() ? "Welcome, Counsellor\n" + email : "Welcome, Counsellor");

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(CounselorHomeActivity.this, CounselorProfileActivity.class))
        );
        // userId is read from SharedPreferences inside CounselorProfileActivity directly
    }
}