package com.asus.filemanager.utility;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Utility {

    /**
     * Returns true if Monkey is running.
     */
    public static boolean isMonkeyRunning() {
        return (ActivityManager.isUserAMonkey()
                || reflectionApis.getSystemPropertyBoolean("debug.monkey", false));
    }

    public static int getPackageVersionCode(Context context, String packageName) {
        int versionCode = 0;
        try {
            versionCode = context.getPackageManager().getPackageInfo(packageName,
                    PackageManager.PERMISSION_GRANTED).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    public static boolean isEnabledAndInstalledPackage(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).enabled;
        } catch (NameNotFoundException e) {
        }
        return false;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }

}
