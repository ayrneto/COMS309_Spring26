package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

public class CounselorProfileActivity extends AppCompatActivity {

    private TextView tvEmail, tvDisplayName, tvSpecialization, tvBio, tvStatus, tvRating;
    private Button btnEditProfile, btnBack, btnDelete;

    private String userId = "";
    private String email = "";
    private String savedName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counselor_profile);

        tvEmail          = findViewById(R.id.tvEmail);
        tvDisplayName    = findViewById(R.id.tvDisplayName);
        tvSpecialization = findViewById(R.id.tvSpecialization);
        tvBio            = findViewById(R.id.tvBio);
        tvStatus         = findViewById(R.id.tvStatus);
        tvRating         = findViewById(R.id.tvRating);
        btnEditProfile   = findViewById(R.id.btnEditProfile);
        btnBack          = findViewById(R.id.btn_back_home);
        btnDelete        = findViewById(R.id.btn_delete_profile);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        email     = prefs.getString("USER_EMAIL", "");
        userId    = prefs.getString("USER_ID", "");
        savedName = prefs.getString("USER_NAME", "");

        tvEmail.setText(email == null || email.isEmpty() ? "Email: (not found)" : "Email: " + email);

        fetchProfile();

        btnBack.setOnClickListener(v -> finish());

        btnDelete.setOnClickListener(v -> {
            if (userId == null || userId.trim().isEmpty()) {
                Toast.makeText(this, "User ID missing.", Toast.LENGTH_LONG).show();
                return;
            }

            new AlertDialog.Builder(CounselorProfileActivity.this)
                    .setTitle("Delete Profile?")
                    .setMessage("Are you sure you want to delete your account? This cannot be undone.")
                    .setPositiveButton("Yes, Delete", (dialog, which) -> deleteUser(userId))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(CounselorProfileActivity.this, CounselorEditProfileActivity.class);
            intent.putExtra("userId",          Long.parseLong(userId));
            intent.putExtra("displayName",       tvDisplayName.getText().toString().replace("Name: ", ""));
            intent.putExtra("specialization",    tvSpecialization.getText().toString().replace("Specialization: ", ""));
            intent.putExtra("bio",               tvBio.getText().toString().replace("Bio: ", ""));
            intent.putExtra("profilePictureUrl", "");
            intent.putExtra("status",            tvStatus.getText().toString().replace("Status: ", ""));
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!userId.isEmpty()) fetchProfile();
    }

    private void fetchProfile() {
        // Pre-fill from SharedPreferences immediately
        SharedPreferences prefs     = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String cachedName           = prefs.getString("COUNSELOR_DISPLAY_NAME", savedName);
        String cachedSpecialization = prefs.getString("COUNSELOR_SPECIALIZATION", "");
        String cachedBio            = prefs.getString("COUNSELOR_BIO", "");
        String cachedStatus         = prefs.getString("COUNSELOR_STATUS", "");

        tvDisplayName.setText("Name: "            + (cachedName.isEmpty() ? "—" : cachedName));
        tvSpecialization.setText("Specialization: " + (cachedSpecialization.isEmpty() ? "—" : cachedSpecialization));
        tvBio.setText("Bio: "                     + (cachedBio.isEmpty() ? "—" : cachedBio));
        tvStatus.setText("Status: "               + (cachedStatus.isEmpty() ? "—" : cachedStatus));

        String url = ApiConstants.counselorProfile(Long.parseLong(userId));

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    // API overrides only if values are non-empty
                    String displayName = response.optString("displayName", "");
                    tvDisplayName.setText("Name: " + (displayName.isEmpty() ? cachedName : displayName));

                    String spec = response.optString("specialization", "");
                    tvSpecialization.setText("Specialization: " + (spec.isEmpty() ? cachedSpecialization : spec));

                    String bio = response.optString("bio", "");
                    tvBio.setText("Bio: " + (bio.isEmpty() ? cachedBio : bio));

                    String status = response.optString("status", "");
                    tvStatus.setText("Status: " + (status.isEmpty() ? cachedStatus : status));

                    double avg   = response.optDouble("ratingAverage", 0.0);
                    int    count = response.optInt("ratingCount", 0);
                    tvRating.setText(String.format("Rating: %.1f (%d reviews)", avg, count));
                },
                error -> {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        Toast.makeText(this, "No profile yet. Create one!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void deleteUser(String userId) {
        String url = ApiConstants.DELETE + userId;

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    SharedPreferences.Editor editor = getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit();
                    editor.clear();
                    editor.apply();

                    Toast.makeText(CounselorProfileActivity.this, "Profile deleted", Toast.LENGTH_SHORT).show();

                    Intent i = new Intent(CounselorProfileActivity.this, SignUpActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finishAffinity();
                },
                error -> Toast.makeText(CounselorProfileActivity.this,
                        "Delete failed: " + (error.getMessage() != null ? error.getMessage() : "unknown error"),
                        Toast.LENGTH_LONG).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}