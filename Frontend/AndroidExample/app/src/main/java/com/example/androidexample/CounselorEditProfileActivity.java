//package com.example.androidexample;
//
//import android.os.Bundle;
//import android.view.View;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ProgressBar;
//import android.widget.Spinner;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.android.volley.Request;
//import com.android.volley.VolleyError;
//import com.android.volley.toolbox.JsonObjectRequest;
//
//import org.json.JSONObject;
//
//public class CounselorEditProfileActivity extends AppCompatActivity {
//
//    private EditText etDisplayName, etSpecialization, etBio, etProfilePictureUrl;
//    private Spinner spStatus;
//    private Button btnSave;
//    private ProgressBar progress;
//
//    private long userId = -1;
//
//    private static final String[] STATUSES = {"AVAILABLE", "BUSY", "OFFLINE"};
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_counsellor_edit_profile);
//
//        etDisplayName = findViewById(R.id.etDisplayName);
//        etSpecialization = findViewById(R.id.etSpecialization);
//        etBio = findViewById(R.id.etBio);
//        etProfilePictureUrl = findViewById(R.id.etProfilePictureUrl);
//        spStatus = findViewById(R.id.spStatus);
//        btnSave = findViewById(R.id.btnSave);
//        progress = findViewById(R.id.progress);
//
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(
//                this,
//                android.R.layout.simple_spinner_dropdown_item,
//                STATUSES
//        );
//        spStatus.setAdapter(adapter);
//
//        // Get userId from intent extras (same style as your EditProfile activity)
//        Bundle extras = getIntent().getExtras();
//        if (extras != null) {
//            userId = extras.getLong("userId", -1);
//        }
//
//        if (userId == -1) {
//            Toast.makeText(this, "Missing userId. Can't edit profile.", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
//
//        // Load existing profile (if any)
//        fetchExistingProfile();
//
//        btnSave.setOnClickListener(v -> saveProfile());
//    }
//
//    private void setLoading(boolean isLoading) {
//        progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
//        btnSave.setEnabled(!isLoading);
//    }
//
//    private void fetchExistingProfile() {
//        setLoading(true);
//
//        String url = ApiConstants.counselorProfile(userId);
//
//        JsonObjectRequest req = new JsonObjectRequest(
//                Request.Method.GET,
//                url,
//                null,
//                response -> {
//                    setLoading(false);
//
//                    // Fill UI safely (optString avoids crashes)
//                    etDisplayName.setText(response.optString("displayName", ""));
//                    etSpecialization.setText(response.optString("specialization", ""));
//                    etBio.setText(response.optString("bio", ""));
//                    etProfilePictureUrl.setText(response.optString("profilePictureUrl", ""));
//
//                    String status = response.optString("status", "OFFLINE");
//                    setSpinnerToStatus(status);
//                },
//                error -> {
//                    setLoading(false);
//
//                    // If profile doesn't exist yet, backend might return 404.
//                    // That's fine: user is creating it now.
//                    if (is404(error)) {
//                        Toast.makeText(this, "No profile yet — create one!", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(this, "Failed to load profile: " + error.getMessage(), Toast.LENGTH_LONG).show();
//                    }
//                }
//        );
//
//        VolleySingleton.getInstance(this).addToRequestQueue(req);
//    }
//
//    private void saveProfile() {
//        String displayName = etDisplayName.getText().toString().trim();
//        String specialization = etSpecialization.getText().toString().trim();
//        String bio = etBio.getText().toString().trim();
//        String picUrl = etProfilePictureUrl.getText().toString().trim();
//        String status = spStatus.getSelectedItem().toString();
//
//        if (displayName.isEmpty()) {
//            etDisplayName.setError("Required");
//            return;
//        }
//
//        setLoading(true);
//
//        JSONObject body = new JSONObject();
//        try {
//            body.put("displayName", displayName);
//            body.put("specialization", specialization);
//            body.put("bio", bio);
//            body.put("profilePictureUrl", picUrl);
//            body.put("status", status);
//        } catch (Exception e) {
//            setLoading(false);
//            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        String url = ApiConstants.counselorProfile(userId);
//
//        JsonObjectRequest req = new JsonObjectRequest(
//                Request.Method.PUT,
//                url,
//                body,
//                response -> {
//                    setLoading(false);
//                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
//                    finish(); // or navigate back to dashboard
//                },
//                error -> {
//                    setLoading(false);
//                    Toast.makeText(this, "Save failed: " + volleyErrorToText(error), Toast.LENGTH_LONG).show();
//                }
//        );
//
//        VolleySingleton.getInstance(this).addToRequestQueue(req);
//    }
//
//    private void setSpinnerToStatus(String status) {
//        for (int i = 0; i < STATUSES.length; i++) {
//            if (STATUSES[i].equalsIgnoreCase(status)) {
//                spStatus.setSelection(i);
//                return;
//            }
//        }
//        spStatus.setSelection(2); // OFFLINE default
//    }
//
//    private boolean is404(VolleyError error) {
//        return error != null && error.networkResponse != null && error.networkResponse.statusCode == 404;
//    }
//
//    private String volleyErrorToText(VolleyError error) {
//        if (error == null) return "unknown error";
//        if (error.networkResponse != null) {
//            return "HTTP " + error.networkResponse.statusCode;
//        }
//        return error.getMessage() != null ? error.getMessage() : "network error";
//    }
//}