package io.card.payment;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.view.ViewGroup.LayoutParams;

import java.lang.reflect.Constructor;
import java.util.Date;

import io.card.payment.i18n.LocalizedStrings;
import io.card.payment.i18n.StringKey;
import io.card.payment.ui.ActivityHelper;
import io.card.payment.interfaces.*;

/**
 *
 * Significant changes have been made in order to create CardIO as a Fragment. Primarely in {@link CardIOActivity}, {@link CardScanner}, {@link OverlayView} and {@link Preview}
 * {@link CardIOFragment#takePictureOfCard()} has been created to facilitate in case the scan fails for any reason. Implementing {@link CardScanListener#onPictureTaken(byte[])} in the FragmentActivity/Activity should do the trick
 * Essentially, all dependencies were turned into references to Interfaces (As sorted by n0m0r3pa1n at Github) so it became more flexible. Thus, it also makes it possible to handle callbacks in the FragmentActivity/Activity, whichever handles the CardIOFragment
 * As a side note, bear in mind that some features are not working or for some reason it wasn't found out why by the time. For example, {@link CardScanner#setScanExpiry(boolean)} among other data from scanned CreditCards
 *
 * It might still need some cleanup
 *
 * How to create a fragment(cardIOViewHolder being a RelativeLayout which will hold Card.io SurfaceView):
 *
 *    //arguments setup for Card.io
 *    Bundle args = new Bundle();
 *    args.putBoolean(CardIOConstants.PORTRAIT_ORIENTATION_LOCK, true);
 *    args.putInt(CardIOConstants.CARD_IO_VIEW, cardIOViewHolder.getId());
 *    args.putInt(CardIOConstants.CARD_IO_OVERLAY_COLOUR, null);
 *    args.putString(CardIOConstants.EXTRA_SCAN_INSTRUCTIONS, "");
 *    args.putBoolean(CardIOConstants.CARD_EXPIRY, true); //not working
 *
 *
 *    //adding Card.io as fragment
 *    cardFragment = new CardIOFragment();
 *    cardFragment.setArguments(args);
 *
 *    FragmentManager fragManager = getFragmentManager();
 *    FragmentTransaction fragTransaction = fragManager.beginTransaction();
 *    fragTransaction.add(containerPreview.getId(), cardFragment);
 *    fragTransaction.commit();
 *
 * Created by glaubermartins on 2018-03-21.
 *
 */

public class CardIOFragment extends Fragment implements CardScanRecognition, CardIOCameraControl, Camera.PictureCallback {

    private static final String TAG = CardIOActivity.class.getSimpleName();

    private OverlayView mOverlay;
    private OrientationEventListener orientationListener;

    // TODO: the preview is accessed by the scanner. Not the best practice.
    private Preview mPreview;

    private CreditCard mDetectedCard;
    private Rect mGuideFrame;
    private int mLastDegrees;
    private int mFrameOrientation;
    private boolean suppressManualEntry;
    private boolean mDetectOnly;
    private boolean waitingForPermission;

    private boolean useApplicationTheme;

    private CardScanner mCardScanner;

    private boolean manualEntryFallbackOrForced = false;

    /**
     * Static variable for the decorated card image. This is ugly, but works. Parceling and
     * unparceling card image data to pass to the next {@link android.app.Activity} does not work because the image
     * data
     * is too big and causes a somewhat misleading exception. Compressing the image data yields a
     * reduction to 30% of the original size, but still gives the same exception. An alternative
     * would be to persist the image data in a file. That seems like a pretty horrible idea, as we
     * would be persisting very sensitive data on the device.
     */
    static Bitmap markedCardImage = null;

    private RelativeLayout previewFrame;
    private Intent clientData;
    private boolean isPortraitOrientationLocked = false, includeExpiry = false, hideCardIOLogo = true, useCardIOLogo = false;
    private CardScanListener cardScanListener;
    private int cardIOViewHolder, overlayGuideColour;
    private String scanInstructions;

