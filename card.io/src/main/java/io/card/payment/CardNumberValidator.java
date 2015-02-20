package io.card.payment;

/* CardNumberValidator.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

class CardNumberValidator implements Validator {
    // private static final String TAG = CardNumberValidator.class.getSimpleName();

    private String numberString;

    final static int[] AMEX_SPACER = { 4, 11 };
    final static int[] NORMAL_SPACER = { 4, 9, 14 };
    private int spacerToDelete = 0;

    public CardNumberValidator() {
    }

    public CardNumberValidator(String number) {
        numberString = number;
    }

    @Override
    public void afterTextChanged(Editable source) {
        // TODO document whatever is going on here
        numberString = StringHelper.getDigitsOnlyString(source.toString());
        CardType type = CardType.fromCardNumber(numberString);

        if (spacerToDelete > 1) {
            int e = spacerToDelete;
            int s = spacerToDelete - 1;
            spacerToDelete = 0;

            if (e > s) {
                source.delete(s, e);
            }
        }

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if ((type.numberLength() == 15 && (i == 4 || i == 11))
                    || ((type.numberLength() == 16 || type.numberLength() == 14) && (i == 4 || i == 9 || i == 14))) {
                if (c != ' ') {
                    source.insert(i, " ");
                }
            } else if (c == ' ') {
                source.delete(i, i + 1);
                i--;
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public boolean hasFullLength() {
        if (TextUtils.isEmpty(numberString)) {
            return false;
        }

        CardType type = CardType.fromCardNumber(numberString);
        return (numberString.length() == type.numberLength());
    }

    @Override
    public boolean isValid() {
        if (!this.hasFullLength()) {
            return false;
        }
        if (!CreditCardNumber.passesLuhnChecksum(numberString)) {
            return false;
        }

        // Log.v(TAG,"card number is valid");
        return true;
    }

    @Override
    public String getValue() {
        return numberString;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
                               int dend) {
        // Log.v(TAG, "filter(source:\"" + source + "\" start:" + start + " end:" + end + " dest:"
        // + dest + " dstart:" + dstart + " dend:" + dend + ")");

        String updated = new SpannableStringBuilder(dest).replace(dstart, dend, source, start, end)
                .toString();
        String updatedDigits = StringHelper.getDigitsOnlyString(updated);
        CardType type = CardType.fromCardNumber(updatedDigits);
        int maxLength = type.numberLength();

        // Log.v(TAG, "updatedDigits:" + updatedDigits + ",cardType:" + type + ",maxLength:"
        // + maxLength);
        if (updatedDigits.length() > maxLength) {
            return "";
        }

        SpannableStringBuilder result = new SpannableStringBuilder(source);

        int[] spacers;
        if (maxLength == 15) {
            spacers = AMEX_SPACER;
        } else {
            spacers = NORMAL_SPACER;
        }

        int replen = dend - dstart;

        for (int i = 0; i < spacers.length; i++) {
            if (source.length() == 0 && dstart == spacers[i] && dest.charAt(dstart) == ' ') {
                spacerToDelete = spacers[i];
            }
            if (dstart - replen <= spacers[i] && dstart + end - replen >= spacers[i]) {
                int loc = spacers[i] - dstart;
                if (loc == end || (0 <= loc && loc < end && result.charAt(loc) != ' ')) {
                    result.insert(loc, " ");
                    // Log.v(TAG, "adding space");

                    end++;
                }
            }
        }

        return result;
    }
}
