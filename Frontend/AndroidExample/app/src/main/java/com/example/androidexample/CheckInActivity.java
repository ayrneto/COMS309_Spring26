package com.example.androidexample;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CheckInActivity extends AppCompatActivity {
    private View[] circles;
    private int selectedRating = 0;
    private EditText reminderTimeInput;
    private EditText descriptionInput;
    private String checkInId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        // Initialize circles
        circles = new View[]{
                findViewById(R.id.circle1),
                findViewById(R.id.circle2),
                findViewById(R.id.circle3),
                findViewById(R.id.circle4),
                findViewById(R.id.circle5)
        };

        reminderTimeInput = findViewById(R.id.inputReminderTime);
        descriptionInput = findViewById(R.id.inputDescription);
        Button summaryBtn = findViewById(R.id.btnSummary);
        Button saveBtn = findViewById(R.id.btnSave);

        // Set up circle clicks
        for (int i = 0; i < circles.length; i++) {
            final int rating = i + 1;
            circles[i].setOnClickListener(v -> updateRating(rating));
        }

        // Time picker
        reminderTimeInput.setOnClickListener(v -> showTimePicker());

        // Summary button
        summaryBtn.setOnClickListener(v -> {
            Intent intent = new Intent(CheckInActivity.this, CheckInSummaryActivity.class);
            startActivity(intent);
        });

        // Save button
        saveBtn.setOnClickListener(v -> saveCheckIn());

        // Check if editing existing check-in from summary
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("checkInId")) {
            checkInId = extras.getString("checkInId");
            int rating = extras.getInt("rating");
            String description = extras.getString("description");
            String reminderTime = extras.getString("reminderTime");

            updateRating(rating);
            descriptionInput.setText(description);
            reminderTimeInput.setText(reminderTime);
        } else {
            // Load today's check-in if it exists
            loadTodayCheckIn();
        }

        // Load today's check-in if it exists
        loadTodayCheckIn();
    }

    private void updateRating(int rating) {
        selectedRating = rating;

        // Update circle appearances
        for (int i = 0; i < circles.length; i++) {
            if (i < rating) {
                // Solid color for selected
                circles[i].setBackgroundResource(getCircleDrawable(i + 1, false));
            } else {
                // Faded color for unselected
                circles[i].setBackgroundResource(getCircleDrawable(i + 1, true));
            }
        }
    }

    private int getCircleDrawable(int position, boolean faded) {
        if (faded) {
            switch (position) {
                case 1: return R.drawable.circle_rating_1_faded;
                case 2: return R.drawable.circle_rating_2_faded;
                case 3: return R.drawable.circle_rating_3_faded;
                case 4: return R.drawable.circle_rating_4_faded;
                case 5: return R.drawable.circle_rating_5_faded;
            }
        } else {
            switch (position) {
                case 1: return R.drawable.circle_rating_1;
                case 2: return R.drawable.circle_rating_2;
                case 3: return R.drawable.circle_rating_3;
                case 4: return R.drawable.circle_rating_4;
                case 5: return R.drawable.circle_rating_5;
            }
        }
        return R.drawable.circle_rating_1;
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    reminderTimeInput.setText(time);
                },
                hour, minute, true
        );
        timePickerDialog.show();
    }

    private void loadTodayCheckIn() {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/checkins/date/" + today;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        // Today's check-in exists, pre-fill
                        checkInId = response.getString("id");
                        int rating = response.getInt("rating");
                        String description = response.getString("description");
                        String reminderTime = response.optString("reminderTime", "20:00");

                        updateRating(rating);
                        descriptionInput.setText(description);
                        reminderTimeInput.setText(reminderTime);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // No check-in for today, that's fine - new entry
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void saveCheckIn() {
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = descriptionInput.getText().toString();
        String reminderTime = reminderTimeInput.getText().toString();

        if (reminderTime.isEmpty()) {
            reminderTime = "20:00";  // Default to 8 PM
        }

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");

        String url;
        int method;

        if (checkInId == null) {
            // Create new
            url = ApiConstants.BASE_URL + "/api/users/" + userId + "/checkins";
            method = Request.Method.POST;
        } else {
            // Update existing
            url = ApiConstants.BASE_URL + "/api/checkins/" + checkInId;
            method = Request.Method.PUT;
        }

        JSONObject checkInData = new JSONObject();
        try {
            checkInData.put("rating", selectedRating);
            checkInData.put("description", description);
            checkInData.put("reminderTime", reminderTime);
            checkInData.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                method,
                url,
                checkInData,
                response -> {
                    Toast.makeText(this, "Check-in saved!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

}