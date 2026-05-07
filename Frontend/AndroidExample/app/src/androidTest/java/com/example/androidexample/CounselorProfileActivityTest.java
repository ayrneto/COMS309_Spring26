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
public class CounselorProfileActivityTest {

    @Rule
    public ActivityScenarioRule<CounselorProfileActivity> activityRule =
            new ActivityScenarioRule<>(CounselorProfileActivity.class);

    @Test
    public void testEmailDisplayed() {
        onView(withId(R.id.tvEmail)).check(matches(isDisplayed()));
    }

    @Test
    public void testDisplayNameDisplayed() {
        onView(withId(R.id.tvDisplayName)).check(matches(isDisplayed()));
    }

    @Test
    public void testSpecializationDisplayed() {
        onView(withId(R.id.tvSpecialization)).check(matches(isDisplayed()));
    }

    @Test
    public void testBioDisplayed() {
        onView(withId(R.id.tvBio)).check(matches(isDisplayed()));
    }

    @Test
    public void testStatusDisplayed() {
        onView(withId(R.id.tvStatus)).check(matches(isDisplayed()));
    }

    @Test
    public void testRatingDisplayed() {
        onView(withId(R.id.tvRating)).check(matches(isDisplayed()));
    }

    @Test
    public void testBackButton() {
        onView(withId(R.id.btn_back_home)).perform(click());
    }

    @Test
    public void testEditProfileButton() {
        onView(withId(R.id.btnEditProfile)).perform(click());
    }

    @Test
    public void testActivityLoads() {
        onView(withId(R.id.tvEmail)).check(matches(isDisplayed()));
    }
}