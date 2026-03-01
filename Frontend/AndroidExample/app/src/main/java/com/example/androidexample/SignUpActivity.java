package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword, etName;
    private Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignup = findViewById(R.id.btn_signup);
        TextView tvHint = findViewById(R.id.tvHint);
        btnSignup.setOnClickListener(v -> attemptSignup());
        tvHint.setOnClickListener(v -> {              // 👈 ADD THIS
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
        });
    }

    private void attemptSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        if (name.isEmpty()) {
            etName.setError("Enter your name");
            etName.requestFocus();
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("email", email);
            body.put("password", password);
            body.put("confirmPassword", confirm);
        } catch (JSONException e) {
            Toast.makeText(this, "JSON error", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                ApiConstants.SIGNUP,
                body,
                response -> {
                    try {
                        String createdId = response.getString("id");  // get ID from Mockoon

                        SharedPreferences.Editor editor =
                                getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit();

                        editor.putString("USER_EMAIL", email);
                        editor.putString("USER_ID", createdId);   // SAVE THE ID
                        editor.apply();

                        Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(this, HomeActivity.class));
                        finish();

                    } catch (Exception e) {
                        Toast.makeText(this, "Error reading response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this,
                        "Signup failed: " + (error.getMessage() != null ? error.getMessage() : "unknown"),
                        Toast.LENGTH_LONG).show()
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void saveUserId(String id) {
        SharedPreferences sp = getSharedPreferences("app_prefs", MODE_PRIVATE);
        sp.edit().putString("user_id", id).apply();
    }
}