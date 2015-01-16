package io.card.payment;

/* Validator.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.text.InputFilter;
import android.text.TextWatcher;

interface Validator extends TextWatcher, InputFilter {
    public String getValue();

    boolean isValid();

    boolean hasFullLength();
}
