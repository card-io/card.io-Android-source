package io.card.payment.i18n;

/* i18nManager.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Manages the internationalization
 *
 * @author jbrateman
 */

public class I18nManager<E extends Enum<?>> {
    private static final String TAG = I18nManager.class.getSimpleName();

    /**
     * Map of incorrect locales that we accept, and their target locale
     */
    private static final Map<String, String> SPECIAL_LOCALE_MAP = new HashMap<String, String>();

    /**
     * Set of all RTL locales
     */
    private static final Set<String> RIGHT_TO_LEFT_LOCALE_SET = new HashSet<String>();

    static {
        SPECIAL_LOCALE_MAP.put("zh_CN", "zh-Hans");
        SPECIAL_LOCALE_MAP.put("zh_TW", "zh-Hant_TW");
        SPECIAL_LOCALE_MAP.put("zh_HK", "zh-Hant");
        SPECIAL_LOCALE_MAP.put("en_UK", "en_GB");
        SPECIAL_LOCALE_MAP.put("en_IE", "en_GB");
        /*
         * Support Hebrew from phone settings (see
         * http://code.google.com/p/android/issues/detail?id=3639)
         */
        SPECIAL_LOCALE_MAP.put("iw_IL", "he");
        SPECIAL_LOCALE_MAP.put("no", "nb");

        RIGHT_TO_LEFT_LOCALE_SET.add("he");
        RIGHT_TO_LEFT_LOCALE_SET.add("ar");
    }

    private Map<String, SupportedLocale<E>> supportedLocales;
    private SupportedLocale<E> currentLocale;
    private Class<E> enumClazz;

    public I18nManager(Class<E> enumClazz, List<SupportedLocale<E>> locales) {
        this.supportedLocales = new LinkedHashMap<String, SupportedLocale<E>>();
        this.enumClazz = enumClazz;

        // add all supported locales
        for (SupportedLocale<E> locale : locales) {
            addLocale(locale);
        }

        // start off with defaults
        setLanguage(null);
    }

    /**
     * Prints out error messages for missing localizations of display messages
     *
     * @param localeName
     */
    private void logMissingLocalizations(String localeName) {
        List<String> errorMessages = getMissingLocaleMessages(localeName);

        for (String errorMessage : errorMessages) {
            Log.i(TAG, errorMessage);
        }
    }

    /**
     * Returns a list of all missing localizations over all locales. Useful for
     * testing.
     *
     * @return
     */
    public List<String> getMissingLocaleMessages() {
        List<String> errorMessages = new ArrayList<String>();
        for (String locale : supportedLocales.keySet()) {
            errorMessages.addAll(getMissingLocaleMessages(locale));
        }

        return errorMessages;
    }

    /**
     * Returns a list of all the missing localizations in the specific locale.
     *
     * @return
     */
    private List<String> getMissingLocaleMessages(String localeName) {
        SupportedLocale<E> locale = supportedLocales.get(localeName);
        List<String> errorMessages = new ArrayList<String>();

        Log.i(TAG, "Checking locale " + localeName);

        for (E key : enumClazz.getEnumConstants()) {
            String prettyKeyValue = "[" + localeName + "," + key + "]";
            // Log.d(TAG, prettyKeyValue);

            if (null == locale.getAdaptedDisplay(key, null)) {
                // Log.e(TAG, "Missing " + prettyKeyValue);
                errorMessages.add("Missing " + prettyKeyValue);
            }
        }

        return errorMessages;
    }

    /**
     * Sets the locale to the given locale specifier if not null. If null or not
     * found, fall back to defaults.
     *
     * @param localeSpecifier
     */
    public void setLanguage(String localeSpecifier) {
        Log.d(TAG, "setLanguage(" + localeSpecifier + ")");

        // reset current locale since we're trying to set it to something new.
        currentLocale = null;

        currentLocale = getLocaleFromSpecifier(localeSpecifier);

        assert currentLocale != null;
        Log.d(TAG, "setting locale to:" + currentLocale.getName());
    }

    /**
     * Returns the SupportedLocale object corresponding to the provided
     * localeSpecifier, if found. If not found, use phone settings, then default
     * to English.
     *
     * @param localeSpecifier
     * @return
     */
    public SupportedLocale<E> getLocaleFromSpecifier(String localeSpecifier) {
        SupportedLocale<E> foundLocale = null;
        if (null != localeSpecifier) {
            foundLocale = lookupSupportedLocale(localeSpecifier);
        }

        if (null == foundLocale) {
            // use default phone
            String phoneLanguage = Locale.getDefault().toString();
            Log.d(TAG, localeSpecifier + " not found.  Attempting to look for " + phoneLanguage);

            foundLocale = lookupSupportedLocale(phoneLanguage);
        }

        if (null == foundLocale) {
            // use english
            Log.d(TAG, "defaulting to english");
            foundLocale = supportedLocales.get("en");
        }

        assert foundLocale != null;
        return foundLocale;
    }

