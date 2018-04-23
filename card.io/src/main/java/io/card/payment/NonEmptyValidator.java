package io.card.payment;

/* NonEmptyValidator.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.text.Editable;
import android.text.Spanned;

/**
 * Accepts all nonempty (after trimming) values.
 */
class NonEmptyValidator implements Validator {
    private String value;

    @Override
    public void afterTextChanged(Editable s) {
        value = s.toString().trim();
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
        if (value != null && value.length() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
                               int dend) {
        return null;
    }

}
