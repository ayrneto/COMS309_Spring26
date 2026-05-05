package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import org.json.JSONException;
import org.json.JSONObject;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorryNotes extends AppCompatActivity {

    private List<JSONObject> allNotes = new ArrayList<>();
    private Spinner filterSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worry_notes);

        filterSpinner = findViewById(R.id.filterSpinner);
        setupFilter();

        //Getting global values:
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
                response -> {
                    allNotes.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            allNotes.add(response.getJSONObject(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    sortAndDisplay(filterSpinner.getSelectedItemPosition());
                },
                error -> Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show()
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void displayNotes(List<JSONObject> notes) {
        GridLayout container = findViewById(R.id.worriesContainer);
        container.removeAllViews();

        for (JSONObject note : notes) {
            try {
                long noteId = note.getLong("id");
                String title = note.getString("title");
                String content = note.getString("content");
                String dueDate = note.getString("dueDate");
                String label = note.getString("label");

                addNoteCard(container, title, content, dueDate, label, noteId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
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

        builder.setNeutralButton("Share", (dialog, which) -> {
            // Open counselor selection page
            Intent intent = new Intent(WorryNotes.this, SelectCounselorActivity.class);
            intent.putExtra("noteId", noteId);
            startActivity(intent);
        });

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

    private void setupFilter() {
        String[] filterOptions = {"By Date (Newest)", "By Label (A-Z)", "By Due Date"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, filterOptions);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortAndDisplay(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void sortAndDisplay(int filterType) {
        List<JSONObject> sorted = new ArrayList<>(allNotes);

        switch (filterType) {
            case 0: // By Date (Newest first)
                Collections.reverse(sorted);
                break;

            case 1: // By Label (A-Z), then by date
                Collections.sort(sorted, (a, b) -> {
                    try {
                        String labelA = a.getString("label");
                        String labelB = b.getString("label");
                        int labelCompare = labelA.compareTo(labelB);
                        if (labelCompare != 0) {
                            return labelCompare;
                        }
                        return b.getInt("id") - a.getInt("id");
                    } catch (JSONException e) {
                        return 0;
                    }
                });
                break;

            case 2: // By Due Date (earliest first)
                Collections.sort(sorted, (a, b) -> {
                    try {
                        String dateA = a.optString("dueDate", "9999-12-31");
                        String dateB = b.optString("dueDate", "9999-12-31");
                        return dateA.compareTo(dateB);
                    } catch (Exception e) {
                        return 0;
                    }
                });
                break;
        }

        displayNotes(sorted);
    }


}
