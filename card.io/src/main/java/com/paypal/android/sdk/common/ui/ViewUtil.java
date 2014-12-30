package com.paypal.android.sdk.common.ui;

/* ViewUtil.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for altering views.
 *
 */
public class ViewUtil {

    /**
     * Wrapper to only use the deprecated {@link #setBackgroundDrawable} on
     * older systems.
     *
     * @param v
     * @param d
     */
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void setBackground(View v, Drawable d) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            v.setBackground(d);
        } else {
            v.setBackgroundDrawable(d);
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

    /*
     * Pads left and right with proper amount to align with fill width buttons that have focus box
     * support.
     */
    public static void setMarginsForFocusBoxAlignment(View v) {
        ViewUtil.setMargins(
                v, Appearance.FOCUS_BORDER_PADDING, null, Appearance.FOCUS_BORDER_PADDING, null);
    }

    public static void setMarginsForFocusBoxAlignment(View v, String top, String bottom) {
        ViewUtil.setMargins(
                v, Appearance.FOCUS_BORDER_PADDING, top, Appearance.FOCUS_BORDER_PADDING, bottom);
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

    public static void setLayoutGravity(View view, int gravity, float weight) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) params;
            linearParams.gravity = gravity;
            linearParams.weight = weight;
        }
    }

    /**
     * Alter the view's layout dimensions using String values Should be used
     * after the view is already added to a layout.
     *
     * @param view
     * @param width String of dimension + unit, e.g. "2dip"
     * @param height String of dimension + unit, e.g. "2dip"
     *
     * @category layout
     */

    public static void setDimensions(View view, String width, String height) {
        Context context = view.getContext();
        setDimensions(
                view, typedDimensionValueToPixelsInt(width, context),
                typedDimensionValueToPixelsInt(height, context));
    }

    public static void setDimensions(View view, int width, String height) {
        Context context = view.getContext();
        setDimensions(view, width, typedDimensionValueToPixelsInt(height, context));
    }

    public static void setDimensions(View view, String width, int height) {
        Context context = view.getContext();
        setDimensions(view, typedDimensionValueToPixelsInt(width, context), height);
    }

    public static void setDimensions(View view, int width, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        params.height = height;
    }

    public static void styleAsButton(View v, boolean primary, Context context) {
        setDimensions(v, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        setPadding(v, "10dip", "0dip", "10dip", "0dip");
        setBackground(
                v,
                primary ? Appearance.buttonBackgroundPrimary(context) : Appearance
                        .buttonBackgroundSecondary(context));

        v.setFocusable(true);

        v.setMinimumHeight(ViewUtil.typedDimensionValueToPixelsInt(
                Appearance.BUTTON_HEIGHT, context));
        if (v instanceof TextView) {
            styleAsButtonText((TextView) v);
        }
        if (!(v instanceof Button)) {
            v.setClickable(true);
        }
    }

    public static void styleAsButtonText(TextView v) {
        v.setGravity(Gravity.CENTER);
        v.setTextColor(Appearance.TEXT_COLOR_BUTTON);
        v.setTextSize(Appearance.TEXT_SIZE_BUTTON);
        v.setTypeface(Appearance.TYPEFACE_BUTTON);
    }

    public static void styleAsComplianceText(TextView v) {
        v.setTextColor(Appearance.TEXT_COLOR_EDIT);
        v.setLinkTextColor(Appearance.TEXT_COLOR_LINK);
        v.setTypeface(Appearance.TYPEFACE_EDIT);
        v.setTextSize(Appearance.TEXT_SIZE_TINY);
        v.setSingleLine(false);
        v.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public static void styleAsLink(Button b) {
        styleAsLink(b, Gravity.CENTER);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void styleAsLink(Button b, int gravity) {
        setPadding(b, "2dip", "2dip", "2dip", "2dip");
        b.setTypeface(Appearance.TYPEFACE_LINK);
        b.setTextColor(Appearance.TEXT_COLOR_LINK);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            b.setBackgroundDrawable(Appearance.linkStates(b.getContext()));
        } else {
            b.setBackground(Appearance.linkStates(b.getContext()));
        }
        b.setAutoLinkMask(Linkify.ALL);
        b.setTextSize(Appearance.TEXT_SIZE_LINK);
        b.setTextColor(Appearance.TEXT_COLOR_LINK);
        b.setGravity(gravity);
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

    /**
     * Returns a {@linkplain SpannableString} with all text underlined.
     *
     * @param text
     * @return
     */
    public static SpannableString underlinedString(String text) {
        SpannableString content = new SpannableString(text);
        content.setSpan(new UnderlineSpan(), 0, text.length(), 0);
        return content;
    }

    public static void setContainerMargins(View view) {
        setMargins(
                view, Appearance.CONTAINER_MARGIN_HORIZONTAL, Appearance.CONTAINER_MARGIN_VERTICAL,
                Appearance.CONTAINER_MARGIN_HORIZONTAL, Appearance.CONTAINER_MARGIN_VERTICAL);
    }

    public static View addHorizontalSeparatorLine(LinearLayout layoutView) {
        return addHorizontalSeparatorLine(
                layoutView, Appearance.LIST_PADDING, Appearance.LIST_PADDING);
    }

    public static View addHorizontalSeparatorLine(LinearLayout layoutView, String marginTop,
            String marginBottom) {
        View v = new View(layoutView.getContext());
        layoutView.addView(v);
        ViewUtil.setBackground(v, new ColorDrawable(Appearance.SEPARATOR_LINE_COLOR));
        ViewUtil.setDimensions(v, LayoutParams.MATCH_PARENT, "1dip");
        ViewUtil.setMargins(v, null, marginTop, null, marginBottom);
        return v;
    }

    public static LinearLayout getLinearLayoutAsButton(Context context, boolean isPrimary, int id,
            LinearLayout parent) {
        LinearLayout ret;
        ret = new LinearLayout(context);

        if (id != 0) {
            ret.setId(id);
        }
        parent.addView(ret);
        ret.setGravity(Gravity.CENTER);
        ret.setOrientation(LinearLayout.HORIZONTAL);
        ViewUtil.styleAsButton(ret, isPrimary, context);
        ViewUtil.setDimensions(ret, LayoutParams.MATCH_PARENT, "58dip");
        ViewUtil.setMargins(ret, null, null, null, "4dip");

        return ret;
    }
    
    // CREATORS
    public static ViewGroup createScrollView(Context context) {
        ScrollView ret = new ScrollView(context);
        ret.setBackgroundColor(Appearance.DEFAULT_BACKGROUND_COLOR);
        return ret;
    }
    
    public static LinearLayout createMainLayout(ViewGroup parent) {
        LinearLayout mainLayout = new LinearLayout(parent.getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Appearance.DEFAULT_BACKGROUND_COLOR);
        parent.addView(mainLayout);
        ViewUtil.setDimensions(mainLayout, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        return mainLayout;
    }

    /**
     * TODO remove this when 2fa branch is merged - it will no longer be used.
     */
    @Deprecated
    public static LinearLayout createLinearLayout(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        //linearLayout.setBackgroundColor(Appearance.DEFAULT_BACKGROUND_COLOR2);
        ViewUtil.setPadding(linearLayout, "10dip", "14dip", "10dip", "14dip");
        return linearLayout;
    }

    public static LinearLayout createLinearLayout(ViewGroup mainLayout) {
        LinearLayout linearLayout = new LinearLayout(mainLayout.getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        //linearLayout.setBackgroundColor(Appearance.DEFAULT_BACKGROUND_COLOR2);
        ViewUtil.setPadding(linearLayout, "10dip", "14dip", "10dip", "14dip");
        mainLayout.addView(linearLayout, ViewUtil.getLayoutParams());
        return linearLayout;
    }

    public static TextView createTextAsLink(ViewGroup v) {
        TextView link = new TextView(v.getContext());
        v.addView(link);
        ViewUtil.setDimensions(link, "0dp", LayoutParams.WRAP_CONTENT);
        link.setGravity(Gravity.END);
        link.setTextColor(Appearance.PAL_BLUE_COLOR);
        link.setPaintFlags(link.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        //ViewUtil.setLayoutGravity(logoutText, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 1.0f);
        //ViewUtil.setPadding(logoutText, "2dip", "4dip", "2dip", "0dip");
        link.setClickable(true);
        return link;
    }
    
    public static ImageView createIconView(Context context, String encodedImage, String description) {
        ImageView imageView = new ImageView(context);
        imageView.setScaleType(ScaleType.CENTER_INSIDE);
        imageView.setImageBitmap(ViewUtil.base64ToBitmap(encodedImage, context));
        imageView.setAdjustViewBounds(true);
        imageView.setContentDescription(description);
        return imageView;
    }
    
    //--- TEXT STYLE
    public static void styleAsPrimaryText(TextView textView, int gravity) {
        textView.setTextSize(Appearance.TEXT_SIZE_TABLE);
        textView.setTypeface(Appearance.TYPEFACE_HEADER);
        textView.setSingleLine(true);
        textView.setGravity(gravity);
        textView.setTextColor(Appearance.TEXT_COLOR_LIGHT);
    }

    public static void styleAsSubPrimaryText(TextView textView, int gravity) {
        textView.setTextSize(Appearance.TEXT_SIZE_MEDIUM);
        textView.setTypeface(Appearance.TYPEFACE_SUB_HEADER);
        textView.setSingleLine(true);
        textView.setGravity(gravity);
        textView.setTextColor(Appearance.TEXT_COLOR_LIGHT);
    }

    public static void styleAsMinorText(TextView textView, int gravity) {
        textView.setTextSize(Appearance.TEXT_SIZE_SMALL);
        textView.setTypeface(Appearance.TYPEFACE_TABLE_LABEL);
        textView.setSingleLine(true);
        textView.setGravity(gravity);
        textView.setTextColor(Appearance.TEXT_COLOR_LIGHT);
    }
    
    public static void styleAsSubMinorText(TextView textView, int gravity) {
        textView.setTextSize(Appearance.TEXT_SIZE_TINY);
        textView.setTypeface(Appearance.TYPEFACE_TABLE_LABEL);
        textView.setSingleLine(true);
        textView.setGravity(gravity);
        textView.setTextColor(Appearance.TEXT_COLOR_LIGHT);
    }
    
    // HELPER
    public static LayoutParams getLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
    }
    
    //--- RELATIVE LAYOUT HELPERS
    
    public static RelativeLayout.LayoutParams makeRLParams(int w, int h, int rule, int id) {
        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(w, h);
        rlParams.addRule(rule, id);
        return rlParams;
    }
    
    public static RelativeLayout.LayoutParams makeRLParams(Context context, String w, String h, int rule) {
        RelativeLayout.LayoutParams rlParams =
                new RelativeLayout.LayoutParams(typedDimensionValueToPixelsInt(w, context),
                        typedDimensionValueToPixelsInt(h, context));
        rlParams.addRule(rule);
        return rlParams;
    }
    
    public static RelativeLayout.LayoutParams makeRLParams(Context context, String w, String h, int rule, int id) {
        RelativeLayout.LayoutParams rlParams =
                new RelativeLayout.LayoutParams(typedDimensionValueToPixelsInt(w, context),
                        typedDimensionValueToPixelsInt(h, context));
        rlParams.addRule(rule, id);
        return rlParams;
    }
    

}
