package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

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
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.DatePickerDialog;
import java.util.Calendar;

public class AssignTaskActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_task);

        // Force light mode for colors to show correctly
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        EditText emailInput = findViewById(R.id.taskEmail);
        EditText titleInput = findViewById(R.id.taskTitle);
        EditText descriptionInput = findViewById(R.id.taskDescription);
        EditText dueDateInput = findViewById(R.id.taskDueDate);
        EditText reminderDateInput = findViewById(R.id.taskReminderDate);
        EditText reminderTimeInput = findViewById(R.id.taskReminderTime);
        Button saveBtn = findViewById(R.id.btnSaveTask);

        // Due Date Picker
        dueDateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AssignTaskActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                        dueDateInput.setText(date);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        // Reminder Date Picker
        reminderDateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AssignTaskActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                        reminderDateInput.setText(date);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        // Reminder Time Picker
        reminderTimeInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    AssignTaskActivity.this,
                    (view, selectedHour, selectedMinute) -> {
                        String time = String.format("%02d:%02d", selectedHour, selectedMinute);
                        reminderTimeInput.setText(time);
                    },
                    hour, minute, true
            );
            timePickerDialog.show();
        });

        // Save Button
        saveBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String title = titleInput.getText().toString();
            String description = descriptionInput.getText().toString();
            String dueDate = dueDateInput.getText().toString();
            String reminderDate = reminderDateInput.getText().toString();
            String reminderTime = reminderTimeInput.getText().toString();

            // Validate all fields
            if (email.isEmpty()) {
                emailInput.setError("Email is required");
                emailInput.requestFocus();
                return;
            }

            if (title.isEmpty()) {
                titleInput.setError("Title is required");
                titleInput.requestFocus();
                return;
            }

            if (description.isEmpty()) {
                descriptionInput.setError("Description is required");
                descriptionInput.requestFocus();
                return;
            }

            if (dueDate.isEmpty()) {
                dueDateInput.setError("Due date is required");
                dueDateInput.requestFocus();
                return;
            }

            if (reminderDate.isEmpty() || reminderTime.isEmpty()) {
                Toast.makeText(this, "Reminder date and time are required", Toast.LENGTH_SHORT).show();
                return;
            }

            assignTask(email, title, description, dueDate, reminderDate, reminderTime);
        });
    }


    private void assignTask(String email, String title, String description, String dueDate, String reminderDate, String reminderTime) {
        // Combine reminderDate and reminderTime
        String reminderDateTime = reminderDate + "T" + reminderTime + ":00Z";  // ISO format

        String url = ApiConstants.BASE_URL + "/api/tasks";  // TODO: Check with backend for correct endpoint

        JSONObject taskData = new JSONObject();
        try {
            taskData.put("userEmail", email);  // TODO: Check field names with backend:
            taskData.put("title", title);
            taskData.put("description", description);
            taskData.put("dueDate", dueDate);
            taskData.put("reminderDateTime", reminderDateTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                taskData,
                response -> {
                    Toast.makeText(this, "Task assigned!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Failed to assign task", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

}
