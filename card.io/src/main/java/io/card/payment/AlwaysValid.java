package io.card.payment;

/* AlwaysValid.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.text.Editable;
import android.text.Spanned;

class AlwaysValid implements Validator {
    private String placeholder;

    public AlwaysValid(String value) {
        placeholder = value;
    }

    public AlwaysValid() {
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getValue() {
        return placeholder;
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
                               int dend) {
        return null;
    }

    @Override
    public boolean hasFullLength() {
        return true;
    }

}
