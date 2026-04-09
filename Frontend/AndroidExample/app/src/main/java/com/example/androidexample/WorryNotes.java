package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WorryNotes extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worry_notes);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        Long userId = Long.parseLong(prefs.getString("USER_ID", "-1"));

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Add note button
        FrameLayout btnAddWorryNote = findViewById(R.id.btnAddWorryNote);
        btnAddWorryNote.setOnClickListener(view ->
                startActivity(new Intent(WorryNotes.this, AddWorryActivity.class))
        );

        fetchUserNotes(userId);
    }

    private void fetchUserNotes(Long userId) {
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/notes";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> displayNotes(response),
                error -> Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show()
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void displayNotes(JSONArray notes) {
        GridLayout container = findViewById(R.id.worriesContainer);
        container.removeAllViews();

        try {
            for (int i = notes.length() - 1; i >= 0; i--) {
                JSONObject note = notes.getJSONObject(i);
                long   noteId  = note.getLong("id");
                String title   = note.getString("title");
                String content = note.getString("content");
                String dueDate = "No date";
                String label   = "Label";
                addNoteCard(container, title, content, dueDate, label, noteId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addNoteCard(GridLayout container, String title, String content,
                             String dueDate, String label, long noteId) {
        View cardView = getLayoutInflater().inflate(R.layout.note_card, container, false);

        ((TextView) cardView.findViewById(R.id.worryTitle)).setText(title);
        ((TextView) cardView.findViewById(R.id.worryContent)).setText(content);
        ((TextView) cardView.findViewById(R.id.worryDueDate)).setText("Due: " + dueDate);
        ((TextView) cardView.findViewById(R.id.worryLabel)).setText(label);

        cardView.setOnClickListener(v ->
                showEditDeleteDialog(noteId, title, content, dueDate, label));

        container.addView(cardView);
    }

    private void showEditDeleteDialog(long noteId, String title, String content,
                                      String dueDate, String label) {
        new AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setPositiveButton("Edit", (dialog, which) -> {
                    Intent intent = new Intent(WorryNotes.this, AddWorryActivity.class);
                    intent.putExtra("noteId",  noteId);
                    intent.putExtra("title",   title);
                    intent.putExtra("content", content);
                    intent.putExtra("dueDate", dueDate);
                    intent.putExtra("label",   label);
                    startActivity(intent);
                })
                .setNegativeButton("Delete", (dialog, which) -> deleteNote(noteId))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void deleteNote(long noteId) {
        String url = ApiConstants.BASE_URL + "/api/notes/" + noteId;

        StringRequest request = new StringRequest(
                Request.Method.DELETE, url,
                response -> {
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                    SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
                    fetchUserNotes(Long.parseLong(prefs.getString("USER_ID", "-1")));
                },
                error -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        fetchUserNotes(Long.parseLong(prefs.getString("USER_ID", "-1")));
    }
}