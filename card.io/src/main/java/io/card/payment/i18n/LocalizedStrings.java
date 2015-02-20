package io.card.payment.i18n;

/* LocalizedStrings.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.content.Intent;

import java.util.Collection;

import io.card.payment.CardIOActivity;
import io.card.payment.i18n.locales.LocalizedStringsList;

/**
 * String constants available for localizing. Public class because of tests.
 */
public final class LocalizedStrings {
    /**
     * Returns the localized message to display for the given key.
     *
     * @param key - which UI string
     * @return localized version of this string
     */
    public static String getString(StringKey key) {
        return i18nManager.getString(key);
    }

    /**
     * Returns the localized message to display for the given key, and given localization. This
     * method is only for static use by implementing apps/libraries, and should only be used to
     * override any previous locale that could have possibly been set (or cleared).
     *
     * @param key The UI string key
     * @param languageOrLocale the target translation locale
     * @return localized version of this string
     */
    public static String getString(StringKey key, String languageOrLocale) {
        return i18nManager.getString(key, i18nManager.getLocaleFromSpecifier(languageOrLocale));
    }

    /**
     * Sets the language for an activity. Should be called in onCreate, before any possible messages
     * are generated.
     *
     * @param intent
     */
    public static void setLanguage(Intent intent) {
        i18nManager.setLanguage(intent.getStringExtra(CardIOActivity.EXTRA_LANGUAGE_OR_LOCALE));
    }

    private static final I18nManager<StringKey> i18nManager;

    static {
        /**
         * TODO optimize this for lazy loading. Should be relatively easy.
         */
        i18nManager = new I18nManager<StringKey>(StringKey.class, LocalizedStringsList.ALL_LOCALES);
    }

    /**
     * Return the missing locale messages for testing
     *
     * @return
     */
    static Collection<String> getMissingLocaleMessages() {
        return i18nManager.getMissingLocaleMessages();
    }
}
