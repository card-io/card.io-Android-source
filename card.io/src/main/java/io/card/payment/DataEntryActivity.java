package io.card.payment;

/* DataEntryActivity.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DateKeyListener;
import android.text.method.DigitsKeyListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import io.card.payment.i18n.LocalizedStrings;
import io.card.payment.i18n.StringKey;
import io.card.payment.ui.ActivityHelper;
import io.card.payment.ui.Appearance;
import io.card.payment.ui.ViewUtil;

/**
 * Activity responsible for entry of Expiry, CVV, Postal Code (and card number in manual case).
 *
 * @version 2.0
 */
public final class DataEntryActivity extends Activity implements TextWatcher {

    /**
     * PayPal REST Apis only handle max 20 chars postal code, so we'll do the same here.
     */
    private static final int MAX_POSTAL_CODE_LENGTH = 20;
    private static final String PADDING_DIP = "4dip";
    private static final String LABEL_LEFT_PADDING_DEFAULT = "2dip";
    private static final String LABEL_LEFT_PADDING_HOLO = "12dip";

    private static final String FIELD_HALF_GUTTER = PADDING_DIP;

    private int viewIdCounter = 1;

    private static final int editTextIdBase = 100;

    private int editTextIdCounter = editTextIdBase;

    private TextView activityTitleTextView;
    private EditText numberEdit;
    private Validator numberValidator;
    private EditText expiryEdit;
    private Validator expiryValidator;
    private EditText cvvEdit;
    private Validator cvvValidator;
    private EditText postalCodeEdit;
    private Validator postalCodeValidator;
    private ImageView cardView;
    private Button doneBtn;
    private Button cancelBtn;
    private CreditCard capture;

    private boolean autoAcceptDone;
    private String labelLeftPadding;
    private boolean useApplicationTheme;
    private int defaultTextColor;

    private final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");

        if (null == getIntent().getExtras()) {
            // extras should never be null.  This is some weird android state that we handle by just going back.
            onBackPressed();
            return;
        }