    public CardIOFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            cardIOViewHolder = bundle.getInt(CardIOConstants.CARD_IO_VIEW);
            isPortraitOrientationLocked = bundle.getBoolean(CardIOConstants.PORTRAIT_ORIENTATION_LOCK);
            includeExpiry = bundle.getBoolean(CardIOConstants.CARD_EXPIRY);
            overlayGuideColour = bundle.getInt(CardIOConstants.CARD_IO_OVERLAY_COLOUR);
            hideCardIOLogo = bundle.getBoolean(CardIOConstants.EXTRA_HIDE_CARDIO_LOGO);
            scanInstructions = bundle.getString(CardIOConstants.EXTRA_SCAN_INSTRUCTIONS);
            useCardIOLogo = bundle.getBoolean(CardIOConstants.EXTRA_USE_CARDIO_LOGO);
        }

        clientData = getActivity().getIntent();

        useApplicationTheme = getActivity().getIntent().getBooleanExtra(CardIOConstants.EXTRA_KEEP_APPLICATION_THEME, false);
        ActivityHelper.setActivityTheme(getActivity(), useApplicationTheme);

        LocalizedStrings.setLanguage(clientData);

        // Validate app's manifest is correct.
        mDetectOnly = clientData.getBooleanExtra(CardIOConstants.EXTRA_SUPPRESS_SCAN, false);

        ResolveInfo resolveInfo;
        String errorMsg;

        // Check for DataEntryActivity's portrait orientation

        // Check for CardIOActivity's orientation config in manifest
        resolveInfo = getActivity().getPackageManager().resolveActivity(clientData,
                PackageManager.MATCH_DEFAULT_ONLY);
        errorMsg = Util.manifestHasConfigChange(resolveInfo, getActivity().getClass());
        if (errorMsg != null) {
            throw new RuntimeException(errorMsg); // Throw the actual exception from this class, for
            // clarity.
        }

        suppressManualEntry = clientData.getBooleanExtra(CardIOConstants.EXTRA_SUPPRESS_MANUAL_ENTRY, false);


        if (savedInstanceState != null) {
            waitingForPermission = savedInstanceState.getBoolean(CardIOConstants.BUNDLE_WAITING_FOR_PERMISSION);
        }

        if (clientData.getBooleanExtra(CardIOConstants.EXTRA_NO_CAMERA, false)) {
            manualEntryFallbackOrForced = true;
        } else if (!CardScanner.processorSupported()){
            manualEntryFallbackOrForced = true;
        } else {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (!waitingForPermission) {
                        if (getActivity().checkSelfPermission(Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_DENIED) {
                            String[] permissions = {Manifest.permission.CAMERA};
                            waitingForPermission = true;
                            requestPermissions(permissions, CardIOConstants.PERMISSION_REQUEST_ID);
                        } else {
                            checkCamera();
                            android23AndAboveHandleCamera();
                        }
                    }
                } else {
                    checkCamera();
                    android22AndBelowHandleCamera();
                }
            } catch (Exception e) {
                handleGeneralExceptionError(e);
            }
        }

    }

    /**
     * Suspend/resume camera preview as part of the {@link android.app.Activity} life cycle (side note: we reuse the
     * same buffer for preview callbacks to greatly reduce the amount of required GC).
     */
    @Override
    public void onResume() {
        super.onResume();

        if (!waitingForPermission) {
            if (manualEntryFallbackOrForced) {
                if (suppressManualEntry) {
                    finishIfSuppressManualEntry();
                    return;
                } else {
                    return;
                }
            }

            Util.logNativeMemoryStats();

            ActivityHelper.setFlagSecure(getActivity());

            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (!isPortraitOrientationLocked)
                orientationListener.enable();

            if (!restartPreview()) {
                StringKey error = StringKey.ERROR_CAMERA_UNEXPECTED_FAIL;
                showErrorMessage(LocalizedStrings.getString(error));
                cardScanListener.onCardScanFail();
            }

            if (!isPortraitOrientationLocked)
                doOrientationChange(mLastDegrees);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(CardIOConstants.BUNDLE_WAITING_FOR_PERMISSION, waitingForPermission);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CardScanListener)
            cardScanListener = (CardScanListener) context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof CardScanListener)
            cardScanListener = (CardScanListener) activity;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (orientationListener != null) {
            orientationListener.disable();
        }

        if (mCardScanner != null) {
            mCardScanner.pauseScanning();
        }
    }

    @Override
    public void onDestroy() {
        mOverlay = null;

        if (orientationListener != null) {
            orientationListener.disable();
        }

        if (mCardScanner != null) {
            mCardScanner.endScanning();
            mCardScanner = null;
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        if (requestCode == CardIOConstants.PERMISSION_REQUEST_ID) {
            waitingForPermission = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCameraScannerOverlay();
            } else {
                // show manual entry - handled in onResume()
                manualEntryFallbackOrForced = true;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CardIOConstants.DATA_ENTRY_REQUEST_ID) {
            if (resultCode == CardIOConstants.RESULT_CARD_INFO || resultCode == CardIOConstants.RESULT_ENTRY_CANCELED
                    || manualEntryFallbackOrForced) {
                setResultAndFinish(resultCode, data);
            }
        }
    }

    private void android23AndAboveHandleCamera() {
        if (manualEntryFallbackOrForced) {
            finishIfSuppressManualEntry();
        } else {
            // Guaranteed to be called in API 23+
            showCameraScannerOverlay();
        }
    }


    private void android22AndBelowHandleCamera() {
        if (manualEntryFallbackOrForced) {
            finishIfSuppressManualEntry();
        } else {
            // guaranteed to be called in onCreate on API < 22, so it's ok that we're removing the window feature here
            showCameraScannerOverlay();
        }
    }

    private void finishIfSuppressManualEntry() {
        if (suppressManualEntry) {
            setResultAndFinish(CardIOConstants.RESULT_SCAN_NOT_AVAILABLE, null);
        }
    }

    private void checkCamera() {
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
            Toast toast = Toast.makeText(getActivity(), localizedError, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, CardIOConstants.TOAST_OFFSET_Y);
            toast.show();
            manualEntryFallbackOrForced = true;
        }
    }

    private void showCameraScannerOverlay() {
        try {
            mGuideFrame = new Rect();

            mFrameOrientation = CardIOConstants.ORIENTATION_PORTRAIT;

            if (getActivity().getIntent().getBooleanExtra(CardIOConstants.PRIVATE_EXTRA_CAMERA_BYPASS_TEST_MODE, false)) {
                if (!getActivity().getPackageName().contentEquals("io.card.development")) {
                    throw new IllegalStateException("Illegal access of private extra");
                }
                // use reflection here so that the tester can be safely stripped for release
                // builds.
                Class<?> testScannerClass = Class.forName("io.card.payment.CardScannerTester");
                Constructor<?> cons = testScannerClass.getConstructor(this.getClass(),
                        Integer.TYPE);
                mCardScanner = (CardScanner) cons.newInstance(this,
                        mFrameOrientation);
            } else {
                mCardScanner = new CardScanner(this, mFrameOrientation, false);
                mCardScanner.setScanExpiry(includeExpiry);
            }
            mCardScanner.prepareScanner();

            setPreviewLayout();

            if (!isPortraitOrientationLocked){
                orientationListener = new OrientationEventListener(getActivity(),
                        SensorManager.SENSOR_DELAY_UI) {
                    @Override
                    public void onOrientationChanged(int orientation) {
                        doOrientationChange(orientation);
                    }
                };
            }

        } catch (Exception e) {
            handleGeneralExceptionError(e);
        }
    }

    private void handleGeneralExceptionError(Exception e) {
        StringKey errorKey = StringKey.ERROR_CAMERA_UNEXPECTED_FAIL;
        String localizedError = LocalizedStrings.getString(errorKey);

        Log.e(Util.PUBLIC_LOG_TAG, "Unknown exception, please post the stack trace as a GitHub issue", e);
        Toast toast = Toast.makeText(getActivity(), localizedError, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, CardIOConstants.TOAST_OFFSET_Y);
        toast.show();
        manualEntryFallbackOrForced = true;
    }

    private void doOrientationChange(int orientation) {
        if (orientation < 0 || mCardScanner == null)
            return;

        orientation += mCardScanner.getRotationalOffset();

        // Check if we have gone too far forward with
        // rotation adjustment, keep the result between 0-360
        if (orientation > 360) {
            orientation -= 360;
        }
        int degrees;

        degrees = -1;

        if (orientation < CardIOConstants.DEGREE_DELTA || orientation > 360 - CardIOConstants.DEGREE_DELTA) {
            degrees = 0;
            mFrameOrientation = CardIOConstants.ORIENTATION_PORTRAIT;
        } else if (orientation > 90 - CardIOConstants.DEGREE_DELTA && orientation < 90 + CardIOConstants.DEGREE_DELTA) {
            degrees = 90;
            mFrameOrientation = CardIOConstants.ORIENTATION_LANDSCAPE_LEFT;
        } else if (orientation > 180 - CardIOConstants.DEGREE_DELTA && orientation < 180 + CardIOConstants.DEGREE_DELTA) {
            degrees = 180;
            mFrameOrientation = CardIOConstants.ORIENTATION_PORTRAIT_UPSIDE_DOWN;
        } else if (orientation > 270 - CardIOConstants.DEGREE_DELTA && orientation < 270 + CardIOConstants.DEGREE_DELTA) {
            degrees = 270;
            mFrameOrientation = CardIOConstants.ORIENTATION_LANDSCAPE_RIGHT;
        }
        if (degrees >= 0 && degrees != mLastDegrees) {
            mCardScanner.setDeviceOrientation(mFrameOrientation);
            setDeviceDegrees(degrees);
        }
    }

    /**
     * This {@link android.app.Activity} overrides back button handling to handle back presses properly given the
     * various states this {@link android.app.Activity} can be in.
     * <br><br>
     * This method is called by Android, never directly by application code.
     */
    @Override
    public void onBackPressed() {
        if (!manualEntryFallbackOrForced && mOverlay.isAnimating()) {
            try {
                restartPreview();
            } catch (RuntimeException re) {
                Log.w(TAG, "*** could not return to preview: " + re);
            }
        } else if (mCardScanner != null) {
            getActivity().onBackPressed();
        }
    }

    // ------------------------------------------------------------------------
    // STATIC METHODS
    // ------------------------------------------------------------------------

    /**
     * Determine if the device supports card scanning.
     * <br><br>
     * An ARM7 processor and Android SDK 8 or later are required. Additional checks for specific
     * misbehaving devices may also be added.
     *
     * @return <code>true</code> if camera is supported. <code>false</code> otherwise.
     */
    public static boolean canReadCardWithCamera() {
        try {
            return Util.hardwareSupported();
        } catch (CameraUnavailableException e) {
            return false;
        } catch (RuntimeException e) {
            Log.w(TAG, "RuntimeException accessing Util.hardwareSupported()");
            return false;
        }
    }

    /**
     * Returns the String version of this SDK.  Please include the return value of this method in any support requests.
     *
     * @return The String version of this SDK
     */
    public static String sdkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * @deprecated Always returns {@code new Date()}.
     */
    @Deprecated
    public static Date sdkBuildDate() {
        return new Date();
    }

    // end static

    public void takePictureOfCard() {
        mCardScanner.takePicture(this);
    }

    /**
     * Show an error message using toast.
     */
    private void showErrorMessage(final String msgStr) {
        Log.e(Util.PUBLIC_LOG_TAG, "error display: " + msgStr);
        Toast toast = Toast.makeText(getActivity(), msgStr, Toast.LENGTH_LONG);
        toast.show();
    }

    private boolean restartPreview() {
        mDetectedCard = null;
        assert mPreview != null;
        boolean success = mCardScanner.resumeScanning(mPreview.getSurfaceHolder());

        return success;
    }

    private void setDeviceDegrees(int degrees) {
        View sv;

        sv = mPreview.getSurfaceView();

        if (sv == null) {
            return;
        }

        mGuideFrame = mCardScanner.getGuideFrame(sv.getWidth(), sv.getHeight());

        // adjust for surface view y offset
        mGuideFrame.top += sv.getTop();
        mGuideFrame.bottom += sv.getTop();
        mOverlay.setGuideAndRotation(mGuideFrame, degrees);
        mLastDegrees = degrees;

    }

    /**
     * Fragment setup for camera preview as well as CardScanner. CardIOViewHolder being the container for the CardIO camera as a fragment passed over from FragmentActivity/Activity.
     * */
    private void setPreviewLayout() {

        //get top level container from parent FragmentActivity (not necessarily as it could be just an Activity)
        previewFrame = (RelativeLayout) getActivity().findViewById(cardIOViewHolder);

        //setup SurfaceView
        mPreview = new Preview(getActivity(), null, mCardScanner.mPreviewWidth, mCardScanner.mPreviewHeight);
        mPreview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        //adds to the parent view
        previewFrame.addView(mPreview);

        //setup scancard overlay view
        mOverlay = new OverlayView(getActivity(), this,null, Util.deviceSupportsTorch(getActivity()));
        mOverlay.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mOverlay.setUseCardIOLogo(useCardIOLogo);

        //int color = getActivity().getIntent().getIntExtra(CardIOConstants.EXTRA_GUIDE_COLOR, 0);

        if (overlayGuideColour != 0) {
            // force 100% opaque guide colors.
            int alphaRemovedColor = overlayGuideColour | 0xFF000000;
            mOverlay.setGuideColor(alphaRemovedColor);
        } else {
            // default to greeeeen << geez, that guys is loud
            mOverlay.setGuideColor(Color.GREEN);
        }

        mOverlay.setHideCardIOLogo(hideCardIOLogo);

        if (scanInstructions != null) {
            mOverlay.setScanInstructions(scanInstructions);
        }

        previewFrame.addView(mOverlay);

    }

    private void setResultAndFinish(final int resultCode, final Intent data) {
        getActivity().setResult(resultCode, data);
        markedCardImage = null;
        getActivity().finish();
    }

    @Override
    public void onFirstFrame(int orientation) {
        SurfaceView sv = mPreview.getSurfaceView();
        if (mOverlay != null) {
            mOverlay.setCameraPreviewRect(new Rect(sv.getLeft(), sv.getTop(), sv.getRight(), sv.getBottom()));
        }
        mFrameOrientation = CardIOConstants.ORIENTATION_PORTRAIT;
        setDeviceDegrees(0);

        onEdgeUpdate(new DetectionInfo());
    }

    @Override
    public void onEdgeUpdate(DetectionInfo info) {
        mOverlay.setDetectionInfo(info);
    }

    @Override
    public void onCardDetected(Bitmap bitmap, DetectionInfo info) {
        try {
            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(CardIOConstants.VIBRATE_PATTERN, -1);
        } catch (SecurityException e) {
            Log.e(Util.PUBLIC_LOG_TAG,
                    "Could not activate vibration feedback. Please add <uses-permission android:name=\"android.permission.VIBRATE\" /> to your application's manifest.");
        } catch (Exception e) {
            Log.w(Util.PUBLIC_LOG_TAG, "Exception while attempting to vibrate: ", e);
        }

        mCardScanner.pauseScanning();

        if (info.predicted()) {
            mDetectedCard = info.creditCard();
            mOverlay.setDetectedCard(mDetectedCard);
        }

        float sf;
        if (mFrameOrientation == CardIOConstants.ORIENTATION_PORTRAIT
                || mFrameOrientation == CardIOConstants.ORIENTATION_PORTRAIT_UPSIDE_DOWN) {
            sf = mGuideFrame.right / (float)CardScanner.CREDIT_CARD_TARGET_WIDTH * .95f;
        } else {
            sf = mGuideFrame.right / (float)CardScanner.CREDIT_CARD_TARGET_WIDTH * 1.15f;
        }

        Matrix m = new Matrix();
        m.postScale(sf, sf);

        Bitmap scaledCard = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), m, false);
        mOverlay.setBitmap(scaledCard);

        if (mDetectOnly) {
            Intent dataIntent = new Intent();
            Util.writeCapturedCardImageIfNecessary(getActivity().getIntent(), dataIntent, mOverlay);

            setResultAndFinish(CardIOConstants.RESULT_SCAN_SUPPRESSED, dataIntent);
        } else {
            if (mDetectedCard != null)
                cardScanListener.onCardScanSuccess(mDetectedCard, scaledCard);
            else
                cardScanListener.onCardScanFail();
            mDetectedCard = null;
        }
    }

    @Override
    public void triggerAutoFocus() {
        mCardScanner.triggerAutoFocus(true);
    }

    @Override
    public void toggleFlash() {
        //not being used for fragment
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        try {
            if (data != null)
                cardScanListener.onPictureTaken(data);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
