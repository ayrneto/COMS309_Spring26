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
public class WorryNotesTest {

    @Rule
    public ActivityScenarioRule<WorryNotes> activityRule =
            new ActivityScenarioRule<>(WorryNotes.class);

    @Test
    public void testWorriesContainerDisplayed() {
        onView(withId(R.id.worriesContainer))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAddWorryNoteButtonExists() {
        onView(withId(R.id.btnAddWorryNote))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testAddWorryNoteButtonClick() {
        onView(withId(R.id.btnAddWorryNote))
                .perform(click());

        // Should launch AddWorryActivity
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

    @Test
    public void testActivityLoads() {
        // Activity loads and fetches notes from backend
        // Verify container is displayed
        onView(withId(R.id.worriesContainer))
                .check(matches(isDisplayed()));
    }
}