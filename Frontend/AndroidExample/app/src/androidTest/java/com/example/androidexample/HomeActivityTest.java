package com.example.androidexample;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class HomeActivityTest {

    @Before
    public void setup() {
        // Set up a logged-in USER for testing
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("AA_PREFS", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("USER_ID", "1")
                .putString("USER_EMAIL", "test@example.com")
                .putString("USER_NAME", "Test User")
                .putString("USER_ROLE", "USER")
                .apply();
    }

    @Rule
    public ActivityScenarioRule<HomeActivity> activityRule =
            new ActivityScenarioRule<>(HomeActivity.class);

    @Test
    public void testWelcomeTextDisplayed() {
        onView(withId(R.id.tvWelcome))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDashboardContainerDisplayed() {
        onView(withId(R.id.dashboardContainer))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testHamburgerButtonClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        // Drawer should open
        onView(withId(R.id.drawerLayout))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDrawerWorryNotesClick() {
        // Open drawer
        onView(withId(R.id.btnHamburger))
                .perform(click());

        // Click Worry Notes
        onView(withId(R.id.drawerItemWorryNotes))
                .perform(click());

        // Activity should launch
    }

    @Test
    public void testDrawerCheckInClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemCheckIn))
                .perform(click());
    }

    @Test
    public void testDrawerMyTasksClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemMyTasks))
                .perform(click());
    }

    @Test
    public void testDrawerRoutineTrackerClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerRoutineTracker))
                .perform(click());
    }

    @Test
    public void testDrawerFindCounsellorClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemFindCounsellor))
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
    public void testDrawerAiChatClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemAiChat))
                .perform(click());
    }

    @Test
    public void testDrawerPrescriptionsClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemPrescriptions))
                .perform(click());
    }

    @Test
    public void testDrawerEditProfileClick() {
        onView(withId(R.id.btnHamburger))
                .perform(click());

        onView(withId(R.id.drawerItemEditProfile))
                .perform(click());
    }
}