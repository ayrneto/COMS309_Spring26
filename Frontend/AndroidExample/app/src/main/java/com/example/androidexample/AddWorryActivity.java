package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.DatePickerDialog;
import java.util.Calendar;

public class AddWorryActivity extends AppCompatActivity{
    private boolean isEditMode = false;
    private long noteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_worry);

        EditText titleInput = findViewById(R.id.inputTitle);
        EditText contentInput = findViewById(R.id.inputContent);
        EditText dueDateInput = findViewById(R.id.inputDueDate);
        Spinner labelSpinner = findViewById(R.id.spinnerLabel);
        Button saveBtn = findViewById(R.id.btnSaveWorry);

        // Setup spinner
        String[] labels = {"Work", "Personal", "School", "Finance", "Social"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels);
        labelSpinner.setAdapter(adapter);

        // Check if editing existing note
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("noteId")) {
            isEditMode = true;
            noteId = extras.getLong("noteId");

            // Pre-fill the fields
            titleInput.setText(extras.getString("title"));
            contentInput.setText(extras.getString("content"));
            dueDateInput.setText(extras.getString("dueDate"));

            String label = extras.getString("label");
            int spinnerPosition = adapter.getPosition(label);
            if (spinnerPosition >= 0) {
                labelSpinner.setSelection(spinnerPosition);
            }

            saveBtn.setText("Update");  // Change button text
        }

        saveBtn.setOnClickListener(v -> {
            String title = titleInput.getText().toString();
            String content = contentInput.getText().toString();
            String dueDate = dueDateInput.getText().toString();
            String label = labelSpinner.getSelectedItem().toString();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Title and content required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEditMode) {
                updateWorry(title, content, dueDate, label);
            } else {
                saveWorry(title, content, dueDate, label);
            }
        });

        // Due Date Picker
        dueDateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddWorryActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format as YYYY-MM-DD (or whatever backend expects)
                        String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                        dueDateInput.setText(date);
                    },
                    year,
                    month,
                    day
            );

            datePickerDialog.show();
        });

    }

    // Used only for when saving a NEW Worry Note
    private void saveWorry(String title, String content, String dueDate, String label) {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");

        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/notes";

        JSONObject noteData = new JSONObject();
        try {
            noteData.put("title", title);
            noteData.put("content", content);
            //ToDo: add these after backend:
            //noteData.put("dueDate", dueDate);
            //noteData.put("label", label);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                noteData,
                response -> {
                    Toast.makeText(this, "Worry saved!", Toast.LENGTH_SHORT).show();
                    finish();  // Go back to WorryNotes page
                },
                error -> {
                    Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }


    // Used only when updating an existing note
    private void updateWorry(String title, String content, String dueDate, String label) {
        String url = ApiConstants.BASE_URL + "/api/notes/" + noteId;

        JSONObject noteData = new JSONObject();
        try {
            noteData.put("title", title);
            noteData.put("content", content);
            //ToDo: uncomment these after backend:
            //noteData.put("dueDate", dueDate);
            //noteData.put("label", label);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                noteData,
                response -> {
                    Toast.makeText(this, "Worry updated!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }


}
