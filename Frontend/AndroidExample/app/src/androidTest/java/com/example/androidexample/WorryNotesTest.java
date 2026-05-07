package com.example.androidexample;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class WorryNotesTest {

    @Rule
    public ActivityScenarioRule<WorryNotes> activityRule =
            new ActivityScenarioRule<>(WorryNotes.class);

    @Test
    public void testWorriesContainerDisplayed() {
        Espresso.onView(ViewMatchers.withId(R.id.worriesContainer))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testAddWorryNoteButtonExists() {
        Espresso.onView(ViewMatchers.withId(R.id.btnAddWorryNote))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testAddWorryNoteButtonClick() {
        Espresso.onView(ViewMatchers.withId(R.id.btnAddWorryNote))
                .perform(ViewActions.click());

        // Should launch AddWorryActivity
    }

    @Test
    public void testBackButtonExists() {
        Espresso.onView(ViewMatchers.withId(R.id.btnBack))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void testBackButtonClick() {
        Espresso.onView(ViewMatchers.withId(R.id.btnBack))
                .perform(ViewActions.click());

        // Activity should finish
    }

    @Test
    public void testActivityLoads() {
        // Activity loads and fetches notes from backend
        // Verify container is displayed
        Espresso.onView(ViewMatchers.withId(R.id.worriesContainer))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }
}