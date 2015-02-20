package io.card.payment;

/* Preview.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

/**
 * This class contains a SurfaceView, which is used to display the camera preview frames. It
 * performs basic layout and life cycle tasks for the camera and camera previews.
 * <p/>
 * Technical note: display of camera preview frames will only work when there is a valid surface
 * view to display those frames on. To that end, I have added a valid surface flag that is updated
 * by surface view lifecycle callbacks. We only attempt (re-)start camera preview if there is a
 * valid surface view to draw on.
 */
class Preview extends ViewGroup {
    private static final String TAG = Preview.class.getSimpleName();

    private boolean isSurfaceValid;
    private int mPreviewWidth;
    private int mPreviewHeight;

    SurfaceView mSurfaceView;

    public Preview(Context context, AttributeSet attributeSet, int previewWidth, int previewHeight) {
        super(context, attributeSet);

        // the preview size comes from the cardScanner (camera)
        // need to swap width & height to account for implicit 90deg rotation
        // which is part of cardScanner. see "mCamera.setDisplayOrientation(90);"
        mPreviewWidth = previewHeight;
        mPreviewHeight = previewWidth;

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);
    }

    public SurfaceView getSurfaceView() {
        assert mSurfaceView != null;
        return mSurfaceView;
    }

    SurfaceHolder getSurfaceHolder() {
        SurfaceHolder holder = getSurfaceView().getHolder();
        assert holder != null;
        return holder;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Log.d(TAG, "Preview.onDraw()");

        super.onDraw(canvas);
        canvas.drawARGB(255, 255, 0, 0);
    }

    // ------------------------------------------------------------------------
    // LAYOUT METHODS
    // ------------------------------------------------------------------------

    // TODO - document
    // Need a better explanation of why onMeasure is needed and how width/height are determined.
    // Must the camera be set first via setCamera? What if mSupportedPreviewSizes == null?
    // Why do we startPreview within this method if the surface is valid?
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        Log.d(TAG, String.format("Preview.onMeasure(w:%d, h:%d) setMeasuredDimension(w:%d, h:%d)",
                widthMeasureSpec, heightMeasureSpec, width, height));

        setMeasuredDimension(width, height);
    }

    // TODO - document
    // What is the child surface? The camera preview image?
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "Preview.onLayout()");
        Log.d(TAG, "- isSurfaceValid: " + isSurfaceValid);

        if (changed && getChildCount() > 0) {
            assert mSurfaceView != null;

            final int width = r - l;
            final int height = b - t;

            // Log.i(TAG, String.format("onLayout() - child measurements: {w:%d, h:%d}",
            // getMeasuredWidth(), getMeasuredHeight()));

            // Center the child SurfaceView within the parent, making sure that the preview is
            // *always* fully contained on the device screen.
            if (width * mPreviewHeight > height * mPreviewWidth) {
                final int scaledChildWidth = mPreviewWidth * height / mPreviewHeight;
                mSurfaceView.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = mPreviewHeight * width / mPreviewWidth;
                mSurfaceView.layout(0, (height - scaledChildHeight) / 2, width,
                        (height + scaledChildHeight) / 2);
            }
        }
    }

}
