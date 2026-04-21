package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RoutineSummaryActivity extends AppCompatActivity {

    private GridLayout routinesContainer;
    private Spinner filterSpinner;
    private List<JSONObject> allRoutines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_summary);

        routinesContainer = findViewById(R.id.routinesContainer);
        filterSpinner = findViewById(R.id.filterSpinner);
        FrameLayout btnAddRoutine = findViewById(R.id.btnAddRoutine);

        setupFilter();
        fetchRoutines();

        btnAddRoutine.setOnClickListener(v -> {
            Intent intent = new Intent(RoutineSummaryActivity.this, RoutineTrackerActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchRoutines();
    }

    private void setupFilter() {
        String[] filterOptions = {"Active First", "Completed First", "Progress %"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, filterOptions);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortAndDisplay(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchRoutines() {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/routines";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    allRoutines.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            allRoutines.add(response.getJSONObject(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    sortAndDisplay(filterSpinner.getSelectedItemPosition());
                },
                error -> {
                    Toast.makeText(this, "Failed to load routines", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void sortAndDisplay(int filterType) {
        List<JSONObject> sorted = new ArrayList<>(allRoutines);

        switch (filterType) {
            case 0: // Active First
                Collections.sort(sorted, (a, b) -> {
                    try {
                        boolean aCompleted = a.getBoolean("completed");
                        boolean bCompleted = b.getBoolean("completed");
                        if (aCompleted == bCompleted) {
                            return b.getInt("streakCount") - a.getInt("streakCount");
                        }
                        return aCompleted ? 1 : -1;
                    } catch (JSONException e) {
                        return 0;
                    }
                });
                break;

            case 1: // Completed First
                Collections.sort(sorted, (a, b) -> {
                    try {
                        boolean aCompleted = a.getBoolean("completed");
                        boolean bCompleted = b.getBoolean("completed");
                        if (aCompleted == bCompleted) {
                            return b.getInt("streakCount") - a.getInt("streakCount");
                        }
                        return aCompleted ? -1 : 1;
                    } catch (JSONException e) {
                        return 0;
                    }
                });
                break;

            case 2: // Progress %
                Collections.sort(sorted, (a, b) -> {
                    try {
                        return b.getInt("streakCount") - a.getInt("streakCount");
                    } catch (JSONException e) {
                        return 0;
                    }
                });
                break;
        }

        displayRoutines(sorted);
    }

    private void displayRoutines(List<JSONObject> routines) {
        routinesContainer.removeAllViews();

        for (JSONObject routine : routines) {
            try {
                String id = routine.getString("id");
                String title = routine.getString("title");
                String description = routine.getString("description");
                String label = routine.getString("label");
                int streakCount = routine.getInt("streakCount");
                boolean completed = routine.getBoolean("completed");

                addRoutineCard(id, title, description, label, streakCount, completed);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void addRoutineCard(String id, String title, String description, String label, int streakCount, boolean completed) {
        View cardView = getLayoutInflater().inflate(R.layout.routine_card, routinesContainer, false);

        TextView titleView = cardView.findViewById(R.id.routineTitle);
        TextView progressView = cardView.findViewById(R.id.routineProgress);
        TextView labelView = cardView.findViewById(R.id.routineLabel);
        ProgressBar progressBar = cardView.findViewById(R.id.progressBar);
        View cardBackground = cardView.findViewById(R.id.cardBackground);

        titleView.setText(title);
        progressView.setText("Day " + streakCount + "/60");
        labelView.setText(label);
        progressBar.setMax(60);
        progressBar.setProgress(streakCount);

        // Set background color based on progress
        int backgroundColor = getProgressColor(streakCount, completed);
        cardBackground.setBackgroundColor(backgroundColor);

        // Click to view details or check-in
        cardView.setOnClickListener(v -> {
            if (completed) {
                showCompletedDialog(title);
            } else {
                showRoutineOptions(id, title, description, streakCount);
            }
        });

        routinesContainer.addView(cardView);
    }

    private int getProgressColor(int streakCount, boolean completed) {
        if (completed || streakCount >= 60) {
            return Color.parseColor("#4CAF50"); // Green
        } else if (streakCount >= 41) {
            return Color.parseColor("#FFEB3B"); // Yellow
        } else if (streakCount >= 21) {
            return Color.parseColor("#FF9800"); // Orange
        } else {
            return Color.parseColor("#F44336"); // Red
        }
    }

    private void showCompletedDialog(String title) {
        new AlertDialog.Builder(this)
                .setTitle("Completed!")
                .setMessage(title + " is completed. Congratulations!")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showRoutineOptions(String routineId, String title, String description, int currentDay) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(description + "\n\nCurrent: Day " + currentDay + "/60");

        builder.setPositiveButton("Check In Today", (dialog, which) -> {
            checkInRoutine(routineId);
        });

        builder.setNegativeButton("Delete", (dialog, which) -> {
            deleteRoutine(routineId);
        });

        builder.setNeutralButton("Cancel", null);

        builder.show();
    }

    private void checkInRoutine(String routineId) {
        String url = ApiConstants.BASE_URL + "/api/routines/" + routineId + "/checkin";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    Toast.makeText(this, "Checked in!", Toast.LENGTH_SHORT).show();
                    fetchRoutines();
                },
                error -> {
                    Toast.makeText(this, "Failed to check in", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void deleteRoutine(String routineId) {
        String url = ApiConstants.BASE_URL + "/api/routines/" + routineId;

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Toast.makeText(this, "Routine deleted", Toast.LENGTH_SHORT).show();
                    fetchRoutines();
                },
                error -> {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }
}