package com.example.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskDetailsActivity extends AppCompatActivity {

    private long taskId;
    private String currentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        // Force light mode for colors to show correctly
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Get data from intent
        taskId = getIntent().getLongExtra("taskId", -1);
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String dueDate = getIntent().getStringExtra("dueDate");
        currentStatus = getIntent().getStringExtra("status");

        // Find views
        TextView titleView = findViewById(R.id.taskTitle);
        TextView descriptionView = findViewById(R.id.taskDescription);
        TextView dueDateView = findViewById(R.id.taskDueDate);
        Spinner statusSpinner = findViewById(R.id.spinnerStatus);

        // Display data
        titleView.setText(title);
        descriptionView.setText(description);
        dueDateView.setText("Due: " + dueDate);

        // Setup status spinner
        String[] statuses = {"Not Started", "Ongoing", "Completed"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
        statusSpinner.setAdapter(adapter);

        // Set current status
        int statusPosition = adapter.getPosition(currentStatus);
        statusSpinner.setSelection(statusPosition);

        // Handle status change
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newStatus = parent.getItemAtPosition(position).toString();
                if (!newStatus.equals(currentStatus)) {
                    updateTaskStatus(newStatus);
                    currentStatus = newStatus;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        Button backBtn = findViewById(R.id.btnBack);
        backBtn.setOnClickListener(v -> {
            finish();  // Goes back to previous activity
        });
    }

    private void updateTaskStatus(String newStatus) {
        String url = ApiConstants.BASE_URL + "/api/tasks/" + taskId + "/status";  // Check endpoint with backend

        JSONObject statusData = new JSONObject();
        try {
            statusData.put("status", newStatus);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                statusData,
                response -> {
                    Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}