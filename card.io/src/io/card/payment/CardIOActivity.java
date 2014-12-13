package io.card.payment;

/* CardIOActivity.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import io.card.payment.i18n.LocalizedStrings;
import io.card.payment.i18n.StringKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.paypal.android.sdk.common.ui.Appearance;
import com.paypal.android.sdk.common.ui.ViewUtil;

/**
 * This is the entry point activity for a Card.IO client to use the Card.IO service.
 * 
 * @version 1.0
 */
public final class CardIOActivity extends Activity {
     /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If set, the card will not be scanned
     * with the camera. Does not apply to charge requests.
     */
    public static final String EXTRA_NO_CAMERA = "io.card.payment.noCamera";

    /**
     * Boolean extra. Optional. Defaults to <code>true</code>. Ignored for charge requests. If
     * unset, expiry information will not be required.
     */
    public static final String EXTRA_REQUIRE_EXPIRY = "io.card.payment.requireExpiry";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If set, the user will be prompted
     * for the card CVV. For charge requests with manual entry, the user is always prompted.
     */
    public static final String EXTRA_REQUIRE_CVV = "io.card.payment.requireCVV";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If set, the user will be prompted
     * for the card billing postal code. For charge requests with manual entry, the user is always
     * prompted. 
     */
    public static final String EXTRA_REQUIRE_POSTAL_CODE = "io.card.payment.requirePostalCode";
    
    /**
     * Equivalent to EXTRA_REQUIRE_POSTAL_CODE
     */
    @Deprecated
    public static final String EXTRA_REQUIRE_ZIP = EXTRA_REQUIRE_POSTAL_CODE;

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. If set, the card.io logo will be
     * shown instead of the PayPal logo.
     */
    public static final String EXTRA_USE_CARDIO_LOGO = "io.card.payment.useCardIOLogo";

    /**
     * Parcelable extra containing {@link CreditCard}. The data intent returned to your activity's
     * onActivityResult() will contain this extra if the resultCode is RESULT_CARD_INFO.
     */
    public static final String EXTRA_SCAN_RESULT = "io.card.payment.scanResult";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. Removes the keyboard button from the
     * scan screen.
     * 
     * If scanning is unavailable, the activity result will be RESULT_SCAN_NOT_AVAILABLE.
     */
    public static final String EXTRA_SUPPRESS_MANUAL_ENTRY = "io.card.payment.suppressManual";

    /**
     * String extra. Optional. The preferred language for all strings appearing in the user
     * interface. If not set, or if set to null, defaults to the device's current language setting. <br/>
     * <br/>
     * Can be specified as a language code ("en", "fr", "zh-Hans", etc.) or as a locale ("en_AU",
     * "fr_FR", "zh-Hant_TW", etc.). <br/>
     * <br/>
     * If the library does not contain localized strings for a specified locale, then will fall back
     * to the language. E.g., "es_CO" -> "es". <br/>
     * If the library does not contain localized strings for a specified language, then will fall
     * back to American English. <br/>
     * <br/>
     * If you specify only a language code, and that code matches the device's currently preferred
     * language, then the library will attempt to use the device's current region as well. E.g.,
     * specifying "en" on a device set to "English" and "United Kingdom" will result in "en_GB". <br/>
     * <br/>
     * These localizations are currently included: <br/>
     * 
     * da, de, en, en_AU, en_GB, en_SE, es, es_MX, fr, he, is, it, ja, ko, nb, nl, pl, pt, pt_BR, ru,
     * sv, tr, zh-Hans, zh-Hant, zh-Hant_TW.
     */
    public static final String EXTRA_LANGUAGE_OR_LOCALE = "io.card.payment.languageOrLocale";

    /**
     * Integer extra. Optional. Defaults to Green. Changes the color of the guide overlay on the
     * camera.
     */
    public static final String EXTRA_GUIDE_COLOR = "io.card.payment.guideColor";

    /**
     * Boolean extra. Optional. If this value is set to true the user will not be prompted to
     * confirm their card number after processing.
     */
    public static final String EXTRA_SUPPRESS_CONFIRMATION = "io.card.payment.suppressConfirmation";

