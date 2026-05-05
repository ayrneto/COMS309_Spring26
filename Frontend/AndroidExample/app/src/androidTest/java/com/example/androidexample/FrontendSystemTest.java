package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

/**
 * Comprehensive system tests for the Calmify Android frontend.
 *
 * Covers: LandingActivity, LoginActivity, SignUpActivity, HomeActivity,
 * CheckInActivity, CheckInSummaryActivity, WorryNotes, AddWorryActivity,
 * TasksOverview, TaskDetailsActivity, RoutineTrackerActivity,
 * RoutineSummaryActivity, CounsellorSearchActivity, ProfileActivity,
 * EditProfile, PrescriptionsActivity, SharedNotesActivity,
 * AdminDashboardActivity, CounselorHomeActivity.
 *
 * NOTE: AIChatActivity is intentionally excluded to preserve API tokens.
 *
 * @author Team
 */
@RunWith(AndroidJUnit4.class)
public class FrontendSystemTest {

    // ── Shared test credentials ──────────────────────────────────────────────

    private static final String TEST_EMAIL    = "shreytest@calmify.com";
    private static final String TEST_PASSWORD = "TestPass123";
    private static final String TEST_NAME     = "Shrey Test";

    private static final String COUNSELLOR_EMAIL    = "shreycounsellor@calmify.com";
    private static final String COUNSELLOR_PASSWORD = "TestPass123";
    private static final String COUNSELLOR_NAME     = "Shrey Counsellor";

    // ── @BeforeClass – provision accounts once per suite ────────────────────

    @BeforeClass
    public static void registerTestAccounts() throws Exception {
        OkHttpClient client = new OkHttpClient();
        registerAccount(client, TEST_NAME, TEST_EMAIL, TEST_PASSWORD, "USER");
        registerAccount(client, COUNSELLOR_NAME, COUNSELLOR_EMAIL, COUNSELLOR_PASSWORD, "COUNSELLOR");
    }

    private static void registerAccount(OkHttpClient client, String name, String email,
                                        String password, String role) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        body.put("confirmPassword", password);
        body.put("role", role);

        RequestBody rb = RequestBody.create(body.toString(),
                MediaType.parse("application/json; charset=utf-8"));
        Request req = new Request.Builder().url(ApiConstants.SIGNUP).post(rb).build();

