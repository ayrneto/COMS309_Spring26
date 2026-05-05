package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class CounselorHomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private TextView     tvAppointmentBadge;
    private String       userId, email, displayName, specialization, profilePicUrl, status;

    // Status cycle: tap to rotate through
    private static final String[] STATUSES     = {"AVAILABLE", "BUSY", "OFFLINE"};
    private static final int[]    STATUS_COLORS = {0xFF2E7D32, 0xFFE65100, 0xFF757575};
    private int currentStatusIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counsellor_home);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        userId        = prefs.getString("USER_ID",               "");
        email         = prefs.getString("USER_EMAIL",            "");
        // Fall back to USER_NAME if display name not yet set via Edit Profile
        String rawDisplay = prefs.getString("COUNSELOR_DISPLAY_NAME","");
        displayName   = rawDisplay.isEmpty() ? prefs.getString("USER_NAME", "") : rawDisplay;
        specialization= prefs.getString("COUNSELOR_SPECIALIZATION","");
        profilePicUrl = prefs.getString("COUNSELOR_PROFILE_PIC", "");
        status        = prefs.getString("COUNSELOR_STATUS",      "AVAILABLE");

        drawerLayout       = findViewById(R.id.drawerLayout);
        tvAppointmentBadge = findViewById(R.id.tvAppointmentBadge);

        // ── Hero banner ───────────────────────────────────────────────────────
        String firstName = displayName.isEmpty() ? "Counsellor" : displayName.split(" ")[0];
        ((TextView) findViewById(R.id.tvWelcome)).setText(firstName);

        // ── Drawer header ─────────────────────────────────────────────────────
        ((TextView) findViewById(R.id.drawerName))
                .setText(displayName.isEmpty() ? "Hello!" : "Hi, " + displayName);
        ((TextView) findViewById(R.id.drawerEmail)).setText(email);

        AvatarHelper.load(this, displayName, profilePicUrl,
                findViewById(R.id.tvDrawerAvatar),
                findViewById(R.id.ivDrawerAvatar));

        // Re-check avatar after 2 seconds in case fetchCounsellorProfile
        // in LoginActivity hasn't finished saving yet when onCreate fires
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            String latestPic = getSharedPreferences("AA_PREFS", MODE_PRIVATE)
                    .getString("COUNSELOR_PROFILE_PIC", "");
            String latestName = getSharedPreferences("AA_PREFS", MODE_PRIVATE)
                    .getString("COUNSELOR_DISPLAY_NAME", "");
            AvatarHelper.load(this, latestName, latestPic,
                    findViewById(R.id.tvDrawerAvatar),
                    findViewById(R.id.ivDrawerAvatar));
        }, 2000);

        // ── Status toggle button ──────────────────────────────────────────────
        com.google.android.material.button.MaterialButton btnStatus =
                findViewById(R.id.btnStatusToggle);
        setStatusButton(btnStatus, status);

        btnStatus.setOnClickListener(v -> {
            currentStatusIndex = (currentStatusIndex + 1) % STATUSES.length;
            String newStatus = STATUSES[currentStatusIndex];
            setStatusButton(btnStatus, newStatus);
            saveStatus(newStatus);
        });

        // ── Hamburger ─────────────────────────────────────────────────────────
        ((ImageButton) findViewById(R.id.btnHamburger))
                .setOnClickListener(v -> drawerLayout.open());

        // ── Drawer navigation ─────────────────────────────────────────────────
        findViewById(R.id.drawerItemAppointments).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, AppointmentRequestsActivity.class));
        });
        findViewById(R.id.drawerItemChat).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, UserListActivity.class));
        });
        findViewById(R.id.drawerItemAssignTask).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, AssignTaskActivity.class));
        });
        findViewById(R.id.drawerItemSharedNotes).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, SharedNotesActivity.class));
        });
        findViewById(R.id.drawerItemEditProfile).setOnClickListener(v -> {
            drawerLayout.close();
            Intent intent = new Intent(this, CounselorEditProfileActivity.class);
            intent.putExtra("userId",            Long.parseLong(userId.isEmpty() ? "-1" : userId));
            intent.putExtra("displayName",       displayName);
            intent.putExtra("specialization",    specialization);
            intent.putExtra("bio",               prefs.getString("COUNSELOR_BIO", ""));
            intent.putExtra("profilePictureUrl", profilePicUrl);
            intent.putExtra("status",            status);
            startActivity(intent);
        });
        findViewById(R.id.drawerItemLogout).setOnClickListener(v -> {
            drawerLayout.close();
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        });

        // ── Load data ─────────────────────────────────────────────────────────
        if (!userId.isEmpty()) {
            fetchStats(userId);
            fetchUpcomingAppointments(userId);
            fetchRecentActivity(userId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        displayName    = prefs.getString("COUNSELOR_DISPLAY_NAME",   "");
        profilePicUrl  = prefs.getString("COUNSELOR_PROFILE_PIC",    "");
        specialization = prefs.getString("COUNSELOR_SPECIALIZATION", "");
        status         = prefs.getString("COUNSELOR_STATUS",         "AVAILABLE");

        String firstName = displayName.isEmpty() ? "Counsellor" : displayName.split(" ")[0];
        ((TextView) findViewById(R.id.tvWelcome)).setText(firstName);
        ((TextView) findViewById(R.id.drawerName))
                .setText(displayName.isEmpty() ? "Hello!" : "Hi, " + displayName);
        AvatarHelper.load(this, displayName, profilePicUrl,
                findViewById(R.id.tvDrawerAvatar),
                findViewById(R.id.ivDrawerAvatar));

        setStatusButton(findViewById(R.id.btnStatusToggle), status);

        String uid = prefs.getString("USER_ID", "");
        if (!uid.isEmpty()) {
            fetchStats(uid);
            fetchUpcomingAppointments(uid);
            fetchRecentActivity(uid);
        }
    }

    // ── Status toggle helpers ─────────────────────────────────────────────────

    private void setStatusButton(com.google.android.material.button.MaterialButton btn,
                                 String s) {
        btn.setText(s);
        for (int i = 0; i < STATUSES.length; i++) {
            if (STATUSES[i].equals(s)) {
                currentStatusIndex = i;
                btn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(STATUS_COLORS[i]));
                break;
            }
        }
    }

    private void saveStatus(String newStatus) {
        String url = ApiConstants.counselorProfile(Long.parseLong(userId));
        JSONObject body = new JSONObject();
        try {
            body.put("status", newStatus);
        } catch (Exception e) { return; }

        final String bodyStr = body.toString();

        StringRequest req = new StringRequest(Request.Method.PUT, url,
                response -> {
                    getSharedPreferences("AA_PREFS", MODE_PRIVATE)
                            .edit().putString("COUNSELOR_STATUS", newStatus).apply();
                    Toast.makeText(this, "Status: " + newStatus, Toast.LENGTH_SHORT).show();
                },
                error -> {
                    String errMsg = "Could not update status";
                    if (error.networkResponse != null)
                        errMsg += " (HTTP " + error.networkResponse.statusCode + ")";
                    else if (error.getMessage() != null)
                        errMsg += ": " + error.getMessage();
                    Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
                    android.util.Log.e("StatusToggle", "URL: " + url + " | Body: " + bodyStr + " | Error: " + error.toString());
                }
        ) {
            @Override public byte[] getBody() { return bodyStr.getBytes(StandardCharsets.UTF_8); }
            @Override public String getBodyContentType() { return "application/json; charset=utf-8"; }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void fetchStats(String counselorId) {
        // Accepted users count
        String acceptedUrl = ApiConstants.acceptedByCounsellor(Long.parseLong(counselorId));
        JsonArrayRequest usersReq = new JsonArrayRequest(Request.Method.GET, acceptedUrl, null,
                response -> {
                    // Deduplicate by userId
                    java.util.Set<Long> seen = new java.util.HashSet<>();
                    for (int i = 0; i < response.length(); i++) {
                        try { seen.add(response.getJSONObject(i).optLong("userId", -1)); }
                        catch (Exception e) { e.printStackTrace(); }
                    }
                    TextView tv = findViewById(R.id.tvStatUsers);
                    if (tv != null) tv.setText(String.valueOf(seen.size()));
                },
                error -> {});
        VolleySingleton.getInstance(this).addToRequestQueue(usersReq);

        // Pending appointments
        String pendingUrl = ApiConstants.appointmentsByCounsellor(Long.parseLong(counselorId));
        JsonArrayRequest pendingReq = new JsonArrayRequest(Request.Method.GET, pendingUrl, null,
                response -> {
                    int pending = 0;
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            if ("PENDING".equalsIgnoreCase(
                                    response.getJSONObject(i).optString("status"))) pending++;
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    TextView tv = findViewById(R.id.tvStatPending);
                    if (tv != null) tv.setText(String.valueOf(pending));
                    // Update badge too
                    if (pending > 0) {
                        tvAppointmentBadge.setText(String.valueOf(pending));
                        tvAppointmentBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvAppointmentBadge.setVisibility(View.GONE);
                    }
                },
                error -> {});
        VolleySingleton.getInstance(this).addToRequestQueue(pendingReq);

        // Shared notes count
        String notesUrl = ApiConstants.BASE_URL + "/api/counsellors/" + counselorId + "/shared-notes";
        JsonArrayRequest notesReq = new JsonArrayRequest(Request.Method.GET, notesUrl, null,
                response -> {
                    TextView tv = findViewById(R.id.tvStatSharedNotes);
                    if (tv != null) tv.setText(String.valueOf(response.length()));
                },
                error -> {});
        VolleySingleton.getInstance(this).addToRequestQueue(notesReq);
    }

    // ── Upcoming Appointments ─────────────────────────────────────────────────

    private void fetchUpcomingAppointments(String counselorId) {
        String url = ApiConstants.acceptedByCounsellor(Long.parseLong(counselorId));

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    LinearLayout container = findViewById(R.id.containerUpcoming);
                    TextView tvNone = findViewById(R.id.tvNoUpcoming);
                    if (container == null) return;

                    // Remove old dynamic views (keep tvNoUpcoming)
                    for (int i = container.getChildCount() - 1; i >= 0; i--) {
                        if (container.getChildAt(i) != tvNone)
                            container.removeViewAt(i);
                    }

                    int shown = 0;
                    for (int i = 0; i < response.length() && shown < 3; i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            String userName = obj.optString("userName", "Unknown");
                            String date     = obj.optString("date",     "—");
                            String time     = obj.optString("timeSlot", "");

                            // Build appointment row card
                            CardView card = new CardView(this);
                            CardView.LayoutParams lp = new CardView.LayoutParams(
                                    CardView.LayoutParams.MATCH_PARENT,
                                    CardView.LayoutParams.WRAP_CONTENT);
                            lp.bottomMargin = (int)(8 * getResources().getDisplayMetrics().density);
                            card.setLayoutParams(lp);
                            card.setRadius(12 * getResources().getDisplayMetrics().density);
                            card.setCardElevation(2 * getResources().getDisplayMetrics().density);
                            card.setCardBackgroundColor(Color.WHITE);

                            LinearLayout row = new LinearLayout(this);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                            int p = (int)(14 * getResources().getDisplayMetrics().density);
                            row.setPadding(p, p, p, p);

                            TextView tvDot = new TextView(this);
                            tvDot.setText("📅");
                            tvDot.setTextSize(16);
                            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                            dotLp.rightMargin = (int)(12 * getResources().getDisplayMetrics().density);
                            tvDot.setLayoutParams(dotLp);

                            LinearLayout info = new LinearLayout(this);
                            info.setOrientation(LinearLayout.VERTICAL);
                            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                            info.setLayoutParams(infoLp);

                            TextView tvName = new TextView(this);
                            tvName.setText(userName);
                            tvName.setTextSize(14);
                            tvName.setTextColor(Color.parseColor("#1B1B1B"));
                            tvName.setTypeface(null, android.graphics.Typeface.BOLD);

                            TextView tvDate = new TextView(this);
                            tvDate.setText(date + (time.isEmpty() ? "" : "  ·  " + time));
                            tvDate.setTextSize(12);
                            tvDate.setTextColor(Color.parseColor("#6B6B6B"));

                            info.addView(tvName);
                            info.addView(tvDate);

                            row.addView(tvDot);
                            row.addView(info);
                            card.addView(row);
                            container.addView(card);
                            shown++;
                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    if (shown == 0) {
                        tvNone.setVisibility(View.VISIBLE);
                    } else {
                        tvNone.setVisibility(View.GONE);
                    }
                },
                error -> {
                    TextView tvNone = findViewById(R.id.tvNoUpcoming);
                    if (tvNone != null) tvNone.setVisibility(View.VISIBLE);
                });

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }

    // ── Recent Activity ───────────────────────────────────────────────────────

    private void fetchRecentActivity(String counselorId) {
        // Use shared notes as recent activity events
        String url = ApiConstants.BASE_URL + "/api/counsellors/" + counselorId + "/shared-notes";

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    LinearLayout container = findViewById(R.id.containerActivity);
                    TextView tvNone = findViewById(R.id.tvNoActivity);
                    if (container == null) return;

                    for (int i = container.getChildCount() - 1; i >= 0; i--) {
                        if (container.getChildAt(i) != tvNone)
                            container.removeViewAt(i);
                    }

                    int shown = 0;
                    for (int i = 0; i < response.length() && shown < 5; i++) {
                        try {
                            JSONObject obj  = response.getJSONObject(i);
                            String sharedBy = obj.optString("sharedByName", "A user");
                            String title    = obj.optString("title", "a worry note");
                            String date     = obj.optString("dueDate", "");

                            CardView card = new CardView(this);
                            CardView.LayoutParams lp = new CardView.LayoutParams(
                                    CardView.LayoutParams.MATCH_PARENT,
                                    CardView.LayoutParams.WRAP_CONTENT);
                            lp.bottomMargin = (int)(6 * getResources().getDisplayMetrics().density);
                            card.setLayoutParams(lp);
                            card.setRadius(12 * getResources().getDisplayMetrics().density);
                            card.setCardElevation(2 * getResources().getDisplayMetrics().density);
                            card.setCardBackgroundColor(Color.WHITE);

                            LinearLayout row = new LinearLayout(this);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                            int p = (int)(12 * getResources().getDisplayMetrics().density);
                            row.setPadding(p, p, p, p);

                            TextView tvIcon = new TextView(this);
                            tvIcon.setText("📝");
                            tvIcon.setTextSize(15);
                            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                            iconLp.rightMargin = (int)(10 * getResources().getDisplayMetrics().density);
                            tvIcon.setLayoutParams(iconLp);

                            TextView tvEvent = new TextView(this);
                            tvEvent.setText(sharedBy + " shared \"" + title + "\"" +
                                    (date.isEmpty() ? "" : "  ·  " + date));
                            tvEvent.setTextSize(12);
                            tvEvent.setTextColor(Color.parseColor("#1B1B1B"));
                            LinearLayout.LayoutParams eventLp = new LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                            tvEvent.setLayoutParams(eventLp);

                            row.addView(tvIcon);
                            row.addView(tvEvent);
                            card.addView(row);
                            container.addView(card);
                            shown++;
                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    if (shown == 0) {
                        tvNone.setVisibility(View.VISIBLE);
                    } else {
                        tvNone.setVisibility(View.GONE);
                    }
                },
                error -> {
                    TextView tvNone = findViewById(R.id.tvNoActivity);
                    if (tvNone != null) tvNone.setVisibility(View.VISIBLE);
                });

        VolleySingleton.getInstance(this).addToRequestQueue(req);
    }
}