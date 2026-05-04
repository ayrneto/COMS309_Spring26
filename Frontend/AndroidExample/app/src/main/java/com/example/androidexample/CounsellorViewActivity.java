package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

public class CounsellorViewActivity extends AppCompatActivity {

    private long   counsellorId;
    private String counsellorName;

    private TextView tvAvatarLarge, tvDisplayName, tvSpecialization, tvBio, tvStatus, tvRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counsellor_view);

        counsellorId   = getIntent().getLongExtra("counsellorId",   -1);
        counsellorName = getIntent().getStringExtra("counsellorName");

        if (counsellorId == -1) {
            Toast.makeText(this, "Counsellor not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvAvatarLarge    = findViewById(R.id.tvAvatarLarge);
        tvDisplayName    = findViewById(R.id.tvDisplayName);
        tvSpecialization = findViewById(R.id.tvSpecialization);
        tvBio            = findViewById(R.id.tvBio);
        tvStatus         = findViewById(R.id.tvStatus);
        tvRating         = findViewById(R.id.tvRating);

        // Set initial letter avatar from name passed via intent
        if (counsellorName != null && !counsellorName.isEmpty()) {
            tvAvatarLarge.setText(String.valueOf(counsellorName.charAt(0)).toUpperCase());
            tvDisplayName.setText(counsellorName);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Chat button
        ((Button) findViewById(R.id.btnChat)).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("partnerUserId", counsellorId);
            intent.putExtra("partnerName",   counsellorName);
            startActivity(intent);
        });

        fetchCounsellorProfile();
    }

    // ── Fetch counsellor profile from backend ─────────────────────────────────

    private void fetchCounsellorProfile() {
        String url = ApiConstants.counselorProfile(counsellorId);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    String displayName    = response.optString("displayName",    counsellorName);
                    String specialization = response.optString("specialization", "General Wellness");
                    String bio            = response.optString("bio",            "No bio available.");
                    String status         = response.optString("status",         "AVAILABLE");
                    double rating         = response.optDouble("averageRating",  0.0);

                    if (!displayName.isEmpty()) {
                        tvDisplayName.setText(displayName);
                        tvAvatarLarge.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
                        counsellorName = displayName;
                    }

                    tvSpecialization.setText(specialization);
                    tvBio.setText(bio);
                    tvStatus.setText(status);

                    String ratingText = rating > 0
                            ? String.format("★ %.1f", rating)
                            : "No ratings yet";
                    tvRating.setText(ratingText);

                    // Update chat button label with real name
                    ((Button) findViewById(R.id.btnChat))
                            .setText("💬  Chat with " + counsellorName);
                },
                error -> {
                    // Profile fetch failed — name from intent is still shown, chat still works
                    tvSpecialization.setText("—");
                    tvBio.setText("Could not load profile details.");
                    tvStatus.setText("—");
                    tvRating.setText("—");
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }
}