package io.card.payment;

/* FixedLengthValidator.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.text.Editable;
import android.text.Spanned;

/**
 * Validates that a field is exactly a certain length.
 */
class FixedLengthValidator implements Validator {
    // private final String TAG = this.getClass().getName();

    public int requiredLength;
    private String value;

    public FixedLengthValidator(int length) {
        requiredLength = length;
    }

    @Override
    public void afterTextChanged(Editable s) {
        value = s.toString();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean hasFullLength() {
        return this.isValid();
    }

    @Override
    public boolean isValid() {
        if (value != null && value.length() == requiredLength) {
            // Log.v(TAG, "number has length " + requiredLength);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
                               int dend) {
        if (end > 0 && dest.length() + dend - dstart + end > requiredLength) {
            return "";
        } else {
            return null;
        }
    }

}
