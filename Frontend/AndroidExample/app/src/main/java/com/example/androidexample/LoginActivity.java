package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private Spinner roleSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText emailInput    = findViewById(R.id.inputEmail);
        EditText passwordInput = findViewById(R.id.inputPassword);
        Button loginBtn        = findViewById(R.id.btnLogin);
        LinearLayout signUpBtn = findViewById(R.id.btnSignUp);
        roleSpinner            = findViewById(R.id.spinner_role);

        String[] roles = {"USER", "COUNSELLOR", "ADMIN"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        roleSpinner.setAdapter(adapter);

        loginBtn.setOnClickListener(v -> {
            String email    = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            String role     = roleSpinner.getSelectedItem().toString();

            if (email.isEmpty()) {
                emailInput.setError("Enter your email");
                emailInput.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                passwordInput.setError("Enter your password");
                passwordInput.requestFocus();
                return;
            }

            sendLoginRequest(email, password, role);
        });

        signUpBtn.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class))
        );
    }

    private void sendLoginRequest(String email, String password, String role) {
        String url = ApiConstants.LOGIN;

        // Create JSON object with email and password
        JSONObject loginData = new JSONObject();
        try {
            loginData.put("email",    email);
            loginData.put("password", password);
            loginData.put("role",     role);
        } catch (JSONException e) {
            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Create request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                loginData,
                response -> {
                    try {
                        String userId        = String.valueOf(response.getLong("id"));
                        String emailReturned = response.getString("email");
                        String returnedRole  = response.getString("role");

                        SharedPreferences.Editor editor =
                                getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit();

                        // Clear ALL previous session data first
                        editor.clear();

                        editor.putString("USER_ID",    userId);
                        editor.putString("USER_EMAIL", emailReturned);
                        editor.putString("USER_ROLE",  returnedRole);
                        editor.apply();

                        // Start WebSocket service
                        Intent serviceIntent = new Intent(LoginActivity.this, WebSocketService.class);
                        startService(serviceIntent);

                        // Route based on role
                        switch (returnedRole) {
                            case "COUNSELLOR":
                                startActivity(new Intent(this, CounselorHomeActivity.class));
                                break;
                            case "ADMIN":
                                startActivity(new Intent(this, AdminDashboardActivity.class));
                                break;
                            default:
                                startActivity(new Intent(this, HomeActivity.class));
                                break;
                        }
                        finish();

                    } catch (Exception e) {
                        Toast.makeText(this,
                                "Bad login response: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        if (statusCode == 401) {
                            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show();
                        } else if (statusCode == 403) {
                            Toast.makeText(this, "Account is inactive", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Login failed: HTTP " + statusCode, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Network error. Check connection.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // Add to request queue
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}