    /**
     * Boolean extra. Optional. Defaults to <code>false</code>. When set to true the Card IO logo
     * will not be shown overlayed on the camera.
     */
    public static final String EXTRA_HIDE_CARDIO_LOGO = "io.card.payment.hideLogo";

    /**
     * String extra. Optional. Used to display instructions to the user while they are scanning
     * their card.
     */
    public static final String EXTRA_SCAN_INSTRUCTIONS = "io.card.payment.scanInstructions";

    /**
     * Boolean extra. Optional. Once a card image has been captured but before it has been
     * processed, this value will determine whether to continue processing as usual. If the value is
     * true the CardIOActivity will finish with a RESULT_SCAN_SUPPRESSED result code.
     */
    public static final String EXTRA_SUPPRESS_SCAN = "io.card.payment.suppressScan";

    /**
     * String extra. If EXTRA_RETURN_CARD_IMAGE is set to true, the data intent passed to your
     * Activity will have the card image stored as a JPEG formatted byte array in this extra.
     */
    public static final String EXTRA_CAPTURED_CARD_IMAGE = "io.card.payment.capturedCardImage";

    /**
     * Boolean extra. Optional. If this value is set to true the card image will be passed as an
     * extra in the data intent that is returned to your Activity using the
     * EXTRA_CAPTURED_CARD_IMAGE key.
     */
    public static final String EXTRA_RETURN_CARD_IMAGE = "io.card.payment.returnCardImage";

    /**
     * Integer extra. Optional. If this value is provided the view will be inflated and will overlay
     * the camera during the scan process. The integer value must be the id of a valid layout
     * resource.
     */
    public static final String EXTRA_SCAN_OVERLAY_LAYOUT_ID = "io.card.payment.scanOverlayLayoutId";

    /** Boolean extra. Used for testing only. */
    static final String PRIVATE_EXTRA_CAMERA_BYPASS_TEST_MODE = "io.card.payment.cameraBypassTestMode";

    private static int lastResult = 0xca8d10; // arbitrary. chosen to be well above
                                              // Activity.RESULT_FIRST_USER.
    /**
     * result code supplied to onActivityResult() when a scan request completes.
     */
    public static final int RESULT_CARD_INFO = lastResult++;

    /**
     * result code supplied to onActivityResult() when the user presses the cancel button.
     */
    public static final int RESULT_ENTRY_CANCELED = lastResult++;

    /**
     * result code indicating that scan is not available. Only returned when
     * EXTRA_SUPPRESS_MANUAL_ENTRY is set and scanning is not available.
     * 
     * This error can be avoided in normal situations by checking
     * {@code CardIOActivity.canReadCardWithCamera()}.
     */
    public static final int RESULT_SCAN_NOT_AVAILABLE = lastResult++;

    /**
     * result code indicating that we only captured the card image.
     */
    public static final int RESULT_SCAN_SUPPRESSED = lastResult++;

    /**
     * result code indicating that confirmation was suppressed.
     */
    public static final int RESULT_CONFIRMATION_SUPPRESSED = lastResult++;

    private static final String TAG = "CardIOActivity";

    private static final int DEGREE_DELTA = 15;

    private static final int ORIENTATION_PORTRAIT = 1;
    private static final int ORIENTATION_PORTRAIT_UPSIDE_DOWN = 2;
    private static final int ORIENTATION_LANDSCAPE_RIGHT = 3;
    private static final int ORIENTATION_LANDSCAPE_LEFT = 4;

    private static final int FRAME_ID = 1;
    private static final int UIBAR_ID = 2;
    private static final int KEY_BTN_ID = 3;

    private static final float UIBAR_VERTICAL_MARGIN_DP = 15.0f;

    private static final long[] VIBRATE_PATTERN = { 0, 70, 10, 40 };

    private static final int TOAST_OFFSET_Y = -75;

    private static int uniqueOMatic = 10;
    private static final int REQUEST_DATA_ENTRY = uniqueOMatic++;

    private OverlayView mOverlay;
    private OrientationEventListener orientationListener;

    // TODO: the preview is accessed by the scanner. Not the best practice.
    Preview mPreview;

    private CreditCard mDetectedCard;
    private Rect mGuideFrame;
    private int mLastDegrees;
    private int mFrameOrientation;
    private boolean suppressManualEntry = false;
    private boolean mDetectOnly = false;
    private LinearLayout customOverlayLayout;

