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
public class AppointmentRequestsActivityTest {

    @Rule
    public ActivityScenarioRule<AppointmentRequestsActivity> activityRule =
            new ActivityScenarioRule<>(AppointmentRequestsActivity.class);

//    @Test
//    public void testRecyclerViewExists() {
//        onView(withId(R.id.recyclerRequests))
//                .check(matches(isDisplayed()));
//    }

    @Test
    public void testRequestCountExists() {
        onView(withId(R.id.tvRequestCount))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyTextExists() {
        onView(withId(R.id.tvEmpty))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonExists() {
        onView(withId(R.id.btnBack))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonClick() {
        onView(withId(R.id.btnBack))
                .perform(click());

        // Activity should finish
    }

//    @Test
//    public void testActivityLoads() {
//        // Activity loads and fetches appointment requests
//        onView(withId(R.id.recyclerRequests))
//                .check(matches(isDisplayed()));
//    }
}