package io.card.payment;

/* CardType.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import io.card.payment.i18n.LocalizedStrings;
import io.card.payment.i18n.StringKey;
import io.card.payment.ui.ViewUtil;

/**
 * Enumerates each supported card type. see http://en.wikipedia.org/wiki/Bank_card_number for more
 * details.
 *
 * @version 1.0
 */
public enum CardType {
    /**
     * American Express cards start in 34 or 37
     */
    AMEX("AmEx"),
    /**
     * Diners Club
     */
    DINERSCLUB("DinersClub"),
    /**
     * Discover starts with 6x for some values of x.
     */
    DISCOVER("Discover"),
    /**
     * JCB (see http://www.jcbusa.com/) cards start with 35
     */
    JCB("JCB"),
    /**
     * Mastercard starts with 51-55
     */
    MASTERCARD("MasterCard"),
    /**
     * Visa starts with 4
     */
    VISA("Visa"),
    /**
     * Maestro
     */
    MAESTRO("Maestro"),
    /**
     * Unknown card type.
     */
    UNKNOWN("Unknown"),
    /**
     * Not enough information given.
     * <p/>
     * More digits are required to know the card type. (e.g. all we have is a 3, so we don't know if
     * it's JCB or AmEx)
     */
    INSUFFICIENT_DIGITS("More digits required");

    public final String name;

    private static int minDigits = 1;

