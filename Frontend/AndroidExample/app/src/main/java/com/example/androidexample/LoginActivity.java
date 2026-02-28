package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

        EditText usernameInput = findViewById(R.id.inputUsername); // EditText is the default text entry
        EditText passwordInput = findViewById(R.id.inputPassword);
        Button loginBtn = findViewById(R.id.btnLogin);
        LinearLayout SignUpBtn = findViewById(R.id.btnSignUp);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString();
                String password = passwordInput.getText().toString();

                // Sending to Mockoon API
                sendLoginRequest(username, password);
            }
        });

        // Go to SignUp page on click
        SignUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void sendLoginRequest(String username, String password){
        String url = "http://10.0.2.2:3000/api/login";  // Mockoon port

        // Create JSON object with username and password
        JSONObject loginData = new JSONObject();
        try {
            loginData.put("username", username);
            loginData.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Create request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                loginData,
                response -> {
                    // Success
                    Log.d("Login", "Success: " + response.toString());
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    // Error
                    Log.e("Login", "Error: " + error.toString());
                    Toast.makeText(LoginActivity.this, "Login failed!", Toast.LENGTH_SHORT).show();
                }
        );

        // Add to request queue
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}