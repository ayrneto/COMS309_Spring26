package com.example.androidexample;
import com.example.androidexample.ApiConstants;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvEmail;
    private Button btnDelete, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        // Grabbing info from previous page:
        Bundle extras = getIntent().getExtras();
        String username = extras.getString("username");
        String password = extras.getString("password");


        tvEmail = findViewById(R.id.tvEmail);
        btnDelete = findViewById(R.id.btn_delete_profile);
        btnBack = findViewById(R.id.btn_back_home);
        Button btnEditProfile = findViewById(R.id.btnEditProfile);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String email = prefs.getString("USER_EMAIL", "");
        String userId = prefs.getString("USER_ID", ""); // IMPORTANT: saved after signup

        tvEmail.setText(email == null || email.isEmpty() ? "Email: (not found)" : "Email: " + email);

        btnBack.setOnClickListener(v -> finish());

        btnDelete.setOnClickListener(v -> {
            if (userId == null || userId.trim().isEmpty()) {
                Toast.makeText(this, "User ID missing. Save ID after signup first.", Toast.LENGTH_LONG).show();
                return;
            }

            new AlertDialog.Builder(ProfileActivity.this)
                    .setTitle("Delete Profile?")
                    .setMessage("Are you sure you want to delete your account? This cannot be undone.")
                    .setPositiveButton("Yes, Delete", (dialog, which) -> deleteUser(userId))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, EditProfile.class);
                startActivity(intent);
                // Pass the user's info:
                intent.putExtra("email", username);
                intent.putExtra("password", password);
            }
        });
    }

    private void deleteUser(String userId) {
        String url = ApiConstants.DELETE + "/" + userId;

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    // Clear local session
                    SharedPreferences.Editor editor = getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit();
                    editor.clear();
                    editor.apply();

                    Toast.makeText(ProfileActivity.this, "Profile deleted", Toast.LENGTH_SHORT).show();

                    // Back to signup
                    Intent i = new Intent(ProfileActivity.this, SignUpActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finishAffinity();
                },
                error -> Toast.makeText(ProfileActivity.this,
                        "Delete failed: " + (error.getMessage() != null ? error.getMessage() : "unknown error"),
                        Toast.LENGTH_LONG).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
}