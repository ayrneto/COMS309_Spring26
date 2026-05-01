package com.example.androidexample;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NakshatraSystemTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    // Test 1: Valid login navigates to home screen
    @Test
    public void testValidLoginNavigatesToHome() {
        onView(withId(R.id.inputEmail))
                .perform(clearText(), typeText("testuser@test.com"), closeSoftKeyboard());
        onView(withId(R.id.inputPassword))
                .perform(clearText(), typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());

        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }

        onView(withId(R.id.tvWelcome)).check(matches(isDisplayed()));
    }

    // Test 2: Empty fields — login button still displayed (stays on login screen)
    @Test
    public void testEmptyLoginFieldsStaysOnLogin() {
        onView(withId(R.id.inputEmail))
                .perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.inputPassword))
                .perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());

        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
    }

    // Test 3: Invalid credentials stays on login screen
    @Test
    public void testInvalidCredentialsStaysOnLogin() {
        onView(withId(R.id.inputEmail))
                .perform(clearText(), typeText("wrong@wrong.com"), closeSoftKeyboard());
        onView(withId(R.id.inputPassword))
                .perform(clearText(), typeText("wrongpassword"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());

        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }

        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
    }

    // Test 4: Navigate to signup screen from login
    @Test
    public void testNavigateToSignup() {
        onView(withId(R.id.btnSignUp)).perform(click());

        try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }

        onView(withId(R.id.btn_signup)).check(matches(isDisplayed()));
    }

    // Test 5: Signup screen shows all required fields
    @Test
    public void testSignupScreenFieldsDisplayed() {
        onView(withId(R.id.btnSignUp)).perform(click());

        try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }

        onView(withId(R.id.et_name)).check(matches(isDisplayed()));
        onView(withId(R.id.et_email)).check(matches(isDisplayed()));
        onView(withId(R.id.et_password)).check(matches(isDisplayed()));
        onView(withId(R.id.et_confirm_password)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_signup)).check(matches(isDisplayed()));
    }
}