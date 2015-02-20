package io.card.payment;

/* CreditCardNumber.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import java.text.CharacterIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.Calendar;
import java.util.Date;

class CreditCardNumber {

    /**
     * Checks if the given string represents a number that passes the Luhn Checksum which all valid
     * CCs will pass.
     *
     * @param number
     * @return true if the number does pass the checksum, else false
     */
    public static boolean passesLuhnChecksum(String number) {
        int even = 0;
        int sum = 0;

        final int[][] sums = { { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, { 0, 2, 4, 6, 8, 1, 3, 5, 7, 9 } };

        CharacterIterator iter = new StringCharacterIterator(number);
        for (char c = iter.last(); c != CharacterIterator.DONE; c = iter.previous()) {
            if (!Character.isDigit(c)) {
                return false;
            }
            int cInt = c - '0';
            sum += sums[even++ & 0x1][cInt];
        }
        return sum % 10 == 0;
    }

    /**
     * @param numStr the String of numbers to view
     * @return null if numStr is not formattable by the known formatting rules
     */

    public static String formatString(String numStr) {
        return formatString(numStr, true, null);
    }

    public static String formatString(String numStr, boolean filterDigits, CardType type) {
        String digits;
        if (filterDigits) {
            digits = StringHelper.getDigitsOnlyString(numStr);
        } else {
            digits = numStr;
        }
        if (type == null) {
            type = CardType.fromCardNumber(digits);
        }
        int numLen = type.numberLength();
        if (digits.length() == numLen) {
            if (numLen == 16) {
                return formatSixteenString(digits);
            } else if (numLen == 15) {
                return formatFifteenString(digits);
            }
        }
        return numStr; // at the worst case, pass back what was given
    }

    private static String formatFifteenString(String digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            if (i == 4 || i == 10) {
                sb.append(' ');
            }
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    private static String formatSixteenString(String digits) {
        StringBuilder sb = new StringBuilder();
        {
            for (int i = 0; i < 16; i++) {
                if (i != 0 && i % 4 == 0) {
                    sb.append(' ');// insert every 4th char, except at end
                }
                sb.append(digits.charAt(i));
            }
        }
        return sb.toString();
    }

    public static boolean isDateValid(int expiryMonth, int expiryYear) {
        if (expiryMonth < 1 || 12 < expiryMonth) {
            return false;
        }

        Calendar now = Calendar.getInstance();
        int thisYear = now.get(Calendar.YEAR);
        int thisMonth = now.get(Calendar.MONTH) + 1;

        if (expiryYear < thisYear) {
            return false;
        }
        if (expiryYear == thisYear && expiryMonth < thisMonth) {
            return false;
        }
        if (expiryYear > thisYear + CreditCard.EXPIRY_MAX_FUTURE_YEARS) {
            return false;
        }

        return true;
    }

    public static boolean isDateValid(String dateString) {
        String digitsOnly = StringHelper.getDigitsOnlyString(dateString);
        SimpleDateFormat validDate = getDateFormatForLength(digitsOnly.length());
        if (validDate == null) {
            return false;
        }
        try {
            validDate.setLenient(false);
            Date enteredDate = validDate.parse(digitsOnly);
            return isDateValid(enteredDate.getMonth() + 1, enteredDate.getYear() + 1900);
        } catch (ParseException pe) {
            return false;
        }
    }

    public static SimpleDateFormat getDateFormatForLength(int len) {
        if (len == 4) {
            return new SimpleDateFormat("MMyy");
        } else if (len == 6) {
            return new SimpleDateFormat("MMyyyy");
        } else {
            return null;
        }
    }

    public static Date getDateForString(String dateString) {
        String digitsOnly = StringHelper.getDigitsOnlyString(dateString);
        SimpleDateFormat validDate = getDateFormatForLength(digitsOnly.length());
        if (validDate != null) {
            try {
                validDate.setLenient(false);
                Date date = validDate.parse(digitsOnly);
                return date;
            } catch (ParseException pe) {
                return null;
            }
        }
        return null;
    }
}
