package com.asus.filemanager.hiddenzone.encrypt;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.preference.PreferenceManager;

public class FingerprintUtils {
    private static final String KEY_ALLOW_FINGERPRINT_TO_UNLOCK = "allow_fingerprint_to_unlock";

    private static FingerprintManager getFingerprintManager(Context context) {
        return context.getSystemService(FingerprintManager.class);
    }

    private static boolean isRunningOnMarshmallowOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static boolean isSupportFingerprintFeature(Context context) {
        if (!isRunningOnMarshmallowOrHigher()) {
            return false;
        }
        return getFingerprintManager(context).isHardwareDetected();
    }

    public static boolean hasEnrolledFingerprints(Context context) {
        if (!isRunningOnMarshmallowOrHigher()) {
            return false;
        }
        try {
            return getFingerprintManager(context).hasEnrolledFingerprints();
        } catch (SecurityException e) {
            // Samsung framework issue, only happens on some of samsung devices (J5, J7...)
            // Permission Denial:
            // getCurrentUser() requires android.permission.INTERACT_ACROSS_USERS
            return false;
        }
    }

    public static boolean getUserHasAndAllowFingerprint(Context context) {
        return hasEnrolledFingerprints(context) && PreferenceManager.getDefaultSharedPreferences(
                context).getBoolean(KEY_ALLOW_FINGERPRINT_TO_UNLOCK, false);
    }

    public static void setUserAllowFingerprint(Context context, boolean isAllow) {
        PreferenceManager.getDefaultSharedPreferences(
                context).edit().putBoolean(KEY_ALLOW_FINGERPRINT_TO_UNLOCK, isAllow).commit();
    }
}
