package com.example.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class SelectCounselorActivity extends AppCompatActivity {

    private long noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_counselor);

        // Get noteId from intent
        noteId = getIntent().getLongExtra("noteId", -1);

        fetchCounselors();
    }

    private void fetchCounselors() {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");

        String url = ApiConstants.BASE_URL + "/users/" + userId + "/counsellors";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (response.length() == 0) {
                        Toast.makeText(this, "No counselors assigned to you yet", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    displayCounselors(response);
                },
                error -> {
                    Toast.makeText(this, "Failed to load counselors", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void displayCounselors(org.json.JSONArray counselors) {
        LinearLayout container = findViewById(R.id.counselorsContainer);
        container.removeAllViews();

        try {
            for (int i = 0; i < counselors.length(); i++) {
                JSONObject counselor = counselors.getJSONObject(i);

                // TODO: VERIFY FIELD NAMES - Adjust based on actual backend response
                // Expected fields: id, name, email (or displayName)
                String counselorId = counselor.getString("id");
                String counselorName = counselor.optString("name", "Counselor #" + counselorId);
                String counselorEmail = counselor.optString("email", "");

                addCounselorCard(container, counselorId, counselorName, counselorEmail);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addCounselorCard(LinearLayout container, String counselorId, String counselorName, String counselorEmail) {
        View cardView = getLayoutInflater().inflate(R.layout.counselor_card, container, false);

        TextView nameView = cardView.findViewById(R.id.counselorName);
        TextView emailView = cardView.findViewById(R.id.counselorEmail);

        nameView.setText(counselorName);
        emailView.setText(counselorEmail);

        // Set avatar initial
        android.widget.TextView tvAvatar = cardView.findViewById(R.id.tvAvatar);
        if (tvAvatar != null && !counselorName.isEmpty()) {
            tvAvatar.setText(String.valueOf(counselorName.charAt(0)).toUpperCase());
        }

        // Click to share note with this counselor
        cardView.setOnClickListener(v -> {
            shareNoteWithCounselor(counselorId, counselorName);
        });

        container.addView(cardView);
    }

    private void shareNoteWithCounselor(String counselorId, String counselorName) {
        String url = ApiConstants.BASE_URL + "/api/notes/" + noteId + "/share";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("counsellorUserId", Long.parseLong(counselorId));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PATCH,
                url,
                requestBody,
                response -> {
                    Toast.makeText(this, "Note shared with " + counselorName, Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Failed to share note", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }
}