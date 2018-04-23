package io.card.payment.i18n;

/* SupportedLocale.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

/**
 * A locale supported by this particular sdk or library.
 */
public interface SupportedLocale<E extends Enum<?>> {
    /**
     * Returns the name of this locale
     *
     * @return
     */
    String getName();

    /**
     * Returns a country-adapted string translation of the given key. If no
     * adaptation is available, or no country is provided, the default
     * translation for the given key is returned.
     *
     * @param key
     * @param country (Optional)
     * @return
     */
    String getAdaptedDisplay(E key, String country);
}
