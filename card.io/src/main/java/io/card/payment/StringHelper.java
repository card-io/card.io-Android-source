package io.card.payment;

/* StringHelper.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import java.text.SimpleDateFormat;
import java.util.Date;

class StringHelper {
    public static String getDigitsOnlyString(String numString) {
        StringBuilder sb = new StringBuilder();
        for (char c : numString.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static int getNumDigits(String numString) {
        int result = 0;
        for (char c : numString.toCharArray()) {
            if (Character.isDigit(c)) {
                result++;
            }
        }
        return result;
    }

    public static String getFormattedDate(Date date) {
        return new SimpleDateFormat("MM/yy").format(date);
    }
}