    private RelativeLayout mUIBar;
    private FrameLayout mMainLayout;

    static private int numActivityAllocations = 0;

    private CardScanner mCardScanner;

    private boolean manualEntryFallbackOrForced = false;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private static boolean scanAuthorized = true;

    /**
     * Static variable for the decorated card image. This is ugly, but works. Parceling and
     * unparceling card image data to pass to the next activity does not work because the image data
     * is too big and causes a somewhat misleading exception. Compressing the image data yields a
     * reduction to 30% of the original size, but still gives the same exception. An alternative
     * would be to persist the image data in a file. That seems like a pretty horrible idea, as we
     * would be persisting very sensitive data on the device.
     */
    static Bitmap markedCardImage = null;

    // ------------------------------------------------------------------------
    // ACTIVITY LIFECYCLE
    // ------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate() ================================================================");

        numActivityAllocations++;
        // NOTE: java native asserts are disabled by default on Android.
        if (numActivityAllocations != 1) {
            // it seems that this can happen in the autotest loop, but it doesn't seem to break.
            // was happening for lemon... (ugh, long story) but we're now protecting the underlying
            // DMZ/scanner from over-release.
            Log.i(TAG, String.format(
                    "INTERNAL WARNING: There are %d (not 1) CardIOActivity allocations!",
                    numActivityAllocations));
        }

        super.onCreate(savedInstanceState);

        final Intent clientData = this.getIntent();

        LocalizedStrings.setLanguage(clientData);

        // Validate app's manifest is correct.
        mDetectOnly = clientData.getBooleanExtra(EXTRA_SUPPRESS_SCAN, false);

        ResolveInfo resolveInfo;
        String errorMsg;

        // Check for DataEntryActivity's portrait orientation

        // Check for CardIOActivity's orientation config in manifest
        resolveInfo = getPackageManager().resolveActivity(clientData,
                PackageManager.MATCH_DEFAULT_ONLY);
        errorMsg = Util.manifestHasConfigChange(resolveInfo, CardIOActivity.class);
        if (errorMsg != null) {
            throw new RuntimeException(errorMsg); // Throw the actual exception from this class, for
                                                  // clarity.
        }

        suppressManualEntry = clientData.getBooleanExtra(EXTRA_SUPPRESS_MANUAL_ENTRY, false);

        if (clientData.getBooleanExtra(EXTRA_NO_CAMERA, false)) {
            Log.i(Util.PUBLIC_LOG_TAG, "EXTRA_NO_CAMERA set to true. Skipping camera.");
            manualEntryFallbackOrForced = true;
        } else {
            try {
                if (!Util.hardwareSupported()) {
                    StringKey errorKey = StringKey.ERROR_NO_DEVICE_SUPPORT;
                    String localizedError = LocalizedStrings.getString(errorKey);
                    Log.w(Util.PUBLIC_LOG_TAG, errorKey + ": " + localizedError);
                    manualEntryFallbackOrForced = true;
                }
            } catch (CameraUnavailableException e) {
                StringKey errorKey = StringKey.ERROR_CAMERA_CONNECT_FAIL;
                String localizedError = LocalizedStrings.getString(errorKey);

                Log.e(Util.PUBLIC_LOG_TAG, errorKey + ": " + localizedError);
                Toast toast = Toast.makeText(this, localizedError, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, TOAST_OFFSET_Y);
                toast.show();
                manualEntryFallbackOrForced = true;
            } catch (Exception e) {
                handleGeneralExceptionError(e);
            }
        }

