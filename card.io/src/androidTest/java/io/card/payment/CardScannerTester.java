package io.card.payment;

/* CardScannerTester.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Iterator;

/**
 * This class is used for Robotium testing ONLY!
 * <p/>
 * ALL classes that match *Tester are excluded from the library jar. As such, they should only be
 * accessed by reflection.
 */
public class CardScannerTester extends CardScanner {
    private static final String TAG = CardScannerTester.class.getSimpleName();
    private static Iterator<byte[]> recording = null;

    static final long FRAME_INTERVAL = (long) (1000.0 / 30);

    private Handler mHandler = new Handler();
    private int testFrameCount = 0;
    private boolean scanAllowed = false;

    public CardScannerTester(CardIOActivity scanActivity, int currentFrameOrientation) {
        super(scanActivity, currentFrameOrientation);
        useCamera = false;
    }

    private Runnable frameRunner = new Runnable() {
        private byte[] lastFrame = null;

        @Override
        public void run() {
            if (!scanAllowed) {
                return;
            }
            if (recording == null) {
                Log.e(TAG, "null recording!");
                return;
            }
            if (recording.hasNext()) {
                Log.i(TAG, "Setting test frame: " + testFrameCount++);
                lastFrame = recording.next();
            } else {
                Log.w(TAG, "No more frames left at " + testFrameCount + " repeating last frame indefinitely.");
            }
            onPreviewFrame(lastFrame, null);
            mHandler.postDelayed(this, FRAME_INTERVAL);
        }
    };

    private Runnable expireRunner = new Runnable() {
        @Override
        public void run() {
            if (!scanAllowed) {
                return;
            }
            mScanActivityRef.get().finish();
        }
    };

    public static void setRecording(Iterator<byte[]> r) {
        recording = r;
    }

    @Override
    boolean resumeScanning(SurfaceHolder holder) {
        boolean result = super.resumeScanning(holder);
        scanAllowed = true;
        mHandler.postDelayed(frameRunner, FRAME_INTERVAL);
        return result;
    }

    @Override
    public void pauseScanning() {
        scanAllowed = false;
        super.pauseScanning();
    }
}
