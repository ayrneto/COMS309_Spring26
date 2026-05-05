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
public class RoutineSummaryActivityTest {

    @Rule
    public ActivityScenarioRule<RoutineSummaryActivity> activityRule =
            new ActivityScenarioRule<>(RoutineSummaryActivity.class);

    @Test
    public void testRoutinesContainerDisplayed() {
        onView(withId(R.id.routinesContainer))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testFilterSpinnerDisplayed() {
        onView(withId(R.id.filterSpinner))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAddRoutineButtonDisplayed() {
        onView(withId(R.id.btnAddRoutine))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAddRoutineButtonClick() {
        onView(withId(R.id.btnAddRoutine))
                .perform(click());

        // Should launch RoutineTrackerActivity
    }

    @Test
    public void testFilterSpinnerClick() {
        onView(withId(R.id.filterSpinner))
                .perform(click());

        // Dropdown opens
    }

    @Test
    public void testActivityLoads() {
        // Activity loads and fetches routines
        // Verify container is displayed
        onView(withId(R.id.routinesContainer))
                .check(matches(isDisplayed()));
    }
}