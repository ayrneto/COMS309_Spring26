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
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class AssignTaskActivityTest {

    @Rule
    public ActivityScenarioRule<AssignTaskActivity> activityRule =
            new ActivityScenarioRule<>(AssignTaskActivity.class);

    @Test
    public void testEmailInput() {
        onView(withId(R.id.taskEmail))
                .perform(typeText("user@example.com"), closeSoftKeyboard());

        onView(withId(R.id.taskEmail))
                .check(matches(withText("user@example.com")));
    }

    @Test
    public void testTitleInput() {
        onView(withId(R.id.taskTitle))
                .perform(typeText("Complete assignment"), closeSoftKeyboard());

        onView(withId(R.id.taskTitle))
                .check(matches(withText("Complete assignment")));
    }

    @Test
    public void testDescriptionInput() {
        onView(withId(R.id.taskDescription))
                .perform(typeText("Finish the project by Friday"), closeSoftKeyboard());

        onView(withId(R.id.taskDescription))
                .check(matches(withText("Finish the project by Friday")));
    }

    @Test
    public void testSaveButtonExists() {
        onView(withId(R.id.btnSaveTask))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDueDateFieldExists() {
        onView(withId(R.id.taskDueDate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testReminderDateFieldExists() {
        onView(withId(R.id.taskReminderDate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testReminderTimeFieldExists() {
        onView(withId(R.id.taskReminderTime))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSaveWithEmptyEmail() {
        // Fill other fields but leave email empty
        onView(withId(R.id.taskTitle))
                .perform(typeText("Task"), closeSoftKeyboard());

        onView(withId(R.id.taskDescription))
                .perform(typeText("Description"), closeSoftKeyboard());

        // Click save
        onView(withId(R.id.btnSaveTask))
                .perform(click());

        // Should stay on same screen
        onView(withId(R.id.btnSaveTask))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSaveWithEmptyTitle() {
        onView(withId(R.id.taskEmail))
                .perform(typeText("user@test.com"), closeSoftKeyboard());

        onView(withId(R.id.taskDescription))
                .perform(typeText("Description"), closeSoftKeyboard());

        onView(withId(R.id.btnSaveTask))
                .perform(click());

        onView(withId(R.id.btnSaveTask))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSaveWithAllFields() {
        onView(withId(R.id.taskEmail))
                .perform(typeText("patient@test.com"), closeSoftKeyboard());

        onView(withId(R.id.taskTitle))
                .perform(typeText("Morning Exercise"), closeSoftKeyboard());

        onView(withId(R.id.taskDescription))
                .perform(typeText("Do 30 min cardio"), closeSoftKeyboard());

        onView(withId(R.id.taskDueDate))
                .perform(typeText("2026-05-15"), closeSoftKeyboard());

        onView(withId(R.id.taskReminderDate))
                .perform(typeText("2026-05-14"), closeSoftKeyboard());

        onView(withId(R.id.taskReminderTime))
                .perform(typeText("08:00"), closeSoftKeyboard());

        onView(withId(R.id.btnSaveTask))
                .perform(click());

        // If backend works, activity finishes
    }

    @Test
    public void testDueDatePickerClick() {
        // Click due date field to trigger date picker lambda
        onView(withId(R.id.taskDueDate))
                .perform(click());

        // Date picker opens (we can't interact with it easily in Espresso)
        // But the lambda code has been executed

        // Verify field is still displayed
        onView(withId(R.id.taskDueDate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testReminderDatePickerClick() {
        onView(withId(R.id.taskReminderDate))
                .perform(click());

        onView(withId(R.id.taskReminderDate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testReminderTimePickerClick() {
        onView(withId(R.id.taskReminderTime))
                .perform(click());

        onView(withId(R.id.taskReminderTime))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSaveWithEmptyDescription() {
        onView(withId(R.id.taskEmail))
                .perform(typeText("user@test.com"), closeSoftKeyboard());

        onView(withId(R.id.taskTitle))
                .perform(typeText("Task Title"), closeSoftKeyboard());

        // Leave description empty

        onView(withId(R.id.btnSaveTask))
                .perform(click());

        onView(withId(R.id.btnSaveTask))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSaveWithEmptyDueDate() {
        onView(withId(R.id.taskEmail))
                .perform(typeText("user@test.com"), closeSoftKeyboard());

        onView(withId(R.id.taskTitle))
                .perform(typeText("Task"), closeSoftKeyboard());

        onView(withId(R.id.taskDescription))
                .perform(typeText("Description"), closeSoftKeyboard());

        // Leave dueDate empty

        onView(withId(R.id.btnSaveTask))
                .perform(click());

        onView(withId(R.id.btnSaveTask))
                .check(matches(isDisplayed()));
    }

}