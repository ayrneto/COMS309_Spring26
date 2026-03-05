package com.example.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class CounselorEditProfileActivity extends AppCompatActivity {

    private static final String[] STATUSES = {"AVAILABLE", "BUSY", "OFFLINE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counsellor_edit_profile);

        EditText etDisplayName       = findViewById(R.id.etDisplayName);
        EditText etSpecialization    = findViewById(R.id.etSpecialization);
        EditText etBio               = findViewById(R.id.etBio);
        EditText etProfilePictureUrl = findViewById(R.id.etProfilePictureUrl);
        Spinner  spStatus            = findViewById(R.id.spStatus);
        Button   btnSave             = findViewById(R.id.btnSave);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                STATUSES
        );
        spStatus.setAdapter(adapter);

        String userId = "";
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userId = String.valueOf(extras.getLong("userId", -1));

            etDisplayName.setText(extras.getString("displayName", ""));
            etSpecialization.setText(extras.getString("specialization", ""));
            etBio.setText(extras.getString("bio", ""));
            etProfilePictureUrl.setText(extras.getString("profilePictureUrl", ""));

            String status = extras.getString("status", "OFFLINE");
            for (int i = 0; i < STATUSES.length; i++) {
                if (STATUSES[i].equalsIgnoreCase(status)) {
                    spStatus.setSelection(i);
                    break;
                }
            }
        }

        if (userId.equals("-1") || userId.isEmpty()) {
            Toast.makeText(this, "User ID missing. Can't edit profile.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final String finalUserId = userId;

        btnSave.setOnClickListener(v -> {
            String displayName       = etDisplayName.getText().toString().trim();
            String specialization    = etSpecialization.getText().toString().trim();
            String bio               = etBio.getText().toString().trim();
            String profilePictureUrl = etProfilePictureUrl.getText().toString().trim();
            String status            = spStatus.getSelectedItem().toString();

            if (displayName.isEmpty()) {
                etDisplayName.setError("Required");
                return;
            }

            saveProfile(finalUserId, displayName, specialization, bio, profilePictureUrl, status);
        });
    }

    private void saveProfile(String userId, String displayName, String specialization,
                             String bio, String profilePictureUrl, String status) {

        String url = ApiConstants.counselorProfile(Long.parseLong(userId));

        JSONObject body = new JSONObject();
        try {
            body.put("displayName", displayName);
            body.put("specialization", specialization);
            body.put("bio", bio);
            body.put("profilePictureUrl", profilePictureUrl);
            body.put("status", status);
        } catch (JSONException e) {
            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest request = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    // Save edited values to SharedPreferences so profile screen reflects them
                    SharedPreferences.Editor editor =
                            getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit();
                    editor.putString("COUNSELOR_DISPLAY_NAME",   displayName);
                    editor.putString("COUNSELOR_SPECIALIZATION", specialization);
                    editor.putString("COUNSELOR_BIO",            bio);
                    editor.putString("COUNSELOR_PROFILE_PIC",    profilePictureUrl);
                    editor.putString("COUNSELOR_STATUS",         status);
                    editor.apply();

                    Toast.makeText(CounselorEditProfileActivity.this, "Profile saved!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    String msg = "Save failed!";
                    if (error.networkResponse != null) msg += " HTTP " + error.networkResponse.statusCode;
                    Toast.makeText(CounselorEditProfileActivity.this, msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public byte[] getBody() {
                return body.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}