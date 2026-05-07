package com.example.androidexample;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testEmailInput() {
        onView(withId(R.id.inputEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());

        onView(withId(R.id.inputEmail))
                .check(matches(withText("test@example.com")));
    }

    @Test
    public void testPasswordInput() {
        onView(withId(R.id.inputPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        onView(withId(R.id.inputPassword))
                .check(matches(withText("password123")));
    }

    @Test
    public void testRoleSpinnerExists() {
        onView(withId(R.id.spinner_role))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testRoleSpinnerSelection() {
        onView(withId(R.id.spinner_role))
                .perform(click());

        onData(anything())
                .atPosition(1)
                .perform(click());

        onView(withId(R.id.spinner_role))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLoginButtonExists() {
        onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSignUpButtonExists() {
        onView(withId(R.id.btnSignUp))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSignUpButtonClick() {
        onView(withId(R.id.btnSignUp))
                .perform(click());

        // Should launch SignUpActivity
    }

    @Test
    public void testLoginWithEmptyEmail() {
        onView(withId(R.id.inputPassword))
                .perform(typeText("password"), closeSoftKeyboard());

        onView(withId(R.id.btnLogin))
                .perform(click());

        // Should stay on login screen
        onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLoginWithEmptyPassword() {
        onView(withId(R.id.inputEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());

        onView(withId(R.id.btnLogin))
                .perform(click());

        // Should stay on login screen
        onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLoginWithBothFields() {
        onView(withId(R.id.inputEmail))
                .perform(typeText("test@test.com"), closeSoftKeyboard());

        onView(withId(R.id.inputPassword))
                .perform(typeText("pass123"), closeSoftKeyboard());

        onView(withId(R.id.btnLogin))
                .perform(click());

        // Login request sent (may fail if credentials wrong, but code executes)
    }
}