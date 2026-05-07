package com.example.androidexample;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
public class AssignPrescriptionActivityTest {

    @Test
    public void testActivityWithIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AssignPrescriptionActivity.class);
        intent.putExtra("TARGET_USER_ID", 123L);
        intent.putExtra("TARGET_USER_NAME", "John Doe");

        ActivityScenario.launch(intent);

        onView(withId(R.id.etMedName))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testMedicationNameInput() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AssignPrescriptionActivity.class);
        intent.putExtra("TARGET_USER_ID", 123L);

        ActivityScenario.launch(intent);

        onView(withId(R.id.etMedName))
                .perform(typeText("Aspirin"), closeSoftKeyboard());
    }

    @Test
    public void testDosageInput() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AssignPrescriptionActivity.class);
        intent.putExtra("TARGET_USER_ID", 123L);

        ActivityScenario.launch(intent);

        onView(withId(R.id.etDosage))
                .perform(typeText("500mg"), closeSoftKeyboard());
    }

    @Test
    public void testBackButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AssignPrescriptionActivity.class);
        intent.putExtra("TARGET_USER_ID", 123L);

        ActivityScenario.launch(intent);

        onView(withId(R.id.btnBack))
                .perform(click());
    }

    @Test
    public void testSubmitWithEmptyFields() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AssignPrescriptionActivity.class);
        intent.putExtra("TARGET_USER_ID", 123L);

        ActivityScenario.launch(intent);

        onView(withId(R.id.btnSubmit))
                .perform(click());

        // Should show validation error
        onView(withId(R.id.btnSubmit))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSubmitWithAllFields() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AssignPrescriptionActivity.class);
        intent.putExtra("TARGET_USER_ID", 123L);

        ActivityScenario.launch(intent);

        onView(withId(R.id.etMedName))
                .perform(typeText("Aspirin"), closeSoftKeyboard());

        onView(withId(R.id.etDosage))
                .perform(typeText("500mg"), closeSoftKeyboard());

        onView(withId(R.id.etStartDate))
                .perform(typeText("2026-05-10"), closeSoftKeyboard());

        onView(withId(R.id.etDurationDays))
                .perform(typeText("30"), closeSoftKeyboard());

        onView(withId(R.id.btnSubmit))
                .perform(click());
    }
}