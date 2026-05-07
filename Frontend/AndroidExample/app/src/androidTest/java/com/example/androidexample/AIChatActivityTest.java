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
public class AIChatActivityTest {

    @Rule
    public ActivityScenarioRule<AIChatActivity> activityRule =
            new ActivityScenarioRule<>(AIChatActivity.class);

    @Test
    public void testChatContainerExists() {
        onView(withId(R.id.chatContainer))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testScrollViewExists() {
        onView(withId(R.id.scrollView))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testMessageInputExists() {
        onView(withId(R.id.etMessage))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSendButtonExists() {
        onView(withId(R.id.btnSend))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonExists() {
        onView(withId(R.id.btnBack))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testClearHistoryButtonExists() {
        onView(withId(R.id.btnClearHistory))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testBackButtonClick() {
        onView(withId(R.id.btnBack))
                .perform(click());

        // Activity should finish
    }

    @Test
    public void testMessageInput() {
        onView(withId(R.id.etMessage))
                .perform(typeText("Hello AI"), closeSoftKeyboard());
    }

    @Test
    public void testSendMessage() {
        onView(withId(R.id.etMessage))
                .perform(typeText("How are you?"), closeSoftKeyboard());

        onView(withId(R.id.btnSend))
                .perform(click());

        // Message sent to backend, typing indicator appears
    }

    @Test
    public void testSendEmptyMessage() {
        // Don't type anything
        onView(withId(R.id.btnSend))
                .perform(click());

        // Should do nothing (no message sent)
        onView(withId(R.id.btnSend))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testActivityLoads() {
        // Activity loads and fetches history
        onView(withId(R.id.chatContainer))
                .check(matches(isDisplayed()));
    }
}