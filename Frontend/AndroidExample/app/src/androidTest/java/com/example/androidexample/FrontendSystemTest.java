package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;


@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FrontendSystemTest {

    private static final String TEST_EMAIL    = "frontendtest@calmify.com";
    private static final String TEST_PASSWORD = "TestPass123";
    private static final String TEST_NAME     = "Frontend Test";

    @BeforeClass
    public static void registerTestAccount() throws Exception {
        OkHttpClient client = new OkHttpClient();
        JSONObject body = new JSONObject();
        body.put("name", TEST_NAME);
        body.put("email", TEST_EMAIL);
        body.put("password", TEST_PASSWORD);
        body.put("confirmPassword", TEST_PASSWORD);
        body.put("role", "USER");

        RequestBody rb = RequestBody.create(body.toString(),
                MediaType.parse("application/json; charset=utf-8"));
        Request req = new Request.Builder().url(ApiConstants.SIGNUP).post(rb).build();
        try (Response r = client.newCall(req).execute()) {
            int code = r.code();
            if (code != 201 && code != 409)
                throw new AssertionError("Account setup failed — HTTP " + code);
        }
    }

    private void seedPrefs(String role) {
        SharedPreferences.Editor ed = ApplicationProvider.getApplicationContext()
                .getSharedPreferences("AA_PREFS", android.content.Context.MODE_PRIVATE).edit();
        ed.putString("USER_ID",    "1");
        ed.putString("USER_EMAIL", TEST_EMAIL);
        ed.putString("USER_NAME",  TEST_NAME);
        ed.putString("USER_ROLE",  role);
        ed.putString("USER_PIC_URL", "");
        ed.apply();
    }

    @Before  public void setUp()    { Intents.init(); }
    @After   public void tearDown() { Intents.release(); }

    // =========================================================================
    // TC-01  LandingActivity — both UI elements + both navigation paths
    // =========================================================================
    @Test
    public void tc01_landingActivity() throws InterruptedException {
        try (ActivityScenario<LandingActivity> s =
                     ActivityScenario.launch(LandingActivity.class)) {
            onView(withId(R.id.btnGetStarted)).check(matches(isDisplayed()));
            onView(withId(R.id.btnGoToLogin)).check(matches(isDisplayed()));
            onView(withId(R.id.btnGetStarted)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(SignUpActivity.class.getName()));
        }

        try (ActivityScenario<LandingActivity> s =
                     ActivityScenario.launch(LandingActivity.class)) {
            onView(withId(R.id.btnGoToLogin)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(LoginActivity.class.getName()));
        }
    }

    // =========================================================================
    // TC-02  LoginActivity — empty email, empty password, sign up link
    // =========================================================================
    @Test
    public void tc02_loginValidationAndSignUpLink() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail)).check(matches(isDisplayed()));
            onView(withId(R.id.inputPassword)).check(matches(isDisplayed()));
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));

            // Empty email → stays
            onView(withId(R.id.btnLogin)).perform(click());
            onView(withId(R.id.inputEmail)).check(matches(isDisplayed()));

            // Email filled, password empty → stays
            onView(withId(R.id.inputEmail))
                    .perform(replaceText("someone@test.com"), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            onView(withId(R.id.inputPassword)).check(matches(isDisplayed()));

            // Sign Up link → SignUpActivity
            onView(withId(R.id.btnSignUp)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(SignUpActivity.class.getName()));
        }
    }

    // =========================================================================
    // TC-03  LoginActivity — wrong credentials stay; valid credentials → Home
    // =========================================================================
    @Test
    public void tc03_loginWrongThenValid() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail))
                    .perform(replaceText("wrong@invalid.com"), closeSoftKeyboard());
            onView(withId(R.id.inputPassword))
                    .perform(replaceText("WrongPass999"), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            Thread.sleep(3000);
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
        }

        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail))
                    .perform(replaceText(TEST_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.inputPassword))
                    .perform(replaceText(TEST_PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            Thread.sleep(4000);
            Intents.intended(hasComponent(HomeActivity.class.getName()));
        }
    }

    // =========================================================================
    // TC-04  SignUpActivity — empty name, invalid email, short password, mismatch
    // =========================================================================
    @Test
    public void tc04_signupValidationBranches() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnSignUp)).perform(click());
            Thread.sleep(500);

            // Empty name → stays
            onView(withId(R.id.btn_signup)).perform(click());
            onView(withId(R.id.et_name)).check(matches(isDisplayed()));

            // Invalid email → stays
            onView(withId(R.id.et_name)).perform(replaceText("Test"), closeSoftKeyboard());
            onView(withId(R.id.et_email)).perform(replaceText("notanemail"), closeSoftKeyboard());
            onView(withId(R.id.btn_signup)).perform(click());
            onView(withId(R.id.et_email)).check(matches(isDisplayed()));

            // Short password → stays
            onView(withId(R.id.et_email)).perform(replaceText("valid@test.com"), closeSoftKeyboard());
            onView(withId(R.id.et_password)).perform(replaceText("abc"), closeSoftKeyboard());
            onView(withId(R.id.et_confirm_password)).perform(replaceText("abc"), closeSoftKeyboard());
            onView(withId(R.id.btn_signup)).perform(click());
            onView(withId(R.id.et_password)).check(matches(isDisplayed()));

            // Mismatched passwords → stays
            onView(withId(R.id.et_password)).perform(replaceText("LongPass1"), closeSoftKeyboard());
            onView(withId(R.id.et_confirm_password)).perform(replaceText("Different1"), closeSoftKeyboard());
            onView(withId(R.id.btn_signup)).perform(click());
            onView(withId(R.id.btn_signup)).check(matches(isDisplayed()));
        }
    }

    // =========================================================================
    // TC-05  HomeActivity — welcome text, hamburger, drawer header + USER items
    // =========================================================================
    @Test
    public void tc05_homeActivityUiAndDrawerHeader() throws InterruptedException {
        seedPrefs("USER");
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.tvWelcome)).check(matches(isDisplayed()));
            onView(withId(R.id.btnHamburger)).check(matches(isDisplayed()));

            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerEmail)).check(matches(withText(TEST_EMAIL)));
            onView(withId(R.id.drawerItemCheckIn)).check(matches(isDisplayed()));
            onView(withId(R.id.drawerItemWorryNotes)).check(matches(isDisplayed()));
            onView(withId(R.id.drawerItemFindCounsellor)).check(matches(isDisplayed()));
            onView(withId(R.id.drawerItemMyTasks)).check(matches(isDisplayed()));
            onView(withId(R.id.drawerItemAiChat)).check(matches(isDisplayed()));
        }
    }

    // =========================================================================
    // TC-06  HomeActivity drawer → CheckInActivity
    // =========================================================================
    @Test
    public void tc06_homeDrawerToCheckIn() throws InterruptedException {
        seedPrefs("USER");
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemCheckIn)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(CheckInActivity.class.getName()));
        }
    }

    // =========================================================================
    // TC-07  HomeActivity drawer → WorryNotes → AddWorry
    // =========================================================================
    @Test
    public void tc07_homeToWorryNotesToAddWorry() throws InterruptedException {
        seedPrefs("USER");
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemWorryNotes)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(WorryNotes.class.getName()));

            onView(withId(R.id.btnAddWorryNote)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(AddWorryActivity.class.getName()));
        }
    }

    // =========================================================================
    // TC-08  HomeActivity drawer → TasksOverview + CounsellorSearch
    // =========================================================================
    @Test
    public void tc08_homeDrawerToTasksAndCounsellorSearch() throws InterruptedException {
        seedPrefs("USER");
        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemMyTasks)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(TasksOverview.class.getName()));
        }

        try (ActivityScenario<HomeActivity> s =
                     ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemFindCounsellor)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(CounsellorSearchActivity.class.getName()));
        }
    }

    // =========================================================================
    // TC-09  CheckInActivity — all circles, description input, summary nav
    // =========================================================================
    @Test
    public void tc09_checkInActivityFullCoverage() throws InterruptedException {
        seedPrefs("USER");
        try (ActivityScenario<CheckInActivity> s =
                     ActivityScenario.launch(CheckInActivity.class)) {
            onView(withId(R.id.circle1)).check(matches(isDisplayed()));
            onView(withId(R.id.circle5)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSummary)).check(matches(isDisplayed()));

            for (int id : new int[]{R.id.circle1, R.id.circle2, R.id.circle3, R.id.circle4, R.id.circle5})
                onView(withId(id)).perform(click());

            onView(withId(R.id.inputDescription))
                    .perform(replaceText("Feeling okay today"), closeSoftKeyboard());
            onView(withId(R.id.inputDescription)).check(matches(withText("Feeling okay today")));

            onView(withId(R.id.btnSummary)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(CheckInSummaryActivity.class.getName()));
        }
    }

    // =========================================================================
    // TC-10  CheckInSummaryActivity + RoutineSummaryActivity — back + filter
    // =========================================================================
    @Test
    public void tc10_summaryActivities() {
        seedPrefs("USER");

        try (ActivityScenario<CheckInSummaryActivity> s =
                     ActivityScenario.launch(CheckInSummaryActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBack)).perform(click());
        }

        try (ActivityScenario<RoutineSummaryActivity> s =
                     ActivityScenario.launch(RoutineSummaryActivity.class)) {
            onView(withId(R.id.filterSpinner)).check(matches(isDisplayed()));
            onView(withId(R.id.filterSpinner)).perform(click());
            onData(anything()).atPosition(0).perform(click());
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // =========================================================================
    // TC-11  RoutineTrackerActivity — all fields, spinner, date picker, back
    // =========================================================================
    @Test
    public void tc11_routineTrackerActivity() {
        seedPrefs("USER");
        try (ActivityScenario<RoutineTrackerActivity> s =
                     ActivityScenario.launch(RoutineTrackerActivity.class)) {
            onView(withId(R.id.routineTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.routineDescription)).check(matches(isDisplayed()));
            onView(withId(R.id.routineStartDate)).check(matches(isDisplayed()));
            onView(withId(R.id.routineReminder)).check(matches(isDisplayed()));
            onView(withId(R.id.spinnerLabel)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));

            onView(withId(R.id.routineTitle)).perform(replaceText("Morning Run"), closeSoftKeyboard());
            onView(withId(R.id.routineDescription)).perform(replaceText("30 min jog"), closeSoftKeyboard());

            onView(withId(R.id.spinnerLabel)).perform(click());
            onData(anything()).atPosition(1).perform(click());

            onView(withId(R.id.routineStartDate)).perform(click());
            androidx.test.espresso.Espresso.pressBack();

            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // =========================================================================
    // TC-12  AddWorryActivity — validation + edit mode pre-fill
    // =========================================================================
    @Test
    public void tc12_addWorryActivity() {
        seedPrefs("USER");

        try (ActivityScenario<AddWorryActivity> s =
                     ActivityScenario.launch(AddWorryActivity.class)) {
            onView(withId(R.id.inputTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.inputContent)).check(matches(isDisplayed()));
            onView(withId(R.id.tvTopBarTitle)).check(matches(isDisplayed()));

            // Empty title → stays
            onView(withId(R.id.inputContent)).perform(replaceText("some content"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveWorry)).perform(click());
            onView(withId(R.id.btnSaveWorry)).check(matches(isDisplayed()));

            onView(withId(R.id.spinnerLabel)).perform(click());
            onData(anything()).atPosition(2).perform(click());
            onView(withId(R.id.btnBack)).perform(click());
        }

        // Edit mode — fields pre-filled, button says "Update"
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                AddWorryActivity.class);
        intent.putExtra("noteId", 77L);
        intent.putExtra("title",   "Pre-filled Title");
        intent.putExtra("content", "Pre-filled Content");
        intent.putExtra("dueDate", "2026-06-01");
        intent.putExtra("label",   "Work");

        try (ActivityScenario<AddWorryActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.inputTitle)).check(matches(withText("Pre-filled Title")));
            onView(withId(R.id.inputContent)).check(matches(withText("Pre-filled Content")));
            onView(withId(R.id.btnSaveWorry)).check(matches(withText("Update")));
        }
    }

    // =========================================================================
    // TC-13  TasksOverview + TaskDetailsActivity — intent data, spinner
    // =========================================================================
    @Test
    public void tc13_tasksOverviewAndDetails() {
        seedPrefs("USER");

        try (ActivityScenario<TasksOverview> s =
                     ActivityScenario.launch(TasksOverview.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            onView(withId(R.id.tasksContainer)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBack)).perform(click());
        }

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                TaskDetailsActivity.class);
        intent.putExtra("taskId",      1L);
        intent.putExtra("title",       "Fix login bug");
        intent.putExtra("description", "Button crashes on empty input");
        intent.putExtra("dueDate",     "2026-06-15");
        intent.putExtra("status",      "Ongoing");

        try (ActivityScenario<TaskDetailsActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.taskTitle)).check(matches(withText("Fix login bug")));
            onView(withId(R.id.taskDescription)).check(matches(withText("Button crashes on empty input")));
            onView(withId(R.id.spinnerStatus)).check(matches(isDisplayed()));
            onView(withId(R.id.spinnerStatus)).perform(click());
            onData(anything()).atPosition(2).perform(click());
        }
    }

    // =========================================================================
    // TC-14  CounsellorSearchActivity — search, status filter, rating filter, back
    // =========================================================================
    @Test
    public void tc14_counsellorSearchActivity() {
        seedPrefs("USER");
        try (ActivityScenario<CounsellorSearchActivity> s =
                     ActivityScenario.launch(CounsellorSearchActivity.class)) {
            onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
            onView(withId(R.id.spinnerStatus)).check(matches(isDisplayed()));
            onView(withId(R.id.spinnerRating)).check(matches(isDisplayed()));
            onView(withId(R.id.recyclerCounsellors)).check(matches(isDisplayed()));
            onView(withId(R.id.tvResultCount)).check(matches(isDisplayed()));

            onView(withId(R.id.etSearch)).perform(replaceText("Dr"), closeSoftKeyboard());
            onView(withId(R.id.etSearch)).check(matches(withText("Dr")));

            onView(withId(R.id.spinnerStatus)).perform(click());
            onData(anything()).atPosition(1).perform(click());

            onView(withId(R.id.spinnerRating)).perform(click());
            onData(anything()).atPosition(0).perform(click());

            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // =========================================================================
    // TC-15  ProfileActivity — email display, edit profile nav, worry notes nav
    // =========================================================================
    @Test
    public void tc15_profileActivity() throws InterruptedException {
        seedPrefs("USER");
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                ProfileActivity.class);
        intent.putExtra("username", TEST_EMAIL);

        try (ActivityScenario<ProfileActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.tvEmail)).check(matches(withText("Email: " + TEST_EMAIL)));
            onView(withId(R.id.btnEditProfile)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(EditProfile.class.getName()));
        }

        try (ActivityScenario<ProfileActivity> s = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnWorryNotes)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(WorryNotes.class.getName()));
        }
    }

    // =========================================================================
    // TC-16  EditProfile — pre-fill from prefs, mismatched passwords, empty name
    // =========================================================================
    @Test
    public void tc16_editProfileValidation() {
        seedPrefs("USER");
        try (ActivityScenario<EditProfile> s =
                     ActivityScenario.launch(EditProfile.class)) {
            onView(withId(R.id.editName)).check(matches(withText(TEST_NAME)));
            onView(withId(R.id.editEmail)).check(matches(withText(TEST_EMAIL)));

            // Mismatched passwords → stays
            onView(withId(R.id.editPassword)).perform(replaceText("NewPass123"), closeSoftKeyboard());
            onView(withId(R.id.editConfirmPassword)).perform(replaceText("Different1"), closeSoftKeyboard());
            onView(withId(R.id.btnSave)).perform(click());
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));

            // Empty name/email → stays
            onView(withId(R.id.editName)).perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.editEmail)).perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.editPassword)).perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.editConfirmPassword)).perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.btnSave)).perform(click());
            onView(withId(R.id.btnSave)).check(matches(isDisplayed()));

            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // =========================================================================
    // TC-17  PrescriptionsActivity + SharedNotesActivity — back buttons
    // =========================================================================
    @Test
    public void tc17_prescriptionsAndSharedNotes() {
        seedPrefs("USER");

        try (ActivityScenario<PrescriptionsActivity> s =
                     ActivityScenario.launch(PrescriptionsActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            onView(withId(R.id.prescriptionsContainer)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBack)).perform(click());
        }

        try (ActivityScenario<SharedNotesActivity> s =
                     ActivityScenario.launch(SharedNotesActivity.class)) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // =========================================================================
    // TC-18  AdminDashboardActivity — search, tabs, count label
    // =========================================================================
    @Test
    public void tc18_adminDashboard() {
        seedPrefs("ADMIN");
        try (ActivityScenario<AdminDashboardActivity> s =
                     ActivityScenario.launch(AdminDashboardActivity.class)) {
            onView(withId(R.id.etAdminSearch)).check(matches(isDisplayed()));
            onView(withId(R.id.tabUsers)).check(matches(isDisplayed()));
            onView(withId(R.id.tabCounsellors)).check(matches(isDisplayed()));
            onView(withId(R.id.recyclerAdminList)).check(matches(isDisplayed()));
            onView(withId(R.id.tvAdminCount)).check(matches(isDisplayed()));

            onView(withId(R.id.etAdminSearch)).perform(replaceText("john"), closeSoftKeyboard());
            onView(withId(R.id.etAdminSearch)).check(matches(withText("john")));

            onView(withId(R.id.tabCounsellors)).perform(click());
            onView(withId(R.id.tabUsers)).perform(click());
        }
    }

    // =========================================================================
    // TC-19  E2E: login → Home → Check-In → rate + describe → Summary
    // =========================================================================
    @Test
    public void tc19_e2e_loginToCheckInToSummary() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail)).perform(replaceText(TEST_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.inputPassword)).perform(replaceText(TEST_PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(click());
            Thread.sleep(4000);
            Intents.intended(hasComponent(HomeActivity.class.getName()));

            onView(withId(R.id.btnHamburger)).perform(click());
            Thread.sleep(500);
            onView(withId(R.id.drawerItemCheckIn)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(CheckInActivity.class.getName()));

            onView(withId(R.id.circle4)).perform(click());
            onView(withId(R.id.inputDescription)).perform(replaceText("Good day"), closeSoftKeyboard());
            onView(withId(R.id.btnSummary)).perform(click());
            Thread.sleep(500);
            Intents.intended(hasComponent(CheckInSummaryActivity.class.getName()));
            onView(withId(R.id.btnBack)).perform(click());
        }
    }

    // =========================================================================
    // TC-20  E2E: login → Home → Worry Notes → Add Worry (fill + save)
    // =========================================================================
    @Test
    public void tc20_e2e_loginToWorryNotesToAddWorry() throws InterruptedException {
        try (ActivityScenario<LoginActivity> s =
                     ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.inputEmail)).perform(replaceText(TEST_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.inputPassword)).perform(replaceText(TEST_PASSWORD), closeSoftKeyboard());
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

            onView(withId(R.id.inputTitle)).perform(replaceText("Exam stress"), closeSoftKeyboard());
            onView(withId(R.id.inputContent)).perform(replaceText("Worried about finals"), closeSoftKeyboard());
            onView(withId(R.id.spinnerLabel)).perform(click());
            onData(anything()).atPosition(2).perform(click());
            onView(withId(R.id.btnSaveWorry)).perform(click());
            Thread.sleep(2000);
        }
    }
}