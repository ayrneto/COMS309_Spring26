package com.example.androidexample;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class CounselorHomeActivityTest {

    @Rule
    public ActivityScenarioRule<CounselorHomeActivity> activityRule =
            new ActivityScenarioRule<>(CounselorHomeActivity.class);

    @Test
    public void testWelcomeTextExists() {
        onView(withId(R.id.tvWelcome))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testHamburgerButtonExists() {
        onView(withId(R.id.btnHamburger))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testStatusToggleButtonExists() {
        onView(withId(R.id.btnStatusToggle))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testHamburgerButtonClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        // Drawer opens
        onView(withId(R.id.drawerLayout))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testStatusToggleClick() {
        onView(withId(R.id.btnStatusToggle))
                .perform(click());

        // Status cycles to next value
        onView(withId(R.id.btnStatusToggle))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDrawerAppointmentsClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemAppointments))
                .perform(click());
    }

    @Test
    public void testDrawerChatClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemChat))
                .perform(click());
    }

    @Test
    public void testDrawerAssignTaskClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemAssignTask))
                .perform(click());
    }

    @Test
    public void testDrawerSharedNotesClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemSharedNotes))
                .perform(click());
    }

    @Test
    public void testDrawerEditProfileClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemEditProfile))
                .perform(click());
    }

    @Test
    public void testActivityLoads() {
        // Activity loads and fetches stats, appointments, activity
        onView(withId(R.id.tvWelcome))
                .check(matches(isDisplayed()));
    }
}