        try (Response response = client.newCall(req).execute()) {
            int code = response.code();
            if (code != 201 && code != 409) {
                throw new AssertionError("Account setup failed for " + email + " — HTTP " + code);
            }
        }
    }

    // ── Intents lifecycle ────────────────────────────────────────────────────

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 1: LandingActivity
    // ════════════════════════════════════════════════════════════════════════

    @Rule
    public ActivityScenarioRule<LandingActivity> landingRule =
            new ActivityScenarioRule<>(LandingActivity.class);

    /**
     * Verifies all key UI elements are visible on the landing screen.
     */
    @Test
    public void landing_allElementsVisible() {
        onView(withId(R.id.btnGetStarted)).check(matches(isDisplayed()));
        onView(withId(R.id.btnGoToLogin)).check(matches(isDisplayed()));
    }

    /**
     * Tapping "Get Started" navigates to SignUpActivity.
     */
    @Test
    public void landing_getStartedNavigatesToSignup() throws InterruptedException {
        onView(withId(R.id.btnGetStarted)).perform(click());
        Thread.sleep(500);
        Intents.intended(hasComponent(SignUpActivity.class.getName()));
    }

    /**
     * Tapping "Log In" link navigates to LoginActivity.
     */
    @Test
    public void landing_goToLoginNavigatesToLogin() throws InterruptedException {
        onView(withId(R.id.btnGoToLogin)).perform(click());
        Thread.sleep(500);
        Intents.intended(hasComponent(LoginActivity.class.getName()));
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 2: LoginActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Login screen: both fields and login button visible.
     */
    @Test
    public void login_uiElementsVisible() {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail)).check(matches(isDisplayed()));
            onView(withId(R.id.inputPassword)).check(matches(isDisplayed()));
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSignUp)).check(matches(isDisplayed()));
        }
    }

    /**
     * Pressing Login with an empty email stays on LoginActivity.
     */
    @Test
    public void login_emptyEmailShowsError() {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnLogin)).perform(click());
            onView(withId(R.id.inputEmail)).check(matches(isDisplayed()));
        }
    }

    /**
     * Pressing Login with email but no password stays on LoginActivity.
     */
    @Test
    public void login_emptyPasswordShowsError() {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail))
                    .perform(typeText("someone@test.com"), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            onView(withId(R.id.inputPassword)).check(matches(isDisplayed()));
        }
    }

    /**
     * Typing in the email field reflects the typed text.
     */
    @Test
    public void login_typingEmailReflectsInput() {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail))
                    .perform(typeText(TEST_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.inputEmail)).check(matches(withText(TEST_EMAIL)));
        }
    }

    /**
     * Tapping "Sign Up" link from login opens SignUpActivity.
     */
    @Test
    public void login_signUpLinkNavigatesToSignup() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnSignUp)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(SignUpActivity.class.getName()));
        }
    }

    /**
     * Valid USER login routes to HomeActivity.
     */
    @Test
    public void login_validUserCredentialsOpenHome() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail))
                    .perform(typeText(TEST_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.inputPassword))
                    .perform(typeText(TEST_PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            Thread.sleep(4000);
            Intents.intended(hasComponent(HomeActivity.class.getName()));
        }
    }

    /**
     * Wrong credentials — stays on LoginActivity (network error or 401).
     */
    @Test
    public void login_wrongCredentialsStaysOnLogin() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail))
                    .perform(typeText("wrong@invalid.com"), closeSoftKeyboard());
            onView(withId(R.id.inputPassword))
                    .perform(typeText("WrongPass999"), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            Thread.sleep(3000);
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 3: SignUpActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * All signup form fields are visible.
     */
    @Test
    public void signup_allFieldsVisible() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnSignUp)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.et_name)).check(matches(isDisplayed()));
            onView(withId(R.id.et_email)).check(matches(isDisplayed()));
            onView(withId(R.id.et_password)).check(matches(isDisplayed()));
            onView(withId(R.id.et_confirm_password)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_signup)).check(matches(isDisplayed()));
        }
    }

    /**
     * Submitting with an empty name stays on signup.
     */
    @Test
    public void signup_emptyNameStaysOnSignup() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnSignUp)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.btn_signup)).perform(click());
            onView(withId(R.id.et_name)).check(matches(isDisplayed()));
        }
    }

    /**
     * Submitting with an invalid email stays on signup.
     */
    @Test
    public void signup_invalidEmailStaysOnSignup() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnSignUp)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.et_name)).perform(typeText("Test"), closeSoftKeyboard());
            onView(withId(R.id.et_email)).perform(typeText("notanemail"), closeSoftKeyboard());
            onView(withId(R.id.btn_signup)).perform(click());
            onView(withId(R.id.et_email)).check(matches(isDisplayed()));
        }
    }

    /**
     * Passwords that don't match keep user on signup.
     */
    @Test
    public void signup_mismatchedPasswordsStaysOnSignup() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnSignUp)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.et_name)).perform(typeText("Test User"), closeSoftKeyboard());
            onView(withId(R.id.et_email)).perform(typeText("mismatch@calmify.com"), closeSoftKeyboard());
            onView(withId(R.id.et_password)).perform(typeText("SecurePass1"), closeSoftKeyboard());
            onView(withId(R.id.et_confirm_password)).perform(typeText("Different1"), closeSoftKeyboard());
            onView(withId(R.id.btn_signup)).perform(click());
            onView(withId(R.id.btn_signup)).check(matches(isDisplayed()));
        }
    }

    /**
     * Password shorter than 8 characters stays on signup.
     */
    @Test
    public void signup_shortPasswordStaysOnSignup() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnSignUp)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.et_name)).perform(typeText("Test User"), closeSoftKeyboard());
            onView(withId(R.id.et_email)).perform(typeText("short@calmify.com"), closeSoftKeyboard());
            onView(withId(R.id.et_password)).perform(typeText("abc"), closeSoftKeyboard());
            onView(withId(R.id.et_confirm_password)).perform(typeText("abc"), closeSoftKeyboard());
            onView(withId(R.id.btn_signup)).perform(click());
            onView(withId(R.id.et_password)).check(matches(isDisplayed()));
        }
    }

    /**
     * Typing in name field works.
     */
    @Test
    public void signup_namefieldAcceptsInput() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnSignUp)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.et_name)).perform(typeText("Jane Doe"), closeSoftKeyboard());
            onView(withId(R.id.et_name)).check(matches(withText("Jane Doe")));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 4: HomeActivity (launched directly with seeded prefs)
    // ════════════════════════════════════════════════════════════════════════

    private void seedUserPrefs() {
        SharedPreferences.Editor editor = ApplicationProvider.getApplicationContext()
                .getSharedPreferences("AA_PREFS", android.content.Context.MODE_PRIVATE)
                .edit();
        editor.putString("USER_ID",    "1");
        editor.putString("USER_EMAIL", TEST_EMAIL);
        editor.putString("USER_NAME",  TEST_NAME);
        editor.putString("USER_ROLE",  "USER");
        editor.putString("USER_PIC_URL", "");
        editor.apply();
    }

    /**
     * HomeActivity renders welcome text and hamburger button.
     */
    @Test
    public void home_welcomeAndHamburgerVisible() {
        seedUserPrefs();
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).check(matches(isDisplayed()));
            onView(withId(R.id.tvWelcome)).check(matches(isDisplayed()));
        }
    }

    /**
     * Tapping hamburger opens the navigation drawer.
     */
    @Test
    public void home_hamburgerOpensDrawer() throws InterruptedException {
        seedUserPrefs();
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerLayout)).check(matches(isDisplayed()));
        }
    }

    /**
     * Drawer shows USER-specific navigation items.
     */
    @Test
    public void home_drawerShowsUserItems() throws InterruptedException {
        seedUserPrefs();
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemCheckIn)).check(matches(isDisplayed()));
            onView(withId(R.id.drawerItemWorryNotes)).check(matches(isDisplayed()));
            onView(withId(R.id.drawerItemFindCounsellor)).check(matches(isDisplayed()));
        }
    }

    /**
     * Drawer shows email in the header.
     */
    @Test
    public void home_drawerHeaderShowsEmail() throws InterruptedException {
        seedUserPrefs();
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerEmail)).check(matches(withText(TEST_EMAIL)));
        }
    }

    /**
     * Navigating to Check-In from drawer opens CheckInActivity.
     */
    @Test
    public void home_drawerCheckInNavigatesToCheckIn() throws InterruptedException {
        seedUserPrefs();
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemCheckIn)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(CheckInActivity.class.getName()));
        }
    }

    /**
     * Navigating to Worry Notes from drawer opens WorryNotes.
     */
    @Test
    public void home_drawerWorryNotesNavigatesToWorryNotes() throws InterruptedException {
        seedUserPrefs();
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemWorryNotes)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(WorryNotes.class.getName()));
        }
    }

    /**
     * Navigating to Find Counsellor from drawer opens CounsellorSearchActivity.
     */
    @Test
    public void home_drawerCounsellorSearchNavigates() throws InterruptedException {
        seedUserPrefs();
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemFindCounsellor)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(CounsellorSearchActivity.class.getName()));
        }
    }

    /**
     * Navigating to My Tasks from drawer opens TasksOverview.
     */
    @Test
    public void home_drawerMyTasksNavigates() throws InterruptedException {
        seedUserPrefs();
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemMyTasks)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(TasksOverview.class.getName()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 5: CheckInActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * CheckIn screen: rating circles, save, and summary buttons exist.
     */
    @Test
    public void checkIn_uiElementsVisible() {
        seedUserPrefs();
        try (ActivityScenario<CheckInActivity> s =
                     ActivityScenario.launch(CheckInActivity.class)) {
            onView(withId(R.id.circle1)).check(matches(isDisplayed()));
            onView(withId(R.id.circle2)).check(matches(isDisplayed()));
            onView(withId(R.id.circle3)).check(matches(isDisplayed()));
            onView(withId(R.id.circle4)).check(matches(isDisplayed()));
            onView(withId(R.id.circle5)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSummary)).check(matches(isDisplayed()));
        }
    }

    /**
     * Tapping a rating circle doesn't crash.
     */
    @Test
    public void checkIn_clickingRatingCircleWorks() {
        seedUserPrefs();
        try (ActivityScenario<CheckInActivity> s =
                     ActivityScenario.launch(CheckInActivity.class)) {
            onView(withId(R.id.circle3)).perform(click());
            onView(withId(R.id.circle3)).check(matches(isDisplayed()));
        }
    }

    /**
     * Selecting each rating circle (1–5) does not crash.
     */
    @Test
    public void checkIn_allRatingCirclesClickable() {
        seedUserPrefs();
        try (ActivityScenario<CheckInActivity> s =
                     ActivityScenario.launch(CheckInActivity.class)) {
            onView(withId(R.id.circle1)).perform(click());
            onView(withId(R.id.circle2)).perform(click());
            onView(withId(R.id.circle3)).perform(click());
            onView(withId(R.id.circle4)).perform(click());
            onView(withId(R.id.circle5)).perform(click());
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
        }
    }

    /**
     * Typing a description works.
     */
    @Test
    public void checkIn_descriptionInputAcceptsText() {
        seedUserPrefs();
        try (ActivityScenario<CheckInActivity> s =
                     ActivityScenario.launch(CheckInActivity.class)) {
            onView(withId(R.id.inputDescription))
                    .perform(typeText("Feeling great today"), closeSoftKeyboard());
            onView(withId(R.id.inputDescription))
                    .check(matches(withText("Feeling great today")));
        }
    }

    /**
     * Tapping "View Summary" navigates to CheckInSummaryActivity.
     */
    @Test
    public void checkIn_summaryButtonNavigates() throws InterruptedException {
        seedUserPrefs();
        try (ActivityScenario<CheckInActivity> s =
                     ActivityScenario.launch(CheckInActivity.class)) {
            onView(withId(R.id.btnSummary)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(CheckInSummaryActivity.class.getName()));
        }
    }

    /**
     * Back button exists and activity finishes cleanly.
     */
    @Test
    public void checkIn_backButtonExists() {
        seedUserPrefs();
        try (ActivityScenario<CheckInActivity> s =
                     ActivityScenario.launch(CheckInActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 6: CheckInSummaryActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Summary screen renders back button.
     */
    @Test
    public void checkInSummary_backButtonVisible() {
        seedUserPrefs();
        try (ActivityScenario<CheckInSummaryActivity> s =
                     ActivityScenario.launch(CheckInSummaryActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    /**
     * Back button on summary returns to previous screen.
     */
    @Test
    public void checkInSummary_backButtonFinishesActivity() {
        seedUserPrefs();
        try (ActivityScenario<CheckInSummaryActivity> s =
                     ActivityScenario.launch(CheckInSummaryActivity.class)) {
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 7: RoutineTrackerActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Routine tracker UI elements are all visible.
     */
    @Test
    public void routineTracker_uiElementsVisible() {
        seedUserPrefs();
        try (ActivityScenario<RoutineTrackerActivity> s =
                     ActivityScenario.launch(RoutineTrackerActivity.class)) {
            onView(withId(R.id.routineTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.routineDescription)).check(matches(isDisplayed()));
            onView(withId(R.id.routineStartDate)).check(matches(isDisplayed()));
            onView(withId(R.id.routineReminder)).check(matches(isDisplayed()));
            onView(withId(R.id.spinnerLabel)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
        }
    }

    /**
     * Typing a routine title works.
     */
    @Test
    public void routineTracker_titleFieldAcceptsText() {
        seedUserPrefs();
        try (ActivityScenario<RoutineTrackerActivity> s =
                     ActivityScenario.launch(RoutineTrackerActivity.class)) {
            onView(withId(R.id.routineTitle))
                    .perform(typeText("Morning Workout"), closeSoftKeyboard());
            onView(withId(R.id.routineTitle))
                    .check(matches(withText("Morning Workout")));
        }
    }

    /**
     * Typing a description works.
     */
    @Test
    public void routineTracker_descriptionFieldAcceptsText() {
        seedUserPrefs();
        try (ActivityScenario<RoutineTrackerActivity> s =
                     ActivityScenario.launch(RoutineTrackerActivity.class)) {
            onView(withId(R.id.routineDescription))
                    .perform(typeText("30 min jog every morning"), closeSoftKeyboard());
            onView(withId(R.id.routineDescription))
                    .check(matches(withText("30 min jog every morning")));
        }
    }

    /**
     * Label spinner is clickable and selectable.
     */
    @Test
    public void routineTracker_spinnerSelectable() {
        seedUserPrefs();
        try (ActivityScenario<RoutineTrackerActivity> s =
                     ActivityScenario.launch(RoutineTrackerActivity.class)) {
            onView(withId(R.id.spinnerLabel)).perform(click());
            onData(anything()).atPosition(1).perform(click());
            onView(withId(R.id.spinnerLabel)).check(matches(isDisplayed()));
        }
    }

    /**
     * Clicking date picker doesn't crash (dismisses with back).
     */
    @Test
    public void routineTracker_datePickerOpens() {
        seedUserPrefs();
        try (ActivityScenario<RoutineTrackerActivity> s =
                     ActivityScenario.launch(RoutineTrackerActivity.class)) {
            onView(withId(R.id.routineStartDate)).perform(click());
            androidx.test.espresso.Espresso.pressBack();
            onView(withId(R.id.routineTitle)).check(matches(isDisplayed()));
        }
    }

    /**
     * Back button closes routine tracker.
     */
    @Test
    public void routineTracker_backButtonWorks() {
        seedUserPrefs();
        try (ActivityScenario<RoutineTrackerActivity> s =
                     ActivityScenario.launch(RoutineTrackerActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 8: RoutineSummaryActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Routine summary screen renders back button and filter spinner.
     */
    @Test
    public void routineSummary_uiVisible() {
        seedUserPrefs();
        try (ActivityScenario<RoutineSummaryActivity> s =
                     ActivityScenario.launch(RoutineSummaryActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            onView(withId(R.id.filterSpinner)).check(matches(isDisplayed()));
        }
    }

    /**
     * Filter spinner on routine summary is clickable.
     */
    @Test
    public void routineSummary_filterSpinnerClickable() {
        seedUserPrefs();
        try (ActivityScenario<RoutineSummaryActivity> s =
                     ActivityScenario.launch(RoutineSummaryActivity.class)) {
            onView(withId(R.id.filterSpinner)).perform(click());
            onData(anything()).atPosition(0).perform(click());
            onView(withId(R.id.filterSpinner)).check(matches(isDisplayed()));
        }
    }

    /**
     * Back button on routine summary finishes the activity.
     */
    @Test
    public void routineSummary_backButtonWorks() {
        seedUserPrefs();
        try (ActivityScenario<RoutineSummaryActivity> s =
                     ActivityScenario.launch(RoutineSummaryActivity.class)) {
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 9: WorryNotes
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Worry notes screen renders add button and back button.
     */
    @Test
    public void worryNotes_uiElementsVisible() {
        seedUserPrefs();
        try (ActivityScenario<WorryNotes> s =
                     ActivityScenario.launch(WorryNotes.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            onView(withId(R.id.btnAddWorryNote)).check(matches(isDisplayed()));
        }
    }

    /**
     * Tapping Add Worry Note navigates to AddWorryActivity.
     */
    @Test
    public void worryNotes_addButtonNavigatesToAddWorry() throws InterruptedException {
        seedUserPrefs();
        try (ActivityScenario<WorryNotes> s =
                     ActivityScenario.launch(WorryNotes.class)) {
            onView(withId(R.id.btnAddWorryNote)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(AddWorryActivity.class.getName()));
        }
    }

    /**
     * Back button on WorryNotes finishes the activity.
     */
    @Test
    public void worryNotes_backButtonWorks() {
        seedUserPrefs();
        try (ActivityScenario<WorryNotes> s =
                     ActivityScenario.launch(WorryNotes.class)) {
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 10: AddWorryActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Add worry form: all fields visible.
     */
    @Test
    public void addWorry_allFieldsVisible() {
        seedUserPrefs();
        try (ActivityScenario<AddWorryActivity> s =
                     ActivityScenario.launch(AddWorryActivity.class)) {
            onView(withId(R.id.inputTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.inputContent)).check(matches(isDisplayed()));
            onView(withId(R.id.inputDueDate)).check(matches(isDisplayed()));
            onView(withId(R.id.spinnerLabel)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSaveWorry)).check(matches(isDisplayed()));
        }
    }

    /**
     * Title field accepts text.
     */
    @Test
    public void addWorry_titleAcceptsInput() {
        seedUserPrefs();
        try (ActivityScenario<AddWorryActivity> s =
                     ActivityScenario.launch(AddWorryActivity.class)) {
            onView(withId(R.id.inputTitle))
                    .perform(typeText("Work deadline"), closeSoftKeyboard());
            onView(withId(R.id.inputTitle)).check(matches(withText("Work deadline")));
        }
    }

    /**
     * Content field accepts text.
     */
    @Test
    public void addWorry_contentAcceptsInput() {
        seedUserPrefs();
        try (ActivityScenario<AddWorryActivity> s =
                     ActivityScenario.launch(AddWorryActivity.class)) {
            onView(withId(R.id.inputContent))
                    .perform(typeText("I'm worried about my project"), closeSoftKeyboard());
            onView(withId(R.id.inputContent))
                    .check(matches(withText("I'm worried about my project")));
        }
    }

    /**
     * Spinner label is selectable.
     */
    @Test
    public void addWorry_spinnerSelectable() {
        seedUserPrefs();
        try (ActivityScenario<AddWorryActivity> s =
                     ActivityScenario.launch(AddWorryActivity.class)) {
            onView(withId(R.id.spinnerLabel)).perform(click());
            onData(anything()).atPosition(2).perform(click());
            onView(withId(R.id.spinnerLabel)).check(matches(isDisplayed()));
        }
    }

    /**
     * Saving with empty title stays on AddWorryActivity.
     */
    @Test
    public void addWorry_emptyTitleStaysOnScreen() {
        seedUserPrefs();
        try (ActivityScenario<AddWorryActivity> s =
                     ActivityScenario.launch(AddWorryActivity.class)) {
            onView(withId(R.id.inputContent))
                    .perform(typeText("Some content"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveWorry)).perform(click());
            onView(withId(R.id.btnSaveWorry)).check(matches(isDisplayed()));
        }
    }

    /**
     * Saving with empty content stays on AddWorryActivity.
     */
    @Test
    public void addWorry_emptyContentStaysOnScreen() {
        seedUserPrefs();
        try (ActivityScenario<AddWorryActivity> s =
                     ActivityScenario.launch(AddWorryActivity.class)) {
            onView(withId(R.id.inputTitle))
                    .perform(typeText("Some title"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveWorry)).perform(click());
            onView(withId(R.id.btnSaveWorry)).check(matches(isDisplayed()));
        }
    }

    /**
     * Top bar title is displayed.
     */
    @Test
    public void addWorry_topBarTitleVisible() {
        seedUserPrefs();
        try (ActivityScenario<AddWorryActivity> s =
                     ActivityScenario.launch(AddWorryActivity.class)) {
            onView(withId(R.id.tvTopBarTitle)).check(matches(isDisplayed()));
        }
    }

    /**
     * Edit mode pre-fills fields via intent extras.
     */
    @Test
    public void addWorry_editModePreFillsFields() {
        seedUserPrefs();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AddWorryActivity.class);
        intent.putExtra("noteId", 99L);
        intent.putExtra("title",   "Pre-filled Title");
        intent.putExtra("content", "Pre-filled Content");
        intent.putExtra("dueDate", "2026-06-01");
        intent.putExtra("label",   "Work");

        try (ActivityScenario<AddWorryActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.inputTitle)).check(matches(withText("Pre-filled Title")));
            onView(withId(R.id.inputContent)).check(matches(withText("Pre-filled Content")));
        }
    }

    /**
     * Edit mode changes save button text to "Update".
     */
    @Test
    public void addWorry_editModeButtonSaysUpdate() {
        seedUserPrefs();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AddWorryActivity.class);
        intent.putExtra("noteId", 88L);
        intent.putExtra("title",   "Old Title");
        intent.putExtra("content", "Old Content");
        intent.putExtra("dueDate", "2026-05-01");
        intent.putExtra("label",   "Personal");

        try (ActivityScenario<AddWorryActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnSaveWorry)).check(matches(withText("Update")));
        }
    }

    /**
     * Back button on add worry finishes the activity.
     */
    @Test
    public void addWorry_backButtonWorks() {
        seedUserPrefs();
        try (ActivityScenario<AddWorryActivity> s =
                     ActivityScenario.launch(AddWorryActivity.class)) {
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 11: TasksOverview
    // ════════════════════════════════════════════════════════════════════════

    /**
     * TasksOverview renders back button and tasks container.
     */
    @Test
    public void tasksOverview_uiElementsVisible() {
        seedUserPrefs();
        try (ActivityScenario<TasksOverview> s =
                     ActivityScenario.launch(TasksOverview.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            onView(withId(R.id.tasksContainer)).check(matches(isDisplayed()));
        }
    }

    /**
     * Back button on TasksOverview finishes the activity.
     */
    @Test
    public void tasksOverview_backButtonWorks() {
        seedUserPrefs();
        try (ActivityScenario<TasksOverview> s =
                     ActivityScenario.launch(TasksOverview.class)) {
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 12: TaskDetailsActivity
    // ════════════════════════════════════════════════════════════════════════

    private Intent buildTaskDetailsIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                TaskDetailsActivity.class);
        intent.putExtra("taskId",      1L);
        intent.putExtra("title",       "Fix the bug");
        intent.putExtra("description", "The login button crashes on empty fields");
        intent.putExtra("dueDate",     "2026-06-15");
        intent.putExtra("status",      "Ongoing");
        return intent;
    }

    /**
     * TaskDetails screen renders title, description, due date, and spinner.
     */
    @Test
    public void taskDetails_uiElementsVisible() {
        seedUserPrefs();
        try (ActivityScenario<TaskDetailsActivity> s =
                     ActivityScenario.launch(buildTaskDetailsIntent())) {
            onView(withId(R.id.taskTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.taskDescription)).check(matches(isDisplayed()));
            onView(withId(R.id.taskDueDate)).check(matches(isDisplayed()));
            onView(withId(R.id.spinnerStatus)).check(matches(isDisplayed()));
        }
    }

    /**
     * Task title is correctly set from intent.
     */
    @Test
    public void taskDetails_titleSetFromIntent() {
        seedUserPrefs();
        try (ActivityScenario<TaskDetailsActivity> s =
                     ActivityScenario.launch(buildTaskDetailsIntent())) {
            onView(withId(R.id.taskTitle)).check(matches(withText("Fix the bug")));
        }
    }

    /**
     * Task description is correctly set from intent.
     */
    @Test
    public void taskDetails_descriptionSetFromIntent() {
        seedUserPrefs();
        try (ActivityScenario<TaskDetailsActivity> s =
                     ActivityScenario.launch(buildTaskDetailsIntent())) {
            onView(withId(R.id.taskDescription))
                    .check(matches(withText("The login button crashes on empty fields")));
        }
    }

    /**
     * Status spinner is clickable.
     */
    @Test
    public void taskDetails_statusSpinnerClickable() {
        seedUserPrefs();
        try (ActivityScenario<TaskDetailsActivity> s =
                     ActivityScenario.launch(buildTaskDetailsIntent())) {
            onView(withId(R.id.spinnerStatus)).perform(click());
            onData(anything()).atPosition(2).perform(click()); // Completed
            onView(withId(R.id.spinnerStatus)).check(matches(isDisplayed()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 13: CounsellorSearchActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Counsellor search renders search bar, spinners, and recycler.
     */
    @Test
    public void counsellorSearch_uiElementsVisible() {
        seedUserPrefs();
        try (ActivityScenario<CounsellorSearchActivity> s =
                     ActivityScenario.launch(CounsellorSearchActivity.class)) {
            onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
            onView(withId(R.id.spinnerStatus)).check(matches(isDisplayed()));
            onView(withId(R.id.spinnerRating)).check(matches(isDisplayed()));
            onView(withId(R.id.recyclerCounsellors)).check(matches(isDisplayed()));
        }
    }

    /**
     * Typing in search bar reflects input.
     */
    @Test
    public void counsellorSearch_searchFieldAcceptsInput() {
        seedUserPrefs();
        try (ActivityScenario<CounsellorSearchActivity> s =
                     ActivityScenario.launch(CounsellorSearchActivity.class)) {
            onView(withId(R.id.etSearch))
                    .perform(typeText("Dr"), closeSoftKeyboard());
            onView(withId(R.id.etSearch)).check(matches(withText("Dr")));
        }
    }

    /**
     * Status filter spinner is clickable.
     */
    @Test
    public void counsellorSearch_statusFilterClickable() {
        seedUserPrefs();
        try (ActivityScenario<CounsellorSearchActivity> s =
                     ActivityScenario.launch(CounsellorSearchActivity.class)) {
            onView(withId(R.id.spinnerStatus)).perform(click());
            onData(anything()).atPosition(1).perform(click()); // Available
            onView(withId(R.id.spinnerStatus)).check(matches(isDisplayed()));
        }
    }

    /**
     * Rating filter spinner is clickable.
     */
    @Test
    public void counsellorSearch_ratingFilterClickable() {
        seedUserPrefs();
        try (ActivityScenario<CounsellorSearchActivity> s =
                     ActivityScenario.launch(CounsellorSearchActivity.class)) {
            onView(withId(R.id.spinnerRating)).perform(click());
            onData(anything()).atPosition(2).perform(click());
            onView(withId(R.id.spinnerRating)).check(matches(isDisplayed()));
        }
    }

    /**
     * Result count label is displayed.
     */
    @Test
    public void counsellorSearch_resultCountVisible() {
        seedUserPrefs();
        try (ActivityScenario<CounsellorSearchActivity> s =
                     ActivityScenario.launch(CounsellorSearchActivity.class)) {
            onView(withId(R.id.tvResultCount)).check(matches(isDisplayed()));
        }
    }

    /**
     * Back button on counsellor search works.
     */
    @Test
    public void counsellorSearch_backButtonWorks() {
        seedUserPrefs();
        try (ActivityScenario<CounsellorSearchActivity> s =
                     ActivityScenario.launch(CounsellorSearchActivity.class)) {
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 14: ProfileActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Profile screen renders email, back, edit, and worry notes buttons.
     */
    @Test
    public void profile_uiElementsVisible() {
        seedUserPrefs();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                ProfileActivity.class);
        intent.putExtra("username", TEST_EMAIL);
        intent.putExtra("password", TEST_PASSWORD);

        try (ActivityScenario<ProfileActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvEmail)).check(matches(isDisplayed()));
            onView(withId(R.id.btn_back_home)).check(matches(isDisplayed()));
            onView(withId(R.id.btnEditProfile)).check(matches(isDisplayed()));
            onView(withId(R.id.btnWorryNotes)).check(matches(isDisplayed()));
        }
    }

    /**
     * Email label shows the stored email.
     */
    @Test
    public void profile_emailLabelMatchesPrefs() {
        seedUserPrefs();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                ProfileActivity.class);
        intent.putExtra("username", TEST_EMAIL);

        try (ActivityScenario<ProfileActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvEmail))
                    .check(matches(withText("Email: " + TEST_EMAIL)));
        }
    }

    /**
     * Edit profile button navigates to EditProfile.
     */
    @Test
    public void profile_editProfileButtonNavigates() throws InterruptedException {
        seedUserPrefs();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                ProfileActivity.class);
        intent.putExtra("username", TEST_EMAIL);

        try (ActivityScenario<ProfileActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnEditProfile)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(EditProfile.class.getName()));
        }
    }

    /**
     * Worry Notes button navigates to WorryNotes from profile.
     */
    @Test
    public void profile_worryNotesButtonNavigates() throws InterruptedException {
        seedUserPrefs();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                ProfileActivity.class);
        intent.putExtra("username", TEST_EMAIL);

        try (ActivityScenario<ProfileActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnWorryNotes)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(WorryNotes.class.getName()));
        }
    }

    /**
     * Back button on Profile returns to previous screen.
     */
    @Test
    public void profile_backButtonWorks() {
        seedUserPrefs();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                ProfileActivity.class);
        intent.putExtra("username", TEST_EMAIL);

        try (ActivityScenario<ProfileActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btn_back_home)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 15: EditProfile
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Edit profile form: all fields visible.
     */
    @Test
    public void editProfile_allFieldsVisible() {
        seedUserPrefs();
        try (ActivityScenario<EditProfile> s =
                     ActivityScenario.launch(EditProfile.class)) {
            onView(withId(R.id.editName)).check(matches(isDisplayed()));
            onView(withId(R.id.editEmail)).check(matches(isDisplayed()));
            onView(withId(R.id.editPassword)).check(matches(isDisplayed()));
            onView(withId(R.id.editConfirmPassword)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
        }
    }

    /**
     * Name field is pre-filled from SharedPreferences.
     */
    @Test
    public void editProfile_namePrefilledFromPrefs() {
        seedUserPrefs();
        try (ActivityScenario<EditProfile> s =
                     ActivityScenario.launch(EditProfile.class)) {
            onView(withId(R.id.editName)).check(matches(withText(TEST_NAME)));
        }
    }

    /**
     * Email field is pre-filled from SharedPreferences.
     */
    @Test
    public void editProfile_emailPrefilledFromPrefs() {
        seedUserPrefs();
        try (ActivityScenario<EditProfile> s =
                     ActivityScenario.launch(EditProfile.class)) {
            onView(withId(R.id.editEmail)).check(matches(withText(TEST_EMAIL)));
        }
    }

    /**
     * Mismatched passwords shows error and stays on screen.
     */
    @Test
    public void editProfile_mismatchedPasswordsStaysOnScreen() {
        seedUserPrefs();
        try (ActivityScenario<EditProfile> s =
                     ActivityScenario.launch(EditProfile.class)) {
            onView(withId(R.id.editPassword))
                    .perform(typeText("NewPass123"), closeSoftKeyboard());
            onView(withId(R.id.editConfirmPassword))
                    .perform(typeText("Different123"), closeSoftKeyboard());
            onView(withId(R.id.btnSave)).perform(click());
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
        }
    }

    /**
     * Empty name triggers validation and stays on screen.
     */
    @Test
    public void editProfile_emptyNameStaysOnScreen() {
        seedUserPrefs();
        try (ActivityScenario<EditProfile> s =
                     ActivityScenario.launch(EditProfile.class)) {
            onView(withId(R.id.editName)).perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.editEmail)).perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.btnSave)).perform(click());
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
        }
    }

    /**
     * Back button on EditProfile finishes.
     */
    @Test
    public void editProfile_backButtonWorks() {
        seedUserPrefs();
        try (ActivityScenario<EditProfile> s =
                     ActivityScenario.launch(EditProfile.class)) {
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 16: PrescriptionsActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Prescriptions screen renders back button and container.
     */
    @Test
    public void prescriptions_uiElementsVisible() {
        seedUserPrefs();
        try (ActivityScenario<PrescriptionsActivity> s =
                     ActivityScenario.launch(PrescriptionsActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            onView(withId(R.id.prescriptionsContainer)).check(matches(isDisplayed()));
        }
    }

    /**
     * Back button on prescriptions finishes.
     */
    @Test
    public void prescriptions_backButtonWorks() {
        seedUserPrefs();
        try (ActivityScenario<PrescriptionsActivity> s =
                     ActivityScenario.launch(PrescriptionsActivity.class)) {
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 17: SharedNotesActivity
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Shared notes screen renders back button.
     */
    @Test
    public void sharedNotes_backButtonVisible() {
        seedUserPrefs();
        try (ActivityScenario<SharedNotesActivity> s =
                     ActivityScenario.launch(SharedNotesActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    /**
     * Back button on shared notes finishes.
     */
    @Test
    public void sharedNotes_backButtonWorks() {
        seedUserPrefs();
        try (ActivityScenario<SharedNotesActivity> s =
                     ActivityScenario.launch(SharedNotesActivity.class)) {
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 18: AdminDashboardActivity
    // ════════════════════════════════════════════════════════════════════════

    private void seedAdminPrefs() {
        SharedPreferences.Editor editor = ApplicationProvider.getApplicationContext()
                .getSharedPreferences("AA_PREFS", android.content.Context.MODE_PRIVATE)
                .edit();
        editor.putString("USER_ID",    "999");
        editor.putString("USER_EMAIL", "admin@calmify.com");
        editor.putString("USER_NAME",  "Admin User");
        editor.putString("USER_ROLE",  "ADMIN");
        editor.apply();
    }

    /**
     * Admin dashboard renders search, tab buttons, and recycler.
     */
    @Test
    public void adminDashboard_uiElementsVisible() {
        seedAdminPrefs();
        try (ActivityScenario<AdminDashboardActivity> s =
                     ActivityScenario.launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.etAdminSearch)).check(matches(isDisplayed()));
            onView(withId(R.id.tabUsers)).check(matches(isDisplayed()));
            onView(withId(R.id.tabCounsellors)).check(matches(isDisplayed()));
            onView(withId(R.id.recyclerAdminList)).check(matches(isDisplayed()));
        }
    }

    /**
     * Typing in admin search bar reflects input.
     */
    @Test
    public void adminDashboard_searchFieldAcceptsInput() {
        seedAdminPrefs();
        try (ActivityScenario<AdminDashboardActivity> s =
                     ActivityScenario.launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.etAdminSearch))
                    .perform(typeText("john"), closeSoftKeyboard());
            onView(withId(R.id.etAdminSearch)).check(matches(withText("john")));
        }
    }

    /**
     * Counsellors tab button is clickable.
     */
    @Test
    public void adminDashboard_counsellorTabClickable() {
        seedAdminPrefs();
        try (ActivityScenario<AdminDashboardActivity> s =
                     ActivityScenario.launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.tabCounsellors)).perform(click());
            onView(withId(R.id.tabCounsellors)).check(matches(isDisplayed()));
        }
    }

    /**
     * Users tab button is clickable.
     */
    @Test
    public void adminDashboard_usersTabClickable() {
        seedAdminPrefs();
        try (ActivityScenario<AdminDashboardActivity> s =
                     ActivityScenario.launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.tabCounsellors)).perform(click());
            onView(withId(R.id.tabUsers)).perform(click());
            onView(withId(R.id.tabUsers)).check(matches(isDisplayed()));
        }
    }

    /**
     * User count label is visible.
     */
    @Test
    public void adminDashboard_countLabelVisible() {
        seedAdminPrefs();
        try (ActivityScenario<AdminDashboardActivity> s =
                     ActivityScenario.launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.tvAdminCount)).check(matches(isDisplayed()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION 19: Full end-to-end flows with live login
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Login as USER → open drawer → tap My Tasks → verify TasksOverview opens.
     */
    @Test
    public void e2e_loginThenOpenTasksOverview() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail))
                    .perform(typeText(TEST_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.inputPassword))
                    .perform(typeText(TEST_PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            Thread.sleep(4000);
            Intents.intended(hasComponent(HomeActivity.class.getName()));

            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemMyTasks)).perform(click());
            Thread.sleep(1000);
            Intents.intended(hasComponent(TasksOverview.class.getName()));
        }
    }

    /**
     * Login as USER → open drawer → tap Check-In → verify CheckInActivity opens.
     */
    @Test
    public void e2e_loginThenOpenCheckIn() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail))
                    .perform(typeText(TEST_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.inputPassword))
                    .perform(typeText(TEST_PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            Thread.sleep(4000);

            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemCheckIn)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(CheckInActivity.class.getName()));

            // Verify check-in UI is fully rendered
            onView(withId(R.id.circle1)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
        }
    }

    /**
     * Login as USER → open drawer → navigate to Worry Notes → tap Add.
     */
    @Test
    public void e2e_loginThenAddWorry() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail))
                    .perform(typeText(TEST_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.inputPassword))
                    .perform(typeText(TEST_PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            Thread.sleep(4000);

            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemWorryNotes)).perform(click());
            Thread.sleep(1000);
            Intents.intended(hasComponent(WorryNotes.class.getName()));

            onView(withId(R.id.btnAddWorryNote)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(AddWorryActivity.class.getName()));
            onView(withId(R.id.inputTitle)).check(matches(isDisplayed()));
        }
    }

    /**
     * Login as USER → navigate to Counsellor Search → search and filter.
     */
    @Test
    public void e2e_loginThenSearchCounsellors() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail))
                    .perform(typeText(TEST_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.inputPassword))
                    .perform(typeText(TEST_PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            Thread.sleep(4000);

            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemFindCounsellor)).perform(click());
            Thread.sleep(1500);

            Intents.intended(hasComponent(CounsellorSearchActivity.class.getName()));
            onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
            onView(withId(R.id.spinnerStatus)).check(matches(isDisplayed()));
            onView(withId(R.id.recyclerCounsellors)).check(matches(isDisplayed()));

            // Type in search field
            onView(withId(R.id.etSearch))
                    .perform(typeText("Smith"), closeSoftKeyboard());
            onView(withId(R.id.etSearch)).check(matches(withText("Smith")));
        }
    }
}