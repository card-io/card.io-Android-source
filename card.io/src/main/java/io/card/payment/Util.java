package io.card.payment;

/* Util.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Build;
import android.os.Debug;
import android.util.Log;

/**
 * This class has various static utility methods.
 */

class Util {
    private static final String TAG = "Util";
    private static final boolean TORCH_BLACK_LISTED = (Build.MODEL.equals("DROID2"));

    public static final String PUBLIC_LOG_TAG = "card.io";

    private static Boolean sHardwareSupported;

    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String dump(byte[] buf, int offset, int length) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", buf[i + offset]));
            sb.append(i % 16 == 15 ? '\n' : ' ');
        }

        return sb.toString();
    }

    public static String dump(int[] buf, int offset, int length) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < length; i++) {
            sb.append(String.format("%08x", buf[i + offset]));
            sb.append(i % 8 == 7 ? '\n' : ' ');
        }

        return sb.toString();
    }

    public static boolean deviceSupportsTorch(Context context) {
        return !TORCH_BLACK_LISTED
                && context.getPackageManager()
                        .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private static NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager conMan;
        if (context != null) {
            conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        } else {
            Log.e(TAG,
                    "No Context for retrieving Connectivity Manager System Service: pass non-null context");
            return null;
        }
        return conMan.getActiveNetworkInfo();
    }

    public static String getConnectionType(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        if (info == null) {
            Log.w(TAG, "Network type string is NULL!");
            return null;
        }
        String connectionType = info.getTypeName();
        Log.d(TAG, "Network type string is: " + connectionType);
        return connectionType;
    }

    public static boolean isNetworkConnectionAvailable(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        if (info == null) {
            Log.w(TAG, "Network type is NULL!");
            return false;
        }
        Log.d(TAG, "Network type is: " + info.getTypeName());
        State network = info.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }

    public static float clamp(long start, float v1, long stop, float v2) {
        long now = System.currentTimeMillis();

        if (now < start)
            return v1;
        else if (now > stop)
            return v2;
        else {
            long mt = stop - start;
            long dt = now - start;
            float dv = v2 - v1;
            float pct = (float) dt / mt;

            return v1 + dv * pct;
        }
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
        if (error != null)
            Log.e(Util.PUBLIC_LOG_TAG, error);
        return error;
    }

    public static boolean hasConfigFlag(int config, int configFlag) {
        return ((config & configFlag) == configFlag);
    }

    @SuppressWarnings("rawtypes")
    public static String manifestHasPortriatOrientation(ResolveInfo resolveInfo, Class activityClass) {
        String error = null;
        if (resolveInfo == null) {
            error = String.format("Didn't find %s in the AndroidManifest.xml",
                    activityClass.getName());
        } else if (resolveInfo.activityInfo.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            error = activityClass.getName()
                    + " requires attribute android:screenOrientation=\"portrait\"";
        }
        if (error != null)
            Log.e(Util.PUBLIC_LOG_TAG, error);
        return error;
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

    public static String getRuntimeMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        float megaBytes = 1024 * 1024;
        return String.format("used: %.2f  free: %.2f  total: %.2f  max: %.2f",
                (runtime.totalMemory() - runtime.freeMemory()) / megaBytes, runtime.freeMemory()
                        / megaBytes, runtime.totalMemory() / megaBytes, runtime.maxMemory()
                        / megaBytes);
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