package com.example.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class AssignPrescriptionActivity extends AppCompatActivity {

    private EditText etMedName, etDosage, etInstructions, etStartDate, etDurationDays;
    private Button   btnSubmit;
    private RequestQueue requestQueue;
    private long targetUserId;
    private String counsellorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_prescription);

        etMedName      = findViewById(R.id.etMedName);
        etDosage       = findViewById(R.id.etDosage);
        etInstructions = findViewById(R.id.etInstructions);
        etStartDate    = findViewById(R.id.etStartDate);
        etDurationDays = findViewById(R.id.etDurationDays);
        btnSubmit      = findViewById(R.id.btnSubmit);

        // Logged-in counsellor's user ID
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        counsellorId = prefs.getString("USER_ID", "1");

        // Target patient's user ID passed from UserProfileActivity
        targetUserId = getIntent().getLongExtra("TARGET_USER_ID", -1);
        String targetName = getIntent().getStringExtra("TARGET_USER_NAME");

        if (targetUserId == -1) {
            Toast.makeText(this, "No user selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (targetName != null && !targetName.isEmpty()) {
            ((android.widget.TextView) findViewById(R.id.tvPatientName))
                    .setText("Prescribing for: " + targetName);
        }

        requestQueue = Volley.newRequestQueue(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitPrescription());
    }

    private void submitPrescription() {
        String medName      = etMedName.getText().toString().trim();
        String dosage       = etDosage.getText().toString().trim();
        String instructions = etInstructions.getText().toString().trim();
        String startDate    = etStartDate.getText().toString().trim();
        String daysStr      = etDurationDays.getText().toString().trim();

        if (medName.isEmpty() || dosage.isEmpty() || startDate.isEmpty() || daysStr.isEmpty()) {
            Toast.makeText(this, "Please fill in medication name, dosage, start date and duration", Toast.LENGTH_SHORT).show();
            return;
        }

        int durationDays;
        try {
            durationDays = Integer.parseInt(daysStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Duration must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("medicationName", medName);
            body.put("dosage",         dosage);
            body.put("instructions",   instructions);
            body.put("startDate",      startDate);
            body.put("durationDays",   durationDays);

            // URL: /prescriptions/users/{patientUserId}?counsellorId={counsellorUserId}
            String url = ApiConstants.userPrescriptions(targetUserId)
                    + "?counsellorId=" + counsellorId;

            final String requestBody = body.toString();

            btnSubmit.setEnabled(false);

            StringRequest req = new StringRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        Toast.makeText(this, "Prescription assigned successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    error -> {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(this, "Failed to assign prescription", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public byte[] getBody() {
                    return requestBody.getBytes(StandardCharsets.UTF_8);
                }

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };

            requestQueue.add(req);

        } catch (Exception e) {
            Toast.makeText(this, "Error building request", Toast.LENGTH_SHORT).show();
            btnSubmit.setEnabled(true);
        }
    }
}