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
public class ChatActivityTest {

    @Test
    public void testChatWithPartner() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ChatActivity.class);
        intent.putExtra("partnerUserId", 123L);
        intent.putExtra("partnerName", "Dr. Smith");

        ActivityScenario.launch(intent);

        onView(withId(R.id.recyclerMessages)).check(matches(isDisplayed()));
    }

    @Test
    public void testMessageInput() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ChatActivity.class);
        intent.putExtra("partnerUserId", 123L);
        intent.putExtra("partnerName", "Dr. Smith");

        ActivityScenario.launch(intent);

        onView(withId(R.id.etMessage)).perform(typeText("Hello"), closeSoftKeyboard());
    }

    @Test
    public void testSendButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ChatActivity.class);
        intent.putExtra("partnerUserId", 123L);
        intent.putExtra("partnerName", "Dr. Smith");

        ActivityScenario.launch(intent);

        onView(withId(R.id.etMessage)).perform(typeText("Test message"), closeSoftKeyboard());
        onView(withId(R.id.btnSend)).perform(click());
    }

    @Test
    public void testBackButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ChatActivity.class);
        intent.putExtra("partnerUserId", 123L);
        intent.putExtra("partnerName", "Dr. Smith");

        ActivityScenario.launch(intent);

        onView(withId(R.id.btnBack)).perform(click());
    }

    @Test
    public void testAttachButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ChatActivity.class);
        intent.putExtra("partnerUserId", 123L);
        intent.putExtra("partnerName", "Dr. Smith");

        ActivityScenario.launch(intent);

        onView(withId(R.id.btnAttach)).check(matches(isDisplayed()));
    }
}