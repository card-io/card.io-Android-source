package io.card.payment;

/* ClientInfo.java
 * See the file "LICENSE.md" for the full license governing this code.
 */

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

class ClientInfo {
    static final String TAG = "ClientInfo";

    public static String clientVersion(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();

        String versionStr;
        int versionNum;
        try {
            versionNum = context.getPackageManager().getPackageInfo(appInfo.packageName, 0).versionCode;

            versionStr = context.getPackageManager().getPackageInfo(appInfo.packageName, 0).versionName;
        } catch (NameNotFoundException e1) {
            Log.e(TAG, "couldn't get version string", e1);
            versionNum = -1;
            versionStr = "unknown";
        }

        String clientVersion = appInfo.packageName + '/' + versionNum + " (" + versionStr + ')';
        Log.i(TAG, clientVersion);
        return clientVersion;
    }

    public static String clientJailbreakStatus(Context context) {
        String result = "";

        // Mock locations doesn't mean that the location is fake, only that it is possible the user
        // or other apps to set fake locations.
        if (!Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ALLOW_MOCK_LOCATION).equals("0")) {
            result += "mock_locations;";
        }

        if (!Build.TYPE.equalsIgnoreCase("user")) {
            result += "build_type:" + Build.TYPE + ";";
        }

        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            result += "no_gps;";
        }
        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            result += "no_network_location;";
        }

        Log.i(TAG, "ClientCheck: " + result);
        return result;
    }
}
