package io.card.payment;

/* MaxLengthValidator.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

/**
 * Validates that a field is non-empty, and does not exceed a max value.
 */
class MaxLengthValidator extends NonEmptyValidator implements Validator {

    private int maxLength;

    MaxLengthValidator(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && getValue().length() <= maxLength;
    }

}
