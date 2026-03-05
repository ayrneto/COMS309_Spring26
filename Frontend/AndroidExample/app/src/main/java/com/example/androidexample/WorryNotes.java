package com.example.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
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
        Long userId = prefs.getLong("userId", -1);

        fetchUserNotes(userId);
    }

    private void fetchUserNotes(Long userId){
        String url = ApiConstants.USERS + "/" + "/notes";

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
        LinearLayout container = findViewById(R.id.)

        try{
            for(int i = 0; i < notes.length(); i++){
                JSONObject note = notes.getJSONObject(i);

                String title = note.getString("title");
                String content = note.getString("content");
                //ToDo: String dueDate = note.getString("dueDate");
                //ToDo: String label = note.getString("label");

                Log.d("Notes", "Title: " + title);
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }






}
