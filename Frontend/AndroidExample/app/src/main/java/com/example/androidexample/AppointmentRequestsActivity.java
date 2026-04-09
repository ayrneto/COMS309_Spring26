package com.example.androidexample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AppointmentRequestsActivity
 *
 * Counselor sees all PENDING appointment requests.
 * They can Accept or Decline each one.
 *
 * On Accept:
 *   - PATCH /api/appointments/{id}/accept  → sets status to ACCEPTED
 *   - Saves the user's ID + name to SharedPreferences so Chat works
 *
 * On Decline:
 *   - PATCH /api/appointments/{id}/decline → sets status to DECLINED
 *
 * Launched from CounselorHomeActivity drawer → "Appointment Requests"
 *
 * Tell Shrey:
 *   GET  /api/appointments/counsellor/{counsellorId}  → list of appointments for this counselor
 *   PATCH /api/appointments/{id}/accept               → set status=ACCEPTED, create assignment
 *   PATCH /api/appointments/{id}/decline              → set status=DECLINED
 */
public class AppointmentRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerRequests;
    private TextView tvRequestCount, tvEmpty;

    private final List<AppointmentRequest> requestList = new ArrayList<>();
    private RequestAdapter adapter;

    private long myCounselorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_requests);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        myCounselorId = Long.parseLong(prefs.getString("USER_ID", "-1"));

        recyclerRequests = findViewById(R.id.recyclerRequests);
        tvRequestCount   = findViewById(R.id.tvRequestCount);
        tvEmpty          = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new RequestAdapter(requestList,
                this::acceptRequest,
                this::declineRequest);
        recyclerRequests.setLayoutManager(new LinearLayoutManager(this));
        recyclerRequests.setAdapter(adapter);

        fetchRequests();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/appointments/counsellor/{counsellorId}
    // Returns all appointments for this counselor (any status)
    // Frontend shows only PENDING ones
    // Expected JSON array:
    // [
    //   { "id": 1, "userId": 3, "userName": "Test User 1",
    //     "date": "2026-04-10", "timeSlot": "2:00 PM",
    //     "notes": "Feeling anxious", "status": "PENDING" },
    //   ...
    // ]
    // ─────────────────────────────────────────────────────────────────────────
    private void fetchRequests() {
        String url = ApiConstants.BASE_URL +
                "/api/appointments/counsellor/" + myCounselorId;

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    requestList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            String status = obj.optString("status", "PENDING");
                            // Only show PENDING requests
                            if (!"PENDING".equalsIgnoreCase(status)) continue;

                            AppointmentRequest r = new AppointmentRequest();
                            r.id       = obj.getLong("id");
                            r.userId   = obj.getLong("userId");
                            r.userName = obj.optString("userName",  "Unknown User");
                            r.date     = obj.optString("date",      "");
                            r.timeSlot = obj.optString("timeSlot",  "");
                            r.notes    = obj.optString("notes",     "");
                            r.status   = status;
                            requestList.add(r);
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                },
                error -> {
                    Toast.makeText(this, "Failed to load requests", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/appointments/{id}/accept
    // Backend should:
    //   1. Set appointment status = ACCEPTED
    //   2. Create a UserCounsellorAssignment (userId ↔ counsellorId)
    // ─────────────────────────────────────────────────────────────────────────
    private void acceptRequest(AppointmentRequest r) {
        String url = ApiConstants.BASE_URL + "/api/appointments/" + r.id + "/accept";

        StringRequest req = new StringRequest(
                Request.Method.PATCH, url,
                response -> {
                    // Save assigned user to SharedPreferences so Chat works immediately
                    getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit()
                            .putLong("ASSIGNED_USER_ID",    r.userId)
                            .putString("ASSIGNED_USER_NAME", r.userName)
                            .apply();

                    Toast.makeText(this,
                            "Accepted! You can now chat with " + r.userName,
                            Toast.LENGTH_SHORT).show();

                    // Remove from list
                    requestList.remove(r);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                },
                error -> {
                    // Treat 200/204 as success (Volley quirk with no-body responses)
                    if (error.networkResponse != null &&
                            (error.networkResponse.statusCode == 200 ||
                                    error.networkResponse.statusCode == 204)) {
                        getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit()
                                .putLong("ASSIGNED_USER_ID",    r.userId)
                                .putString("ASSIGNED_USER_NAME", r.userName)
                                .apply();
                        Toast.makeText(this,
                                "Accepted! You can now chat with " + r.userName,
                                Toast.LENGTH_SHORT).show();
                        requestList.remove(r);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        return;
                    }
                    String msg = "Failed to accept";
                    if (error.networkResponse != null)
                        msg += " (HTTP " + error.networkResponse.statusCode + ")";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/appointments/{id}/decline
    // ─────────────────────────────────────────────────────────────────────────
    private void declineRequest(AppointmentRequest r) {
        String url = ApiConstants.BASE_URL + "/api/appointments/" + r.id + "/decline";

        StringRequest req = new StringRequest(
                Request.Method.PATCH, url,
                response -> {
                    Toast.makeText(this, "Request declined", Toast.LENGTH_SHORT).show();
                    requestList.remove(r);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                },
                error -> {
                    if (error.networkResponse != null &&
                            (error.networkResponse.statusCode == 200 ||
                                    error.networkResponse.statusCode == 204)) {
                        Toast.makeText(this, "Request declined", Toast.LENGTH_SHORT).show();
                        requestList.remove(r);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        return;
                    }
                    String msg = "Failed to decline";
                    if (error.networkResponse != null)
                        msg += " (HTTP " + error.networkResponse.statusCode + ")";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void updateEmptyState() {
        int count = requestList.size();
        tvRequestCount.setText(count + " pending request" + (count == 1 ? "" : "s"));
        tvEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        recyclerRequests.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    // ── Model ─────────────────────────────────────────────────────────────────
    static class AppointmentRequest {
        long   id, userId;
        String userName, date, timeSlot, notes, status;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    interface OnAccept  { void accept(AppointmentRequest r); }
    interface OnDecline { void decline(AppointmentRequest r); }

    static class RequestAdapter
            extends RecyclerView.Adapter<RequestAdapter.VH> {

        private final List<AppointmentRequest> list;
        private final OnAccept  onAccept;
        private final OnDecline onDecline;

        RequestAdapter(List<AppointmentRequest> list,
                       OnAccept onAccept, OnDecline onDecline) {
            this.list      = list;
            this.onAccept  = onAccept;
            this.onDecline = onDecline;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_appointment_request_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            AppointmentRequest r = list.get(pos);

            h.tvUserName.setText(r.userName);
            h.tvDateTime.setText(r.date + "  ·  " + r.timeSlot);
            h.tvNotes.setText(r.notes.isEmpty() ? "No notes" : r.notes);
            h.tvStatus.setText(r.status);

            h.btnAccept.setOnClickListener(v  -> onAccept.accept(r));
            h.btnDecline.setOnClickListener(v -> onDecline.decline(r));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvUserName, tvDateTime, tvNotes, tvStatus;
            com.google.android.material.button.MaterialButton btnAccept, btnDecline;

            VH(View v) {
                super(v);
                tvUserName = v.findViewById(R.id.tvUserName);
                tvDateTime = v.findViewById(R.id.tvDateTime);
                tvNotes    = v.findViewById(R.id.tvNotes);
                tvStatus   = v.findViewById(R.id.tvStatus);
                btnAccept  = v.findViewById(R.id.btnAccept);
                btnDecline = v.findViewById(R.id.btnDecline);
            }
        }
    }
}