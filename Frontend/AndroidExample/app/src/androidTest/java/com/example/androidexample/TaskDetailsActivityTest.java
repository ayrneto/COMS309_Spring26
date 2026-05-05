package com.example.androidexample;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

@RunWith(AndroidJUnit4.class)
public class TaskDetailsActivityTest {

    @Test
    public void testTaskDetailsDisplay() {
        // Launch activity with task data
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TaskDetailsActivity.class);
        intent.putExtra("taskId", 123L);
        intent.putExtra("title", "Complete Assignment");
        intent.putExtra("description", "Finish the project report");
        intent.putExtra("dueDate", "2026-05-15");
        intent.putExtra("status", "Not Started");

        ActivityScenario.launch(intent);

        // Verify data is displayed
        onView(withId(R.id.taskTitle))
                .check(matches(withText("Complete Assignment")));

        onView(withId(R.id.taskDescription))
                .check(matches(withText("Finish the project report")));

        onView(withId(R.id.taskDueDate))
                .check(matches(withText("Due: 2026-05-15")));
    }

    @Test
    public void testStatusSpinnerExists() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TaskDetailsActivity.class);
        intent.putExtra("taskId", 123L);
        intent.putExtra("title", "Task");
        intent.putExtra("description", "Description");
        intent.putExtra("dueDate", "2026-05-15");
        intent.putExtra("status", "Not Started");

        ActivityScenario.launch(intent);

        onView(withId(R.id.spinnerStatus))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonClick() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TaskDetailsActivity.class);
        intent.putExtra("taskId", 123L);
        intent.putExtra("title", "Task");
        intent.putExtra("description", "Description");
        intent.putExtra("dueDate", "2026-05-15");
        intent.putExtra("status", "Not Started");

        ActivityScenario<TaskDetailsActivity> scenario = ActivityScenario.launch(intent);

        onView(withId(R.id.btnBack))
                .perform(click());

        // Activity should finish
        scenario.close();
    }

    @Test
    public void testStatusSpinnerSelection() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TaskDetailsActivity.class);
        intent.putExtra("taskId", 123L);
        intent.putExtra("title", "Task");
        intent.putExtra("description", "Description");
        intent.putExtra("dueDate", "2026-05-15");
        intent.putExtra("status", "Not Started");

        ActivityScenario.launch(intent);

        // Click spinner
        onView(withId(R.id.spinnerStatus))
                .perform(click());

        // Select "Ongoing"
        onData(anything())
                .atPosition(1)
                .perform(click());

        // Status should update (calls backend)
    }

    @Test
    public void testAllFieldsDisplayed() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), TaskDetailsActivity.class);
        intent.putExtra("taskId", 456L);
        intent.putExtra("title", "Another Task");
        intent.putExtra("description", "More details here");
        intent.putExtra("dueDate", "2026-06-01");
        intent.putExtra("status", "Ongoing");

        ActivityScenario.launch(intent);

        onView(withId(R.id.taskTitle))
                .check(matches(isDisplayed()));

        onView(withId(R.id.taskDescription))
                .check(matches(isDisplayed()));

        onView(withId(R.id.taskDueDate))
                .check(matches(isDisplayed()));

        onView(withId(R.id.spinnerStatus))
                .check(matches(isDisplayed()));

        onView(withId(R.id.btnBack))
                .check(matches(isDisplayed()));
    }
}