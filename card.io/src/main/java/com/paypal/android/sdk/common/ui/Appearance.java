package com.paypal.android.sdk.common.ui;

/* Apperance.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;

/**
 * Appearance constants and utilities
 * 
 */
public class Appearance {

    // Margin + Padding

    public static final String CONTAINER_MARGIN_HORIZONTAL = "16dip";
    public static final String CONTAINER_MARGIN_VERTICAL = "20dip";

    public static final String BASE_SPACING = "4dip";
    public static final String VERTICAL_SPACING = "8dip";
    public static final String BUTTON_GUTTER = "8dip";
    public static final String LIST_PADDING = "12dip";
    
    public static final String CHECKMARK_SIZE = "20dip";
    public static final String CHECKMARK_PADDING = "8dip";

    public static final String BUTTON_HEIGHT = "54dip";
    public static final String SMALL_BUTTON_HEIGHT = "42dip";

    public static final String LABEL_MARGIN = "10dip";

    // States

    public static final int[] BUTTON_STATE_PRESSED = new int[] {
            android.R.attr.state_pressed, android.R.attr.state_enabled };
    public static final int[] BUTTON_STATE_NORMAL = new int[] { android.R.attr.state_enabled };
    public static final int[] BUTTON_STATE_DISABLED = new int[] { -android.R.attr.state_enabled };
    public static final int[] BUTTON_STATE_FOCUSED = new int[] { android.R.attr.state_focused };

    // Colors

    public static final int PAY_BLUE_COLOR = Color.parseColor("#003087");
    public static final int PAY_BLUE_COLOR_OPACITY_66 = Color.parseColor("#aa003087");
    public static final int PAL_BLUE_COLOR = Color.parseColor("#009CDE");
    public static final int PAL_BLUE_COLOR_OPACITY_66 = Color.parseColor("#aa009CDE");

    // Background colors

    public static final Drawable ACTIONBAR_BACKGROUND = new ColorDrawable(
            Color.parseColor("#717074"));
    public static final int DEFAULT_BACKGROUND_COLOR = Color.parseColor("#f5f5f5");
    public static final int ENVIRONMENT_LABEL_COLOR = Color.parseColor("#c4dceb");

    public static final int BUTTON_PRIMARY_NORMAL_COLOR = PAL_BLUE_COLOR;
    public static final int BUTTON_PRIMARY_FOCUS_COLOR = PAL_BLUE_COLOR_OPACITY_66;
    public static final int BUTTON_PRIMARY_PRESSED_COLOR = PAY_BLUE_COLOR;
    public static final int BUTTON_PRIMARY_DISABLED_COLOR = Color.parseColor("#c5ddeb");

    public static final int BUTTON_SECONDARY_NORMAL_COLOR = Color.parseColor("#717074");
    public static final int BUTTON_SECONDARY_FOCUS_COLOR = Color.parseColor("#aa717074");
    public static final int BUTTON_SECONDARY_PRESSED_COLOR = Color.parseColor("#5a5a5d");
    public static final int BUTTON_SECONDARY_DISABLED_COLOR = Color.parseColor("#f5f5f5");

    public static final int SEPARATOR_LINE_COLOR = Color.parseColor("#e5e5e5");

    // Text colors

    public static final int TEXT_COLOR_NORMAL = Color.parseColor("#333333");
    public static final int TEXT_COLOR_LIGHT = Color.parseColor("#515151"); // Style guide says
                                                                            // #5e5e5d, but seems
                                                                            // inconsistent
    public static final int TEXT_COLOR_LIGHTER = Color.parseColor("#797979");
    public static final int TEXT_COLOR_ERROR = Color.parseColor("#b32317");

    public static final int TEXT_COLOR_CONTENT = TEXT_COLOR_LIGHT;
    public static final int TEXT_COLOR_LABEL = TEXT_COLOR_LIGHT;
    public static final int TEXT_COLOR_VALUE = TEXT_COLOR_LIGHT;
    public static final int TEXT_COLOR_BUTTON = Color.WHITE;
    public static final int TEXT_COLOR_TITLE = PAY_BLUE_COLOR;
    public static final int TEXT_COLOR_EDIT = TEXT_COLOR_LIGHT;
    public static final int TEXT_COLOR_FINE_PRINT = TEXT_COLOR_LIGHTER;

    // Text sizes

    public static final float TEXT_SIZE_BUTTON = 20.0f;
    public static final float TEXT_SIZE_MARKETING = 18.0f;
    public static final float TEXT_SIZE_TABLE = 18.0f;
    public static final float TEXT_SIZE_MEDIUM = 16.0f;
    public static final float TEXT_SIZE_SMALL = 14.0f;
    public static final float TEXT_SIZE_FINE_PRINT = 14.0f;
    public static final float TEXT_SIZE_LINK = 14.0f;
    public static final float TEXT_SIZE_MEDIUM_BUTTON = 16.0f;
    public static final float TEXT_SIZE_SMALL_BUTTON = 14.0f;
    public static final float TEXT_SIZE_TINY = 13.0f;

    // Typefaces

    public static final Typeface TYPEFACE_CONTENT = typefaceLight();
    public static final Typeface TYPEFACE_BUTTON = typefaceLight();
    public static final Typeface TYPEFACE_LINK = typefaceLight();
    public static final Typeface TYPEFACE_HEADER = typefaceBold();
    public static final Typeface TYPEFACE_SUB_HEADER = typefaceBoldItalic();
    public static final Typeface TYPEFACE_TABLE_LABEL = typefaceLight();
    public static final Typeface TYPEFACE_TABLE_VALUE = typefaceNormal();
    public static final Typeface TYPEFACE_EDIT = typefaceLight();

