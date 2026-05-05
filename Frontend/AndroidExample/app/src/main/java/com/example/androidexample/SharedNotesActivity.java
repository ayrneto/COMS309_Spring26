package com.example.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class SharedNotesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_notes);

        // Back button
        if (findViewById(R.id.btnBack) != null)
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        fetchSharedNotes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchSharedNotes();
    }

    private void fetchSharedNotes() {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String counsellorId = prefs.getString("USER_ID", "-1");

        String url = ApiConstants.BASE_URL + "/api/counsellors/" + counsellorId + "/shared-notes";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    displaySharedNotes(response);
                },
                error -> {
                    Toast.makeText(this, "Failed to load shared notes", Toast.LENGTH_SHORT).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void displaySharedNotes(org.json.JSONArray notes) {
        GridLayout container = findViewById(R.id.sharedNotesContainer);
        container.removeAllViews();

        try {
            for (int i = notes.length() - 1; i >= 0; i--) {
                JSONObject note = notes.getJSONObject(i);

                String noteId = note.getString("id");
                String title = note.getString("title");
                String content = note.getString("content");
                String dueDate = note.optString("dueDate", "No due date");
                String label = note.optString("label", "");
                String userId = note.getString("userId");
                String userName = note.optString("userName", "Patient #" + userId);

                addSharedNoteCard(container, noteId, title, content, dueDate, label, userName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addSharedNoteCard(GridLayout container, String noteId, String title,
                                   String content, String dueDate, String label, String userName) {
        View cardView = getLayoutInflater().inflate(R.layout.shared_note_card, container, false);

        TextView sharedByText = cardView.findViewById(R.id.sharedByText);
        TextView titleView = cardView.findViewById(R.id.noteTitle);
        TextView contentView = cardView.findViewById(R.id.noteContent);
        TextView dueDateView = cardView.findViewById(R.id.noteDueDate);
        TextView labelView = cardView.findViewById(R.id.noteLabel);

        sharedByText.setText("Shared by: " + userName);
        titleView.setText(title);
        contentView.setText(content);
        dueDateView.setText("Due: " + dueDate);
        labelView.setText(label);

        cardView.setOnClickListener(v -> {
            showNoteDetails(title, content, dueDate, label, userName);
        });

        container.addView(cardView);
    }

    private void showNoteDetails(String title, String content, String dueDate, String label, String userName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(
                "Shared by: " + userName + "\n\n" +  // Changed
                        content + "\n\n" +
                        "Due: " + dueDate + "\n" +
                        "Label: " + label
        );
        builder.setPositiveButton("Close", null);
        builder.show();
    }
}