package io.card.payment;

/* CardIONativeLibsConfig.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

/**
 * A config class that is used to specify an alternative search path for the native libraries.
 *
 * Its primary use case is for when the libraries are loaded over the network instead of being
 * shipped with the APK and are stored in the app's data folder.
 */
public class CardIONativeLibsConfig {

    private static String alternativeLibsPath;

    public static void init(String path) {
        alternativeLibsPath = path;
    }

    static String getAlternativeLibsPath() {
        return alternativeLibsPath;
    }
}
