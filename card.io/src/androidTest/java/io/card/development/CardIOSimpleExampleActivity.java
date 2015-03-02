package io.card.development;

/* CardIOSimpleExampleActivity.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.card.development.recording.Recording;
import io.card.payment.CardIOActivity;
import io.card.payment.CardScannerTester;
import io.card.payment.CardType;
import io.card.payment.CreditCard;
import io.card.payment.i18n.StringKey;
import io.card.payment.i18n.SupportedLocale;
import io.card.payment.i18n.locales.LocalizedStringsList;

public class CardIOSimpleExampleActivity extends Activity {
    protected static final String TAG = CardIOSimpleExampleActivity.class.getSimpleName();

    private static final String RECORDING_DIR = Environment.getExternalStorageDirectory().getPath()
            + "/card_recordings";

    private static int unique = 10; // bigger than known Android statuses
    static final int REQUEST_SCAN = unique++;
    static final int REQUEST_AUTOTEST = unique++;

    // UI elements
    private CheckBox mManualToggle;
    private CheckBox mEnableExpiryToggle;
    private CheckBox mScanExpiryToggle;
    private CheckBox mCvvToggle;
    private CheckBox mPostalCodeToggle;
    private CheckBox mSuppressManualToggle;
    private CheckBox mSuppressConfirmationToggle;
    private CheckBox mSuppressScanToggle;
    private int guideColor = Color.GREEN;

    private Spinner mRecordingListSpinner;

    private TextView mResultLabel;
    private ImageView mResultImage;
    private ImageView mResultCardTypeImage;

    private boolean autotestMode;
    private int numAutotestsPassed;
    private CheckBox mUseCardIOLogoToggle;
    private CheckBox mShowPayPalActionBarIconToggle;
    private CheckBox mKeepApplicationThemeToggle;
    private Spinner mLanguageSpinner;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        Log.v(TAG, "onCreate()");

        // debugging. Requires android-9, but preferably android-11 or 14
        // StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        // .detectAll()
        // .penaltyLog()
        // .build());
        // StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
        // .detectAll()
        // .penaltyLog()
        // .penaltyDeath()
        // .build());
        // --------------

        setContentView(R.layout.demo);

        mManualToggle = (CheckBox) findViewById(R.id.forceManual);
        mEnableExpiryToggle = (CheckBox) findViewById(R.id.gatherExpiry);
        mScanExpiryToggle = (CheckBox) findViewById(R.id.scanExpiry);
        mCvvToggle = (CheckBox) findViewById(R.id.gatherCvv);
        mPostalCodeToggle = (CheckBox) findViewById(R.id.gatherPostalCode);
        mSuppressManualToggle = (CheckBox) findViewById(R.id.suppressManual);
        mSuppressConfirmationToggle = (CheckBox) findViewById(R.id.suppressConfirmation);
        mSuppressScanToggle = (CheckBox) findViewById(R.id.detectOnly);

        mUseCardIOLogoToggle = (CheckBox) findViewById(R.id.useCardIOLogo);
        mShowPayPalActionBarIconToggle = (CheckBox) findViewById(R.id.showPayPalActionBarIcon);
        mKeepApplicationThemeToggle = (CheckBox) findViewById(R.id.keepApplicationTheme);

        mLanguageSpinner = (Spinner) findViewById(R.id.languageSpinner);

        List<String> languages = new ArrayList<String>();
        // add null string for device level testing
        languages.add(null);
        for (SupportedLocale<StringKey> locale : LocalizedStringsList.ALL_LOCALES) {
            languages.add(locale.getName());
        }

        GenericStringListAdapter sa = new GenericStringListAdapter(this,
                R.layout.generic_list_item, R.id.text, languages);
        mLanguageSpinner.setAdapter(sa);
        mLanguageSpinner.setSelection(sa.getIndexForName("en"));

        mResultLabel = (TextView) findViewById(R.id.resultText);
        mResultLabel.setText("card.io library:\n" + CardIOActivity.sdkVersion() + "\n"
                + CardIOActivity.sdkBuildDate());

        mResultImage = (ImageView) findViewById(R.id.resultImage);
        mResultCardTypeImage = (ImageView) findViewById(R.id.resultCardTypeImage);

        mEnableExpiryToggle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setScanExpiryEnabled();
            }
        });
        setScanExpiryEnabled();
    }

    private void setScanExpiryEnabled(){
        mScanExpiryToggle.setEnabled(mEnableExpiryToggle.isChecked());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");

        KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardLock mLock = mKeyGuardManager.newKeyguardLock(getClass().getName());
        mLock.disableKeyguard();

        mRecordingListSpinner = (Spinner) findViewById(R.id.recordingSpinner);
        File dir = new File(RECORDING_DIR);
        File[] allFiles = dir.listFiles();
        final ArrayList<CharSequence> recordingNames = new ArrayList<CharSequence>(
                ((allFiles != null) ? allFiles.length : 0) + 1);
        recordingNames.add("Select a recording");
        if (allFiles != null && allFiles.length > 0) {

            Log.d(TAG, "Recordings in " + dir.getPath());
            for (File f : allFiles) {
                String name = f.getName();
                if (f.isFile() && name.startsWith("recording_") && name.endsWith(".zip")) {
                    Log.d(TAG, "\t" + name);
                    recordingNames.add(name);
                }
            }
        }
        if (recordingNames.size() > 1) {
            ArrayAdapter<CharSequence> aa = new ArrayAdapter<CharSequence>(this,
                    android.R.layout.simple_dropdown_item_1line, recordingNames);
            mRecordingListSpinner.setAdapter(aa);
            mRecordingListSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View content, int pos, long id) {
                    if (pos <= 0) {
                        return;
                    }

                    String filename = String.valueOf(recordingNames.get(pos));
                    Log.i(TAG, "chose recording: " + filename);
                    String path = RECORDING_DIR + "/" + String.valueOf(filename);
                    Recording rec = new Recording(new File(path));
                    CardScannerTester.setRecording(rec);

                    Intent intent = new Intent(CardIOSimpleExampleActivity.this,
                            CardIOActivity.class);
                    intent.putExtra("io.card.payment.cameraBypassTestMode", true);
                    intent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, mEnableExpiryToggle.isChecked());
                    intent.putExtra(CardIOActivity.EXTRA_SCAN_EXPIRY, mScanExpiryToggle.isChecked());
                    intent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, mCvvToggle.isChecked());
                    intent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE,
                            mPostalCodeToggle.isChecked());

                    CardIOSimpleExampleActivity.this.startActivityForResult(intent, REQUEST_SCAN);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // nothing should happen.
                }

            });
        } else {
            mRecordingListSpinner.setVisibility(View.GONE);
        }
    }

    public void onScanPressed(@SuppressWarnings("unused") View pressed) {
        if (!mManualToggle.isChecked()) {
            if (CardIOActivity.canReadCardWithCamera()) {
                Log.i(TAG, "Device supports camera reads.");
            } else {
                Log.w(TAG,
                        "Camera read not available on this device. Will fallback to manual entry.");
            }
        }
        Intent intent = new Intent(CardIOSimpleExampleActivity.this, CardIOActivity.class);

        intent.putExtra(CardIOActivity.EXTRA_NO_CAMERA, mManualToggle.isChecked());
        intent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, mEnableExpiryToggle.isChecked());
        intent.putExtra(CardIOActivity.EXTRA_SCAN_EXPIRY, mScanExpiryToggle.isChecked());
        intent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, mCvvToggle.isChecked());
        intent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, mPostalCodeToggle.isChecked());
        intent.putExtra(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY,
                mSuppressManualToggle.isChecked());
        intent.putExtra(CardIOActivity.EXTRA_USE_CARDIO_LOGO, mUseCardIOLogoToggle.isChecked());

        Object selectedLanguageOrLocale = mLanguageSpinner.getSelectedItem();

        intent.putExtra(CardIOActivity.EXTRA_LANGUAGE_OR_LOCALE,
                null == selectedLanguageOrLocale ? null : selectedLanguageOrLocale.toString());
        intent.putExtra(CardIOActivity.EXTRA_USE_PAYPAL_ACTIONBAR_ICON,
                mShowPayPalActionBarIconToggle.isChecked());
        intent.putExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME,
                mKeepApplicationThemeToggle.isChecked());
        intent.putExtra(CardIOActivity.EXTRA_GUIDE_COLOR, guideColor);
        intent.putExtra(CardIOActivity.EXTRA_SUPPRESS_CONFIRMATION,
                mSuppressConfirmationToggle.isChecked());

        intent.putExtra(CardIOActivity.EXTRA_SUPPRESS_SCAN, mSuppressScanToggle.isChecked());

        intent.putExtra(CardIOActivity.EXTRA_RETURN_CARD_IMAGE, true);

        CardIOSimpleExampleActivity.this.startActivityForResult(intent, REQUEST_SCAN);
    }


    public void onAutotestPressed(@SuppressWarnings("unused") View pressed) {
        autotest();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
    }

    private void autotest() {
        Log.i(TAG, "\n\n\n ============================== \n" + "successfully completed "
                + numAutotestsPassed + " tests\n" + "beginning new test run\n");

        Intent intent = new Intent(CardIOSimpleExampleActivity.this, CardIOActivity.class);

        intent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false);
        intent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false);
        intent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false);
        intent.putExtra("debug_autoAcceptResult", true);

        CardIOSimpleExampleActivity.this.startActivityForResult(intent, REQUEST_AUTOTEST);

        autotestMode = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        mResultLabel.setText("");
        Log.v(TAG, "onStop()");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v(TAG, "onActivityResult(" + requestCode + ", " + resultCode + ", " + data + ")");

        String outStr = new String();
        Bitmap cardTypeImage = null;

        if ((requestCode == REQUEST_SCAN || requestCode == REQUEST_AUTOTEST) && data != null
                && data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
            CreditCard result = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
            if (result != null) {
                outStr += "Card number: " + result.getRedactedCardNumber() + "\n";

                CardType cardType = result.getCardType();
                cardTypeImage = cardType.imageBitmap(this);
                outStr += "Card type: " + cardType.name() + " cardType.getDisplayName(null)="
                        + cardType.getDisplayName(null) + "\n";

                if (mEnableExpiryToggle.isChecked()) {
                    outStr += "Expiry: " + result.expiryMonth + "/" + result.expiryYear + "\n";
                }

                if (mCvvToggle.isChecked()) {
                    outStr += "CVV: " + result.cvv + "\n";
                }

                if (mPostalCodeToggle.isChecked()) {
                    outStr += "Postal Code: " + result.postalCode + "\n";
                }
            }

            if (autotestMode) {
                numAutotestsPassed++;
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        autotest();
                    }
                }, 500);

            }

        } else if (resultCode == Activity.RESULT_CANCELED) {
            autotestMode = false;
        }

        Bitmap card = CardIOActivity.getCapturedCardImage(data);
        mResultImage.setImageBitmap(card);
        mResultCardTypeImage.setImageBitmap(cardTypeImage);

        Log.i(TAG, "Set result: " + outStr);
        mResultLabel.setText(outStr);

    }
}
