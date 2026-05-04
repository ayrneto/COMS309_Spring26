package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

import android.app.DatePickerDialog;
import java.util.Calendar;
import android.util.Log;

public class AddWorryActivity extends AppCompatActivity {

    private boolean isEditMode = false;
    private long noteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_worry);

        // Force light mode for colors to show correctly
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        EditText titleInput   = findViewById(R.id.inputTitle);
        EditText contentInput = findViewById(R.id.inputContent);
        EditText dueDateInput = findViewById(R.id.inputDueDate);
        Spinner  labelSpinner = findViewById(R.id.spinnerLabel);
        Button   saveBtn      = findViewById(R.id.btnSaveWorry);
        TextView tvTopTitle   = findViewById(R.id.tvTopBarTitle);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Spinner
        String[] labels = {"Work", "Personal", "School", "Finance", "Social"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels);
        labelSpinner.setAdapter(adapter);

        // Edit mode pre-fill
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("noteId")) {
            isEditMode = true;
            noteId = extras.getLong("noteId");

            // Pre-fill the fields
            titleInput.setText(extras.getString("title"));
            contentInput.setText(extras.getString("content"));
            dueDateInput.setText(extras.getString("dueDate"));

            String label = extras.getString("label");
            int pos = adapter.getPosition(label);
            if (pos >= 0) labelSpinner.setSelection(pos);

            saveBtn.setText("Update");
            tvTopTitle.setText("Edit Worry Note");
        }

        saveBtn.setOnClickListener(v -> {
            String title   = titleInput.getText().toString();
            String content = contentInput.getText().toString();
            String dueDate = dueDateInput.getText().toString();
            String label   = labelSpinner.getSelectedItem().toString();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Title and content required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEditMode) updateWorry(title, content, dueDate, label);
            else            saveWorry(title, content, dueDate, label);
        });

        // Date picker
        dueDateInput.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, day) -> dueDateInput.setText(
                            String.format("%04d-%02d-%02d", year, month + 1, day)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    // Used only for when saving a NEW Worry Note
    private void saveWorry(String title, String content, String dueDate, String label) {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");
        String url    = ApiConstants.BASE_URL + "/api/users/" + userId + "/notes";

        JSONObject noteData = new JSONObject();
        try {
            noteData.put("title", title);
            noteData.put("content", content);
            noteData.put("label", label);

            // Only send dueDate if user picked a date
            if (!dueDate.isEmpty()) {
                noteData.put("dueDate", dueDate);
            }

            Log.d("AddWorry", "Sending JSON: " + noteData.toString());

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
            noteData.put("dueDate", dueDate);
            noteData.put("label", label);
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
