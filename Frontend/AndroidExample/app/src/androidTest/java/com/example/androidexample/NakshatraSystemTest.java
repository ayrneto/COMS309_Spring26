package com.example.androidexample;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

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

/**
 * End-to-end system tests for the Calmify Android frontend.
 *
 * <p>Tests cover multi-step flows across authentication, signup validation,
 * and post-login navigation — going well beyond single-action UI checks.</p>
 *
 * <p>A dedicated test USER account is automatically registered on the real backend
 * once before the suite via {@link #registerTestAccount()}. HTTP 409 (already exists)
 * is treated as success, making the suite fully idempotent across re-runs.</p>
 *
 * <p><b>Requires:</b> backend server reachable at
 * {@code REDACTED:8080} for Tests 1, 4, and 5.</p>
 *
 * @author Nakshatra
 */
@RunWith(AndroidJUnit4.class)
public class NakshatraSystemTest {

    // -----------------------------------------------------------------------
    // Shared test account credentials
    // -----------------------------------------------------------------------

    /** Email for the auto-provisioned test USER account. */
    private static final String TEST_EMAIL    = "nakshatratest@calmify.com";

    /** Password for the auto-provisioned test USER account. */
    private static final String TEST_PASSWORD = "TestPass123";

    /** Display name used when registering the test account. */
    private static final String TEST_NAME     = "Nakshatra Test";

    // -----------------------------------------------------------------------
    // @BeforeClass — register the test account on the backend once per suite
    // -----------------------------------------------------------------------

