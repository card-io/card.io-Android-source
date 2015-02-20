package io.card.development.recording;

/* Recording.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Represents captured recording sequences.
 * <p/>
 * Test recordings are fed directly into the video frame callback for testing of the vision pipeline.
 * <p/>
 * This package should be striped from released code. (e.g. via Proguard)
 */

public class Recording implements Iterator<byte[]> {
    private final String TAG = this.getClass().getName();

    private ManifestEntry[] manifestEntries;

    private Recording(ManifestEntry[] manifestEntries) {
        this.manifestEntries = manifestEntries;
    }

    public ManifestEntry[] getManifestEntries() {
        return manifestEntries;
    }

    public void setManifestEntries(ManifestEntry[] manifestEntries) {
        this.manifestEntries = manifestEntries;
    }

    private int currentFrameIdx = 0;
    private Hashtable<String, byte[]> recordingContents;

    public Recording(File zipfile) {
        try {
            FileInputStream fileStream = new FileInputStream(zipfile);
            BufferedInputStream bufStream = new BufferedInputStream(fileStream);
            recordingContents = unzipFiles(bufStream);

            assert recordingContents != null;

            Log.d(TAG, String.format("recording has %d items", recordingContents.size()));
            Log.d(TAG, "keys: ");

            manifestEntries = ManifestEntry.getManifest(recordingContents);

        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public boolean hasNext() {
        return (currentFrameIdx < manifestEntries.length - 1);
    }

    @Override
    public byte[] next() {
        byte[] frame = getFrame(currentFrameIdx);
        currentFrameIdx++;
        return frame;
    }

    public byte[] getFrame(int frameIdx) {
        ManifestEntry me = manifestEntries[frameIdx];

        byte[] Y = me.getYImage();
        byte[] Cb = me.getCbImage();
        byte[] Cr = me.getCrImage();

        assert Cb.length == Cr.length;
        assert Y.length == Cb.length * 4;

        byte[] frame = new byte[Y.length + Cb.length + Cr.length];
        System.arraycopy(Y, 0, frame, 0, Y.length);
        for (int i = 0; i < Cb.length; i++) {
            frame[Y.length + 2 * i + 1] = Cb[i];
            frame[Y.length + 2 * i] = Cr[i];
        }

        return frame;
    }

    @Override
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Frames cannot be removed from a recording");
    }

    public int getCurrentFrameIndex() {
        return currentFrameIdx;
    }

    public static Recording[] parseRecordings(InputStream recordingStream) throws IOException,
            JSONException {
        Hashtable<String, byte[]> recordingFiles = unzipFiles(recordingStream);
        Hashtable<String, byte[]> manifestFiles = findManifestFiles(recordingFiles);

        int recordingIndex = 0;
        Recording[] recordings = new Recording[manifestFiles.size()];
        Enumeration<String> manifestFilenames = manifestFiles.keys();
        while (manifestFilenames.hasMoreElements()) {
            String manifestFilename = manifestFilenames.nextElement();
            String manifestDirName = manifestFilename.substring(0,
                    manifestFilename.lastIndexOf("/"));
            byte[] manifestData = manifestFiles.get(manifestFilename);

            ManifestEntry[] manifestEntries = buildManifest(manifestDirName, manifestData,
                    recordingFiles);
            recordings[recordingIndex] = new Recording(manifestEntries);
            recordingIndex++;
        }

        return recordings;
    }

    private static Hashtable<String, byte[]> unzipFiles(InputStream recordingStream)
            throws IOException {
        ZipEntry zipEntry;
        ZipInputStream zis = new ZipInputStream(recordingStream);
        Hashtable<String, byte[]> fileHash = new Hashtable<String, byte[]>();
        byte[] buffer = new byte[512];

        while ((zipEntry = zis.getNextEntry()) != null) {
            if (!zipEntry.isDirectory()) {
                int read = 0;
                ByteArrayOutputStream fileStream = new ByteArrayOutputStream();

                do {
                    read = zis.read(buffer, 0, buffer.length);
                    if (read != -1) {
                        fileStream.write(buffer, 0, read);
                    }
                } while (read != -1);

                byte[] fileData = fileStream.toByteArray();
                fileHash.put(zipEntry.getName(), fileData);
            }
        }

        return fileHash;
    }

    private static Hashtable<String, byte[]> findManifestFiles(
            Hashtable<String, byte[]> recordingFiles) {
        Hashtable<String, byte[]> manifestFiles = new Hashtable<String, byte[]>();
        Enumeration<String> fileNames = recordingFiles.keys();

        while (fileNames.hasMoreElements()) {
            String fileName = fileNames.nextElement();
            if (fileName.endsWith("manifest.json")) {
                manifestFiles.put(fileName, recordingFiles.get(fileName));
            }
        }

        return manifestFiles;
    }

    private static ManifestEntry[] buildManifest(String manifestDirName, byte[] manifestData,
                                                 Hashtable<String, byte[]> recordingFiles) throws JSONException {
        String manifestString = new String(manifestData);
        JSONArray manifestJson = new JSONArray(manifestString);

        ManifestEntry[] manifestEntries = new ManifestEntry[manifestJson.length()];
        for (int i = 0; i < manifestEntries.length; i++) {
            JSONObject manifestEntryJson = manifestJson.getJSONObject(i);
            manifestEntries[i] = new ManifestEntry(manifestDirName, manifestEntryJson,
                    recordingFiles);
        }

        return manifestEntries;
    }
}
