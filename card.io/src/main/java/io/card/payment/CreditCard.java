package io.card.payment;

/* CreditCard.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Describes a credit card within the Card.IO system.
 * 
 * @version 2.0
 */
public class CreditCard implements Parcelable {

    /**
     * Number of years into the future that a card expiration date is considered to be valid.
     */
    public static final int EXPIRY_MAX_FUTURE_YEARS = 15;

    private static final String TAG = CreditCard.class.getSimpleName();

    /**
     * 15 or 16 digit card number. All numbers, no spaces.
     */
    public String cardNumber;

    /**
     * Month in two digit natural form. {January=1, ..., December=12}
     */
    public int expiryMonth = 0;

    /**
     * Four digit year
     */
    public int expiryYear = 0;

    /**
     * Three or four character security code.
     */
    public String cvv;

    /**
     * Billing postal code for card.
     */
    public String postalCode;

    // these should NOT be public
    String scanId;
    boolean flipped = false;
    int yoff;
    int[] xoff;

    // constructors
    public CreditCard() {
        xoff = new int[16];
        scanId = UUID.randomUUID().toString();
    }

    CreditCard(String json) {
        // Note that we ignore offset results returned from the server since we don't really use
        // them.

        // don't log JSON unless necessary -- contains card numbers
        // Log.d(TAG,"parsing JSON: " + json);

        if (json == null || json.length() == 0)
            return;

        try {
            JSONObject jo = new JSONObject(json);

            cardNumber = jo.optString("numbers");
            Log.d(TAG, "- number: " + getRedactedCardNumber());

            JSONArray expo = jo.optJSONArray("expiry");

            if (expo != null && expo.length() == 2) {
                expiryYear = expo.getInt(0);
                Log.d(TAG, "- year: " + expiryYear);

                expiryMonth = expo.getInt(1);
                Log.d(TAG, "- month: " + expiryMonth);
            }

            flipped = jo.optBoolean("is_flipped");
            Log.d(TAG, "- isFlipped: " + flipped);

            scanId = jo.optString("scan_id");
            Log.d(TAG, "- scanId: " + scanId);

            yoff = jo.optInt("y_offset");

            JSONArray ja = jo.optJSONArray("x_offsets");

            xoff = new int[ja.length()];

            for (int i = 0; i < ja.length(); i++) {
                xoff[i] = ja.getInt(i);
            }

        } catch (Exception e) {
            Log.w(TAG, "error parsing credit card response: ", e);
            // the info is read in order of importance, so if something is missing, we're probably
            // ok, but we should check the card number
            if (cardNumber != null
                    && (cardNumber.length() < 15 || !CreditCardNumber
                            .passesLuhnChecksum(cardNumber))) {
                cardNumber = null;
            }
        }
    }

    public CreditCard(String number, int month, int year, String code, String postalCode) {
        this.cardNumber = number;
        this.expiryMonth = month;
        this.expiryYear = year;
        this.cvv = code;
        this.postalCode = postalCode;
    }

    // parcelable
    private CreditCard(Parcel src) {
        cardNumber = src.readString();
        expiryMonth = src.readInt();
        expiryYear = src.readInt();
        cvv = src.readString();
        postalCode = src.readString();
        scanId = src.readString();
        yoff = src.readInt();
        xoff = src.createIntArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public final void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cardNumber);
        dest.writeInt(expiryMonth);
        dest.writeInt(expiryYear);
        dest.writeString(cvv);
        dest.writeString(postalCode);
        dest.writeString(scanId);
        dest.writeInt(yoff);
        dest.writeIntArray(xoff);
    }

    public static final Parcelable.Creator<CreditCard> CREATOR = new Parcelable.Creator<CreditCard>() {

        @Override
        public CreditCard createFromParcel(Parcel source) {
            return new CreditCard(source);
        }

        @Override
        public CreditCard[] newArray(int size) {
            return new CreditCard[size];
        }
    };

    /**
     * @return The last four digits of the card number
     */
    public String getLastFourDigitsOfCardNumber() {
        if (cardNumber != null) {
            int available = Math.min(4, cardNumber.length());
            return cardNumber.substring(cardNumber.length() - available);
        } else {
            return "";
        }
    }

    /**
     * @return The card number string consisting of all but the last four digits replaced with
     *         bullet ('\u2022').
     */
    public String getRedactedCardNumber() {
        if (cardNumber != null) {
            String redacted = "";
            if (cardNumber.length() > 4) {
                redacted += String.format("%" + (cardNumber.length() - 4) + "s", "").replace(' ',
                        '\u2022');
            }
            redacted += getLastFourDigitsOfCardNumber();
            return CreditCardNumber.formatString(redacted, false,
                    CardType.fromCardNumber(cardNumber));
        } else {
            return "";
        }
    }

    /**
     * @return The type of card, detected from the number
     */
    public CardType getCardType() {
        return CardType.fromCardNumber(cardNumber);
    }

    /**
     * @return A string suitable for display, with spaces inserted for readability.
     */
    public String getFormattedCardNumber() {
        return CreditCardNumber.formatString(cardNumber);
    }

    /**
     * @return A list of {@link NameValuePair}s for submitting to the charge server.
     */
    List<NameValuePair> toNameValueList() {
        List<NameValuePair> result = new ArrayList<NameValuePair>();
        result.add(new BasicNameValuePair("card_number", cardNumber));
        if (expiryMonth > 0 && expiryYear > 0) {
            result.add(new BasicNameValuePair("card_exp_month", String.valueOf(expiryMonth)));
            result.add(new BasicNameValuePair("card_exp_year", String.valueOf(expiryYear)));
        }
        if (cvv != null) {
            result.add(new BasicNameValuePair("card_cvv", cvv));
        }
        if (postalCode != null) {
            result.add(new BasicNameValuePair("card_postal_code", postalCode));
        }
        if (scanId != null) {
            result.add(new BasicNameValuePair("scan_id", scanId));
        }

        return result;
    }

    /**
     * @return true indicates a current, valid date.
     */
    public boolean isExpiryValid() {
        return CreditCardNumber.isDateValid(expiryMonth, expiryYear);
    }

    boolean failed() {
        return (cardNumber == null || cardNumber.length() == 0);
    }

    /**
     * @return a string suitable for writing to a log. Should not be displayed to the user.
     */
    @Override
    public String toString() {
        String s = "{" + getCardType() + ": " + getRedactedCardNumber();
        if (expiryMonth > 0 || expiryYear > 0) {
            s += "  expiry:" + expiryMonth + "/" + expiryYear;
        }
        if (postalCode != null) {
            s += "  postalCode:" + postalCode;
        }
        if (cvv != null) {
            s += "  cvvLength:" + ((cvv != null) ? cvv.length() : 0);
        }
        s += "}";
        return s;
    }
}
