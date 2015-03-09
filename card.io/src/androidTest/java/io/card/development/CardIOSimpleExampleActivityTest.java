package io.card.development;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;

import com.robotium.solo.Solo;

import java.util.ArrayList;
import java.util.Iterator;

import io.card.payment.CardIOActivity;
import io.card.payment.CardType;
import io.card.payment.DataEntryActivity;
import io.card.payment.i18n.LocalizedStrings;
import io.card.payment.i18n.StringKey;

/**
 * This is a simple framework for a test of an Application. See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on how to write
 * and extend Application tests.
 * <p/>
 * To run this test, you can type: adb shell am instrument -w \ -e class
 * io.card.development.CardIOSimpleExampleActivityTest \
 * io.card.development.tests/android.test.InstrumentationTestRunner
 */
public class CardIOSimpleExampleActivityTest extends
        ActivityInstrumentationTestCase2<CardIOSimpleExampleActivity> {
    private static final String TAG = "TESTER";
    public static final int EXPIRY_CHECKBOX_INDEX = 0;
    public static final int CVV_CHECKBOX_INDEX = 1;
    public static final int POSTAL_CODE_CHECKBOX_INDEX = 2;
    public static final int FORCE_MANUAL_CHECKBOX_INDEX = 4;

    private Solo solo;

    public CardIOSimpleExampleActivityTest() {
        super(CardIOSimpleExampleActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        // setUp() is run before a test case is started.
        // This is where the solo object is created.

        Activity myActivity = getActivity();

        KeyguardManager mKeyGuardManager = (KeyguardManager) myActivity
                .getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardLock mLock = mKeyGuardManager.newKeyguardLock(myActivity.getClass().getName());
        mLock.disableKeyguard();

        solo = new Solo(getInstrumentation(), myActivity);

    }

    @Override
    public void tearDown() throws Exception {
        // tearDown() is run after a test case has finished.
        // finishOpenedActivities() will finish all the activities that have been opened during the
        // test execution.
        solo.finishOpenedActivities();
        super.tearDown();
    }

    private void selectEntryItems(boolean shouldForceManual) throws Exception {
        solo.assertCurrentActivity("Expected Buffalo Activity", CardIOSimpleExampleActivity.class);
        solo.scrollToTop();
        solo.clickOnCheckBox(EXPIRY_CHECKBOX_INDEX);
        solo.clickOnCheckBox(CVV_CHECKBOX_INDEX);
        solo.clickOnCheckBox(POSTAL_CODE_CHECKBOX_INDEX);

        if (shouldForceManual) {
            solo.clickOnCheckBox(FORCE_MANUAL_CHECKBOX_INDEX);
        }
    }

    private void completeDataEntry() throws Exception {
        solo.sleep(250);

        solo.assertCurrentActivity("Expected DataEntryActivity", DataEntryActivity.class);

        int i = 0;
        solo.enterText(i++, "4111111111111111");
        solo.enterText(i++, "12/22");
        solo.enterText(i++, "123");
        solo.enterText(i++, "95131");

        solo.takeScreenshot("entry");

        String btnText = LocalizedStrings.getString(StringKey.DONE);
        solo.clickOnButton(btnText);

        solo.sleep(250);

        solo.assertCurrentActivity("Expected completion", CardIOSimpleExampleActivity.class);
        solo.takeScreenshot("completion");

        assertEquals("Expiry not found", true, solo.searchText("Expiry: 12/2022"));
        assertEquals("CVV not found", true, solo.searchText("CVV: 123"));
        assertEquals("Postal Code not found", true, solo.searchText("Postal Code: 95131"));
    }

    public void test015_Cancel() throws Exception {

        solo.assertCurrentActivity("expecting example app", CardIOSimpleExampleActivity.class);
        solo.clickOnCheckBox(FORCE_MANUAL_CHECKBOX_INDEX);
        solo.sleep(250);
        solo.clickOnButton("Scan Credit Card using Card.io");

        solo.sleep(500);

        solo.assertCurrentActivity("Expected DataEntryActivity", DataEntryActivity.class);

        String btnText = LocalizedStrings.getString(StringKey.CANCEL);
        solo.clickOnButton(btnText);

        solo.assertCurrentActivity("expecting example app", CardIOSimpleExampleActivity.class);
    }

    public void test02_DataEntry() throws Exception {
        selectEntryItems(true);
        solo.clickOnButton("Scan Credit Card using Card.io");

        completeDataEntry();
    }

    public void test03_ScanWithDataEntry() throws Exception {
        Log.i(TAG, "begin testEntryFlow() +++++++++");

        selectEntryItems(false);

        boolean ableToScan = CardIOActivity.canReadCardWithCamera();

        solo.clickOnButton("Scan Credit Card using Card.io");

        if (ableToScan) {
            Log.v(TAG, "testing scanner ...");

            solo.assertCurrentActivity("Expected CardIOActivity (scan)", CardIOActivity.class);

            solo.sleep(500);

            solo.takeScreenshot("scan");

            String btnText = LocalizedStrings.getString(StringKey.KEYBOARD);
            solo.clickOnButton(btnText);
        } else {
            Log.i(TAG, "device can't use camera, skipping scan test");
        }

        completeDataEntry();


        Log.i(TAG, "end testEntryFlow() ----------");
    }

    /**
     * solo.getCurrentSpinners() went away in Robotium 4.0. This is the code that went missing.
     * <p>
     * Extracted from <a href=
     * "http://grepcode.com/file/repo1.maven.org/maven2/com.jayway.android.robotium/robotium-solo/1.3.1/com/jayway/android/robotium/solo/ViewFetcher.java#ViewFetcher.getCurrentSpinners%28%29"
     * >some random website</a>.
     * </p>
     *
     * @param solo
     * @return
     */
    private ArrayList<Spinner> getCurrentSpinners(Solo solo) {
        ArrayList<Spinner> spinnerList = new ArrayList<Spinner>();
        ArrayList<View> viewList = solo.getViews();
        Iterator<View> iterator = viewList.iterator();
        while (iterator.hasNext()) {
            View view = iterator.next();
            if (view.getClass().getName().equals("android.widget.Spinner")) {
                spinnerList.add((Spinner) view);
            }
        }
        return spinnerList;
    }

    public void test04_RecordingPlayback() throws Exception {
        if (Build.VERSION.SDK_INT <= 10) {
            Log.i(TAG, "playback tests aren't reliable on Gingerbread and earlier, so skipping");
            return;
        }

        if (!CardIOActivity.canReadCardWithCamera()) {
            Log.i(TAG, "device can't use camera, skipping playback test");
            return;
        }

        Log.i(TAG, "begin testRecordingPlayback() +++++++++");

        solo.assertCurrentActivity("Expected Buffalo to launch",
                CardIOSimpleExampleActivity.class);

        solo.clickOnCheckBox(EXPIRY_CHECKBOX_INDEX);

        Spinner spinner = getCurrentSpinners(solo).get(1);

        int numItems = spinner.getCount();
        for (int i = 1; i < numItems; i++) {
            Log.v(TAG, "testing recording " + i);
            solo.pressSpinnerItem(1, i); // move up 1 on the only spinner.
            solo.assertCurrentActivity("Expected CardIOActivity (scan)", CardIOActivity.class);

            solo.waitForLogMessage("ready for manual entry");

            solo.sleep(1000); // extra settle time.

            solo.enterText(0, "12/22");

            solo.assertCurrentActivity("didn't finish scan", DataEntryActivity.class);

            solo.takeScreenshot("captured_" + i);

            String btnText = LocalizedStrings.getString(StringKey.DONE);
            solo.clickOnButton(btnText);

            assertEquals("Scan result not found", true, solo.searchText("Card number:"));
        }

        Log.i(TAG, "end testRecordingPlayback() ----------");
    }

    /**
     * This test merely asserts that certain public methods are present and accessible to those who
     * use this library.
     *
     * @throws Exception
     */
    public void test05_LibraryMethods() throws Exception {
        assertEquals(CardType.VISA, CardType.fromCardNumber("4111111111111111"));
        assertEquals(CardType.VISA, CardType.fromString("visa"));
        assertEquals(CardType.UNKNOWN, CardType.fromCardNumber("999999"));
    }

    public void test06_ScanWithFlash() throws Exception {
        Log.i(TAG, "begin testFlash() +++++++++");

        selectEntryItems(false);

        boolean ableToScan = CardIOActivity.canReadCardWithCamera();

        solo.clickOnButton("Scan Credit Card using Card.io");

        if (ableToScan) {
            Log.v(TAG, "testing scanner ...");

            solo.assertCurrentActivity("Expected CardIOActivity (scan)", CardIOActivity.class);
            solo.sleep(1000);
            CardIOActivity cardIOActivity = (CardIOActivity) (solo.getCurrentActivity());
            Rect torchRect = cardIOActivity.getTorchRect();

            if (torchRect != null) {
                solo.clickOnScreen(torchRect.exactCenterX(), torchRect.exactCenterY());
                solo.sleep(500);
                solo.clickOnScreen(torchRect.exactCenterX(), torchRect.exactCenterY());
            }

            solo.sleep(500);

            solo.takeScreenshot("scan");

            String btnText = LocalizedStrings.getString(StringKey.KEYBOARD);
            solo.clickOnButton(btnText);
        } else {
            Log.i(TAG, "device can't use camera, skipping scan test");
        }

        completeDataEntry();

        Log.i(TAG, "end testFlash() ----------");
    }
}