    /**
     * Attempts to look up the locale based on the specifier. These are pretty
     * specific rules, so here's the summarized description from iOS:
     * <p/>
     * Can be specified as a language code ("en", "fr", "zh-Hans", etc.) or as a
     * locale ("en_AU", "fr_FR", "zh-Hant_TW", etc.).
     * <p/>
     * If the library does not contain localized strings for a specified locale,
     * then will fall back to the language.
     * <p/>
     * E.g., "es_CO" -> "es".
     * <p/>
     * If the library does not contain localized strings for a specified region,
     * then will fall back to American English.
     * <p/>
     * If you specify only a language code, and that code matches the device's
     * currently preferred language, then the library will attempt to use the
     * device's current region as well.
     * <p/>
     * E.g., specifying "en" on a device set to "English" and "United Kingdom"
     * will result in "en_GB".
     *
     * @param localeSpecifier
     * @return
     */
    private SupportedLocale<E> lookupSupportedLocale(final String localeSpecifier) {
        if (null == localeSpecifier || localeSpecifier.length() < 2) {
            // not enough info provided
            return null;
        }

        SupportedLocale<E> supportedLocale = null;

        // special cases taken care of first
        if (SPECIAL_LOCALE_MAP.containsKey(localeSpecifier)) {
            String localeToUse = SPECIAL_LOCALE_MAP.get(localeSpecifier);
            supportedLocale = supportedLocales.get(localeToUse);
            Log.d(TAG, "Overriding locale specifier " + localeSpecifier + " with " + localeToUse);
        }

        // First try for <language>_<COUNTRY>:
        if (null == supportedLocale) {
            String language_country;
            if (localeSpecifier.contains("_")) {
                language_country = localeSpecifier;
            } else {
                // append country code to the current localeSpecifier. This handles the case where
                // the locale is set to "en", and country is set to "GB", so we want to set language
                // to "en_GB"
                language_country = localeSpecifier + "_" + Locale.getDefault().getCountry();
            }
            supportedLocale = supportedLocales.get(language_country);
        }

        // Next, fall back to the exact requested locale (specifically handles zh-Hans case):
        if (null == supportedLocale) {
            supportedLocale = supportedLocales.get(localeSpecifier);
        }

        // Next, fall back to just the stripped off <language>:
        if (null == supportedLocale) {
            String languageCode = localeSpecifier.substring(0, 2);
            supportedLocale = supportedLocales.get(languageCode);
        }

        return supportedLocale;
    }

    public String getString(E key) {
        return getString(key, currentLocale);
    }

    public String getString(E key, SupportedLocale<E> localeToTranslate) {
        String countryCode = Locale.getDefault().getCountry().toUpperCase(Locale.US);
        String s = localeToTranslate.getAdaptedDisplay(key, countryCode);

        if (s == null) {
            String errorMessage =
                    "Missing localized string for [" + currentLocale.getName() + ",Key."
                            + key.toString() + "]";
            Log.i(TAG, errorMessage);

            // return what we have in the canonical "en"
            // and if that's missing fake it
            s = supportedLocales.get("en").getAdaptedDisplay(key, countryCode);
        }

        if (s == null) {
            Log.i(TAG, "Missing localized string for [en,Key." + key.toString()
                    + "], so defaulting to keyname");
            s = key.toString();
        }

        // Log.d(TAG, "returning [" + key + "," + s + "]");
        return s;
    }

    public List<String> getSupportedLocales() {
        return new ArrayList<String>(supportedLocales.keySet());
    }

    /**
     * Adds a supported locale
     *
     * @param supportedLocale
     */
    private void addLocale(SupportedLocale<E> supportedLocale) {
        String localeName = supportedLocale.getName();
        if (null == localeName) {
            throw new RuntimeException("Null localeName");
        }
        if (supportedLocales.containsKey(localeName)) {
            throw new RuntimeException("Locale " + localeName + " already added");
        }
        supportedLocales.put(localeName, supportedLocale);

        logMissingLocalizations(localeName);
    }

    /**
     * Returns <code>true</code> if the current locale is right-to-left
     */
    public boolean isCurrentLocaleRightToLeftLang() {
        return RIGHT_TO_LEFT_LOCALE_SET.contains(currentLocale.getName());
    }
}
