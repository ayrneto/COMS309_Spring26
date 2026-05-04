package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONObject;

public class UserProfileActivity extends AppCompatActivity {

    private long   targetUserId;
    private String targetUserName;
    private String targetUserEmail;

    private TextView tvLatestMood, tvLatestNote, tvLatestDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Receive data from UserListActivity
        targetUserId   = getIntent().getLongExtra("TARGET_USER_ID", -1);
        targetUserName  = getIntent().getStringExtra("TARGET_USER_NAME");
        targetUserEmail = getIntent().getStringExtra("TARGET_USER_EMAIL");
        String appointmentDate = getIntent().getStringExtra("APPOINTMENT_DATE");

        if (targetUserId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Populate header
        String initial = (targetUserName == null || targetUserName.isEmpty()) ? "?"
                : String.valueOf(targetUserName.charAt(0)).toUpperCase();
        ((TextView) findViewById(R.id.tvAvatarLarge)).setText(initial);
        ((TextView) findViewById(R.id.tvUserName)).setText(
                targetUserName == null ? "Unknown" : targetUserName);
        ((TextView) findViewById(R.id.tvUserEmail)).setText(
                targetUserEmail == null ? "" : targetUserEmail);

        if (appointmentDate != null && !appointmentDate.trim().isEmpty()) {
            ((TextView) findViewById(R.id.tvAppointmentInfo)).setText(
                    "Appointment: " + appointmentDate.trim());
        } else {
            findViewById(R.id.tvAppointmentInfo).setVisibility(View.GONE);
        }

        // Check-in summary views
        tvLatestMood  = findViewById(R.id.tvLatestMood);
        tvLatestNote  = findViewById(R.id.tvLatestNote);
        tvLatestDate  = findViewById(R.id.tvLatestDate);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // ── Action: Chat ─────────────────────────────────────────────────────
        ((Button) findViewById(R.id.btnChat)).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("partnerUserId", targetUserId);
            intent.putExtra("partnerName",   targetUserName);
            startActivity(intent);
        });

        // ── Action: Assign Task ───────────────────────────────────────────────
        ((Button) findViewById(R.id.btnAssignTask)).setOnClickListener(v -> {
            Intent intent = new Intent(this, AssignTaskActivity.class);
            // Pre-fill email so counsellor doesn't have to type it
            intent.putExtra("PREFILL_EMAIL", targetUserEmail);
            intent.putExtra("PREFILL_NAME",  targetUserName);
            startActivity(intent);
        });

        // ── Action: Assign Prescription ───────────────────────────────────────
        ((Button) findViewById(R.id.btnPrescriptions)).setOnClickListener(v -> {
            Intent intent = new Intent(this, AssignPrescriptionActivity.class);
            intent.putExtra("TARGET_USER_ID",   targetUserId);
            intent.putExtra("TARGET_USER_NAME", targetUserName);
            startActivity(intent);
        });

        // Load latest check-in for this user
        loadLatestCheckIn();
    }

    // ── Load latest check-in summary ─────────────────────────────────────────

    private void loadLatestCheckIn() {
        String url = ApiConstants.BASE_URL + "/users/" + targetUserId + "/checkins";

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    if (response.length() == 0) {
                        showNoCheckIn();
                        return;
                    }
                    // Take the last entry (most recent)
                    try {
                        JSONObject latest = response.getJSONObject(response.length() - 1);
                        int    rating      = latest.optInt("rating", 0);
                        String description = latest.optString("description", "");
                        String date        = latest.optString("date", "");

                        String moodEmoji = moodToEmoji(rating);
                        tvLatestMood.setText("Latest Mood: " + moodEmoji + " (" + rating + "/5)");
                        tvLatestNote.setText(description.isEmpty() ? "No note left" : "\u201C" + description + "\u201D");
                        tvLatestDate.setText(date.isEmpty() ? "" : "on " + date);

                    } catch (Exception e) {
                        showNoCheckIn();
                    }
                },
                error -> showNoCheckIn()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void showNoCheckIn() {
        tvLatestMood.setText("No check-ins recorded yet");
        tvLatestNote.setVisibility(View.GONE);
        tvLatestDate.setVisibility(View.GONE);
    }

    private String moodToEmoji(int rating) {
        switch (rating) {
            case 1: return "\uD83D\uDE22"; // crying
            case 2: return "\uD83D\uDE1F"; // sad
            case 3: return "\uD83D\uDE10"; // neutral
            case 4: return "\uD83D\uDE42"; // slightly happy
            case 5: return "\uD83D\uDE04"; // big smile
            default: return "—";
        }
    }
}