package io.card.payment;

/* ExpiryValidator.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import java.util.Date;

class ExpiryValidator implements Validator {
    @SuppressWarnings("unused")
    private final String TAG = this.getClass().getName();

    public int month;
    public int year;

    private boolean fullLength;

    public ExpiryValidator() {
    }

    public ExpiryValidator(int m, int y) {
        month = m;
        year = y;

        fullLength = (month > 0 && year > 0);

        // accept 2-digit years.
        if (year < 2000) {
            year += 2000;
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        fullLength = (s.length() >= 5);

        String dateStr = s.toString();
        if (dateStr == null) {
            return;
        }

        Date expiry = CreditCardNumber.getDateForString(dateStr);
        if (expiry == null) {
            return;
        }

        month = expiry.getMonth() + 1; // Java months are 0-11
        year = expiry.getYear();

        if (year < 1900) {
            year += 1900;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        month = 0;
        year = 0;
        fullLength = false;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public String getValue() {
        return String.format("%02d/%02d", month, year % 100);
    }

    @Override
    public boolean hasFullLength() {
        return fullLength;
    }

    @Override
    public boolean isValid() {
        if (month < 1 || 12 < month) {
            return false;
        }

        Date now = new Date();

        if (year > 1900 + now.getYear() + 15) {
            return false;
        }

        return (year > 1900 + now.getYear() || (year == 1900 + now.getYear() && month >= now
                .getMonth() + 1));
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
                               int dend) {

        // Log.v(TAG, "filter( source:"+source
        // + " start:" + start
        // + " end:" + end
        // + " dest:" + dest
        // + " dstart:" + dstart
        // + " dend:" + dend);

        // do all in place edits

        SpannableStringBuilder result = new SpannableStringBuilder(source);

        if (dstart == 0 && result.length() > 0
                && ('1' < result.charAt(0) && result.charAt(0) <= '9')) {
            result.insert(0, "0");
            end++;
        }

        int replen = dend - dstart;
        if (dstart - replen <= 2 && dstart + end - replen >= 2) {
            int loc = 2 - dstart;
            if (loc == end || (0 <= loc && loc < end && result.charAt(loc) != '/')) {
                result.insert(loc, "/");
                end++;
            }
        }

        // look at what the updated string will be

        String updated = new SpannableStringBuilder(dest).replace(dstart, dend, result, start, end)
                .toString();

        if (updated.length() >= 1) {
            if (updated.charAt(0) < '0' || '1' < updated.charAt(0)) {
                return "";
            }
        }

        if (updated.length() >= 2) {
            if (updated.charAt(0) != '0' && updated.charAt(1) > '2') {
                return "";
            }
            if (updated.charAt(0) == '0' && updated.charAt(1) == '0') {
                return "";
            }
        }

        if (updated.length() > 5) {
            return "";
        }

        return result;
    }

}
