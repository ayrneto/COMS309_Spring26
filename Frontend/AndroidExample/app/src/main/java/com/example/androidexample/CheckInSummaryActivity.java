package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CheckInSummaryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_summary);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");

        fetchCheckIns(userId);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");
        fetchCheckIns(userId);
    }

    private void fetchCheckIns(String userId) {
        String url = ApiConstants.BASE_URL + "/users/" + userId + "/checkins";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    displayCheckIns(response);
                },
                error -> {
                    Toast.makeText(this, "Failed to load check-ins", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void displayCheckIns(JSONArray checkIns) {
        GridLayout container = findViewById(R.id.checkInsContainer);
        container.removeAllViews();

        try {
            for (int i = checkIns.length() - 1; i >= 0; i--) {
                JSONObject checkIn = checkIns.getJSONObject(i);

                String checkInId = checkIn.getString("id");
                String date = checkIn.getString("date");
                int rating = checkIn.getInt("rating");
                String description = checkIn.getString("description");
                String reminderTime = checkIn.optString("reminderTime", "20:00");

                addCheckInCard(container, checkInId, date, rating, description, reminderTime);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addCheckInCard(GridLayout container, String checkInId, String date, int rating, String description, String reminderTime) {
        View cardView = getLayoutInflater().inflate(R.layout.checkin_card, container, false);

        TextView dateView = cardView.findViewById(R.id.checkInDate);
        View ratingCircle = cardView.findViewById(R.id.ratingCircle);
        TextView ratingText = cardView.findViewById(R.id.ratingText);

        dateView.setText(date);
        ratingText.setText(rating + "/5");
        ratingCircle.setBackgroundResource(getCircleDrawable(rating));

        // Make card clickable
        cardView.setOnClickListener(v -> {
            showEditDeleteDialog(checkInId, date, rating, description, reminderTime);
        });

        container.addView(cardView);
    }

    private int getCircleDrawable(int rating) {
        switch (rating) {
            case 1: return R.drawable.circle_rating_1;
            case 2: return R.drawable.circle_rating_2;
            case 3: return R.drawable.circle_rating_3;
            case 4: return R.drawable.circle_rating_4;
            case 5: return R.drawable.circle_rating_5;
            default: return R.drawable.circle_rating_3;
        }
    }

    private void showEditDeleteDialog(String checkInId, String date, int rating, String description, String reminderTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Action");

        builder.setPositiveButton("Edit", (dialog, which) -> {
            Intent intent = new Intent(CheckInSummaryActivity.this, CheckInActivity.class);
            intent.putExtra("checkInId", checkInId);
            intent.putExtra("date", date);
            intent.putExtra("rating", rating);
            intent.putExtra("description", description);
            intent.putExtra("reminderTime", reminderTime);
            startActivity(intent);
        });

        builder.setNegativeButton("Delete", (dialog, which) -> {
            deleteCheckIn(checkInId);
        });

        builder.setNeutralButton("Cancel", null);

        builder.show();
    }

    private void deleteCheckIn(String checkInId) {
        String url = ApiConstants.BASE_URL + "/users/checkins/" + checkInId;

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Toast.makeText(this, "Check-in deleted", Toast.LENGTH_SHORT).show();
                    SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
                    String userId = prefs.getString("USER_ID", "-1");
                    fetchCheckIns(userId);
                },
                error -> {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

}