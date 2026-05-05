package com.example.androidexample;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class TasksOverviewTest {

    @Rule
    public ActivityScenarioRule<TasksOverview> activityRule =
            new ActivityScenarioRule<>(TasksOverview.class);

    @Test
    public void testTasksContainerDisplayed() {
        onView(withId(R.id.tasksContainer))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testActivityLoads() {
        // Activity loads and fetches tasks from backend
        // Verify container is displayed
        onView(withId(R.id.tasksContainer))
                .check(matches(isDisplayed()));
    }
}