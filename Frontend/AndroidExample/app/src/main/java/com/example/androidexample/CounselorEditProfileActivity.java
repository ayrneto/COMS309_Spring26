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
        EditText etEmail             = findViewById(R.id.etEmail);
        EditText etPassword          = findViewById(R.id.etPassword);
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

        // ── Pre-fill from SharedPreferences (always up to date) ──────────────
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", "");

        etDisplayName.setText(prefs.getString("COUNSELOR_DISPLAY_NAME",   ""));
        etEmail.setText(prefs.getString("USER_EMAIL",                      ""));
        etSpecialization.setText(prefs.getString("COUNSELOR_SPECIALIZATION",""));
        etBio.setText(prefs.getString("COUNSELOR_BIO",                     ""));
        etProfilePictureUrl.setText(prefs.getString("COUNSELOR_PROFILE_PIC",""));

        String savedStatus = prefs.getString("COUNSELOR_STATUS", "AVAILABLE");
        for (int i = 0; i < STATUSES.length; i++) {
            if (STATUSES[i].equalsIgnoreCase(savedStatus)) {
                spStatus.setSelection(i);
                break;
            }
        }

        // Also accept overrides from intent extras if provided
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String intentUserId = String.valueOf(extras.getLong("userId", -1));
            if (!intentUserId.equals("-1")) userId = intentUserId;
        }

        if (userId.isEmpty() || userId.equals("-1")) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ── Save ──────────────────────────────────────────────────────────────
        btnSave.setOnClickListener(v -> {
            String displayName    = etDisplayName.getText().toString().trim();
            String email          = etEmail.getText().toString().trim();
            String password       = etPassword.getText().toString().trim();
            String specialization = etSpecialization.getText().toString().trim();
            String bio            = etBio.getText().toString().trim();
            String picUrl         = etProfilePictureUrl.getText().toString().trim();
            String status         = spStatus.getSelectedItem().toString();

            if (displayName.isEmpty()) {
                etDisplayName.setError("Required");
                return;
            }

            saveProfile(displayName, email, password, specialization, bio, picUrl, status);
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

    // ── Save counselor profile ────────────────────────────────────────────────
    private void saveProfile(String displayName, String email, String password,
                             String specialization, String bio,
                             String profilePictureUrl, String status) {

        // Endpoint: PUT /api/counsellors/{userId}/update
        String url = ApiConstants.BASE_URL + "/api/counsellors/" + userId + "/update";

        JSONObject body = new JSONObject();
        try {
            body.put("displayName",       displayName);
            body.put("specialization",    specialization);
            body.put("bio",               bio);
            body.put("profilePictureUrl", profilePictureUrl);
            body.put("status",            status);
            // Only include email/password if provided
            if (!email.isEmpty())    body.put("email",    email);
            if (!password.isEmpty()) body.put("password", password);
        } catch (JSONException e) {
            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        final String bodyStr = body.toString();

        StringRequest request = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    // Cache all updated values in SharedPreferences
                    SharedPreferences.Editor editor =
                            getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit();
                    editor.putString("COUNSELOR_DISPLAY_NAME",   displayName);
                    editor.putString("COUNSELOR_SPECIALIZATION", specialization);
                    editor.putString("COUNSELOR_BIO",            bio);
                    editor.putString("COUNSELOR_PROFILE_PIC",    profilePictureUrl);
                    editor.putString("COUNSELOR_STATUS",         status);
                    if (!email.isEmpty()) editor.putString("USER_EMAIL", email);
                    editor.apply();

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
            public byte[] getBody() { return bodyStr.getBytes(); }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    // ── Delete user ───────────────────────────────────────────────────────────
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