        useApplicationTheme = getIntent().getBooleanExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME, false);
        ActivityHelper.setActivityTheme(this, useApplicationTheme);

        defaultTextColor = new TextView(this).getTextColors().getDefaultColor();

        labelLeftPadding = ActivityHelper.holoSupported() ? LABEL_LEFT_PADDING_HOLO
                : LABEL_LEFT_PADDING_DEFAULT;

        LocalizedStrings.setLanguage(getIntent());

        int paddingPx = ViewUtil.typedDimensionValueToPixelsInt(PADDING_DIP, this);

        RelativeLayout container = new RelativeLayout(this);
        if( !useApplicationTheme ) {
            container.setBackgroundColor(Appearance.DEFAULT_BACKGROUND_COLOR);
        }
        ScrollView scrollView = new ScrollView(this);
        scrollView.setId(viewIdCounter++);
        RelativeLayout.LayoutParams scrollParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        scrollParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        container.addView(scrollView, scrollParams);

        LinearLayout wrapperLayout = new LinearLayout(this);
        wrapperLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(wrapperLayout, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mainParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        capture = getIntent().getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);

        autoAcceptDone = getIntent().getBooleanExtra("debug_autoAcceptResult", false);

        if (capture != null) {
            numberValidator = new CardNumberValidator(capture.cardNumber);

            cardView = new ImageView(this);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            cardView.setPadding(0, 0, 0, paddingPx);
            cardParams.weight = 1;

            // static access is necessary, else we see weird crashes on some devices.
            cardView.setImageBitmap(io.card.payment.CardIOActivity.markedCardImage);

            mainLayout.addView(cardView, cardParams);
            ViewUtil.setMargins(cardView, null, null, null, Appearance.VERTICAL_SPACING);

        } else {

            activityTitleTextView = new TextView(this);
            activityTitleTextView.setTextSize(24);
            if(! useApplicationTheme ) {
                activityTitleTextView.setTextColor(Appearance.PAY_BLUE_COLOR);
            }
            mainLayout.addView(activityTitleTextView);
            ViewUtil.setPadding(activityTitleTextView, null, null, null,
                    Appearance.VERTICAL_SPACING);
            ViewUtil.setDimensions(activityTitleTextView, LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);

            LinearLayout numberLayout = new LinearLayout(this);
            numberLayout.setOrientation(LinearLayout.VERTICAL);
            ViewUtil.setPadding(numberLayout, null, PADDING_DIP, null, PADDING_DIP);

            TextView numberLabel = new TextView(this);
            ViewUtil.setPadding(numberLabel, labelLeftPadding, null, null, null);
            numberLabel.setText(LocalizedStrings.getString(StringKey.ENTRY_CARD_NUMBER));
            if(! useApplicationTheme ) {
                numberLabel.setTextColor(Appearance.TEXT_COLOR_LABEL);
            }
            numberLayout.addView(numberLabel, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

            numberEdit = new EditText(this);
            numberEdit.setId(editTextIdCounter++);
            numberEdit.setMaxLines(1);
            numberEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
            numberEdit.setTextAppearance(getApplicationContext(),
                    android.R.attr.textAppearanceLarge);
            numberEdit.setInputType(InputType.TYPE_CLASS_PHONE);
            numberEdit.setHint("1234 5678 1234 5678");

            numberValidator = new CardNumberValidator();
            numberEdit.addTextChangedListener(numberValidator);
            numberEdit.addTextChangedListener(this);
            numberEdit.setFilters(new InputFilter[] { new DigitsKeyListener(), numberValidator });

            numberLayout.addView(numberEdit, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            mainLayout.addView(numberLayout, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }

        LinearLayout optionLayout = new LinearLayout(this);
        LinearLayout.LayoutParams optionLayoutParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ViewUtil.setPadding(optionLayout, null, PADDING_DIP, null, null);
        optionLayout.setOrientation(LinearLayout.HORIZONTAL);

        boolean requireExpiry = getIntent().getBooleanExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false);
        boolean requireCVV = getIntent().getBooleanExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false);
        boolean requirePostalCode = getIntent().getBooleanExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false);

        if (requireExpiry) {
            LinearLayout expiryLayout = new LinearLayout(this);
            LinearLayout.LayoutParams expiryLayoutParam = new LinearLayout.LayoutParams(0,
                    LayoutParams.MATCH_PARENT, 1);
            expiryLayout.setOrientation(LinearLayout.VERTICAL);

            TextView expiryLabel = new TextView(this);
            if(! useApplicationTheme ) {
                expiryLabel.setTextColor(Appearance.TEXT_COLOR_LABEL);
            }
            expiryLabel.setText(LocalizedStrings.getString(StringKey.ENTRY_EXPIRES));
            ViewUtil.setPadding(expiryLabel, labelLeftPadding, null, null, null);

            expiryLayout.addView(expiryLabel, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

            expiryEdit = new EditText(this);
            expiryEdit.setId(editTextIdCounter++);
            expiryEdit.setMaxLines(1);
            expiryEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
            expiryEdit.setTextAppearance(getApplicationContext(),
                    android.R.attr.textAppearanceLarge);
            expiryEdit.setInputType(InputType.TYPE_CLASS_PHONE);
            expiryEdit.setHint(LocalizedStrings.getString(StringKey.EXPIRES_PLACEHOLDER));

            if (capture != null) {
                expiryValidator = new ExpiryValidator(capture.expiryMonth, capture.expiryYear);
            } else {
                expiryValidator = new ExpiryValidator();
            }
            if (expiryValidator.hasFullLength()) {
                expiryEdit.setText(expiryValidator.getValue());
            }
            expiryEdit.addTextChangedListener(expiryValidator);
            expiryEdit.addTextChangedListener(this);
            expiryEdit.setFilters(new InputFilter[] { new DateKeyListener(), expiryValidator });

            expiryLayout.addView(expiryEdit, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            optionLayout.addView(expiryLayout, expiryLayoutParam);
            ViewUtil.setMargins(expiryLayout, null, null,
                    (requireCVV || requirePostalCode) ? FIELD_HALF_GUTTER : null, null);
        } else {
            expiryValidator = new AlwaysValid();
        }

        if (requireCVV) {
            LinearLayout cvvLayout = new LinearLayout(this);
            LinearLayout.LayoutParams cvvLayoutParam = new LinearLayout.LayoutParams(0,
                    LayoutParams.MATCH_PARENT, 1);
            cvvLayout.setOrientation(LinearLayout.VERTICAL);

            TextView cvvLabel = new TextView(this);
            if(! useApplicationTheme ) {
                cvvLabel.setTextColor(Appearance.TEXT_COLOR_LABEL);
            }
            ViewUtil.setPadding(cvvLabel, labelLeftPadding, null, null, null);
            cvvLabel.setText(LocalizedStrings.getString(StringKey.ENTRY_CVV));

            cvvLayout.addView(cvvLabel, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

            cvvEdit = new EditText(this);
            cvvEdit.setId(editTextIdCounter++);
            cvvEdit.setMaxLines(1);
            cvvEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
            cvvEdit.setTextAppearance(getApplicationContext(), android.R.attr.textAppearanceLarge);
            cvvEdit.setInputType(InputType.TYPE_CLASS_PHONE);
            cvvEdit.setHint("123");

            int length = 4;
            if (capture != null) {
                CardType type = CardType.fromCardNumber(numberValidator.getValue());
                length = type.cvvLength();
            }
            cvvValidator = new FixedLengthValidator(length);
            cvvEdit.setFilters(new InputFilter[] { new DigitsKeyListener(), cvvValidator });
            cvvEdit.addTextChangedListener(cvvValidator);
            cvvEdit.addTextChangedListener(this);

            cvvLayout.addView(cvvEdit, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            optionLayout.addView(cvvLayout, cvvLayoutParam);
            ViewUtil.setMargins(cvvLayout, requireExpiry ? FIELD_HALF_GUTTER : null, null,
                    requirePostalCode ? FIELD_HALF_GUTTER : null, null);
        } else {
            cvvValidator = new AlwaysValid();
        }

        if (requirePostalCode) {
            LinearLayout postalCodeLayout = new LinearLayout(this);
            LinearLayout.LayoutParams postalCodeLayoutParam = new LinearLayout.LayoutParams(0,
                    LayoutParams.MATCH_PARENT, 1);
            postalCodeLayout.setOrientation(LinearLayout.VERTICAL);

            TextView zipLabel = new TextView(this);
            if(! useApplicationTheme ) {
                zipLabel.setTextColor(Appearance.TEXT_COLOR_LABEL);
            }
            ViewUtil.setPadding(zipLabel, labelLeftPadding, null, null, null);
            zipLabel.setText(LocalizedStrings.getString(StringKey.ENTRY_POSTAL_CODE));

            postalCodeLayout
                    .addView(zipLabel, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

            postalCodeEdit = new EditText(this);
            postalCodeEdit.setId(editTextIdCounter++);
            postalCodeEdit.setMaxLines(1);
            postalCodeEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
            postalCodeEdit.setTextAppearance(getApplicationContext(),
                    android.R.attr.textAppearanceLarge);
            postalCodeEdit.setInputType(InputType.TYPE_CLASS_TEXT);

            postalCodeValidator = new MaxLengthValidator(MAX_POSTAL_CODE_LENGTH);
            postalCodeEdit.addTextChangedListener(postalCodeValidator);
            postalCodeEdit.addTextChangedListener(this);

            postalCodeLayout.addView(postalCodeEdit, LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            optionLayout.addView(postalCodeLayout, postalCodeLayoutParam);
            ViewUtil.setMargins(postalCodeLayout, (requireExpiry || requireCVV) ? FIELD_HALF_GUTTER
                    : null, null, null, null);
        } else {
            postalCodeValidator = new AlwaysValid();
        }

        mainLayout.addView(optionLayout, optionLayoutParam);
        wrapperLayout.addView(mainLayout, mainParams);
        ViewUtil.setMargins(mainLayout, Appearance.CONTAINER_MARGIN_HORIZONTAL,
                Appearance.CONTAINER_MARGIN_VERTICAL, Appearance.CONTAINER_MARGIN_HORIZONTAL,
                Appearance.CONTAINER_MARGIN_VERTICAL);

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setId(viewIdCounter++);
        RelativeLayout.LayoutParams buttonLayoutParam = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        buttonLayoutParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonLayout.setPadding(0, paddingPx, 0, 0);
        buttonLayout.setBackgroundColor(Color.TRANSPARENT);

        scrollParams.addRule(RelativeLayout.ABOVE, buttonLayout.getId());

        doneBtn = new Button(this);
        LinearLayout.LayoutParams doneParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1);

        doneBtn.setText(LocalizedStrings.getString(StringKey.DONE));
        doneBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                completed();
            }
        });

        doneBtn.setEnabled(false);

        buttonLayout.addView(doneBtn, doneParam);
        ViewUtil.styleAsButton(doneBtn, true, this);
        ViewUtil.setPadding(doneBtn, "5dip", null, "5dip", null);
        ViewUtil.setMargins(doneBtn, "8dip", "8dip", "4dip", "8dip");
        doneBtn.setTextSize(Appearance.TEXT_SIZE_MEDIUM_BUTTON);

        cancelBtn = new Button(this);

        LinearLayout.LayoutParams cancelParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1);
        cancelBtn.setText(LocalizedStrings.getString(StringKey.CANCEL));

        cancelBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataEntryActivity.this.setResult(CardIOActivity.RESULT_ENTRY_CANCELED);
                finish();
            }
        });

        buttonLayout.addView(cancelBtn, cancelParam);
        ViewUtil.styleAsButton(cancelBtn, false, this);
        ViewUtil.setPadding(cancelBtn, "5dip", null, "5dip", null);
        ViewUtil.setMargins(cancelBtn, "4dip", "8dip", "8dip", "8dip");
        cancelBtn.setTextSize(Appearance.TEXT_SIZE_MEDIUM_BUTTON);

        container.addView(buttonLayout, buttonLayoutParam);

        ActivityHelper.addActionBarIfSupported(this);

        setContentView(container);

        Drawable icon = null;
        boolean usePayPalActionBarIcon = getIntent().getBooleanExtra(CardIOActivity.EXTRA_USE_PAYPAL_ACTIONBAR_ICON, true);
        if (usePayPalActionBarIcon) {
            Bitmap bitmap = ViewUtil.base64ToBitmap(Base64EncodedImages.paypal_monogram_actionbar_icon, this,
                    DisplayMetrics.DENSITY_HIGH);
            icon = new BitmapDrawable(this.getResources(), bitmap);
        }

        // update UI to reflect expiry validness
        if(requireExpiry && expiryValidator.isValid()){
            afterTextChanged(expiryEdit.getEditableText());
        }

        ActivityHelper.setupActionBarIfSupported(this, activityTitleTextView,
                LocalizedStrings.getString(StringKey.MANUAL_ENTRY_TITLE), "card.io - ", icon);
    }

    private void completed() {
        if (capture == null) {
            capture = new CreditCard();
        }
        if (expiryEdit != null) {
            capture.expiryMonth = ((ExpiryValidator) expiryValidator).month;
            capture.expiryYear = ((ExpiryValidator) expiryValidator).year;
        }

        CreditCard result = new CreditCard(numberValidator.getValue(), capture.expiryMonth,
                capture.expiryYear, cvvValidator.getValue(), postalCodeValidator.getValue());
        Intent completion = new Intent();
        completion.putExtra(CardIOActivity.EXTRA_SCAN_RESULT, result);
        DataEntryActivity.this.setResult(CardIOActivity.RESULT_CARD_INFO, completion);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActivityHelper.setFlagSecure(this);

        validateAndEnableDoneButtonIfValid();

        if (numberEdit == null && expiryEdit != null && !expiryValidator.isValid()) {
            expiryEdit.requestFocus();
        } else {
            advanceToNextEmptyField();
        }

        if (numberEdit != null || expiryEdit != null || cvvEdit != null || postalCodeEdit != null) {
            getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        Log.i(TAG, "ready for manual entry"); // used by tests. don't delete.
    }

    private EditText advanceToNextEmptyField() {
        int viewId = editTextIdBase;
        EditText et;
        while ((et = (EditText) findViewById(viewId++)) != null) {
            if (et.getText().length() == 0) {
                if (et.requestFocus()) {
                    return et;
                }
            }
        }
        // all fields have content
        return null;
    }

    private void validateAndEnableDoneButtonIfValid() {
        doneBtn.setEnabled(numberValidator.isValid() && expiryValidator.isValid()
                && cvvValidator.isValid() && postalCodeValidator.isValid());

        Log.d(TAG, "setting doneBtn.enabled=" + doneBtn.isEnabled());

        if (autoAcceptDone && numberValidator.isValid() && expiryValidator.isValid()
                && cvvValidator.isValid() && postalCodeValidator.isValid()) {
            completed();
        }
    }

    @Override
    public void afterTextChanged(Editable et) {

        if (numberEdit != null && et == numberEdit.getText()) {
            if (numberValidator.hasFullLength()) {
                if (!numberValidator.isValid()) {
                    numberEdit.setTextColor(Appearance.TEXT_COLOR_ERROR);
                } else {
                    setDefaultColor(numberEdit);
                    advanceToNextEmptyField();
                }
            } else {
                setDefaultColor(numberEdit);
            }

            if (cvvEdit != null) {
                CardType type = CardType.fromCardNumber(numberValidator.getValue().toString());
                FixedLengthValidator v = (FixedLengthValidator) cvvValidator;
                int length = type.cvvLength();
                v.requiredLength = length;
                cvvEdit.setHint(length == 4 ? "1234" : "123");
            }
        } else if (expiryEdit != null && et == expiryEdit.getText()) {
            if (expiryValidator.hasFullLength()) {
                if (!expiryValidator.isValid()) {
                    expiryEdit.setTextColor(Appearance.TEXT_COLOR_ERROR);
                } else {
                    setDefaultColor(expiryEdit);
                    advanceToNextEmptyField();
                }
            } else {
                setDefaultColor(expiryEdit);
            }
        } else if (cvvEdit != null && et == cvvEdit.getText()) {
            if (cvvValidator.hasFullLength()) {
                if (!cvvValidator.isValid()) {
                    cvvEdit.setTextColor(Appearance.TEXT_COLOR_ERROR);
                } else {
                    setDefaultColor(cvvEdit);
                    advanceToNextEmptyField();
                }
            } else {
                setDefaultColor(cvvEdit);
            }
        } else if (postalCodeEdit != null && et == postalCodeEdit.getText()) {
            if (postalCodeValidator.hasFullLength()) {
                if (!postalCodeValidator.isValid()) {
                    postalCodeEdit.setTextColor(Appearance.TEXT_COLOR_ERROR);
                } else {
                    setDefaultColor(postalCodeEdit);
                    advanceToNextEmptyField();
                }
            } else {
                setDefaultColor(postalCodeEdit);
            }
        }

        this.validateAndEnableDoneButtonIfValid();
    }

    private void setDefaultColor(EditText editText) {
        if (useApplicationTheme) {
            editText.setTextColor(defaultTextColor);
        } else {
            editText.setTextColor(Appearance.TEXT_COLOR_EDIT_TEXT);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // leave empty
    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // leave empty

    }
}
