// CounselorEditProfileActivityTest.java
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
public class CounselorEditProfileActivityTest {

    @Rule
    public ActivityScenarioRule<CounselorEditProfileActivity> activityRule =
            new ActivityScenarioRule<>(CounselorEditProfileActivity.class);

    @Test
    public void testDisplayNameInput() {
        onView(withId(R.id.etDisplayName))
                .perform(typeText("Dr. Bob"), closeSoftKeyboard());
    }

    @Test
    public void testEmailInput() {
        onView(withId(R.id.etEmail))
                .perform(typeText("bob@test.com"), closeSoftKeyboard());
    }

    @Test
    public void testPasswordInput() {
        onView(withId(R.id.etPassword))
                .perform(typeText("password123"), closeSoftKeyboard());
    }

    @Test
    public void testSpecializationInput() {
        onView(withId(R.id.etSpecialization))
                .perform(typeText("Anxiety"), closeSoftKeyboard());
    }

    @Test
    public void testBioInput() {
        onView(withId(R.id.etBio))
                .perform(typeText("Experienced therapist"), closeSoftKeyboard());
    }

    @Test
    public void testStatusSpinner() {
        onView(withId(R.id.spStatus))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testBackButton() {
        onView(withId(R.id.btnBack))
                .perform(click());
    }

//    @Test
//    public void testSaveWithEmptyDisplayName() {
//        onView(withId(R.id.btnSave))
//                .perform(click());
//
//        onView(withId(R.id.btnSave))
//                .check(matches(isDisplayed()));
//    }

    @Test
    public void testSaveWithAllFields() {
        onView(withId(R.id.etDisplayName))
                .perform(typeText("Dr. Bob"), closeSoftKeyboard());

        onView(withId(R.id.etSpecialization))
                .perform(typeText("Anxiety"), closeSoftKeyboard());

        onView(withId(R.id.btnSave))
                .perform(click());
    }

//    @Test
//    public void testDeleteButton() {
//        onView(withId(R.id.btnDeleteAccount))
//                .check(matches(isDisplayed()));
//    }
}