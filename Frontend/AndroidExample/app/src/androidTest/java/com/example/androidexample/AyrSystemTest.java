package com.example.androidexample;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class AyrSystemTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Test
    public void testEmailInput() {
        // Type email
        onView(withId(R.id.inputEmail))
                .perform(typeText("test@example.com"), closeSoftKeyboard());

        // Verify email appears
        onView(withId(R.id.inputEmail))
                .check(matches(withText("test@example.com")));
    }

    @Test
    public void testPasswordInput() {
        // Type password
        onView(withId(R.id.inputPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Verify password appears
        onView(withId(R.id.inputPassword))
                .check(matches(withText("password123")));
    }

    @Test
    public void testLoginButtonExists() {
        // Check login button is displayed
        onView(withId(R.id.btnLogin))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSignupButtonClick() {
        // Click signup button
        onView(withId(R.id.btnSignUp))
                .perform(click());

        // Verify we're on signup screen
        onView(withId(R.id.btn_signup))
                .check(matches(isDisplayed()));
    }
}
