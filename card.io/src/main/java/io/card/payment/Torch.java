package io.card.payment;

/* Torch.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.Log;

import java.util.Arrays;

class Torch {
    private static final String TAG = Torch.class.getSimpleName();

    private static final float CORNER_RADIUS = 5f;
    private static final int BG_ALPHA = 96;

    private boolean mOn;
    private float mWidth;
    private float mHeight;

    public Torch(float width, float height) {
        mOn = false;
        mWidth = width;
        mHeight = height;
    }

    public void draw(Canvas canvas) {

        canvas.save();
        canvas.translate(-mWidth / 2, -mHeight / 2);
        float cornerRadius = CORNER_RADIUS;
        // Create border paint
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Style.STROKE);
        borderPaint.setAntiAlias(true);
        borderPaint.setStrokeWidth(1.5f);

        // Create fill paint
        Paint fillPaint = new Paint();
        fillPaint.setStyle(Style.FILL);
        fillPaint.setColor(Color.WHITE);
        if (mOn) {
            fillPaint.setAlpha(BG_ALPHA * 2);
        } else {
            fillPaint.setAlpha(BG_ALPHA);
        }

        // Create the button itself
        float[] outerRadii = new float[8];
        Arrays.fill(outerRadii, cornerRadius);
        RoundRectShape buttonShape = new RoundRectShape(outerRadii, null, null);
        buttonShape.resize(mWidth, mHeight);

        // Draw the button stroke and background
        buttonShape.draw(canvas, fillPaint);
        buttonShape.draw(canvas, borderPaint);

        // Create bolt fill paint
        Paint boltPaint = new Paint();
        boltPaint.setStyle(Style.FILL_AND_STROKE);
        boltPaint.setAntiAlias(true);
        if (mOn) {
            boltPaint.setColor(Color.WHITE);
        } else {
            boltPaint.setColor(Color.BLACK);
        }

        // Draw the bolt itself
        Path boltPath = createBoltPath();
        Matrix m = new Matrix();
        float boltHeight = .8f * mHeight;
        m.postScale(boltHeight, boltHeight);
        boltPath.transform(m);
        canvas.translate(mWidth / 2, mHeight / 2);
        canvas.drawPath(boltPath, boltPaint);
        canvas.restore();
    }

    public void setOn(boolean on) {
        Log.d(TAG, "Torch " + (on ? "ON" : "OFF"));
        mOn = on;
    }

    /**
     * @return Path of width height 1 and width 0.65
     */

    static private Path createBoltPath() {

        Path p = new Path();
        p.moveTo(10.0f, 0.0f); // top
        p.lineTo(0.0f, 11.0f); // left
        p.lineTo(6.0f, 11.0f); // left indent
        p.lineTo(2.0f, 20.0f); // bottom
        p.lineTo(13.0f, 8.0f); // right
        p.lineTo(7.0f, 8.0f); // right indent
        p.lineTo(10.0f, 0.0f); // top
        p.setLastPoint(10.0f, 0.0f);
        Matrix m = new Matrix();
        m.postTranslate(-6.5f, -10.0f);
        m.postScale(1 / 20.0f, 1 / 20.0f);
        p.transform(m);
        return p;
    }

}