        if (scanAuthorized && !manualEntryFallbackOrForced) {

            try {
                // Hide the window title.
                requestWindowFeature(Window.FEATURE_NO_TITLE);

                mGuideFrame = new Rect();

                mFrameOrientation = ORIENTATION_PORTRAIT;

                if (clientData.getBooleanExtra(PRIVATE_EXTRA_CAMERA_BYPASS_TEST_MODE, false)) {
                    if (!this.getPackageName().contentEquals("io.card.development")) {
                        Log.e(TAG, this.getPackageName() + " is not correct");
                        throw new IllegalStateException("illegal access of private extra");
                    }
                    // use reflection here so that the tester can be safely stripped for release
                    // builds.
                    Class<?> testScannerClass = Class.forName("io.card.payment.CardScannerTester");
                    Constructor<?> cons = testScannerClass.getConstructor(this.getClass(),
                            Integer.TYPE);
                    mCardScanner = (CardScanner) cons.newInstance(new Object[] { this,
                            mFrameOrientation });
                } else {
                    mCardScanner = new CardScanner(this, mFrameOrientation);
                }
                mCardScanner.prepareScanner();

                setPreviewLayout();

                orientationListener = new OrientationEventListener(this,
                        SensorManager.SENSOR_DELAY_UI) {
                    @Override
                    public void onOrientationChanged(int orientation) {
                        doOrientationChange(orientation);
                    }
                };

            } catch (Exception e) {
                handleGeneralExceptionError(e);
            }
        }

