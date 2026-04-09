package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class CounselorEditProfileActivity extends AppCompatActivity {

    private static final String[] STATUSES = {"AVAILABLE", "BUSY", "OFFLINE"};
    private String userId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counsellor_edit_profile);

        // ── Views ─────────────────────────────────────────────────────────────
        EditText etDisplayName       = findViewById(R.id.etDisplayName);
        EditText etSpecialization    = findViewById(R.id.etSpecialization);
        EditText etBio               = findViewById(R.id.etBio);
        EditText etProfilePictureUrl = findViewById(R.id.etProfilePictureUrl);
        Spinner  spStatus            = findViewById(R.id.spStatus);
        Button   btnSave             = findViewById(R.id.btnSave);
        Button   btnDelete           = findViewById(R.id.btnDeleteAccount);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Status spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, STATUSES);
        spStatus.setAdapter(adapter);

        // ── Pre-fill from Intent extras ───────────────────────────────────────
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userId = String.valueOf(extras.getLong("userId", -1));

            etDisplayName.setText(extras.getString("displayName",     ""));
            etSpecialization.setText(extras.getString("specialization", ""));
            etBio.setText(extras.getString("bio",                     ""));
            etProfilePictureUrl.setText(extras.getString("profilePictureUrl", ""));

            String status = extras.getString("status", "AVAILABLE");
            for (int i = 0; i < STATUSES.length; i++) {
                if (STATUSES[i].equalsIgnoreCase(status)) {
                    spStatus.setSelection(i);
                    break;
                }
            }
        }

        if (userId.equals("-1") || userId.isEmpty()) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ── Save ──────────────────────────────────────────────────────────────
        btnSave.setOnClickListener(v -> {
            String displayName    = etDisplayName.getText().toString().trim();
            String specialization = etSpecialization.getText().toString().trim();
            String bio            = etBio.getText().toString().trim();
            String picUrl         = etProfilePictureUrl.getText().toString().trim();
            String status         = spStatus.getSelectedItem().toString();

            if (displayName.isEmpty()) {
                etDisplayName.setError("Required");
                return;
            }

            saveProfile(displayName, specialization, bio, picUrl, status);
        });

        // ── Delete account ────────────────────────────────────────────────────
        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete Account?")
                        .setMessage("Are you sure? This cannot be undone.")
                        .setPositiveButton("Yes, Delete", (dialog, which) -> deleteUser())
                        .setNegativeButton("Cancel", null)
                        .show()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save counselor profile
    // ─────────────────────────────────────────────────────────────────────────
    private void saveProfile(String displayName, String specialization,
                             String bio, String profilePictureUrl, String status) {
        String url = ApiConstants.counselorProfile(Long.parseLong(userId));

        JSONObject body = new JSONObject();
        try {
            body.put("displayName",      displayName);
            body.put("specialization",   specialization);
            body.put("bio",              bio);
            body.put("profilePictureUrl", profilePictureUrl);
            body.put("status",           status);
        } catch (JSONException e) {
            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest request = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    // Cache updated values
                    getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit()
                            .putString("COUNSELOR_DISPLAY_NAME",   displayName)
                            .putString("COUNSELOR_SPECIALIZATION", specialization)
                            .putString("COUNSELOR_BIO",            bio)
                            .putString("COUNSELOR_PROFILE_PIC",    profilePictureUrl)
                            .putString("COUNSELOR_STATUS",         status)
                            .apply();

                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    String msg = "Save failed";
                    if (error.networkResponse != null)
                        msg += " (HTTP " + error.networkResponse.statusCode + ")";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public byte[] getBody() { return body.toString().getBytes(); }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete user
    // ─────────────────────────────────────────────────────────────────────────
    private void deleteUser() {
        String url = ApiConstants.DELETE + userId;

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit().clear().apply();
                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, SignUpActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finishAffinity();
                },
                error -> Toast.makeText(this,
                        "Delete failed: " + (error.getMessage() != null
                                ? error.getMessage() : "unknown error"),
                        Toast.LENGTH_LONG).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}