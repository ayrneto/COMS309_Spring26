package com.example.androidexample;

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

    private void fetchUserTasks(String userId) {
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/tasks";  // Check endpoint with backend

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

                addTaskCard(container, title, description, dueDate, taskId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addTaskCard(GridLayout container, String title, String description, String dueDate, long taskId) {
        View cardView = getLayoutInflater().inflate(R.layout.task_card, container, false);

        TextView titleView = cardView.findViewById(R.id.taskTitle);
        TextView descriptionView = cardView.findViewById(R.id.taskDescription);
        TextView dueDateView = cardView.findViewById(R.id.taskDueDate);

        titleView.setText(title);
        descriptionView.setText(description);
        dueDateView.setText("Due: " + dueDate);

        container.addView(cardView);
    }
}
