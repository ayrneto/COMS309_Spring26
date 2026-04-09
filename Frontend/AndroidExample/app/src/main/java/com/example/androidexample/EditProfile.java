package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class EditProfile extends AppCompatActivity {

    private String userId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editprofile);

        // ── Views ─────────────────────────────────────────────────────────────
        EditText nameField            = findViewById(R.id.editName);
        EditText emailField           = findViewById(R.id.editEmail);
        EditText passwordField        = findViewById(R.id.editPassword);
        EditText confirmPasswordField = findViewById(R.id.editConfirmPassword);
        Button   btnSave              = findViewById(R.id.btnSave);
        Button   btnDelete            = findViewById(R.id.btnDeleteAccount);

        // Back button in top bar
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // ── Pre-fill from Intent extras ───────────────────────────────────────
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userId = extras.getString("userId", "");
            nameField.setText(extras.getString("name",     ""));
            emailField.setText(extras.getString("email",   ""));
            passwordField.setText(extras.getString("password", ""));
        }

        // ── Save ──────────────────────────────────────────────────────────────
        btnSave.setOnClickListener(v -> {
            String newName     = nameField.getText().toString().trim();
            String newEmail    = emailField.getText().toString().trim();
            String newPassword = passwordField.getText().toString().trim();
            String confirmPwd  = confirmPasswordField.getText().toString().trim();

            if (userId.isEmpty()) {
                Toast.makeText(this, "User ID missing", Toast.LENGTH_LONG).show();
                return;
            }
            if (newName.isEmpty() || newEmail.isEmpty() ||
                    newPassword.isEmpty() || confirmPwd.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPassword.equals(confirmPwd)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            updateProfile(newName, newEmail, newPassword, confirmPwd);
        });

        // ── Delete account ────────────────────────────────────────────────────
        btnDelete.setOnClickListener(v -> {
            if (userId.isEmpty()) {
                Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Delete Account?")
                    .setMessage("Are you sure? This cannot be undone.")
                    .setPositiveButton("Yes, Delete", (dialog, which) -> deleteUser())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update profile
    // ─────────────────────────────────────────────────────────────────────────
    private void updateProfile(String name, String email,
                               String password, String confirmPassword) {
        String url = ApiConstants.EDIT + userId;

        JSONObject updateData = new JSONObject();
        try {
            updateData.put("name",            name);
            updateData.put("email",           email);
            updateData.put("active",          true);
            updateData.put("password",        password);
            updateData.put("confirmPassword", confirmPassword);
        } catch (JSONException e) {
            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest request = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    // Update cached name + email in SharedPreferences
                    getSharedPreferences("AA_PREFS", MODE_PRIVATE)
                            .edit()
                            .putString("USER_NAME",  name)
                            .putString("USER_EMAIL", email)
                            .apply();

                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    String msg = "Update failed";
                    if (error.networkResponse != null)
                        msg += " (HTTP " + error.networkResponse.statusCode + ")";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public byte[] getBody() {
                return updateData.toString().getBytes();
            }

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