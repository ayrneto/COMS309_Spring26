package com.example.androidexample;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CounsellorSearchActivity extends AppCompatActivity {

    private static final String[] TIME_SLOTS = {
            "9:00 AM", "10:00 AM", "11:00 AM",
            "1:00 PM", "2:00 PM",  "3:00 PM",
            "4:00 PM", "5:00 PM"
    };

    private RecyclerView recyclerCounsellors;
    private TextView tvResultCount;
    private EditText etSearch;
    private Spinner spinnerStatus, spinnerRating;

    private final List<CounsellorItem> allCounsellors = new ArrayList<>();
    private final List<CounsellorItem> filteredList   = new ArrayList<>();
    private CounsellorAdapter adapter;

    private String filterStatus   = "ALL";
    private double filterMinRating = 0.0;
    private String searchQuery    = "";

    private long myUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counsellor_search);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        myUserId = Long.parseLong(prefs.getString("USER_ID", "-1"));

        recyclerCounsellors = findViewById(R.id.recyclerCounsellors);
        tvResultCount       = findViewById(R.id.tvResultCount);
        etSearch            = findViewById(R.id.etSearch);
        spinnerStatus       = findViewById(R.id.spinnerStatus);
        spinnerRating       = findViewById(R.id.spinnerRating);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new CounsellorAdapter(filteredList, this::openBookingDialog);
        recyclerCounsellors.setLayoutManager(new LinearLayoutManager(this));
        recyclerCounsellors.setAdapter(adapter);

        // Status spinner
        String[] statusOptions = {"All Statuses", "Available", "Busy"};
        spinnerStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, statusOptions));
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterStatus = pos == 0 ? "ALL" : pos == 1 ? "AVAILABLE" : "BUSY";
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Rating spinner
        String[] ratingOptions = {"Any Rating", "1★ & above", "2★ & above", "3★ & above", "4★ & above"};
        spinnerRating.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, ratingOptions));
        spinnerRating.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterMinRating = pos;
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Search watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                searchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fetchCounsellors();
    }

    private void fetchCounsellors() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                ApiConstants.COUNSELLORS,
                null,
                response -> {
                    allCounsellors.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            allCounsellors.add(parseCounsellor(response.getJSONObject(i)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    applyFilters();
                },
                error -> Toast.makeText(this, "Failed to load counsellors", Toast.LENGTH_SHORT).show()
        );
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void applyFilters() {
        filteredList.clear();
        for (CounsellorItem c : allCounsellors) {
            if (!searchQuery.isEmpty()) {
                boolean nameMatch = c.displayName.toLowerCase().contains(searchQuery);
                boolean specMatch = c.specialization.toLowerCase().contains(searchQuery);
                if (!nameMatch && !specMatch) continue;
            }
            if (!filterStatus.equals("ALL") && !c.status.equalsIgnoreCase(filterStatus)) continue;
            if (c.ratingAverage < filterMinRating) continue;
            filteredList.add(c);
        }
        adapter.notifyDataSetChanged();
        tvResultCount.setText(filteredList.size() + " counsellor" +
                (filteredList.size() == 1 ? "" : "s") + " found");
    }

    private void openBookingDialog(CounsellorItem counsellor) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_book_appointment, null);

        TextView tvName     = dialogView.findViewById(R.id.tvDialogCounsellorName);
        EditText etDate     = dialogView.findViewById(R.id.etDate);
        Spinner  spTimeSlot = dialogView.findViewById(R.id.spinnerTimeSlot);
        EditText etNotes    = dialogView.findViewById(R.id.etNotes);

        tvName.setText("with " + counsellor.displayName);

        spTimeSlot.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, TIME_SLOTS));

        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, day) -> etDate.setText(
                            String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                    year, month + 1, day)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancelBooking)
                .setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnConfirmBooking)
                .setOnClickListener(v -> {
                    String date  = etDate.getText().toString().trim();
                    String slot  = spTimeSlot.getSelectedItem().toString();
                    String notes = etNotes.getText().toString().trim();

                    if (date.isEmpty()) {
                        Toast.makeText(this, "Please pick a date", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    dialog.dismiss();
                    bookAppointment(counsellor, date, slot, notes);
                });

        dialog.show();
    }

    private void bookAppointment(CounsellorItem counsellor,
                                 String date, String timeSlot, String notes) {
        String url = ApiConstants.APPOINTMENTS;

        JSONObject body = new JSONObject();
        try {
            body.put("userId",       myUserId);
            body.put("counsellorId", counsellor.userId);
            body.put("date",         date);
            body.put("timeSlot",     timeSlot);
            body.put("notes",        notes);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Request Sent!")
                            .setMessage("Your appointment request has been sent to "
                                    + counsellor.displayName + " for " + date
                                    + " at " + timeSlot
                                    + ". Please wait for them to accept.")
                            .setPositiveButton("OK", null)
                            .show();
                },
                error -> {
                    String msg = "Booking failed";
                    if (error.networkResponse != null)
                        msg += " (HTTP " + error.networkResponse.statusCode + ")";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public byte[] getBody() { return body.toString().getBytes(); }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private CounsellorItem parseCounsellor(JSONObject obj) throws JSONException {
        CounsellorItem c = new CounsellorItem();
        c.id             = obj.optLong("id",             -1);
        c.userId         = obj.optLong("userId",         -1);
        c.displayName    = obj.optString("displayName",  "");
        c.specialization = obj.optString("specialization", "");
        c.bio            = obj.optString("bio",          "");
        c.status         = obj.optString("status",       "OFFLINE");
        c.ratingAverage  = obj.optDouble("ratingAverage", 0.0);
        c.ratingCount    = obj.optInt("ratingCount",     0);
        return c;
    }

    // ── Model ─────────────────────────────────────────────────────────────────
    static class CounsellorItem {
        long   id, userId;
        String displayName, specialization, bio, status;
        double ratingAverage;
        int    ratingCount;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    interface OnBookClick { void onBook(CounsellorItem c); }

    static class CounsellorAdapter
            extends RecyclerView.Adapter<CounsellorAdapter.VH> {

        private final List<CounsellorItem> list;
        private final OnBookClick onBookClick;

        CounsellorAdapter(List<CounsellorItem> list, OnBookClick cb) {
            this.list = list;
            this.onBookClick = cb;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_counsellor_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            CounsellorItem c = list.get(position);

            h.tvName.setText(c.displayName);
            h.tvSpecialization.setText(c.specialization);
            h.tvBio.setText(c.bio);
            h.tvRating.setText(String.format(Locale.getDefault(),
                    "★ %.1f (%d reviews)", c.ratingAverage, c.ratingCount));

            h.tvStatus.setText(c.status);
            if (c.status.equalsIgnoreCase("AVAILABLE")) {
                h.tvStatus.setBackgroundResource(R.drawable.dark_green_background);
            } else if (c.status.equalsIgnoreCase("BUSY")) {
                h.tvStatus.setBackgroundColor(0xFFE65100);
            } else {
                h.tvStatus.setBackgroundColor(0xFF9E9E9E);
            }

            h.btnBook.setOnClickListener(v -> onBookClick.onBook(c));
            h.btnBook.setEnabled(c.status.equalsIgnoreCase("AVAILABLE"));
            h.btnBook.setAlpha(c.status.equalsIgnoreCase("AVAILABLE") ? 1f : 0.4f);
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvSpecialization, tvBio, tvRating, tvStatus;
            com.google.android.material.button.MaterialButton btnBook;

            VH(View v) {
                super(v);
                tvName           = v.findViewById(R.id.tvName);
                tvSpecialization = v.findViewById(R.id.tvSpecialization);
                tvBio            = v.findViewById(R.id.tvBio);
                tvRating         = v.findViewById(R.id.tvRating);
                tvStatus         = v.findViewById(R.id.tvStatus);
                btnBook          = v.findViewById(R.id.btnBook);
            }
        }
    }
}