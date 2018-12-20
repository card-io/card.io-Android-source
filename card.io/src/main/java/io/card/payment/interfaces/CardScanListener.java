package io.card.payment.interfaces;

import android.graphics.Bitmap;

import io.card.payment.CreditCard;

/**
 * Created by glaubermartins on 2018-03-29.
 */

public interface CardScanListener {

    void onCardScanSuccess(CreditCard cc, Bitmap bitmap);
    void onCardScanFail();
    void onPictureTaken(byte[] data);
}
