package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText emailInput = findViewById(R.id.inputEmail);
        EditText passwordInput = findViewById(R.id.inputPassword);
        Button loginBtn = findViewById(R.id.btnLogin);
        LinearLayout signUpBtn = findViewById(R.id.btnSignUp);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString();

                sendLoginRequest(email, password);
            }
        });

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });
    }

    private void sendLoginRequest(String email, String password) {

        String url = ApiConstants.LOGIN;

        // Create JSON object with email and password
        JSONObject loginData = new JSONObject();
        try {
            loginData.put("email", email);
            loginData.put("password", password);
        } catch (JSONException e) {
            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                loginData,
                response -> {
                    try {
                        // Backend returns: { "id": ..., "email": ... }
                        String userId = String.valueOf(response.getLong("id"));
                        String emailReturned = response.getString("email");

                        SharedPreferences.Editor editor =
                                getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit();
                        editor.putString("USER_ID", userId);
                        editor.putString("USER_EMAIL", emailReturned);
                        editor.apply();

                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();

                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "Bad login response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {

                    if (error.networkResponse != null) {

                        int statusCode = error.networkResponse.statusCode;

                        if (statusCode == 401) {
                            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (statusCode == 403) {
                            Toast.makeText(this, "Account is inactive", Toast.LENGTH_LONG).show();
                            return;
                        }

                        Toast.makeText(this, "Login failed: HTTP " + statusCode, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Network error. Check connection.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}