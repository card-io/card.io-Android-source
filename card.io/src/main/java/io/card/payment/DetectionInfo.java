package io.card.payment;

/* DetectionInfo.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

/**
 * This class implements a data structure used to pass card detection details back and forth between
 * java and native code/
 */

class DetectionInfo {
    public boolean complete;
    public boolean topEdge;
    public boolean bottomEdge;
    public boolean leftEdge;
    public boolean rightEdge;
    public float focusScore;
    public int[] prediction;
    public int expiry_month;
    public int expiry_year;
    public CreditCard detectedCard;

    public DetectionInfo() {
        complete = false;
        
        prediction = new int[16];
        prediction[0] = -1;
        prediction[15] = -1;

        detectedCard = new CreditCard();
    }

    ;

    boolean sameEdgesAs(DetectionInfo other) {
        return other.topEdge == this.topEdge && other.bottomEdge == this.bottomEdge
                && other.leftEdge == this.leftEdge && other.rightEdge == this.rightEdge;
    }

    boolean detected() {
        return (topEdge && bottomEdge && rightEdge && leftEdge);
    }

    boolean predicted() {
        return complete;
    }

    CreditCard creditCard() {
        String numberStr = new String();
        for (int i = 0; i < 16 && 0 <= prediction[i] && prediction[i] < 10; i++) {
            numberStr += String.valueOf(prediction[i]);
        }
        detectedCard.cardNumber = numberStr;

        // set these regardless. They'll just be zeroes if not found.
        detectedCard.expiryMonth = expiry_month;
        detectedCard.expiryYear = expiry_year;
        
        return detectedCard;
    }

    int numVisibleEdges() {
        return (topEdge ? 1 : 0) + (bottomEdge ? 1 : 0) + (leftEdge ? 1 : 0) + (rightEdge ? 1 : 0);
    }
}