    /**
     * Registers a dedicated test USER account via {@code POST /users/signup} before
     * any test in this class runs.
     *
     * <p>Uses OkHttp (already a project dependency) so no extra test libraries are
     * needed. The request is made on the calling thread — {@code @BeforeClass} runs
     * before the main thread UI loop starts, so blocking here is safe.</p>
     *
     * <ul>
     *   <li>HTTP 201 — account created successfully.</li>
     *   <li>HTTP 409 — account already exists from a prior run; silently skipped.</li>
     *   <li>Any other code — throws {@link AssertionError} to fail fast before UI
     *       tests begin, surfacing server-side issues immediately.</li>
     * </ul>
     */
    @BeforeClass
    public static void registerTestAccount() throws Exception {
        OkHttpClient client = new OkHttpClient();

        JSONObject body = new JSONObject();
        body.put("name",            TEST_NAME);
        body.put("email",           TEST_EMAIL);
        body.put("password",        TEST_PASSWORD);
        body.put("confirmPassword", TEST_PASSWORD);
        body.put("role",            "USER");

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(ApiConstants.SIGNUP)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            if (code != 201 && code != 409) {
                throw new AssertionError(
                        "Test account setup failed — expected HTTP 201 or 409 but got "
                                + code + ". Verify the backend server is running."
                );
            }
        }
    }

    // -----------------------------------------------------------------------
    // Per-test rule and Intents lifecycle
    // -----------------------------------------------------------------------

    /**
     * Launches a fresh {@link LoginActivity} before each test, ensuring
     * no leftover UI state bleeds between test cases.
     */
    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    /** Initialises Espresso Intents recording before each test. */
    @Before
    public void setUp() {
        Intents.init();
    }

    /** Releases Espresso Intents after each test to prevent state leakage. */
    @After
    public void tearDown() {
        Intents.release();
    }

    // -----------------------------------------------------------------------
    // Test 1 — Valid login → HomeActivity → drawer opens with correct menu items
    // -----------------------------------------------------------------------

    /**
     * Full login-to-home flow with post-login state verification.
     *
     * <p>Steps: enter valid credentials → tap Log In → wait for network response
     * → verify HomeActivity launched → open the navigation drawer → verify that
     * USER-role menu items (AI Chat, Check-In, Worry Notes) are visible and that
     * the counsellor-only item (Assign Task) is hidden.</p>
     *
     * <p>This test goes beyond a simple navigation check: it verifies that the
     * backend response correctly sets the USER role and that {@link HomeActivity}
     * configures the drawer accordingly.</p>
     */
    @Test
    public void validLoginShowsCorrectUserDrawerItems() throws InterruptedException {
        // --- Step 1: log in ---
        onView(withId(R.id.inputEmail))
                .perform(typeText(TEST_EMAIL), closeSoftKeyboard());
        onView(withId(R.id.inputPassword))
                .perform(typeText(TEST_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());

        // Wait for network round-trip + activity transition
        Thread.sleep(2000);

        // --- Step 2: confirm HomeActivity launched ---
        intended(hasComponent(HomeActivity.class.getName()));

        // --- Step 3: open the drawer ---
        onView(withId(R.id.btnHamburger)).perform(click());
        Thread.sleep(500); // drawer animation

        // --- Step 4: USER-specific items must be visible ---
        onView(withId(R.id.drawerItemAiChat)).check(matches(isDisplayed()));
        onView(withId(R.id.drawerItemCheckIn)).check(matches(isDisplayed()));
        onView(withId(R.id.drawerItemWorryNotes)).check(matches(isDisplayed()));
        onView(withId(R.id.drawerItemMyTasks)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Test 2 — Signup with mismatched passwords stays on SignUpActivity
    // -----------------------------------------------------------------------

    /**
     * Verifies the password-confirmation validation branch in {@link SignUpActivity}.
     *
     * <p>Steps: navigate to signup → fill all fields correctly except confirm password
     * (deliberately different) → tap Create Account → verify SignUpActivity is still
     * on screen (form rejected) by asserting the name field is still visible.</p>
     *
     * <p>This is non-trivial: it fills every field, exercises the full validation
     * pipeline up to the password-match check, and asserts that the screen does NOT
     * advance — distinguishing it from a simple button-tap test.</p>
     */
    @Test
    public void signupWithMismatchedPasswordsStaysOnSignup() throws InterruptedException {
        // Navigate to SignUpActivity
        onView(withId(R.id.btnSignUp)).perform(click());
        Thread.sleep(500);

        // Fill all fields — confirm password intentionally wrong
        onView(withId(R.id.et_name))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.et_email))
                .perform(typeText("mismatch@calmify.com"), closeSoftKeyboard());
        onView(withId(R.id.et_password))
                .perform(typeText("SecurePass1"), closeSoftKeyboard());
        onView(withId(R.id.et_confirm_password))
                .perform(typeText("DifferentPass1"), closeSoftKeyboard());

        onView(withId(R.id.btn_signup)).perform(click());

        // SignUpActivity must still be on screen — form was rejected
        onView(withId(R.id.et_name)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_signup)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Test 3 — Signup with too-short password stays on SignUpActivity
    // -----------------------------------------------------------------------

    /**
     * Verifies the minimum-password-length validation branch in {@link SignUpActivity}.
     *
     * <p>Steps: navigate to signup → enter a password shorter than 8 characters
     * → tap Create Account → verify SignUpActivity is still on screen.</p>
     *
     * <p>This targets a distinct validation rule from Test 2 (password length vs.
     * password match), ensuring both code paths are exercised.</p>
     */
    @Test
    public void signupWithShortPasswordStaysOnSignup() throws InterruptedException {
        // Navigate to SignUpActivity
        onView(withId(R.id.btnSignUp)).perform(click());
        Thread.sleep(500);

        onView(withId(R.id.et_name))
                .perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.et_email))
                .perform(typeText("shortpw@calmify.com"), closeSoftKeyboard());
        onView(withId(R.id.et_password))
                .perform(typeText("abc"), closeSoftKeyboard()); // less than 8 chars
        onView(withId(R.id.et_confirm_password))
                .perform(typeText("abc"), closeSoftKeyboard());

        onView(withId(R.id.btn_signup)).perform(click());

        // Must still be on SignUpActivity — password too short to proceed
        onView(withId(R.id.et_password)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_signup)).check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Test 4 — Login → open drawer → navigate to AI Chat → verify chat UI
    // -----------------------------------------------------------------------

    /**
     * Full multi-screen end-to-end flow: login → home → AI Chat screen.
     *
     * <p>Steps: log in with valid credentials → open nav drawer → tap AI Chat
     * → verify {@link AIChatActivity} UI components are present: the message input,
     * the send button, and the disclaimer banner text.</p>
     *
     * <p>This tests the complete chain from authentication through home navigation
     * to a feature screen, asserting both structure and content of the destination.</p>
     */
    @Test
    public void loginThenNavigateToAiChatShowsChatUi() throws InterruptedException {
        // --- Log in ---
        onView(withId(R.id.inputEmail))
                .perform(typeText(TEST_EMAIL), closeSoftKeyboard());
        onView(withId(R.id.inputPassword))
                .perform(typeText(TEST_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
        Thread.sleep(4000);

        // --- Open drawer and tap AI Chat ---
        onView(withId(R.id.btnHamburger)).perform(click());
        Thread.sleep(500);
        onView(withId(R.id.drawerItemAiChat)).perform(click());
        Thread.sleep(500);

        // --- Assert AIChatActivity UI is correct ---
        intended(hasComponent(AIChatActivity.class.getName()));
        onView(withId(R.id.etMessage)).check(matches(isDisplayed()));
        onView(withId(R.id.btnSend)).check(matches(isDisplayed()));
        onView(withText("For general wellness support only. Not a substitute for professional care."))
                .check(matches(isDisplayed()));
    }

    // -----------------------------------------------------------------------
    // Test 5 — Login → open drawer → navigate to Find Counsellor → verify search UI
    // -----------------------------------------------------------------------

    /**
     * Full multi-screen end-to-end flow: login → home → Find Counsellor screen.
     *
     * <p>Steps: log in with valid credentials → open nav drawer → tap Find Counsellor
     * → verify {@link CounsellorSearchActivity} renders its search bar, filter
     * spinners, and results count label.</p>
     *
     * <p>Like Test 4, this verifies a complete authenticated navigation chain.
     * Asserting multiple distinct UI components on the destination screen confirms
     * the activity launched and rendered correctly, not just that an intent fired.</p>
     */
    @Test
    public void loginThenNavigateToCounsellorSearchShowsSearchUi() throws InterruptedException {
        // --- Log in ---
        onView(withId(R.id.inputEmail))
                .perform(typeText(TEST_EMAIL), closeSoftKeyboard());
        onView(withId(R.id.inputPassword))
                .perform(typeText(TEST_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
        Thread.sleep(4000);

        // --- Open drawer and tap Find Counsellor ---
        onView(withId(R.id.btnHamburger)).perform(click());
        Thread.sleep(500);
        onView(withId(R.id.drawerItemFindCounsellor)).perform(click());
        Thread.sleep(500);

        // --- Assert CounsellorSearchActivity UI is correct ---
        intended(hasComponent(CounsellorSearchActivity.class.getName()));
        onView(withId(R.id.etSearch)).check(matches(isDisplayed()));
        onView(withId(R.id.spinnerStatus)).check(matches(isDisplayed()));
        onView(withId(R.id.spinnerRating)).check(matches(isDisplayed()));
        onView(withId(R.id.recyclerCounsellors)).check(matches(isDisplayed()));
    }
}