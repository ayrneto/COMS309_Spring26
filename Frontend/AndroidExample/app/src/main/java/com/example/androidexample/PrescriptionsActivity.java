package com.example.androidexample;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class PrescriptionsActivity extends AppCompatActivity {

    private LinearLayout prescriptionsContainer;
    private TextView tvEmpty;
    private RequestQueue requestQueue;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescriptions);

        prescriptionsContainer = findViewById(R.id.prescriptionsContainer);
        tvEmpty                = findViewById(R.id.tvEmpty);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        userId = prefs.getString("USER_ID", "1");

        requestQueue = Volley.newRequestQueue(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadPrescriptions();
    }

    // ── Load all prescriptions for this user ──────────────────────────────────

    private void loadPrescriptions() {
        String url = ApiConstants.userPrescriptions(Long.parseLong(userId));

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> renderPrescriptions(response),
                error  -> Toast.makeText(this, "Failed to load prescriptions", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(req);
    }

    private void renderPrescriptions(JSONArray data) {
        prescriptionsContainer.removeAllViews();

        if (data.length() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvEmpty.setVisibility(View.GONE);

        for (int i = 0; i < data.length(); i++) {
            try {
                JSONObject p = data.getJSONObject(i);

                String medicationName = p.optString("medicationName", "Unknown");
                String dosage         = p.optString("dosage", "—");
                String instructions   = p.optString("instructions", "—");
                String startDate      = p.optString("startDate", "—");
                String endDate        = p.optString("endDate", "—");
                int    durationDays   = p.optInt("durationDays", 0);
                boolean active        = p.optBoolean("active", false);

                View row = LayoutInflater.from(this)
                        .inflate(R.layout.item_prescription, prescriptionsContainer, false);

                ((TextView) row.findViewById(R.id.tvMedName)).setText(medicationName);
                ((TextView) row.findViewById(R.id.tvDosage)).setText("Dosage: " + dosage);
                ((TextView) row.findViewById(R.id.tvInstructions)).setText("Instructions: " + instructions);
                ((TextView) row.findViewById(R.id.tvDates)).setText("From " + startDate + " to " + endDate
                        + (durationDays > 0 ? " (" + durationDays + " days)" : ""));

                TextView tvStatus = row.findViewById(R.id.tvStatus);
                if (active) {
                    tvStatus.setText("ACTIVE");
                    tvStatus.setBackgroundColor(Color.parseColor("#3A7D55"));
                } else {
                    tvStatus.setText("INACTIVE");
                    tvStatus.setBackgroundColor(Color.parseColor("#9E9E9E"));
                }

                prescriptionsContainer.addView(row);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}