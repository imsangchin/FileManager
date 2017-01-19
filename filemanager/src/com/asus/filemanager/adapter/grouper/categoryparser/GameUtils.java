package com.asus.filemanager.adapter.grouper.categoryparser;

import java.util.List;

import com.asus.filemanager.provider.GameAppDbHelper;
import com.asus.filemanager.provider.GameAppProvider;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class GameUtils {
    private static final String TAG = "GameUtils";
    private static long FOUR_HOURS_IN_MILLIS = 4 * 60 * 60 * 1000;

    public static final String KEY_LAST_INSTALLED_APP_PARSED_TIME = "last_installed_app_parsed_time";

    public static void parseAppCategoryForInstalledApp(Context context) {
        long currentTime = System.currentTimeMillis();
        long lastParseTime = getLastParseTime(context);

        if (currentTime - lastParseTime < FOUR_HOURS_IN_MILLIS) {
            Log.w(TAG, "skip parse request, current: " + currentTime + ", last: " + lastParseTime);
            return;
        }

        final PackageManager pm = context.getPackageManager();
        // get a list of installed apps.
        List<ApplicationInfo> packages = pm
                .getInstalledApplications(PackageManager.GET_META_DATA);

        boolean hasValidItem = false;
        boolean allAppsInDb = true;
        for (ApplicationInfo packageInfo : packages) {
            // Filter out system app
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM) {
                if (GameAppDbHelper.isExistInDb(context, packageInfo.packageName)) {
                    Log.w(TAG, "skip Insert package :" + packageInfo.packageName);
                    continue;
                } else {
                    allAppsInDb = false;
                }
                if (parseAppCategoryByPackageName(context, packageInfo.packageName)) {
                    hasValidItem = true;
                }
            }
        }

        if (hasValidItem || allAppsInDb) {
            setLastParseTime(context, currentTime);
        }
    }

    public static boolean parseAppCategoryByPackageName(Context context, String packageName) {
        boolean hasValidItem = false;
        String categoryID = null;
        AppInfoRetriever retriever = AppInfoRetriever
                .getAppInfoRetriever(context, new String[] {packageName},
                        false, true, true, false);
        List<AppInfo> results = retriever.getAppInfo();
        // there should be exact one parse result
        if (results.size() > 0) {
            AppInfo info = results.get(0);
            if (info.isValid()) {
                categoryID = info.getCategory();
                hasValidItem = true;
            }
        }
        int isGameCategory = GameAppDbHelper.CategoryStatus.UNKNOWN.ordinal();
        if (hasValidItem) {
            if (categoryID != null && categoryID.contains("GAME")) {
                isGameCategory = GameAppDbHelper.CategoryStatus.GAME_APP.ordinal();
            } else {
                isGameCategory = GameAppDbHelper.CategoryStatus.NOT_GAME_APP.ordinal();
            }
        } else {
            isGameCategory = GameAppDbHelper.CategoryStatus.UNKNOWN.ordinal();
        }
        Log.d(TAG, "Insert package:" + packageName + ", categoryID: " + categoryID);
        ContentValues values = new ContentValues();
        values.put(GameAppProvider.PACKAGE_NAME, packageName);
        values.put(GameAppProvider.CATEGORY, categoryID);
        values.put(GameAppProvider.IS_GAME, isGameCategory);
        GameAppDbHelper.updateDbByPackageName(context, values);
        return hasValidItem;
    }

    public static void setAppComponentEnabled(Context context, Class<?> className, boolean isEnabled) {
        ComponentName receiver = new ComponentName(context, className);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                isEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private static long getLastParseTime(Context context) {
        SharedPreferences sp =PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(KEY_LAST_INSTALLED_APP_PARSED_TIME, 0);
    }

    private static void setLastParseTime(Context context, long currentTime) {
        SharedPreferences sp =PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(KEY_LAST_INSTALLED_APP_PARSED_TIME, currentTime).commit();
    }
}
