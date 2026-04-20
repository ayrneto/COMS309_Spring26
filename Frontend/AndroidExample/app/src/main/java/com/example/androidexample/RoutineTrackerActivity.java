package com.example.androidexample;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class RoutineTrackerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_tracker);

        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        EditText routineTitle = findViewById(R.id.routineTitle);
        EditText routineDescription = findViewById(R.id.routineDescription);
        EditText routineStartDate = findViewById(R.id.routineStartDate);
        EditText routineReminder = findViewById(R.id.routineReminder);
        Spinner spinnerLabel = findViewById(R.id.spinnerLabel);
        Button btnSave = findViewById(R.id.btnSave);

        // Spinner
        String[] labels = {"Work", "Personal", "School", "Finance", "Social"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels);
        spinnerLabel.setAdapter(adapter);

        // Date picker
        routineStartDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, day) -> routineStartDate.setText(
                            String.format("%04d-%02d-%02d", year, month + 1, day)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // Time picker for reminder
        routineReminder.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(this,
                    (view, hour, minute) -> routineReminder.setText(
                            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)),
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
            ).show();
        });

        // Save button
        btnSave.setOnClickListener(v -> {
            String title = routineTitle.getText().toString();
            String description = routineDescription.getText().toString();
            String startDate = routineStartDate.getText().toString();
            String reminderTime = routineReminder.getText().toString();
            String label = spinnerLabel.getSelectedItem().toString();

            if (title.isEmpty() || startDate.isEmpty()) {
                Toast.makeText(this, "Title and start date required", Toast.LENGTH_SHORT).show();
                return;
            }

            saveRoutine(title, description, startDate, reminderTime, label);
        });
    }

    private void saveRoutine(String title, String description, String startDate, String reminderTime, String label) {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/routines";

        JSONObject routineData = new JSONObject();
        try {
            routineData.put("title", title);
            routineData.put("description", description);
            routineData.put("startDate", startDate);
            routineData.put("label", label);

            if (!reminderTime.isEmpty()) {
                routineData.put("reminderTime", reminderTime);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                routineData,
                response -> {
                    Toast.makeText(this, "Routine created!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Failed to create routine", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }
}
