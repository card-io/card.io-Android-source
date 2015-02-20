package io.card.payment.ui;

/* ViewUtil.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for altering views.
 */
public class ViewUtil {
    /**
     * Wrapper to only use the deprecated {@link View#setBackgroundDrawable} on
     * older systems.
     *
     * @param view
     * @param drawable
     */
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void setBackground(View view, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    // DIMENSION HELPERS

    // see also similar work: http://stackoverflow.com/a/11353603/306657
    private static final Map<String, Integer> DIMENSION_STRING_CONSTANT =
            initDimensionStringConstantMap();
    static Pattern DIMENSION_VALUE_PATTERN = Pattern
            .compile("^\\s*(\\d+(\\.\\d+)*)\\s*([a-zA-Z]+)\\s*$");

    static Map<String, Integer> initDimensionStringConstantMap() {
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("px", TypedValue.COMPLEX_UNIT_PX);
        m.put("dip", TypedValue.COMPLEX_UNIT_DIP);
        m.put("dp", TypedValue.COMPLEX_UNIT_DIP);
        m.put("sp", TypedValue.COMPLEX_UNIT_SP);
        m.put("pt", TypedValue.COMPLEX_UNIT_PT);
        m.put("in", TypedValue.COMPLEX_UNIT_IN);
        m.put("mm", TypedValue.COMPLEX_UNIT_MM);
        return Collections.unmodifiableMap(m);
    }

    public static int typedDimensionValueToPixelsInt(String dimensionValueString, Context context) {
        if (dimensionValueString == null) {
            return 0;
        } else {
            return (int) typedDimensionValueToPixels(dimensionValueString, context);
        }
    }

    static HashMap<String, Float> pxDimensionLookupTable = new HashMap<String, Float>();

    @SuppressLint("DefaultLocale")
    public static float typedDimensionValueToPixels(String dimensionValueString, Context context) {
        if (dimensionValueString == null) {
            return 0;
        }
        dimensionValueString = dimensionValueString.toLowerCase();
        if (pxDimensionLookupTable.containsKey(dimensionValueString)) {
            return pxDimensionLookupTable.get(dimensionValueString);
        }
        Matcher m = DIMENSION_VALUE_PATTERN.matcher(dimensionValueString);
        if (!m.matches()) {
            throw new NumberFormatException();
        }
        float value = Float.parseFloat(m.group(1));
        String dimensionString = m.group(3).toLowerCase();
        Integer unit = DIMENSION_STRING_CONSTANT.get(dimensionString);
        if (unit == null) {
            unit = TypedValue.COMPLEX_UNIT_DIP;
        }
        float ret =
                TypedValue.applyDimension(unit, value, context.getResources().getDisplayMetrics());
        pxDimensionLookupTable.put(dimensionValueString, ret);
        return ret;
    }

    // ATTRIBUTE HELPERS

    public static void setPadding(View view, String left, String top, String right, String bottom) {
        Context context = view.getContext();
        view.setPadding(
                typedDimensionValueToPixelsInt(left, context),
                typedDimensionValueToPixelsInt(top, context),
                typedDimensionValueToPixelsInt(right, context),
                typedDimensionValueToPixelsInt(bottom, context));
    }

    // LAYOUT PARAM HELPERS

    /**
     * Set margins for given view if its LayoutParams are MarginLayoutParams.
     * Should be used after the view is already added to a layout.
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     * @category layout
     */
    public static void setMargins(View view, String left, String top, String right, String bottom) {
        Context context = view.getContext();
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) params).setMargins(
                    typedDimensionValueToPixelsInt(left, context),
                    typedDimensionValueToPixelsInt(top, context),
                    typedDimensionValueToPixelsInt(right, context),
                    typedDimensionValueToPixelsInt(bottom, context));
        }
    }

    public static void setDimensions(View view, int width, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        params.height = height;
    }

    public static void styleAsButton(View view, boolean primary, Context context) {
        setDimensions(view, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        setPadding(view, "10dip", "0dip", "10dip", "0dip");
        setBackground(
                view,
                primary ? Appearance.buttonBackgroundPrimary(context) : Appearance
                        .buttonBackgroundSecondary(context));

        view.setFocusable(true);

        view.setMinimumHeight(ViewUtil.typedDimensionValueToPixelsInt(
                Appearance.BUTTON_HEIGHT, context));
        if (view instanceof TextView) {
            styleAsButtonText((TextView) view);
        }
        if (!(view instanceof Button)) {
            view.setClickable(true);
        }
    }

    public static void styleAsButtonText(TextView textView) {
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(Appearance.TEXT_COLOR_BUTTON);
        textView.setTextSize(Appearance.TEXT_SIZE_BUTTON);
        textView.setTypeface(Appearance.TYPEFACE_BUTTON);
    }

    public static Bitmap base64ToBitmap(String base64Data, Context context) {
        return base64ToBitmap(base64Data, context, DisplayMetrics.DENSITY_HIGH);
    }

    public static Bitmap base64ToBitmap(String base64Data, Context context,
                                        int displayMetricsDensity) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (context != null) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            options.inTargetDensity = metrics.densityDpi;
        } else {
            options.inTargetDensity = DisplayMetrics.DENSITY_MEDIUM;
        }
        options.inDensity = displayMetricsDensity;
        options.inScaled = false;

        byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
    }
}