    private CardType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Convenience method to return a CardType string (e.g. "Visa", "American Express", "JCB",
     * "MasterCard", or "Discover") suitable for display. This string will be translated into the
     * language specified. See {@link CardIOActivity#EXTRA_LANGUAGE_OR_LOCALE} for a detailed
     * explanation of languageOrLocale.
     *
     * @param languageOrLocale See {@link CardIOActivity#EXTRA_LANGUAGE_OR_LOCALE}.
     */
    public String getDisplayName(String languageOrLocale) {
        switch (this) {
            case AMEX:
                return LocalizedStrings.getString(StringKey.CARDTYPE_AMERICANEXPRESS, languageOrLocale);
            case DINERSCLUB:
            case DISCOVER:
                return LocalizedStrings.getString(StringKey.CARDTYPE_DISCOVER, languageOrLocale);
            case JCB:
                return LocalizedStrings.getString(StringKey.CARDTYPE_JCB, languageOrLocale);
            case MASTERCARD:
                return LocalizedStrings.getString(StringKey.CARDTYPE_MASTERCARD, languageOrLocale);
            case VISA:
                return LocalizedStrings.getString(StringKey.CARDTYPE_VISA, languageOrLocale);
            default:
                break;
        }

        return null;
    }

    /**
     * @return 15 for AmEx, -1 for unknown, 16 for others.
     */
    public int numberLength() {
        int result = -1;
        switch (this) {
            case AMEX:
                result = 15;
                break;
            case JCB:
            case MASTERCARD:
            case VISA:
            case DISCOVER:
                result = 16;
                break;
            case DINERSCLUB:
                result = 14;
                break;
            case INSUFFICIENT_DIGITS:
                // this represents the maximum number of digits before we can know the card type
                result = minDigits;
                break;
            case UNKNOWN:
            default:
                result = -1;
                break;
        }
        return result;
    }

    /**
     * @return 4 for Amex, 3 for others, -1 for unknown
     */
    public int cvvLength() {
        int result = -1;
        switch (this) {
            case AMEX:
                result = 4;
                break;
            case JCB:
            case MASTERCARD:
            case VISA:
            case DISCOVER:
            case DINERSCLUB:
                result = 3;
                break;
            case UNKNOWN:
            default:
                result = -1;
                break;
        }

        return result;
    }

    /**
     * Returns the {@link Bitmap} of the card logo (e.g. Visa, MC, etc.), if known. Otherwise, returns null.
     * <p/>
     * Returned bitmap is suitable for display with a masked card number, for example, to indicate a user's chosen
     * card.
     *
     * @param context For retrieving the image density
     */
    public Bitmap imageBitmap(Context context) {
        String cardImageData = null;
        switch (this) {
            case AMEX: {
                cardImageData = Base64EncodedImages.paypal_sdk_icon_amex_large;
                break;
            }
            case VISA: {
                cardImageData = Base64EncodedImages.paypal_sdk_icon_visa_large;
                break;
            }
            case MASTERCARD: {
                cardImageData = Base64EncodedImages.paypal_sdk_icon_mastercard_large;
                break;
            }
            case DISCOVER:
            case DINERSCLUB: {
                cardImageData = Base64EncodedImages.paypal_sdk_icon_discover;
                break;
            }
            case JCB: {
                cardImageData = Base64EncodedImages.paypal_sdk_icon_jcb_large;
                break;
            }
            default: {
                // use generic cc image by default? nah, because if it's not one of the above, it's not
                // valid.
                // cardImageData = Base64EncodedImages.paypal_sdk_icon_jcb_large;
                break;
            }
        }

        if (null != cardImageData) {
            return ViewUtil.base64ToBitmap(cardImageData, context);
        }

        return null;
    }

    /**
     * Determine if a number matches a prefix interval
     *
     * @param number credit card number
     * @param intervalStart prefix (e.g. "4") or prefix interval start (e.g. "51")
     * @param intervalEnd prefix interval end (e.g. "55") or null for non-intervals
     * @return -1 for insufficient digits, 0 for no, 1 for yes.
     */
    private static boolean isNumberInInterval(String number, String intervalStart,
                                              String intervalEnd) {
        // Log.d("CardType", "numberInInterval(number:" + number + ",intervalStart:" + intervalStart
        // + ",intervalEnd:" + intervalEnd + ")");

        int numCompareStart = Math.min(number.length(), intervalStart.length());
        int numCompareEnd = Math.min(number.length(), intervalEnd.length());

        if (Integer.parseInt(number.substring(0, numCompareStart)) < Integer.parseInt(intervalStart
                .substring(0, numCompareStart))) {
            // number is too low
            return false;
        } else if (Integer.parseInt(number.substring(0, numCompareEnd)) > Integer
                .parseInt(intervalEnd.substring(0, numCompareEnd))) {
            // number is too high
            return false;
        }

        return true;
    }

    private static HashMap<Pair<String, String>, CardType> intervalLookup;

    static {
        // initialize
        intervalLookup = new HashMap<Pair<String, String>, CardType>();
        intervalLookup.put(getNewPair("300", "305"), CardType.DINERSCLUB);      // Diners Club (Discover)
        intervalLookup.put(getNewPair("309", null), CardType.DINERSCLUB);       // Diners Club (Discover)
        intervalLookup.put(getNewPair("34", null), CardType.AMEX);              // AmEx
        intervalLookup.put(getNewPair("3528", "3589"), CardType.JCB);           // JCB
        intervalLookup.put(getNewPair("36", null), CardType.DINERSCLUB);        // Diners Club (Discover)
        intervalLookup.put(getNewPair("37", null), CardType.AMEX);              // AmEx
        intervalLookup.put(getNewPair("38", "39"), CardType.DINERSCLUB);        // Diners Club (Discover)
        intervalLookup.put(getNewPair("4", null), CardType.VISA);               // Visa
        intervalLookup.put(getNewPair("50", null), CardType.MAESTRO);           // Maestro
        intervalLookup.put(getNewPair("51", "55"), CardType.MASTERCARD);        // MasterCard
        intervalLookup.put(getNewPair("56", "59"), CardType.MAESTRO);           // Maestro
        intervalLookup.put(getNewPair("6011", null), CardType.DISCOVER);        // Discover
        intervalLookup.put(getNewPair("61", null), CardType.MAESTRO);           // Maestro
        intervalLookup.put(getNewPair("62", null), CardType.DISCOVER);          // China UnionPay (Discover)
        intervalLookup.put(getNewPair("63", null), CardType.MAESTRO);           // Maestro
        intervalLookup.put(getNewPair("644", "649"), CardType.DISCOVER);        // Discover
        intervalLookup.put(getNewPair("65", null), CardType.DISCOVER);          // Discover
        intervalLookup.put(getNewPair("66", "69"), CardType.MAESTRO);           // Maestro
        intervalLookup.put(getNewPair("88", null), CardType.DISCOVER);          // China UnionPay (Discover)

        for (Entry<Pair<String, String>, CardType> entry : getIntervalLookup().entrySet()) {
            minDigits = Math.max(minDigits, entry.getKey().first.length());
            if (entry.getKey().second != null) {
                minDigits = Math.max(minDigits, entry.getKey().second.length());
            }
        }
    }

    private static HashMap<Pair<String, String>, CardType> getIntervalLookup() {
        return intervalLookup;
    }

    private static Pair<String, String> getNewPair(String intervalStart, String intervalEnd) {
        if (intervalEnd == null) {
            // set intervalEnd to intervalStart before creating the Pair object, because apparently
            // Pair.hashCode() can't handle nulls on some devices/versions. WTF.
            intervalEnd = intervalStart;
        }
        return new Pair<String, String>(intervalStart, intervalEnd);
    }

    /**
     * Infer the card type from a string.
     *
     * @param typeStr
     * @return matched real type
     */
    public static CardType fromString(String typeStr) {
        if (typeStr == null) {
            return CardType.UNKNOWN;
        }

        for (CardType type : CardType.values()) {
            if (type == CardType.UNKNOWN || type == CardType.INSUFFICIENT_DIGITS) {
                continue;
            }

            if (typeStr.equalsIgnoreCase(type.toString())) {
                return type;
            }
        }
        return CardType.UNKNOWN;
    }

    /**
     * Infer the CardType from the number string. See http://en.wikipedia.org/wiki/Bank_card_number
     * for these ranges (last checked: 19 Feb 2013)
     *
     * @param numStr A string containing only the card number.
     * @return Inferred card type
     */
    public static CardType fromCardNumber(String numStr) {
        if (TextUtils.isEmpty(numStr)) {
            return CardType.UNKNOWN;
        }

        HashSet<CardType> possibleCardTypes = new HashSet<CardType>();
        for (Entry<Pair<String, String>, CardType> entry : getIntervalLookup().entrySet()) {
            boolean isPossibleCard = isNumberInInterval(numStr, entry.getKey().first,
                    entry.getKey().second);
            if (isPossibleCard) {
                possibleCardTypes.add(entry.getValue());
            }
        }

        if (possibleCardTypes.size() > 1) {
            return CardType.INSUFFICIENT_DIGITS;
        } else if (possibleCardTypes.size() == 1) {
            return possibleCardTypes.iterator().next();
        } else {
            return CardType.UNKNOWN;
        }
    }
}
