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
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword, etName;
    private Button btnSignup;
    private Spinner roleSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etName            = findViewById(R.id.et_name);
        etEmail           = findViewById(R.id.et_email);
        etPassword        = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignup         = findViewById(R.id.btn_signup);
        TextView tvHint   = findViewById(R.id.tvHint);

        roleSpinner = findViewById(R.id.spinner_role);
        String[] roles = {"USER", "COUNSELLOR"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        roleSpinner.setAdapter(adapter);

        btnSignup.setOnClickListener(v -> attemptSignup());
        tvHint.setOnClickListener(v ->
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class))
        );
    }

    private void attemptSignup() {
        String name     = etName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();
        String role     = roleSpinner.getSelectedItem().toString();

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
            body.put("role", role);
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
                        String createdId = String.valueOf(response.getLong("id"));

                        SharedPreferences.Editor editor =
                                getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit();
                        editor.putString("USER_NAME",  name);
                        editor.putString("USER_EMAIL", email);
                        editor.putString("USER_ID",    createdId);
                        editor.putString("USER_ROLE",  role);

                        // Clear cached counselor profile from any previous user
                        editor.remove("COUNSELOR_DISPLAY_NAME");
                        editor.remove("COUNSELOR_SPECIALIZATION");
                        editor.remove("COUNSELOR_BIO");
                        editor.remove("COUNSELOR_PROFILE_PIC");
                        editor.remove("COUNSELOR_STATUS");

                        editor.apply();

                        Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();

                        if (role.equals("COUNSELLOR")) {
                            startActivity(new Intent(this, CounselorHomeActivity.class));
                        } else {
                            startActivity(new Intent(this, HomeActivity.class));
                        }
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error reading response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    String msg = "Signup failed";

                    if (error.networkResponse != null) {
                        int code = error.networkResponse.statusCode;
                        String bodyStr = new String(error.networkResponse.data);

                        if (code == 409) {
                            msg = "Email already registered. Try logging in.";
                        } else if (code == 400) {
                            msg = "HTTP 400: " + bodyStr;
                        } else {
                            msg = "HTTP " + code + ": " + bodyStr;
                        }
                    } else {
                        msg = error.toString();
                    }

                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }
}