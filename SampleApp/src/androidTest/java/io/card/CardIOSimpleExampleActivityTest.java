package io.card;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;

import com.lukekorth.deviceautomator.DeviceAutomator;

import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.card.development.CardIOSimpleExampleActivity;
import io.card.development.R;
import io.card.payment.i18n.LocalizedStrings;
import io.card.payment.i18n.StringKey;

public class CardIOSimpleExampleActivityTest {

    @Rule
    public final ActivityTestRule<CardIOSimpleExampleActivity> mActivityTestRule =
            new ActivityTestRule<>(CardIOSimpleExampleActivity.class);

    @SuppressWarnings("MissingPermission")
    @Before
    public void setup() {
        mActivityTestRule.getActivity();
        KeyguardManager mKeyGuardManager = (KeyguardManager) InstrumentationRegistry.getTargetContext()
                .getSystemService(Context.KEYGUARD_SERVICE);
        mKeyGuardManager.newKeyguardLock("CardIOSimpleExampleActivityTest")
                .disableKeyguard();
    }

    @Test
    public void cancelInManualEntryExistsActivity() {
        Espresso.onView(ViewMatchers.withText("Force keyboard entry (bypass scan)")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("Scan Credit Card using Card.io")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("Card Number")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withText(LocalizedStrings.getString(StringKey.CANCEL))).perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withText("Force keyboard entry (bypass scan)")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    @Test
    public void manualEntryReturnsCardData() {
        Espresso.onView(ViewMatchers.withText("Expiry")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("CVV")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("Postal Code")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("Cardholder Name")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("Force keyboard entry (bypass scan)")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("Scan Credit Card using Card.io")).perform(ViewActions.click());

        fillInCardForm();
        Espresso.onView(ViewMatchers.withText(LocalizedStrings.getString(StringKey.DONE))).perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.resultText)).check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString("1111"))));
        Espresso.onView(ViewMatchers.withId(R.id.resultText)).check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString("Expiry: 12/2022"))));
        Espresso.onView(ViewMatchers.withId(R.id.resultText)).check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString("CVV: 123"))));
        Espresso.onView(ViewMatchers.withId(R.id.resultText)).check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString("Postal Code: 95131"))));
        Espresso.onView(ViewMatchers.withId(R.id.resultText)).check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString("Cardholder Name: John Doe"))));
    }

    @Test
    public void canEnterManualEntryFromScanActivity() {
        Espresso.onView(ViewMatchers.withText("Expiry")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("CVV")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("Postal Code")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("Cardholder Name")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withText("Scan Credit Card using Card.io")).perform(ViewActions.click());

        DeviceAutomator.onDevice().acceptRuntimePermission(Manifest.permission.CAMERA);

        Espresso.onView(ViewMatchers.withText(LocalizedStrings.getString(StringKey.KEYBOARD))).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        Espresso.onView(ViewMatchers.withText(LocalizedStrings.getString(StringKey.KEYBOARD))).perform(ViewActions.click());

        fillInCardForm();
        Espresso.onView(ViewMatchers.withText(LocalizedStrings.getString(StringKey.DONE))).perform(ViewActions.click());
    }

    @Test
    public void recordingPlayback() {
        Espresso.onView(ViewMatchers.withText("Expiry")).perform(ViewActions.click());
        Espresso.onView(ViewMatchers.withId(R.id.recordingSpinner)).perform(ViewActions.click());
        Espresso.onData(AllOf.allOf(Is.is(Matchers.instanceOf(String.class)), Is.is("recording_320455133.550273.zip"))).perform(ViewActions.click());

        SystemClock.sleep(5000);
        Espresso.onView(ViewMatchers.withId(100)).perform(ViewActions.click(), ViewActions.typeText("1222"));
        Espresso.onView(ViewMatchers.withText(LocalizedStrings.getString(StringKey.DONE))).perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.resultText)).check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString("Expiry: 12/2022"))));
    }

    private void fillInCardForm() {
        Espresso.onView(ViewMatchers.withId(100)).perform(ViewActions.click(), ViewActions.typeText("4111111111111111"));
        Espresso.onView(ViewMatchers.withId(101)).perform(ViewActions.click(), ViewActions.typeText("1222"));
        Espresso.onView(ViewMatchers.withId(102)).perform(ViewActions.click(), ViewActions.typeText("123"));
        Espresso.onView(ViewMatchers.withId(103)).perform(ViewActions.click(), ViewActions.typeText("95131"));
        Espresso.onView(ViewMatchers.withId(104)).perform(ViewActions.click(), ViewActions.typeText("John Doe"));
    }
}
