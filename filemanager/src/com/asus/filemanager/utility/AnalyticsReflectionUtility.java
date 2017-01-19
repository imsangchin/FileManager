package com.asus.filemanager.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.asus.filemanager.wrap.WrapEnvironment;

public class AnalyticsReflectionUtility {
    private static String Asus_Settings = null;
    private static final String ASUS_ANALYTICS = "asus_analytics";
    static {
        try {
            Asus_Settings = (String) (Class.forName(
                    "android.provider.Settings$System").getField(
                    "ASUS_ANALYTICS").get(String.class));
        } catch (IllegalArgumentException e) {
            // Error : IllegalArgumentException
        } catch (IllegalAccessException e) {
            // Error : IllegalAccessException
        } catch (NoSuchFieldException e) {
            // Error : NoSuchFieldException
        } catch (ClassNotFoundException e) {
            // Error : ClassNotFoundException
        }
    }

    public static boolean getEnableAsusAnalytics(Context context) {
        if (WrapEnvironment.IS_VERIZON
                || ItemOperationUtility.getInstance().enableCtaCheck()) {
            return false;
        }
        if (!InspireAsusExist(context)) {
            SharedPreferences settings2 = context.getSharedPreferences(
                    "MyPrefsFile", 0);
            return settings2.getBoolean(ConstantsUtil.PREV_INSPIREUS, true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Settings.Secure.getInt(context.getContentResolver(),
                    ASUS_ANALYTICS, 0) == 1;
        }
        return Settings.System.getInt(context.getContentResolver(),
                Asus_Settings, 0) == 1;
    }

    public static boolean InspireAsusExist(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Settings.Secure.getInt(context.getContentResolver(),
                    ASUS_ANALYTICS, -1 /* Not Defined */) != -1;
        } else {
            return Asus_Settings != null;
        }
    }

    public static void registerContentObserver(Context context,
            ContentObserver observer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri gaUri = Settings.Secure.getUriFor(ASUS_ANALYTICS);
            if (gaUri != null) {
                context.getContentResolver().registerContentObserver(gaUri,
                        false, observer);
            }
        } else {
            if (Asus_Settings != null) {
                context.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(Asus_Settings), false,
                        observer);
            }
        }
    }

    public static void unregisterContentObserver(Context context,
            ContentObserver observer) {
        context.getContentResolver().unregisterContentObserver(observer);
    }
}