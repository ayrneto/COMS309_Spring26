package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerAdminList;
    private TextView tvAdminCount, tabUsers, tabCounsellors;
    private EditText etAdminSearch;

    private final List<AdminUserItem> allItems = new ArrayList<>();
    private final List<AdminUserItem> filtered = new ArrayList<>();
    private AdminUserAdapter adapter;

    private boolean showingUsers = true;
    private String searchQuery   = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        recyclerAdminList = findViewById(R.id.recyclerAdminList);
        tvAdminCount      = findViewById(R.id.tvAdminCount);
        etAdminSearch     = findViewById(R.id.etAdminSearch);
        tabUsers          = findViewById(R.id.tabUsers);
        tabCounsellors    = findViewById(R.id.tabCounsellors);

        // Logout
        findViewById(R.id.btnAdminLogout).setOnClickListener(v -> {
            getSharedPreferences("AA_PREFS", MODE_PRIVATE).edit().clear().apply();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finishAffinity();
        });

        // Adapter
        adapter = new AdminUserAdapter(filtered, item -> {
            Intent intent = new Intent(this, AdminEditUserActivity.class);
            intent.putExtra("userId",           item.id);
            intent.putExtra("userName",         item.name);
            intent.putExtra("userEmail",        item.email);
            intent.putExtra("userRole",         item.role);
            intent.putExtra("userActive",       item.active);
            intent.putExtra("userPassword",     item.password);
            intent.putExtra("userConfirmPassword", item.confirmPassword);
            // counsellor extras
            intent.putExtra("cDisplayName",     item.displayName);
            intent.putExtra("cSpecialization",  item.specialization);
            intent.putExtra("cBio",             item.bio);
            intent.putExtra("cStatus",          item.status);
            startActivity(intent);
        });
        recyclerAdminList.setLayoutManager(new LinearLayoutManager(this));
        recyclerAdminList.setAdapter(adapter);

        // Tabs
        tabUsers.setOnClickListener(v -> {
            showingUsers = true;
            tabUsers.setTextColor(0xFF4A6E60);
            tabCounsellors.setTextColor(0xFF9B9B9B);
            etAdminSearch.setText("");
            fetchUsers();
        });

        tabCounsellors.setOnClickListener(v -> {
            showingUsers = false;
            tabCounsellors.setTextColor(0xFF4A6E60);
            tabUsers.setTextColor(0xFF9B9B9B);
            etAdminSearch.setText("");
            fetchCounsellors();
        });

        // Search
        etAdminSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                searchQuery = s.toString().toLowerCase().trim();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fetchUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (showingUsers) fetchUsers(); else fetchCounsellors();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /users
    // ─────────────────────────────────────────────────────────────────────────
    private void fetchUsers() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, ApiConstants.USERS, null,
                response -> {
                    allItems.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            AdminUserItem item = new AdminUserItem();
                            item.id              = obj.getLong("id");
                            item.name            = obj.optString("name",            "");
                            item.email           = obj.optString("email",           "");
                            item.password        = obj.optString("password",        "");
                            item.confirmPassword = obj.optString("confirmPassword", "");
                            item.role            = obj.optString("role",            "USER");
                            item.active          = obj.optBoolean("active",         true);
                            // skip counsellors — they appear in the Counsellors tab
                            if ("COUNSELLOR".equals(item.role)) continue;
                            allItems.add(item);
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    applyFilter();
                },
                error -> Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
        );
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/counsellors
    // ─────────────────────────────────────────────────────────────────────────
    private void fetchCounsellors() {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, ApiConstants.COUNSELLORS, null,
                response -> {
                    allItems.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            AdminUserItem item = new AdminUserItem();
                            item.id              = obj.optLong("userId",            -1);
                            item.name            = obj.optString("displayName",     "");
                            item.email           = obj.optString("email",           "");
                            item.password        = obj.optString("password",        "");
                            item.confirmPassword = obj.optString("confirmPassword", "");
                            item.role            = "COUNSELLOR";
                            item.active          = obj.optBoolean("active",         true);
                            item.displayName     = obj.optString("displayName",     "");
                            item.specialization  = obj.optString("specialization",  "");
                            item.bio             = obj.optString("bio",             "");
                            item.status          = obj.optString("status",          "AVAILABLE");
                            allItems.add(item);
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    applyFilter();
                },
                error -> Toast.makeText(this, "Failed to load counsellors", Toast.LENGTH_SHORT).show()
        );
        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    private void applyFilter() {
        filtered.clear();
        for (AdminUserItem item : allItems) {
            if (!searchQuery.isEmpty()) {
                boolean nameMatch  = item.name.toLowerCase().contains(searchQuery);
                boolean emailMatch = item.email.toLowerCase().contains(searchQuery);
                if (!nameMatch && !emailMatch) continue;
            }
            filtered.add(item);
        }
        adapter.notifyDataSetChanged();
        String label = showingUsers ? "user" : "counsellor";
        tvAdminCount.setText(filtered.size() + " " + label +
                (filtered.size() == 1 ? "" : "s") + " found");
    }

    // ── Model ─────────────────────────────────────────────────────────────────
    static class AdminUserItem {
        long    id;
        String  name, email, password, confirmPassword, role, status;
        boolean active;
        String  displayName = "", specialization = "", bio = "";
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    interface OnRowClick { void onClick(AdminUserItem item); }

    static class AdminUserAdapter
            extends RecyclerView.Adapter<AdminUserAdapter.VH> {

        private final List<AdminUserItem> list;
        private final OnRowClick cb;

        AdminUserAdapter(List<AdminUserItem> list, OnRowClick cb) {
            this.list = list;
            this.cb   = cb;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_user_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            AdminUserItem item = list.get(pos);
            h.tvName.setText(item.name.isEmpty() ? "(no name)" : item.name);
            h.tvEmail.setText(item.email.isEmpty() ? item.role : item.email);
            h.tvRole.setText(item.role);

            if (item.active) {
                h.tvActive.setText("Active");
                h.tvActive.setBackgroundResource(R.drawable.dark_green_background);
            } else {
                h.tvActive.setText("Inactive");
                h.tvActive.setBackgroundColor(0xFFB0B0B0);
            }

            h.itemView.setOnClickListener(v -> cb.onClick(item));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvRole, tvActive;
            VH(View v) {
                super(v);
                tvName   = v.findViewById(R.id.tvRowName);
                tvEmail  = v.findViewById(R.id.tvRowEmail);
                tvRole   = v.findViewById(R.id.tvRowRole);
                tvActive = v.findViewById(R.id.tvRowActive);
            }
        }
    }
}