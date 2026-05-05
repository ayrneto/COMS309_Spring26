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
public class RoutineTrackerActivityTest {

    @Rule
    public ActivityScenarioRule<RoutineTrackerActivity> activityRule =
            new ActivityScenarioRule<>(RoutineTrackerActivity.class);

    @Test
    public void testTitleInput() {
        onView(withId(R.id.routineTitle))
                .perform(typeText("Morning Exercise"), closeSoftKeyboard());

        onView(withId(R.id.routineTitle))
                .check(matches(withText("Morning Exercise")));
    }

    @Test
    public void testDescriptionInput() {
        onView(withId(R.id.routineDescription))
                .perform(typeText("30 min cardio daily"), closeSoftKeyboard());

        onView(withId(R.id.routineDescription))
                .check(matches(withText("30 min cardio daily")));
    }

    @Test
    public void testSaveButtonExists() {
        onView(withId(R.id.btnSave))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testStartDateFieldExists() {
        onView(withId(R.id.routineStartDate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testReminderFieldExists() {
        onView(withId(R.id.routineReminder))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSaveWithEmptyTitle() {
        onView(withId(R.id.routineDescription))
                .perform(typeText("Description"), closeSoftKeyboard());

        onView(withId(R.id.routineStartDate))
                .perform(typeText("2026-05-15"), closeSoftKeyboard());

        onView(withId(R.id.btnSave))
                .perform(click());

        // Should stay on screen - validation failed
        onView(withId(R.id.btnSave))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSaveWithEmptyStartDate() {
        onView(withId(R.id.routineTitle))
                .perform(typeText("Exercise"), closeSoftKeyboard());

        onView(withId(R.id.routineDescription))
                .perform(typeText("Description"), closeSoftKeyboard());

        onView(withId(R.id.btnSave))
                .perform(click());

        onView(withId(R.id.btnSave))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSaveWithAllFields() {
        onView(withId(R.id.routineTitle))
                .perform(typeText("Daily Meditation"), closeSoftKeyboard());

        onView(withId(R.id.routineDescription))
                .perform(typeText("10 min mindfulness"), closeSoftKeyboard());

        onView(withId(R.id.routineStartDate))
                .perform(typeText("2026-05-01"), closeSoftKeyboard());

        onView(withId(R.id.routineReminder))
                .perform(typeText("07:00"), closeSoftKeyboard());

        onView(withId(R.id.spinnerLabel))
                .perform(click());
        onData(anything()).atPosition(1).perform(click());

        onView(withId(R.id.btnSave))
                .perform(click());

        // Should save and finish activity
    }

    @Test
    public void testSaveWithoutReminder() {
        onView(withId(R.id.routineTitle))
                .perform(typeText("Reading"), closeSoftKeyboard());

        onView(withId(R.id.routineDescription))
                .perform(typeText("30 min per day"), closeSoftKeyboard());

        onView(withId(R.id.routineStartDate))
                .perform(typeText("2026-05-10"), closeSoftKeyboard());

        // Don't fill reminder - it's optional

        onView(withId(R.id.btnSave))
                .perform(click());

        // Should save successfully
    }
}