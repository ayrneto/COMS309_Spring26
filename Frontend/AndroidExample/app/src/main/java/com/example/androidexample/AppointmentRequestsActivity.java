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
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
    // Shrey's backend returns status as "PENDING" / "CONFIRMED" / "CANCELLED"
    // Frontend shows only PENDING ones
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
    // Shrey's backend sets status = "CONFIRMED"
    // ─────────────────────────────────────────────────────────────────────────
    private void acceptRequest(AppointmentRequest r) {
        String url = ApiConstants.BASE_URL + "/api/appointments/" + r.id + "/accept";

        StringRequest req = new StringRequest(
                Request.Method.PATCH, url,
                response -> onAcceptSuccess(r),
                error -> {
                    if (error.networkResponse != null &&
                            (error.networkResponse.statusCode == 200 ||
                                    error.networkResponse.statusCode == 201 ||
                                    error.networkResponse.statusCode == 204)) {
                        onAcceptSuccess(r);
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

    private void onAcceptSuccess(AppointmentRequest r) {
        Toast.makeText(this,
                "Accepted! You can now chat with " + r.userName,
                Toast.LENGTH_SHORT).show();
        requestList.remove(r);
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/appointments/{id}/decline
    // Shrey's backend sets status = "CANCELLED"
    // ─────────────────────────────────────────────────────────────────────────
    private void declineRequest(AppointmentRequest r) {
        String url = ApiConstants.BASE_URL + "/api/appointments/" + r.id + "/decline";

        StringRequest req = new StringRequest(
                Request.Method.PATCH, url,
                response -> onDeclineSuccess(r),
                error -> {
                    if (error.networkResponse != null &&
                            (error.networkResponse.statusCode == 200 ||
                                    error.networkResponse.statusCode == 204)) {
                        onDeclineSuccess(r);
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

    private void onDeclineSuccess(AppointmentRequest r) {
        Toast.makeText(this, "Request declined", Toast.LENGTH_SHORT).show();
        requestList.remove(r);
        adapter.notifyDataSetChanged();
        updateEmptyState();
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