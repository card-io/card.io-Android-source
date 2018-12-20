package io.card.payment.interfaces;

import android.app.Activity;
import android.graphics.Bitmap;

import io.card.payment.DetectionInfo;

/**
 * Created by glaubermartins on 2018-03-22.
 */

public interface CardScanRecognition {

    Activity getActivity();

    void onFirstFrame(int orientation);
    void onCardDetected(Bitmap bitmap, DetectionInfo info);
    void onEdgeUpdate(DetectionInfo info);

}
