package io.card.payment;

/* CardScanner.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;

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

    private static final int CAMERA_CONNECT_TIMEOUT = 5000;
    private static final int CAMERA_CONNECT_RETRY_INTERVAL = 50;

    static final int ORIENTATION_PORTRAIT = 1;
    //static final int ORIENTATION_PORTRAIT_UPSIDE_DOWN = 2;
    //static final int ORIENTATION_LANDSCAPE_RIGHT = 3;
    //static final int ORIENTATION_LANDSCAPE_LEFT = 4;

    // these values MUST match those in dmz_constants.h
    static final int CREDIT_CARD_TARGET_WIDTH = 428; // kCreditCardTargetWidth
    static final int CREDIT_CARD_TARGET_HEIGHT = 270; // kCreditCardTargetHeight

    // NATIVE
    public static native boolean nUseNeon();

    public static native boolean nUseTegra();

    private native void nSetup(boolean shouldDetectOnly, float minFocusScore);

    private native void nResetAnalytics();

    private native void nGetGuideFrame(int orientation, int previewWidth, int previewHeight, Rect r);

    private native void nScanFrame(byte[] data, int frameWidth, int frameHeight, int orientation,
                                   DetectionInfo dinfo, Bitmap resultBitmap, boolean scanExpiry);

    private native int nGetNumFramesScanned();

    private native void nCleanup();

    private Bitmap detectedBitmap;

    private static boolean manualFallbackForError = false;

    // member data
    protected WeakReference<CardIOActivity> mScanActivityRef;
    private boolean mSuppressScan = false;
    private boolean mScanExpiry;

    // read by CardIOActivity to set up Preview
    final int mPreviewWidth = 640;
    final int mPreviewHeight = 480;

    private int mFrameOrientation = ORIENTATION_PORTRAIT;

    private boolean mFirstPreviewFrame = true;
    private long captureStart;
    private long mAutoFocusStartedAt = 0;
    private long mAutoFocusCompletedAt = 0;

    private DetectionInfo mLastDetectionInfo;

    private Camera mCamera = null;
    private byte[] mPreviewBuffer;

    // accessed by test harness subclass.
    protected boolean useCamera = true;

    private boolean isSurfaceValid = false;

    private int numManualRefocus;
    private int numAutoRefocus;
    private int numManualTorchChange;
    private int numFramesSkipped;

    // ------------------------------------------------------------------------
    // STATIC INITIALIZATION
    // ------------------------------------------------------------------------

    static {
        Log.i(Util.PUBLIC_LOG_TAG, "card.io " + BuildConfig.PRODUCT_VERSION + " " + BuildConfig.BUILD_TIME);

        try {
            System.loadLibrary("cardioDecider");
            Log.d(Util.PUBLIC_LOG_TAG, "Loaded card.io decider library.  nUseNeon():" + nUseNeon()
                    + ",nUseTegra():" + nUseTegra());

            if (nUseNeon() || nUseTegra()) {
                System.loadLibrary("opencv_core");
                Log.d(Util.PUBLIC_LOG_TAG, "Loaded opencv core library");
                System.loadLibrary("opencv_imgproc");
                Log.d(Util.PUBLIC_LOG_TAG, "Loaded opencv imgproc library");
            }
            if (nUseNeon()) {
                System.loadLibrary("cardioRecognizer");
                Log.i(Util.PUBLIC_LOG_TAG, "Loaded card.io NEON library");
            } else if (nUseTegra()) {
                System.loadLibrary("cardioRecognizer_tegra2");
                Log.i(Util.PUBLIC_LOG_TAG, "Loaded card.io Tegra2 library");
            } else {
                Log.w(Util.PUBLIC_LOG_TAG,
                        "unsupported processor - card.io scanning requires ARMv7 architecture");
                manualFallbackForError = true;
            }
        } catch (UnsatisfiedLinkError e) {
            String error = "Failed to load native library: " + e.getMessage();
            Log.e(Util.PUBLIC_LOG_TAG, error);
            manualFallbackForError = true;
        }
    }

    static boolean processorSupported() {
        return (!manualFallbackForError && (nUseNeon() || nUseTegra()));
    }

    CardScanner(CardIOActivity scanActivity, int currentFrameOrientation) {
        Intent scanIntent = scanActivity.getIntent();
        if (scanIntent != null) {
            mSuppressScan = scanIntent.getBooleanExtra(CardIOActivity.EXTRA_SUPPRESS_SCAN, false);
            mScanExpiry = scanIntent.getBooleanExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false)
                    && scanIntent.getBooleanExtra(CardIOActivity.EXTRA_SCAN_EXPIRY, true);
        }
        mScanActivityRef = new WeakReference<CardIOActivity>(scanActivity);
        mFrameOrientation = currentFrameOrientation;
        nSetup(mSuppressScan, MIN_FOCUS_SCORE);
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
                    Log.e(Util.PUBLIC_LOG_TAG,
                            "Unexpected exception. Please report it to support@card.io", e);
                    maxTimeout = 0;
                }

            } while (System.currentTimeMillis() - start < maxTimeout);
        }
        Log.w(TAG, "camera connect timeout");
        return null;
    }

    void prepareScanner() {
        Log.v(TAG, "prepareScanner()");
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
            } else {
                Log.v(TAG, "camera is connected");
            }

            mCamera.setDisplayOrientation(90);

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
                    Log.w(Util.PUBLIC_LOG_TAG,
                            "Didn't find a supported 640x480 resolution, so forcing");

                    previewSize = supportedPreviewSizes.get(0);

                    previewSize.width = mPreviewWidth;
                    previewSize.height = mPreviewHeight;
                }
            }

            Log.d(TAG, "- parameters: " + parameters);

            parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);

            mCamera.setParameters(parameters);
        } else if (!useCamera) {
            Log.w(TAG, "useCamera is false!");
        } else if (mCamera != null) {
            Log.v(TAG, "we already have a camera instance: " + mCamera);
        }
        if (detectedBitmap == null) {
            detectedBitmap = Bitmap.createBitmap(CREDIT_CARD_TARGET_WIDTH,
                    CREDIT_CARD_TARGET_HEIGHT, Bitmap.Config.ARGB_8888);
        }
    }

    @SuppressWarnings("deprecation")
    boolean resumeScanning(SurfaceHolder holder) {
        Log.v(TAG, "resumeScanning(" + holder + ")");
        if (mCamera == null) {
            Log.v(TAG, "preparing the scanner...");
            prepareScanner();
            Log.v(TAG, "preparations complete");
        }
        if (useCamera && mCamera == null) {
            // prepare failed!
            Log.i(TAG, "null camera. failure");
            return false;
        }

        assert holder != null;

        if (useCamera && mPreviewBuffer == null) {
            int previewFormat = ImageFormat.NV21; // the default.

            Log.v(TAG, "- mCamera:" + mCamera);
            Camera.Parameters parameters = mCamera.getParameters();
            previewFormat = parameters.getPreviewFormat();

            Log.v(TAG, "- preview format: " + previewFormat);

            int bytesPerPixel = ImageFormat.getBitsPerPixel(previewFormat) / 8;
            Log.v(TAG, "- bytes per pixel: " + bytesPerPixel);

            int bufferSize = mPreviewWidth * mPreviewHeight * bytesPerPixel * 3;
            Log.v(TAG, "- buffer size: " + bufferSize);

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
            Log.d(TAG, "- released camera");
            mCamera = null;
        }
        Log.i(TAG, "scan paused"); // tests look for this message. don't delete it.
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
        Log.d(TAG, "surfaceFrame: " + String.valueOf(holder.getSurfaceFrame()));
        mFirstPreviewFrame = true;

        if (useCamera) {
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                Log.e(Util.PUBLIC_LOG_TAG, "can't set preview display", e);
                return false;
            }
            try {
                mCamera.startPreview();
                mCamera.autoFocus(this);

                Log.d(TAG, "startPreview success");
            } catch (RuntimeException e) {
                Log.e(Util.PUBLIC_LOG_TAG, "startPreview failed on camera. Error: ", e);
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
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        Log.d(TAG, "Preview.surfaceCreated()");

        if (mCamera != null || !useCamera) {
            isSurfaceValid = true;
            makePreviewGo(holder);
        } else {
            Log.wtf(Util.PUBLIC_LOG_TAG, "CardScanner.surfaceCreated() - camera is null!");
            return;
        }

        Log.d(TAG, "Preview.surfaceCreated(), surface is valid");
    }

    /*
     * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder , int,
     * int, int)
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.d(TAG, String.format("Preview.surfaceChanged(holder?:%b, f:%d, w:%d, h:%d )",
                (holder != null), format, width, height));
    }

    /*
     * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view. SurfaceHolder)
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Preview.surfaceDestroyed()");
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

    private int frameCount = 0;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        if (data == null) {
            Log.w(TAG, "frame is null! skipping");
            return;
        }

        if (processingInProgress) {
            Log.e(TAG, "processing in progress.... dropping frame");
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
            Log.d(TAG, "mFirstPreviewFrame");
            mFirstPreviewFrame = false;
            mFrameOrientation = ORIENTATION_PORTRAIT;
            mScanActivityRef.get().onFirstFrame(ORIENTATION_PORTRAIT);
        }

        DetectionInfo dInfo = new DetectionInfo();

        /** pika **/
        nScanFrame(data, mPreviewWidth, mPreviewHeight, mFrameOrientation, dInfo, detectedBitmap, mScanExpiry);

        boolean sufficientFocus = (dInfo.focusScore >= MIN_FOCUS_SCORE);

        if (!sufficientFocus) {
            triggerAutoFocus(false);
        } else if (dInfo.predicted() || (mSuppressScan && dInfo.detected())) {
            Log.d(TAG, "detected card: " + dInfo.creditCard());
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
        // Log.d(TAG, "onEdgeUpdate");
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
        Log.d(TAG, "toggleFlash: currently " + (isFlashOn() ? "ON" : "OFF"));
        setFlashOn(!isFlashOn());
        Log.d(TAG, "toggleFlash - now " + (isFlashOn() ? "ON" : "OFF"));
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
            Log.d(TAG, "setFlashOn: " + b);
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
}