    // Focus: For console support, focus is indicated by a box around clickable elements. Other
    // elements must be padded by this amount for proper alignment. The width of the focus box is
    // calculated at half of this padding amount.
    public static final String FOCUS_BORDER_PADDING = "4dip";

    public static final ColorStateList TEXT_COLOR_LINK = new ColorStateList(new int[][] {
            BUTTON_STATE_PRESSED, BUTTON_STATE_NORMAL }, new int[] {
            BUTTON_PRIMARY_PRESSED_COLOR, BUTTON_PRIMARY_NORMAL_COLOR });

    public static Drawable buttonBackgroundPrimary(Context context) {
        StateListDrawable d = new StateListDrawable();
        d.addState(BUTTON_STATE_PRESSED, new ColorDrawable(BUTTON_PRIMARY_PRESSED_COLOR));
        d.addState(BUTTON_STATE_DISABLED, new ColorDrawable(BUTTON_PRIMARY_DISABLED_COLOR));
        d.addState(BUTTON_STATE_FOCUSED, buttonBackgroundPrimaryFocused(context));
        d.addState(BUTTON_STATE_NORMAL, buttonBackgroundPrimaryNormal(context));
        return d;
    }

    private static float getFocusBorderWidthPixels(Context context) {
        float scale = context.getResources().getDisplayMetrics().density;
        float adjustedwidth =
                (ViewUtil.typedDimensionValueToPixels(FOCUS_BORDER_PADDING, context) / 2.0f)
                        * scale;
        return adjustedwidth;
    }

    private static Drawable buttonBackgroundPrimaryNormal(Context context) {
        return buttonNormal(BUTTON_PRIMARY_NORMAL_COLOR, getFocusBorderWidthPixels(context));
    }

    private static Drawable buttonBackgroundPrimaryFocused(Context context) {
        return buttonFocused(
                BUTTON_PRIMARY_NORMAL_COLOR, BUTTON_PRIMARY_FOCUS_COLOR,
                getFocusBorderWidthPixels(context));
    }

    public static Drawable buttonBackgroundSecondary(Context context) {
        StateListDrawable d = new StateListDrawable();
        d.addState(BUTTON_STATE_PRESSED, new ColorDrawable(BUTTON_SECONDARY_PRESSED_COLOR));
        d.addState(BUTTON_STATE_DISABLED, new ColorDrawable(BUTTON_SECONDARY_DISABLED_COLOR));
        d.addState(BUTTON_STATE_FOCUSED, buttonBackgroundSecondaryFocused(context));
        d.addState(BUTTON_STATE_NORMAL, buttonBackgroundSecondaryNormal(context));
        return d;
    }

    private static Drawable buttonBackgroundSecondaryNormal(Context context) {
        return buttonNormal(BUTTON_SECONDARY_NORMAL_COLOR, getFocusBorderWidthPixels(context));
    }

    private static Drawable buttonBackgroundSecondaryFocused(Context context) {
        return buttonFocused(
                BUTTON_SECONDARY_NORMAL_COLOR, BUTTON_SECONDARY_FOCUS_COLOR,
                getFocusBorderWidthPixels(context));
    }

    protected static Drawable linkStates(Context context) {
        StateListDrawable d = new StateListDrawable();
        d.addState(BUTTON_STATE_FOCUSED, linkFocused(context));
        d.addState(BUTTON_STATE_NORMAL, new ColorDrawable(Color.TRANSPARENT));
        return d;
    }

    private static Drawable linkFocused(Context context) {
        return buttonFocused(
                Color.TRANSPARENT, BUTTON_PRIMARY_FOCUS_COLOR, getFocusBorderWidthPixels(context));
    }

    private static Drawable buttonNormal(int color, float width) {
        Drawable[] layers = new Drawable[2];
        layers[0] = new ColorDrawable(color);
        ShapeDrawable s = new ShapeDrawable(new RectShape());
        s.getPaint().setStrokeWidth(2 * width);
        s.getPaint().setStyle(Style.STROKE);
        s.getPaint().setColor(DEFAULT_BACKGROUND_COLOR);
        layers[1] = s;
        LayerDrawable ld = new LayerDrawable(layers);
        return ld;
    }

    private static Drawable buttonFocused(int backgroundColor, int focusBoxColor,
            float scaledBorderWidth) {
        Drawable[] layers = new Drawable[3];
        layers[0] = new ColorDrawable(backgroundColor);

        ShapeDrawable s = new ShapeDrawable(new RectShape());
        s.getPaint().setStrokeWidth(2 * scaledBorderWidth);
        s.getPaint().setStyle(Style.STROKE);
        s.getPaint().setColor(DEFAULT_BACKGROUND_COLOR);
        layers[1] = s;

        ShapeDrawable s2 = new ShapeDrawable(new RectShape());
        s2.getPaint().setStrokeWidth(scaledBorderWidth);
        s2.getPaint().setStyle(Style.STROKE);
        s2.getPaint().setColor(focusBoxColor);
        layers[2] = s2;

        LayerDrawable ld = new LayerDrawable(layers);
        return ld;
    }

    private static Typeface typefaceLight() {
        return Typeface.create("sans-serif-light", Typeface.NORMAL);
    }

    private static Typeface typefaceNormal() {
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }

    private static Typeface typefaceBold() {
        return Typeface.create("sans-serif-bold", Typeface.NORMAL);
    }

    private static Typeface typefaceBoldItalic() {
        return Typeface.create("sans-serif", Typeface.ITALIC);
    }

}
