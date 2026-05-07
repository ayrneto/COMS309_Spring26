package com.example.androidexample;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
public class AdminEditUserActivityTest {

    @Test
    public void testActivityWithUserRole() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminEditUserActivity.class);
        intent.putExtra("userId", 123L);
        intent.putExtra("userName", "John Doe");
        intent.putExtra("userEmail", "john@example.com");
        intent.putExtra("userRole", "USER");
        intent.putExtra("userActive", true);
        intent.putExtra("userPassword", "pass123");
        intent.putExtra("userConfirmPassword", "pass123");

        ActivityScenario.launch(intent);

        onView(withId(R.id.etEditName))
                .check(matches(withText("John Doe")));

        onView(withId(R.id.etEditEmail))
                .check(matches(withText("john@example.com")));
    }

    @Test
    public void testActivityWithCounsellorRole() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminEditUserActivity.class);
        intent.putExtra("userId", 456L);
        intent.putExtra("userName", "Dr. Smith");
        intent.putExtra("userEmail", "smith@example.com");
        intent.putExtra("userRole", "COUNSELLOR");
        intent.putExtra("userActive", true);
        intent.putExtra("cDisplayName", "Dr. Smith");
        intent.putExtra("cSpecialization", "Anxiety");
        intent.putExtra("cBio", "Experienced therapist");
        intent.putExtra("cStatus", "AVAILABLE");

        ActivityScenario.launch(intent);

        // Verify counsellor fields are visible
        onView(withId(R.id.counsellorFieldsSection))
                .check(matches(isDisplayed()));

        onView(withId(R.id.etCDisplayName))
                .check(matches(withText("Dr. Smith")));
    }

    @Test
    public void testNameInput() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminEditUserActivity.class);
        intent.putExtra("userId", 123L);
        intent.putExtra("userRole", "USER");
        intent.putExtra("userActive", true);

        ActivityScenario.launch(intent);

        onView(withId(R.id.etEditName))
                .perform(typeText("New Name"), closeSoftKeyboard());

        onView(withId(R.id.etEditName))
                .check(matches(withText("New Name")));
    }

    @Test
    public void testBackButtonClick() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminEditUserActivity.class);
        intent.putExtra("userId", 123L);
        intent.putExtra("userRole", "USER");
        intent.putExtra("userActive", true);

        ActivityScenario<AdminEditUserActivity> scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.btnBack))
                .perform(click());

        scenario.close();
    }

    @Test
    public void testSaveUserButtonExists() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminEditUserActivity.class);
        intent.putExtra("userId", 123L);
        intent.putExtra("userRole", "USER");
        intent.putExtra("userActive", true);

        ActivityScenario.launch(intent);

        onView(withId(R.id.btnSaveUser))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDeleteButtonExists() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminEditUserActivity.class);
        intent.putExtra("userId", 123L);
        intent.putExtra("userRole", "USER");
        intent.putExtra("userActive", true);

        ActivityScenario.launch(intent);

        onView(withId(R.id.btnDeleteUser))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDeactivateButtonExists() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminEditUserActivity.class);
        intent.putExtra("userId", 123L);
        intent.putExtra("userRole", "USER");
        intent.putExtra("userActive", true);

        ActivityScenario.launch(intent);

        onView(withId(R.id.btnDeactivate))
                .check(matches(isDisplayed()));
    }
}