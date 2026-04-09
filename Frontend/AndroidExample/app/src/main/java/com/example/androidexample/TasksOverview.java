package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TasksOverview extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks_overview);

        // Get userId from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");

        fetchUserTasks(userId);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");
        fetchUserTasks(userId);
    }

    private void fetchUserTasks(String userId) {
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/tasks";  // TODO: Check endpoint with backend

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    displayTasks(response);
                },
                error -> {
                    Toast.makeText(this, "Failed to load tasks", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }


    private void displayTasks(JSONArray tasks) {
        GridLayout container = findViewById(R.id.tasksContainer);
        container.removeAllViews();

        try {
            for (int i = tasks.length() - 1; i >= 0; i--) {
                JSONObject task = tasks.getJSONObject(i);

                long taskId = task.getLong("id");
                String title = task.getString("title");
                String description = task.getString("description");
                String dueDate = task.optString("dueDate", "No due date");
                String reminderDateTime = task.optString("reminderDateTime", "");
                String status = task.optString("status", "Not Started");

                addTaskCard(container, title, description, dueDate, status, taskId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addTaskCard(GridLayout container, String title, String description, String dueDate, String status, long taskId) {
        View cardView = getLayoutInflater().inflate(R.layout.task_card, container, false);

        TextView titleView = cardView.findViewById(R.id.taskTitle);
        TextView descriptionView = cardView.findViewById(R.id.taskDescription);
        TextView dueDateView = cardView.findViewById(R.id.taskDueDate);
        TextView statusView = cardView.findViewById(R.id.taskStatus);

        titleView.setText(title);
        descriptionView.setText(description);
        dueDateView.setText("Due: " + dueDate);
        statusView.setText(status);

        // Set background color based on status
        if (status.equals("Not Started")) {
            statusView.setBackgroundResource(R.drawable.red_background);
        } else if (status.equals("Ongoing")) {
            statusView.setBackgroundResource(R.drawable.yellow_background);
        } else if (status.equals("Completed")) {
            statusView.setBackgroundResource(R.drawable.green_background);
        }

        // Make card clickable
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(TasksOverview.this, TaskDetailsActivity.class);
            intent.putExtra("taskId", taskId);
            intent.putExtra("title", title);
            intent.putExtra("description", description);
            intent.putExtra("dueDate", dueDate);
            intent.putExtra("status", status);
            startActivity(intent);
        });

        container.addView(cardView);
    }
}
