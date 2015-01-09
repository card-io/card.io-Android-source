package io.card.payment;

/* Logo.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;

import io.card.payment.ui.ViewUtil;

// TODO - cache logo drawing as a bitmap and just draw that
// TODO - get alpha overlay computation working properly with whites. should not look gray.

class Logo {

    private static final int ALPHA = 100;
    private final Paint mPaint;

    private Bitmap mLogo;
    private boolean mUseCardIOLogo;
    private final Context mContext;

    public Logo(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setAlpha(ALPHA);
        mLogo = null;
        mContext = context;
    }

    void loadLogo(boolean useCardIOLogo) {
        if (mLogo != null && useCardIOLogo == mUseCardIOLogo) {
            return; // no change, don't reload
        }
        int density = DisplayMetrics.DENSITY_HIGH;
        mUseCardIOLogo = useCardIOLogo;
        if (useCardIOLogo) {
            mLogo = ViewUtil.base64ToBitmap(Base64EncodedImages.card_io_logo, mContext, density);
        } else {
            mLogo = ViewUtil.base64ToBitmap(Base64EncodedImages.paypal_logo, mContext, density);
        }
    }

    public void draw(Canvas canvas, float maxWidth, float maxHeight) {

        if (mLogo == null) {
            loadLogo(false);
        }

        canvas.save();

        float drawWidth, drawHeight;
        float targetAspectRatio = (float) mLogo.getHeight() / mLogo.getWidth();
        if ((maxHeight / maxWidth) < targetAspectRatio) {
            drawHeight = maxHeight;
            drawWidth = maxHeight / targetAspectRatio;
        } else {
            drawWidth = maxWidth;
            drawHeight = maxWidth * targetAspectRatio;
        }

        float halfWidth = drawWidth / 2;
        float halfHeight = drawHeight / 2;

        canvas.drawBitmap(mLogo, new Rect(0, 0, mLogo.getWidth(), mLogo.getHeight()), new RectF(
                -halfWidth, -halfHeight, halfWidth, halfHeight), mPaint);

        canvas.restore();
    }

}
