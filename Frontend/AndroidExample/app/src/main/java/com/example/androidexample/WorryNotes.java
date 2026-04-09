package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
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

        //Getting global values:
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userIdString = prefs.getString("USER_ID", "-1");
        Long userId = Long.parseLong(userIdString);
        FrameLayout btnAddWorryNote = findViewById(R.id.btnAddWorryNote);
        fetchUserNotes(userId);

        btnAddWorryNote.setOnClickListener(view -> {
            Intent intent = new Intent(WorryNotes.this, AddWorryActivity.class);
            startActivity(intent);
        });

    }

    private void fetchUserNotes(Long userId){
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/notes";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    displayNotes(response);
                },
                error -> {
                    Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }


    private void displayNotes(JSONArray notes){
        GridLayout container = findViewById(R.id.worriesContainer);
        container.removeAllViews(); // This resets all the worries in view

        try{
            for(int i = notes.length() - 1; i >= 0; i--){
                JSONObject note = notes.getJSONObject(i);

                long noteId = note.getLong("id");
                String title = note.getString("title");
                String content = note.getString("content");
                String dueDate = note.getString("dueDate");
                String label = note.getString("label");
                // Placeholders waiting for backend
                //String dueDate = "No date";
                //String label = "Label";

                addNoteCard(container, title, content, dueDate, label, noteId);
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void addNoteCard(GridLayout container, String title, String content, String dueDate, String label, long noteId){
        View cardView = getLayoutInflater().inflate(R.layout.note_card, container, false);

        TextView titleView = cardView.findViewById(R.id.worryTitle);
        TextView contentView = cardView.findViewById(R.id.worryContent);
        TextView dueDateView = cardView.findViewById(R.id.worryDueDate);
        TextView labelView = cardView.findViewById(R.id.worryLabel);

        titleView.setText(title);
        contentView.setText(content);
        dueDateView.setText("Due: " + dueDate);
        labelView.setText(label);

        // Makes the card summary clickable
        cardView.setOnClickListener(v -> {
            showEditDeleteDialog(noteId, title, content, dueDate, label);
        });

        container.addView(cardView);
    }

    // Methods for editing / deleting note popup:
    private void showEditDeleteDialog(long noteId, String title, String content, String dueDate, String label) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Action");

        builder.setPositiveButton("Edit", (dialog, which) -> {
            // Open AddWorryActivity with existing data
            Intent intent = new Intent(WorryNotes.this, AddWorryActivity.class);
            intent.putExtra("noteId", noteId);
            intent.putExtra("title", title);
            intent.putExtra("content", content);
            intent.putExtra("dueDate", dueDate);
            intent.putExtra("label", label);
            startActivity(intent);
        });

        builder.setNegativeButton("Delete", (dialog, which) -> {
            deleteNote(noteId);
        });

        builder.setNeutralButton("Cancel", null);

        builder.show();
    }

    private void deleteNote(long noteId) {
        String url = ApiConstants.BASE_URL + "/api/notes/" + noteId;

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                    // Refresh the list
                    SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
                    String userIdString = prefs.getString("USER_ID", "-1");
                    Long userId = Long.parseLong(userIdString);
                    fetchUserNotes(userId);
                },
                error -> {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    // Causes the WorryNotes page to refresh right away when updating or adding notes
    @Override
    protected void onResume() {
        super.onResume();

        // Refresh the notes list
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userIdString = prefs.getString("USER_ID", "-1");
        Long userId = Long.parseLong(userIdString);
        fetchUserNotes(userId);
    }


}
