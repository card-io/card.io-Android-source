package io.card.payment;

/* StringHelper.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

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
}
