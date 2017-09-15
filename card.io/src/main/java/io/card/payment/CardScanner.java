package io.card.payment;

/* CardScanner.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the core image scanning.
 * <p/>
 * As of 7/20/12, the flow should be:
 * <p/>
 * 1. CardIOActivity sets up the CardScanner, Preview and Overlay. 2. As each frame is received &
 * processed by the scanner, the scanner notifies the activity of any relevant changes. (e.g. edges
 * detected, scan complete etc.) 3. CardIOActivity passes on the information to the preview and
 * overlay, which can then update themselves as needed. 4. Once a result is reported, CardIOActivty
 * closes the scanner and launches the next activity.
 * <p/>
 * HOWEVER, at the moment, the CardScanner is directly communicating with the Preview.
 */
class CardScanner implements Camera.PreviewCallback, Camera.AutoFocusCallback,
        SurfaceHolder.Callback {
    private static final String TAG = CardScanner.class.getSimpleName();

    private static final float MIN_FOCUS_SCORE = 6; // TODO - parameterize this
    // value based on phone? or
    // change focus behavior?

    private static final int DEFAULT_UNBLUR_DIGITS = -1; // no blur per default

    private static final int CAMERA_CONNECT_TIMEOUT = 5000;
    private static final int CAMERA_CONNECT_RETRY_INTERVAL = 50;

    static final int ORIENTATION_PORTRAIT = 1;

    // these values MUST match those in dmz_constants.h
    static final int CREDIT_CARD_TARGET_WIDTH = 428; // kCreditCardTargetWidth
    static final int CREDIT_CARD_TARGET_HEIGHT = 270; // kCreditCardTargetHeight

    // NATIVE
    public static native boolean nUseNeon();

    public static native boolean nUseTegra();

    public static native boolean nUseX86();

    private native void nSetup(boolean shouldDetectOnly, float minFocusScore);

    private native void nSetup(boolean shouldDetectOnly, float minFocusScore, int unBlur);

    private native void nResetAnalytics();

    private native void nGetGuideFrame(int orientation, int previewWidth, int previewHeight, Rect r);

    private native void nScanFrame(byte[] data, int frameWidth, int frameHeight, int orientation,
                                   DetectionInfo dinfo, Bitmap resultBitmap, boolean scanExpiry);

    private native int nGetNumFramesScanned();

    private native void nCleanup();

    private Bitmap detectedBitmap;

    private static boolean manualFallbackForError;

    // member data
    protected WeakReference<CardIOActivity> mScanActivityRef;
    private boolean mSuppressScan = false;
    private boolean mScanExpiry;
    private int mUnblurDigits = DEFAULT_UNBLUR_DIGITS;

    // read by CardIOActivity to set up Preview
    final int mPreviewWidth = 640;
    final int mPreviewHeight = 480;

    private int mFrameOrientation = ORIENTATION_PORTRAIT;

    private boolean mFirstPreviewFrame = true;
    private long captureStart;
    private long mAutoFocusStartedAt;
    private long mAutoFocusCompletedAt;

    private Camera mCamera;
    private byte[] mPreviewBuffer;

    // accessed by test harness subclass.
    protected boolean useCamera = true;

    private boolean isSurfaceValid;

    private int numManualRefocus;
    private int numAutoRefocus;
    private int numManualTorchChange;
    private int numFramesSkipped;

    // ------------------------------------------------------------------------
    // STATIC INITIALIZATION
    // ------------------------------------------------------------------------

    static {
        Log.i(Util.PUBLIC_LOG_TAG, "card.io " + BuildConfig.VERSION_NAME);

        try {
            loadLibrary("cardioDecider");
            Log.d(Util.PUBLIC_LOG_TAG, "Loaded card.io decider library.");
            Log.d(Util.PUBLIC_LOG_TAG, "    nUseNeon(): " + nUseNeon());
            Log.d(Util.PUBLIC_LOG_TAG, "    nUseTegra():" + nUseTegra());
            Log.d(Util.PUBLIC_LOG_TAG, "    nUseX86():  " + nUseX86());

            if (usesSupportedProcessorArch()) {
                loadLibrary("opencv_core");
                Log.d(Util.PUBLIC_LOG_TAG, "Loaded opencv core library");
                loadLibrary("opencv_imgproc");
                Log.d(Util.PUBLIC_LOG_TAG, "Loaded opencv imgproc library");
            }
            if (nUseNeon()) {
                loadLibrary("cardioRecognizer");
                Log.i(Util.PUBLIC_LOG_TAG, "Loaded card.io NEON library");
            } else if (nUseX86()) {
                loadLibrary("cardioRecognizer");
                Log.i(Util.PUBLIC_LOG_TAG, "Loaded card.io x86 library");
            } else if (nUseTegra()) {
                loadLibrary("cardioRecognizer_tegra2");
                Log.i(Util.PUBLIC_LOG_TAG, "Loaded card.io Tegra2 library");
            } else {
                Log.w(Util.PUBLIC_LOG_TAG,
                        "unsupported processor - card.io scanning requires ARMv7 or x86 architecture");
                manualFallbackForError = true;
            }
        } catch (UnsatisfiedLinkError e) {
            String error = "Failed to load native library: " + e.getMessage();
            Log.e(Util.PUBLIC_LOG_TAG, error);
            manualFallbackForError = true;
        }
    }

    /**
     * Custom loadLibrary method that first tries to load the libraries from the built-in libs
     * directory and if it fails, tries to use the alternative libs path if one is set.
     *
     * No checks are performed to ensure that the native libraries match the cardIO library version.
     * This needs to be handled by the consuming application.
     */
    private static void loadLibrary(String libraryName) throws UnsatisfiedLinkError {
        try {
            System.loadLibrary(libraryName);
        } catch (UnsatisfiedLinkError e) {
            String altLibsPath = CardIONativeLibsConfig.getAlternativeLibsPath();
            if (altLibsPath == null || altLibsPath.length() == 0) {
                throw e;
            }
            if (!File.separator.equals(altLibsPath.charAt(altLibsPath.length() - 1))) {
                altLibsPath += File.separator;
            }
            String fullPath = altLibsPath + Build.CPU_ABI + File.separator +
                    System.mapLibraryName(libraryName);
            Log.d(Util.PUBLIC_LOG_TAG, "loadLibrary failed for library " + libraryName + ". Trying " + fullPath);
            // If we couldn't find the library in the normal places and we have an additional
            // search path, try loading from there.
            System.load(fullPath);
        }
    }

    private static boolean usesSupportedProcessorArch() {
        return nUseNeon() || nUseTegra() || nUseX86();
    }

    static boolean processorSupported() {
        return (!manualFallbackForError && (usesSupportedProcessorArch()));
    }

    CardScanner(CardIOActivity scanActivity, int currentFrameOrientation) {
        Intent scanIntent = scanActivity.getIntent();
        if (scanIntent != null) {
            mSuppressScan = scanIntent.getBooleanExtra(CardIOActivity.EXTRA_SUPPRESS_SCAN, false);
            mScanExpiry = scanIntent.getBooleanExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false)
                    && scanIntent.getBooleanExtra(CardIOActivity.EXTRA_SCAN_EXPIRY, true);
            mUnblurDigits = scanIntent.getIntExtra(CardIOActivity.EXTRA_UNBLUR_DIGITS, DEFAULT_UNBLUR_DIGITS);
        }
        mScanActivityRef = new WeakReference<>(scanActivity);
        mFrameOrientation = currentFrameOrientation;
        nSetup(mSuppressScan, MIN_FOCUS_SCORE, mUnblurDigits);
    }

    /**
     * Connect or reconnect to camera. If fails, sleeps and tries again. Returns <code>true</code> if successful,
     * <code>false</code> if maxTimeout passes.
     */
    private Camera connectToCamera(int checkInterval, int maxTimeout) {
        long start = System.currentTimeMillis();
        if (useCamera) {
            do {
                try {
                    // Camera.open() will open the back-facing camera. Front cameras are not
                    // attempted.
                    return Camera.open();
                } catch (RuntimeException e) {
                    try {
                        Log.w(Util.PUBLIC_LOG_TAG,
                                "Wasn't able to connect to camera service. Waiting and trying again...");
                        Thread.sleep(checkInterval);
                    } catch (InterruptedException e1) {
                        Log.e(Util.PUBLIC_LOG_TAG, "Interrupted while waiting for camera", e1);
                    }
                } catch (Exception e) {
                    Log.e(Util.PUBLIC_LOG_TAG, "Unexpected exception. Please report it as a GitHub issue", e);
                    maxTimeout = 0;
                }

            } while (System.currentTimeMillis() - start < maxTimeout);
        }

        return null;
    }

    void prepareScanner() {
        mFirstPreviewFrame = true;
        mAutoFocusStartedAt = 0;
        mAutoFocusCompletedAt = 0;

        numManualRefocus = 0;
        numAutoRefocus = 0;
        numManualTorchChange = 0;

        numFramesSkipped = 0;

        if (useCamera && mCamera == null) {
            mCamera = connectToCamera(CAMERA_CONNECT_RETRY_INTERVAL, CAMERA_CONNECT_TIMEOUT);
            if (mCamera == null) {
                Log.e(Util.PUBLIC_LOG_TAG, "prepare scanner couldn't connect to camera!");
                return;
            }

            setCameraDisplayOrientation(mCamera);

            Camera.Parameters parameters = mCamera.getParameters();

            List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            if (supportedPreviewSizes != null) {
                Size previewSize = null;
                for (Size s : supportedPreviewSizes) {
                    if (s.width == 640 && s.height == 480) {
                        previewSize = s;
                        break;
                    }
                }
                if (previewSize == null) {
                    previewSize = supportedPreviewSizes.get(0);

                    previewSize.width = mPreviewWidth;
                    previewSize.height = mPreviewHeight;
                }
            }

            parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);

            mCamera.setParameters(parameters);
        }

        if (detectedBitmap == null) {
            detectedBitmap = Bitmap.createBitmap(CREDIT_CARD_TARGET_WIDTH,
                    CREDIT_CARD_TARGET_HEIGHT, Bitmap.Config.ARGB_8888);
        }
    }

    @SuppressWarnings("deprecation")
    boolean resumeScanning(SurfaceHolder holder) {
        if (mCamera == null) {
            prepareScanner();
        }

        if (useCamera && mCamera == null) {
            return false;
        }

        assert holder != null;

        if (useCamera && mPreviewBuffer == null) {
            Camera.Parameters parameters = mCamera.getParameters();
            int previewFormat = parameters.getPreviewFormat();
            int bytesPerPixel = ImageFormat.getBitsPerPixel(previewFormat) / 8;
            int bufferSize = mPreviewWidth * mPreviewHeight * bytesPerPixel * 3;

            mPreviewBuffer = new byte[bufferSize];
            mCamera.addCallbackBuffer(mPreviewBuffer);
        }

        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if (useCamera) {
            mCamera.setPreviewCallbackWithBuffer(this);
        }

        if (isSurfaceValid) {
            makePreviewGo(holder);
        }

        // Turn flash off
        setFlashOn(false);
        captureStart = System.currentTimeMillis();

        nResetAnalytics();

        return true;
    }

    public void pauseScanning() {
        setFlashOn(false);
        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewDisplay(null);
            } catch (IOException e) {
                Log.w(Util.PUBLIC_LOG_TAG, "can't stop preview display", e);
            }
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mPreviewBuffer = null;
            mCamera = null;
        }
    }

    public void endScanning() {
        if (mCamera != null) {
            pauseScanning();
        }
        nCleanup();

        mPreviewBuffer = null;
    }

    /*
     * --------------------------- SurfaceHolder callbacks
     */

    private boolean makePreviewGo(SurfaceHolder holder) {
        // method name from http://www.youtube.com/watch?v=-WmGvYDLsj4
        assert holder != null;
        assert holder.getSurface() != null;
        mFirstPreviewFrame = true;

        if (useCamera) {
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                return false;
            }
            try {
                mCamera.startPreview();
                mCamera.autoFocus(this);
            } catch (RuntimeException e) {
                return false;
            }
        }
        return true;
    }

    /*
     * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder )
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where to draw.
        if (mCamera != null || !useCamera) {
            isSurfaceValid = true;
            makePreviewGo(holder);
        } else {
            Log.wtf(Util.PUBLIC_LOG_TAG, "CardScanner.surfaceCreated() - camera is null!");
            return;
        }
    }

    /*
     * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder , int,
     * int, int)
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    /*
     * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view. SurfaceHolder)
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                Log.e(Util.PUBLIC_LOG_TAG, "error stopping camera", e);
            }
        }
        isSurfaceValid = false;
    }

    /**
     * Handles processing of each frame.
     * <p/>
     * This method is called by Android, never directly by application code.
     */
    private static boolean processingInProgress = false;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        if (data == null) {
            return;
        }

        if (processingInProgress) {
            // return frame buffer to pool
            numFramesSkipped++;
            if (camera != null) {
                camera.addCallbackBuffer(data);
            }
            return;
        }
        processingInProgress = true;

        // TODO: eliminate this foolishness and measure/layout properly.
        if (mFirstPreviewFrame) {
            mFirstPreviewFrame = false;
            mFrameOrientation = ORIENTATION_PORTRAIT;
            mScanActivityRef.get().onFirstFrame();
        }

        DetectionInfo dInfo = new DetectionInfo();

        /** pika **/
        nScanFrame(data, mPreviewWidth, mPreviewHeight, mFrameOrientation, dInfo, detectedBitmap, mScanExpiry);

        boolean sufficientFocus = (dInfo.focusScore >= MIN_FOCUS_SCORE);

        if (!sufficientFocus) {
            triggerAutoFocus(false);
        } else if (dInfo.predicted() || (mSuppressScan && dInfo.detected())) {
            mScanActivityRef.get().onCardDetected(detectedBitmap, dInfo);
        }
        // give the image buffer back to the camera, AFTER we're done reading
        // the image.
        if (camera != null) {
            camera.addCallbackBuffer(data);
        }
        processingInProgress = false;

    }

    void onEdgeUpdate(DetectionInfo dInfo) {
        mScanActivityRef.get().onEdgeUpdate(dInfo);
    }

    Rect getGuideFrame(int orientation, int previewWidth, int previewHeight) {
        Rect r = null;
        if (processorSupported()) {
            r = new Rect();
            nGetGuideFrame(orientation, previewWidth, previewHeight, r);
        }
        return r;
    }

    Rect getGuideFrame() {
        return getGuideFrame(mFrameOrientation, mPreviewHeight, mPreviewWidth);
    }

    Rect getGuideFrame(int width, int height) {
        return getGuideFrame(mFrameOrientation, width, height);
    }

    void setDeviceOrientation(int orientation) {
        mFrameOrientation = orientation;
    }

    int getDeviceOrientation() {
        return mFrameOrientation;
    }

    Map<String, Object> getAnalytics() {
        HashMap<String, Object> analytics = new HashMap<String, Object>(11);

        analytics.put("num_frames_scanned", Integer.valueOf(nGetNumFramesScanned()));
        analytics.put("num_frames_skipped", Integer.valueOf(numFramesSkipped));

        analytics.put("elapsed_time", Double.valueOf((System.currentTimeMillis() - captureStart) / 1000));

        analytics.put("num_manual_refocusings", Integer.valueOf(numManualRefocus));
        analytics.put("num_auto_triggered_refocusings", Integer.valueOf(numAutoRefocus));
        analytics.put("num_manual_torch_changes", Integer.valueOf(numManualTorchChange));
        return analytics;
    }

    // ------------------------------------------------------------------------
    // CAMERA CONTROL & CALLBACKS
    // ------------------------------------------------------------------------

    /**
     * Invoked when autoFocus is complete
     * <p/>
     * This method is called by Android, never directly by application code.
     */
    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        mAutoFocusCompletedAt = System.currentTimeMillis();
    }

    /**
     * True if autoFocus is in progress
     */
    boolean isAutoFocusing() {
        return mAutoFocusCompletedAt < mAutoFocusStartedAt;
    }

    void toggleFlash() {
        setFlashOn(!isFlashOn());
    }

    // ------------------------------------------------------------------------
    // MISC CAMERA CONTROL
    // ------------------------------------------------------------------------

    /**
     * Tell Preview's camera to trigger autofocus.
     *
     * @param isManual callback for when autofocus is complete
     */
    void triggerAutoFocus(boolean isManual) {
        if (useCamera && !isAutoFocusing()) {
            try {
                mAutoFocusStartedAt = System.currentTimeMillis();
                mCamera.autoFocus(this);
                if (isManual) {
                    numManualRefocus++;
                } else {
                    numAutoRefocus++;
                }
            } catch (RuntimeException e) {
                Log.w(TAG, "could not trigger auto focus: " + e);
            }
        }
    }

    /**
     * Check if the flash is on.
     *
     * @return state of the flash.
     */

    public boolean isFlashOn() {
        if (!useCamera) {
            return false;
        }
        Camera.Parameters params = mCamera.getParameters();
        return params.getFlashMode().equals(Parameters.FLASH_MODE_TORCH);
    }

    /**
     * Set the flash on or off
     *
     * @param b desired flash state
     * @return <code>true</code> if successful
     */

    public boolean setFlashOn(boolean b) {
        if (mCamera != null) {
            try {
                Camera.Parameters params = mCamera.getParameters();
                params.setFlashMode(b ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(params);

                numManualTorchChange++;

                return true;
            } catch (RuntimeException e) {
                Log.w(TAG, "Could not set flash mode: " + e);
            }
        }
        return false;
    }

    private void setCameraDisplayOrientation(Camera mCamera) {
        int result;

        /* check API level. If upper API level 21, re-calculate orientation. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(0, info);
            int degrees = getRotationalOffset();
            int cameraOrientation = info.orientation;
            result = (cameraOrientation - degrees + 360) % 360;
        } else {
            /* if API level is lower than 21, use the default value */
            result = 90;
        }

        /*set display orientation*/
        mCamera.setDisplayOrientation(result);
    }

    /**
     * @see <a
     * href="http://stackoverflow.com/questions/12216148/android-screen-orientation-differs-between-devices">SO
     * post</a>
     */
    int getRotationalOffset() {
        final int rotationOffset;
        // Check "normal" screen orientation and adjust accordingly
        int naturalOrientation = ((WindowManager) mScanActivityRef.get().getSystemService(Context.WINDOW_SERVICE))
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
}
