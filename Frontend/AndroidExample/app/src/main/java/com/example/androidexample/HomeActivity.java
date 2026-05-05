package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

public class HomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID",    "");
        String email  = prefs.getString("USER_EMAIL", "");
        String name   = prefs.getString("USER_NAME",  "");
        String role   = prefs.getString("USER_ROLE",  "");

        drawerLayout = findViewById(R.id.drawerLayout);

        // Welcome text — show first name only
        String firstName = name.isEmpty() ? "" : name.split(" ")[0];
        ((TextView) findViewById(R.id.tvWelcome))
                .setText(firstName.isEmpty() ? "Welcome" : firstName);
        // Update hint if we have a name
        if (!firstName.isEmpty()) {
            ((TextView) findViewById(R.id.tvHint))
                    .setText("How are you feeling today, " + firstName + "?");
        }

        // Load quote and stats
        loadDailyQuote();
        if (role.equals("USER")) {
            loadQuickStats(userId);
        }

        // Drawer header
        String displayName = name.isEmpty() ? "Hello!" : "Hi, " + name;
        ((TextView) findViewById(R.id.drawerName)).setText(displayName);
        ((TextView) findViewById(R.id.drawerEmail)).setText(email);

        // Drawer avatar — show profile pic or initial
        String picUrl = prefs.getString("USER_PIC_URL", "");
        AvatarHelper.load(this, name, picUrl,
                findViewById(R.id.tvDrawerAvatar),
                findViewById(R.id.ivDrawerAvatar));

        // Hamburger
        ((ImageButton) findViewById(R.id.btnHamburger))
                .setOnClickListener(v -> drawerLayout.open());

        // ── Find a Counselor ──────────────────────────────────────────────────
        findViewById(R.id.drawerItemFindCounsellor).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, CounsellorSearchActivity.class));
        });

        // ── Worry Notes ───────────────────────────────────────────────────────
        findViewById(R.id.drawerItemWorryNotes).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, WorryNotes.class));
        });

        // ── Chat with Counselor ───────────────────────────────────────────────
        findViewById(R.id.drawerItemChat).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, ChatListActivity.class));
        });

        // ── Edit Profile ──────────────────────────────────────────────────────
        findViewById(R.id.drawerItemEditProfile).setOnClickListener(v -> {
            drawerLayout.close();
            Intent intent = new Intent(this, EditProfile.class);
            intent.putExtra("userId",   userId);
            intent.putExtra("email",    email);
            intent.putExtra("name",     name);
            intent.putExtra("password", "");
            startActivity(intent);
        });

        // ── Prescriptions (USER only) ─────────────────────────────────────────
        findViewById(R.id.drawerItemPrescriptions).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, PrescriptionsActivity.class));
        });

        // ── Show/hide menu items based on role ────────────────────────────────
        if (role.equals("COUNSELLOR")) {
            findViewById(R.id.drawerItemAssignTask).setVisibility(View.VISIBLE);
            findViewById(R.id.drawerItemMyTasks).setVisibility(View.GONE);
            findViewById(R.id.drawerItemCheckIn).setVisibility(View.GONE);
            findViewById(R.id.drawerRoutineTracker).setVisibility(View.GONE);
            findViewById(R.id.drawerItemAiChat).setVisibility(View.GONE);
            findViewById(R.id.drawerItemPrescriptions).setVisibility(View.GONE);
        } else if (role.equals("ADMIN")) {
            findViewById(R.id.drawerItemMyTasks).setVisibility(View.GONE);
            findViewById(R.id.drawerItemCheckIn).setVisibility(View.GONE);
            findViewById(R.id.drawerItemAssignTask).setVisibility(View.GONE);
            findViewById(R.id.drawerRoutineTracker).setVisibility(View.GONE);
            findViewById(R.id.drawerItemAiChat).setVisibility(View.GONE);
            findViewById(R.id.drawerItemPrescriptions).setVisibility(View.GONE);
        } else {
            // USER
            findViewById(R.id.drawerItemMyTasks).setVisibility(View.VISIBLE);
            findViewById(R.id.drawerItemCheckIn).setVisibility(View.VISIBLE);
            findViewById(R.id.drawerItemAssignTask).setVisibility(View.GONE);
            findViewById(R.id.drawerRoutineTracker).setVisibility(View.VISIBLE);
            findViewById(R.id.drawerItemAiChat).setVisibility(View.VISIBLE);
            findViewById(R.id.drawerItemPrescriptions).setVisibility(View.VISIBLE);
        }

        // ── My Tasks ──────────────────────────────────────────────────────────
        findViewById(R.id.drawerItemMyTasks).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, TasksOverview.class));
        });

        // ── Assign Task ───────────────────────────────────────────────────────
        findViewById(R.id.drawerItemAssignTask).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, AssignTaskActivity.class));
        });

        // ── Check-In ──────────────────────────────────────────────────────────
        findViewById(R.id.drawerItemCheckIn).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, CheckInActivity.class));
        });

        // Routine Summary listener
        findViewById(R.id.drawerRoutineTracker).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, RoutineSummaryActivity.class));
        });

        // ── AI Assistant (USER only) ───────────────────────────────────────────
        findViewById(R.id.drawerItemAiChat).setOnClickListener(v -> {
            drawerLayout.close();
            startActivity(new Intent(this, AIChatActivity.class));
        });

        // ── Log Out ───────────────────────────────────────────────────────────
        findViewById(R.id.drawerItemLogout).setOnClickListener(v -> {
            drawerLayout.close();
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        });

        // Load the summaries
        LinearLayout dashboardContainer = findViewById(R.id.dashboardContainer);
        dashboardContainer.removeAllViews();
        loadDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Skip reload on first launch (onCreate already loaded it)
        if (isFirstLoad) {
            isFirstLoad = false;
            return;
        }

        // Re-read name from SharedPreferences in case user just updated it in EditProfile
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String name      = prefs.getString("USER_NAME",  "");
        String email     = prefs.getString("USER_EMAIL", "");
        String firstName = name.isEmpty() ? "" : name.split(" ")[0];

        ((TextView) findViewById(R.id.tvWelcome))
                .setText(firstName.isEmpty() ? "Welcome" : firstName);
        ((TextView) findViewById(R.id.tvHint))
                .setText(firstName.isEmpty()
                        ? "How are you feeling today?"
                        : "How are you feeling today, " + firstName + "?");
        ((TextView) findViewById(R.id.drawerName))
                .setText(name.isEmpty() ? "Hello!" : "Hi, " + name);
        ((TextView) findViewById(R.id.drawerEmail)).setText(email);

        String picUrl = prefs.getString("USER_PIC_URL", "");
        AvatarHelper.load(this, name, picUrl,
                findViewById(R.id.tvDrawerAvatar),
                findViewById(R.id.ivDrawerAvatar));

        LinearLayout dashboardContainer = findViewById(R.id.dashboardContainer);
        dashboardContainer.removeAllViews();
        loadDashboard();
    }

    private void loadDashboard() {
        LinearLayout dashboardContainer = findViewById(R.id.dashboardContainer);
        dashboardContainer.setVisibility(android.view.View.VISIBLE);
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String role = prefs.getString("USER_ROLE", "");

        if (role.equals("USER")) {
            addRoutineSummaryCard(dashboardContainer);
            addWorryNoteCard(dashboardContainer);
            addTaskCard(dashboardContainer);
            addCheckInCard(dashboardContainer);
        } else {
            // Counselor dashboard cards
        }
    }

    private void addRoutineSummaryCard(LinearLayout container) {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/routines";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (response.length() == 0) {
                        // No routines, show placeholder or hide card
                        return;
                    }

                    try {
                        // Find routine with the highest progress (but uncompleted)
                        JSONObject topRoutine = null;
                        int maxProgress = -1;

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject routine = response.getJSONObject(i);
                            boolean completed = routine.getBoolean("completed");
                            int streakCount = routine.getInt("streakCount");

                            if (!completed && streakCount > maxProgress) {
                                maxProgress = streakCount;
                                topRoutine = routine;
                            }
                        }

                        if (topRoutine == null) {
                            // All routines completed
                            return;
                        }

                        // Inflate and populate card
                        View card = getLayoutInflater().inflate(R.layout.routine_card, container, false);

                        TextView title = card.findViewById(R.id.routineTitle);
                        TextView progress = card.findViewById(R.id.routineProgress);
                        TextView label = card.findViewById(R.id.routineLabel);
                        ProgressBar progressBar = card.findViewById(R.id.progressBar);

                        title.setText(topRoutine.getString("title"));
                        progress.setText("Day " + maxProgress + "/60");
                        label.setText(topRoutine.getString("label"));
                        progressBar.setMax(60);
                        progressBar.setProgress(maxProgress);

                        // Click to open routines page
                        card.setOnClickListener(v -> {
                            startActivity(new Intent(this, RoutineSummaryActivity.class));
                        });

                        container.addView(card);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Failed to load routines, don't show card
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void addWorryNoteCard(LinearLayout container) {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/notes";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (response.length() == 0) {
                        // No worry notes, don't show card
                        return;
                    }

                    try {
                        // Get most recent note (last in array)
                        JSONObject latestNote = response.getJSONObject(response.length() - 1);

                        // Inflate and populate card
                        View card = getLayoutInflater().inflate(R.layout.dashboard_worry_note_card, container, false);

                        TextView title = card.findViewById(R.id.worryTitle);
                        TextView content = card.findViewById(R.id.worryContent);
                        TextView dueDate = card.findViewById(R.id.worryDueDate);
                        TextView label = card.findViewById(R.id.worryLabel);

                        title.setText(latestNote.getString("title"));
                        content.setText(latestNote.getString("content"));

                        String dueDateStr = latestNote.optString("dueDate", "No due date");
                        dueDate.setText("Due: " + dueDateStr);

                        label.setText(latestNote.optString("label", ""));

                        // Click to open worry notes page
                        card.setOnClickListener(v -> {
                            startActivity(new Intent(this, WorryNotes.class));
                        });

                        container.addView(card);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Failed to load notes, don't show card
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void addTaskCard(LinearLayout container) {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");
        String url = ApiConstants.BASE_URL + "/api/users/" + userId + "/tasks";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (response.length() == 0) {
                        // No tasks, don't show card
                        return;
                    }

                    try {
                        // Find most urgent task (soonest due date, not completed)
                        JSONObject urgentTask = null;
                        String earliestDueDate = null;

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject task = response.getJSONObject(i);
                            String status = task.getString("status");

                            if (status.equals("Completed")) {
                                continue; // Skip completed tasks
                            }

                            String dueDate = task.optString("dueDate", null);
                            if (dueDate != null) {
                                if (earliestDueDate == null || dueDate.compareTo(earliestDueDate) < 0) {
                                    earliestDueDate = dueDate;
                                    urgentTask = task;
                                }
                            }
                        }

                        if (urgentTask == null) {
                            // No active tasks
                            return;
                        }

                        // Inflate and populate card
                        View card = getLayoutInflater().inflate(R.layout.dashboard_task_card, container, false);

                        TextView title = card.findViewById(R.id.taskTitle);
                        TextView description = card.findViewById(R.id.taskDescription);
                        TextView dueDate = card.findViewById(R.id.taskDueDate);
                        TextView status = card.findViewById(R.id.taskStatus);

                        title.setText(urgentTask.getString("title"));
                        description.setText(urgentTask.getString("description"));
                        dueDate.setText("Due: " + urgentTask.optString("dueDate", "No due date"));

                        String taskStatus = urgentTask.getString("status");
                        status.setText(taskStatus);

                        // Set status background color
                        if (taskStatus.equals("Not Started")) {
                            status.setBackgroundResource(R.drawable.red_background);
                        } else if (taskStatus.equals("Ongoing")) {
                            status.setBackgroundResource(R.drawable.yellow_background);
                        } else {
                            status.setBackgroundResource(R.drawable.green_background);
                        }

                        // Click to open tasks page
                        card.setOnClickListener(v -> {
                            startActivity(new Intent(this, TasksOverview.class));
                        });

                        container.addView(card);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Failed to load tasks, don't show card
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void addCheckInCard(LinearLayout container) {
        SharedPreferences prefs = getSharedPreferences("AA_PREFS", MODE_PRIVATE);
        String userId = prefs.getString("USER_ID", "-1");
        String url = ApiConstants.BASE_URL + "/users/" + userId + "/checkins";

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    // Inflate card
                    View card = getLayoutInflater().inflate(R.layout.dashboard_checkin_card, container, false);

                    View circle = card.findViewById(R.id.checkinCircle);
                    TextView status = card.findViewById(R.id.checkinStatus);

                    boolean checkedInToday = false;
                    int rating = 0;

                    // Check if checked in today
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject checkIn = response.getJSONObject(i);
                            String date = checkIn.getString("date");

                            if (date.equals(today)) {
                                checkedInToday = true;
                                rating = checkIn.getInt("rating");
                                break;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (checkedInToday) {
                        // Show rating circle
                        circle.setBackgroundResource(getCircleDrawable(rating));
                        status.setText("You rated today: " + rating + "/5");
                    } else {
                        // Show faded circle
                        circle.setBackgroundResource(R.drawable.circle_rating_3_faded);
                        status.setText("Not checked in yet");
                    }

                    // Click to open check-in
                    card.setOnClickListener(v -> {
                        startActivity(new Intent(this, CheckInActivity.class));
                    });

                    container.addView(card);
                },
                error -> {
                    // Failed to load, show "not checked in"
                    View card = getLayoutInflater().inflate(R.layout.dashboard_checkin_card, container, false);

                    View circle = card.findViewById(R.id.checkinCircle);
                    TextView status = card.findViewById(R.id.checkinStatus);

                    circle.setBackgroundResource(R.drawable.circle_rating_3_faded);
                    status.setText("Not checked in yet");

                    card.setOnClickListener(v -> {
                        startActivity(new Intent(this, CheckInActivity.class));
                    });

                    container.addView(card);
                }
        );

        Volley.newRequestQueue(this).add(request);
    }

    private int getCircleDrawable(int rating) {
        switch (rating) {
            case 1: return R.drawable.circle_rating_1;
            case 2: return R.drawable.circle_rating_2;
            case 3: return R.drawable.circle_rating_3;
            case 4: return R.drawable.circle_rating_4;
            case 5: return R.drawable.circle_rating_5;
            default: return R.drawable.circle_rating_3;
        }
    }
    // ── Daily Quote ───────────────────────────────────────────────────────────
    private void loadDailyQuote() {
        String[] quotes = {
                "You don't have to be positive all the time. It's perfectly okay to feel sad, angry, or stressed.|Let yourself feel it.",
                "Mental health is not a destination, but a process.|It's about how you drive, not where you're going.",
                "You are allowed to be both a masterpiece and a work in progress simultaneously.|— Sophia Bush",
                "Healing is not linear.|Be patient with yourself.",
                "Your present circumstances don't determine where you can go.|They merely determine where you start.",
                "Self-care is how you take your power back.|— Lalah Delia",
                "It's okay to ask for help.|Reaching out is a sign of courage, not weakness.",
                "Small steps still move you forward.|Every day counts.",
                "You survived 100% of your worst days.|You are stronger than you think.",
                "Rest is not a reward.|It is a necessity.",
                "Progress, not perfection.|That's the goal.",
                "Take care of your body. It's the only place you have to live.|— Jim Rohn",
                "What we think, we become.|— Buddha",
                "Be gentle with yourself.|You are a child of the universe.",
                "The greatest glory in living lies not in never falling,|but in rising every time we fall."
        };

        // Pick quote based on day of year so it changes daily
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        String[] parts = quotes[dayOfYear % quotes.length].split(java.util.regex.Pattern.quote("|"));

        TextView tvQuote  = findViewById(R.id.tvDailyQuote);
        TextView tvAuthor = findViewById(R.id.tvQuoteAuthor);

        if (tvQuote  != null) tvQuote.setText(parts[0].trim());
        if (tvAuthor != null) tvAuthor.setText(parts.length > 1 ? parts[1].trim() : "");
    }

    // ── Quick Stats ───────────────────────────────────────────────────────────
    private void loadQuickStats(String userId) {
        // Tasks stat
        String tasksUrl = ApiConstants.BASE_URL + "/api/users/" + userId + "/tasks";
        JsonArrayRequest tasksReq = new JsonArrayRequest(Request.Method.GET, tasksUrl, null,
                response -> {
                    int pending = 0;
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            String status = response.getJSONObject(i).optString("status", "");
                            if (!status.equals("Completed")) pending++;
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    TextView tv = findViewById(R.id.tvStatTasks);
                    if (tv != null) tv.setText(String.valueOf(pending));
                },
                error -> {
                    TextView tv = findViewById(R.id.tvStatTasks);
                    if (tv != null) tv.setText("0");
                }
        );
        Volley.newRequestQueue(this).add(tasksReq);

        // Routines stat
        String routinesUrl = ApiConstants.BASE_URL + "/api/users/" + userId + "/routines";
        JsonArrayRequest routinesReq = new JsonArrayRequest(Request.Method.GET, routinesUrl, null,
                response -> {
                    int active = 0;
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            boolean completed = response.getJSONObject(i).optBoolean("completed", false);
                            if (!completed) active++;
                        } catch (JSONException e) { e.printStackTrace(); }
                    }
                    TextView tv = findViewById(R.id.tvStatRoutines);
                    if (tv != null) tv.setText(String.valueOf(active));
                },
                error -> {
                    TextView tv = findViewById(R.id.tvStatRoutines);
                    if (tv != null) tv.setText("0");
                }
        );
        Volley.newRequestQueue(this).add(routinesReq);

        // Check-in stat — today's mood
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String checkInUrl = ApiConstants.BASE_URL + "/users/" + userId + "/checkins";
        JsonArrayRequest checkInReq = new JsonArrayRequest(Request.Method.GET, checkInUrl, null,
                response -> {
                    int todayRating = 0;
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            if (today.equals(obj.optString("date", ""))) {
                                todayRating = obj.optInt("rating", 0);
                                break;
                            }
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                    TextView tv = findViewById(R.id.tvStatCheckIn);
                    if (tv != null) {
                        String[] emojis = {"—", "😢", "😕", "😐", "🙂", "😄"};
                        tv.setText(todayRating > 0 ? emojis[todayRating] : "—");
                    }
                },
                error -> {
                    TextView tv = findViewById(R.id.tvStatCheckIn);
                    if (tv != null) tv.setText("—");
                }
        );
        Volley.newRequestQueue(this).add(checkInReq);
    }


}