package io.card.payment;

/* Util.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Build;
import android.os.Debug;
import android.util.Log;

import java.util.List;

/**
 * This class has various static utility methods.
 */

class Util {
    private static final String TAG = Util.class.getSimpleName();
    private static final boolean TORCH_BLACK_LISTED = (Build.MODEL.equals("DROID2"));

    public static final String PUBLIC_LOG_TAG = "card.io";

    private static Boolean sHardwareSupported;

    public static boolean deviceSupportsTorch(Context context) {
        return !TORCH_BLACK_LISTED
                && context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @SuppressWarnings("rawtypes")
    public static String manifestHasConfigChange(ResolveInfo resolveInfo, Class activityClass) {
        String error = null;
        if (resolveInfo == null) {
            error = String.format("Didn't find %s in the AndroidManifest.xml",
                    activityClass.getName());
        } else if (!Util.hasConfigFlag(resolveInfo.activityInfo.configChanges,
                ActivityInfo.CONFIG_ORIENTATION)) {
            error = activityClass.getName()
                    + " requires attribute android:configChanges=\"orientation\"";
        }
        if (error != null) {
            Log.e(Util.PUBLIC_LOG_TAG, error);
        }
        return error;
    }

    public static boolean hasConfigFlag(int config, int configFlag) {
        return ((config & configFlag) == configFlag);
    }

    /* --- HARDWARE SUPPORT --- */

    public static boolean hardwareSupported() {
        if (sHardwareSupported == null) {
            sHardwareSupported = hardwareSupportCheck();
        }
        return sHardwareSupported;
    }

    private static boolean hardwareSupportCheck() {
        Log.i(PUBLIC_LOG_TAG, "Checking hardware support...");

        // we currently need froyo or better (aka Android 2.2, API level 8)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            Log.w(PUBLIC_LOG_TAG,
                    "- Android SDK too old. Minimum Android 2.2 / API level 8+ (Froyo) required");
            return false;
        }

        if (!CardScanner.processorSupported()) {
            Log.w(PUBLIC_LOG_TAG, "- Processor type is not supported");
            return false;
        }

        // Camera needs to open
        Camera c = null;
        try {
            c = Camera.open();
        } catch (RuntimeException e) {
            Log.w(PUBLIC_LOG_TAG, "- Error opening camera: " + e);
            throw new CameraUnavailableException();
        }
        if (c == null) {
            Log.w(PUBLIC_LOG_TAG, "- No camera found");
            return false;
        } else {
            List<Camera.Size> list = c.getParameters().getSupportedPreviewSizes();
            c.release();

            boolean supportsVGA = false;

            for (Camera.Size s : list) {
                if (s.width == 640 && s.height == 480) {
                    supportsVGA = true;
                    break;
                }
            }

            if (!supportsVGA) {
                Log.w(PUBLIC_LOG_TAG, "- Camera resolution is insufficient");
                return false;
            }
        }
        return true;
    }

    public static String getNativeMemoryStats() {
        return "(free/alloc'd/total)" + Debug.getNativeHeapFreeSize() + "/"
                + Debug.getNativeHeapAllocatedSize() + "/" + Debug.getNativeHeapSize();
    }

    public static void logNativeMemoryStats() {
        Log.d("MEMORY", "Native memory stats: " + getNativeMemoryStats());
    }

    static public Rect rectGivenCenter(Point center, int width, int height) {
        return new Rect(center.x - width / 2, center.y - height / 2, center.x + width / 2, center.y
                + height / 2);
    }

    static public void setupTextPaintStyle(Paint paint) {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setAntiAlias(true);
        float[] black = { 0f, 0f, 0f };
        paint.setShadowLayer(1.5f, 0.5f, 0f, Color.HSVToColor(200, black));
    }
}