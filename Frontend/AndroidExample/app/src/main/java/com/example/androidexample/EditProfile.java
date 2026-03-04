package com.example.androidexample;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class EditProfile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editprofile);

        EditText nameField = findViewById(R.id.editName);
        EditText emailField = findViewById(R.id.editEmail);
        EditText passwordField = findViewById(R.id.editPassword);
        EditText confirmPasswordField = findViewById(R.id.editConfirmPassword);
        Button saveBtn = findViewById(R.id.btnSave);

        String userId;
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            userId = extras.getString("userId", "");

            String name = extras.getString("name", "");
            String email = extras.getString("email", "");
            String password = extras.getString("password", "");

            nameField.setText(name);
            emailField.setText(email);
            passwordField.setText(password);

            // confirmPasswordField.setText(password);
        } else {
            userId = "";
        }

        final String finalUserId = userId;

        saveBtn.setOnClickListener(v -> {
            String newName = nameField.getText().toString().trim();
            String newEmail = emailField.getText().toString().trim();
            String newPassword = passwordField.getText().toString().trim();
            String confirmPassword = confirmPasswordField.getText().toString().trim();

            if (finalUserId.isEmpty()) {
                Toast.makeText(this, "User ID missing", Toast.LENGTH_LONG).show();
                return;
            }

            if (newName.isEmpty() || newEmail.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            updateProfile(finalUserId, newName, newEmail, newPassword, confirmPassword);
        });
    }

    private void updateProfile(String userId, String name, String email, String password, String confirmPassword) {

        String url = ApiConstants.EDIT + userId;

        JSONObject updateData = new JSONObject();
        try {
            updateData.put("name", name);
            updateData.put("email", email);
            updateData.put("active", true);
            updateData.put("password", password);
        } catch (JSONException e) {
            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest request = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    if (response != null && response.toLowerCase().contains("success")) {
                        Toast.makeText(EditProfile.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditProfile.this, "Update failed: " + response, Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    String msg = "Update failed!";
                    if (error.networkResponse != null) msg += " HTTP " + error.networkResponse.statusCode;
                    Toast.makeText(EditProfile.this, msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public byte[] getBody() {
                return updateData.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}