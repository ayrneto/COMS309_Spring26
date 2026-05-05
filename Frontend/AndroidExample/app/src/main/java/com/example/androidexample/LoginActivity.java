package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

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

        // Force light mode for colors to show correctly
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        EditText emailInput    = findViewById(R.id.inputEmail);
        EditText passwordInput = findViewById(R.id.inputPassword);
        Button loginBtn        = findViewById(R.id.btnLogin);
        LinearLayout signUpBtn = findViewById(R.id.btnSignUp);
        roleSpinner            = findViewById(R.id.spinner_role);

        // Request notification permission on app start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

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
                        String nameReturned  = response.optString("name", "");

                        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);

                        // Read preserved values BEFORE clearing
                        String nameFromServer = response.optString("name", "");
                        String savedName      = prefs.getString("USER_NAME", "");
                        String finalName      = nameFromServer.isEmpty() ? savedName : nameFromServer;

                        String picFromServer  = response.optString("profilePicture", "");
                        String savedPic       = prefs.getString("USER_PIC_URL", "");
                        String finalPic       = picFromServer.isEmpty() ? savedPic : picFromServer;

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.clear();

                        editor.putString("USER_ID",      userId);
                        editor.putString("USER_EMAIL",   emailReturned);
                        editor.putString("USER_ROLE",    returnedRole);
                        editor.putString("USER_NAME",    finalName);
                        editor.putString("USER_PIC_URL", finalPic);
                        editor.apply();

                        // For COUNSELLOR — fetch profile first, THEN navigate
                        // so all SharedPrefs are populated before home screen loads
                        if (!"COUNSELLOR".equals(returnedRole)) {
                            navigateAfterLogin(returnedRole);
                        } else {
                            fetchCounsellorProfileThenNavigate(userId);
                        }

                        // Start WebSocket service
                        Intent serviceIntent = new Intent(LoginActivity.this, WebSocketService.class);
                        startService(serviceIntent);

                        // After starting WebSocket service
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                            }
                        }

                        if ("COUNSELLOR".equals(returnedRole)) return; // handled above

                        // Route based on role
                        switch (returnedRole) {
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
    // Navigate to correct home screen based on role
    private void navigateAfterLogin(String role) {
        switch (role) {
            case "COUNSELLOR":
                startActivity(new android.content.Intent(this, CounselorHomeActivity.class));
                break;
            case "ADMIN":
                startActivity(new android.content.Intent(this, AdminDashboardActivity.class));
                break;
            default:
                startActivity(new android.content.Intent(this, HomeActivity.class));
                break;
        }
        finish();
    }

    // Fetch counsellor profile THEN navigate — ensures all prefs are set before home loads
    private void fetchCounsellorProfileThenNavigate(String userId) {
        String url = ApiConstants.BASE_URL + "/api/counsellors/" + userId + "/profile";

        com.android.volley.toolbox.JsonObjectRequest req =
                new com.android.volley.toolbox.JsonObjectRequest(
                        com.android.volley.Request.Method.GET, url, null,
                        response -> {
                            getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit()
                                    .putString("COUNSELOR_DISPLAY_NAME",   response.optString("displayName",       ""))
                                    .putString("COUNSELOR_SPECIALIZATION", response.optString("specialization",    ""))
                                    .putString("COUNSELOR_BIO",            response.optString("bio",               ""))
                                    .putString("COUNSELOR_PROFILE_PIC",    response.optString("profilePictureUrl", ""))
                                    .putString("COUNSELOR_STATUS",         response.optString("status",            "AVAILABLE"))
                                    .apply();
                            // Navigate only after profile is saved
                            startActivity(new android.content.Intent(this, CounselorHomeActivity.class));
                            finish();
                        },
                        error -> {
                            // Profile fetch failed — navigate anyway, fields will be empty
                            startActivity(new android.content.Intent(this, CounselorHomeActivity.class));
                            finish();
                        }
                );

        com.android.volley.toolbox.Volley.newRequestQueue(this).add(req);
    }


}