        if (manualEntryFallbackOrForced || !scanAuthorized) {
            if (suppressManualEntry) {
                Log.i(Util.PUBLIC_LOG_TAG, "Camera not available and manual entry suppressed.");
                setResultAndFinish(RESULT_SCAN_NOT_AVAILABLE, null);
            }
        }

    }

    private void handleGeneralExceptionError(Exception e) {
        StringKey errorKey = StringKey.ERROR_CAMERA_UNEXPECTED_FAIL;
        String localizedError = LocalizedStrings.getString(errorKey);

        Log.e(Util.PUBLIC_LOG_TAG,
                "Unkown exception - please send the stack trace to support@card.io", e);
        Toast toast = Toast.makeText(this, localizedError, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, TOAST_OFFSET_Y);
        toast.show();
        manualEntryFallbackOrForced = true;
    }

    private void doOrientationChange(int orientation) {
        // Log.d(TAG, "onOrientationChanged(" + orientation + ")");

        if (orientation < 0 || mCardScanner == null)
            return;

        orientation += getRotationalOffset();

        // Check if we have gone too far forward with
        // rotation adjustment, keep the result between 0-360
        if (orientation > 360) {
            orientation -= 360;
        }
        int degrees;

        degrees = -1;

        if (orientation < DEGREE_DELTA || orientation > 360 - DEGREE_DELTA) {
            degrees = 0;
            mFrameOrientation = ORIENTATION_PORTRAIT;
        } else if (orientation > 90 - DEGREE_DELTA && orientation < 90 + DEGREE_DELTA) {
            degrees = 90;
            mFrameOrientation = ORIENTATION_LANDSCAPE_LEFT;
        } else if (orientation > 180 - DEGREE_DELTA && orientation < 180 + DEGREE_DELTA) {
            degrees = 180;
            mFrameOrientation = ORIENTATION_PORTRAIT_UPSIDE_DOWN;
        } else if (orientation > 270 - DEGREE_DELTA && orientation < 270 + DEGREE_DELTA) {
            degrees = 270;
            mFrameOrientation = ORIENTATION_LANDSCAPE_RIGHT;
        }
        if (degrees >= 0 && degrees != mLastDegrees) {
            Log.d(TAG, "onOrientationChanged(" + degrees + ") calling setDeviceOrientation("
                    + mFrameOrientation + ")");
            mCardScanner.setDeviceOrientation(mFrameOrientation);
            setDeviceDegrees(degrees);
            if (degrees == 90) {
                rotateCustomOverlay(270);
            } else if (degrees == 270) {
                rotateCustomOverlay(90);
            } else {
                rotateCustomOverlay(degrees);
            }
        }
    }

    /**
     * @see <a
     *      href="http://stackoverflow.com/questions/12216148/android-screen-orientation-differs-between-devices">SO
     *      post</a>
     */
    private int getRotationalOffset() {
        final int rotationOffset;
        // Check "normal" screen orientation and adjust accordingly
        int naturalOrientation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getRotation();
        if (naturalOrientation == Surface.ROTATION_0) {
            rotationOffset = 0;
        } else if (naturalOrientation == Surface.ROTATION_90) {
            rotationOffset = 90;
        } else if (naturalOrientation == Surface.ROTATION_180) {
            rotationOffset = 180;
        } else if (naturalOrientation == Surface.ROTATION_270) {
            rotationOffset = 270;
        } else {
            // just hope for the best (shouldn't happen)
            rotationOffset = 0;
        }
        return rotationOffset;
    }

    /**
     * Suspend/resume camera preview as part of the activity life cycle (side note: we reuse the
     * same buffer for preview callbacks to greatly reduce the amount of required GC).
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() ----------------------------------------------------------");

        if (manualEntryFallbackOrForced) {
            nextActivity();
            return;
        }
        if (!scanAuthorized) {
            Log.e(Util.PUBLIC_LOG_TAG, "This app is not authorized to scan");
            setResultAndFinish(Activity.RESULT_CANCELED, null);
            return;
        }

        Util.logNativeMemoryStats();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        orientationListener.enable();

        if (!restartPreview()) {
            Log.e(TAG, "Could not connect to camera.");
            StringKey error = StringKey.ERROR_CAMERA_UNEXPECTED_FAIL;
            showErrorMessage(LocalizedStrings.getString(error));
            nextActivity();
        } else {
            // Turn flash off
            setFlashOn(false);
        }

        doOrientationChange(mLastDegrees);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause() xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

        super.onPause();

        if (orientationListener != null)
            orientationListener.disable();
        setFlashOn(false);

        if (mCardScanner != null) {
            mCardScanner.pauseScanning();
        } else if (!manualEntryFallbackOrForced) {
            Log.wtf(Util.PUBLIC_LOG_TAG, "cardScanner is null in onPause()");
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        mOverlay = null;
        numActivityAllocations--;

        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }

        if (mCardScanner != null) {
            mCardScanner.endScanning();
            mCardScanner = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, String.format("onActivityResult(requestCode:%d, resultCode:%d, ...",
                requestCode, resultCode));

        if (resultCode == RESULT_CARD_INFO || resultCode == RESULT_ENTRY_CANCELED
                || manualEntryFallbackOrForced) {
            if (data != null && data.hasExtra(EXTRA_SCAN_RESULT)) {
                Log.v(TAG, "data entry result: " + data.getParcelableExtra(EXTRA_SCAN_RESULT));
            }
            setResultAndFinish(resultCode, data);

        } else {
            if (mUIBar != null)
                mUIBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * This activity overrides back button handling to handle back presses properly given the
     * various states this activity can be in.
     * 
     * This method is called by Android, never directly by application code.
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, "CardIOActivity.onBackPressed()");

        if (!manualEntryFallbackOrForced && mOverlay.isAnimating()) {
            try {
                restartPreview();
            } catch (RuntimeException re) {
                Log.w(TAG, "*** could not return to preview: " + re);
            }
        } else if (mCardScanner != null) {
            super.onBackPressed();
        }
    }

    // ------------------------------------------------------------------------
    // STATIC METHODS
    // ------------------------------------------------------------------------

    /**
     * Determine if the device supports card scanning.
     * 
     * An ARM7 processor and Android SDK 8 or later are required. Additional checks for specific
     * misbehaving devices may also be added.
     * 
     * @return True if camera is supported. False otherwise.
     */
    public static boolean canReadCardWithCamera() {
        try {
            return scanAuthorized && Util.hardwareSupported();
        } catch (CameraUnavailableException e) {
            return false;
        } catch (RuntimeException e) {
            Log.w(TAG, "RuntimeException accessing Util.hardwareSupported()");
            return false;
        }
    }

    /**
     * @param context
     *            ignored
     * @return canReadCardWithCamera();
     */
    @Deprecated
    public static boolean canReadCardWithCamera(@SuppressWarnings("unused") Context context) {
        return canReadCardWithCamera();
    }

    /**
     * Please include the return value of this method in any support requests.
     * 
     * @return An string describing the version of the card.io library.
     */
    public static String sdkVersion() {
        return io.card.payment.Version.REVISION;
    }

    @SuppressWarnings("deprecation")
    public static Date sdkBuildDate() {
        return new Date(io.card.payment.Version.BUILD_DATE);
    }
    
    /**
     * Utility method for decoding card bitmap
     * 
     * @param intent
     *            - intent received in onActivityResult()
     * @return decoded bitmap or null
     */
    public static Bitmap getCapturedCardImage(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_CAPTURED_CARD_IMAGE))
            return null;

        byte[] imageData = intent.getByteArrayExtra(EXTRA_CAPTURED_CARD_IMAGE);
        ByteArrayInputStream inStream = new ByteArrayInputStream(imageData);
        Bitmap result = BitmapFactory.decodeStream(inStream, null, new BitmapFactory.Options());
        return result;
    }
    
    // end static

    void onFirstFrame(int orientation) {
        Log.d(TAG, "mFirstPreviewFrame");
        SurfaceView sv = mPreview.getSurfaceView();
        if (mOverlay != null)
            mOverlay.setCameraPreviewRect(new Rect(sv.getLeft(), sv.getTop(), sv.getRight(), sv
                    .getBottom()));
        mFrameOrientation = ORIENTATION_PORTRAIT;
        setDeviceDegrees(0);

        if (orientation != mFrameOrientation) {
            Log.wtf(Util.PUBLIC_LOG_TAG,
                    "the orientation of the scanner doesn't match the orientation of the activity");
        }
        onEdgeUpdate(new DetectionInfo());
    }

    void onEdgeUpdate(DetectionInfo dInfo) {
        mOverlay.setDetectionInfo(dInfo);
    }

    void onCardDetected(Bitmap detectedBitmap, DetectionInfo dInfo) {
        Log.d(TAG, "processDetections");

        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_PATTERN, -1);
        } catch (SecurityException e) {
            Log.e(Util.PUBLIC_LOG_TAG,
                    "Could not activate vibration feedback. Please add <uses-permission android:name=\"android.permission.VIBRATE\" /> to your application's manifest.");
        } catch (Exception e) {
            Log.w(Util.PUBLIC_LOG_TAG, "Exception while attempting to vibrate: ", e);
        }

        mCardScanner.pauseScanning();
        mUIBar.setVisibility(View.INVISIBLE);

        if (dInfo.predicted()) {
            mDetectedCard = dInfo.creditCard();
            mOverlay.setDetectedCard(mDetectedCard);
        }

        float sf;
        if (mFrameOrientation == ORIENTATION_PORTRAIT
                || mFrameOrientation == ORIENTATION_PORTRAIT_UPSIDE_DOWN) {
            sf = mGuideFrame.right / 428f * .95f;
        } else {
            sf = mGuideFrame.right / 428f * 1.15f;
        }

        Matrix m = new Matrix();
        Log.d(TAG, "Scale factor: " + sf);
        m.postScale(sf, sf);

        Bitmap scaledCard = Bitmap.createBitmap(detectedBitmap, 0, 0, detectedBitmap.getWidth(),
                detectedBitmap.getHeight(), m, false);
        mOverlay.setBitmap(scaledCard);

        if (mDetectOnly && scanAuthorized) {

            ByteArrayOutputStream scaledCardBytes = new ByteArrayOutputStream();
            scaledCard.compress(Bitmap.CompressFormat.JPEG, 80, scaledCardBytes);

            Intent dataIntent = new Intent();
            if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_RETURN_CARD_IMAGE, false)) {
                dataIntent.putExtra(EXTRA_CAPTURED_CARD_IMAGE, scaledCardBytes.toByteArray());
            }

            setResultAndFinish(RESULT_SCAN_SUPPRESSED, dataIntent);
        } else {
            nextActivity();
        }
    }

    private void nextActivity() {
        Log.d(TAG, "CardIOActivity.nextActivity()");

        if (!scanAuthorized)
            return;

        Intent origIntent = getIntent();
        if (origIntent != null && origIntent.getBooleanExtra(EXTRA_SUPPRESS_CONFIRMATION, false)) {
            Intent dataIntent = new Intent(CardIOActivity.this, DataEntryActivity.class);
            dataIntent.putExtra(EXTRA_SCAN_RESULT, mDetectedCard);
            mDetectedCard = null;

            if ( origIntent.getBooleanExtra(EXTRA_RETURN_CARD_IMAGE, false)
                    && mOverlay != null && mOverlay.getBitmap() != null) {
                ByteArrayOutputStream scaledCardBytes = new ByteArrayOutputStream();
                mOverlay.getBitmap().compress(Bitmap.CompressFormat.JPEG, 80, scaledCardBytes);
                dataIntent.putExtra(EXTRA_CAPTURED_CARD_IMAGE, scaledCardBytes.toByteArray());
            }

            setResultAndFinish(RESULT_CONFIRMATION_SUPPRESSED, dataIntent);
        } else {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "CardIOActivity.nextActivity().post(Runnable)");

                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

                    Intent intent = new Intent(CardIOActivity.this, DataEntryActivity.class);

                    if (mOverlay != null) {
                        mOverlay.markupCard();
                        if (markedCardImage != null && !markedCardImage.isRecycled()) {
                            markedCardImage.recycle();
                        }
                        markedCardImage = mOverlay.getCardImage();
                    }
                    if (mDetectedCard != null) {
                        intent.putExtra(EXTRA_SCAN_RESULT, mDetectedCard);
                        mDetectedCard = null;
                    }

                    intent.putExtras(getIntent()); // passing on any received params (such as isCvv
                                                   // and language)
                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivityForResult(intent, REQUEST_DATA_ENTRY);
                }
            });
        }
    }

    /**
     * Show an error message using toast.
     */
    private void showErrorMessage(final String msgStr) {
        Log.e(Util.PUBLIC_LOG_TAG, "error display: " + msgStr);
        Toast toast = Toast.makeText(CardIOActivity.this, msgStr, Toast.LENGTH_LONG);
        toast.show();
    }

    private boolean restartPreview() {
        Log.d(TAG, "restartPreview()");

        mDetectedCard = null;
        assert mPreview != null;
        boolean success = mCardScanner.resumeScanning(mPreview.getSurfaceHolder());
        if (success) {
            mUIBar.setVisibility(View.VISIBLE);
        }

        return success;
    }

    private void setDeviceDegrees(int degrees) {
        View sv;

        sv = mPreview.getSurfaceView();

        if (sv == null) {
            Log.wtf(Util.PUBLIC_LOG_TAG,
                    "surface view is null.. recovering... rotation might be weird.");
            return;
        }

        mGuideFrame = mCardScanner.getGuideFrame(sv.getWidth(), sv.getHeight());

        // adjust for surface view y offset
        mGuideFrame.top += sv.getTop();
        mGuideFrame.bottom += sv.getTop();
        mOverlay.setGuideAndRotation(mGuideFrame, degrees);
        mLastDegrees = degrees;
    }

    // Called by OverlayView
    void toggleFlash() {
        setFlashOn(!mCardScanner.isFlashOn());
    }

    void setFlashOn(boolean b) {
        boolean success = (mPreview != null && mOverlay != null && mCardScanner.setFlashOn(b));
        if (success) {
            mOverlay.setTorchOn(b);
        }
    }

    void triggerAutoFocus() {
        mCardScanner.triggerAutoFocus(true);
    }

    /**
     * Manually set up the layout for this activity. It may be possible to use the standard xml
     * layout mechanism instead, but to know for sure would require more work
     */
    private void setPreviewLayout() {

        // top level container
        mMainLayout = new FrameLayout(this);
        mMainLayout.setBackgroundColor(Color.BLACK);
        mMainLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        FrameLayout previewFrame = new FrameLayout(this);
        previewFrame.setId(FRAME_ID);

        mPreview = new Preview(this, null, mCardScanner.mPreviewWidth, mCardScanner.mPreviewHeight);
        mPreview.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT, Gravity.TOP));
        previewFrame.addView(mPreview);

        mOverlay = new OverlayView(this, null, Util.deviceSupportsTorch(this));
        mOverlay.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        if (getIntent() != null) {
            boolean useCardIOLogo = getIntent().getBooleanExtra(EXTRA_USE_CARDIO_LOGO, false);
            mOverlay.setUseCardIOLogo(useCardIOLogo);

            int color = getIntent().getIntExtra(EXTRA_GUIDE_COLOR, 0);

            if (color != 0) {
                // force 100% opaque guide colors.
                int alphaRemovedColor = color | 0xFF000000;
                if (color != alphaRemovedColor) {
                    Log.w(Util.PUBLIC_LOG_TAG, "Removing transparency from provided guide color.");
                }
                mOverlay.setGuideColor(alphaRemovedColor);
            } else {
                // default to greeeeeen
                mOverlay.setGuideColor(Color.GREEN);
            }

            boolean hideCardIOLogo = getIntent().getBooleanExtra(EXTRA_HIDE_CARDIO_LOGO, false);
            mOverlay.setHideCardIOLogo(hideCardIOLogo);

            String scanInstructions = getIntent().getStringExtra(EXTRA_SCAN_INSTRUCTIONS);
            if (scanInstructions != null) {
                mOverlay.setScanInstructions(scanInstructions);
            }
            
        }

        previewFrame.addView(mOverlay);

        RelativeLayout.LayoutParams previewParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        previewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        previewParams.addRule(RelativeLayout.ABOVE, UIBAR_ID);
        mMainLayout.addView(previewFrame, previewParams);

        mUIBar = new RelativeLayout(this);
        mUIBar.setGravity(Gravity.BOTTOM);
        RelativeLayout.LayoutParams mUIBarParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        previewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mUIBar.setLayoutParams(mUIBarParams);

        mUIBar.setId(UIBAR_ID);

        mUIBar.setGravity(Gravity.BOTTOM | Gravity.RIGHT);

        // Show the keyboard button
        if (!suppressManualEntry) {
            Button keyboardBtn = new Button(this);
            keyboardBtn.setId(KEY_BTN_ID);
            keyboardBtn.setText(LocalizedStrings.getString(StringKey.KEYBOARD));
            keyboardBtn.setTextSize(12.0f);
            keyboardBtn.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nextActivity();
                }
            });
            mUIBar.addView(keyboardBtn);
            ViewUtil.styleAsButton(keyboardBtn, false, this);
            keyboardBtn.setTextSize(Appearance.TEXT_SIZE_SMALL_BUTTON);
            keyboardBtn.setMinimumHeight(ViewUtil.typedDimensionValueToPixelsInt(
                    Appearance.SMALL_BUTTON_HEIGHT, this));
            RelativeLayout.LayoutParams keyboardParams = (RelativeLayout.LayoutParams) keyboardBtn
                    .getLayoutParams();
            keyboardParams.width = LayoutParams.WRAP_CONTENT;
            keyboardParams.height = LayoutParams.WRAP_CONTENT;
            keyboardParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            ViewUtil.setPadding(keyboardBtn, Appearance.CONTAINER_MARGIN_HORIZONTAL, null,
                    Appearance.CONTAINER_MARGIN_HORIZONTAL, null);
            ViewUtil.setMargins(keyboardBtn, Appearance.BASE_SPACING, Appearance.BASE_SPACING,
                    Appearance.BASE_SPACING, Appearance.BASE_SPACING);

        }
        // Device has a flash, show the flash button
        RelativeLayout.LayoutParams uiParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        uiParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        final float scale = getResources().getDisplayMetrics().density;
        int uiBarMarginPx = (int) (UIBAR_VERTICAL_MARGIN_DP * scale + 0.5f);
        uiParams.setMargins(0, uiBarMarginPx, 0, uiBarMarginPx);
        mMainLayout.addView(mUIBar, uiParams);

        if ( getIntent() != null) {
            if (customOverlayLayout != null) {
                mMainLayout.removeView(customOverlayLayout);
                customOverlayLayout = null;
            }

            int resourceId = getIntent().getIntExtra(EXTRA_SCAN_OVERLAY_LAYOUT_ID, -1);
            if (resourceId != -1) {
                customOverlayLayout = new LinearLayout(this);
                customOverlayLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT));

                LayoutInflater inflater = this.getLayoutInflater();

                inflater.inflate(resourceId, customOverlayLayout);
                mMainLayout.addView(customOverlayLayout);
            }
        }

        this.setContentView(mMainLayout);
    }

    private void rotateCustomOverlay(float degrees) {
        if (customOverlayLayout != null) {
            float pivotX = customOverlayLayout.getWidth() / 2;
            float pivotY = customOverlayLayout.getHeight() / 2;

            Animation an = new RotateAnimation(0, degrees, pivotX, pivotY);
            an.setDuration(0);
            an.setRepeatCount(0);
            an.setFillAfter(true);

            customOverlayLayout.setAnimation(an);
        }
    }

    private void setResultAndFinish(final int resultCode, final Intent data) {
        setResult(resultCode, data);
        markedCardImage = null;
        finish();
    }

    // for torch test
    public Rect getTorchRect() {
        if (mOverlay == null) {
            return null;
        }
        return mOverlay.getTorchRect();
    }

}
