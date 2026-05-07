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

@RunWith(AndroidJUnit4.class)
public class AdminDashboardActivityTest {

    @Rule
    public ActivityScenarioRule<AdminDashboardActivity> activityRule =
            new ActivityScenarioRule<>(AdminDashboardActivity.class);

    @Test
    public void testRecyclerViewExists() {
        onView(withId(R.id.recyclerAdminList))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSearchFieldExists() {
        onView(withId(R.id.etAdminSearch))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testTabUsersExists() {
        onView(withId(R.id.tabUsers))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testTabCounsellorsExists() {
        onView(withId(R.id.tabCounsellors))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCountTextExists() {
        onView(withId(R.id.tvAdminCount))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testLogoutButtonExists() {
        onView(withId(R.id.btnAdminLogout))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testTabUsersClick() {
        onView(withId(R.id.tabUsers))
                .perform(click());

        // Should reload users list
        onView(withId(R.id.recyclerAdminList))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testTabCounsellorsClick() {
        onView(withId(R.id.tabCounsellors))
                .perform(click());

        // Should reload counselors list
        onView(withId(R.id.recyclerAdminList))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSearchInput() {
        onView(withId(R.id.etAdminSearch))
                .perform(typeText("test"), closeSoftKeyboard());

        // Triggers applyFilter()
        onView(withId(R.id.recyclerAdminList))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testActivityLoads() {
        // Activity loads and fetches users
        onView(withId(R.id.recyclerAdminList))
                .check(matches(isDisplayed()));
    }
}