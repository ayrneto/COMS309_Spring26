package com.example.androidexample;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class AdminEditUserActivity extends AppCompatActivity {

    private static final String[] STATUSES = {"AVAILABLE", "BUSY", "OFFLINE"};

    private long   userId = -1;
    private String userRole = "USER";

    // User fields
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Switch   switchActive;

    // Counsellor fields
    private LinearLayout counsellorSection;
    private EditText etCDisplayName, etCSpecialization, etCBio;
    private Spinner  spCStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_user);

        // ── Bind views ────────────────────────────────────────────────────────
        TextView tvTitle         = findViewById(R.id.tvEditTitle);
        etName                   = findViewById(R.id.etEditName);
        etEmail                  = findViewById(R.id.etEditEmail);
        etPassword               = findViewById(R.id.etEditPassword);
        etConfirmPassword        = findViewById(R.id.etEditConfirmPassword);
        switchActive             = findViewById(R.id.switchActive);
        counsellorSection        = findViewById(R.id.counsellorFieldsSection);
        etCDisplayName           = findViewById(R.id.etCDisplayName);
        etCSpecialization        = findViewById(R.id.etCSpecialization);
        etCBio                   = findViewById(R.id.etCBio);
        spCStatus                = findViewById(R.id.spCStatus);
        Button btnSaveUser       = findViewById(R.id.btnSaveUser);
        Button btnSaveCounsellor = findViewById(R.id.btnSaveCounsellor);
        Button btnDeactivate     = findViewById(R.id.btnDeactivate);
        Button btnDelete         = findViewById(R.id.btnDeleteUser);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Status spinner
        spCStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, STATUSES));

        // ── Read Intent extras ────────────────────────────────────────────────
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userId   = extras.getLong("userId", -1);
            userRole = extras.getString("userRole", "USER");

            etName.setText(extras.getString("userName",  ""));
            etEmail.setText(extras.getString("userEmail", ""));
            etPassword.setText(extras.getString("userPassword", ""));
            etConfirmPassword.setText(extras.getString("userConfirmPassword", ""));
            switchActive.setChecked(extras.getBoolean("userActive", true));

            tvTitle.setText("Edit " + userRole.charAt(0) +
                    userRole.substring(1).toLowerCase());

            if ("COUNSELLOR".equals(userRole)) {
                counsellorSection.setVisibility(View.VISIBLE);
                etCDisplayName.setText(extras.getString("cDisplayName",    ""));
                etCSpecialization.setText(extras.getString("cSpecialization", ""));
                etCBio.setText(extras.getString("cBio",                    ""));

                String status = extras.getString("cStatus", "AVAILABLE");
                for (int i = 0; i < STATUSES.length; i++) {
                    if (STATUSES[i].equalsIgnoreCase(status)) {
                        spCStatus.setSelection(i);
                        break;
                    }
                }
            }
        }

        if (userId == -1) {
            Toast.makeText(this, "Error: user ID missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ── Save user ─────────────────────────────────────────────────────────
        btnSaveUser.setOnClickListener(v -> {
            String name            = etName.getText().toString().trim();
            String email           = etEmail.getText().toString().trim();
            String password        = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            boolean active         = switchActive.isChecked();

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Name and email required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.isEmpty() && !password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            updateUser(name, email, password, confirmPassword, active);
        });

        // ── Save counsellor profile ───────────────────────────────────────────
        btnSaveCounsellor.setOnClickListener(v -> {
            String displayName    = etCDisplayName.getText().toString().trim();
            String specialization = etCSpecialization.getText().toString().trim();
            String bio            = etCBio.getText().toString().trim();
            String status         = spCStatus.getSelectedItem().toString();

            if (displayName.isEmpty()) {
                etCDisplayName.setError("Required");
                return;
            }
            updateCounsellorProfile(displayName, specialization, bio, status);
        });

        // ── Deactivate ────────────────────────────────────────────────────────
        btnDeactivate.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Deactivate Account?")
                        .setMessage("This will prevent the user from logging in.")
                        .setPositiveButton("Deactivate", (d, w) -> {
                            String name  = etName.getText().toString().trim();
                            String email = etEmail.getText().toString().trim();
                            updateUser(name, email, "", "", false);
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        // ── Delete ────────────────────────────────────────────────────────────
        btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete Account?")
                        .setMessage("This cannot be undone.")
                        .setPositiveButton("Yes, Delete", (d, w) -> deleteUser())
                        .setNegativeButton("Cancel", null)
                        .show()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/admin/update/{id}
    // ─────────────────────────────────────────────────────────────────────────
    private void updateUser(String name, String email,
                            String password, String confirmPassword,
                            boolean active) {
        String url = ApiConstants.EDIT + userId;

        JSONObject body = new JSONObject();
        try {
            body.put("name",   name);
            body.put("email",  email);
            body.put("active", active);
            if (!password.isEmpty()) {
                body.put("password",        password);
                body.put("confirmPassword", confirmPassword);
            }
        } catch (JSONException e) {
            Toast.makeText(this, "JSON error", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest req = new StringRequest(
                Request.Method.PUT, url,
                response -> {
                    switchActive.setChecked(active);
                    Toast.makeText(this,
                            active ? "User updated!" : "Account deactivated",
                            Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    String msg = "Update failed";
                    if (error.networkResponse != null)
                        msg += " (HTTP " + error.networkResponse.statusCode + ")";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override public byte[] getBody() { return body.toString().getBytes(); }
            @Override public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/counsellors/{userId}/profile
    // ─────────────────────────────────────────────────────────────────────────
    private void updateCounsellorProfile(String displayName, String specialization,
                                         String bio, String status) {
        String url = ApiConstants.counselorProfile(userId);

        JSONObject body = new JSONObject();
        try {
            body.put("displayName",       displayName);
            body.put("specialization",    specialization);
            body.put("bio",               bio);
            body.put("profilePictureUrl", "");
            body.put("status",            status);
        } catch (JSONException e) {
            Toast.makeText(this, "JSON error", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest req = new StringRequest(
                Request.Method.PUT, url,
                response -> Toast.makeText(this,
                        "Counsellor profile updated!", Toast.LENGTH_SHORT).show(),
                error -> {
                    String msg = "Update failed";
                    if (error.networkResponse != null)
                        msg += " (HTTP " + error.networkResponse.statusCode + ")";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override public byte[] getBody() { return body.toString().getBytes(); }
            @Override public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/admin/{id}
    // ─────────────────────────────────────────────────────────────────────────
    private void deleteUser() {
        String url = ApiConstants.ADMIN_DELETE + userId;

        StringRequest req = new StringRequest(
                Request.Method.DELETE, url,
                response -> {
                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    if (error.networkResponse != null &&
                            (error.networkResponse.statusCode == 200 ||
                                    error.networkResponse.statusCode == 204)) {
                        Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    String msg = "Delete failed";
                    if (error.networkResponse != null)
                        msg += " (HTTP " + error.networkResponse.statusCode + ")";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }
}