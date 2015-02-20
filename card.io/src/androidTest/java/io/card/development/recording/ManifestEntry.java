package io.card.development.recording;

/* ManifestEntry.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Describes metadata about test recordings.
 * <p/>
 * Test recordings are fed directly into the video frame callback for testing of the vision pipeline.
 * <p/>
 * This package should be striped from released code. (e.g. via Proguard)
 */

public class ManifestEntry {
    private String yFilename;
    private String cbFilename;
    private String crFilename;
    private byte[] yImage;
    private byte[] cbImage;
    private byte[] crImage;
    private int[] focusScores;

    private int overallLuma;
    private double fallbackFocusScore;
    private int temporalMotion;
    private boolean exposureLimitsReached;
    private int focusPosition;
    private int currentOrientation;
    private double timestamp;

    private static final String TAG = ManifestEntry.class.getSimpleName();

    public static ManifestEntry[] getManifest(Hashtable<String, byte[]> recordingData) {

        Enumeration<String> keys = recordingData.keys();
        String manifestKey = null;
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (key.endsWith("manifest.json")) {
                manifestKey = key;
                break;
            }
        }
        if (manifestKey == null) {
            return null;
        }

        String manifestDirName = manifestKey.substring(0, manifestKey.lastIndexOf("/"));
        String manifestString = new String(recordingData.get(manifestKey));

        JSONArray manifestData;
        try {
            manifestData = new JSONArray(manifestString);
        } catch (JSONException e1) {
            Log.e(TAG, "Exception parsing JSON array: " + e1.getMessage());
            return null;
        }

        ManifestEntry[] manifest = new ManifestEntry[manifestData.length()];

        for (int i = 0; i < manifestData.length(); i++) {
            try {
                JSONObject jo = manifestData.getJSONObject(i);
                manifest[i] = new ManifestEntry(manifestDirName, jo, recordingData);
            } catch (JSONException e) {
                Log.e(TAG, "Couldn't parse JSON: " + e.getMessage());
            }
        }

        return manifest;
    }

    public ManifestEntry(String manifestDirName, JSONObject data,
                         Hashtable<String, byte[]> recordingData) throws JSONException {
        yFilename = data.getString("y_filename");
        cbFilename = data.getString("cb_filename");
        crFilename = data.getString("cr_filename");
        fallbackFocusScore = data.getDouble("fallback_focus_score");
        temporalMotion = data.getInt("temporal_motion");
        exposureLimitsReached = data.getInt("exposure_limits_reached") > 0;
        currentOrientation = data.getInt("current_orientation");
        timestamp = data.getDouble("timestamp");

        if (data.has("overall_luma")) {
            overallLuma = data.getInt("overall_luma");
        }

        if (data.has("focus_position")) {
            focusPosition = data.getInt("focus_position");
        }

        JSONArray focusScoresData = data.getJSONArray("focus_scores");
        focusScores = new int[focusScoresData.length()];
        for (int i = 0; i < focusScoresData.length(); i++) {
            focusScores[i] = focusScoresData.getInt(i);
        }

        yImage = recordingData.get(manifestDirName + "/" + yFilename);
        cbImage = recordingData.get(manifestDirName + "/" + cbFilename);
        crImage = recordingData.get(manifestDirName + "/" + crFilename);
    }

    public String getYFilename() {
        return yFilename;
    }

    public String getCbFilename() {
        return cbFilename;
    }

    public String getCrFilename() {
        return crFilename;
    }

    public int[] getFocusScores() {
        return focusScores;
    }

    private static byte[] decompress(byte[] compressed) {
        /*
         * DO NOT EVER USE THIS METHOD IN PRODUCTION This is horribly inefficient, but written only
         * for testing purposes.
         */

        Bitmap b = BitmapFactory.decodeByteArray(compressed, 0, compressed.length);
        ByteBuffer bb = ByteBuffer.allocate(b.getWidth() * b.getHeight() * 4);
        b.copyPixelsToBuffer(bb);
        b.recycle();

        byte[] ba = bb.array();
        byte[] singleChannel = new byte[ba.length / 4]; // 4 channels
        for (int i = 0; i < singleChannel.length; i++) {
            singleChannel[i] = ba[i * 4 + 1];
        }

        return singleChannel;
    }

    public byte[] getYImage() {
        return decompress(yImage);
    }

    public byte[] getCbImage() {
        return decompress(cbImage);
    }

    public byte[] getCrImage() {
        return decompress(crImage);
    }

    public int getOverallLuma() {
        return overallLuma;
    }

    public double getFallbackFocusScore() {
        return fallbackFocusScore;
    }

    public int getTemporalMotion() {
        return temporalMotion;
    }

    public boolean isExposureLimitsReached() {
        return exposureLimitsReached;
    }

    public int getFocusPosition() {
        return focusPosition;
    }

    public int getCurrentOrientation() {
        return currentOrientation;
    }

    public double getTimestamp() {
        return timestamp;
    }

}
