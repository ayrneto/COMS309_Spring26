package com.example.androidexample;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class EditProfile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editprofile);

        EditText usernameField = findViewById(R.id.editUsername);
        EditText passwordField = findViewById(R.id.editPassword);
        Button saveBtn = findViewById(R.id.btnSave);

        String userId;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userId = extras.getString("userId", "");

            String username = extras.getString("username", "");
            String password = extras.getString("password", "");

            usernameField.setText(username);
            passwordField.setText(password);
        } else {
            userId = "";
        }

        final String finalUserId = userId;

        saveBtn.setOnClickListener(v -> {
            String newUsername = usernameField.getText().toString();
            String newPassword = passwordField.getText().toString();

            updateProfile(finalUserId, newUsername, newPassword);
        });
    }

    private void updateProfile(String userId, String username, String password) {
        String url = ApiConstants.USERS + "/" + userId;

        JSONObject updateData = new JSONObject();
        try {
            updateData.put("username", username);
            updateData.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                updateData,
                response -> {
                    Toast.makeText(EditProfile.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    finish();  // Go back to profile page
                },
                error -> {
                    Toast.makeText(EditProfile.this, "Update failed!", Toast.LENGTH_SHORT).show();
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

}
